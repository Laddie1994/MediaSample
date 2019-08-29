package com.yu.audioplayer;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.yu.audioplayer.audio.SimpleAudioPlayer;
import com.yu.audioplayer.audio.listener.MediaErrorListener;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    File mMusicFile = new File(Environment.getExternalStorageDirectory(), "test.mp4");
    private SimpleAudioPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e("TAG", "file is exist: " + mMusicFile.exists());
        Log.e("TAG", "file path: " + mMusicFile.getAbsolutePath());

        mPlayer = new SimpleAudioPlayer();
        mPlayer.setDataSource(mMusicFile.getAbsolutePath());

        mPlayer.setOnErrorListener(new MediaErrorListener() {
            @Override
            public void onError(int code, String msg) {
                Log.e("TAG", "error code: " + code);
                Log.e("TAG", "error msg: " + msg);
            }
        });

        mPlayer.play();


//        List<Interceptor> interceptors = new ArrayList<>();
//        interceptors.add(new BrideInterceptor());
//        interceptors.add(new RetryAndFollowInterceptor());
//        interceptors.add(new CacheInterceptor());
//
//        RealInterceptorChain request = new RealInterceptorChain(0, interceptors, "request");
//        request.proceed("request");
    }
}
