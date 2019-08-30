//
// Created by yc on 19-8-29.
//

#include <jni.h>
#include "audio_encoder.h"

AudioEncoder *audioEncoder;

extern "C" JNIEXPORT void JNICALL
Java_com_yu_audioencoder_AudioEncoder_initEncoder(JNIEnv *env, jobject instance,
                                                  jint bitRate, jint channles, jint sampleRate,
                                                  jstring outPath) {
    const char *cOutPath = env->GetStringUTFChars(outPath, JNI_FALSE);
    audioEncoder = new AudioEncoder(cOutPath, bitRate, sampleRate, channles);
    audioEncoder->prepare();
    env->ReleaseStringUTFChars(outPath, cOutPath);
}

extern "C" JNIEXPORT void JNICALL
Java_com_yu_audioencoder_AudioEncoder_encodeAudio(JNIEnv *env, jobject instance, jbyteArray buffer,
                                                  jint length) {
    jbyte *bytes = env->GetByteArrayElements(buffer, JNI_FALSE);
    if (audioEncoder) {
        audioEncoder->encodeBuffer((unsigned char *) bytes, length);
    }
    env->ReleaseByteArrayElements(buffer, bytes, NULL);
}

extern "C" JNIEXPORT void JNICALL
Java_com_yu_audioencoder_AudioEncoder_startEncoder(JNIEnv *env, jobject instance) {
    if (audioEncoder) {
        audioEncoder->start();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_yu_audioencoder_AudioEncoder_stopEncoder(JNIEnv *env, jobject instance) {
    if (audioEncoder) {
        audioEncoder->stop();
    }
}

