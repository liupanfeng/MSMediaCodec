package com.meishe.msmediacodec;

import android.app.Application;
import android.content.Context;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/13 上午10:44
 * @Description :
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */
public class App extends Application {

    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext=this;
        AndroidOS.initConfig(this);
    }

    public static Context getContext(){
        return mContext;
    }
}
