package com.yu.audioencoder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int BIT_RATE = 128 * 1024;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNLES_LAYOUT = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final String[] PERMISSTION_GROUP = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSTION_REQUEST_CODE = 223;
    private boolean isRecording;

    private AudioThread mAudioThread;
    private String mOutPath = new File(Environment.getExternalStorageDirectory(), "encode_aac.aac").toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onStart(View view) {
        if (checkPermisstion()) {
            if (isRecording) {
                stopAudio();
            } else {
                startAudio();
            }
            isRecording = !isRecording;
            ((Button) view).setText(isRecording ? "stop" : "start");
        }
    }

    private void startAudio() {
        if (mAudioThread == null) {
            mAudioThread = new AudioThread(BIT_RATE, SAMPLE_RATE, CHANNLES_LAYOUT, AUDIO_FORMAT, mOutPath);
        }
        mAudioThread.start();
    }

    private void stopAudio() {
        if (mAudioThread == null) {
            return;
        }
        mAudioThread.requestExit();
        mAudioThread = null;
    }

    private boolean checkPermisstion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, PERMISSTION_GROUP, PERMISSTION_REQUEST_CODE);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSTION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startAudio();
            }
        }
    }

    private static class AudioThread extends Thread {
        private int minBufferSize;
        private boolean mShouldExit;
        private byte[] mAudioBuffer;
        private AudioRecord mAudioRecord;
        private final AudioEncoder mAudioEncoder;

        public AudioThread(int bitRate, int sampleRate, int channleLayout, int format, String outPath) {
            minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channleLayout, format);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                    , sampleRate
                    , channleLayout
                    , format, minBufferSize * 2);
            mAudioEncoder = new AudioEncoder();
            mAudioEncoder.initEncoder(bitRate, channleLayout, sampleRate, outPath);
        }

        @Override
        public void run() {
            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                return;
            }
            mAudioRecord.startRecording();
            mAudioEncoder.startEncoder();
            int length;
            while (true) {
                if (mShouldExit) {
                    release();
                    break;
                }
                mAudioBuffer = new byte[minBufferSize];
                length = mAudioRecord.read(mAudioBuffer, 0, minBufferSize);
                if (length > 0) {
                    mAudioEncoder.encodeAudio(mAudioBuffer, mAudioBuffer.length);
                }
            }
        }

        private void release() {
            try {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioEncoder.stopEncoder();
                join(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void requestExit() {
            mShouldExit = true;
        }
    }
}
