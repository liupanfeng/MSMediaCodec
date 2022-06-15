#include "jni_callback.h"

/**
 * 初始化 vm env 并得到回调方法的方法id
 * @param vm
 * @param env
 * @param job
 */
JniUtil::JniUtil(JavaVM *vm, JNIEnv *env, jobject job) {
    this->vm = vm;
    this->env = env;

    /*jobject不能跨越线程不能跨越函数，必须全局引用*/
    this->job = env->NewGlobalRef(job);

    jclass clazz = env->GetObjectClass(job);
    /*准备工作 方法id*/
    jmd_prepared = env->GetMethodID(clazz, "onPrepared", "()V");
    /*异常方法 方法id*/
    jmd_onError = env->GetMethodID(clazz, "onError", "(I)V");
    /*播放音频的时间进度回调 方法id*/
    jmd_onProgress = env->GetMethodID(clazz, "onProgress", "(I)V");
    /*播放状态回调*/
    jmd_onPlayState =env->GetMethodID(clazz,"onPlayStateChange","(I)V");

}

/**
 * 析构方法 释放指针变量
 */
JniUtil::~JniUtil()  {
    vm = nullptr;
    /*释放全局变量*/
    env->DeleteGlobalRef(job);
    job = nullptr;
    env = nullptr;
}
/**
 * 准备工作回调
 * @param thread_type 线程类型
 */
void JniUtil::onPrepared(int thread_type) {
    if (thread_type == MS_THREAD_MAIN) {
        /*主线程*/
        env->CallVoidMethod(job, jmd_prepared);
    } else if (thread_type == MS_THREAD_CHILD) {
        /* 子线程
         * env也不可以跨线程吧，申明一个新的env
         * */
        JNIEnv * env_child;
        vm->AttachCurrentThread(&env_child, nullptr);
        env_child->CallVoidMethod(job, jmd_prepared);
        vm->DetachCurrentThread();
    }
}

/**
 * 异常回调
 * @param thread_type 线程类型
 */
void JniUtil::onError(int thread_type, int error_code) {
    if (thread_type == MS_THREAD_MAIN){
        /*主线程*/
        env->CallVoidMethod(job,jmd_onError,error_code);
    } else{
        /*当前子线程的 JNIEnv  JNIEnv 不能传递*/
        JNIEnv *env_child;
        vm->AttachCurrentThread(&env_child,0);
        env_child->CallVoidMethod(job,jmd_onError,error_code);
        vm->DetachCurrentThread();

    }
}

/**
 * 播放进度回调
 * @param thread_type 线程类型
 */
void JniUtil::onProgress(int thread_type, int audio_time) {
    if (thread_type == MS_THREAD_MAIN) {
        /*主线程*/
        env->CallVoidMethod(job, jmd_onError);
    } else {
        /*当前子线程的 JNIEnv*/
        JNIEnv *env_child;
        vm->AttachCurrentThread(&env_child, 0);
        env_child->CallVoidMethod(job, jmd_onProgress, audio_time);
        vm->DetachCurrentThread();
    }
}

/**
 * 播放状态回调
 * @param thread_type
 * @param play_state
 */
void JniUtil::onPlayState(int thread_type, int play_state) {
    if (thread_type == MS_THREAD_MAIN) {
        /*主线程*/
        env->CallVoidMethod(job, jmd_onPlayState,play_state);
    } else {
        /*当前子线程的 JNIEnv*/
        JNIEnv *env_child;
        vm->AttachCurrentThread(&env_child, 0);
        env_child->CallVoidMethod(job, jmd_onPlayState, play_state);
        vm->DetachCurrentThread();
    }
}