package com.meishe.msmediacodec;

/**
 * Created by zhongjihao on 18-2-7.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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

@SuppressLint("NewApi")
public class MSVideoChannel {
    private static final String TAG = "VideoGather";
    private int preWidth;
    private int preHeight;
    private int frameRate;
    private static MSVideoChannel mCameraWrapper;

    // 定义系统所用的照相机
    private Camera mCamera;
    //预览尺寸
    private Camera.Size previewSize;
    private Camera.Parameters mCameraParamters;
    private boolean mIsPreviewing = false;
    private CameraPreviewCallback mCameraPreviewCallback;

    private Callback mCallback;
    private CameraOperateCallback cameraCb;
    private Context mContext;

    private MSVideoChannel() {
        mPreviewScale = mPreviewDefaultHeight * 1f / mPreviewDefaultWidth;
    }

    public interface CameraOperateCallback {
        public void cameraHasOpened();
        public void cameraHasPreview(int width,int height,int fps);
    }

    public interface Callback {
        public void videoData(byte[] data);
    }

    public static MSVideoChannel getInstance() {
        if (mCameraWrapper == null) {
            synchronized (MSVideoChannel.class) {
                if (mCameraWrapper == null) {
                    mCameraWrapper = new MSVideoChannel();
                }
            }
        }
        return mCameraWrapper;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void doOpenCamera(CameraOperateCallback callback) {
        Log.d(TAG, "====zhongjihao====Camera open....");
        cameraCb = callback;
        if(mCamera != null)
            return;
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        Log.d(TAG, "====zhongjihao=====Camera open over....");
        cameraCb.cameraHasOpened();
    }

    public void doStartPreview(Activity activity,SurfaceHolder surfaceHolder) {
        if (mIsPreviewing) {
            return;
        }
        mContext = activity;
        setCameraDisplayOrientation(activity, Camera.CameraInfo.CAMERA_FACING_BACK);
        setCameraParamter(surfaceHolder);
        try {
            // 通过SurfaceView显示取景画面
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        mIsPreviewing = true;
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Log.d(TAG, "=====zhongjihao===onAutoFocus----->success: "+success);
            }
        });
        Log.d(TAG, "=====zhongjihao===Camera Preview Started...");
        cameraCb.cameraHasPreview(preWidth,preHeight,frameRate);
    }

    public void doStopCamera() {
        Log.d(TAG, "doStopCamera-------mCamera: "+mCamera+"   mCameraWrapper: "+mCameraWrapper);
        // 如果camera不为null，释放摄像头
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);
            mCameraPreviewCallback = null;
            if (mIsPreviewing)
                mCamera.stopPreview();
            mIsPreviewing = false;
            mCamera.release();
            mCamera = null;
        }

        if(mCameraWrapper != null){
            mCallback = null;
            cameraCb = null;
            mContext = null;
            mCameraWrapper = null;
        }
    }

    private void setCameraParamter(SurfaceHolder surfaceHolder) {
        if (!mIsPreviewing && mCamera != null) {

            mCameraParamters = mCamera.getParameters();
            // 如果摄像头不支持这些参数都会出错的，所以设置的时候一定要判断是否支持

            // 设置闪光模式
            List<String> supportedFlashModes = mCameraParamters.getSupportedFlashModes();
            if (supportedFlashModes != null &&
                    supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                mCameraParamters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            // 设置聚焦模式
            List<String> supportedFocusModes = mCameraParamters.getSupportedFocusModes();

            // 连续聚焦
            if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                mCameraParamters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            // 自动聚焦
            if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                mCameraParamters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            // 设置预览图片格式
            mCameraParamters.setPreviewFormat(ImageFormat.NV21);
            // 设置拍照图片格式
            mCameraParamters.setPictureFormat(ImageFormat.JPEG);
            // 设置曝光强度
            mCameraParamters.setExposureCompensation(0);

            previewSize = getSuitableSize(mCameraParamters.getSupportedPreviewSizes(), "preview");
            preWidth = previewSize.width;
            preHeight = previewSize.height;
            mCameraParamters.setPreviewSize(previewSize.width, previewSize.height);

            Log.d(TAG, "-----preWidth" + preWidth+"    preHeight: "+preHeight);

            //set fps range.
            int defminFps = 0;
            int defmaxFps = 0;
            List<int[]> supportedPreviewFpsRange = mCameraParamters.getSupportedPreviewFpsRange();
            for (int[] fps : supportedPreviewFpsRange) {
                Log.d(TAG, "=====zhongjihao=====setParameters====find fps:" + Arrays.toString(fps));
                if (defminFps <= fps[PREVIEW_FPS_MIN_INDEX] && defmaxFps <= fps[PREVIEW_FPS_MAX_INDEX]) {
                    defminFps = fps[PREVIEW_FPS_MIN_INDEX];
                    defmaxFps = fps[PREVIEW_FPS_MAX_INDEX];
                }
            }
            //设置相机预览帧率
            Log.d(TAG, "=====zhongjihao=====setParameters====defminFps:" + defminFps+"    defmaxFps: "+defmaxFps);
            mCameraParamters.setPreviewFpsRange(defminFps,defmaxFps);
            frameRate = defmaxFps / 1000;
            surfaceHolder.setFixedSize(previewSize.width, previewSize.height);
            mCameraPreviewCallback = new CameraPreviewCallback();
            mCamera.addCallbackBuffer(new byte[previewSize.width * previewSize.height*3/2]);
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            Log.d(TAG, "=====zhongjihao=====setParameters====preWidth:" + preWidth+"   preHeight: "+preHeight+"  frameRate: "+frameRate);
            mCamera.setParameters(mCameraParamters);
        }
    }

//    private void setCameraParamter(SurfaceHolder surfaceHolder) {
//        if (!mIsPreviewing && mCamera != null) {
//            mCameraParamters = mCamera.getParameters();
//            List<Integer> previewFormats = mCameraParamters.getSupportedPreviewFormats();
//            for(int i=0;i<previewFormats.size();i++){
//                Log.d(TAG,"support preview format : "+previewFormats.get(i));
//            }
//            mCameraParamters.setPreviewFormat(ImageFormat.NV21);
//            // 设置拍照图片格式
//            mCameraParamters.setPictureFormat(ImageFormat.JPEG);
//
//            // 设置预览图片大小
//            previewSize = getSuitableSize(mCameraParamters.getSupportedPreviewSizes(), "preview");
//            preWidth = previewSize.width;
//            preHeight = previewSize.height;
//            mCameraParamters.setPreviewSize(previewSize.width, previewSize.height);
//
//            //set fps range.
//            int defminFps = 0;
//            int defmaxFps = 0;
//            List<int[]> supportedPreviewFpsRange = mCameraParamters.getSupportedPreviewFpsRange();
//            for (int[] fps : supportedPreviewFpsRange) {
//                Log.d(TAG, "=====zhongjihao=====setParameters====find fps:" + Arrays.toString(fps));
//                if (defminFps <= fps[PREVIEW_FPS_MIN_INDEX] && defmaxFps <= fps[PREVIEW_FPS_MAX_INDEX]) {
//                    defminFps = fps[PREVIEW_FPS_MIN_INDEX];
//                    defmaxFps = fps[PREVIEW_FPS_MAX_INDEX];
//                }
//            }
//            //设置相机预览帧率
//            Log.d(TAG, "=====zhongjihao=====setParameters====defminFps:" + defminFps+"    defmaxFps: "+defmaxFps);
//            mCameraParamters.setPreviewFpsRange(defminFps,defmaxFps);
//            frameRate = defmaxFps / 1000;
//            surfaceHolder.setFixedSize(previewSize.width, previewSize.height);
//            mCameraPreviewCallback = new CameraPreviewCallback();
//            mCamera.addCallbackBuffer(new byte[previewSize.width * previewSize.height*3/2]);
//            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
//            List<String> focusModes = mCameraParamters.getSupportedFocusModes();
//            for (String focusMode : focusModes){//检查支持的对焦
//                Log.d(TAG, "=====zhongjihao=====setParameters====focusMode:" + focusMode);
//                if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
//                    mCameraParamters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//                }else if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
//                    mCameraParamters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//                }else if(focusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
//                    mCameraParamters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//                }
//            }
//            Log.d(TAG, "=====zhongjihao=====setParameters====preWidth:" + preWidth+"   preHeight: "+preHeight+"  frameRate: "+frameRate);
//            mCamera.setParameters(mCameraParamters);
//        }
//    }

    private int mPreviewDefaultWidth = 1920; // default 1440
    private int mPreviewDefaultHeight = 1080; // default 1080
    private float mPreviewScale;


    /**
     * @param sizes
     * @param type
     * @return
     */
    private Camera.Size getSuitableSize(List<Camera.Size> sizes, String type) {
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size size = sizes.get(i);
            Log.d(TAG, " type = " + type + " SupportedSize, width: " + size.width + ", height: " + size.height);
            // 先判断比例是否相等
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
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        Log.d(TAG, "=====zhongjihao=====setCameraDisplayOrientation=====result:" + result+"  rotation: "+rotation+"  degrees: "+degrees+"  orientation: "+info.orientation);
        mCamera.setDisplayOrientation(result);
    }

    class CameraPreviewCallback implements Camera.PreviewCallback {
        private CameraPreviewCallback() {

        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Size size = camera.getParameters().getPreviewSize();
            //通过回调,拿到的data数据是原始数据
            //丢给VideoRunnable线程,使用MediaCodec进行h264编码操作
            if(data != null){
                if(mCallback != null)
                    mCallback.videoData(data);
                camera.addCallbackBuffer(data);
            }
            else {
                camera.addCallbackBuffer(new byte[size.width * size.height *3/2]);
            }
        }
    }

}
