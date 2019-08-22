package com.yu.opengles_smple.filter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class RenderCoordinate {

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
    private final FloatBuffer mVertextBuffer;
    private final FloatBuffer mFragmentBuffer;

    public RenderCoordinate(){
        mVertextBuffer = ByteBuffer.allocateDirect(mVertexCoordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mVertexCoordinate);
        mVertextBuffer.position(0);

        mFragmentBuffer = ByteBuffer.allocateDirect(mFragmentCoordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mFragmentCoordinate);
        mFragmentBuffer.position(0);
    }

    public FloatBuffer getVertextBuffer(){
        return mVertextBuffer;
    }

    public FloatBuffer getFragmentBuffer(){
        return mFragmentBuffer;
    }

    public float[] getVertextCoordinate(){
        return mVertexCoordinate;
    }

    public float[] getFragmentCoordinate(){
        return mFragmentCoordinate;
    }

}
