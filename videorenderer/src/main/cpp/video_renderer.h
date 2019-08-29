//
// Created by yc on 19-8-26.
//

#ifndef OPENGLSAMPLE_VIDEO_RENDERER_H
#define OPENGLSAMPLE_VIDEO_RENDERER_H

#include "const_util.h"
#include "video_texture.h"
#include "video_decoder.h"
#include "movie_frame.h"
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

static const char* vertext =
        "attribute vec2 aTexCoord;\n"
        "attribute vec4 aVertexCoord;\n"
        "varying highp vec2 vTextureCoord;\n"
        "uniform highp mat4 uVerMatrix;\n"
        "void main(){\n"
        "    vTextureCoord = aTexCoord;\n"
        "    gl_Position = uVerMatrix * aVertexCoord;\n"
        "}";

static const char* fragment =
        "#extension GL_OES_EGL_image_external : require\n"
        "precision mediump float;\n"
        "varying highp vec2 vTextureCoord;\n"
        "uniform sampler2D uTextureY;\n"
        "uniform sampler2D uTextureU;\n"
        "uniform sampler2D uTextureV;\n"
        "void main(){\n"
        "    highp float y = texture2D(uTextureY, vTextureCoord).r;\n"
        "    highp float u = texture2D(uTextureU, vTextureCoord).r - 0.5;\n"
        "    highp float v = texture2D(uTextureV, vTextureCoord).r - 0.5;\n"
        "    highp float r = y + 1.402 * v;\n"
        "    highp float g = y - 0.344 * u - 0.714 * v;\n"
        "    highp float b = y + 1.772 * u;\n"
        "    gl_FragColor = vec4(r, g, b, 1.0);\n"
        "}";

static const GLfloat vertext_coordinate[] = {
        -1.0f, 1.0f,
        -1.0f, -1.0f,
        1.0f, 1.0f,
        1.0f, -1.0f
};

static const GLfloat fragment_coordinate[] = {
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
};

class VideoRenderer {
private:
    GLuint vertextBuffer;
    GLuint fragmentBuffer;
    GLuint glProgram;
    VideoTexture* videoTexture;
    GLuint vertextCoordLocation;
    GLuint texCoordLocation;
    GLuint verMatrixLocation;
    GLuint textureLocation[3];
public:
    void initialized();
    int initShader();
    GLuint loadShader(GLenum type, const GLchar* source);
    GLuint createProgram(GLuint vertext, GLuint fragment);
    void sizeChange(int width, int height);
    void drawFrame(VideoFrame* frame);
    void release();
};


#endif //OPENGLSAMPLE_VIDEO_RENDERER_H
