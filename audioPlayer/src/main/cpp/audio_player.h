//
// Created by yc on 19-8-20.
//

#ifndef MUSICPLAYER_AUDIO_PLAYER_H
#define MUSICPLAYER_AUDIO_PLAYER_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <unistd.h>
#include "DZConstDefine.h"

extern "C" {
#include "libavformat/avformat.h"
#include "libswresample/swresample.h"
#include <libavutil/frame.h>
};

struct AudioPcm{
    void *data;
    int dataSize;
};

typedef int (*audioPlayerCallback)(AudioPcm** buffer, void *ctx);


class AudioPlayer {

private:
    int playingState = SL_PLAYSTATE_STOPPED;
    SLObjectItf enginObject = NULL;
    SLEngineItf engineInterface;
    SLEnvironmentalReverbItf outputMixInterface;
    SLPlayItf playerInterface;
    SLAndroidSimpleBufferQueueItf playerBufferQueue;
    SLObjectItf outputMixObject;
    SLObjectItf playerObject;
    audioPlayerCallback produceDataCallback;
    void *ctx;
    void *buffer;
    AudioPcm *audioPcm;

    void createEngin();

    void createMix();

    void createBufferQueue();

    void createPlay();

    static void playerCallback(SLAndroidSimpleBufferQueueItf caller, void *pContext);

    void productPacket();

public:
    AudioPlayer();

    ~AudioPlayer();

    void initialize(audioPlayerCallback, void *ctx);

    void start();

    void play();

    void pause();

    void stop();

    void release();
};


#endif //MUSICPLAYER_AUDIO_PLAYER_H
