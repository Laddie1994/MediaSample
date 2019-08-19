#include <jni.h>
#include <malloc.h>
#include <android/log.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "DZLivePush.h"

extern "C" {
#include "rtmp/rtmp.h"
}

DZLivePush *pLivePush = NULL;
JavaVM *pJavaVM = NULL;
DZJNICall *pJniCall = NULL;

extern "C"
JNIEXPORT void JNICALL
Java_com_darren_live_LivePush_nInitConnect(JNIEnv *env, jobject instance, jstring url_) {
    const char *url = env->GetStringUTFChars(url_, 0);
    if (pLivePush == NULL) {
        // 初始化媒体房间连接
        pJniCall = new DZJNICall(pJavaVM, env, instance);
        pLivePush = new DZLivePush(url, pJniCall);
        pLivePush->initConnect();
    }
    env->ReleaseStringUTFChars(url_, url);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_darren_live_LivePush_nStop(JNIEnv *env, jobject instance) {
    if (pLivePush != NULL) {
        delete pLivePush;
        pLivePush = NULL;
    }
    if (pJniCall != NULL) {
        delete pJniCall;
        pJniCall = NULL;
    }
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *javaVM, void *reserved) {
    pJavaVM = javaVM;
    JNIEnv *env;
    if (javaVM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    return JNI_VERSION_1_6;
}
