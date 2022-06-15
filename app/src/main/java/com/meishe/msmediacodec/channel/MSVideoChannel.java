package com.meishe.msmediacodec.channel;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/14 下午2:42
 * @Description : 视频通道
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */
public class MSVideoChannel {
    private static final String TAG = "VideoGather";
    private int mWidth;
    private int mHeight;
    private int mFrameRate;

    /**
     * 定义系统所用的照相机
     */
    private Camera mCamera;
    /**
     * 预览尺寸
     */
    private Camera.Size mPreviewSize;

    private Camera.Parameters mCameraParamters;
    private boolean mIsPreviewing = false;
    private CameraPreviewCallback mCameraPreviewCallback;

    private Callback mCallback;
    /**
     * default 1440
     */
    private int mPreviewDefaultWidth = 1920;
    /**
     * default 1080
     */
    private int mPreviewDefaultHeight = 1080;
    private float mPreviewScale;

    private MSVideoChannel() {
        mPreviewScale = mPreviewDefaultHeight * 1f / mPreviewDefaultWidth;
    }

    public interface Callback {
         void videoData(byte[] data);
    }

    public static MSVideoChannel getInstance() {
        return Helper.instance;
    }

    private static class Helper{
        private static MSVideoChannel instance=new MSVideoChannel();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void openCamera(Activity activity, SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Camera open....");
        if(mCamera != null){
            return;
        }
        mCamera = Camera.open();
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        startPreview(activity,surfaceHolder);
    }

    private void startPreview(Activity activity, SurfaceHolder surfaceHolder) {
        if (mIsPreviewing) {
            return;
        }
        setCameraDisplayOrientation(activity, Camera.CameraInfo.CAMERA_FACING_BACK);
        setCameraParameter(surfaceHolder);
        try {
            /*通过SurfaceView显示取景画面*/
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        mIsPreviewing = true;
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Log.d(TAG, "onAutoFocus success: "+success);
            }
        });
        Log.d(TAG, "----Camera Preview Started----");
    }

    public void doStopCamera() {
        Log.d(TAG, "doStopCamera-------mCamera: "+mCamera);
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);
            mCameraPreviewCallback = null;
            if (mIsPreviewing){
                mCamera.stopPreview();
            }
            mIsPreviewing = false;
            mCamera.release();
            mCamera = null;
        }
        mCallback = null;
    }

    /**
     * 设置拍摄参数
     * @param surfaceHolder
     */
    private void setCameraParameter(SurfaceHolder surfaceHolder) {
        if (!mIsPreviewing && mCamera != null) {

            mCameraParamters = mCamera.getParameters();

            /*设置闪光模式*/
            List<String> supportedFlashModes = mCameraParamters.getSupportedFlashModes();
            if (supportedFlashModes != null &&
                    supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                mCameraParamters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }

            /*设置聚焦模式*/
            List<String> supportedFocusModes = mCameraParamters.getSupportedFocusModes();

            /*连续聚焦*/
            if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                mCameraParamters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            /*自动聚焦*/
            if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                mCameraParamters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            /*设置预览图片格式*/
            mCameraParamters.setPreviewFormat(ImageFormat.NV21);
            /*设置拍照图片格式*/
            mCameraParamters.setPictureFormat(ImageFormat.JPEG);
            /*设置曝光强度*/
            mCameraParamters.setExposureCompensation(0);

            mPreviewSize = getSuitableSize(mCameraParamters.getSupportedPreviewSizes(), "preview");
            mWidth = mPreviewSize.width;
            mHeight = mPreviewSize.height;
            mCameraParamters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

            /*设置fps范围*/
            int defMinFps = 0;
            int defMaxFps = 0;
            List<int[]> supportedPreviewFpsRange = mCameraParamters.getSupportedPreviewFpsRange();
            for (int[] fps : supportedPreviewFpsRange) {
                Log.d(TAG, "supportedPreviewFpsRange:" + Arrays.toString(fps));
                if (defMinFps <= fps[PREVIEW_FPS_MIN_INDEX] && defMaxFps <= fps[PREVIEW_FPS_MAX_INDEX]) {
                    defMinFps = fps[PREVIEW_FPS_MIN_INDEX];
                    defMaxFps = fps[PREVIEW_FPS_MAX_INDEX];
                }
            }
            /*设置相机预览帧率*/
            mCameraParamters.setPreviewFpsRange(defMinFps,defMaxFps);
            mFrameRate = defMaxFps / 1000;
            surfaceHolder.setFixedSize(mPreviewSize.width, mPreviewSize.height);
            mCameraPreviewCallback = new CameraPreviewCallback();
            /*设置缓冲区大小*/
            mCamera.addCallbackBuffer(new byte[mPreviewSize.width * mPreviewSize.height*3/2]);
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            mCamera.setParameters(mCameraParamters);

            Log.d(TAG, "defMinFps=" + defMinFps+" defMaxFps: "+defMaxFps);
            Log.d(TAG, "mWidth:" + mWidth +"  mHeight: "+ mHeight +"  mFrameRate: "+ mFrameRate);
        }
    }

    /**
     * 得到合适的尺寸
     * @param sizes
     * @param type
     * @return
     */
    private Camera.Size getSuitableSize(List<Camera.Size> sizes, String type) {
        /*最小的差值，初始值应该设置大点保证之后的计算中会被重置*/
        int minDelta = Integer.MAX_VALUE;
        /* 最小的差值对应的索引坐标*/
        int index = 0;
        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size size = sizes.get(i);
            Log.d(TAG, " type = " + type +
                    " SupportedSize, width: " + size.width +
                    ", height: " + size.height);
            /*先判断比例是否相等*/
            if (size.width * mPreviewScale == size.height) {
                int delta = Math.abs(mPreviewDefaultWidth - size.width);
                if (delta == 0) {
                    return size;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return sizes.get(index);
    }

    /**
     * 相机显示方向
     * @param activity
     * @param cameraId
     */
    private void setCameraDisplayOrientation(Activity activity,int cameraId) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            /*补偿镜子*/
            result = (360 - result) % 360;
        } else {
            /*背面*/
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
        Log.d(TAG, "setCameraDisplayOrientation----result:" + result+" rotation: "+rotation+"  degrees: "+degrees+"  orientation: "+info.orientation);
    }

    class CameraPreviewCallback implements Camera.PreviewCallback {
        private CameraPreviewCallback() { }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Size size = camera.getParameters().getPreviewSize();
            /*丢给VideoRunnable线程,使用MediaCodec进行h264编码操作*/
            if(data != null){
                if(mCallback != null){
                    mCallback.videoData(data);
                }
                camera.addCallbackBuffer(data);
            } else {
                camera.addCallbackBuffer(new byte[size.width * size.height *3/2]);
            }
        }
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getFrameRate() {
        return mFrameRate;
    }
}
