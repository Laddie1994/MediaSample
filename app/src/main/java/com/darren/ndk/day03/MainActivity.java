package com.darren.ndk.day03;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.darren.ndk.day03.camera.CameraRenderActivity;
import com.darren.ndk.day03.record.VideoRecorderActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void renderBitmap(View view) {
        Intent intent = new Intent(this, BitmapRenderActivity.class);
        startActivity(intent);
    }

    public void multipleRender(View view) {
        Intent intent = new Intent(this, MultipleRenderActivity.class);
        startActivity(intent);
    }

    public void cameraRender(View view) {
        Intent intent = new Intent(this, CameraRenderActivity.class);
        startActivity(intent);
    }

    public void recordVideo(View view) {
        Intent intent = new Intent(this, VideoRecorderActivity.class);
        startActivity(intent);
    }
}