//
// Created by yc on 19-8-26.
//

#ifndef OPENGLSAMPLE_PICTURE_RENDERER_H
#define OPENGLSAMPLE_PICTURE_RENDERER_H

#include "const_util.h"
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

static const char* vertex_coordinate =
        "attribute vec4 vPosition;\n"
        "attribute vec2 vCoordinate;\n"
        "uniform mat4 vMatrix;\n"
        "varying vec2 aCoordinate;\n"
        "varying vec4 aPosition;\n"
        "void main(){\n"
        "    gl_Position = vMatrix * vPosition;\n"
        "    aCoordinate = vCoordinate;\n"
        "    aPosition = vMatrix * vPosition;\n"
        "}";

static const char* fragment_coordinate =
        "precision mediump float;\n"
        "uniform sampler2D vTexture;\n"
        "varying vec2 aCoordinate;\n"
        "void main(){\n"
        "    gl_FragColor = texture2D(vTexture, aCoordinate);\n"
        "}";

class PictureRederer {
private:
    GLuint vertextBuffer;
    GLuint fragmentBuffer;
    GLuint glProgram;
    GLint matrixLocation;
    GLint textureLocation;
    GLint posutionLocation;
    GLint coordinateLocation;
public:
    void initialized();
    int initShader();
    GLuint loadShader(GLenum type, const GLchar* source);
    GLuint createProgram(GLuint vertext, GLuint fragment);
    int useProgram();
    void sizeChange(int width, int height);
};


#endif //OPENGLSAMPLE_PICTURE_RENDERER_H
