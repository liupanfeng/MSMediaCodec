package com.meishe.msmediacodec;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/14 下午2:45
 * @Description : 音频 视频 合成器
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */
public class MSMediaMuxer {
    private final static String TAG = "MSMediaMuxer";
    /**
     * 视频轨道
     */
    public static final int TRACK_VIDEO = 0;
    /**
     * 音频轨道
     */
    public static final int TRACK_AUDIO = 1;

    private final Object lock = new Object();
    /**
     * 媒体混合器
     */
    private MediaMuxer mMediaMuxer;
    /**
     * 缓冲传输过来的数据
     */
    private LinkedBlockingQueue<MuxerData> mMuxerDatas = new LinkedBlockingQueue<>();
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private boolean mIsVideoAdd;
    private boolean mIsAudioAdd;
    /**
     * 视频通道
     */
    private MSVideoChannel mVideoChannel;
    /**
     * 音频通道
     */
    private MSAudioChannel mAudioChannel;
    private MSMediaCodec mAVEncoder;
    private boolean mIsMediaMuxerStart;
    private volatile boolean mLoop;
    private Disposable mSubscribe;

    private MSMediaMuxer() {
    }

    public static MSMediaMuxer getInstance() {
        return Helper.instance;
    }

    private static class Helper{
        private static MSMediaMuxer instance=new MSMediaMuxer();
    }

    /**
     * 初始化混合器
     * @param outfile
     */
    public void initMediaMuxer(String outfile) {
        if (mLoop) {
            throw new RuntimeException(" MediaMuxer线程已经启动");
        }
        try {
            Log.d(TAG, " init MediaMuxer start");
            mMediaMuxer = new MediaMuxer(outfile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            Log.d(TAG, " init MediaMuxer end");
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, " init MediaMuxer error: "+e.toString());
        }
        mVideoChannel = MSVideoChannel.getInstance();
        mAudioChannel = MSAudioChannel.getInstance();
        mAVEncoder = MSMediaCodec.getInstance();
        setListener();
        mSubscribe = Observable.just(1).observeOn(Schedulers.io()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                /*混合器未开启*/
                synchronized (lock) {
                    try {
                        Log.d(TAG, "媒体混合器等待开启");
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while (mLoop && !Thread.interrupted()) {
                    try {
                        MuxerData data = mMuxerDatas.take();
                        int track = -1;
                        if (data.trackIndex == TRACK_VIDEO) {
                            track = mVideoTrackIndex;
                        } else if(data.trackIndex == TRACK_AUDIO){
                            track = mAudioTrackIndex;
                        }
                        Log.d(TAG, " track: "+track+"    写入混合数据大小 " + data.bufferInfo.size);
                        //添加数据
                        mMediaMuxer.writeSampleData(track, data.byteBuf, data.bufferInfo);
                    } catch (InterruptedException e) {
                        Log.e(TAG, " 写入混合数据失败!" + e.toString());
                        e.printStackTrace();
                    }
                }
                mMuxerDatas.clear();
                stopMediaMuxer();
                Log.d(TAG, " 媒体混合器退出...");
            }
        });

        mLoop = true;
    }

    /**
     * 初始化视频编码器
     */
    public void initVideoEncoder(int width,int height,int fps){
        mAVEncoder.initVideoEncoder(width,height,fps);
    }

    /**
     * 初始化音频编码器
     */
    public void initAudioEncoder(){
        mAVEncoder.initAudioEncoder(mAudioChannel.getSampleRate(),
                mAudioChannel.getPcmFormat(),
                mAudioChannel.getChannelCount());
    }

    /**
     * 开始音频采集
     */
    public void startAudioGather() {
        mAudioChannel.prepareAudioRecord();
        mAudioChannel.startRecord();
    }

    /**
     * 停止音频采集
     */
    public void stopAudioGather() {
        mAudioChannel.stopRecord();
    }

    /**
     * 释放
     */
    public void release() {
        mAudioChannel.release();
        mAudioChannel = null;
        mLoop = false;
        if (mSubscribe!=null){
            mSubscribe.dispose();
        }
        mVideoChannel = null;
        mAVEncoder = null;
    }

    private void startMediaMuxer() {
        if (mIsMediaMuxerStart){
            return;
        }
        synchronized (lock) {
            if (mIsAudioAdd && mIsVideoAdd) {
                Log.d(TAG, "启动媒体混合器 ");
                mMediaMuxer.start();
                mIsMediaMuxerStart = true;
                lock.notify();
            }
        }
    }

    private void stopMediaMuxer() {
        if (!mIsMediaMuxerStart){
            return;
        }
        mMediaMuxer.stop();
        mMediaMuxer.release();
        mIsMediaMuxerStart = false;
        mIsAudioAdd = false;
        mIsVideoAdd = false;
        Log.d(TAG, " 停止媒体混合器 ");
    }

    /**
     * 开始编码
     */
    public void startEncoder() {
        mAVEncoder.start();
    }

    /**
     * 停止编码
     */
    public void stopEncoder() {
        mAVEncoder.stop();
    }

    private void setListener() {
        mVideoChannel.setCallback(new MSVideoChannel.Callback() {
            @Override
            public void videoData(byte[] data) {
                if (mAVEncoder != null){
                    mAVEncoder.putVideoData(data);
                }
            }
        });

        mAudioChannel.setCallback(new MSAudioChannel.OnAudioDataCallback() {
            @Override
            public void audioData(byte[] data) {
                if (mAVEncoder != null){
                    mAVEncoder.putAudioData(data);
                }
            }
        });

        mAVEncoder.setCallback(new MSMediaCodec.Callback() {
            @Override
            public void outputVideoFrame(final int trackIndex, final ByteBuffer outBuf, final MediaCodec.BufferInfo bufferInfo) {
                try {
                    Log.d(TAG, "视频通道压缩后的数据，加入混合器队列");
                    mMuxerDatas.put(new MuxerData(
                            trackIndex, outBuf, bufferInfo));
                } catch (InterruptedException e) {
                    Log.e(TAG, " 视频通道压缩后的数据，加入混合器队列 error: " + e.toString());
                    e.printStackTrace();
                }
            }

            @Override
            public void outputAudioFrame(final int trackIndex,final ByteBuffer outBuf,final MediaCodec.BufferInfo bufferInfo) {
                try {
                    Log.d(TAG, "音频通道的数据加入混合器");
                    mMuxerDatas.put(new MuxerData(
                            trackIndex, outBuf, bufferInfo));
                } catch (InterruptedException e) {
                    Log.e(TAG, " 音频通道的数据加入混合器 error: "+e.toString());
                    e.printStackTrace();
                }
            }

            @Override
            public void outMediaFormat(final int trackIndex, MediaFormat mediaFormat) {
                if (trackIndex == TRACK_AUDIO) {
                    Log.d(TAG, " outMediaFormat mediaMuxer: " + (mMediaMuxer != null));
                    if (mMediaMuxer != null) {
                        mAudioTrackIndex = mMediaMuxer.addTrack(mediaFormat);
                        mIsAudioAdd = true;
                    }
                } else if (trackIndex == TRACK_VIDEO) {
                    Log.d(TAG, " outMediaFormat mediaMuxer: " + (mMediaMuxer != null));
                    if (mMediaMuxer != null) {
                        mVideoTrackIndex = mMediaMuxer.addTrack(mediaFormat);
                        mIsVideoAdd = true;
                    }
                }
                startMediaMuxer();
            }
        });
    }

    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {
        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }

}
