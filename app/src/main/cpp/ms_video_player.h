//
//

#ifndef MSPLAYER_MS_VIDEO_PLAYER_H
#define MSPLAYER_MS_VIDEO_PLAYER_H

#include <cstring>
#include <pthread.h>
#include "ms_audio_channel.h"
#include "ms_video_channel.h" // 可以直接访问函数指针
#include "jni_callback.h"
#include "ms_video_statel.h"

/*ffmpeg是纯c写的，必须采用c的编译方式，*/
extern "C" {
#include <libavformat/avformat.h>
};


class MSPlayer {
private:
    /*资源路径 指针请赋初始值，否则会出现乱指的情况  指针类型在析构函数需要释放*/
    char *data_source = 0;
    /*prepare线程id*/
    pthread_t pid_prepare;
    /*start 线程id*/
    pthread_t pid_start;
    /*封装格式上下文*/
    AVFormatContext *avFormatContext = 0;
    /*音频轨道 处理音频数据*/
    AudioChannel *audio_channel = 0;
    /*视频轨道 处理视频数据*/
    VideoChannel *video_channel = 0;
    /*通过jni 回调java方法*/
    JniUtil *helper = 0;
    /*播放标识 是否播放*/
    bool isPlaying;
    /*渲染回调*/
    RenderCallback renderCallback;
    /*视频时长*/
    int duration;
    /*seek 异步锁，用于seek操作加锁*/
    pthread_mutex_t seek_mutex;
    /*停止 线程*/
    pthread_t pid_stop;
    /*JNI 层回调 */
    JniUtil *jniCallback = 0;

public:
    /**
     *
     * @param data_source  视频路径
     * @param helper 回调接口
     */
    MSPlayer(const char *data_source, JniUtil *helper);
    ~MSPlayer();

    /**
     * 开启准备工作的线程
     */
    void prepare();
    /**
     * 子线程执行  ffmpeg 解封装
     */
    void prepare_();

    /**
     * 开始播放的准备工作，创建子线程等
     */
    void start();
    /**
     * 子线程执行开始播放任务
     */
    void start_();
    /**
     * 设置渲染的回调
     * @param renderCallback
     */
    void setRenderCallback(RenderCallback renderCallback);
    /**
     * 得到视频长度
     * @return
     */
    int getDuration();

    /**
     * 进行seek操作
     * @param progress
     */
    void seek(int progress);
    /**
     * 停止操作
     */
    void stop();
    /**
     * 停止操作
     * @param pPlayer
     */
    void stop_(MSPlayer *pPlayer);

    /*获取播放状态*/
    bool getPlayState();


    void setJNICallback(JniUtil *jniCallback) ;

};


#endif //MSPLAYER_MS_VIDEO_PLAYER_H
