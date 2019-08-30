//
// Created by yc on 19-8-29.
//

#ifndef OPENGLSAMPLE_AUDIO_ENCODER_H
#define OPENGLSAMPLE_AUDIO_ENCODER_H

#include <jni.h>
#include <android/log.h>

extern "C"{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libswresample/swresample.h"
#include "libavutil/avutil.h"
#include <libavutil/opt.h>
}

#define TAG "JNI_TAG"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

class AudioEncoder {

private:
    char* outPath;
    int bitRate;
    int sampleRate;
    int channles;
    AVFormatContext *pFormatContext;
    AVStream *pStream;
    AVOutputFormat *pFormat;
    AVCodec *pCodec;
    AVCodecContext *pCodecContext;
    AVFrame* pFrame;
    int frameSize;
    uint8_t *frameBuffer;
    AVPacket pPacket;
    SwrContext *swrContext;

public:
    AudioEncoder(const char* outPath, int bitRate, int sampleRate, int channels);
    ~AudioEncoder();

    int prepare();
    int start();
    int encodeFrame(AVFrame *pFrame);
    int encodeBuffer(const unsigned char *buffer, int length);
    int stop();
};


#endif //OPENGLSAMPLE_AUDIO_ENCODER_H
