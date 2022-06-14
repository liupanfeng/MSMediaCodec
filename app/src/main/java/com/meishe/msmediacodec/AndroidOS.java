package com.meishe.msmediacodec;

import android.content.Context;
import android.os.Build;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/13 上午10:44
 * @Description :
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */
public class AndroidOS {
    public static boolean USE_SCOPED_STORAGE;
    public static void initConfig(Context context){
        //android11的适配版本
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            USE_SCOPED_STORAGE = true;
        }
    }
}
