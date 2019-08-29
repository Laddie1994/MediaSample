//
// Created by yc on 19-8-26.
//

#ifndef OPENGLSAMPLE_VIDEO_TEXTURE_H
#define OPENGLSAMPLE_VIDEO_TEXTURE_H

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include "video_decoder.h"

class VideoTexture{
private:
    GLuint textureId[3];
public:
    VideoTexture();
    ~VideoTexture();
    int createTexture();
    void updateTexImage(VideoFrame *pFrame);
    bool bindTexture(GLuint* uniformSampler);
    void dealloc();
};


#endif //OPENGLSAMPLE_VIDEO_TEXTURE_H
