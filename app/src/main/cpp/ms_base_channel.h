//
//

#ifndef MSPLAYER_BASECHANNEL_H

#define MAX_SIZE  100
#define MSPLAYER_BASECHANNEL_H


extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/time.h>
};

#include "ms_safe_queue.h"
#include "android_log_util.h"
#include "jni_callback.h"

class BaseChannel {
public:
    int stream_index; // 音频 或 视频 的下标
    SafeQueue<AVPacket *> packets; // 压缩的 数据包
    SafeQueue<AVFrame *> frames; // 原始的 数据包
    bool isPlaying; // 音频 和 视频 都会有的标记 是否播放
    AVCodecContext *codecContext = 0; // 音频 视频 都需要的 解码器上下文

    /*音视频同步  （AudioChannel VideoChannel 都需要时间基）单位而已*/
    AVRational time_base;


    JniUtil *jniCallback = 0;
    void setJNICallbakcHelper(JniUtil *jniCallbakcHelper) {
        this->jniCallback = jniCallbakcHelper;
    }

    BaseChannel(int stream_index, AVCodecContext *codecContext,AVRational time_base)
            :
            stream_index(stream_index),
            codecContext(codecContext),
             time_base(time_base)  // 注意：这个接收的是 子类传递过来的 时间基
            {
        packets.setReleaseCallback(releaseAVPacket); // 给队列设置Callback，Callback释放队列里面的数据
        frames.setReleaseCallback(releaseAVFrame); // 给队列设置Callback，Callback释放队列里面的数据
    }

   /*父类析构一定要加virtual*/
    virtual ~BaseChannel() {
        packets.clear();
        frames.clear();
    }

    /**
     * 释放 队列中 所有的 AVPacket *
     * @param packet
     */
    // typedef void (*ReleaseCallback)(T *);
    static void releaseAVPacket(AVPacket **p) {
        if (p) {
            av_packet_free(p); // 释放队列里面的 T == AVPacket
            *p = 0;
        }
    }

    /**
     * 释放 队列中 所有的 AVFrame *
     * @param packet
     */
    // typedef void (*ReleaseCallback)(T *);
    static void releaseAVFrame(AVFrame **f) {
        if (f) {
            av_frame_free(f); // 释放队列里面的 T == AVFrame
            *f = 0;
        }
    }
};
    //这句必须在最后  否则会报错：error: redefinition
#endif //MSPLAYER_BASECHANNEL_H