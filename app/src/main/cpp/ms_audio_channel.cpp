//
// Created by lpf on 2022/6/1.
//

#include "ms_audio_channel.h"
   /*音频三要素
    * 1.采样率 44100 48000
    * 2.位声/采用格式大小  16bit == 2字节
    * 3.声道数 2  --- 人类就是两个耳朵
    */
   /*
    * 音频压缩数据包  AAC
    * 采样率 44100
    * 32bit  算法效率高 浮点运算高
    *  双通道 立体声
    *
    *  重采样 音频格式转换 OpenSL ES
    * */
/**
 * 音频通道需要开启两个线程：
 * 第一个线程： 音频：取出队列的压缩包 进行解码 解码后的原始包再push队列中去
   第二线线程：音频：从队列取出原始包，播放
 *
 * 音频通道 构造函数
 * @param stream_index
 * @param codecContext
 * @param time_base
 */
AudioChannel::AudioChannel(int stream_index, AVCodecContext *codecContext, AVRational time_base)
        : BaseChannel(stream_index, codecContext,time_base) {


    /*
     * 缓冲区大小怎么定义：out_buffers  out_buffers_size
     * 初始化缓冲区
     * AV_SAMPLE_FMT_S16: 位声、采用格式大小，存放大小
     * */
    out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO); // STEREO:双声道类型 == 获取 声道数 2
    out_sample_size = av_get_bytes_per_sample(AV_SAMPLE_FMT_S16); // 每个sample是16 bit == 2字节
    /*采样率*/
    out_sample_rate = 44100;

    /*out_buffers_size = 176,400  44100 * 2 * 2 = 176,400*/
    out_buffers_size = out_sample_rate * out_sample_size * out_channels;
    /*堆区开辟而已*/
    out_buffers = static_cast<uint8_t *>(malloc(out_buffers_size));

    /*FFmpeg 音频 重采样  音频重采样上下文 */
    swr_ctx  = swr_alloc_set_opts(0,
           /* 输出*/
                                  AV_CH_LAYOUT_STEREO,  // 声道布局类型 双声道
                                  AV_SAMPLE_FMT_S16,  // 采样大小 16bit
                                  out_sample_rate, // 采样率  44100

            /*输入*/
                                  codecContext->channel_layout, // 声道布局类型
                                  codecContext->sample_fmt, // 采样大小
                                  codecContext->sample_rate,  // 采样率
                                  0, 0);
    /*初始化重采样上下文*/
    swr_init(swr_ctx);
}

AudioChannel::~AudioChannel() {
    if (swr_ctx){
        swr_free(&swr_ctx);
    }
    DELETE(out_buffers);
}

void AudioChannel::stop() {


    /* 等解码线程 播放线程全部停止，再做释放工作*/
    pthread_join(pid_audio_decode, nullptr);
    pthread_join(pid_audio_play, nullptr);


    /*保证两个线程执行完毕，再释放*/
    isPlaying = false;
    packets.setWork(0);
    frames.setWork(0);

    /*设置停止状态*/
    if (bqPlayerPlay) {
        (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_STOPPED);
        bqPlayerPlay = nullptr;
    }

    /*销毁播放器*/
    if (bqPlayerObject) {
        (*bqPlayerObject)->Destroy(bqPlayerObject);
        bqPlayerObject = nullptr;
        bqPlayerBufferQueue = nullptr;
    }

    /*销毁混音器*/
    if (outputMixObject) {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = nullptr;
    }

    /*销毁引擎*/
    if (engineObject) {
        (*engineObject)->Destroy(engineObject);
        engineObject = nullptr;
        engineInterface = nullptr;
    }

    /*队列清空*/
    packets.clear();
    frames.clear();

}

/**
 * 音频解码
 * @param args
 * @return
 */
void *task_audio_decode(void * args) {
    auto *audio_channel = static_cast<AudioChannel *>(args);
    audio_channel->audio_decode();
    return nullptr;
}

/**
 * 音频播放
 * @param args
 * @return
 */
void *task_audio_play(void * args) {
    auto *audio_channel = static_cast<AudioChannel *>(args);
    audio_channel->audio_play();
    return nullptr;
}

/**
 * 把队列里面的压缩包(AVPacket *)取出来，然后解码成（AVFrame * ）原始包 ----> 保存队列
 * 把队列里面的原始包(AVFrame *)取出来， 音频播放OpenSLES
 */
void AudioChannel::start() {
    isPlaying = 1;

    /*队列开始工作了samples_per_channel*/
    packets.setWork(1);
    frames.setWork(1);

    /*音频：取出队列的压缩包 进行解码 解码后的原始包 再push队列中去 （音频：PCM数据）*/
    pthread_create(&pid_audio_decode, 0, task_audio_decode, this);

    /*第二线线程：视频：从队列取出原始包，播放  音频播放OpenSLES*/
    pthread_create(&pid_audio_play, 0, task_audio_play, this);

}

/**
 *  音频：取出队列的压缩包 进行解码 解码后的原始包 再push队列中去 （音频：PCM数据）
 */
void AudioChannel::audio_decode() {
    AVPacket *pkt = 0;
    while (isPlaying) {
        if (isPlaying&& frames.size() > AV_MAX_SIZE){
            av_usleep(10000);
            continue;
        }
        /*阻塞式函数*/
        int ret = packets.getQueueAndDel(pkt);
        if (!isPlaying) {
            break; // 如果关闭了播放，跳出循环，releaseAVPacket(&pkt);
        }

        if (!ret) { // ret == 0
            continue; // 哪怕是没有成功，也要继续（假设：你生产太慢(压缩包加入队列)，我消费就等一下你）
        }

        // 最新的FFmpeg，和旧版本差别很大， 新版本：1.发送pkt（压缩包）给缓冲区，  2.从缓冲区拿出来（原始包）
        ret = avcodec_send_packet(codecContext, pkt);

        /*FFmpeg源码缓存一份pkt，可以释放*/
//        releaseAVPacket(&pkt);

        if (ret) {
            break; // avcodec_send_packet 出现了错误，结束循环
        }

        /*下面是从FFmpeg缓冲区 获取 原始包
         * AVFrame： 解码后的视频原始数据包
         * */
        AVFrame *frame = av_frame_alloc();
        ret = avcodec_receive_frame(codecContext, frame);
        /*音频也有帧的概念，所以获取原始包的时候，最好还是判断下*/
        if (ret == AVERROR(EAGAIN)) {
            /*有可能音频帧，也会获取失败，重新拿一次*/
            continue;
        } else if (ret != 0) {
            if (frame){
                releaseAVFrame(&frame);
            }
            break; // 错误了
        }
        /*拿到了原始包-- PCM数据*/
        frames.insertToQueue(frame);
        av_packet_unref(pkt);
        releaseAVPacket(&pkt);
    } // while end
//    releaseAVPacket(&pkt);
    /* 释放结构体内部成员在堆区分配的内存*/
    av_packet_unref(pkt);
    /*释放AVPacket **/
    releaseAVPacket(&pkt);
}


/**
 * 回调函数
 * @param bq  队列
 * @param args  this // 给回调函数的参数
 */
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void * args) {

    auto *audio_channel = static_cast<AudioChannel *>(args);

    int pcm_size = audio_channel->getPCM();

    // 添加数据到缓冲区里面去
    (*bq)->Enqueue(
            bq, // 传递自己，为什么（因为没有this，为什么没有this，因为不是C++对象，所以需要传递自己） JNI讲过了
            audio_channel->out_buffers, // PCM数据
            pcm_size); // PCM数据对应的大小，缓冲区大小怎么定义？（复杂）
}

/**
 * 1.out_buffers给予数据
 * 2.out_buffers给予数据的大小计算工作
 * @return  大小还要计算，因为我们还要做重采样工作，重采样之后，大小不同了
 */
int AudioChannel::getPCM() {
    int pcm_data_size = 0;

    /*
     * 获取PCM数据
     * PCM数据在哪里:队列frames队列中  frame->data == PCM数据(待重采样   32bit)
     * */
    AVFrame *frame = 0;
    while (isPlaying) {
        int ret = frames.getQueueAndDel(frame);
        if (!isPlaying) {
            /*如果关闭了播放，跳出循环，releaseAVPacket(&pkt);*/
        }
        if (!ret) {
            /*哪怕是没有成功，也要继续（可能是由于生产太慢(原始包加入队列)）*/
            continue;
        }


        /*
         * 开始重采样
         * 来源：10个48000   ---->  目标:44100  11个44100
         * 获取单通道的样本数 (计算目标样本数：  10个48000 --->  48000/44100因为除不尽  11个44100)
         * */
        int dst_nb_samples = av_rescale_rnd(swr_get_delay(swr_ctx, frame->sample_rate) + frame->nb_samples, // 获取下一个输入样本相对于下一个输出样本将经历的延迟
                                            out_sample_rate, // 输出采样率
                                            frame->sample_rate, // 输入采样率
                                            AV_ROUND_UP); // 先上取 取去11个才能容纳的上

        /*
         * pcm的处理逻辑
         * 音频播放器的数据格式是在下面定义的
         * 而原始数据（待播放的音频pcm数据）
         * 重采样工作
         * 返回的结果：每个通道输出的样本数(注意：是转换后的)    做一个简单的重采样实验(通道基本上都是:1024)
         *
         * */
        int samples_per_channel = swr_convert(swr_ctx,
                // 下面是输出区域
                                              &out_buffers,  // 【成果的buff】  重采样后的
                                              dst_nb_samples, // 【成果的 单通道的样本数 无法与out_buffers对应，所以有下面的pcm_data_size计算】

                // 下面是输入区域
                                              (const uint8_t **) frame->data, // 队列的AVFrame * 那的  PCM数据 未重采样的
                                              frame->nb_samples); // 输入的样本数

        /*由于out_buffers 和 dst_nb_samples 无法对应，所以需要重新计算*/
        pcm_data_size = samples_per_channel * out_sample_size * out_channels; // 941通道样本数  *  2样本格式字节数  *  2声道数  =3764


        /*
         * 单通道样本数:1024  * 2声道  * 2(16bit)  =  4,096
         * 时间基TimeBase理解：例如：（fps25 一秒钟25帧， 那么每一帧==25分之1，而25分之1就是时间基概念）
         * frame->best_effort_timestamp // 时间有单位：微妙 毫秒 秒 等，但是在FFmpeg里面有自己的单位（时间基TimeBase）
         * */

        /*
          typedef struct AVRational{
            int num; ///< Numerator   分子
            int den; ///< Denominator 分母
        } AVRational;
         */

        // audio_time == 0.00000 0.0231233 0.035454 就是音频播放的时间搓
        audio_time = frame->best_effort_timestamp * av_q2d(time_base); // 必须这样计算后，才能拿到真正的时间搓


        if (this->jniCallback) {
            jniCallback->onProgress(MS_THREAD_CHILD, audio_time);
        }

        break; // 利用while循环 来写我们的逻辑

    } // while end

    // FFmpeg录制 Mac 麦克风  输出 每一个音频包的size == 4096
    // 4096是单声道的样本数，  44100是每秒钟采样的数
    // 采样率 和 样本数的关系：
    // 单通道样本数:1024  * 2声道  * 2(16bit)  =  4,096 ==  4096是单声道的样本数
    //采样率 44100是每秒钟采样的次数

    // 样本数 = 采样率 * 声道数 * 位声

    // 双声道的样本数：  （采样率 * 声道数 * 位声） * 2
    av_frame_unref(frame);
    av_frame_free(&frame);

    return pcm_data_size;
}

/**
 * 视频：从队列取出原始包，播放  音频播放OpenSLES
 */
void AudioChannel::audio_play() {
    SLresult result; // 用于接收 执行成功或者失败的返回值

    /*创建引擎对象并获取  创建引擎对象：SLObjectItf engineObject*/
    result = slCreateEngine(&engineObject, 0, 0, 0, 0, 0);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("创建引擎 slCreateEngine error");
        return;
    }

    /*初始化引擎*/
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE); // SL_BOOLEAN_FALSE:延时等待你创建成功
    if (SL_RESULT_SUCCESS != result) {
        LOGE("创建引擎 Realize error");
        return;
    }

    /*获取引擎接口*/
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineInterface);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("创建引擎接口 Realize error");
        return;
    }

    /*健壮判断*/
    if (engineInterface) {
        LOGD("创建引擎接口 create success");
    } else {
        LOGD("创建引擎接口 create error");
        return;
    }

    /*设置混音器  环境特效，混响特效，.. */
    result = (*engineInterface)->CreateOutputMix(engineInterface, &outputMixObject,
                                                 0, nullptr, nullptr);
    if (SL_RESULT_SUCCESS != result) {
        LOGD("初始化混音器 CreateOutputMix failed");
        return;
    }

    /*初始化混音器*/
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE); // SL_BOOLEAN_FALSE:延时等待你创建成功
    if (SL_RESULT_SUCCESS != result) {
        LOGD("初始化混音器 (*outputMixObject)->Realize failed");
        return;
    }

    // 不启用混响可以不用获取混音器接口 【声音的效果】
    // 获得混音器接口
    /*
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                                             &outputMixEnvironmentalReverb);
    if (SL_RESULT_SUCCESS == result) {
    // 设置混响 ： 默认。
    SL_I3DL2_ENVIRONMENT_PRESET_ROOM: 室内
    SL_I3DL2_ENVIRONMENT_PRESET_AUDITORIUM : 礼堂 等
    const SLEnvironmentalReverbSettings settings = SL_I3DL2_ENVIRONMENT_PRESET_DEFAULT;
    (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(
           outputMixEnvironmentalReverb, &settings);
    }
    */
    LOGI("设置混音器 Success");

    /*创建播放器  创建buffer缓存类型的队列  2的队列大小*/
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
                                                       10};

    // pcm数据格式 == PCM是不能直接播放，mp3可以直接播放(参数集)，人家不知道PCM的参数
    //  SL_DATAFORMAT_PCM：数据格式为pcm格式
    //  2：双声道
    //  SL_SAMPLINGRATE_44_1：采样率为44100
    //  SL_PCMSAMPLEFORMAT_FIXED_16：采样格式为16bit
    // SL_PCMSAMPLEFORMAT_FIXED_16：数据大小为16bit
    // SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT：左右声道（双声道）
    // SL_BYTEORDER_LITTLEENDIAN：小端模式
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, // PCM数据格式
                                   2, // 声道数
                                   SL_SAMPLINGRATE_44_1, // 采样率（每秒44100个点）
                                   SL_PCMSAMPLEFORMAT_FIXED_16, // 每秒采样样本 存放大小 16bit
                                   SL_PCMSAMPLEFORMAT_FIXED_16, // 每个样本位数 16bit
                                   SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, // 前左声道  前右声道
                                   SL_BYTEORDER_LITTLEENDIAN}; // 字节序(小端) 例如：int类型四个字节（到底是 高位在前 还是 低位在前 的排序方式，一般我们都是小端）

    /**
     * 数据源 将上述配置信息放到这个数据源中
     *  audioSrc最终配置音频信息的成果，给后面代码使用
     */
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};
    // audioSrc 给后面代码用的
    // 独立声卡：24bit  集成声卡16bit

    // 配置音轨（输出）
    // 设置混音器
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject}; // SL_DATALOCATOR_OUTPUTMIX:输出混音器类型
    SLDataSink audioSnk = {&loc_outmix, NULL}; // outmix最终混音器的成果，给后面代码使用
    // 需要的接口 操作队列的接口
    const SLInterfaceID ids[1] = {SL_IID_BUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};

    // 创建播放器 SLObjectItf bqPlayerObject
    result = (*engineInterface)->CreateAudioPlayer(engineInterface, // 参数1：引擎接口
                                                   &bqPlayerObject, // 参数2：播放器
                                                   &audioSrc, // 参数3：音频配置信息
                                                   &audioSnk, // 参数4：混音器

            // 打开队列的工作
                                                   1, // 参数5：开放的参数的个数
                                                   ids,  // 参数6：代表我们需要 Buff
                                                   req // 参数7：代表我们上面的Buff 需要开放出去
    );

    if (SL_RESULT_SUCCESS != result) {
        LOGD("创建播放器 CreateAudioPlayer failed!");
        return;
    }

    //初始化播放器：SLObjectItf bqPlayerObject
    result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);  // SL_BOOLEAN_FALSE:延时等待你创建成功
    if (SL_RESULT_SUCCESS != result) {
        LOGD("实例化播放器 CreateAudioPlayer failed!");
        return;
    }
    LOGD("创建播放器 CreateAudioPlayer success!");

    //获取播放器接口 【以后播放全部使用 播放器接口去干（核心）】
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerPlay); // SL_IID_PLAY:播放接口 == iplayer
    if (SL_RESULT_SUCCESS != result) {
        LOGD("获取播放接口 GetInterface SL_IID_PLAY failed!");
        return;
    }
    LOGI("创建播放器 Success");


    /*设置回调函数*/
    //获取播放器队列接口：SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue  // 播放需要的队列
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE, &bqPlayerBufferQueue);
    if (result != SL_RESULT_SUCCESS) {
        LOGD("获取播放队列 GetInterface SL_IID_BUFFERQUEUE failed!");
        return;
    }

    //设置回调 void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
    (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue,  // 传入刚刚设置好的队列
                                             bqPlayerCallback,  // 回调函数
                                             this); // 给回调函数的参数
    LOGI("设置播放回调函数 Success");

    /*设置播放器状态为播放状态*/
    (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    LOGI("设置播放器状态为播放状态 Success");

    /*手动激活回调函数*/
    bqPlayerCallback(bqPlayerBufferQueue, this);
    LOGI("手动激活回调函数 Success");
}




