//
// Created by yc on 19-8-26.
//

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include "video_texture.h"

VideoTexture::VideoTexture() {
}

int VideoTexture::createTexture() {
    glGenTextures(3, textureId);
    for (int i = 0; i < 3; ++i) {
        glBindTexture(GL_TEXTURE_2D, textureId[i]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, 600, 1049, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    return 0;
}

bool VideoTexture::bindTexture(GLuint* uniformSampler) {
    for (int i = 0; i < 3; ++i) {
        glActiveTexture(GL_TEXTURE0 + i);
        glBindTexture(GL_TEXTURE_2D, textureId[i]);
        glUniform1i(uniformSampler[i], i);
    }
    return true;
}

void VideoTexture::dealloc() {
    if (textureId[0]){
        glDeleteTextures(3, textureId);
    }
}

void VideoTexture::updateTexImage(VideoFrame *pFrame) {
    int width = pFrame->width;
    int height = pFrame->height;
    if (width % 16 != 0){
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    }
    uint8_t* pixels[3] = {pFrame->luma, pFrame->chromaB, pFrame->chromaR};
    int widths[] = {width, width >> 1, width >> 1};
    int heights[] = {height, height >> 1, height >> 1};
    for (int i = 0; i < 3; ++i) {
        glActiveTexture(GL_TEXTURE0 + i);
        glBindTexture(GL_TEXTURE_2D, textureId[i]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, widths[i], heights[i], 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pixels[i]);
    }
}
