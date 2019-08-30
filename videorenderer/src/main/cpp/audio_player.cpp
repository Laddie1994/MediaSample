//
// Created by yc on 19-8-20.
//

#include "audio_player.h"
#include "../../../../audioPlayer/src/main/cpp/audio_player.h"


AudioPlayer::AudioPlayer(void *ctx) {
    this->ctx = ctx;
    createEngin();
    createMix();
    createPlay();
    createBufferQueue();
}

AudioPlayer::~AudioPlayer() {

}

void AudioPlayer::registerCallback(audioPlayerCallback audioPlayerCallback) {
    this->produceDataCallback = audioPlayerCallback;
}

void AudioPlayer::start() {
    SLresult result = (*playerInterface)->SetPlayState(playerInterface, SL_PLAYSTATE_PLAYING);
    if (result != SL_RESULT_SUCCESS) {
        return;
    }
    playingState = SL_PLAYSTATE_PLAYING;
    productPacket();
}

void AudioPlayer::playerCallback(SLAndroidSimpleBufferQueueItf caller, void *pContext) {
    AudioPlayer *audioPlayer = (AudioPlayer *) pContext;
    audioPlayer->productPacket();
}

void AudioPlayer::productPacket() {
    for (;;) {
        if (playingState != SL_PLAYSTATE_PLAYING) {
            break;
        }
        int dataSize = produceDataCallback(&audioPcm, ctx);
        if (dataSize <= 0) {
            continue;
        }
        SLresult result = (*playerBufferQueue)->Enqueue(playerBufferQueue, audioPcm->samples, audioPcm->size);
        delete audioPcm;
        if (result == SL_RESULT_SUCCESS) {
            LOGE("播放成功");
            break;
        }
    }
}

void AudioPlayer::play() {
    SLresult result = (*playerInterface)->SetPlayState(playerInterface, SL_PLAYSTATE_PLAYING);
    if (result != SL_RESULT_SUCCESS) {
        return;
    }
    playingState = SL_PLAYSTATE_PLAYING;
}

void AudioPlayer::pause() {
    SLresult result = (*playerInterface)->SetPlayState(playerInterface, SL_PLAYSTATE_PAUSED);
    if (result != SL_RESULT_SUCCESS) {
        return;
    }
    playingState = SL_PLAYSTATE_PAUSED;
}

void AudioPlayer::stop() {
    SLresult result = (*playerInterface)->SetPlayState(playerInterface, SL_PLAYSTATE_STOPPED);
    if (result != SL_RESULT_SUCCESS) {
        return;
    }
    playingState = SL_PLAYSTATE_STOPPED;
}

void AudioPlayer::release() {
    if (enginObject){
        (*enginObject)->Destroy(enginObject);
        enginObject = NULL;
        engineInterface = NULL;
    }

    if (playerObject){
        (*playerObject)->Destroy(playerObject);
        playerObject = NULL;
        playerInterface = NULL;
        playerBufferQueue = NULL;
        produceDataCallback = NULL;
    }

    if (outputMixObject){
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixInterface = NULL;
    }

}

void AudioPlayer::createEngin() {
    slCreateEngine(&enginObject, 0, NULL, 0, NULL, NULL);
    (*enginObject)->Realize(enginObject, SL_BOOLEAN_FALSE);
    (*enginObject)->GetInterface(enginObject, SL_IID_ENGINE, &engineInterface);
}

void AudioPlayer::createMix() {
    const SLInterfaceID ids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean req[1] = {SL_BOOLEAN_FALSE};
    (*engineInterface)->CreateOutputMix(engineInterface, &outputMixObject, 1, ids, req);
    (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                                     &outputMixInterface);
    const SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;
    (*outputMixInterface)->SetEnvironmentalReverbProperties(outputMixInterface, &reverbSettings);
}

void AudioPlayer::createBufferQueue() {

}

void AudioPlayer::createPlay() {
//设置PCM格式信息
    SLDataLocator_AndroidSimpleBufferQueue simpleBufferQueue = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM formatPcm = {
            SL_DATAFORMAT_PCM,//播放PCM
            2,//双通道
            SL_SAMPLINGRATE_44_1,//44100
            SL_PCMSAMPLEFORMAT_FIXED_16,//16bit
            SL_PCMSAMPLEFORMAT_FIXED_16,//16bit
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,//立体声(前左右)
            SL_BYTEORDER_LITTLEENDIAN};//结束标志
    SLDataSource audioSrc = {&simpleBufferQueue, &formatPcm};
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&outputMix, NULL};
    SLInterfaceID interfaceIds[3] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME, SL_IID_PLAYBACKRATE};
    SLboolean interfaceRequired[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    (*engineInterface)->CreateAudioPlayer(engineInterface, &playerObject, &audioSrc, &audioSnk, 3,
                                          interfaceIds, interfaceRequired);
    (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playerInterface);

    //设置缓存队列
    (*playerObject)->GetInterface(playerObject, SL_IID_BUFFERQUEUE, &playerBufferQueue);

    //设置回调函数
    (*playerBufferQueue)->RegisterCallback(playerBufferQueue, playerCallback, this);

}