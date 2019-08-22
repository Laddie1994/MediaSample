//
// Created by hcDarren on 2019/6/16.
//

#include "DZJNICall.h"
#include "DZConstDefine.h"

DZJNICall::DZJNICall(JNIEnv *jniEnv, jobject jPlayerObj) {
    jniEnv->GetJavaVM(&javaVM);
    this->jniEnv = jniEnv;
    this->jPlayerObj = jniEnv->NewGlobalRef(jPlayerObj);
    initCrateAudioTrack();

    jclass jPlayerClass = jniEnv->GetObjectClass(jPlayerObj);
    jPlayerErrorMid = jniEnv->GetMethodID(jPlayerClass, "onError", "(ILjava/lang/String;)V");
}

void DZJNICall::initCrateAudioTrack() {
    jclass jAudioTrackClass = getJniEnv()->FindClass("android/media/AudioTrack");
    jmethodID jAudioTackCMid = getJniEnv()->GetMethodID(jAudioTrackClass, "<init>", "(IIIIII)V");

    int streamType = 3;
    int sampleRateInHz = AUDIO_SAMPLE_RATE;
    int channelConfig = (0x4 | 0x8);
    int audioFormat = 2;
    int mode = 1;

    // int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat)
    jmethodID getMinBufferSizeMid = getJniEnv()->GetStaticMethodID(jAudioTrackClass, "getMinBufferSize",
            "(III)I");
    int bufferSizeInBytes = getJniEnv()->CallStaticIntMethod(jAudioTrackClass, getMinBufferSizeMid,
            sampleRateInHz, channelConfig, audioFormat);
    LOGE("bufferSizeInBytes = %d", bufferSizeInBytes);

    jAudioTrackObj = getJniEnv()->NewObject(jAudioTrackClass, jAudioTackCMid, streamType,
            sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode);

    // start  method
    jmethodID playMid = getJniEnv()->GetMethodID(jAudioTrackClass, "play", "()V");
    getJniEnv()->CallVoidMethod(jAudioTrackObj, playMid);

    // write method
    jAudioTrackWriteMid = getJniEnv()->GetMethodID(jAudioTrackClass, "write", "([BII)I");

    // 由于要在子线程中调用，需要生成全局变量
    jAudioTrackObj = getJniEnv()->NewGlobalRef(jAudioTrackObj);
}

void DZJNICall::callAudioTrackWrite(jbyteArray audioData, int offsetInBytes, int sizeInBytes) {
    getJniEnv()->CallIntMethod(jAudioTrackObj, jAudioTrackWriteMid, audioData, offsetInBytes,
            sizeInBytes);
}

DZJNICall::~DZJNICall() {
    jniEnv->DeleteGlobalRef(jAudioTrackObj);
    jniEnv->DeleteGlobalRef(jPlayerObj);
}

void DZJNICall::callPlayerError(int code, const char *msg) {
    jstring jMsg = getJniEnv()->NewStringUTF(msg);
    getJniEnv()->CallVoidMethod(jPlayerObj, jPlayerErrorMid, code, jMsg);
    getJniEnv()->DeleteLocalRef(jMsg);
}

JNIEnv *DZJNICall::getJniEnv() {
    JNIEnv* jniEnv = NULL;
    int state = javaVM->GetEnv((void **)(&jniEnv), JNI_VERSION_1_6);
    if (state < 0){
        state = javaVM->AttachCurrentThread(&jniEnv, NULL);
        if (state < 0){
            jniEnv = NULL;
        }
    }
    return jniEnv;
}
