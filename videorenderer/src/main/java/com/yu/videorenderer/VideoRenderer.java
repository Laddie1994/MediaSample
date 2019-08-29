package com.yu.videorenderer;

import android.view.Surface;
import android.view.SurfaceView;

public class VideoRenderer {

    static{
        System.loadLibrary("video-renderer");
    }

    public static native void nativeRenderer(String path, Surface surface, int width, int height);

}
