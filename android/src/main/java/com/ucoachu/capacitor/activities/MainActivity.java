package com.ucoachu.capacitor.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.ucoachu.capacitor.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout mLayoutButtonLandscape;
    private LinearLayout mLayoutButtonPortrait;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLayoutButtonLandscape = (LinearLayout) findViewById(R.id.layout_button_landscape);
        mLayoutButtonLandscape.setOnClickListener(this);

        mLayoutButtonPortrait = (LinearLayout) findViewById(R.id.layout_button_portrait);
        mLayoutButtonPortrait.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.layout_button_landscape) {
            Intent myIntent = new Intent(MainActivity.this, CameraActivity.class);
            myIntent.putExtra("mode", "landscape");
            startActivity(myIntent);
        } else if (v.getId() == R.id.layout_button_portrait) {
            Intent myIntent = new Intent(MainActivity.this, CameraActivity.class);
            myIntent.putExtra("mode", "portrait");
            startActivity(myIntent);
        }
    }
}