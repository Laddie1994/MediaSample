package com.yu.audioplayer.audio.listener;

/**
 * Created by hcDarren on 2019/6/16.
 * 错误回调
 */
public interface MediaErrorListener {
    void onError(int code, String msg);
}
