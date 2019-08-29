//
// Created by yc on 19-8-28.
//

#ifndef OPENGLSAMPLE_VIDEO_PLAYER_H
#define OPENGLSAMPLE_VIDEO_PLAYER_H


#include "video_renderer.h"
#include "egl_core.h"
#include <jni.h>

typedef int (*videoFillDataCallback)(VideoFrame**, void*);

typedef enum {
    STATU_NONE, STATU_CREATE, STATU_CHANGE, STATU_DRAW, STATU_EXIT
} RendererStatus;

class VideoPlayer {
private:
    VideoRenderer *videoRenderer;
    pthread_t startThread;
    RendererStatus status = STATU_NONE;
    bool shouldLoop;
    EGLCore *eglCore;
    ANativeWindow *nativeWindow;
    EGLSurface eglSurface;
    videoFillDataCallback productAudioDataCallback;
    void* ctx;
    int width;
    int height;
public:
    VideoPlayer(void* ctx);
    ~VideoPlayer();
    void registerCallback(videoFillDataCallback callback);
    void onCreateSurface(ANativeWindow* window);
    void onSizeChange(int width, int height);
    void start();
    void stop();
    void prepare();
    void release();
    static void *threadStartCallback(void *ctx);
    void rendererLoop();
    void drawFrame();
};


#endif //OPENGLSAMPLE_VIDEO_PLAYER_H
