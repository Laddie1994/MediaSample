package com.darren.ndk.day03.record;


import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.darren.ndk.day03.EglHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.CyclicBarrier;

import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

/**
 * 修改 GLSurfaceView 源码，能共用 EGLContext
 */
public abstract class BaseVideoRecorder {
    private WeakReference<BaseVideoRecorder> mVideoRecorderWr = new WeakReference<>(this);

    private VideoRenderThread mRenderThread;
    private VideoEncoderThread mVideoEncoderThread;
    // private AudioEncoderThread mAudioEncoderThread;

    private Context mContext;
    private Surface mSurface;
    private EGLContext mEglContext;
    private GLSurfaceView.Renderer mRenderer;

    private MediaCodec mVideoCodec;
    private MediaCodec mAudioCodec;
    private MediaMuxer mMediaMuxer;

    private CyclicBarrier mStartCb = new CyclicBarrier(2);

    public BaseVideoRecorder(Context context, EGLContext eglContext) {
        this.mContext = context;
        this.mEglContext = eglContext;
    }

    public void setRenderer(GLSurfaceView.Renderer mRenderer) {
        this.mRenderer = mRenderer;
        mRenderThread = new VideoRenderThread(mVideoRecorderWr);
    }

    /**
     * 初始化视频参数
     *
     * @param videoWidth  视频宽度
     * @param videoHeight 视频高度
     */
    public void initVideoParams(String outfilePath, String audioPath, int videoWidth, int videoHeight) {
        mRenderThread.setSize(videoWidth, videoHeight);
        try {
            // 用来合成视频文件
            mMediaMuxer = new MediaMuxer(outfilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initVideoCodec(videoWidth, videoHeight);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initVideoCodec(int videoWidth, int videoHeight) throws IOException {
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                videoWidth, videoHeight);
        // 设置颜色格式
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoWidth * videoWidth * 4);
        // 设置帧率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        // 设置 I 帧间隔的时间
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        // 创建编码器
        mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mVideoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        // 相机的像素数据绘制到该 surface 上面
        mSurface = mVideoCodec.createInputSurface();

        mVideoEncoderThread = new VideoEncoderThread(mVideoRecorderWr);
    }

    public void startRecord() {
        mRenderThread.start();
        mVideoEncoderThread.start();
        // mAudioEncoderThread.start();
    }

    public void stopRecord() {
        mVideoEncoderThread.requestExit();
        mRenderThread.requestExit();
        // mAudioEncoderThread.requestExit();
    }

    /**
     * 编码视频线程
     */
    public static final class VideoEncoderThread extends Thread {
        private WeakReference<BaseVideoRecorder> mVideoRecorderWr;
        private volatile boolean mShouldExit;

        private MediaCodec mVideoCodec;
        private MediaCodec.BufferInfo mBufferInfo;
        private MediaMuxer mMediaMuxer;
        /**
         * 视频轨道
         */
        private int mVideoTrackIndex = -1;

        private long mVideoPts = 0;

        public VideoEncoderThread(WeakReference<BaseVideoRecorder> videoRecorderWr) {
            this.mVideoRecorderWr = videoRecorderWr;
            mVideoCodec = videoRecorderWr.get().mVideoCodec;
            mBufferInfo = new MediaCodec.BufferInfo();
            mMediaMuxer = videoRecorderWr.get().mMediaMuxer;
        }

        @Override
        public void run() {
            mShouldExit = false;
            mVideoCodec.start();

            try {
                while (true) {
                    if (mShouldExit) {
                        onDestroy();
                        return;
                    }

                    int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, 0);

                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mVideoTrackIndex = mMediaMuxer.addTrack(mVideoCodec.getOutputFormat());
                        mMediaMuxer.start();
                    } else {
                        while (outputBufferIndex >= 0) {
                            // 获取数据
                            ByteBuffer outBuffer = mVideoCodec.getOutputBuffers()[outputBufferIndex];
                            outBuffer.position(mBufferInfo.offset);
                            outBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                            // 修改视频的 pts
                            if (mVideoPts == 0) {
                                mVideoPts = mBufferInfo.presentationTimeUs;
                            }
                            mBufferInfo.presentationTimeUs -= mVideoPts;

                            // 写入数据
                            mMediaMuxer.writeSampleData(mVideoTrackIndex, outBuffer, mBufferInfo);

                            // 回调当前录制时间
                            if (mVideoRecorderWr.get().mRecordInfoListener != null) {
                                mVideoRecorderWr.get().mRecordInfoListener.onTime(mBufferInfo.presentationTimeUs / 1000);
                            }

                            // 释放 OutputBuffer
                            mVideoCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, 0);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onDestroy();
            }
        }

        private void onDestroy() {
            try {
                mVideoCodec.stop();
                mVideoCodec.release();
                mMediaMuxer.stop();
                mMediaMuxer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void requestExit() {
            mShouldExit = true;
        }
    }

    /**
     * 编码音频线程
     */
    public static final class AudioEncoderThread extends Thread {
        private WeakReference<BaseVideoRecorder> mVideoRecorderWr;
        private boolean mShouldExit = false;

        public AudioEncoderThread(WeakReference<BaseVideoRecorder> videoRecorderWr,
                MediaExtractor inputMediaExtractor, int audioTrackIndex, int audioMaxInputSize) {
            this.mVideoRecorderWr = videoRecorderWr;
        }

        @Override
        public void run() {
            mShouldExit = false;

            try {

                Log.e("TAG", "audio start");
                while (true) {
                    if (mShouldExit) {
                        onDestroy();
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Log.e("TAG", "audio onDestroy");
                onDestroy();
            }
        }

        private void onDestroy() {

        }

        public void requestExit() {
            mShouldExit = true;
        }
    }

    /**
     * 录制的渲染线程
     */
    public static final class VideoRenderThread extends Thread {
        private WeakReference<BaseVideoRecorder> mVideoRecorderWr;
        private boolean mShouldExit = false;
        private boolean mHashCreateContext = false;
        private boolean mHashSurfaceChanged = false;
        private boolean mHashSurfaceCreated = false;
        private EglHelper mEGlHelper;
        private int mWidth;
        private int mHeight;

        public VideoRenderThread(WeakReference<BaseVideoRecorder> videoRecorderWr) {
            this.mVideoRecorderWr = videoRecorderWr;
            mEGlHelper = new EglHelper();
        }

        public void setSize(int width, int height) {
            this.mWidth = width;
            this.mHeight = height;
        }

        @Override
        public void run() {
            while (true) {
                if (mShouldExit) {
                    onDestroy();
                    return;
                }

                BaseVideoRecorder videoRecorder = mVideoRecorderWr.get();
                if (videoRecorder == null) {
                    mShouldExit = true;
                    continue;
                }

                if (!mHashCreateContext) {
                    // 初始化创建 EGL 环境，然后回调 Renderer
                    mEGlHelper.initCreateEgl(videoRecorder.mSurface, videoRecorder.mEglContext);
                    mHashCreateContext = true;
                }

                GL10 gl = (GL10) mEGlHelper.getEglContext().getGL();

                if (!mHashSurfaceCreated) {
                    videoRecorder.mRenderer.onSurfaceCreated(gl, mEGlHelper.getEGLConfig());
                    mHashSurfaceCreated = true;
                }

                if (!mHashSurfaceChanged) {
                    videoRecorder.mRenderer.onSurfaceChanged(gl, mWidth, mHeight);
                    mHashSurfaceChanged = true;
                }

                videoRecorder.mRenderer.onDrawFrame(gl);

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

    private RecordInfoListener mRecordInfoListener;

    public void setOnRecordInfoListener(RecordInfoListener mRecordInfoListener) {
        this.mRecordInfoListener = mRecordInfoListener;
    }

    public interface RecordInfoListener {
        void onTime(long times);
    }
}
