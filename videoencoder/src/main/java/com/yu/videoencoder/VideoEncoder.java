package com.yu.videoencoder;

public class VideoEncoder {

    static {
        System.loadLibrary("native-invoke");
    }

    public native void initialize(String outPath, int width, int height, int bitRate);

    public native void encode(byte[] buffer, int length);

    public native void stopEncode();
}
