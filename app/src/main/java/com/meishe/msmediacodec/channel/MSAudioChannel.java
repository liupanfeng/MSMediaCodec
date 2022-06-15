package com.meishe.msmediacodec.channel;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/14 下午2:42
 * @Description : 音频通道
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */
public class MSAudioChannel {
    private static final String TAG = "MSAudioChannel";
    /**
     * 音频录制借助AudioRecord
     */
    private AudioRecord mAudioRecord;
    private int mChannelCount;
    private int mSampleRate;
    private int mPcmFormat;
    /**
     * 音频数据buffer
     */
    private byte[] mAudioBuf;

    private volatile boolean isRecording = false;

    private OnAudioDataCallback mCallback;
    private Disposable mSubscribe;

    public static MSAudioChannel getInstance() {
        return Helper.instance;
    }

    private static class Helper {
        private static MSAudioChannel instance = new MSAudioChannel();
    }

    private MSAudioChannel() {}

    public void prepareAudioRecord() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        /*音频采样率，44100是目前的标准，但是某些设备仍然支持22050,16000,11025,8000,4000*/
        int[] sampleRates = {44100, 22050, 16000, 11025, 8000, 4000};
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            for (int sampleRate : sampleRates) {
                /*stereo 立体声,mono单声道*/
                int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
                /*根据 采样率 声道 位深 计算最小的buffer大小*/
                final int minBufferSize = 2 * AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
                /*实例化AudioRecord
                * 1.采集设备
                * 2.采样率
                * 3.声道
                * 4.位深
                * 5.最小的缓存大小
                * */
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRate, channelConfig,
                        AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
                /*判断是否是初始化状态*/
                if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    mAudioRecord = null;
                    Log.e(TAG, "initialized the mic failed");
                    continue;
                }

                mSampleRate = sampleRate;
                mChannelCount = channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO ? 2 : 1;
                mPcmFormat = 16;
                /*ByteBuffer分配内存的最大值为4096*/
                int buffSize = Math.min(4096, minBufferSize);
                mAudioBuf = new byte[buffSize];
                Log.d(TAG, "mSampleRate: " + mSampleRate +
                        "   mChannelCount: " + mChannelCount + "   minBufferSize: " + minBufferSize);
                break;
            }
        } catch (final Exception e) {
            Log.e(TAG, "AudioThread#run", e);
        }
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        if (isRecording) {
            return;
        }
        /*observeOn 控制后边执行的线程*/
         mSubscribe = Observable.just(1).observeOn(Schedulers.io()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.e("lpf","currentThread="+Thread.currentThread().getName());
                if (mAudioRecord != null) {
                    mAudioRecord.startRecording();
                }
                while (isRecording && !Thread.interrupted()) {
                    /*读取音频数据到buf*/
                    int size = mAudioRecord.read(mAudioBuf, 0, mAudioBuf.length);
                    if (size > 0) {
                        Log.d(TAG, "录音字节数:" + size);
                        if (mCallback != null) {
                            mCallback.audioData(mAudioBuf);
                        }
                    }
                }
                Log.d(TAG, "-----Audio录音线程退出-----");
            }
        });

        isRecording = true;
    }


    public int getChannelCount() {
        return mChannelCount;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getPcmFormat() {
        return mPcmFormat;
    }

    public void stopRecord() {
        Log.d(TAG, "---stopRecord---");
        if (mAudioRecord != null) {
            mAudioRecord.stop();
        }
        isRecording = false;
        if (mSubscribe != null) {
            mSubscribe.dispose();
        }
    }

    public void release() {
        if (mAudioRecord != null) {
            mAudioRecord.release();
        }
        mAudioRecord = null;
        mCallback = null;
        Helper.instance = null;
        mSubscribe=null;
    }

    public void setCallback(OnAudioDataCallback callback) {
        this.mCallback = callback;
    }

    public interface OnAudioDataCallback {
        /**
         * 回调音频录制数据
         * @param data
         */
        public void audioData(byte[] data);
    }
}
