package com.yu.opengles_smple;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class SimpleGlSurfaceView extends GLSurfaceView {
    public SimpleGlSurfaceView(Context context) {
        super(context, null);
    }

    public SimpleGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setRenderer(new ImageRenderer(context, R.mipmap.zly));
        // 设置是否持续渲染
        // 渲染一次（RENDERMODE_WHEN_DIRTY）
        // 循环渲染（RENDERMODE_CONTINUOUSLY）
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


}
