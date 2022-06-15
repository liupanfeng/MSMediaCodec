package com.meishe.msmediacodec;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;


/**
 * @author lpf 拍摄页面
 */
public class CaptureActivity extends AppCompatActivity implements View.OnClickListener {

    static {
        System.loadLibrary("native-lib");
    }

    private final static String TAG = "MainActivity";
    private Button mBtnStart;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsStarted;
    private MSMediaMuxer mMediaMuxer;
    private boolean mHasPermission;
    private MSVideoChannel mMsVideoChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mMsVideoChannel = MSVideoChannel.getInstance();

        initView();

        initMuxer();

        requestPermission();
        MSYuvEngineHelper.getInstance().startYuvEngine();
    }

    private void initMuxer() {
        String dirPath = PathUtils.getRecordDir();
        String filePath = dirPath + FileUtil.getMp4FileName(System.currentTimeMillis());
        mMediaMuxer = MSMediaMuxer.getInstance();
        mMediaMuxer.initMediaMuxer(filePath);
        Log.d(TAG, "initMediaMuxer filePath:" + filePath);
    }

    private void initView() {
        mIsStarted = false;
        mHasPermission = false;
        mBtnStart = findViewById(R.id.btn_start);
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.setKeepScreenOn(true);
        mSurfaceHolder = mSurfaceView.getHolder();
        /*设置surface不需要自己的维护缓存区*/
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(new MSSurfaceCallback());
        mBtnStart.setOnClickListener(this);
    }

    /**
     * 获取授权
     */
    private void requestPermission() {
        XXPermissions.with(this).permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.CAMERA)
                .permission(Permission.RECORD_AUDIO)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            mBtnStart.post(new Runnable() {
                                @Override
                                public void run() {
                                    mBtnStart.setEnabled(true);
                                    mHasPermission = true;
                                    // 打开摄像头
                                    mMsVideoChannel.openCamera(CaptureActivity.this,mSurfaceHolder);
                                }
                            });

                        } else {
                            mBtnStart.setEnabled(false);
                            mHasPermission = false;
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        mBtnStart.setEnabled(false);
                        mHasPermission = false;
                    }
                });
    }


    private void codecToggle() {
        if (mIsStarted) {
            mIsStarted = false;
            //停止编码 先要停止编码，然后停止采集
            mMediaMuxer.stopEncoder();
            //停止音频采集
            mMediaMuxer.stopAudioGather();
            //释放编码器
            mMediaMuxer.release();
            mMediaMuxer = null;
        } else {
            mIsStarted = true;
            if (mMediaMuxer == null) {
                initMuxer();
            }

            //采集音频
            mMediaMuxer.startAudioGather();
            //初始化音频编码器
            mMediaMuxer.initAudioEncoder();
            //初始化视频编码器
            mMediaMuxer.initVideoEncoder(mMsVideoChannel.getWidth(),
                    mMsVideoChannel.getHeight(), mMsVideoChannel.getFrameRate());
            //启动编码
            mMediaMuxer.startEncoder();
        }
        mBtnStart.setText(mIsStarted ? "停止" : "开始");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsStarted) {
            mIsStarted = false;
            if (mMediaMuxer != null) {
                //停止编码 先要停止编码，然后停止采集
                mMediaMuxer.stopEncoder();
                //停止音频采集
                mMediaMuxer.stopAudioGather();
                //释放编码器
                mMediaMuxer.release();
                mMediaMuxer = null;
            }
        }
        mMsVideoChannel.doStopCamera();
        mMsVideoChannel = null;
        MSYuvEngineHelper.getInstance().stopYuvEngine();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_start:
                codecToggle();
                break;
        }
    }



    public boolean hasPermission() {
        return mHasPermission;
    }

    private class MSSurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            Log.d(TAG, "------surfaceCreated------");
            if (hasPermission()) {
                mMsVideoChannel.openCamera(CaptureActivity.this,mSurfaceHolder);
            }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            Log.d(TAG, "------surfaceChanged------");
            if (hasPermission()) {
                mMsVideoChannel.openCamera(CaptureActivity.this,mSurfaceHolder);
            }
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            Log.d(TAG, "------surfaceDestroyed------");
            mMsVideoChannel.doStopCamera();
        }
    }


}
