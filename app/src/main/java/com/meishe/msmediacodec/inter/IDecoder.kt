package com.meishe.msmediacodec.inter

import android.media.MediaFormat

/**
 * All rights Reserved, Designed By www.meishesdk.com
 *
 * @Author: lpf
 * @CreateDate: 2022/9/5 下午6:31
 * @Description:  定义了解码器的一些基础操作，如暂停/继续/停止解码，获取视频的时长，视频的宽高，解码状态等等
 * 这里使用的是同步模式解码，需要不断循环压入和拉取数据，是一个耗时操作，因此，我们将解码器定义为一
个Runnable，最后放到线程池中执行。
 * @Copyright: www.meishesdk.com Inc. All rights reserved.
 */
interface IDecoder :Runnable{

    /**
     * 暂停解码
     */
    fun pause()

    /**
     * 继续解码
     */
    fun goOn()

    /**
     * 停止解码
     */
    fun stop()

    /**
     * 是否正在解码
     */
    fun isDecoding(): Boolean

    /**
     * 是否正在快进
     */
    fun isSeeking(): Boolean

    /**
     * 是否停止解码
     */
    fun isStop(): Boolean

    /**
     * 设置状态监听器
     */
    fun setStateListener(l: IDecoderStateListener?)

    /**
     * 获取视频宽
     */
    fun getWidth(): Int

    fun getHeight(): Int

    fun getDuration(): Long

    /**
     * 获取视频旋转角度
     */
    fun getRotationAngle(): Int


    /**
     * 获取音视频对应的格式参数
     */
    fun getMediaFormat(): MediaFormat?


    /**
     * 获取音视频对应的媒体轨道
     */
    fun getTrack(): Int

    /**
     * 获取解码的文件路径
     */
    fun getFilePath(): String




}