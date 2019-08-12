package com.yu.opengles_smple;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class FboRenderer {

    public final String mVertexShaderStr = "attribute vec4 v_Position;\n" +
            "    attribute vec2 f_Position;\n" +
            "    varying vec2 ft_Position;\n" +
            "    uniform mat4 v_matrix;" +
            "    void main() {\n" +
            "        ft_Position = f_Position;\n" +
            "        gl_Position = v_matrix * v_Position;\n" +
            "    }";

    public final String mFragmentShaderStr = "precision mediump float;\n" +
            "varying vec2 ft_Position;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main() {\n" +
            "    vec4 nColor = texture2D(sTexture, ft_Position);\n" +
            "    gl_FragColor = nColor + vec4(0.0, 0.0, 0.2, 0.0);" +
            "}";

    /**
     * 顶点坐标
     *
     * @param gl
     * @param config
     */
    private float[] mVertexCoordinate = {
            -1.0f, 1.0f,//top left
            1.0f, 1.0f,//top right
            -1.0f, -1.0f,//bottom left
            1.0f, -1.0f//bottom right
    };

    /**
     * fbo纹理坐标
     */
    private float[] mFragmentCoordinate = {
            0.0f, 0.0f, //top left
            1.0f, 0.0f, //top right
            0.0f, 1.0f, //bottom left
            1.0f, 1.0f //bottom right
    };
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mFragmentBuffer;
    private Context mContext;
    private int mProgram;
    private int vPosition;
    private int fPosition;
    private int vTexture;
    private int mFboId;
    private int mTextureId;
    private int mVboId;
    private int mWidth, mHeight;
    private int vMatrix;
    private float[] mMvpMatrix = new float[16];

    public  FboRenderer(Context context){
        mContext = context;
        //开辟空间
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

        mWidth = Utils.getScreenWidth(context);
        mHeight = Utils.getScreenHeight(context);
        Matrix.orthoM(mMvpMatrix, 0, -1, 1, -1f, 1f, -1f, 1f);
        Matrix.rotateM(mMvpMatrix, 0, 180, 1, 0, 0);
    }

    public void onSurfaceCreated(int width, int height) {
        mWidth = width > 0 ? width : mWidth;
        mHeight = height > 0 ? height : mHeight;

        mProgram = Utils.createProgram(mVertexShaderStr, mFragmentShaderStr);

        vPosition = GLES20.glGetAttribLocation(mProgram, "v_Position");
        fPosition = GLES20.glGetAttribLocation(mProgram, "f_Position");
        vTexture = GLES20.glGetUniformLocation(mProgram, "sTexture");
        vMatrix = GLES20.glGetUniformLocation(mProgram, "v_matrix");

        mVboId = setupVbo();

        mTextureId = setupTexture();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(vTexture, 0);

        //创建和绑定fbo
        int[] fbos = new int[1];
        GLES20.glGenFramebuffers(1, fbos, 0);
        mFboId = fbos[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);

        //开辟fbo空间
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA
                , mWidth, mHeight, 0
                , GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        //绑定纹理到fbo
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER
                , GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D
                , mTextureId, 0);

        //检测fbo是否绑定成功
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("TAG", "fbo bind failure");
        } else {
            Log.e("TAG", "fbo bind success");
        }

        //解邦
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void drawFrame() {
        GLES20.glUseProgram(mProgram);

        //绑定vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

        GLES20.glUniformMatrix4fv(vMatrix, 1, false
                , mMvpMatrix, 0);

        //赋值顶点坐标
        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT
                , false, 8, 0);

        //赋值纹理坐标
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT
                , false, 8, mVertexCoordinate.length * 4);

        //绘制到屏幕
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        //设置纹理环绕方式
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        //设置纹理过滤方式
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        //解邦纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
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

    public int getTextureId(){
        return mTextureId;
    }

    public void bindFbo(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
    }

    public void unbindFbo(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
}
