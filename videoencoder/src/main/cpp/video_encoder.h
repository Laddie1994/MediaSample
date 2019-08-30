//
// Created by yc on 19-8-30.
//

#ifndef OPENGLSAMPLE_VIDEO_ENCODER_H
#define OPENGLSAMPLE_VIDEO_ENCODER_H

#include <jni.h>
#include <android/log.h>
#include "libyuv.h"

extern "C" {
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libavformat/avformat.h"
#include "libavutil/avutil.h"
#include "libavutil/imgutils.h"
};

#define TAG "JNI_TAG"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

class VideoEncoder {

private:
    const char *outPath;
    int width;
    int height;
    int bitRate;
    AVRational frameRate;
    AVRational timeBase;
    int fps = 15;
    AVFormatContext *pFormatContext;
    AVCodec *pCodec;
    AVCodecContext *pCodecContext;
    AVStream *pStream;
    AVFrame *pFrame;
    AVPacket pPacket;
    int index;
    uint8_t *frameBuffer;

public:
    VideoEncoder(const char *outPath, int width, int height, int bitRate);

    int prepare();

    int encodeFrame(AVFrame *pFrame, AVPacket *pPacket);
    int encodeBuffer(unsigned char *buffer, int length);

    void stopEncode();

};


#endif //OPENGLSAMPLE_VIDEO_ENCODER_H
