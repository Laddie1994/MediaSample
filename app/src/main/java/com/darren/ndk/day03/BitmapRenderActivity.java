package com.darren.ndk.day03;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.darren.live.LivePush;

public class BitmapRenderActivity extends AppCompatActivity {

    private LivePush mLivePush;
    private BitmapGLSurfaceView mSurfaceView;

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmap_render);
        mSurfaceView = findViewById(R.id.surface_view);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // mLivePush.stop();
    }
}