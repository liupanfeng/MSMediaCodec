package com.meishe.msmediacodec;

import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

/**
 * @author : lpf
 * @FileName: MSPlayer
 * @Date: 2022/6/1 15:07
 * @Description:
 */
public class MSPlayer implements SurfaceHolder.Callback{


    // 打不开视频
    private static final int FFMPEG_CAN_NOT_OPEN_URL = 1;

    // 找不到流媒体
    private static final int FFMPEG_CAN_NOT_FIND_STREAMS = 2;

    // 找不到解码器
    private static final int FFMPEG_FIND_DECODER_FAIL = 3;

    // 无法根据解码器创建上下文
    private static final int FFMPEG_ALLOC_CODEC_CONTEXT_FAIL = 4;

    //  根据流信息 配置上下文参数失败
    private static final int FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL = 6;

    // 打开解码器失败
    private static final int FFMPEG_OPEN_DECODER_FAIL = 7;

    // 没有音视频
    private static final int FFMPEG_NOMEDIA = 8;

    private SurfaceHolder surfaceHolder;

    public MSPlayer(){}

    /*C++层准备情况的接口*/
    private OnPreparedListener onPreparedListener;

    /**
     * 设置准备OK的监听方法
     */
    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    /* 媒体源（文件路径， 直播地址rtmp）*/
    private String dataSource;

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 播放前的 准备工作
     */
    public void prepare() {
        prepareNative(dataSource);
    }

    /**
     * 开始播放
     */
    public void start() {
        startNative();
    }

    /**
     * 停止播放
     */
    public void stop() {
        stopNative();
    }

    /**
     * 释放资源
     */
    public void release() {
        releaseNative();
    }

    /**
     * 给jni反射调用的
     */
    public void onPrepared() {
        if (onPreparedListener != null) {
            onPreparedListener.onPrepared();
        }
    }

    /**
     * set SurfaceView
     * @param surfaceView
     */
    public void setSurfaceView(SurfaceView surfaceView) {
        if (this.surfaceHolder != null) {
            surfaceHolder.removeCallback(this); // 清除上一次的
        }
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this); // 监听

        setSurfaceNative(surfaceHolder.getSurface());
    }



    /**
     * 准备OK的监听
     */
    public interface OnPreparedListener {
        void onPrepared();
    }


    /**
     * 给jni反射调用的 准备错误了
     */
    public void onError(int errorCode) {
        if (null != this.onErrorListener) {
            String msg = null;
            switch (errorCode) {
                case FFMPEG_CAN_NOT_OPEN_URL:
                    msg = "打不开视频";
                    break;
                case FFMPEG_CAN_NOT_FIND_STREAMS:
                    msg = "找不到流媒体";
                    break;
                case FFMPEG_FIND_DECODER_FAIL:
                    msg = "找不到解码器";
                    break;
                case FFMPEG_ALLOC_CODEC_CONTEXT_FAIL:
                    msg = "无法根据解码器创建上下文";
                    break;
                case FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL:
                    msg = "根据流信息 配置上下文参数失败";
                    break;
                case FFMPEG_OPEN_DECODER_FAIL:
                    msg = "打开解码器失败";
                    break;
                case FFMPEG_NOMEDIA:
                    msg = "没有音视频";
                    break;
            }
            onErrorListener.onError(msg);
        }
    }

    /**
     * jni层回调播放状态
     * @param state
     */
    public void onPlayStateChange(int state){
        if (mOnPlayStateCallback!=null){
            mOnPlayStateCallback.onPlayStateChange(state);
        }
    }

    public interface OnErrorListener {
        void onError(String errorCode);
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    private OnErrorListener onErrorListener;

    public int getDuration() {
        return getDurationNative();
    }

//---------------------播放状态------------------------------

    private OnPlayStateCallback mOnPlayStateCallback;

    public void setOnPlayStateCallback(OnPlayStateCallback onPlayStateCallback) {
        this.mOnPlayStateCallback = onPlayStateCallback;
    }

    public interface OnPlayStateCallback{
        /**
         * 播放状态回调
          * @param state 0：停止  1；播放
         */
      void onPlayStateChange(int state);

    }



//----------------------播放进度----------------------
    /**
     * 给jni反射调用的  准备成功
     */
    public void onProgress(int progress) {
        if (onProgressListener != null) {
            onProgressListener.onProgress(progress);
        }
    }

    private OnProgressListener onProgressListener;

    public interface OnProgressListener {

        void onProgress(int progress);
    }

    /**
     * 设置准备播放时进度的监听
     */
    public void setOnOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

//-------------------------------end------------------------


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        Log.e("lpf","surfaceCreated-------");
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.e("lpf","surfaceChanged-------");
        setSurfaceNative(surfaceHolder.getSurface());
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.e("lpf","surfaceDestroyed-------");
    }


    public void seek(int playProgress) {
        seekNative(playProgress);
    }


//----------------------------native---------------------------------
    private native void prepareNative(String dataSource);
    private native void startNative();
    private native void stopNative();
    private native void releaseNative();
    private native void setSurfaceNative(Surface surface);
    private native int getDurationNative() ;
    private native void seekNative(int playProgress);
    private native boolean getPlayState();

    //------------------------------end-------------------------

}
