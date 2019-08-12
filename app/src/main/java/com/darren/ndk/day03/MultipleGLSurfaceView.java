package com.darren.ndk.day03;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.darren.ndk.day03.opengl.DZGLSurfaceView;

public class MultipleGLSurfaceView extends DZGLSurfaceView {
    private MultipleRenderer mRenderer;

    public MultipleGLSurfaceView(Context context) {
        this(context, null);
    }

    public MultipleGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        mRenderer = new MultipleRenderer(context);
        setRenderer(mRenderer);
    }

    public MultipleRenderer getRenderer() {
        return mRenderer;
    }

    public void setTextureId(int textureId) {
        mRenderer.setTextureId(textureId);
    }
}
