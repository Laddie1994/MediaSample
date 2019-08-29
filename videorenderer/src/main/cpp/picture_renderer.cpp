//
// Created by yc on 19-8-26.
//

#include "picture_renderer.h"

GLuint PictureRederer::loadShader(GLenum type, const GLchar *source) {
    GLuint shader = glCreateShader(type);
    if (shader != 0){
        glShaderSource(shader, 1, &source, NULL);
        glCompileShader(shader);
        GLint status;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
        if (status != GL_TRUE) {
            LOGE("load shader error");
            glDeleteShader(shader);
            shader = 0;
        }
    }
    return shader;
}

int PictureRederer::initShader() {
    vertextBuffer = loadShader(GL_VERTEX_SHADER, vertex_coordinate);
    if (vertextBuffer == 0){
        LOGE("load vertext fail");
        return -1;
    }
    fragmentBuffer = loadShader(GL_FRAGMENT_SHADER, fragment_coordinate);
    if (fragmentBuffer == 0){
        LOGE("load fragment fail");
        return -1;
    }
    return 0;
}

int PictureRederer::useProgram() {
    glUseProgram(glProgram);
    return 0;
}

GLuint PictureRederer::createProgram(GLuint vertext, GLuint fragment) {
    GLuint glProgram = glCreateProgram();
    glAttachShader(glProgram, vertext);
    glAttachShader(glProgram, fragment);
    glLinkProgram(glProgram);
    GLint status;
    glGetProgramiv(glProgram, GL_LINK_STATUS, &status);
    if (status != GL_TRUE){
        glDeleteProgram(glProgram);
        glProgram = 0;
    }
    return glProgram;
}

void PictureRederer::initialized() {
    initShader();
    glProgram = createProgram(vertextBuffer, fragmentBuffer);
    matrixLocation = glGetUniformLocation(glProgram, "vMatrix");
    textureLocation = glGetUniformLocation(glProgram, "vTexture");
    posutionLocation = glGetAttribLocation(glProgram, "vPosition");
    coordinateLocation = glGetAttribLocation(glProgram, "vCoordinate");
}

void PictureRederer::sizeChange(int width, int height) {
    glViewport(0, 0, width, height);

}
