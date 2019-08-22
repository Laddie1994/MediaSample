package com.yu.opengles_smple;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private CameraView mCameraView;
    private boolean isStarting;
    private VideoRecordDelegate mVideoRecordDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.camera_view);
    }

    public void onStart(View v) {
        Button mActiveBtn = (Button) v;
        if (isStarting) {
            isStarting = false;
            mActiveBtn.setText("开始");
            mVideoRecordDelegate.stopRecord();

        } else {
            isStarting = true;
            mVideoRecordDelegate = new VideoRecordDelegate(this, mCameraView.getEGLContext(), mCameraView.getTextureId());
            String outPath = new StringBuilder(Environment.getExternalStorageDirectory().getAbsolutePath())
                    .append(File.separator)
                    .append("test.mp4")
                    .toString();
            mVideoRecordDelegate.initParams(outPath
                    ,360, 640);
            mActiveBtn.setText("停止");
            mVideoRecordDelegate.startRecord();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.onPause();
    }
}
