package com.meishe.msmediacodec.inter

import android.media.MediaCodec
import java.nio.ByteBuffer

abstract class BaseDecoder:IDecoder {

    //-------------线程相关------------------------

    /*** 解码器是否在运行 */
    private var mIsRunning = true

    /*** 线程等待锁 */
    private val mLock = Object()

    /*** 是否可以进入解码 */
    private var mReadyForDecode = false

    /*** 音视频解码器 */
    protected var mCodec: MediaCodec? = null

    /*** 音视频数据读取器 */
    protected var mExtractor: IExtractor? = null

    /*** 解码输入缓存区 */
    protected var mInputBuffers: Array<ByteBuffer>? = null

    /*** 解码输出缓存区 */
    protected var mOutputBuffers: Array<ByteBuffer>? = null


    /*** 解码数据信息 */
    private var mBufferInfo = MediaCodec.BufferInfo()

    private var mState = DecodeState.STOP

    private var mStateListener: IDecoderStateListener? = null

    /*** 流数据是否结束 */
    private var mIsEOS = false
    protected var mVideoWidth = 0
    protected var mVideoHeight = 0


}