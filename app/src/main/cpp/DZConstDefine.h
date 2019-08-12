//
// Created by 曾辉 on 2019-07-03.
//

#ifndef NDK_DAY03_DZCONSTDEFINE_H
#define NDK_DAY03_DZCONSTDEFINE_H
#include <android/log.h>

#define TAG "JNI_TAG"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

// ---------- 错误码 start ----------
#define RTMP_CONNECT_ERROR_CODE -0x10
#define RTMP_STREAM_CONNECT_ERROR_CODE -0x11
// ---------- 错误码 end ----------

#endif //NDK_DAY03_DZCONSTDEFINE_H
