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
    private RecordDelegate mRecordDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.camera_view);
    }

    public void onStart(View v) {
        if (mRecordDelegate == null){
            mRecordDelegate = new RecordDelegate(this, mCameraView.getEGLContext(), mCameraView.getTextureId());
            String outPath = new StringBuilder(Environment.getExternalStorageDirectory().getAbsolutePath())
                    .append(File.separator)
                    .append("test.mp4")
                    .toString();
            mRecordDelegate.initParams(outPath
                    ,mCameraView.getWidth(), mCameraView.getHeight());
        }

        Button mActiveBtn = (Button) v;
        if (isStarting) {
            isStarting = false;
            mActiveBtn.setText("开始");
            mRecordDelegate.stopRecord();

        } else {
            isStarting = true;
            mActiveBtn.setText("停止");
            mRecordDelegate.startRecord();
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
