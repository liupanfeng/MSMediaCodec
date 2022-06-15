//
// Created by lpf on 2022/6/1.
//

#ifndef NE_PLAYER_MACRO_H
#define NE_PLAYER_MACRO_H

/*定义队列阈值，没有这个会造成内存泄漏，并发生崩溃*/
#define AV_MAX_SIZE 10

/*主线程*/
#define MS_THREAD_MAIN 1
/*子线程*/
#define MS_THREAD_CHILD 2


/*视频路径或者链接异常*/
#define FFMPEG_CAN_NOT_OPEN_URL 1
/*找不到流媒体*/
#define FFMPEG_CAN_NOT_FIND_STREAMS 2
/*找不到解码器*/
#define FFMPEG_FIND_DECODER_FAIL 3
/*无法根据解码器创建上下文*/
#define FFMPEG_ALLOC_CODEC_CONTEXT_FAIL 4
/*打开解码器失败*/
#define FFMPEG_OPEN_DECODER_FAIL 5
/*没有音视频*/
#define FFMPEG_NOMEDIA 6

#endif //NE_PLAYER_MACRO_H


/*宏函数 工具函数 用于释放对象*/
#define DELETE(object) if(object) { delete object ; object=0 ;}