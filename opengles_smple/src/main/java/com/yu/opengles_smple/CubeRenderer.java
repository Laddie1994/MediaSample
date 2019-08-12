package com.yu.opengles_smple;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CubeRenderer implements GLSurfaceView.Renderer {

    private float[] mVertextCoordinate = {
            -1.0f, 1.0f, 1.0f,    //正面左上0
            -1.0f, -1.0f, 1.0f,   //正面左下1
            1.0f, -1.0f, 1.0f,    //正面右下2
            1.0f, 1.0f, 1.0f,     //正面右上3
            -1.0f, 1.0f, -1.0f,    //反面左上4
            -1.0f, -1.0f, -1.0f,   //反面左下5
            1.0f, -1.0f, -1.0f,    //反面右下6
            1.0f, 1.0f, -1.0f,     //反面右上7
    };

    private short[] mShapeIndex = {
            6, 7, 4, 6, 4, 5,    //后面
            6, 3, 7, 6, 2, 3,    //右面
            6, 5, 1, 6, 1, 2,    //下面
            0, 3, 2, 0, 2, 1,    //正面
            0, 1, 5, 0, 5, 4,    //左面
            0, 7, 3, 0, 4, 7,    //上面
    };

    private float[] mShapeColor = {
            0f, 1f, 0f, 1f,
            0f, 1f, 0f, 1f,
            0f, 1f, 0f, 1f,
            0f, 1f, 0f, 1f,
            1f, 0f, 0f, 1f,
            1f, 0f, 0f, 1f,
            1f, 0f, 0f, 1f,
            1f, 0f, 0f, 1f,
    };

    private Context mContext;
    private int mProgram;
    private final FloatBuffer mVertexBuffer;
    private final ShortBuffer mIndexBuffer;
    private final FloatBuffer mColorBuffer;
    private float[] mViewMatrix = new float[16];
    private float[] mProjextMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private int vMatrix;
    private int vPosition;
    private int aColor;

    public CubeRenderer(Context context) {
        mContext = context;

        mVertexBuffer = ByteBuffer.allocateDirect(mVertextCoordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mVertextCoordinate);
        mVertexBuffer.position(0);

        mIndexBuffer = ByteBuffer.allocateDirect(mShapeIndex.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(mShapeIndex);
        mIndexBuffer.position(0);

        mColorBuffer = ByteBuffer.allocateDirect(mShapeColor.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mShapeColor);
        mColorBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //加载顶点着色器
        String vertextSource = Utils.getGLResource(mContext, R.raw.cube_vertext_shader);
        //加载纹理着色器
        String fragmentSource = Utils.getGLResource(mContext, R.raw.cube_fragment_shader);
        //编译GLES程序
        mProgram = Utils.createProgram(vertextSource, fragmentSource);
        //连接GLES程序
        GLES20.glLinkProgram(mProgram);
        //开启深度测试
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //设置视图窗口
        GLES20.glViewport(0, 0, width, height);
        //计算宽高比
        float ratio = width / (height * 1.0f);
        //透视投影
        Matrix.frustumM(mProjextMatrix, 0, -ratio, ratio, -1, 1, 3, 20);
        //相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 5.0f, 5.0f, 10.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjextMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //加入GLES程序
        GLES20.glUseProgram(mProgram);
        //清除颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        //添加背景色
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        //获取顶点着色器vMatrix
        vMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mMVPMatrix, 0);

        //获取顶点着色器vPsition属性
        vPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);

        //获取顶点着色器的aColor
        aColor = GLES20.glGetAttribLocation(mProgram, "aColor");
        GLES20.glEnableVertexAttribArray(aColor);
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, 0, mColorBuffer);

        //绘制
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mShapeIndex.length, GLES20.GL_UNSIGNED_SHORT, mIndexBuffer);
        GLES20.glDisableVertexAttribArray(vPosition);
    }
}
