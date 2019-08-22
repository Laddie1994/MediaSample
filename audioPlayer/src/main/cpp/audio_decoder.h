//
// Created by hcDarren on 2019/6/16.
//

#ifndef MUSICPLAYER_DZFFMPEG_H
#define MUSICPLAYER_DZFFMPEG_H

#include "DZJNICall.h"
#include "audio_player.h"
#include <pthread.h>
#include <queue>

using namespace std;

extern "C" {
#include "libavformat/avformat.h"
#include "libswresample/swresample.h"
};

class AudioDecoder {
public:
    AVFormatContext *pFormatContext = NULL;
    AVCodecContext *pCodecContext = NULL;
    SwrContext *swrContext = NULL;
    char *url = NULL;
    DZJNICall *pJniCall = NULL;
    pthread_t playThreadT;
    std::queue<AudioPcm *> *audioFrameQueue;

    int audioStramIndex = -1;
    AVCodecParameters *pCodecParameters;
    AVCodec *pCodec = NULL;
public:
    AudioDecoder(DZJNICall *pJniCall, const char *url);

    ~AudioDecoder();

public:

    void start();

    void play();

    void pause();

    void stop();

    void prepare();

    void decodeAudio();

    void callPlayerJniError(int code, const char *msg);

    void release();

    int audioFillData(AudioPcm **buffer);

};


#endif //MUSICPLAYER_DZFFMPEG_H
