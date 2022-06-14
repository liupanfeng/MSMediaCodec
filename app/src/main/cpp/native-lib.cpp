#include <jni.h>
#include <string>

#include "yuvengine.h"

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