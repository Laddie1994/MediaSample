//
// Created by yc on 19-8-21.
//

#ifndef MUSICPLAYER_AUDIO_CONTROLLER_H
#define MUSICPLAYER_AUDIO_CONTROLLER_H


#include <jni.h>
#include "audio_player.h"
#include "audio_decoder.h"

class AudioController {

private:
    AudioPlayer *audioPlayer;
    AudioDecoder *audioDecoder;
    DZJNICall *jniCall;
    JNIEnv *jniEnv;

public:
    AudioController(JNIEnv *jniEnv, jobject instance, const char* audioPath);
    ~AudioController();
    static int audioPlayerFillData(AudioPcm** buffer, void *ctx);
    int consumeFillData(AudioPcm **buffer);
    void prepare();
    void start();
    void play();
    void pause();
    void stop();
    void release();
};


#endif //MUSICPLAYER_AUDIO_CONTROLLER_H
