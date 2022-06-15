
#ifndef MSPLAYER_MS_SAFE_QUEUE_H
#define MSPLAYER_MS_SAFE_QUEUE_H


#include <queue>
#include <pthread.h>

using namespace std;

/*模版函数：相当于Java泛型*/
template<typename T>

class SafeQueue {

private:
    /*函数指针定义做回调 用来释放T里面的内容的*/
    typedef void (*ReleaseCallback)(T *);
    typedef void (*SyncCallback)(queue<T> &);
private:
    queue<T> queue;
    /*互斥锁*/
    pthread_mutex_t mutex;
    /*条件 等待和唤醒*/
    pthread_cond_t cond;
    /*标记队列是否工作*/
    int work;
    /*释放回调*/
    ReleaseCallback releaseCallback;
    /*同步丢包*/
    SyncCallback syncCallback;

public:
    SafeQueue() {
        /*初始化互斥锁  动态初始化锁*/
        pthread_mutex_init(&mutex, 0);
        /*初始化条件变量*/
        pthread_cond_init(&cond, 0);
    }

    ~SafeQueue() {
        /*释放互斥锁*/
        pthread_mutex_destroy(&mutex);
        /*释放条件变量*/
        pthread_cond_destroy(&cond);
    }

    /**
     * 入队
     * AVPacket *  压缩包
     * AVFrame * 原始包
     */
    void insertToQueue(T value) {
        /* 多线程的访问（先锁住）*/
        pthread_mutex_lock(&mutex);

        if (work) {
            /*工作状态*/
            queue.push(value);
            /*当插入数据包进队列后，要发出通知唤醒*/
            pthread_cond_signal(&cond);
        } else {
            /*非工作状态，释放value，T类型不明确让外界释放*/
            if (releaseCallback) {
                /*让外界释放我们的 value*/
                releaseCallback(&value);
            }
        }
        /*多线程的访问解锁*/
        pthread_mutex_unlock(&mutex);
    }

    /**
     *  出队
     *  获取队列数据后，并且 删除
     */
    int getQueueAndDel(T &value) {
        /* 默认是false*/
        int ret = 0;
        /*多线程的访问（先锁住）*/
        pthread_mutex_lock(&mutex);

        while (work && queue.empty()) {
            /*如果是工作队列里面没有数据，进行阻塞*/
            pthread_cond_wait(&cond, &mutex);
        }

        if (!queue.empty()) {
            /*取出队列的数据包给外界，并删除队列数据包*/
            value = queue.front();
            /*删除队列中的数据*/
            queue.pop();
            /*成功了*/
            ret = 1;
        }
        /*多线程的访问（要解锁）*/
        pthread_mutex_unlock(&mutex);
        return ret;
    }

    /**
    * 设置工作状态，设置队列是否工作
    * @param work
    */
    void setWork(int work) {
        /* 多线程的访问（先锁住）*/
        pthread_mutex_lock(&mutex);

        this->work = work;

        /*每次设置状态后就去唤醒下，有没有阻塞睡觉的地方*/
        pthread_cond_signal(&cond);
        /* 多线程的访问（要解锁）*/
        pthread_mutex_unlock(&mutex);
    }

    int empty(){
        return queue.empty();
    }

    int size(){
        return queue.size();
    }

    /**
     * 清空队列中所有的数据，循环一个一个的删除
     */
    void clear(){
        /* 多线程的访问（先锁住）*/
        pthread_mutex_lock(&mutex);

        unsigned int size = queue.size();

        for (int i = 0; i < size; ++i) {
            /*循环释放队列中的数据*/
            T value = queue.front();
            if(releaseCallback){
                /*让外界去释放堆区空间*/
                releaseCallback(&value);
            }
            /* 删除队列中的数据，让队列为0*/
            queue.pop();
        }
        /*多线程的访问（要解锁）*/
        pthread_mutex_unlock(&mutex);
    }

    /**
     * 设置此函数指针的回调，让外界去释放
     * @param releaseCallback
     */
    void setReleaseCallback(ReleaseCallback releaseCallback){
        this->releaseCallback = releaseCallback;
    }

    /**
   * 设置此函数指针的回调，让外界去释放
   * @param syncCallback
   */
    void setSyncCallback(SyncCallback syncCallback){
        this->syncCallback = syncCallback;
    }

    /**
    * 同步操作 丢包
    */
    void sync() {
        pthread_mutex_lock(&mutex);
        /*函数指针具体丢包动作，回调到外界完成*/
        syncCallback(queue);
        pthread_mutex_unlock(&mutex);
    }

};


#endif //MSPLAYER_MS_SAFE_QUEUE_H
