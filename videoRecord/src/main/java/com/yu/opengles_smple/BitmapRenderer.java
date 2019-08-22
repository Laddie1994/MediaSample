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

public class BitmapRenderer implements GLSurfaceView.Renderer {

    /**
     * 顶点坐标
     *
     * @param context
     */
    private float[] mVertexCoordinate = new float[]{
            -1, -1,//左上
            1, -1,//右上
            -1, 1,//左下
            1, 1//右下
    };
    private final FloatBuffer mVertexBuffer;

    /**
     * 纹理坐标
     *
     * @param context
     */
    private float[] mFragmentCoordinate = new float[]{
            0, 1,//左上
            1, 1,//右上
            0, 0,//左下
            1, 0//右下
    };
    private final FloatBuffer mFragmentBuffer;
    private Context mContext;
    private int mProgram;
    private int vPosition;
    private int fPosition;
    private int mVboId;
    private int mTextureId;
    private float[] matrix = new float[16];
    private int uMatrix;

    public BitmapRenderer(Context context) {
        mContext = context;
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
        String vertexSource = Utils.getGLResource(mContext, R.raw.vertex_shader_matrix);
        String fragmentSource = Utils.getGLResource(mContext, R.raw.fragment_shader);
        mProgram = Utils.createProgram(vertexSource, fragmentSource);
        vPosition = GLES20.glGetAttribLocation(mProgram, "v_Position");
        fPosition = GLES20.glGetAttribLocation(mProgram, "f_Position");
        int sTexture = GLES20.glGetUniformLocation(mProgram, "sTexture");
        uMatrix = GLES20.glGetUniformLocation(mProgram, "u_Matrix");

        int[] vBos = new int[1];
        GLES20.glGenBuffers(1, vBos, 0);
        mVboId = vBos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, (mFragmentCoordinate.length + mVertexCoordinate.length) * 4
                , null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mVertexCoordinate.length * 4, mVertexBuffer);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mVertexCoordinate.length * 4
                , mFragmentCoordinate.length * 4, mFragmentBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        mTextureId = loadTexture(R.mipmap.zly);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(sTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.zly, opts);
        float imageWidth = opts.outWidth;
        float imageHeight = opts.outHeight;

        if (width > height) {
            Matrix.orthoM(matrix, 0, -width / ((height / imageHeight) * imageWidth),
                    width / ((height / imageHeight) * imageWidth), -1f, 1f, -1f, 1f);
        } else {
            Matrix.orthoM(matrix, 0, -1f, 1f, -height / ((width / imageWidth) * imageHeight),
                    height / ((width / imageWidth) * imageHeight), -1f, 1f);
        }

        // fbo 的坐标是标准坐标
//        Matrix.rotateM(matrix, 0, 90, 1, 0, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.e("TAG", "onDrawFrame");
        // 清屏并绘制红色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1f, 0f, 0f, 1f);

        // 激活 program
        GLES20.glUseProgram(mProgram);
        // 绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

        // 设置正交矩阵的值
        GLES20.glUniformMatrix4fv(uMatrix, 1, false, matrix, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId);
        /**
         * 设置坐标
         * 2：2个为一个点
         * GLES20.GL_FLOAT：float 类型
         * false：不做归一化
         * 8：步长是 8
         */
        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, mVertexCoordinate.length * 4);
        // 绘制到屏幕
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        // 解绑
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private int loadTexture(int resourceId) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        int textureId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        // 设置纹理环绕方式
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        // 设置纹理过滤方式
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // 绘制一张图片
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resourceId);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textureId;
    }
}
