package com.yu.audioencoder;

public class AudioEncoder {

    static {
        System.loadLibrary("native-invoke");
    }

    public native void initEncoder(int bitRate, int channles, int sampleRate, String outPath);

    public native void startEncoder();

    public native void encodeAudio(byte[] buffer, int length);

    public native void stopEncoder();

}
