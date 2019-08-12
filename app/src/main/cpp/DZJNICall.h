//
// Created by 曾辉 on 2019-07-03.
//

#ifndef NDK_DAY03_DZJNICALL_H
#define NDK_DAY03_DZJNICALL_H

#include <jni.h>
#include "DZConstDefine.h"

enum ThreadMode {
    THREAD_CHILD, THREAD_MAIN
};

class DZJNICall {
public:
    JavaVM *javaVM;
    JNIEnv *jniEnv;
    jmethodID jConnectErrorMid;
    jmethodID jConnectSuccessMid;
    jobject jLiveObj;
public:
    DZJNICall(JavaVM *javaVM, JNIEnv *jniEnv, jobject jLiveObj);

    ~DZJNICall();

public:
    void callConnectError(ThreadMode threadMode, int code, char *msg);

    void callConnectSuccess(ThreadMode threadMode);
};


#endif //NDK_DAY03_DZJNICALL_H
