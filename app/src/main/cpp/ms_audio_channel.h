//
// Created by lpf on 2022/6/1.
//
//

#ifndef MSPLAYER_MS_AUDIO_CHANNEL_H
#define MSPLAYER_MS_AUDIO_CHANNEL_H


#include "ms_base_channel.h"
#include "jni_callback.h"

//播放音频原始数据 PCM
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

//音频重采样
extern "C"{
#include <libswresample/swresample.h>
};

/**
 * 音频通道
 */
class AudioChannel : public BaseChannel{

private:
    pthread_t pid_audio_decode;  //音频解码线程id
    pthread_t pid_audio_play;   //音频播放线程id
public:

    int out_channels;           //输出声道数量
    int out_sample_size;        //每个样本的字节数
    int out_sample_rate;        //采样率
    int out_buffers_size;       //缓冲区大小
    uint8_t *out_buffers = 0;   //缓冲区类型
    SwrContext *swr_ctx = 0;

public:
    //引擎对象
    SLObjectItf engineObject = 0;
    // 引擎接口
    SLEngineItf engineInterface = 0;
    // 混音器
    SLObjectItf outputMixObject = 0;
    // 播放器
    SLObjectItf bqPlayerObject = 0;
    // 播放器接口
    SLPlayItf bqPlayerPlay = 0;

    // 播放器队列接口
    SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue = 0;
    /*音视频同步*/
    double audio_time;

public:
    AudioChannel(int streamIndex, AVCodecContext *codecContext, AVRational rational);
    virtual ~AudioChannel();
    void stop();
    void start();

    void audio_decode();

    void audio_play();

    int getPCM();
};


#endif //MSPLAYER_MS_AUDIO_CHANNEL_H
