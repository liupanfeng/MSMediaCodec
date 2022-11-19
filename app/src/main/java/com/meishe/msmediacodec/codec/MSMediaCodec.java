package com.meishe.msmediacodec.codec;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import com.meishe.msmediacodec.helper.MSYuvHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/*
 * 直播流的时间戳不论音频还是视频，在整体时间线上应当呈现递增趋势。如果时间戳计算方法是按照音视频分开计算，那么音频时戳和视频时戳可能并不是在一条时间线上，
 * 这就有可能出现音频时戳在某一个时间点比对应的视频时戳小， 在某一个时间点又跳变到比对应的视频时戳大，导致播放端无法对齐。
 * 目前采用的时间戳以发送视频SPS帧为基础，不区分音频流还是视频流，统一使用即将发送RTMP包的系统时间作为该包的时间戳。
 *
 * tbn is the time base in AVStream that has come from the container, I
 * think. It is used for all AVStream time stamps.
 * tbn 是来自容器的 AVStream 中的时基。它用于所有 AVStream 时间戳。

* tbc is the time base in AVCodecContext for the codec used for a
* particular stream. It is used for all AVCodecContext and related time
* stamps.
*
* tbc 是 AVCodecContext 中用于特定流的编解码器的时间基准。
* 它用于所有 AVCodecContext 和相关的时间戳。

* tbr is guessed from the video stream and is the value users want to see
* when they look for the video frame rate, except sometimes it is twice
* what one would expect because of field rate versus frame rate.
* tbr 是从视频流中猜测出来的，是用户在寻找视频帧速率时希望看到的值，
* 但有时由于场速率与帧速率的关系，它是人们预期的两倍。
*
 */
public class MSMediaCodec {
    private static final String TAG = "MSMediaCodec";
    /**
     * H.264 Advanced Video 硬件编码类型
     */
    private static final String VIDEO_MIME_TYPE = "video/avc";
    /**
     * I 帧 10 between
     */
    private static final int IFRAME_INTERVAL = 5;
    /**
     * 预览格式转换后的数据
     */
    private byte[] mYuvBuffer;
    /**
     * 旋转后的数据和分辨率
     */
    private byte[] mRotateYuvBuffer;

    private int[] mOutWidth = new int[1];
    private int[] mOutHeight = new int[1];

    /**
     * Camera预览分辨率和帧率
     */
    private int mWidth;
    private int mHeight;
    /**
     * 视频编码器
     */
    private MediaCodec mVideoMediaCodec;
    /**
     * 视频媒体格式
     */
    private MediaFormat mVideoFormat;
//    private int mColorFormat = 0;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private ArrayList<Integer> mSupportColorFormatList;
    private volatile boolean mVideoEncoderLoop = false;
    private volatile boolean mVideoEncoderEnd = false;
    /**
     * 视频阻塞队列
     */
    private LinkedBlockingQueue<byte[]> mVideoLinkedBlockQueue;

    /**
     * 音频硬件编码类型
     */
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    /**
     * 音频编码器
     */
    private MediaCodec mAudioMediaCodec;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaCodecInfo mAudioCodecInfo;
    private MediaFormat mAudioFormat;
    private volatile boolean mAudioEncoderLoop = false;
    private volatile boolean mEncoderEnd = false;
    /**
     * 音频阻塞队列
     */
    private LinkedBlockingQueue<byte[]> mAudioLinkedBlockQueue;


    private long mPresentationTimeUs;
    private final int TIMEOUT_USEC = 10000;
    private Callback mCallback;
    private Disposable mVideoSubscribe;
    private Disposable mAudioSubscribe;

    public static MSMediaCodec getInstance() {
        return Helper.instance;
    }
    private static class Helper{
        private static MSMediaCodec instance=new MSMediaCodec();
    }
    private MSMediaCodec() {}

    /**
     * 设置回调
     *
     * @param callback 回调
     */
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public interface Callback {
        void outputVideoFrame(final int trackIndex,final ByteBuffer outBuf,final MediaCodec.BufferInfo bufferInfo);
        void outputAudioFrame(final int trackIndex,final ByteBuffer outBuf,final MediaCodec.BufferInfo bufferInfo);
        void outMediaFormat(final int trackIndex,MediaFormat mediaFormat);
    }

    /**
     * 初始化音频编码器
     * @param sampleRate 采样率
     * @param pcmFormat  pcm数据位深 这里传的是 16
     * @param channelCount 音频通道数量
     */
    public void initAudioEncoder(int sampleRate, int pcmFormat,int channelCount){
        if (mAudioMediaCodec != null) {
            return;
        }
        mBufferInfo = new MediaCodec.BufferInfo();
        /*初始化阻塞队列*/
        mAudioLinkedBlockQueue = new LinkedBlockingQueue<>();
        /*根据类型选择一个音频编码器*/
        mAudioCodecInfo = selectCodec(AUDIO_MIME_TYPE);
        if (mAudioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for=" + AUDIO_MIME_TYPE);
            return;
        }
        /*根据 编码器类型、采样率、音频通道数量 创建音频媒体格式*/
        mAudioFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, sampleRate, channelCount);
        /*如果内容是 AAC 音频，则指定所需的配置文件*/
        mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        /*CHANNEL_IN_STEREO 立体声 双通道*/
        mAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO);
        /*采样率*位深*音频通道*/
        int bitRate = sampleRate * pcmFormat * channelCount;
        /*设置比特率*/
        mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        /*设置通道数量*/
        mAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
        /*设置采样率*/
        mAudioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);

        Log.d(TAG, "mAudioFormat=" + mAudioFormat.toString());
        Log.d(TAG, "selected codec=" + mAudioCodecInfo.getName());
        try {
            mAudioMediaCodec = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("createEncoderByType error", e);
        }
        Log.d(TAG, String.format("编码器:%s创建完成", mAudioMediaCodec.getName()));
    }

    /**
     * 初始化视频编码器
     * @param width 宽
     * @param height 高
     * @param fps 帧率
     */
    public void initVideoEncoder(int width, int height,int fps) {
        if (mVideoMediaCodec != null) {  //如果已经初始化过了，就不用再初始化了
            return;
        }
        this.mWidth = width;
        this.mHeight = height;  //得到宽 高
        /*初始化 视频阻塞队列*/
        mVideoLinkedBlockQueue = new LinkedBlockingQueue<>();
        /*颜色格式列表*/
        mSupportColorFormatList = new ArrayList<>();
        mRotateYuvBuffer = new byte[this.mWidth * this.mHeight * 3 / 2];   //视频buffer的大小是  宽*高*1.5  yuv420
        mYuvBuffer = new byte[this.mWidth * this.mHeight * 3 / 2];
        Log.d(TAG, "mWidth: "+mWidth+"  mHeight: "+mHeight);

        mVideoBufferInfo = new MediaCodec.BufferInfo();
        /*选择系统用于编码H264的编码器信息*/
        MediaCodecInfo codecInfo = selectCodec(VIDEO_MIME_TYPE);
        if (codecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + VIDEO_MIME_TYPE);
            return;
        }
        /*根据MIME格式,选择颜色格式*/
        selectColorFormat(codecInfo, VIDEO_MIME_TYPE);

//        for (int i = 0; i < mSupportColorFormatList.size(); i++) {
//            if (isRecognizedFormat(mSupportColorFormatList.get(i))) {
//                mColorFormat = mSupportColorFormatList.get(i);
//                break;
//            }
//        }

//        if(mColorFormat == 0){
//            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName()
//                            + " / " + VIDEO_MIME_TYPE);
//            return;
//        }

        /*
        *根据MIME创建MediaFormat
        * sensor出来的是逆时针旋转90度的数据，hal层没有做旋转导致APP显示和编码需要自己做顺时针旋转90,这样看到的图像才是正常的
        * */
        mVideoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE,
                this.mHeight, this.mWidth);
        int bitrate = (mWidth * mHeight * 3 / 2) * 8 * fps;
        /*设置比特率,将编码比特率值设为bitrate*/
        mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        /*设置帧率,将编码帧率设为Camera实际帧率fps*/
        mVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
//        /*设置颜色格式*/
//        mVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        //设置关键帧的时间
        mVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

//        Log.d(TAG,"mColorFormat"+mColorFormat);
        Log.d(TAG, "videoFormat: " + mVideoFormat.toString());

        try {
            /*创建一个MediaCodec*/
            mVideoMediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("createByCodecName error=", e);
        }
        Log.d(TAG, String.format("MediaCodec:%s创建完成", mVideoMediaCodec.getName()));
    }

    /**
     * 选择一个颜色格式
     * @param codecInfo
     * @param mimeType
     */
    private void selectColorFormat(MediaCodecInfo codecInfo,
                                        String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        mSupportColorFormatList.clear();
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            Log.d(TAG, "color format: " + colorFormat);
            mSupportColorFormatList.add(colorFormat);
        }
    }

    /**
     * 是否是认可的颜色格式
     * @param colorFormat
     * @return
     */
    private boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            /*对应Camera预览格式I420(YV21/YUV420P)*/
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                /*对应Camera预览格式NV12*/
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                /*对应Camera预览格式NV21*/
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                /*对应Camera预览格式YV12*/
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:{
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * 根据类型选择一个编码器
     * @param mimeType
     * @return
     */
    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * 开始编码
     */
    public void start() {
        startAudioEncode();
        startVideoEncode();
    }

    /**
     * 停止编码
     */
    public void stop() {
        stopAudioEncode();
        stopVideoEncode();
    }

    /**
     * 开始视频编码
     */
    private void startVideoEncode(){
        if (mVideoMediaCodec == null) {
            throw new RuntimeException("pls init mVideoMediaCodec");
        }

        if (mVideoEncoderLoop) {
            throw new RuntimeException("mVideoEncoderLoop need stop first");
        }

        mVideoSubscribe = Observable.just(1).observeOn(Schedulers.io()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d(TAG, "----video encode start---");
                mPresentationTimeUs = System.currentTimeMillis() * 1000;
                mVideoEncoderEnd = false;
                mVideoMediaCodec.configure(mVideoFormat, null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                mVideoMediaCodec.start();
                while (mVideoEncoderLoop && !Thread.interrupted()) {
                    try {
                        /*待编码的数据*/
                        byte[] data = mVideoLinkedBlockQueue.take(); //
                        Log.d(TAG, "要编码的Video数据大小:" + data.length);
                        encodeVideoData(data);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "编码(Video)数据 失败");
                        e.printStackTrace();
                        break;
                    }
                }

                if (mVideoMediaCodec != null) {
                    //停止视频编码器
                    mVideoMediaCodec.stop();
                    //释放视频编码器
                    mVideoMediaCodec.release();
                    mVideoMediaCodec = null;
                }
                mVideoLinkedBlockQueue.clear();
                Log.d(TAG, "---Video encode thread end---");
            }
        });
        mVideoEncoderLoop = true;
    }

    /**
     * 停止视频编码
     */
    private void stopVideoEncode() {
        Log.d(TAG, "stop video encode...");
        mVideoEncoderEnd = true;
    }

    /**
     * 开始音频编码
     */
    private void startAudioEncode() {
        if (mAudioMediaCodec == null) {
            throw new RuntimeException("pls init mAudioMediaCodec");
        }

        if (mAudioEncoderLoop) {
            throw new RuntimeException("mAudioEncoderLoop need stop first");
        }
        mAudioSubscribe = Observable.just(1).observeOn(Schedulers.io()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d(TAG, "------Audio encode start-----");
                mPresentationTimeUs = System.currentTimeMillis() * 1000;
                mEncoderEnd = false;
                mAudioMediaCodec.configure(mAudioFormat, null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                mAudioMediaCodec.start();
                while (mAudioEncoderLoop && !Thread.interrupted()) {
                    try {
                        byte[] data = mAudioLinkedBlockQueue.take();
                        Log.d(TAG, "要编码的Audio数据大小:" + data.length);
                        encodeAudioData(data);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }

                if (mAudioMediaCodec != null) {
                    //停止音频编码器
                    mAudioMediaCodec.stop();
                    //释放音频编码器
                    mAudioMediaCodec.release();
                    mAudioMediaCodec = null;
                }
                mAudioLinkedBlockQueue.clear();
                Log.d(TAG, "----Audio 编码线程退出----");
            }
        });
        mAudioEncoderLoop = true;
    }

    private void stopAudioEncode() {
        Log.d(TAG, "----stop Audio encode----");
        mEncoderEnd = true;
    }

    /**
     * 添加视频数据
     *
     * @param data
     */
    public void putVideoData(byte[] data) {
        try {
            if(mVideoLinkedBlockQueue != null){
                mVideoLinkedBlockQueue.put(data);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加音频数据
     *
     * @param data
     */
    public void putAudioData(byte[] data) {
        try {
            if(mAudioLinkedBlockQueue != null){
                mAudioLinkedBlockQueue.put(data);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 进行视频编码
     * @param input
     */
    private void encodeVideoData(byte[] input) {
        /*input为Camera预览格式NV21数据*/
//        if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
//            Log.d(TAG,"encodeVideoData mColorFormat---"+MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
//            /*nv21格式转为nv12格式*/
//            MSYuvHelper.getInstance().Nv21ToNv12(input, mYuvBuffer, mWidth, mHeight);
//            MSYuvHelper.getInstance().Nv12ClockWiseRotate90(mYuvBuffer, mWidth, mHeight, mRotateYuvBuffer, mOutWidth, mOutHeight);
//        } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
//            Log.d(TAG,"encodeVideoData mColorFormat---"+MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
//            /*用于NV21格式转换为I420(YUV420P)格式*/
//            MSYuvHelper.getInstance().Nv21ToI420(input, mYuvBuffer, mWidth, mHeight);
//            MSYuvHelper.getInstance().I420ClockWiseRotate90(mYuvBuffer, mWidth, mHeight, mRotateYuvBuffer, mOutWidth, mOutHeight);
//        } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar) {
//            Log.d(TAG,"encodeVideoData mColorFormat-----------"+MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar);
//            System.arraycopy(input, 0, mYuvBuffer, 0, mWidth * mHeight * 3 / 2);
//            MSYuvHelper.getInstance().Nv21ClockWiseRotate90(mYuvBuffer, mWidth, mHeight, mRotateYuvBuffer, mOutWidth, mOutHeight);
//        }else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) {
//            Log.d(TAG,"encodeVideoData mColorFormat----"+MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar);
//            /*用于NV21格式转换为YV12格式*/
//            MSYuvHelper.getInstance().Nv21ToYv12(input, mYuvBuffer, mWidth, mHeight);
//            MSYuvHelper.getInstance().Yv12ClockWiseRotate90(mYuvBuffer, mWidth, mHeight, mRotateYuvBuffer, mOutWidth, mOutHeight);
//        }

        Log.d(TAG,"encodeVideoData mColorFormat---"+MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        /*用于NV21格式转换为I420(YUV420P)格式*/
        MSYuvHelper.getInstance().Nv21ToI420(input, mYuvBuffer, mWidth, mHeight);
        MSYuvHelper.getInstance().I420ClockWiseRotate90(mYuvBuffer, mWidth, mHeight, mRotateYuvBuffer, mOutWidth, mOutHeight);

        try {
            /*拿到输入缓冲区,用于传送数据进行编码*/
            ByteBuffer[] inputBuffers = mVideoMediaCodec.getInputBuffers();
            /*得到当前有效的输入缓冲区的索引*/
            int inputBufferIndex = mVideoMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            Log.d(TAG, "Video inputBufferIndex:=" + inputBufferIndex+"  yuvLen= "+ mRotateYuvBuffer.length);
            if (inputBufferIndex >= 0) {
                /*输入缓冲区有效*/
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                Log.d(TAG, "Video inputBufferIndex: " + inputBufferIndex+"  capacity: "+inputBuffer.capacity());
                //往输入缓冲区写入数据
                inputBuffer.put(mRotateYuvBuffer);
                Log.d(TAG, "Video inputBufferIndex: " + inputBufferIndex+"  capacity: "+inputBuffer.capacity()+"  limit: "+inputBuffer.limit());

                /*计算pts，这个值是一定要设置的*/
                long pts = System.currentTimeMillis() * 1000 - mPresentationTimeUs;
                if (mVideoEncoderEnd) {
                    /*结束时，发送结束标志，在编码完成后结束*/
                    Log.d(TAG, "send Video Encoder BUFFER_FLAG_END_OF_STREAM");
                    mVideoMediaCodec.queueInputBuffer(inputBufferIndex, 0, mRotateYuvBuffer.length,
                            pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    //将缓冲区入队
                    Log.d(TAG, "Video  inputBufferIndex: "+inputBufferIndex+"  pts: "+pts);
                    mVideoMediaCodec.queueInputBuffer(inputBufferIndex, 0, mRotateYuvBuffer.length,
                            pts, 0);
                }
            }

            /*拿到输出缓冲区,用于取到编码后的数据*/
            ByteBuffer[] outputBuffers = mVideoMediaCodec.getOutputBuffers();
            /*拿到输出缓冲区的索引*/
            int outputBufferIndex = mVideoMediaCodec.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
            Log.d(TAG, "Video outputBufferIndex: "+outputBufferIndex);
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = mVideoMediaCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d(TAG, "Video INFO_OUTPUT_FORMAT_CHANGED ");
                MediaFormat newFormat = mVideoMediaCodec.getOutputFormat();
                if (null != mCallback && !mVideoEncoderEnd) {
                    Log.d(TAG,"添加视轨 INFO_OUTPUT_FORMAT_CHANGED " + newFormat.toString());
                    mCallback.outMediaFormat(MSMediaMuxer.TRACK_VIDEO, newFormat);
                }
            }
            while (outputBufferIndex >= 0) {
                /*outputBuffer保存的就是H264数据*/
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                            " was null");
                }

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.d(TAG, "Video ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mVideoBufferInfo.size = 0;
                }

                if (mVideoBufferInfo.size != 0) {
                    if (null != mCallback && !mVideoEncoderEnd) {
                        mCallback.outputVideoFrame(MSMediaMuxer.TRACK_VIDEO,outputBuffer, mVideoBufferInfo);
                    }
                }
                /*释放资源*/
                mVideoMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                /*拿到输出缓冲区的索引*/
                outputBufferIndex = mVideoMediaCodec.dequeueOutputBuffer(mVideoBufferInfo, 0);
                /*编码结束的标志*/
                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "Recv Video Encoder===BUFFER_FLAG_END_OF_STREAM=====" );
                    mVideoEncoderLoop = false;
                    mVideoSubscribe.dispose();
                    return;
                }
            }
        } catch (Exception t) {
            Log.e(TAG, "encodeVideoData error: " + t.toString());
        }
    }

    /**
     * 进行音频编码操作
     * @param input
     */
    private void encodeAudioData(byte[] input){
        try {
            /*拿到输入缓冲区,用于传送数据进行编码*/
            ByteBuffer[] inputBuffers = mAudioMediaCodec.getInputBuffers();
            /*得到当前有效的输入缓冲区的索引*/
            int inputBufferIndex = mAudioMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                 Log.d(TAG, "Audio===inputBufferIndex: " + inputBufferIndex);
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                /*往输入缓冲区写入数据*/
                inputBuffer.put(input);

                /*计算pts，这个值是一定要设置的*/
                long pts = System.currentTimeMillis() * 1000 - mPresentationTimeUs;
                if (mEncoderEnd) {
                    /*结束时，发送结束标志，在编码完成后结束*/
                    Log.d(TAG, "send Audio Encoder BUFFER_FLAG_END_OF_STREAM====");
                    mAudioMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length,
                            pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    /*将缓冲区入队*/
                    mAudioMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length,
                            pts, 0);
                }
            }

            /*拿到输出缓冲区,用于取到编码后的数据*/
            ByteBuffer[] outputBuffers = mAudioMediaCodec.getOutputBuffers();
            /*拿到输出缓冲区的索引*/
            int outputBufferIndex = mAudioMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            Log.d(TAG, "Audio outputBufferIndex: "+outputBufferIndex);
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                outputBuffers = mAudioMediaCodec.getOutputBuffers();
            }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                Log.d(TAG, "Audio INFO_OUTPUT_FORMAT_CHANGED ");
                /*加入音轨的时刻,一定要等编码器设置编码格式完成后，再将它加入到混合器中，*/
                /*编码器编码格式设置完成的标志是dequeueOutputBuffer得到返回值为MediaCodec.INFO_OUTPUT_FORMAT_CHANGED*/
                final MediaFormat newformat = mAudioMediaCodec.getOutputFormat();
                if (null != mCallback && !mEncoderEnd) {
                    Log.d(TAG,"添加音轨 INFO_OUTPUT_FORMAT_CHANGED " + newformat.toString());
                    mCallback.outMediaFormat(MSMediaMuxer.TRACK_AUDIO, newformat);
                }
            }
            while (outputBufferIndex >= 0) {
                /*数据已经编码成AAC格式*/
                /*outputBuffer保存的就是AAC数据*/
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    if (null != mCallback && !mEncoderEnd) {
                        mCallback.outputAudioFrame(MSMediaMuxer.TRACK_AUDIO,outputBuffer, mBufferInfo);
                    }
                }
                /*释放资源*/
                mAudioMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                /*拿到输出缓冲区的索引*/
                outputBufferIndex = mAudioMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                /*编码结束的标志*/
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.e(TAG, "Recv Audio Encoder BUFFER_FLAG_END_OF_STREAM ");
                    mAudioEncoderLoop = false;
                    mAudioSubscribe.dispose();
                    return;
                }
            }
        } catch (Exception t) {
            Log.e(TAG, "encodeAudioData error: " + t.toString());
        }
    }
}
