package com.meishe.msmediacodec.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.meishe.msmediacodec.codec.MSMediaMuxer;
import com.meishe.msmediacodec.databinding.ActivityMainBinding;
import com.meishe.msmediacodec.helper.MSYuvHelper;
import com.meishe.msmediacodec.R;
import com.meishe.msmediacodec.channel.MSVideoChannel;
import com.meishe.msmediacodec.utils.Constants;
import com.meishe.msmediacodec.utils.FileUtil;
import com.meishe.msmediacodec.utils.PathUtils;
import com.meishe.msmediacodec.utils.TimerUtils;

import java.io.File;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;


/**
 * @author lpf 拍摄页面
 */
public class CaptureActivity extends AppCompatActivity implements View.OnClickListener {

    static {
        System.loadLibrary("native-lib");
    }
    private final static String TAG = "MainActivity";

    private ActivityMainBinding mBinding;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsStarted;
    private MSMediaMuxer mMediaMuxer;
    private boolean mHasPermission;
    private MSVideoChannel mMsVideoChannel;
    private String mFilePath;
    private Disposable mDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*设置全屏*/
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mBinding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mMsVideoChannel = MSVideoChannel.getInstance();

        initView();
        initMuxer();
        requestPermission();
        MSYuvHelper.getInstance().startYuvEngine();
        initListener();
    }

    private void initListener() {
        mBinding.ivBackDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(mFilePath)){
                    File file=new File(mFilePath);
                    PathUtils.deleteFile(file.getAbsolutePath());
                    mBinding.flMiddleParent.setVisibility(View.GONE);
                }
            }
        });

        mBinding.ivConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(CaptureActivity.this,MSPlayerActivity.class);
                intent.putExtra(Constants.INTENT_KEY_VIDEO_PATH, mFilePath);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initMuxer() {
        String dirPath = PathUtils.getRecordDir();
        mFilePath = dirPath + FileUtil.getMp4FileName(System.currentTimeMillis());
        mMediaMuxer = MSMediaMuxer.getInstance();
        mMediaMuxer.initMediaMuxer(mFilePath);
        Log.d(TAG, "initMediaMuxer mFilePath:" + mFilePath);
    }

    private void initView() {
        mIsStarted = false;
        mHasPermission = false;
        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceView.setKeepScreenOn(true);
        mSurfaceHolder = mSurfaceView.getHolder();
        /*设置surface不需要自己的维护缓存区*/
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(new MSSurfaceCallback());
        mBinding.flTakePhotos.setOnClickListener(this);
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
                            mBinding.flTakePhotos.post(new Runnable() {
                                @Override
                                public void run() {
                                    mHasPermission = true;
                                    // 打开摄像头
                                    mMsVideoChannel.openCamera(CaptureActivity.this,mSurfaceHolder);
                                }
                            });

                        } else {
                            mHasPermission = false;
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        mHasPermission = false;
                    }
                });
    }


    private void codecToggle() {
        if (mIsStarted) {
            mBinding.ivTakePhoto.setBackgroundResource(R.mipmap.capture_take_photo);
            mBinding.ivBackDelete.setVisibility(View.VISIBLE);
            mBinding.ivConfirm.setVisibility(View.VISIBLE);
            mIsStarted = false;
            //停止编码 先要停止编码，然后停止采集
            mMediaMuxer.stopEncoder();
            //停止音频采集
            mMediaMuxer.stopAudioGather();
            //释放编码器
            mMediaMuxer.release();
            mMediaMuxer = null;
            if (mDisposable!=null){
                mDisposable.dispose();
            }
        } else {
            mBinding.ivTakePhoto.setBackgroundResource(R.mipmap.capture_stop_video);
            mBinding.tvTimingNum.setVisibility(View.VISIBLE);

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

            mDisposable = TimerUtils.startTimer(1000, new TimerUtils.TimerListener() {
                @Override
                public void onLoading(int next) {
                    Log.d(TAG, "onLoading: next ======[" + next + "]");
                    mBinding.tvTimingNum.setText("00:"+TimerUtils.getSeconds(1000-next));
                }

                @Override
                public void onComplete() {
                    Log.d(TAG, "onComplete: 计时结束");
                }
            });

        }
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
        MSYuvHelper.getInstance().stopYuvEngine();
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
            case R.id.fl_take_photos:
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
