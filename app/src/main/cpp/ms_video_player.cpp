//
// Created by lpf on 2022/6/1.
//

#include "ms_video_player.h"
#include "ms_video_statel.h"

/**
 * 构造方法
 * @param data_source  资源路径 ，callback
 * @param callbackHelper
 */
MSPlayer::MSPlayer(const char *data_source, JniUtil *helper) {
    // this->data_source = data_source;
    // 如果被释放，会造成悬空指针

    // 深拷贝
    // this->data_source = new char[strlen(data_source)];
    // Java: demo.mp4
    // C层：demo.mp4\0  C层会自动 + \0,  strlen不计算\0的长度，所以我们需要手动加 \0

    this->data_source = new char[strlen(data_source) + 1];
    // 将 data_source 数据 拷贝给data_source
    strcpy(this->data_source, data_source);
    this->helper = helper;
    /*初始化异步锁，用户seek操作*/
    pthread_mutex_init(&seek_mutex, nullptr);
}

/**
 * 析构函数
 */
MSPlayer::~MSPlayer() {
    /*释放data_source*/
    if (data_source) {
        delete data_source;
        data_source = nullptr;
    }

    if (helper) {
        delete helper;
        helper = nullptr;
    }

    pthread_mutex_destroy(&seek_mutex);
}

/**
 * 子线程回调的方法
 * @param args  创建线程的第四个参数
 * @return
 */
void *task_prepare(void *args) {

    auto *player = static_cast<MSPlayer *>(args);
    /*子线程执行解封装的操作*/
    player->prepare_();
    /*必须返回，否则会出错，错误很难找*/
    return nullptr;
}

/**
 * 子线程执行解封装的操作
 */
void MSPlayer::prepare_() {
    /*
     * 因为FFmpeg源码是纯C的，使用上下文Context，为了贯彻环境，
     * 就相当于Java的this能不像C++、Java 够操作成员
     * */
    /*avFormatContext 视频格式上下文 */
    avFormatContext = avformat_alloc_context();

    /*这个参数用于设置额外信息*/
    AVDictionary *dictionary = nullptr;
    av_dict_set(&dictionary, "timeout", "5000000", 0);// 单位微妙

    /*
     * 1，AVFormatContext *
     * 2，路径
     * 3，AVInputFormat *fmt  Mac、Windows 摄像头、麦克风， 目前安卓用不到
     * 4，各种设置：例如：Http 连接超时， 打开rtmp的超时  AVDictionary **options
     */
    int r = avformat_open_input(&avFormatContext, data_source, nullptr, &dictionary);
    /*释放字典额外参数，用完记得释放*/
    av_dict_free(&dictionary);
    /*结果判断*/
    if (r) {
        if (helper) {
            //视频地址或者url异常
            helper->onError(MS_THREAD_CHILD, FFMPEG_CAN_NOT_OPEN_URL);
        }
        avformat_close_input(&avFormatContext);
        return;
    }

     /*
      * 查找媒体中的音视频流的信息
      *  @return >=0 if OK, AVERROR_xxx on error
      */
    r = avformat_find_stream_info(avFormatContext, nullptr);
    if (r < 0) {
        if (helper) {  //未发现音视频流
            helper->onError(MS_THREAD_CHILD, FFMPEG_CAN_NOT_FIND_STREAMS);
        }
        avformat_close_input(&avFormatContext);
        return;
    }

    /*得到视频时间 FFmepg 内部的时间单位是TimeBase
     * 如果需要使用时间单位需要除以 TimeBase才行*/
    this->duration = avFormatContext->duration / AV_TIME_BASE;

    /*编解码上下文*/
    AVCodecContext *codecContext = nullptr;

    /*根据流信息，流的个数 国内的一般是两个流：一个音频流一个视频流：*/
    for (int i = 0; i < avFormatContext->nb_streams; ++i) {
        /*获取媒体流（视频，音频）*/
        AVStream *stream = avFormatContext->streams[i];

        /*
         * 从上面的流中 获取 编码解码的【参数】
         *  从流中获取参数 后面的编码器 解码器 都需要参数（宽高 等等）
         *  */
        AVCodecParameters *parameters = stream->codecpar;
        /*可以拿到视频宽高*/
        int width = parameters->width;
        int height = parameters->height;

        /*（根据上面的【参数】）获取编解码器*/
        AVCodec *codec = avcodec_find_decoder(parameters->codec_id);
        if (!codec) {
            if (helper) {
                helper->onError(MS_THREAD_CHILD, FFMPEG_FIND_DECODER_FAIL);
            }
            avformat_close_input(&avFormatContext);
            return;
        }

        /*编解码器 上下文，真正进行编解码操作的部分*/
        codecContext = avcodec_alloc_context3(codec);
        if (!codecContext) {
            if (helper) { //没找到 解码器
                helper->onError(MS_THREAD_CHILD, FFMPEG_FIND_DECODER_FAIL);
            }
            /*释放此上下文 codecContext就行，不用担心codec，内部会自动释放 */
            avcodec_free_context(&codecContext);
            avformat_close_input(&avFormatContext);
            return;
        }

        /**
        *  将parameters 赋值给codecContext
         * @return >= 0 on success, a negative AVERROR code on failure.
         */
        r = avcodec_parameters_to_context(codecContext, parameters);
        if (r < 0) {
            if (helper) {
                /*无法根据解码器创建上下文*/
                helper->onError(MS_THREAD_CHILD, FFMPEG_ALLOC_CODEC_CONTEXT_FAIL);
            }
            /*释放此上下文 codecContext就行，不用担心codec，内部会自动释放 */
            avcodec_free_context(&codecContext);
            avformat_close_input(&avFormatContext);
            return;
        }

        /*打开解码器*/
        r = avcodec_open2(codecContext, codec, nullptr);
        /*非0就是true*/
        if (r) {
            if (helper) {
                /*打开解码器失败*/
                helper->onError(MS_THREAD_CHILD, FFMPEG_OPEN_DECODER_FAIL);
            }
            avcodec_free_context(&codecContext);
            avformat_close_input(&avFormatContext);
            return;
        }

        /*---------------------音视频同步-----------------------*/
        AVRational time_base = stream->time_base;

         /*从编解码器参数中，获取流的类型 codec_type  ===  音频 视频*/
        if (parameters->codec_type == AVMediaType::AVMEDIA_TYPE_AUDIO) {
            /*初始化音频通道对象*/
            audio_channel = new AudioChannel(i, codecContext, time_base);
            /*非直播，才有意义把 JNICallbackHelper传递过去*/
            if (this->duration != 0) {
                audio_channel->setJNICallbakcHelper(helper);
            }
        } else if (parameters->codec_type == AVMediaType::AVMEDIA_TYPE_VIDEO) {

            /*视频独有的 fps值*/
            AVRational fps_rational = stream->avg_frame_rate;
            int fps = av_q2d(fps_rational);
            /*初始化视频通道*/
            video_channel = new VideoChannel(i, codecContext, time_base, fps);
            /*设置渲染回调*/
            video_channel->setRenderCallback(renderCallback);
            /*非直播，才有意义把 JNICallbackHelper传递过去*/
            if (this->duration != 0) {
                video_channel->setJNICallbakcHelper(helper);
            }

        }

    }

    /*如果流中没有音频 也没有视频 【健壮性校验】*/
    if (!audio_channel && !video_channel) {
        if (helper) {
            helper->onError(MS_THREAD_CHILD, FFMPEG_NOMEDIA);
        }
        if (codecContext) {
            /*
             * 释放此上下文 codecContext 不用管codec
             * */
            avcodec_free_context(&codecContext);
        }
        avformat_close_input(&avFormatContext);
        return;
    }

    /*准备好准备工作，通知给上层*/
    if (helper) {
        helper->onPrepared(MS_THREAD_CHILD);
    }
}

/*data_source == 文件io流，直播网络rtmp，耗时操作，所以必须使用子线程*/
void MSPlayer::prepare() {
    /*创建子线程
     * 第一个参数：线程id
     * 第二个参数：线程属性 一般用不到
     * 第三个参数：子线程执行的方法
     * 第四个参数：就是子线程执行方法的参数
     * */
    pthread_create(&pid_prepare, nullptr, task_prepare, this);
}

/**
 * 开始task 子线程执行函数
 * @param args
 * @return
 */
void *task_start(void *args) {
    MSPlayer *player = static_cast<MSPlayer *>(args);
    player->start_();
    return nullptr;
}

/**
 * 开始播放
 * 1.执行解码
 * 2.执行播放
 */
void MSPlayer::start() {
    isPlaying = true;

    /*
     *  视频：
     *  1.把队列里面的压缩包(AVPacket *)取出来，然后解码成（AVFrame * ）原始包 ----> 保存队列
     *   2.把队列里面的原始包(AVFrame *)取出来播放
     * */
    if (video_channel) {
        video_channel->setAudioChannel(audio_channel);
        video_channel->start();
    }


    /*
     * 音频：
     * 1.把队列里面的压缩包(AVPacket *)取出来，然后解码成（AVFrame * ）原始包 ----> 保存队列
     * 2.把队列里面的原始包(AVFrame *)取出来， 音频播放
     * */
    if (audio_channel) {
        audio_channel->start();
    }

    /*
     *  音频和视频 压缩包加入队列里面去
     *  创建子线程
     * */
    pthread_create(&pid_start, nullptr, task_start, this);

}

/**
 * 子线程执行具体的代码逻辑
 */
void MSPlayer::start_() {
    if (jniCallback){
        jniCallback->onPlayState(MS_THREAD_CHILD,1);
    }
    while (isPlaying) {

        // AVPacket 可能是音频 也可能是视频（压缩包）
        AVPacket *packet = av_packet_alloc();
        if (video_channel && video_channel->packets.size() > AV_MAX_SIZE) {
            av_usleep(10 * 1000);
            continue;
        }

        if (audio_channel && audio_channel->packets.size() > AV_MAX_SIZE) {
            av_usleep(10 * 1000);
            continue;
        }

        int ret = av_read_frame(avFormatContext, packet);
        if (!ret) { // ret == 0

            // AudioChannel    队列
            // VideioChannel   队列

            // 把我们的 AVPacket* 加入队列， 音频 和 视频
            /*AudioChannel.insert(packet);
            VideioChannel.insert(packet);*/

            if (video_channel && video_channel->stream_index == packet->stream_index) {
                // 代表是视频
                video_channel->packets.insertToQueue(packet);
            } else if (audio_channel && audio_channel->stream_index == packet->stream_index) {
                // 代表是音频   将音频 压缩数据包 放入队列
                audio_channel->packets.insertToQueue(packet);
            }
        } else if (ret == AVERROR_EOF) { //   end of file == 读到文件末尾了 == AVERROR_EOF
            //表示读完了，要考虑释放播放完成，表示读完了 并不代表播放完毕，以后处理【同学思考 怎么处理】
            if (video_channel->packets.empty() && audio_channel->packets.empty()) {
                break;
            }
        } else {
            break; // av_read_frame 出现了错误，结束当前循环
        }
    }// end while
    isPlaying = 0;
    video_channel->stop();
    audio_channel->stop();  //音频停止
}


void MSPlayer::setRenderCallback(RenderCallback renderCallback) {
    this->renderCallback = renderCallback;
}

int MSPlayer::getDuration() {
    return duration;
}

void MSPlayer::seek(int progress) {
    // 健壮性判断
    if (progress < 0 || progress > duration) {
        //需要给Java的回调
        return;
    }
    if (!audio_channel && !video_channel) {
        //需要给Java的回调
        return;
    }
    if (!avFormatContext) {
        //需要给Java的回调
        return;
    }

    // avFormatContext 多线程， av_seek_frame内部会对我们的 formatContext上下文的成员做处理，安全的问题
    // 互斥锁 保证多线程情况下安全

    pthread_mutex_lock(&seek_mutex);

    // FFmpeg 大部分单位 == 时间基AV_TIME_BASE
    /**
     * 1.avFormatContext 安全问题
     * 2.-1 代表默认情况，FFmpeg自动选择 音频 还是 视频 做 seek，  模糊：0视频  1音频
     * 3. AVSEEK_FLAG_ANY（老实） 直接精准到 拖动的位置，问题：如果不是关键帧，B帧 可能会造成 花屏情况
     *    AVSEEK_FLAG_BACKWARD（则优  8的位置 B帧 ， 找附件的关键帧 6，如果找不到他也会花屏）
     *    AVSEEK_FLAG_FRAME 找关键帧（非常不准确，可能会跳的太多），一般不会直接用，但是会配合用
     */
    int r = av_seek_frame(avFormatContext, -1, progress * AV_TIME_BASE, AVSEEK_FLAG_BACKWARD);
    if (r < 0) {
        //需要给Java的回调
        return;
    }

    // 如果你的视频，假设出了花屏，AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME， 缺点：慢一些
    // 有一点点冲突，后面再看 （则优  | 配合找关键帧）
    // av_seek_frame(avFormatContext, -1, progress * AV_TIME_BASE, AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);

    // 音视频正在播放，用户去 seek，我是不是应该停掉播放的数据  音频1frames 1packets，  视频1frames 1packets 队列

    // 这四个队列，还在工作中，让他们停下来， seek完成后，重新播放
    if (audio_channel) {
        audio_channel->packets.setWork(0);  // 队列不工作
        audio_channel->frames.setWork(0);  // 队列不工作
        audio_channel->packets.clear();
        audio_channel->frames.clear();
        audio_channel->packets.setWork(1); // 队列继续工作
        audio_channel->frames.setWork(1);  // 队列继续工作
    }

    if (video_channel) {
        video_channel->packets.setWork(0);  // 队列不工作
        video_channel->frames.setWork(0);  // 队列不工作
        video_channel->packets.clear();
        video_channel->frames.clear();
        video_channel->packets.setWork(1); // 队列继续工作
        video_channel->frames.setWork(1);  // 队列继续工作
    }

    pthread_mutex_unlock(&seek_mutex);
}

void *task_stop(void *args) {
    auto *player = static_cast<MSPlayer *>(args);
    player->stop_(player);
    return nullptr;
}


void MSPlayer::stop() {

    if (jniCallback){
        jniCallback->onPlayState(MS_THREAD_MAIN,0);
    }
    // 只要用户关闭了，就不准你回调给Java成 start播放
    helper = nullptr;
    if (audio_channel) {
        audio_channel->jniCallback = nullptr;
    }
    if (video_channel) {
        video_channel->jniCallback = nullptr;
    }

    if (jniCallback){
        jniCallback= nullptr;
    }


    // 如果是直接释放 我们的 prepare_ start_ 线程，不能暴力释放 ，否则会有bug
    // 让他 稳稳的停下来

    // 我们要等这两个线程 稳稳的停下来后，我再释放DerryPlayer的所以工作
    // 由于我们要等 所以会ANR异常

    // 所以我们我们在开启一个 stop_线程 来等你 稳稳的停下来
    // 创建子线程
    pthread_create(&pid_stop, nullptr, task_stop, this);

}

void MSPlayer::stop_(MSPlayer *pPlayer) {

    isPlaying = false;
    pthread_join(pid_prepare, nullptr);
    pthread_join(pid_start, nullptr);

    // pid_prepare pid_start 全部停止下来了 安全的停下来
    if (avFormatContext) {
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = nullptr;
    }
    DELETE(audio_channel);
    DELETE(video_channel);
    DELETE(pPlayer);
    DELETE(jniCallback);

}

bool MSPlayer::getPlayState() {
    return isPlaying;
}

void MSPlayer::setJNICallback(JniUtil *jniCallback) {
    this->jniCallback=jniCallback;
}



