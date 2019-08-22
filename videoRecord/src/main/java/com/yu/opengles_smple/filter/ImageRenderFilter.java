package com.yu.opengles_smple.filter;

import android.opengl.GLES20;
import android.util.Log;

import com.yu.opengles_smple.Utils;

public class ImageRenderFilter {

    private static final String TAG = ImageRenderFilter.class.getSimpleName();

    private static final String DEFAULT_VERTEX =
            "attribute vec2 aTextureCoord;\n" +
                    "attribute vec4 aVertexCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform mat4 uVertexMatrix;\n" +
                    "uniform mat4 vCoordMatrix;\n" +
                    "void main(){\n" +
                    "    vTextureCoord = aTextureCoord;\n" +
                    "    gl_Position = uVertexMatrix * aVertexCoord;\n" +
                    "}";

    private static final String DEFAULT_FRAGMENT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES uTexture;\n" +
                    "void main(){\n" +
                    "    vec4 nColor = texture2D(uTexture, vTextureCoord);\n" +
                    "    gl_FragColor = nColor;\n" +
                    "}";

    private RenderCoordinate mRenderCoordinate;
    private int mProgram;
    private int aTextureCoordLocation;
    private int aVertexCoordLocation;
    private int uVertexMatrix;
    private int vCoordMatrix;
    private int uTexture;

    public ImageRenderFilter(){
        mRenderCoordinate = new RenderCoordinate();
    }

    public void initialize(){
        mProgram = Utils.createProgram(getVertexCode(), getFragmentCode());

        initVertextArguments();
        initFragmentArguments();
    }

    protected void initFragmentArguments() {
        uTexture = GLES20.glGetUniformLocation(mProgram, "uTexture");
    }

    protected void initVertextArguments() {
        aTextureCoordLocation = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        aVertexCoordLocation = GLES20.glGetAttribLocation(mProgram, "aVertexCoord");
        uVertexMatrix = GLES20.glGetUniformLocation(mProgram, "uVertexMatrix");
        vCoordMatrix = GLES20.glGetUniformLocation(mProgram, "vCoordMatrix");
    }

    protected void setupVertextArguments(){
        //赋值顶点坐标
        GLES20.glEnableVertexAttribArray(aVertexCoordLocation);
        GLES20.glVertexAttribPointer(aVertexCoordLocation, 2, GLES20.GL_FLOAT
                , false, 8, 0);
    }

    protected void setupFragmentArguments(){
        //赋值纹理坐标
        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT
                , false, 8, mRenderCoordinate.getVertextCoordinate().length * 4);
    }

    public void draw(int textureId, float[] mvpMatrix){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR){
            Log.e(TAG, "GLES init fail");
            return;
        }

        setupVertextArguments();
        setupFragmentArguments();
        GLES20.glUniformMatrix4fv(uVertexMatrix, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(uTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public String getVertexCode(){
        return DEFAULT_VERTEX;
    }

    public String getFragmentCode(){
        return DEFAULT_FRAGMENT;
    }

}
