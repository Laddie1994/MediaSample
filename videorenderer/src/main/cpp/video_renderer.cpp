//
// Created by yc on 19-8-26.
//

#include "video_renderer.h"
#include "matrix.h"

void VideoRenderer::initialized() {
    initShader();
    glProgram = createProgram(vertextBuffer, fragmentBuffer);
    vertextCoordLocation = glGetAttribLocation(glProgram, "aVertexCoord");
    texCoordLocation = glGetAttribLocation(glProgram, "aTexCoord");
    textureLocation[0] = glGetUniformLocation(glProgram, "uTextureY");
    textureLocation[1] = glGetUniformLocation(glProgram, "uTextureU");
    textureLocation[2] = glGetUniformLocation(glProgram, "uTextureV");
    verMatrixLocation = glGetUniformLocation(glProgram, "uVerMatrix");

    videoTexture = new VideoTexture();
    int result = videoTexture->createTexture();
    if (result < 0){
        videoTexture->dealloc();
    }
}

int VideoRenderer::initShader() {
    vertextBuffer = loadShader(GL_VERTEX_SHADER, vertext);
    if (vertextBuffer == 0){
        LOGE("load vertext fail %d", glGetError());
        return -1;
    }
    fragmentBuffer = loadShader(GL_FRAGMENT_SHADER, fragment);
    if (fragmentBuffer == 0){
        LOGE("load fragment fail");
        return -1;
    }
    return 0;
}

GLuint VideoRenderer::loadShader(GLenum type, const GLchar *source) {
    GLuint shader = glCreateShader(type);
    if (shader){
        glShaderSource(shader, 1, &source, NULL);
        glCompileShader(shader);
        GLint status;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
        if (!status) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*) malloc(infoLen);
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    LOGE("Could not compile shader %d:\n%s\n", type, buf);
                    free(buf);
                }
            } else {
                LOGE( "Guessing at GL_INFO_LOG_LENGTH size\n");
                char* buf = (char*) malloc(0x1000);
                if (buf) {
                    glGetShaderInfoLog(shader, 0x1000, NULL, buf);
                    LOGE("Could not compile shader %d:\n%s\n", type, buf);
                    free(buf);
                }
            }
            glDeleteShader(shader);
            shader = 0;
        }
    }
    return shader;
}

GLuint VideoRenderer::createProgram(GLuint vertext, GLuint fragment) {
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

void VideoRenderer::sizeChange(int width, int height) {
    glViewport(0, 0, width, height);
}

void VideoRenderer::drawFrame(VideoFrame* frame) {
    videoTexture->updateTexImage(frame);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
    glEnable(GL_BLEND);
    glUseProgram(glProgram);

    glEnableVertexAttribArray(vertextCoordLocation);
    glVertexAttribPointer(vertextCoordLocation, 2, GL_FLOAT, GL_FALSE, 0, vertext_coordinate);

    glEnableVertexAttribArray(texCoordLocation);
    glVertexAttribPointer(texCoordLocation, 2, GL_FLOAT, GL_FALSE, 0, fragment_coordinate);

    videoTexture->bindTexture(textureLocation);

    float verMatrix[4*4];
    matrixSetIdentityM(verMatrix);
    glUniformMatrix4fv(verMatrixLocation, 1, GL_FALSE, verMatrix);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    
    glDisableVertexAttribArray(vertextCoordLocation);
    glDisableVertexAttribArray(texCoordLocation);

    glBindTexture(GL_TEXTURE_2D, 0);
}

void VideoRenderer::release() {
    if (glProgram){
        glDeleteProgram(glProgram);
        glProgram = NULL;
    }
    if (videoTexture){
        videoTexture->dealloc();
        free(videoTexture);
        videoTexture = NULL;
    }
}
