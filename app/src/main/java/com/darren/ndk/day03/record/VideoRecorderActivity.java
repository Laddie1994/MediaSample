package com.darren.ndk.day03.record;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.darren.ndk.day03.R;
import com.darren.ndk.day03.camera.widget.CameraFocusView;
import com.darren.ndk.day03.camera.widget.CameraView;
import com.darren.ndk.day03.record.widget.RecordProgressButton;

public class VideoRecorderActivity extends AppCompatActivity {
    private CameraView mCameraView;
    private DefaultVideoRecorder mVideoRecorder;
    private CameraFocusView mFocusView;
    private RecordProgressButton mRecordButton;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_render);
        mCameraView = findViewById(R.id.surface_view);
        mFocusView = findViewById(R.id.camera_focus_view);
        mRecordButton = findViewById(R.id.record_button);
        mRecordButton.setMaxProgress(60000);

        mRecordButton.setOnRecordListener(new RecordProgressButton.RecordListener() {
            @Override
            public void onStart() {
                mVideoRecorder = new DefaultVideoRecorder(VideoRecorderActivity.this, mCameraView.getEglContext(),
                        mCameraView.getTextureId());
                mVideoRecorder.initVideoParams(Environment.getExternalStorageDirectory().getAbsolutePath() + "/live_pusher.mp4",
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/01.mp4",
                        720, 1280);
                mVideoRecorder.startRecord();
                mVideoRecorder.setOnRecordInfoListener(new BaseVideoRecorder.RecordInfoListener() {
                    @Override
                    public void onTime(long times) {
                        mRecordButton.setCurrentProgress((int)times);
                    }
                });
            }

            @Override
            public void onEnd() {
                mVideoRecorder.stopRecord();
            }
        });
        mCameraView.setOnFocusListener(new CameraView.FocusListener() {
            @Override
            public void beginFocus(int x, int y) {
                mFocusView.beginFocus(x, y);
            }

            @Override
            public void endFocus(boolean success) {
                mFocusView.endFocus(true);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraView.onDestroy();
        if (mVideoRecorder != null) {
            mVideoRecorder.stopRecord();
        }
    }
}