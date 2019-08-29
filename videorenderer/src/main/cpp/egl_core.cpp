//
// Created by yc on 19-8-26.
//

#include "egl_core.h"

bool EGLCore::initialize() {
    return initialize(NULL);
}

bool EGLCore::initialize(EGLContext sharedContext) {
    if ((eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay returned error = %d", eglGetError());
        return false;
    }

    if (!eglInitialize(eglDisplay, 0, 0)) {
        LOGE("eglInitialize returned error = %d", eglGetError());
        return false;
    }

    const EGLint config_attribs[] = {
            EGL_BUFFER_SIZE, 32,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_NONE
    };
    EGLint eglConfigNum = 0;
    if (!eglChooseConfig(eglDisplay, config_attribs, &eglConfig, 1, &eglConfigNum)) {
        LOGE("eglChooseConfig returned error = %d", eglGetError());
        return false;
    }

    const EGLint context_attribs[] = {
            EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE
    };
    if (!(eglContext = eglCreateContext(eglDisplay, eglConfig, sharedContext, context_attribs))) {
        LOGE("eglCreateContext returned error = %d", eglGetError());
        return false;
    }
    return true;
}

EGLSurface EGLCore::createWindowSurface(ANativeWindow *nativeWindow) {
    EGLSurface eglSurface = NULL;
    EGLint eglFormat = 0;
    if (!eglGetConfigAttrib(eglDisplay, eglConfig, EGL_NATIVE_VISUAL_ID, &eglFormat)) {
        LOGE("eglGetConfigAttrib returned error = %d", eglGetError());
        release();
        return eglSurface;
    }

    ANativeWindow_setBuffersGeometry(nativeWindow, 0, 0, eglFormat);
    if (!(eglSurface = eglCreateWindowSurface(eglDisplay, eglConfig, nativeWindow, 0))) {
        LOGE("eglGetConfigAttrib returned error = %d", eglGetError());
        release();
        return eglSurface;
    }
    return eglSurface;
}

EGLSurface EGLCore::createOffscreenSurface(int width, int height) {
    EGLSurface eglSurface = NULL;
    EGLint attribs[] = {
            EGL_WIDTH, width,
            EGL_HEIGHT, height,
            EGL_NONE
    };
    if (!(eglSurface = eglCreatePbufferSurface(eglDisplay, eglConfig, attribs))) {
        LOGE("eglCreatePbufferSurface returned error = %d", eglGetError());
    }
    return eglSurface;
}

EGLBoolean EGLCore::makeCurrent(EGLSurface eglSurface) {
    return eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
}

void EGLCore::doneCurrent() {
    eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
}

EGLBoolean EGLCore::swapBuffer(EGLSurface eglSurface) {
    return eglSwapBuffers(eglDisplay, eglSurface);
}

int EGLCore::querySurface(EGLSurface surface, int what) {
    EGLint result = -1;
    eglQuerySurface(eglDisplay, surface, what, &result);
    return result;
}

int EGLCore::setPresentationTime(EGLSurface surface, khronos_stime_nanoseconds_t nsecs) {
    return 0;
}

void EGLCore::releaseSurface(EGLSurface eglSurface) {
    eglDestroySurface(eglDisplay, eglSurface);
    eglSurface = EGL_NO_SURFACE;
}

void EGLCore::release() {
    if (eglContext != NULL) {
        eglDestroyContext(eglDisplay, eglContext);
        eglContext = EGL_NO_CONTEXT;
    }
    if (eglDisplay != NULL) {
        eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDisplay = EGL_NO_DISPLAY;
    }
}

EGLContext EGLCore::getContext() {
    return eglContext;
}

EGLDisplay EGLCore::getDisplay() {
    return eglDisplay;
}

EGLConfig EGLCore::getConfig() {
    return eglConfig;
}
