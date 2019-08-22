//
// Created by hcDarren on 2019/6/16.
//

#ifndef MUSICPLAYER_DZJNICALL_H
#define MUSICPLAYER_DZJNICALL_H


#include <jni.h>

class DZJNICall {
public:
    jobject jAudioTrackObj;
    jmethodID jAudioTrackWriteMid;
    JavaVM *javaVM;
    JNIEnv *jniEnv;
    jmethodID jPlayerErrorMid;
    jobject jPlayerObj;
public:
    DZJNICall(JNIEnv *jniEnv, jobject jPlayerObj);
    ~DZJNICall();

private:
    void initCrateAudioTrack();

public:
    void callAudioTrackWrite(jbyteArray audioData, int offsetInBytes, int sizeInBytes);

    void callPlayerError(int code, const char *msg);

    JNIEnv* getJniEnv();
};


#endif //MUSICPLAYER_DZJNICALL_H
