package com.meishe.msmediacodec.helper;

import com.meishe.msmediacodec.MSYuvOperateJni;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/14 下午2:34
 * @Description : 引擎包装类
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */
public class MSYuvHelper {

    private static MSYuvHelper instance;
    /**
     * c++ YuvEngine 对象的指针
     */
    private long mCPtr;

    public MSYuvHelper() {
        mCPtr = 0;
    }


    public static MSYuvHelper getInstance(){
       if (instance==null){
           instance=new MSYuvHelper();
       }
       return instance;
    }

    /**
     * 拿到yuv引擎 方法句柄
     */
    public void startYuvEngine() {
        mCPtr = MSYuvOperateJni.startYuvEngine();
    }

    /**
     * NV21 -> NV12
     */
    public void Nv21ToNv12(byte[] pNv21,byte[] pNv12,int width,int height){
        if (mCPtr != 0) {
            MSYuvOperateJni.Nv21ToNv12(mCPtr,pNv21,pNv12, width, height);
        }
    }

    /**
     * Nv12顺时针旋转90度
     * pNv12     原nv12数据
     * srcWidth  原nv12对应的宽
     * srcHeight 原nv12对应的高
     * outData   旋转后的nv12数据
     * outWidth  旋转后对应的宽
     * outHeight 旋转后对应的高
     */
    public void Nv12ClockWiseRotate90(byte[] pNv12,int srcWidth,int srcHeight,byte[] outData,int[] outWidth,int[] outHeight){
        if (mCPtr != 0) {
            MSYuvOperateJni.Nv12ClockWiseRotate90(mCPtr, pNv12, srcWidth, srcHeight,outData,outWidth,outHeight);
        }
    }

    /**
     * YV12顺时针旋转90度
     * pYv12     原Yv12数据
     * srcWidth  原Yv12对应的宽
     * srcHeight 原Yv12对应的高
     * outData   旋转后的Yv12数据
     * outWidth  旋转后对应的宽
     * outHeight 旋转后对应的高
     */
    public void Yv12ClockWiseRotate90(byte[] pYv12, int srcWidth,int srcHeight,byte[] outData, int[] outWidth,int[] outHeight){
        if (mCPtr != 0) {
            MSYuvOperateJni.Yv12ClockWiseRotate90(mCPtr, pYv12, srcWidth, srcHeight,outData,outWidth,outHeight);
        }
    }

    /**
     * NV21 -> I420
     */
    public void Nv21ToI420(byte[] pNv21,byte[] pI420,int width,int height) {
        if (mCPtr != 0) {
            MSYuvOperateJni.Nv21ToI420(mCPtr, pNv21,pI420, width,height);
        }
    }

    /**
     * NV21 -> YV12
     */
    public void Nv21ToYv12(byte[] pNv21,byte[] pYv12,int width,int height) {
        if (mCPtr != 0) {
            MSYuvOperateJni.Nv21ToYV12(mCPtr, pNv21,pYv12, width,height);
        }
    }

    /**
     * Nv21顺时针旋转90度
     * pNv21     原nv21数据
     * srcWidth  原nv21对应的宽
     * srcHeight 原nv21对应的高
     * outData   旋转后的nv21数据
     * outWidth  旋转后对应的宽
     * outHeight 旋转后对应的高
     */
    public void Nv21ClockWiseRotate90(byte[] pNv21,int srcWidth,int srcHeight,byte[] outData,int[] outWidth,int[] outHeight){
        if (mCPtr != 0) {
            MSYuvOperateJni.Nv21ClockWiseRotate90(mCPtr, pNv21, srcWidth, srcHeight,outData,outWidth,outHeight);
        }
    }



    /**
     * I420顺时针旋转90度
     * pI420     原I420数据
     * srcWidth  原I420对应的宽
     * srcHeight 原I420对应的高
     * outData   旋转后的I420数据
     * outWidth  旋转后对应的宽
     * outHeight 旋转后对应的高
     */
    public void I420ClockWiseRotate90(byte[] pI420, int srcWidth,int srcHeight,byte[] outData, int[] outWidth,int[] outHeight){
        if (mCPtr != 0) {
            MSYuvOperateJni.I420ClockWiseRotate90(mCPtr, pI420, srcWidth, srcHeight,outData,outWidth,outHeight);
        }
    }

    /**
     * 停止yuv引擎
     */
    public void stopYuvEngine() {
        if (mCPtr != 0) {
            MSYuvOperateJni.stopYuvEngine(mCPtr);
        }
    }


}
