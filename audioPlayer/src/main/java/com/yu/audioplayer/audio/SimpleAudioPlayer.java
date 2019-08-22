package com.yu.audioplayer.audio;

import android.text.TextUtils;

import com.yu.audioplayer.audio.listener.MediaErrorListener;

public class SimpleAudioPlayer {
    static {
        System.loadLibrary("music-player");
    }

    /**
     * url 可以是本地文件路径，也可以是 http 链接
     */
    private String url;

    private MediaErrorListener mErrorListener;

    public void setOnErrorListener(MediaErrorListener mErrorListener) {
        this.mErrorListener = mErrorListener;
    }

    // called from jni
    private void onError(int code, String msg) {
        if (mErrorListener != null) {
            mErrorListener.onError(code, msg);
        }
    }

    public void setDataSource(String url) {
        this.url = url;
    }

    public void play() {
        if (TextUtils.isEmpty(url)) {
            throw new NullPointerException("url is null, please call method setDataSource");
        }
        nativePlayer(url);
    }

    private native void nativePlayer(String url);
}
