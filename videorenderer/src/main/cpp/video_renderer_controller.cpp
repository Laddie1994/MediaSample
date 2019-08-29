//
// Created by yc on 19-8-26.
//

#include "video_renderer_controller.h"

VideoRendererController::VideoRendererController(JNIEnv *env, jobject surface, const char *path) {
    this->env = env;
    videoDecoder = new VideoDecoder(this, path);
    videoBufferQueue = new std::queue<VideoFrame *>();
    audioBufferQueue = new std::queue<AudioFrame *>();
    audioPlayer = new AudioPlayer(this);
    videoPlayer = new VideoPlayer(this);
    videoDecoder->initialize();
    audioPlayer->registerCallback(audioPlayerFillData);
    videoDecoder->registerDecodeCallback(videoDecoderCallback, audioDecoderCallback);
    videoPlayer->registerCallback(videoPlayerFillData);
}

VideoRendererController::~VideoRendererController() {
    delete videoBufferQueue;
    videoBufferQueue = NULL;
    delete audioBufferQueue;
    audioBufferQueue = NULL;
    delete videoDecoder;
    videoDecoder = NULL;
    delete audioPlayer;
    audioPlayer = NULL;
    delete videoPlayer;
    videoPlayer = NULL;
}

void VideoRendererController::onCreateSurface(ANativeWindow *window, int width, int height) {
    if (!window || width <= 0 || height <= 0) {
        return;
    }
    if (videoPlayer){
        videoPlayer->onCreateSurface(window);
    }
    if (videoPlayer) {
        videoPlayer->onSizeChange(width, height);
    }
}

bool VideoRendererController::start() {
    videoDecoder->start();
    audioPlayer->start();
    videoPlayer->start();
    return false;
}

void VideoRendererController::stop() {
    videoDecoder->stop();
    audioPlayer->stop();
    videoPlayer->stop();
}

void VideoRendererController::release() {
    videoDecoder->release();
    audioPlayer->release();
    videoPlayer->release();
}

int VideoRendererController::videoDecoderCallback(VideoFrame **pFrame, void *ctx) {
    VideoRendererController *controller = (VideoRendererController *) ctx;
    return controller->enqueueVideoFrame(pFrame);
}

int VideoRendererController::enqueueVideoFrame(VideoFrame **pFrame) {
    videoBufferQueue->push(*pFrame);
    return 0;
}

int VideoRendererController::audioDecoderCallback(AudioFrame **pFrame, void *ctx) {
    VideoRendererController *controller = (VideoRendererController *) ctx;
    return controller->enqueueAudioFrame(pFrame);
}

int VideoRendererController::enqueueAudioFrame(AudioFrame **pFrame) {
    audioBufferQueue->push(*pFrame);
    return 0;
}

int VideoRendererController::audioPlayerFillData(AudioFrame **buffer, void *ctx) {
    VideoRendererController *controller = (VideoRendererController *) ctx;
    return controller->consumeAudioFillData(buffer);
}

int VideoRendererController::consumeAudioFillData(AudioFrame **pAudioFrame) {
    int size = audioBufferQueue->size();
    if (size > 0) {
        (*pAudioFrame) = audioBufferQueue->front();
        moviePosition = (*pAudioFrame)->position;
        audioBufferQueue->pop();
        return (*pAudioFrame)->size;
    }
    return 0;
}

int VideoRendererController::videoPlayerFillData(VideoFrame **buffer, void *ctx) {
    VideoRendererController *controller = (VideoRendererController *) ctx;
    return controller->consumeVideoFillData(buffer);
}

int VideoRendererController::consumeVideoFillData(VideoFrame **buffer) {
    VideoFrame *resultFrame = NULL;
    int size = videoBufferQueue->size();
    if (size > 0) {
        while (true) {
            resultFrame = videoBufferQueue->front();
            const float delta =
                    (moviePosition - DEFAULT_AUDIO_BUFFER_DURATION_IN_SECS) - resultFrame->position;
            if (delta < (0 - LOCAL_AV_SYNC_MAX_TIME_DIFF)) {
                //视频比音频快了好多,我们还是渲染上一帧
//				LOGI("视频比音频快了好多,我们还是渲染上一帧 moviePosition is %.4f texture->position is %.4f", moviePosition, texture->position);
                resultFrame = NULL;
                break;
            }
            videoBufferQueue->pop();
            if (delta > LOCAL_AV_SYNC_MAX_TIME_DIFF) {
                //视频比音频慢了好多,我们需要继续从queue拿到合适的帧
//				LOGI("视频比音频慢了好多,我们需要继续从queue拿到合适的帧 moviePosition is %.4f texture->position is %.4f", moviePosition, texture->position);
                delete resultFrame;
                continue;
            } else {
                break;
            }
        }
    }
    if (resultFrame && size == 0 && videoDecoder->isVideoEOF()) {
        return VIDEO_DECODE_COMPLETE;
    }
    (*buffer) = resultFrame;
    return 0;
}