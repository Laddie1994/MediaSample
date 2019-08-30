package com.yu.videoencoder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String permisstions[] = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_CODE = 485;
    private CameraHelper mCameraHelper;
    private SurfaceView mSurfaceView;
    private VideoEncoder mVideoEncode;
    private String outPath = new File(Environment.getExternalStorageDirectory(), "test.flv").getAbsolutePath();
    private SurfaceHolder mSurfaceHolder;
    private int mCamerId = Camera.CameraInfo.CAMERA_FACING_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.surface_view);
        mVideoEncode = new VideoEncoder();
        mCameraHelper = new CameraHelper(this);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mCameraHelper.init(mSurfaceView.getHolder());
        mCameraHelper.setOnPreviewDataCallback(new CameraHelper.CameraDataCallback() {
            @Override
            public void onPreviewData(byte[] data) {
                mVideoEncode.encode(data, data.length);
            }
        });
    }

    private boolean checkPermisstion() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permisstions, REQUEST_CODE);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                mCameraHelper.open(mCamerId);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (checkPermisstion()) {
            mCameraHelper.open(mCamerId);
            int width = mCameraHelper.getPreviewWidth();
            int height = mCameraHelper.getPreviewHight();
            mVideoEncode.initialize(outPath, width, height, 100 * 1024 * 8);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCameraHelper.close();
        mVideoEncode.stopEncode();
    }
}
