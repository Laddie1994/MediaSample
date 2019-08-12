package com.yu.opengles_smple;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private EGLContext mEglContext;
    private CameraRenderer mRenderer;
    private CameraHelper mHelper;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    public CameraView(Context context) {
        super(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //获取eglContext
        setEGLContextFactory(new EGLContextFactory() {
            @Override
            public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
                int[] attrList = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
                mEglContext = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrList);
                return mEglContext;
            }

            @Override
            public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
                egl.eglDestroyContext(display, context);
            }
        });
        //设置使用的版本
        setEGLContextClientVersion(2);
        mRenderer = new CameraRenderer(context);
        mHelper = new CameraHelper(context);
        setRenderer(this);
    }

    /**
     * 旋转相机的角度
     */
    private void rotateCameraAngle() {
        mRenderer.resetMatrix();
        // 前置摄像头
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mRenderer.flipMatrix(true, false);
            mRenderer.rotateMatrix(0, 90);
        }
        // 后置摄像头
        else if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mRenderer.rotateMatrix(0, 270);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mHelper.setViewHeight(getMeasuredHeight());
        mHelper.setViewWidth(getMeasuredWidth());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mRenderer.onSurfaceCreated(gl, config);
        SurfaceTexture surfaceTexture = mRenderer.getSurfaceTexture();
        mHelper.init(surfaceTexture);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
        mHelper.open(mCameraId);
        mRenderer.setPreviewSize(mHelper.getPreviewWidth(), mHelper.getPreviewHight());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mRenderer.onSurfaceChanged(gl, width, height);
        rotateCameraAngle();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mRenderer.onDrawFrame(gl);
    }

    public EGLContext getEGLContext(){
        return mEglContext;
    }

    public int getTextureId(){
        return mRenderer.getTextureId();
    }
}
