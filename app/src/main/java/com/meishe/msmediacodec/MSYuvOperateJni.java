package com.meishe.msmediacodec;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/14 下午2:32
 * @Description : JNI 操作桥梁
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */
public class MSYuvOperateJni {


    public native static long startYuvEngine();

    public native static void Nv21ToNv12(long cPtr,byte[] pNv21,byte[] pNv12,int width,int height);

    public native static void Nv12ClockWiseRotate90(long cPtr,byte[] pNv12,int srcWidth,int srcHeight,byte[] outData,int[] outWidth,int[] outHeight);

    public native static void stopYuvEngine(long cPtr);


    public native static void Nv21ToI420(long cPtr,byte[] pNv21,byte[] pI420,int width,int height);

    /**
     * I420(YUV420P)图像顺时针旋转90度
     * @param cPtr
     * @param pI420
     * @param srcWidth
     * @param srcHeight
     * @param outData
     * @param outWidth
     * @param outHeight
     */
    public static final native void I420ClockWiseRotate90(long cPtr,byte[] pI420, int srcWidth,int srcHeight,byte[] outData, int[] outWidth,int[] outHeight);

    public native static void Nv21ClockWiseRotate90(long cPtr,byte[] pNv21,int srcWidth,int srcHeight,byte[] outData,int[] outWidth,int[] outHeight);


    public native static void Nv21ToYV12(long cPtr,byte[] pNv21,byte[] pYv12,int width,int height);

    /**
     * YV12图像顺时针旋转90度
     * @param cPtr
     * @param pYv12
     * @param srcWidth
     * @param srcHeight
     * @param outData
     * @param outWidth
     * @param outHeight
     */
    public static final native void Yv12ClockWiseRotate90(long cPtr,byte[] pYv12, int srcWidth,int srcHeight,byte[] outData, int[] outWidth,int[] outHeight);
}
