package com.ucoachu.capacitor.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ucoachu.capacitor.R;

import java.io.File;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {
    static final String TAG = "ksc.log";

    private VideoView mVideoView;
    private EditText mEditText;

    private RelativeLayout mLayoutButtonAnalyze;
    private RelativeLayout mLayoutButtonDiscard;
    private TextView mTextButtonAnalyzeLater;

    private String mVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mVideoPath = getIntent().getStringExtra("videoUrl");

        mVideoView = (VideoView) findViewById(R.id.video_view);
        mEditText = (EditText) findViewById(R.id.edit_text_title);

        mLayoutButtonAnalyze = (RelativeLayout) findViewById(R.id.layout_button_analyze);
        mLayoutButtonAnalyze.setOnClickListener(this);
        mLayoutButtonDiscard = (RelativeLayout) findViewById(R.id.layout_button_discard);
        mLayoutButtonDiscard.setOnClickListener(this);
        mTextButtonAnalyzeLater = (TextView) findViewById(R.id.text_button_analyze_later);
        mTextButtonAnalyzeLater.setOnClickListener(this);

        Log.e(TAG, "video url = " + mVideoPath);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mediaController);
        mVideoView.setVideoPath(mVideoPath);
        mVideoView.start();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.layout_button_analyze) {
            finish();
        } else if (v.getId() == R.id.layout_button_discard) {
            finish();
        } else if (v.getId() == R.id.text_button_analyze_later) {
            finish();
        }
    }
}