package com.meishe.msmediacodec;


import android.util.Log;
import android.view.SurfaceHolder;

public class MSSurfacePreview implements SurfaceHolder.Callback{
    private final static String TAG = "SurfacePreview";
    private MSVideoChannel.CameraOperateCallback mCallback;
    private PermissionNotify listener;

    public interface PermissionNotify{
        boolean hasPermission();
    }

    public MSSurfacePreview(MSVideoChannel.CameraOperateCallback cb, PermissionNotify listener){
        mCallback = cb;
        this.listener = listener;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        Log.d(TAG, "======zhongjihao=====surfaceDestroyed()====");
        MSVideoChannel.getInstance().doStopCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        Log.d(TAG, "======zhongjihao=====surfaceCreated()====");
        if(listener != null){
            if(listener.hasPermission())
                // 打开摄像头
                MSVideoChannel.getInstance().doOpenCamera(mCallback);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        Log.d(TAG, "======zhongjihao=====surfaceChanged()====");
        if(listener != null){
            if(listener.hasPermission())
                // 打开摄像头
                MSVideoChannel.getInstance().doOpenCamera(mCallback);
        }
    }

}
