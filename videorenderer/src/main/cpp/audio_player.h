//
// Created by yc on 19-8-20.
//

#ifndef MUSICPLAYER_AUDIO_PLAYER_H
#define MUSICPLAYER_AUDIO_PLAYER_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <unistd.h>
#include "const_util.h"
#include "movie_frame.h"

extern "C" {
#include "libavformat/avformat.h"
#include "libswresample/swresample.h"
#include <libavutil/frame.h>
};


typedef int (*audioPlayerCallback)(AudioFrame** buffer, void *ctx);

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
    AudioFrame *audioPcm;

    void createEngin();

    void createMix();

    void createBufferQueue();

    void createPlay();

    static void playerCallback(SLAndroidSimpleBufferQueueItf caller, void *pContext);

    void productPacket();

public:
    AudioPlayer(void *ctx);

    ~AudioPlayer();

    void registerCallback(audioPlayerCallback);

    void start();

    void play();

    void pause();

    void stop();

    void release();
};


#endif //MUSICPLAYER_AUDIO_PLAYER_H
