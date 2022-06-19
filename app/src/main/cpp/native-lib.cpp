#include <jni.h>
#include <string>

#include "yuvengine.h"
#include "ms_video_player.h"
#include "jni_callback.h"
#include <android/native_window_jni.h>
#include "android_log_util.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_meishe_msmediacodec_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_meishe_msmediacodec_MSYuvOperateJni_startYuvEngine(JNIEnv *env, jclass clazz) {
    YuvEngine* pYuvEngine =  new YuvEngine;
    if(pYuvEngine != NULL) {
        return reinterpret_cast<long> (pYuvEngine);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSYuvOperateJni_Nv21ToNv12(JNIEnv * env, jclass jcls __unused,
                                                        jlong jcPtr, jbyteArray jNv21Data, jbyteArray jNv12Data, jint jwidth, jint jheight) {
    jbyte* jNv21 = env->GetByteArrayElements(jNv21Data, NULL);
    jbyte* jNv12 = env->GetByteArrayElements(jNv12Data, NULL);

    unsigned char* pNv21 = (unsigned char*)jNv21;
    unsigned char* pNv12 = (unsigned char*)jNv12;

    YuvEngine* pYuvWater = reinterpret_cast<YuvEngine*> (jcPtr);
    pYuvWater->Nv21ToNv12(pNv21,pNv12,(int)jwidth, (int)jheight);
    env->ReleaseByteArrayElements(jNv21Data, jNv21, 0);
    env->ReleaseByteArrayElements(jNv12Data, jNv12, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSYuvOperateJni_stopYuvEngine(JNIEnv *env, jclass clazz, jlong c_ptr) {
    YuvEngine* pYuvWater = reinterpret_cast<YuvEngine*> (c_ptr);
    delete pYuvWater;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSYuvOperateJni_Nv12ClockWiseRotate90(JNIEnv *env, jclass jcls __unused, jlong jcPtr, jbyteArray jsrcNv12, jint jsrcWidth, jint jsrcHeight, jbyteArray joutData, jintArray joutWidth, jintArray joutHeight) {
    jbyte* jsrcNv12Byte = env->GetByteArrayElements(jsrcNv12, NULL);
    jbyte* joutDataByte = env->GetByteArrayElements(joutData, NULL);

    jint* joutWidthInt = env->GetIntArrayElements(joutWidth, NULL);
    jint* joutHeightInt = env->GetIntArrayElements(joutHeight, NULL);

    int* poutWidth = (int*)joutWidthInt;
    int* poutHeight = (int*)joutHeightInt;

    unsigned char* pSrcNv12 = (unsigned char*)jsrcNv12Byte;
    unsigned char* pOutData = (unsigned char*)joutDataByte;

    YuvEngine* pYuvWater = reinterpret_cast<YuvEngine*> (jcPtr);
    pYuvWater->Nv12ClockWiseRotate90(pSrcNv12,(int)jsrcWidth,(int)jsrcHeight,pOutData,poutWidth, poutHeight);

    LOGD("%s: outWidth: %d,outHeight:%d",__FUNCTION__,*poutWidth,*poutHeight);
    env->ReleaseIntArrayElements(joutWidth, joutWidthInt, 0);
    env->ReleaseIntArrayElements(joutHeight, joutHeightInt, 0);
    env->ReleaseByteArrayElements(jsrcNv12, jsrcNv12Byte, 0);
    env->ReleaseByteArrayElements(joutData, joutDataByte, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSYuvOperateJni_Nv21ToI420 (JNIEnv * env, jclass jcls __unused, jlong jcPtr,
                                                         jbyteArray jNv21Data, jbyteArray jI420Data, jint jwidth, jint jheight) {
    jbyte* jNv21 = env->GetByteArrayElements(jNv21Data, NULL);
    jbyte* jI420 = env->GetByteArrayElements(jI420Data, NULL);

    unsigned char* pNv21 = (unsigned char*)jNv21;
    unsigned char* pI420 = (unsigned char*)jI420;

    YuvEngine* pYuvWater = reinterpret_cast<YuvEngine*> (jcPtr);
    pYuvWater->Nv21ToI420(pNv21,pI420,(int)jwidth, (int)jheight);
    env->ReleaseByteArrayElements(jNv21Data, jNv21, 0);
    env->ReleaseByteArrayElements(jI420Data, jI420, 0);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSYuvOperateJni_I420ClockWiseRotate90(JNIEnv *env, jclass jcls __unused, jlong jcPtr, jbyteArray jsrcI420, jint jsrcWidth,
                                                                   jint jsrcHeight, jbyteArray joutData, jintArray joutWidth, jintArray joutHeight) {
    jbyte* jsrcI420Byte = env->GetByteArrayElements(jsrcI420, NULL);
    jbyte* joutDataByte = env->GetByteArrayElements(joutData, NULL);

    jint* joutWidthInt = env->GetIntArrayElements(joutWidth, NULL);
    jint* joutHeightInt = env->GetIntArrayElements(joutHeight, NULL);

    int* poutWidth = (int*)joutWidthInt;
    int* poutHeight = (int*)joutHeightInt;

    unsigned char* pSrcI420 = (unsigned char*)jsrcI420Byte;
    unsigned char* pOutData = (unsigned char*)joutDataByte;

    YuvEngine* pYuvWater = reinterpret_cast<YuvEngine*> (jcPtr);
    pYuvWater->I420ClockWiseRotate90(pSrcI420,(int)jsrcWidth,(int)jsrcHeight,pOutData,poutWidth, poutHeight);

    LOGD("%s: outWidth: %d,outHeight:%d",__FUNCTION__,*poutWidth,*poutHeight);
    env->ReleaseIntArrayElements(joutWidth, joutWidthInt, 0);
    env->ReleaseIntArrayElements(joutHeight, joutHeightInt, 0);
    env->ReleaseByteArrayElements(jsrcI420, jsrcI420Byte, 0);
    env->ReleaseByteArrayElements(joutData, joutDataByte, 0);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSYuvOperateJni_Nv21ClockWiseRotate90(JNIEnv *env, jclass jcls __unused, jlong jcPtr, jbyteArray jsrcNv21, jint jsrcWidth,
                                                                   jint jsrcHeight, jbyteArray joutData, jintArray joutWidth, jintArray joutHeight) {
    jbyte* jsrcNv21Byte = env->GetByteArrayElements(jsrcNv21, NULL);
    jbyte* joutDataByte = env->GetByteArrayElements(joutData, NULL);

    jint* joutWidthInt = env->GetIntArrayElements(joutWidth, NULL);
    jint* joutHeightInt = env->GetIntArrayElements(joutHeight, NULL);

    int* poutWidth = (int*)joutWidthInt;
    int* poutHeight = (int*)joutHeightInt;

    unsigned char* pSrcNv21 = (unsigned char*)jsrcNv21Byte;
    unsigned char* pOutData = (unsigned char*)joutDataByte;

    YuvEngine* pYuvWater = reinterpret_cast<YuvEngine*> (jcPtr);
    pYuvWater->Nv21ClockWiseRotate90(pSrcNv21,(int)jsrcWidth,(int)jsrcHeight,pOutData,poutWidth, poutHeight);

    LOGD("%s: outWidth: %d,outHeight:%d",__FUNCTION__,*poutWidth,*poutHeight);
    env->ReleaseIntArrayElements(joutWidth, joutWidthInt, 0);
    env->ReleaseIntArrayElements(joutHeight, joutHeightInt, 0);
    env->ReleaseByteArrayElements(jsrcNv21, jsrcNv21Byte, 0);
    env->ReleaseByteArrayElements(joutData, joutDataByte, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSYuvOperateJni_Nv21ToYV12 (JNIEnv * env, jclass jcls __unused, jlong jcPtr, jbyteArray jNv21Data,
                                                         jbyteArray jYv12Data, jint jwidth, jint jheight) {
    jbyte* jNv21 = env->GetByteArrayElements(jNv21Data, NULL);
    jbyte* jYv12 = env->GetByteArrayElements(jYv12Data, NULL);

    unsigned char* pNv21 = (unsigned char*)jNv21;
    unsigned char* pYv12 = (unsigned char*)jYv12;

    YuvEngine* pYuvWater = reinterpret_cast<YuvEngine*> (jcPtr);
    pYuvWater->Nv21ToYv12(pNv21,pYv12,(int)jwidth, (int)jheight);
    env->ReleaseByteArrayElements(jNv21Data, jNv21, 0);
    env->ReleaseByteArrayElements(jYv12Data, jYv12, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSYuvOperateJni_Yv12ClockWiseRotate90 (JNIEnv *env, jclass jcls __unused, jlong jcPtr, jbyteArray jsrcYv12,
                                                                    jint jsrcWidth, jint jsrcHeight, jbyteArray joutData, jintArray joutWidth, jintArray joutHeight) {
    jbyte* jsrcYv12Byte = env->GetByteArrayElements(jsrcYv12, NULL);
    jbyte* joutDataByte = env->GetByteArrayElements(joutData, NULL);

    jint* joutWidthInt = env->GetIntArrayElements(joutWidth, NULL);
    jint* joutHeightInt = env->GetIntArrayElements(joutHeight, NULL);

    int* poutWidth = (int*)joutWidthInt;
    int* poutHeight = (int*)joutHeightInt;

    unsigned char* pSrcYv12 = (unsigned char*)jsrcYv12Byte;
    unsigned char* pOutData = (unsigned char*)joutDataByte;

    YuvEngine* pYuvWater = reinterpret_cast<YuvEngine*> (jcPtr);
    pYuvWater->Yv12ClockWiseRotate90(pSrcYv12,(int)jsrcWidth,(int)jsrcHeight,pOutData,poutWidth, poutHeight);

    LOGD("%s: outWidth: %d,outHeight:%d",__FUNCTION__,*poutWidth,*poutHeight);
    env->ReleaseIntArrayElements(joutWidth, joutWidthInt, 0);
    env->ReleaseIntArrayElements(joutHeight, joutHeightInt, 0);
    env->ReleaseByteArrayElements(jsrcYv12, jsrcYv12Byte, 0);
    env->ReleaseByteArrayElements(joutData, joutDataByte, 0);
}


//得到JavaVM
JavaVM *vm=0;
MSPlayer *msPlayer=0;
pthread_mutex_t mutex=PTHREAD_MUTEX_INITIALIZER;
ANativeWindow *window=0;

jint JNI_OnLoad(JavaVM *vm,void *args){
    //初始化JavaVM
    ::vm=vm;
    return JNI_VERSION_1_6;
}

// 函数指针 实现  渲染工作
void renderFrame(uint8_t * src_data, int width, int height, int src_lineSize) {
    pthread_mutex_lock(&mutex);
    if (!window) {
        pthread_mutex_unlock(&mutex); // 出现了问题后，必须考虑到，释放锁，怕出现死锁问题
    }

    // 设置窗口的大小，各个属性
    ANativeWindow_setBuffersGeometry(window, width, height, WINDOW_FORMAT_RGBA_8888);

    // 他自己有个缓冲区 buffer
    ANativeWindow_Buffer window_buffer; // 目前他是指针吗？

    // 如果我在渲染的时候，是被锁住的，那我就无法渲染，我需要释放 ，防止出现死锁
    if (ANativeWindow_lock(window, &window_buffer, 0)) {
        ANativeWindow_release(window);
        window = 0;

        pthread_mutex_unlock(&mutex); // 解锁，怕出现死锁
        return;
    }

    //开始真正渲染，因为window没有被锁住了，就可以把 rgba数据 ---> 字节对齐 渲染
    // 填充window_buffer  画面就出来了
    uint8_t *dst_data = static_cast<uint8_t *>(window_buffer.bits);
    int dst_linesize = window_buffer.stride * 4;

    for (int i = 0; i < window_buffer.height; ++i) { // 一行一行显示
        /*memcpy(dst_data + i * 1704, src_data + i * 1704, 1704); // 花屏*/
        /*花屏原因：1704 无法 64字节对齐，所以花屏*/

        memcpy(dst_data + i * dst_linesize, src_data + i * src_lineSize, dst_linesize); // OK的
    }

    // 数据刷新
    ANativeWindow_unlockAndPost(window); // 解锁后 并且刷新 window_buffer的数据显示画面

    pthread_mutex_unlock(&mutex);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSPlayer_prepareNative(JNIEnv *env, jobject thiz,
                                                    jstring data_source) {
    const char * data_source_ = env->GetStringUTFChars(data_source, 0);
    auto *helper = new JniUtil(vm, env, thiz); // C++子线程回调 ， C++主线程回调
    msPlayer = new MSPlayer(data_source_, helper);
    msPlayer->setRenderCallback(renderFrame);
    msPlayer->prepare();
    msPlayer->setJNICallback(helper);
    env->ReleaseStringUTFChars(data_source, data_source_);
}



extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSPlayer_startNative(JNIEnv *env, jobject thiz) {
    if (msPlayer){
        msPlayer->start();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSPlayer_stopNative(JNIEnv *env, jobject thiz) {
    if (msPlayer) {
        msPlayer->stop();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSPlayer_releaseNative(JNIEnv *env, jobject thiz) {
    pthread_mutex_lock(&mutex);
    // 先释放之前的显示窗口
    if (window) {
        ANativeWindow_release(window);
        window = nullptr;
    }

    pthread_mutex_unlock(&mutex);

    // 释放工作
    DELETE(msPlayer);
    DELETE(vm);
    DELETE(window);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSPlayer_setSurfaceNative(JNIEnv *env, jobject thiz, jobject surface) {
    pthread_mutex_lock(&mutex);
    // 先释放之前的显示窗口
    if (window) {
        ANativeWindow_release(window);
        window = 0;
    }

    // 创建新的窗口用于视频显示
    window = ANativeWindow_fromSurface(env, surface);

    pthread_mutex_unlock(&mutex);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_meishe_msmediacodec_MSPlayer_getDurationNative(JNIEnv *env, jobject thiz) {
    if (msPlayer){
        return msPlayer->getDuration();
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msmediacodec_MSPlayer_seekNative(JNIEnv *env, jobject thiz, jint play_progress) {
    if (msPlayer) {
        msPlayer->seek(play_progress);
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_meishe_msmediacodec_MSPlayer_getPlayState(JNIEnv *env, jobject thiz) {
    if (msPlayer) {
        return msPlayer->getPlayState();
    }
    return false;
}