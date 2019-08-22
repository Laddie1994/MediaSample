//
// Created by yc on 19-8-21.
//

#include "audio_controller.h"

AudioController::AudioController(JNIEnv *jniEnv, jobject instance, const char* audioPath) {
    audioPlayer = new AudioPlayer();
    audioPlayer->initialize(audioPlayerFillData, this);

    jniCall = new DZJNICall(jniEnv, instance);
    audioDecoder = new AudioDecoder(jniCall, audioPath);
}

void AudioController::prepare() {
    audioDecoder->prepare();
}

void AudioController::start() {
    audioDecoder->start();
    audioPlayer->start();
}

void AudioController::play() {

}

void AudioController::pause() {

}

void AudioController::stop() {

}

void AudioController::release() {

}

int AudioController::audioPlayerFillData(AudioPcm **buffer, void *ctx) {
    AudioController* audioController = static_cast<AudioController *>(ctx);
    return audioController->consumeFillData(buffer);
}

int AudioController::consumeFillData(AudioPcm **buffer) {
    return audioDecoder->audioFillData(buffer);
}

