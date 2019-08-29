//
// Created by yc on 19-8-28.
//

#include "video_player.h"
#include "video_renderer_controller.h"

VideoPlayer::VideoPlayer(void *ctx) {
    this->ctx = ctx;
    eglCore = new EGLCore();
    videoRenderer = new VideoRenderer();
    status = STATU_CREATE;
}

void VideoPlayer::onCreateSurface(ANativeWindow *window) {
    if (!window){
        return;
    }
    nativeWindow = window;
}

void VideoPlayer::onSizeChange(int width, int height) {
    this->width = width;
    this->height = height;
    videoRenderer->sizeChange(width, height);
}

void *VideoPlayer::threadStartCallback(void *ctx) {
    VideoPlayer *controller = (VideoPlayer *) ctx;
    controller->rendererLoop();
    return 0;
}

void VideoPlayer::rendererLoop() {
    shouldLoop = true;
    while (shouldLoop) {
        switch (status) {
            case STATU_NONE:
                break;
            case STATU_CREATE:
                prepare();
                break;
            case STATU_CHANGE:
//                videoRenderer->sizeChange()
                break;
            case STATU_DRAW:
                drawFrame();
                usleep(16 / 1000);
                break;
            case STATU_EXIT:
                shouldLoop = false;
                release();
                break;
        }
    }
}

void VideoPlayer::drawFrame() {
    VideoFrame *pFrame = NULL;
    int result = productAudioDataCallback(&pFrame, ctx);
    if (!result && pFrame) {
        int frameWidth = pFrame->width;
        int frameHeight= pFrame->height;
        videoRenderer->drawFrame(pFrame);
        eglCore->swapBuffer(eglSurface);
        delete pFrame;
    } else if(result == VIDEO_DECODE_COMPLETE) {
        stop();
    }
}

void VideoPlayer::start() {
    pthread_create(&startThread, NULL, threadStartCallback, this);
    pthread_detach(startThread);
}

void VideoPlayer::stop() {
    status = STATU_EXIT;
    pthread_join(startThread, 0);
}

void VideoPlayer::prepare() {
    eglCore->initialize();
    eglSurface = eglCore->createWindowSurface(nativeWindow);
    eglCore->makeCurrent(eglSurface);
    videoRenderer->initialized();
    status = STATU_DRAW;
}

void VideoPlayer::release() {
    eglCore->doneCurrent();
    eglCore->releaseSurface(eglSurface);
    eglCore->release();
    videoRenderer->release();
}

void VideoPlayer::registerCallback(videoFillDataCallback callback) {
    this->productAudioDataCallback = callback;
}

VideoPlayer::~VideoPlayer() {

}