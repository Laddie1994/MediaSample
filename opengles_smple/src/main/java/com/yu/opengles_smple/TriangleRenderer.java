package com.yu.opengles_smple;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 三角形Renderer
 */
public class TriangleRenderer implements GLSurfaceView.Renderer {

    /**
     * 顶点坐标
     *
     * @param gl
     * @param config
     */
    private float[] mTriangleCoordinate = new float[]{
            0.5f, 0.5f, 0f,
            -0.5f, -0.5f, 0f,
            0.5f, -0.5f, 0f
    };
    /**
     * 颜色
     * 红、绿、蓝、透明
     */
    private float[] mTriangleColor = {
            0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f};
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mColorBuffer;
    private int mProgram;
    private int vPosition;
    private int aColor;
    private Context mContext;
    private float[] mViewMatrix = new float[16];
    private float[] mProjextMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private int vMatrix;

    public TriangleRenderer(Context context) {
        mContext = context;
        //申请顶点坐标空间
        mVertexBuffer = ByteBuffer.allocateDirect(mTriangleCoordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mTriangleCoordinate);
        mVertexBuffer.position(0);

        //申请颜色空间
        mColorBuffer = ByteBuffer.allocateDirect(mTriangleColor.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mTriangleColor);
        mColorBuffer.position(0);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //加载顶点着色器代码
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER
                , Utils.getGLResource(mContext, R.raw.triangl_vertex_shader));
        //加载纹理着色器代码
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER
                , Utils.getGLResource(mContext, R.raw.triangle_fragment_shader));

        //获取gles编译程序
        mProgram = GLES20.glCreateProgram();
        //加入顶点着色器
        GLES20.glAttachShader(mProgram, vertexShader);
        //加入纹理着色器
        GLES20.glAttachShader(mProgram, fragmentShader);
        //连接着色器程序
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
        //清除背景色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        //绘制背景颜色
        GLES20.glClearColor(0.5f, .5f, .5f, 1f);
        //将GLES程序加入到OpenGlES中
        GLES20.glUseProgram(mProgram);
        //获取的顶点着色器的vMatrix
        vMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        //设在顶点着色器vMatrix值
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mMVPMatrix, 0);
        //获取顶点着色器的vPosition
        vPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        //启用vPosition
        GLES20.glEnableVertexAttribArray(vPosition);
        //准备三角形坐标系
        GLES20.glVertexAttribPointer(vPosition, 3, GLES20.GL_FLOAT, false, 12, mVertexBuffer);
        //获取顶点着色器的aColor
        aColor = GLES20.glGetAttribLocation(mProgram, "aColor");
        //启用aColor
        GLES20.glEnableVertexAttribArray(aColor);
        //设置颜色
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, 0, mColorBuffer);
        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(vPosition);
    }

    /**
     * 加载shader
     *
     * @param shaderType
     * @param source
     * @return
     */
    private int loadShader(int shaderType, String source) {
        //创建shader
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            //设在资源
            GLES20.glShaderSource(shader, source);
            //编译shader
            GLES20.glCompileShader(shader);
            //判断错误
            int[] status = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
            if (status[0] != GLES20.GL_TRUE) {
                Log.e("TAG", "GLES compile error");
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }
}
