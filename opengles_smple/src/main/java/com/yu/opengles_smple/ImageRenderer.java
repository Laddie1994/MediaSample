package com.yu.opengles_smple;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ImageRenderer implements GLSurfaceView.Renderer {

    private float[] mVertexCoordinate = {
            -1f, 1f,
            -1f, -1f,
            1f, 1f,
            1f, -1f
    };

    private float[] mFragmentCoordinate = {
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
    };

    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private Context mContext;
    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mFragmentBuffer;
    private int mProgram;
    private int mTextureId;
    private int vTexture;
    private int vMatrix;
    private int vPosition;
    private int vCoordinate;
    private final Bitmap mBitmap;

    public ImageRenderer(Context context, int imageRes) {
        mContext = context;

        mBitmap = BitmapFactory.decodeResource(mContext.getResources(), imageRes);

        mVertexBuffer = ByteBuffer.allocateDirect(mVertexCoordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mVertexCoordinate);
        mVertexBuffer.position(0);

        mFragmentBuffer = ByteBuffer.allocateDirect(mFragmentCoordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mFragmentCoordinate);
        mFragmentBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //加载顶点着色器
        String vertextSource = Utils.getGLResource(mContext, R.raw.image_vertex_shader);
        //加载纹理着色器
        String fragmentSource = Utils.getGLResource(mContext, R.raw.image_fragment_shader);
        //编译GLES程序
        mProgram = Utils.createProgram(vertextSource, fragmentSource);
        //开启深度测试
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        vMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        vTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
        vPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        vCoordinate = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float bitRadio = mBitmap.getWidth() / (mBitmap.getHeight() * 1.0f);
        float radio = width / (height * 1.0f);
        if (width > height) {
            if (bitRadio > radio) {
                Matrix.orthoM(mProjectMatrix, 0
                        , -radio * bitRadio
                        , radio * bitRadio
                        , -1
                        , 1
                        , -3, 7);
            } else {
                Matrix.orthoM(mProjectMatrix, 0
                        , -radio / bitRadio
                        , radio / bitRadio
                        , -1
                        , 1
                        , -3, 7);
            }
        } else {
            if (bitRadio > radio) {
                Matrix.orthoM(mProjectMatrix, 0
                        , -1
                        , 1
                        , -1 / radio * bitRadio
                        , 1 / radio * bitRadio
                        , -3, 7);
            } else {
                Matrix.orthoM(mProjectMatrix, 0
                        , -1
                        , 1
                        , -bitRadio / radio
                        , bitRadio / radio
                        , -3, 7);
            }
        }

        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mMVPMatrix, 0);

        GLES20.glUniform1i(vTexture, 1);

        mTextureId = loadTextur();

        Log.e("TAG", "onDrawFrame mTextureId=" + mTextureId);

        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, mVertexBuffer);

        GLES20.glEnableVertexAttribArray(vCoordinate);
        GLES20.glVertexAttribPointer(vCoordinate, 2, GLES20.GL_FLOAT, false, 0, mFragmentBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * 加载纹理
     *
     * @return
     */
    private int loadTextur() {
        if (mBitmap == null || mBitmap.isRecycled()){
            return 0;
        }

        int[] texture = new int[1];
        //生成纹理
        GLES20.glGenTextures(1, texture, 0);
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        // 设置纹理过滤方式
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        // 设置纹理环绕方式
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        //根据设置的参数生成纹理
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
        return texture[0];
    }
}
