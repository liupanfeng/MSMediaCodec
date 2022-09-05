package com.meishe.msmediacodec.inter

import android.media.MediaFormat

/**
 * All rights Reserved, Designed By www.meishesdk.com
 *
 * @Author: lpf
 * @CreateDate: 2022/9/5 下午6:31
 * @Description:
 * @Copyright: www.meishesdk.com Inc. All rights reserved.
 */
interface IDecoder {

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
//    fun setStateListener(l: IDecoderStateListener?)

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