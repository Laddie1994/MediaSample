package com.yu.videorenderer;

import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Debug;
import android.os.Environment;
import android.support.v4.os.TraceCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    String path = new File(Environment.getExternalStorageDirectory(), "test.mp4").getAbsolutePath();
    SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surface_view);
        final int width = getResources().getDisplayMetrics().widthPixels;
        final int height = getResources().getDisplayMetrics().heightPixels;
        surfaceView.post(new Runnable() {
            @Override
            public void run() {
                VideoRenderer.nativeRenderer(path, surfaceView.getHolder().getSurface(), width, height);
            }
        });

    }
}
