package com.darren.ndk.day03;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.darren.ndk.day03.opengl.DZGLSurfaceView;

public class BitmapGLSurfaceView extends DZGLSurfaceView {
    private BitmapRenderer mRenderer;

    public BitmapGLSurfaceView(Context context) {
        super(context);
    }

    public BitmapGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        mRenderer = new BitmapRenderer(context);
        setRenderer(mRenderer);
    }

    public BitmapRenderer getRenderer() {
        return mRenderer;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mRenderer.setViewHeight(getMeasuredHeight());
        mRenderer.setViewWidth(getMeasuredWidth());
    }
}
