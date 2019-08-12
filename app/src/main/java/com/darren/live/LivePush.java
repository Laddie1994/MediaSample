package com.darren.live;

import android.os.Handler;
import android.os.Looper;

public class LivePush {
    static {
        System.loadLibrary("live-push");
    }

    private String mLiveUrl;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public LivePush(String liveUrl) {
        this.mLiveUrl = liveUrl;
    }

    public void initConnect() {
        nInitConnect(mLiveUrl);
    }

    /**
     * 停止推流
     */
    public void stop() {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                nStop();
            }
        });
    }

    public native void nStop();

    /**
     * 初始化连接
     *
     * @param liveUrl 流媒体房间路径
     */
    private native void nInitConnect(String liveUrl);

    /**
     * 初始化打开连接失败
     * Called from jni
     */
    private void onConnectError(int errorCode, String errMsg) {
        stop();
        if (mConnectListener != null) {
            mConnectListener.onError(errorCode, errMsg);
        }
    }

    /**
     * 初始化打开连接成功
     * Called from jni
     */
    private void onConnectSuccess() {
        if (mConnectListener != null) {
            mConnectListener.onSuccess();
        }
    }

    private ConnectListener mConnectListener;

    public void setOnConnectListener(ConnectListener mConnectListener) {
        this.mConnectListener = mConnectListener;
    }

    public interface ConnectListener {
        void onError(int errorCode, String errMsg);

        void onSuccess();
    }
}
