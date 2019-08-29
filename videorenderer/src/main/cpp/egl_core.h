//
// Created by yc on 19-8-26.
//

#ifndef OPENGLSAMPLE_EGL_CORE_H
#define OPENGLSAMPLE_EGL_CORE_H

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include "const_util.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>

class EGLCore {
private:
    EGLDisplay eglDisplay = NULL;
    EGLConfig eglConfig = NULL;
    EGLContext eglContext = NULL;

public:
    bool initialize();
    bool initialize(EGLContext sharedContext);
    EGLSurface createWindowSurface(ANativeWindow* nativeWindow);
    EGLSurface createOffscreenSurface(int width, int height);
    EGLBoolean makeCurrent(EGLSurface eglSurface);
    void doneCurrent();
    EGLBoolean swapBuffer(EGLSurface eglSurface);
    int querySurface(EGLSurface surface, int what);
    int setPresentationTime(EGLSurface surface, khronos_stime_nanoseconds_t nsecs);
    void releaseSurface(EGLSurface eglSurface);
    void release();
    EGLContext getContext();
    EGLDisplay getDisplay();
    EGLConfig getConfig();
};


#endif //OPENGLSAMPLE_EGL_CORE_H
