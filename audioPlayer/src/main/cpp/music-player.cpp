#include <jni.h>
#include "DZJNICall.h"
#include "audio_decoder.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "audio_player.h"
#include "audio_controller.h"

// 在 c++ 中采用 c 的这种编译方式
extern "C" {
#include "libavformat/avformat.h"
#include "libswresample/swresample.h"
}

AudioController* audioController;

extern "C" JNIEXPORT void JNICALL
Java_com_yu_audioplayer_audio_SimpleAudioPlayer_nativePlayer(JNIEnv *env, jobject instance, jstring url_) {
    const char *url = env->GetStringUTFChars(url_, 0);
    audioController = new AudioController(env, instance, url);
    audioController->prepare();
    audioController->start();
    // delete pJniCall;
    // delete pFFmpeg;
    env->ReleaseStringUTFChars(url_, url);
}

