package com.meishe.msmediacodec.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;

import com.meishe.msmediacodec.R;
import com.meishe.msmediacodec.utils.Constants;

import java.io.IOException;

public class MSEditActivity extends AppCompatActivity {

    private String mVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msedit);

        Intent intent = getIntent();
        if (intent != null) {
            mVideoPath = intent.getStringExtra(Constants.INTENT_KEY_VIDEO_PATH);
        }

        MediaExtractor mediaExtractor=new MediaExtractor();
        try {
            mediaExtractor.setDataSource(mVideoPath);
            int trackCount = mediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.contains("video/")) {
                    Log.e("lpf","视频轨道");
                }else{
                    Log.e("lpf","音频轨道");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}