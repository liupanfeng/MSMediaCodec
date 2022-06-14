package com.meishe.msmediacodec;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, MSVideoChannel.CameraOperateCallback, MSSurfacePreview.PermissionNotify{

    static {
        System.loadLibrary("native-lib");
    }

    private final static String TAG = "MainActivity";
    private Button btnStart;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MSSurfacePreview mSurfacePreview;
    private boolean isStarted;
    private MSMediaMuxer mediaMuxer;
    private int width;
    private int height;
    private int frameRate;
    private boolean hasPermission;
    private static final int TARGET_PERMISSION_REQUEST = 100;

    // 要申请的权限
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        isStarted = false;
        hasPermission = false;
        btnStart = (Button) findViewById(R.id.btn_start);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.setKeepScreenOn(true);
        // 获得SurfaceView的SurfaceHolder
        mSurfaceHolder = mSurfaceView.getHolder();
        // 设置surface不需要自己的维护缓存区
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // 为srfaceHolder添加一个回调监听器
        mSurfacePreview = new MSSurfacePreview(this,this);
        mSurfaceHolder.addCallback(mSurfacePreview);
        btnStart.setOnClickListener(this);

        String dirPath =PathUtils.getRecordDir();
        String filePath = dirPath + FileUtil.getMp4FileName(System.currentTimeMillis());
        mediaMuxer = MSMediaMuxer.newInstance();
        mediaMuxer.initMediaMuxer(filePath);
        Log.d(TAG, "===zhongjihao===oncreat====创建混合器,保存至:" + filePath);

        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            for (int i = 0; i < permissions.length; i++) {
                int result = ContextCompat.checkSelfPermission(this, permissions[i]);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (result != PackageManager.PERMISSION_GRANTED) {
                    hasPermission = false;
                    break;
                } else {
                    hasPermission = true;
                }
            }
            if(!hasPermission){
                // 如果没有授予权限，就去提示用户请求
                ActivityCompat.requestPermissions(this,
                        permissions, TARGET_PERMISSION_REQUEST);
            }
        }
        MSYuvEngineHelper.newInstance().startYuvEngine();
    }

    private void codecToggle() {
        if (isStarted) {
            isStarted = false;
            //停止编码 先要停止编码，然后停止采集
            mediaMuxer.stopEncoder();
            //停止音频采集
            mediaMuxer.stopAudioGather();
            //释放编码器
            mediaMuxer.release();
            mediaMuxer = null;
        } else {
            isStarted = true;
            if(mediaMuxer == null){
                String dirPath =PathUtils.getRecordDir();
                String filePath = dirPath + FileUtil.getMp4FileName(System.currentTimeMillis());
                mediaMuxer = MSMediaMuxer.newInstance();
                mediaMuxer.initMediaMuxer(filePath);
                Log.d(TAG, "===zhongjihao===onclick===创建混合器,保存至:" + filePath);
            }

            //采集音频
            mediaMuxer.startAudioGather();
            //初始化音频编码器
            mediaMuxer.initAudioEncoder();
            //初始化视频编码器
            mediaMuxer.initVideoEncoder(width, height, frameRate);
            //启动编码
            mediaMuxer.startEncoder();
        }
        btnStart.setText(isStarted ? "停止" : "开始");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isStarted) {
            isStarted = false;
            if(mediaMuxer != null){
                //停止编码 先要停止编码，然后停止采集
                mediaMuxer.stopEncoder();
                //停止音频采集
                mediaMuxer.stopAudioGather();
                //释放编码器
                mediaMuxer.release();
                mediaMuxer = null;
            }
        }
        MSVideoChannel.getInstance().doStopCamera();
        MSYuvEngineHelper.newInstance().stopYuvEngine();
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

    @Override
    public void cameraHasOpened() {
        MSVideoChannel.getInstance().doStartPreview(this, mSurfaceHolder);
    }

    @Override
    public void cameraHasPreview(int width,int height,int fps) {
        this.width = width;
        this.height = height;
        this.frameRate = fps;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
            if(requestCode == TARGET_PERMISSION_REQUEST){
                btnStart.setEnabled(true);
                hasPermission = true;
                // 打开摄像头
                MSVideoChannel.getInstance().doOpenCamera(this);
            }
        }else{
            btnStart.setEnabled(false);
            hasPermission = false;
        }
    }

    @Override
    public boolean hasPermission(){
        return  hasPermission;
    }
}
