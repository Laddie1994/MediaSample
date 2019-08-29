//
// Created by yc on 19-8-27.
//

#ifndef OPENGLSAMPLE_MOVIE_FRAME_H
#define OPENGLSAMPLE_MOVIE_FRAME_H

#include <cstdint>

typedef enum {
    MOVIE_FRAME_NONE, MOVIE_FRAME_AUDIO, MOVIE_FRAME_VIDEO
} MovieFrameType;

class MovieFrame {
public:
    float position;
    float duration;
    MovieFrame();
    virtual MovieFrameType getType(){
        return MOVIE_FRAME_NONE;
    }
};

class AudioFrame : public MovieFrame {
public:
    void *samples;
    int size;

    AudioFrame();

    ~AudioFrame();

    MovieFrameType getType() {
        return MOVIE_FRAME_AUDIO;
    };
};

class VideoFrame : public MovieFrame {
public:
    int width;
    int height;
    uint8_t *luma;
    uint8_t *chromaB;
    uint8_t *chromaR;

    VideoFrame();

    ~VideoFrame();

    MovieFrameType getType() {
        return MOVIE_FRAME_VIDEO;
    }
};

#endif //OPENGLSAMPLE_MOVIE_FRAME_H
