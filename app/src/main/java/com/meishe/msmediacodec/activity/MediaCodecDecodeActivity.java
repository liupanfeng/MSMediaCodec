package com.meishe.msmediacodec.activity;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 硬件解码播放视频
 */
public class MediaCodecDecodeActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private String mFilePath="/sdcard/DCIM/189017886849403.mp4";
    private MediaExtractor mMediaExtractor;
    private MediaCodec mMediaCodec;
//    private DecodeThread mDecodeThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_media_codec_decode);
        SurfaceView surfaceView=new SurfaceView(this);
        /*不自己维护缓冲区，等待屏幕的渲染引擎 将内容推送到用户前面*/
        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.getHolder().addCallback(this);
        setContentView(surfaceView);
    }



    /*这个是CallBack的回调函数*/

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
//        if (mDecodeThread ==null){
//            mDecodeThread =new DecodeThread(holder.getSurface());
//            mDecodeThread.start();
//        }

        startSyncPlay(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
//        if (mDecodeThread !=null){
//            mDecodeThread.interrupt();   //停止线程的正确姿势
//        }
    }



    public void startSyncPlay(Surface surface){
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(mFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int trackCount = mMediaExtractor.getTrackCount();
        long startMs=System.currentTimeMillis();
        //从媒体提取器中拿到了 MIME 以及MediaFormat   通过MIME 创建的硬件解码器   通过MediaFormat配置的硬件解码器
        for (int i = 0; i < trackCount; i++) {
            MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
            Log.d("lpf","trackFormat is "+trackFormat);
            String mime=trackFormat.getString(MediaFormat.KEY_MIME);
            Log.d("lpf","mime is "+mime);

            if (mime.startsWith("video/")) {
                mMediaExtractor.selectTrack(i);
                try {
                    mMediaCodec=MediaCodec.createDecoderByType(mime);
                    if (mMediaCodec == null){
                        return;
                    }
                    //这样配置之后，解码之后的数据就会 直接显示在mSurface 上边  这里是核心点
                    mMediaCodec.configure(trackFormat,surface,null,0);
                    mMediaCodec.setCallback(new MediaCodec.Callback() {
                        @Override
                        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                            ByteBuffer inputBuffer = codec.getInputBuffer(index);
                            int sampleSize=mMediaExtractor.readSampleData(inputBuffer,0);
                            if (sampleSize>0) {
                                codec.queueInputBuffer(index,0,sampleSize,mMediaExtractor.getSampleTime(),0);
                                mMediaExtractor.advance();  //下一帧数据
                            }else {
                                codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            }
                        }

                        @Override
                        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                                Log.d("lpf","outputBuffer BUFFER_FLAG_END_OF_STREAM");
                                codec.stop();
                                codec.release();// 释放组件
                                mMediaExtractor.release();
                                return;
                            }
                            while (info.presentationTimeUs/1000>System.currentTimeMillis()-startMs){
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            codec.releaseOutputBuffer(index,true);  //释放缓冲区，并交给Surface 进行播放
                        }

                        @Override
                        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                        }

                        @Override
                        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                        }
                    });
                    //调用Start 如果没有异常信息，表示成功构建组件
                    mMediaCodec.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }



//
//    private class DecodeThread extends Thread{
//
//        private MediaExtractor mMediaExtractor;
//        private MediaCodec mMediaCodec;
//        private Surface mSurface;
//
//        /*通过构造方法将surface传递进来*/
//        public DecodeThread(Surface surface){
//            mSurface = surface;
//        }
//
//        @Override
//        public void run() {
//            super.run();
//            mMediaExtractor = new MediaExtractor();
//            try {
//                mMediaExtractor.setDataSource(mFilePath);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            int trackCount = mMediaExtractor.getTrackCount();
//            //从媒体提取器中拿到了 MIME 以及MediaFormat   通过MIME 创建的硬件解码器   通过MediaFormat配置的硬件解码器
//            for (int i = 0; i < trackCount; i++) {
//                MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
//                Log.d("lpf","trackFormat is "+trackFormat);
//                String mime=trackFormat.getString(MediaFormat.KEY_MIME);
//                Log.d("lpf","mime is "+mime);
//                if (mime.startsWith("video/")){
//                    mMediaExtractor.selectTrack(i);
//                    try {
//                        mMediaCodec=MediaCodec.createDecoderByType(mime);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    //这样配置之后，解码之后的数据就会 直接显示在mSurface 上边  这里是核心点
//                    mMediaCodec.configure(trackFormat,mSurface,null,0);
//                    break;
//                }
//            }
//            if (mMediaCodec == null){
//                return;
//            }
//            //调用Start 如果没有异常信息，表示成功构建组件
//            mMediaCodec.start();
//            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
//            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
//            //每个Buffer的元数据包括具体的范围以及偏移大小，以及数据中心相关解码的buffer
//
//            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
//            boolean isEOF=false;
//            long startMs=System.currentTimeMillis();
//
//            while (!Thread.interrupted()){//只要线程不中断
//
//                if (!isEOF){
//                    //返回有效的buffer 索引，如果没有相关的Buffer可用，就返回-1
//                    //传入的timeoutUs为0表示立即返回
////                    如果数据的buffer可用，将无限期等待timeUs的单位是纳秒
//                    int index =mMediaCodec.dequeueInputBuffer(10000);
//                    if (index >= 0){
//                        ByteBuffer byteBuffer=inputBuffers[index];
//                        Log.d("lpf","bytebuffer is "+byteBuffer);
//                        int sampleSize=mMediaExtractor.readSampleData(byteBuffer,0);
//                        Log.d("lpf","sampleSize is "+sampleSize);
//                        if (sampleSize < 0){
//                            Log.d("lpf","inputBuffer is BUFFER_FLAG_END_OF_STREAMING");
//                            mMediaCodec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                            isEOF=true;
//                        }else{
//                            mMediaCodec.queueInputBuffer(index,0,sampleSize,mMediaExtractor.getSampleTime(),0);
//                            mMediaExtractor.advance();  //下一帧数据
//                        }
//                    }
//                }
//
//                int outIndex=mMediaCodec.dequeueOutputBuffer(info,100000);
//                switch (outIndex){
//                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
//                        //当buffer变化时，必须重新指向新的buffer
//                        outputBuffers=mMediaCodec.getOutputBuffers();
//                        break;
//                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
//                        //当Buffer的封装格式发生变化的时候，需重新指向新的buffer格式
//                        Log.d("lpf","output  buffer changed");
//                            break;
//                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        //dequeueOutputBuffer 超时的时候会到这个case
//                        Log.d("lpf","dequeueOutputBuffer timeout");
//                        break;
//                    default:
//                        ByteBuffer buffer=outputBuffers[outIndex];
//                        //由于配置的时候 将Surface 传进去了  所以解码的时候 将数据直接交给了Surface进行显示了
//                        //使用简单的时钟的方式保持视频的fps（每秒显示的帧数），不然视频会播放的比较快
//                        Log.d("lpf","解码之后的 buffer数据="+buffer);
//                        while (info.presentationTimeUs/1000>System.currentTimeMillis()-startMs){
//                            try {
//                                Thread.sleep(10);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        mMediaCodec.releaseOutputBuffer(outIndex,true);
//                        break;
//                }
//
//                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
//                    Log.d("lpf","outputBuffer BUFFER_FLAG_END_OF_STREAM");
//                    break;
//                }
//            }
//
//            mMediaCodec.stop();
//            mMediaCodec.release();// 释放组件
//            mMediaExtractor.release();
//
//        }
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaCodec!=null){
            mMediaCodec.stop();
            mMediaCodec.release();// 释放组件
        }

        if (mMediaExtractor!=null){
            mMediaExtractor.release();
        }
    }
}