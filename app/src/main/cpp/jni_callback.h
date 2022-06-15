#ifndef DERRYPLAYER_JNICALLBACK_H
#define DERRYPLAYER_JNICALLBACK_H

#include <jni.h>
#include "ms_video_statel.h"

/**
 * jni 工具类  处理回调
 */
class JniUtil {

private:
    JavaVM *vm = 0;
    JNIEnv *env = 0;
    /*jobject不能跨越线程，不能跨越函数，必须全局引用*/
    jobject job;
    /*准备工作回调 方法id*/
    jmethodID jmd_prepared;
    /*异常回调 方法id*/
    jmethodID jmd_onError;
    /*播放音频进度回调 方法id*/
    jmethodID jmd_onProgress;
    /*播放状态回调*/
    jmethodID jmd_onPlayState;

public:
    JniUtil(JavaVM *vm, JNIEnv *env, jobject job);
    ~JniUtil();

    void onPrepared(int thread_mode);
    void onError(int thread_type, int error_code);

    void onProgress(int thread_type, int audio_time);

    void onPlayState(int thread_type,int play_state);

};



#endif //DERRYPLAYER_JNICALLBACK_H
