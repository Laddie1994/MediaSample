//
// Created by yc on 19-8-26.
//

#ifndef OPENGLSAMPLE_VIDEP_DECODER_H
#define OPENGLSAMPLE_VIDEP_DECODER_H

#include <pthread.h>
#include <queue>
#include "const_util.h"
#include "movie_frame.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>

extern "C" {
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
#include "libswresample/swresample.h"
};

#define AUDIO_SAMPLE_RATE 44100
#define VIDEO_DECODE_COMPLETE -1
#define VIDEO_DECODE_NULL -2

typedef int (*videoProductCallback)(VideoFrame**, void*);
typedef int (*audioProductCallback)(AudioFrame**, void*);

class VideoDecoder {
private:
    AVFormatContext *pFormatContext = NULL;
    AVCodecContext *videoCodecContext = NULL;
    SwsContext *swsContext = NULL;
    char *url = NULL;
    int videoStramIndex = -1;
    AVCodecParameters *videoCodecParameters;
    AVCodec *videoCodec = NULL;
    videoProductCallback productVideoFrameCallback;
    audioProductCallback productAudioFrameCallback;
    void* ctx;
    pthread_t decodeVideoThread;
    bool isDecodeVideoEOF;
    double videoTimeBase;
    double videoFps;

    int audioStreamIndex = -1;
    AVCodecParameters* audioCodecParameters;
    AVCodec* audioCodec;
    AVCodecContext* audioCodecContext;
    SwrContext* swrContext;
    AVStream* audioStream;
    double audioTimeBase;
    double audioFps;
    uint8_t* audioResampleBuffer;
    int audioFrameSize;

public:
    VideoDecoder(void* ctx, const char *url);
    ~VideoDecoder();
    void registerDecodeCallback(videoProductCallback, audioProductCallback);
    void initialize();
    void start();
    void stop();
    static void* threadStartDecoderVideo(void *ctx);
    void decodeVideo();
    void release();
    VideoFrame* handleVideoFrame(AVFrame *pFrame);
    void copyFrameData(uint8_t * dst, uint8_t * src, int width, int height, int linesize);
    bool isVideoEOF(){
        return isDecodeVideoEOF;
    }

    int initFFmpagContext();
    int openVideoStream();
    int openAudioStream();
    AudioFrame *handleAudioFrame(AVFrame *pFrame);

    int getVideoWidth();
    int getVideoHeight();
};


#endif //OPENGLSAMPLE_VIDEP_DECODER_H
