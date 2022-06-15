package com.meishe.msmediacodec.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import com.meishe.msmediacodec.MSPlayer;
import com.meishe.msmediacodec.R;
import com.meishe.msmediacodec.databinding.ActivityMainBinding;
import com.meishe.msmediacodec.databinding.ActivityMsplayerBinding;
import com.meishe.msmediacodec.utils.Constants;

import java.io.File;

/**
 * @author ms
 * 播放页面
 */
public class MSPlayerActivity extends AppCompatActivity implements MSPlayer.OnPreparedListener, SeekBar.OnSeekBarChangeListener, MSPlayer.OnPlayStateCallback {


    private ActivityMsplayerBinding mBinding;

    private MSPlayer mMSPlayer;
    /**
     * 获取native层的总时长
     */
    private int duration;
    private boolean isTouch;
    private int mPlayState = 0;
    private String mVideoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBinding = ActivityMsplayerBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        initListener();
        mBinding.seekBar.setOnSeekBarChangeListener(this);
        Intent intent = getIntent();
        if (intent!=null){
            mVideoPath=intent.getStringExtra(Constants.INTENT_KEY_VIDEO_PATH);
        }
        initPlayer();
        mBinding.llTopContainer.setVisibility(View.VISIBLE);
        mBinding.llBottomContainer.setVisibility(View.VISIBLE);
    }

    private void initListener() {
        mBinding.playLayout.setVisibility(View.GONE);
        mBinding.btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayState==0){
                    if (mMSPlayer != null) {
                        mMSPlayer.start();
                    }
                }else{
                    if (mMSPlayer != null) {
                        mMSPlayer.stop();
                    }
                }

            }
        });

        mBinding.surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSeekView();
            }
        });

    }

    private void showSeekView() {
//        mBinding.llTopContainer.setVisibility(View.VISIBLE);
//        mBinding.llBottomContainer.setVisibility(View.VISIBLE);
//        mBinding.llBottomContainer.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mBinding.llTopContainer.setVisibility(View.GONE);
//                mBinding.llBottomContainer.setVisibility(View.GONE);
//            }
//        }, 6000);
    }

    private void initPlayer() {
        mMSPlayer = new MSPlayer();
        mMSPlayer.setSurfaceView(mBinding.surfaceView);
        mMSPlayer.setOnPreparedListener(MSPlayerActivity.this);

        mMSPlayer.setOnErrorListener(new MSPlayer.OnErrorListener() {
            @Override
            public void onError(String errorCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MSPlayerActivity.this, "error:" + errorCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });



        if (TextUtils.isEmpty(mVideoPath)){
            mVideoPath = new File(Environment.
                    getExternalStorageDirectory() +
//                File.separator + "fly.mp4")
                    File.separator + "demo.mp4")
                    .getAbsolutePath();
        }
        mMSPlayer.setDataSource(mVideoPath);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;


        Bitmap videoThumb = getVideoThumbnail(mVideoPath, screenWidth,
                screenHeight, MediaStore.Video.Thumbnails.MINI_KIND);

        mBinding.surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (holder == null) {
                    return;
                }
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                /*先锁住画布*/
                Canvas canvas = holder.lockCanvas();
                canvas.drawBitmap(videoThumb, 0, 0, paint);
                holder.unlockCanvasAndPost(canvas);

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });

        mMSPlayer.setOnOnProgressListener(new MSPlayer.OnProgressListener() {
            @Override
            public void onProgress(int progress) {
                //  C++层吧audio_time时间搓传递上来
                if (!isTouch) {
                    // C++层是异步线程调用上来的
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            if (duration != 0) {
                                //播放信息 动起来
                                // progress:C++层 ffmpeg获取的当前播放【时间（单位是秒 80秒都有，肯定不符合界面的显示） -> 1分20秒】
                                mBinding.currentPlaytime.setText(getMinutes(progress) + ":" + getSeconds(progress));

                                // 拖动条 动起来 seekBar相对于总时长的百分比
                                // progress == C++层的 音频时间搓  ----> seekBar的百分比
                                // seekBar.setProgress(progress * 100 / duration 以秒计算seekBar相对总时长的百分比);
                                mBinding.seekBar.setProgress(progress * 100 / duration);
                            }
                        }
                    });
                }
            }
        });

        mMSPlayer.setOnPlayStateCallback(this);
        mMSPlayer.prepare();
    }

    @Override
    public void onPrepared() {

        duration = mMSPlayer.getDuration();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (duration != 0) {
                    mBinding.totalDuration.setText(getMinutes(duration) + ":" + getSeconds(duration));
                }

                Log.d("lpf", "init success");
                if (mMSPlayer != null) {
                    mMSPlayer.start();
                }
            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mMSPlayer != null) {
            mMSPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMSPlayer != null) {
            mMSPlayer.release();
        }
    }

    ///////////////////////////进度条//////////////////////////////////
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            // progress 是进度条的进度 （0 - 100） ------>   秒 分 的效果
            mBinding.currentPlaytime.setText(getMinutes(progress * duration / 100)
                    + ":" +
                    getSeconds(progress * duration / 100));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        isTouch = false;
        /*获取当前seekbar当前进度*/
        int seekBarProgress = seekBar.getProgress();
        // SeekBar1~100  -- 转换 -->  C++播放的时间（61.546565）
        int playProgress = seekBarProgress * duration / 100;
        mMSPlayer.seek(playProgress);
    }

    /*给我一个duration，转换成xxx分钟*/
    private String getMinutes(int duration) {
        int minutes = duration / 60;
        if (minutes <= 9) {
            return "0" + minutes;
        }
        return "" + minutes;
    }

    /*给我一个duration，转换成xxx秒*/
    private String getSeconds(int duration) { //
        int seconds = duration % 60;
        if (seconds <= 9) {
            return "0" + seconds;
        }
        return "" + seconds;
    }


    /**
     * 获取视频的第一帧缩略图
     * 先通过ThumbnailUtils来创建一个视频的缩略图，然后再利用ThumbnailUtils来生成指定大小的缩略图。
     * 如果想要的缩略图的宽和高都小于MICRO_KIND，则类型要使用MICRO_KIND作为kind的值，这样会节省内存。
     *
     * @param videoPath 视频的路径
     * @param width     指定输出视频缩略图的宽度
     * @param height    指定输出视频缩略图的高度度
     * @param kind      参照MediaStore.Images(Video).Thumbnails类中的常量MINI_KIND和MICRO_KIND。
     *                  其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96
     * @return 指定大小的视频缩略图
     */
    public static Bitmap getVideoThumbnail(String videoPath, int width, int height, int kind) {
        Bitmap bitmap = null;
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        /*调用ThumbnailUtils类的静态方法createVideoThumbnail获取视频的截图；*/
        if (bitmap != null) {
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                    /*调用ThumbnailUtils类的静态方法extractThumbnail将原图片（即上方截取的图片）转化为指定大小；*/
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        return bitmap;
    }


    /**
     * 获取视频文件第一帧图
     *
     * @param path 视频文件的路径
     * @return Bitmap 返回获取的Bitmap
     */
    public static Bitmap getVideoThumb(String path) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(path);
        return media.getFrameAtTime();
    }

    /**
     * 播放状态回调
     *
     * @param state 0：停止  1；播放
     */
    @Override
    public void onPlayStateChange(int state) {
        Log.d("lpf", "state=" + state);
        mPlayState = state;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPlayState==0){
                    //暂停状态
                    mBinding.btnPlay.setBackgroundResource(R.mipmap.icon_edit_play);
                }else{
                    mBinding.btnPlay.setBackgroundResource(R.mipmap.icon_edit_pause);
                }
            }
        });
    }
}