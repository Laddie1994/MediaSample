//
// Created by yc on 19-8-26.
//

#ifndef OPENGLSAMPLE_VIDEO_RENDERER_CONTROLLER_H
#define OPENGLSAMPLE_VIDEO_RENDERER_CONTROLLER_H

#include <android/native_window.h>
#include <android/native_window_jni.h>
#include "egl_core.h"
#include "video_texture.h"
#include "video_renderer.h"
#include "movie_frame.h"
#include "audio_player.h"
#include <queue>
#include <jni.h>
#include <unistd.h>
#include "video_player.h"

using namespace std;

#define DEFAULT_AUDIO_BUFFER_DURATION_IN_SECS 0.03
#define LOCAL_AV_SYNC_MAX_TIME_DIFF             0.05

class VideoRendererController {
private:
    VideoDecoder *videoDecoder;
    std::queue<VideoFrame *> *videoBufferQueue;
    std::queue<AudioFrame *> *audioBufferQueue;
    JNIEnv *env;
    AudioPlayer *audioPlayer;
    VideoPlayer *videoPlayer;
    float moviePosition;
public:
    VideoRendererController(JNIEnv *env, jobject surface, const char *path);

    ~VideoRendererController();

    void onCreateSurface(ANativeWindow* window, int width, int height);

    bool start();

    void stop();

    void release();

    static int videoDecoderCallback(VideoFrame **pFrame, void *ctx);

    int enqueueVideoFrame(VideoFrame **pFrame);

    static int audioDecoderCallback(AudioFrame **pFrame, void *ctx);

    int enqueueAudioFrame(AudioFrame **pFrame);

    static int audioPlayerFillData(AudioFrame **buffer, void *ctx);

    int consumeAudioFillData(AudioFrame **buffer);

    static int videoPlayerFillData(VideoFrame **buffer, void *ctx);

    int consumeVideoFillData(VideoFrame **buffer);
};


#endif //OPENGLSAMPLE_VIDEO_RENDERER_CONTROLLER_H
