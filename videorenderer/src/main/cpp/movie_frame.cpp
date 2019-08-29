//
// Created by yc on 19-8-27.
//

#include "movie_frame.h"

AudioFrame::AudioFrame() {
    this->samples = NULL;
    this->size = 0;
    this->duration = 0;
}

AudioFrame::~AudioFrame() {
    if (samples != NULL){
        delete[] samples;
        samples = NULL;
    }
}

VideoFrame::VideoFrame() {
    this->width = 0;
    this->height = 0;
    this->luma = NULL;
    this->chromaB = NULL;
    this->chromaR = NULL;
}

VideoFrame::~VideoFrame() {
    if (luma){
        delete[] luma;
        luma = NULL;
    }
    if (chromaB){
        delete[] chromaB;
        chromaB = NULL;
    }
    if (chromaR){
        delete[] chromaR;
        chromaR = NULL;
    }
}

MovieFrame::MovieFrame() {
    this->position = 0;
    this->duration = 0;
}
