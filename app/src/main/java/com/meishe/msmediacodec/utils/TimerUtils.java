package com.meishe.msmediacodec.utils;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/15 下午5:02
 * @Description :
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;

public class TimerUtils {

    private static int temp;

    //计时器
    public static Disposable startTimer(int Countdown, TimerListener timerListener) {
        temp = Countdown;
        return Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread()).takeWhile(aLong -> {
                    if (temp == 0 || temp == -1) {
                        return false;
                    } else {
                        return true;
                    }
                })
                .subscribeWith(new DisposableObserver<Long>() {
                    @Override
                    public void onNext(Long o) {
                        temp--;
                        if (temp == 0) {
                            timerListener.onComplete();
                        } else if (temp == -1) {
                            //不做任何事情
                        } else {
                            timerListener.onLoading(temp);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /*
     *停止计时器，不做任何动作
     */
    public static void stopTimer() {
        temp = -1;
    }

    /**
     * 停止计时器，并回调onComplete
     */
    public static void completed() {
        temp = 0;
    }

    public interface TimerListener {
        void onLoading(int next);

        void onComplete();
    }


    /**
     * 给我一个duration，转换成xxx分钟
     * @param duration
     * @return
     */
    public static String getMinutes(int duration) {
        int minutes = duration / 60;
        if (minutes <= 9) {
            return "0" + minutes;
        }
        return "" + minutes;
    }

    /**
     * 给我一个duration，转换成xxx秒
     * @param duration
     * @return
     */
    public static String getSeconds(int duration) {
        int seconds = duration % 60;
        if (seconds <= 9) {
            return "0" + seconds;
        }
        return "" + seconds;
    }
}

