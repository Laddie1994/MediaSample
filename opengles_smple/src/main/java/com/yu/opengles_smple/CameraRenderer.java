package com.yu.opengles_smple;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRenderer implements GLSurfaceView.Renderer {

    private static final String VERTEX =
            "attribute vec2 aTextureCoord;\n" +
            "attribute vec4 aVertexCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform mat4 uVertexMatrix;\n" +
            "uniform mat4 vCoordMatrix;\n" +
            "void main(){\n" +
            "    vTextureCoord = aTextureCoord;\n" +
            "    gl_Position = uVertexMatrix * aVertexCoord;\n" +
            "}";

    private static final String FRAGMENT =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "void main(){\n" +
            "    vec4 nColor = texture2D(uTexture, vTextureCoord);\n" +
            "    gl_FragColor = nColor;\n" +
            "}";

    /**
     * 顶点坐标
     */
    private float[] mVertexCoordinate = {
            -1.0f, 1.0f,//top left
            -1.0f, -1.0f,//top right
            1.0f, 1.0f,//bottom left
            1.0f, -1.0f//bottom right
    };

    /**
     * 纹理坐标
     */
    private float[] mFragmentCoordinate = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };

    /**
     * 纹理矩阵
     */
    private float[] mFragmentMatrix = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    private Context mContext;
    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mFragmentBuffer;
    private int mProgram;
    private int mVboId;
    private int mTextureId;
    private SurfaceTexture mCameraTexture;
    private int aTextureCoordLocation;
    private int aVertexCoordLocation;
    private int uVertexMatrix;
    private int vCoordMatrix;
    private float[] mMVPMatrix = new float[16];
    private int mPreviewWidth, mPreviewHeight;
    private int mWidth, mHeight;
    private int uTexture;
    private  FboRenderer mFboRenderer;

    public CameraRenderer(Context context) {
        mContext = context;

        //开辟顶点坐标空间
        mVertexBuffer = ByteBuffer.allocateDirect(mVertexCoordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mVertexCoordinate);
        mVertexBuffer.position(0);

        //开辟纹理坐标空间
        mFragmentBuffer = ByteBuffer.allocateDirect(mFragmentCoordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mFragmentCoordinate);
        mFragmentBuffer.position(0);

        mFboRenderer = new FboRenderer(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mProgram = Utils.createProgram(VERTEX, FRAGMENT);
        mVboId = setupVbo();
        mTextureId = setupTexture();

        mFboRenderer.onSurfaceCreated(mPreviewWidth, mPreviewHeight);

        mCameraTexture = new SurfaceTexture(mTextureId);

        aTextureCoordLocation = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        aVertexCoordLocation = GLES20.glGetAttribLocation(mProgram, "aVertexCoord");
        uVertexMatrix = GLES20.glGetUniformLocation(mProgram, "uVertexMatrix");
        vCoordMatrix = GLES20.glGetUniformLocation(mProgram, "vCoordMatrix");
        uTexture = GLES20.glGetUniformLocation(mProgram, "uTexture");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        GLES20.glViewport(0, 0, width, height);
        computeMatrix();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mFboRenderer.bindFbo();
        //加载program
        GLES20.glUseProgram(mProgram);
        mCameraTexture.updateTexImage();

        //设置正交投影参数
        GLES20.glUniformMatrix4fv(uVertexMatrix, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(vCoordMatrix, 1, false, mFragmentMatrix, 0);

        //绑定vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId);
        //赋值顶点坐标
        GLES20.glEnableVertexAttribArray(aVertexCoordLocation);
        GLES20.glVertexAttribPointer(aVertexCoordLocation, 2, GLES20.GL_FLOAT
                , false, 8, 0);
        //赋值纹理坐标
        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT
                , false, 8, mVertexCoordinate.length * 4);
        //解邦vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        //绑定纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, mTextureId);
        //绘制到屏幕
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        //解邦纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, 0);

        mFboRenderer.unbindFbo();
        mFboRenderer.drawFrame();

    }

    /**
     * 计算坐标矩阵
     */
    private void computeMatrix() {
        if (mWidth <= 0 || mHeight <= 0 || mPreviewWidth <= 0 || mPreviewHeight <= 0) {
            return;
        }
        float sRatio = (float) mWidth / mHeight;
        float pRatio = (float) mPreviewWidth / mPreviewHeight;
        float[] projection = new float[16];
        float[] camera = new float[16];
        //设置正交投影
        if (pRatio > sRatio) {
            Matrix.orthoM(projection, 0, -sRatio / pRatio, sRatio / pRatio, -1, 1, 1, 3);
        } else {
            Matrix.orthoM(projection, 0, -1, 1, -pRatio / sRatio, pRatio / sRatio, 1, 3);
        }
        //设置相机位置
        Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, projection, 0, camera, 0);
    }

    /**
     * 设置纹理
     *
     * @return
     */
    private int setupTexture() {
        int[] textures = new int[1];
        //生成纹理
        GLES20.glGenTextures(1, textures, 0);
        int textureId = textures[0];
        //绑定纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, textureId);
        //设置纹理环绕方式
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        //设置纹理过滤方式
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        //解邦纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, 0);
        return textureId;
    }

    /**
     * 设置vbo
     *
     * @return
     */
    private int setupVbo() {
        int[] vbos = new int[1];
        //创建vbo
        GLES20.glGenBuffers(1, vbos, 0);
        //绑定vbo
        int vboId = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        //开辟vbo空间
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER
                , (mFragmentCoordinate.length + mVertexCoordinate.length) * 4
                , null, GLES20.GL_STATIC_DRAW);
        //缓存顶点坐标
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0
                , mVertexCoordinate.length * 4, mVertexBuffer);

        //缓存纹理坐标
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER
                , mVertexCoordinate.length * 4
                , mFragmentCoordinate.length * 4
                , mFragmentBuffer);
        //解邦vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        return vboId;
    }

    /**
     * 重置matrix
     */
    public void resetMatrix() {
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    /**
     * 翻转matrix
     *
     * @param x 沿x轴翻转
     * @param y 沿y轴翻转
     */
    public void flipMatrix(boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(mMVPMatrix, 0, x ? -1 : 1, y ? -1 : 1, 1);
        }
    }

    /**
     * 旋转matrix
     *
     * @param offset 偏移
     * @param angle  角度
     */
    public void rotateMatrix(int offset, float angle) {
        Matrix.rotateM(mMVPMatrix, offset, angle, 0, 0, 1);
    }

    /**
     * 设置预览大小
     *
     * @param previewWidth
     * @param previewHight
     */
    public void setPreviewSize(int previewWidth, int previewHight) {
        mPreviewHeight = previewHight;
        mPreviewWidth = previewWidth;
        //获取状态栏高度
        computeMatrix();
    }

    public SurfaceTexture getSurfaceTexture() {
        return mCameraTexture;
    }

    public int getTextureId(){
        return mFboRenderer.getTextureId();
    }
}
