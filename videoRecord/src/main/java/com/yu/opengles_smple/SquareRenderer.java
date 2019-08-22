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

public class SquareRenderer implements GLSurfaceView.Renderer {

    /**
     * 顶点坐标
     */
    private float[] mVertexCoordinate = {
            -0.5f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            0.5f, 0.5f, 0.0f
    };
    private short[] mShapeIndex = {
            0, 1, 2, 0, 2, 3
    };
    private Context mContext;
    private FloatBuffer mVertextBuffer;
    private int mProgram;
    private float[] mViewMatrix = new float[16];
    private float[] mProjextMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float[] mShapeColor = {1.0f, 1.0f, 1.0f, 1.0f};
    private final ShortBuffer mShapeBuffer;
    private int vMatrix;
    private int vPosition;
    private int vColor;

    public SquareRenderer(Context context) {
        mContext = context;
        //开辟空间
        mVertextBuffer = ByteBuffer.allocateDirect(mVertexCoordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mVertexCoordinate);
        mVertextBuffer.position(0);

        mShapeBuffer = ByteBuffer.allocateDirect(mShapeIndex.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(mShapeIndex);
        mShapeBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //加载顶点着色器
        String vertextSource = Utils.getGLResource(mContext, R.raw.square_vertex_shader);
        //加载纹理着色器
        String fragmentSource = Utils.getGLResource(mContext, R.raw.square_fragment_shader);
        //编译GLES程序
        mProgram = Utils.createProgram(vertextSource, fragmentSource);
        //连接GLES程序
        GLES20.glLinkProgram(mProgram);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //设置视图窗口
        GLES20.glViewport(0, 0, width, height);
        //计算宽高比
        float ratio = width / (height * 1.0f);
        //透视投影
        Matrix.frustumM(mProjextMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        //相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjextMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //加入GLES程序
        GLES20.glUseProgram(mProgram);
        //清除颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //添加背景色
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        //获取顶点着色器vMatrix
        vMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        //设置vMatrix
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mMVPMatrix, 0);
        //获取顶点着色器vPsition属性
        vPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 3, GLES20.GL_FLOAT, false, 0, mVertextBuffer);

        vColor = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glEnableVertexAttribArray(vColor);
        GLES20.glUniform4fv(vColor, 1, mShapeColor, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mShapeIndex.length, GLES20.GL_UNSIGNED_SHORT, mShapeBuffer);

        GLES20.glDisableVertexAttribArray(vPosition);
    }
}
