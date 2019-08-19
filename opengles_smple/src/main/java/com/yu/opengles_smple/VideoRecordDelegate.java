package com.yu.opengles_smple;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.CyclicBarrier;

import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

public class VideoRecordDelegate {

    private static final String TAG = VideoRecordDelegate.class.getSimpleName();

    private MediaMuxer mMediaMuxer;
    private MediaCodec mVideoCodec;
    private Surface mMediaSurface;
    private int mVideoWidth, mVideoHeight;
    private Context mContext;
    private EGLContext mEglContext;
    private GLSurfaceView.Renderer mRenderer;
    private VideoEncoderThread mVideoEncoderThread;
    private VideoRenderThread mVideoRenderThread;
    private MediaCodec mAudioCodec;
    private AudioRecoderThread mAudioRecoderThread;
    private AudioRecord mAudioRecord;
    private int mAudioBufferSize;
    private CyclicBarrier mStartBarrier;
    private CyclicBarrier mStopBarrier;

    public VideoRecordDelegate(Context context, EGLContext eglContext, int textureId) {
        mContext = context;
        mEglContext = eglContext;
        this.mRenderer = new RecorderRenderer(mContext, textureId);
        mStartBarrier = new CyclicBarrier(2, new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "开启 MediaMuxer");
                mMediaMuxer.start();
            }
        });
        mStopBarrier = new CyclicBarrier(2, new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "停止 MediaMuxer");
                mMediaMuxer.stop();
                mMediaMuxer.release();
            }
        });
    }

    public void initParams(String outPath, int videoWdth, int videoHeight) {
        try {
            this.mVideoWidth = videoWdth % 2 == 0 ? videoWdth : videoWdth - videoWdth % 2;
            this.mVideoHeight = videoHeight % 2 == 0 ? videoHeight : videoHeight - videoHeight % 2;
            this.mMediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initVideoCodec();
            initAudioCodec();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", e.toString());
        }
    }

    private void initAudioCodec() throws Exception {
        MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoWidth * mVideoHeight * 4);
        mAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        mAudioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mAudioBufferSize = AudioRecord.getMinBufferSize(44100
                , AudioFormat.CHANNEL_IN_STEREO
                , AudioFormat.ENCODING_PCM_16BIT) * 2;
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                , 44100, AudioFormat.CHANNEL_IN_STEREO
                , AudioFormat.ENCODING_PCM_16BIT, mAudioBufferSize);

        mAudioRecoderThread = new AudioRecoderThread(this);
    }

    private void initVideoCodec() throws Exception {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mVideoWidth, mVideoHeight);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoWidth * mVideoHeight * 4);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mVideoCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mMediaSurface = mVideoCodec.createInputSurface();

        this.mVideoRenderThread = new VideoRenderThread(this);

        this.mVideoEncoderThread = new VideoEncoderThread(this);

    }

    public void startRecord() {
        mVideoRenderThread.start();
        mVideoEncoderThread.start();

        mAudioRecoderThread.start();
    }

    public void stopRecord() {
        try {
            mAudioRecoderThread.requestExit();
            mVideoRenderThread.requestExit();
            mVideoEncoderThread.requestExit();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void setRenderer(GLSurfaceView.Renderer renderer) {
        this.mRenderer = renderer;
    }

    /**
     * 音频采集线程
     */
    private static class AudioRecoderThread extends Thread {

        private WeakReference<VideoRecordDelegate> mDelegate;
        private final AudioRecord mAudioRecord;
        private boolean mShouldExit;
        private final MediaCodec mAudioCodec;
        private int mAudioBufferSize;
        private CyclicBarrier mStartBarrier;
        private CyclicBarrier mStopBarrier;
        private long mNanoTime;
        private MediaCodec.BufferInfo mBufferInfo;
        private int mAudioTrackIndex;
        private MediaMuxer mMediaMuxer;

        public AudioRecoderThread(VideoRecordDelegate delegate) {
            mDelegate = new WeakReference<>(delegate);
            mAudioRecord = mDelegate.get().mAudioRecord;
            mAudioCodec = mDelegate.get().mAudioCodec;
            mMediaMuxer = mDelegate.get().mMediaMuxer;
            mAudioBufferSize = mDelegate.get().mAudioBufferSize;
            mStartBarrier = mDelegate.get().mStartBarrier;
            mStopBarrier = mDelegate.get().mStopBarrier;
            mBufferInfo = new MediaCodec.BufferInfo();
        }

        @Override
        public void run() {
            try {
                mShouldExit = false;
                mAudioRecord.startRecording();
                mAudioCodec.start();
                mNanoTime = System.nanoTime();
                while (true) {
                    if (mShouldExit) {
                        mNanoTime = 0;
                        release();
                        return;
                    }
                    int inputBufferIndex = mAudioCodec.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = mAudioCodec.getInputBuffers()[inputBufferIndex];
                        inputBuffer.clear();
                        int length = mAudioRecord.read(inputBuffer, mAudioBufferSize);
                        if (length > 0) {
                            mAudioCodec.queueInputBuffer(inputBufferIndex, 0, length, (System.nanoTime() - mNanoTime) / 1000, 0);
                        }
                    }

                    int outputBufferIndex = mAudioCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mAudioTrackIndex = mMediaMuxer.addTrack(mAudioCodec.getOutputFormat());
                        mStartBarrier.await();
                    } else {
                        while (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = mAudioCodec.getOutputBuffers()[outputBufferIndex];
                            outputBuffer.position(mBufferInfo.offset);
                            outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                            byte[] tempBuffer = new byte[mBufferInfo.size + 7];
                            outputBuffer.get(tempBuffer, 7, mBufferInfo.size);

                            addADTS2Packet(tempBuffer, tempBuffer.length);

                            mMediaMuxer.writeSampleData(mAudioTrackIndex, outputBuffer, mBufferInfo);
                            mAudioCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mAudioCodec.dequeueOutputBuffer(mBufferInfo, 0);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }

        /**
         * 添加AAC数据头
         *
         * @param packet
         * @param packetLen
         */
        private void addADTS2Packet(byte[] packet, int packetLen) {
            int profile = 2;  //AAC LC
            int freqIdx = 4;  //44.1KHz
            int chanCfg = 2;  //CPE
            packet[0] = (byte) 0xFF;
            packet[1] = (byte) 0xF9;
            packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
            packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
            packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
            packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
            packet[6] = (byte) 0xFC;
        }

        private void release() {
            try {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioCodec.stop();
                mAudioCodec.release();
                mStopBarrier.await();
            } catch (Exception e) {
                Log.e(TAG, "AudioRecoderThread 错误");
            }
        }

        public void requestExit() {
            mShouldExit = true;
        }
    }

    /**
     * 录制的渲染线程
     */
    public static final class VideoRenderThread extends Thread {
        private WeakReference<VideoRecordDelegate> mVideoRecorderWr;
        private boolean mShouldExit = false;
        private boolean mHashCreateContext = false;
        private EglHelper mEGlHelper;
        private int mWidth;
        private int mHeight;
        private GL10 mGl;

        public VideoRenderThread(VideoRecordDelegate videoRecorder) {
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

                VideoRecordDelegate videoRecorder = mVideoRecorderWr.get();
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

        private WeakReference<VideoRecordDelegate> mDelegate;
        private volatile boolean mShouldExit;
        private final MediaCodec mVideoCodec;
        private final MediaMuxer mMediaMuxer;
        private final MediaCodec.BufferInfo mBufferInfo;
        private int mVideoTrackIndex = -1;
        private long mVideoPts;
        private CyclicBarrier mStartBarrier;
        private CyclicBarrier mStopBarrier;

        public VideoEncoderThread(VideoRecordDelegate delegate) {
            mDelegate = new WeakReference<>(delegate);
            mVideoCodec = mDelegate.get().mVideoCodec;
            mMediaMuxer = mDelegate.get().mMediaMuxer;
            mStartBarrier = mDelegate.get().mStartBarrier;
            mStopBarrier = mDelegate.get().mStopBarrier;
            mBufferInfo = new MediaCodec.BufferInfo();
        }

        @Override
        public void run() {
            try {
                mShouldExit = false;
                mVideoCodec.start();

                while (true) {
                    if (mShouldExit) {
                        release();
                        return;
                    }

                    int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, 0);

                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mVideoTrackIndex = mMediaMuxer.addTrack(mVideoCodec.getOutputFormat());
                        mStartBarrier.await();
                    } else {
                        while (outputBufferIndex >= 0) {
                            ByteBuffer outBuffer = mVideoCodec.getOutputBuffers()[outputBufferIndex];
                            outBuffer.position(mBufferInfo.offset);
                            outBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                            if (mVideoPts == 0) {
                                mVideoPts = mBufferInfo.presentationTimeUs;
                            }
                            mBufferInfo.presentationTimeUs -= mVideoPts;

                            mMediaMuxer.writeSampleData(mVideoTrackIndex, outBuffer, mBufferInfo);

                            mVideoCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, 0);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void requestExit() {
            mShouldExit = true;
        }

        private void release() {
            try {
                mVideoCodec.stop();
                mVideoCodec.release();
                mStopBarrier.await();
            } catch (Exception e) {
                Log.e(TAG, "VideoEncoderThread 错误");
            }
        }
    }
}
