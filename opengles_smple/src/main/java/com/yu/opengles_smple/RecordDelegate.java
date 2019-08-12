package com.yu.opengles_smple;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

public class RecordDelegate {

    private MediaMuxer mMediaMuxer;
    private MediaCodec mMediaCodec;
    private Surface mMediaSurface;
    private int mVideoWidth, mVideoHeight;
    private Context mContext;
    private EGLContext mEglContext;
    private GLSurfaceView.Renderer mRenderer;
    private VideoEncoderThread mVideoEncoderThread;
    private VideoRenderThread mRenderThread;

    public RecordDelegate(Context context, EGLContext eglContext, int textureId){
        mContext = context;
        mEglContext = eglContext;
        this.mRenderer = new RecorderRenderer(mContext, textureId);
    }

    public void initParams(String outPath, int videoWdth, int videoHeight) {
        try {
            this.mVideoWidth = videoWdth % 2 == 0 ? videoWdth : videoWdth - videoWdth % 2;
            this.mVideoHeight = videoHeight % 2 == 0 ? videoHeight : videoHeight - videoHeight % 2;
            this.mMediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initMediaCodec();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", e.toString());
        }
    }

    private void initMediaCodec() throws Exception {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mVideoWidth, mVideoHeight);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoWidth * mVideoHeight * 4);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mMediaSurface = mMediaCodec.createInputSurface();

        this.mRenderThread = new VideoRenderThread(this);

        this.mVideoEncoderThread = new VideoEncoderThread(this);

    }

    public void startRecord(){
        mRenderThread.start();
        mVideoEncoderThread.start();
    }

    public void stopRecord(){
        mRenderThread.requestExit();
        mVideoEncoderThread.requestExit();
    }

    public void setRenderer(GLSurfaceView.Renderer renderer){
        this.mRenderer = renderer;
    }

    /**
     * 录制的渲染线程
     */
    public static final class VideoRenderThread extends Thread {
        private WeakReference<RecordDelegate> mVideoRecorderWr;
        private boolean mShouldExit = false;
        private boolean mHashCreateContext = false;
        private EglHelper mEGlHelper;
        private int mWidth;
        private int mHeight;
        private GL10 mGl;

        public VideoRenderThread(RecordDelegate videoRecorder) {
            this.mVideoRecorderWr = new WeakReference<>(videoRecorder);
            mEGlHelper = new EglHelper();
            mHeight = mVideoRecorderWr.get().mVideoHeight;
            mWidth = mVideoRecorderWr.get().mVideoWidth;
        }

        @Override
        public void run() {
            while (true) {
                if (mShouldExit) {
                    onDestroy();
                    return;
                }

                RecordDelegate videoRecorder = mVideoRecorderWr.get();
                if (videoRecorder == null) {
                    mShouldExit = true;
                    continue;
                }

                if (!mHashCreateContext) {
                    // 初始化创建 EGL 环境，然后回调 Renderer
                    mEGlHelper.initCreateEgl(videoRecorder.mMediaSurface, videoRecorder.mEglContext);
                    mGl = (GL10) mEGlHelper.getEglContext().getGL();
                    videoRecorder.mRenderer.onSurfaceCreated(mGl, mEGlHelper.getEGLConfig());
                    videoRecorder.mRenderer.onSurfaceChanged(mGl, mWidth, mHeight);
                    mHashCreateContext = true;
                }

                videoRecorder.mRenderer.onDrawFrame(mGl);

                // 绘制到 MediaCodec 的 Surface 上面去
                mEGlHelper.swapBuffers();

                try {
                    // 60 fps
                    Thread.sleep(16 / 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void onDestroy() {
            mEGlHelper.destroy();
        }

        public void requestExit() {
            mShouldExit = true;
        }
    }

    private static class VideoEncoderThread extends Thread {

        private WeakReference<RecordDelegate> mDelegate;
        private volatile boolean mShouldExit;
        private final MediaCodec mMediaCodec;
        private final MediaMuxer mMediaMuxer;
        private final MediaCodec.BufferInfo mBufferInfo;
        private int mVideoTrackIndex = -1;
        private long mVideoPts;

        public VideoEncoderThread(RecordDelegate delegate) {
            mDelegate = new WeakReference<>(delegate);
            mMediaCodec = mDelegate.get().mMediaCodec;
            mMediaMuxer = mDelegate.get().mMediaMuxer;

            mBufferInfo = new MediaCodec.BufferInfo();
        }

        @Override
        public void run() {
            mShouldExit = false;
            mMediaCodec.start();

            while (true) {
                if (mShouldExit) {
                    release();
                    return;
                }

                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mVideoTrackIndex = mMediaMuxer.addTrack(mMediaCodec.getOutputFormat());
                    mMediaMuxer.start();
                } else {
                    while (outputBufferIndex >= 0) {
                        ByteBuffer outBuffer = mMediaCodec.getOutputBuffers()[outputBufferIndex];
                        outBuffer.position(mBufferInfo.offset);
                        outBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                        if (mVideoPts == 0) {
                            mVideoPts = mBufferInfo.presentationTimeUs;
                        }
                        mBufferInfo.presentationTimeUs -= mVideoPts;

                        mMediaMuxer.writeSampleData(mVideoTrackIndex, outBuffer, mBufferInfo);

                        mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    }
                }

            }
        }

        public void requestExit() {
            mShouldExit = true;
        }

        private void release() {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaMuxer.stop();
            mMediaMuxer.release();
        }
    }

}
