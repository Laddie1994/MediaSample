
#include <jni.h>
#include "video_encoder.h"

VideoEncoder *videoEncoder;

extern "C" JNIEXPORT void JNICALL
Java_com_yu_videoencoder_VideoEncoder_initialize(JNIEnv *env, jobject instalce, jstring outPath,
                                                 jint width, jint height, jint bitRate) {
    const char *cOutPath = env->GetStringUTFChars(outPath, JNI_FALSE);
    videoEncoder = new VideoEncoder(cOutPath, width, height, bitRate);
    videoEncoder->prepare();
    env->ReleaseStringUTFChars(outPath, cOutPath);
}

extern "C" JNIEXPORT void JNICALL
Java_com_yu_videoencoder_VideoEncoder_encode(JNIEnv *env, jobject instalce, jbyteArray buffer,
                                             jint length) {
    jbyte *videoBuffer = env->GetByteArrayElements(buffer, JNI_FALSE);
    if (videoEncoder != NULL) {
        videoEncoder->encodeBuffer((unsigned char *) (videoBuffer), length);
    }
    env->ReleaseByteArrayElements(buffer, videoBuffer, NULL);
}

extern "C" JNIEXPORT void JNICALL
Java_com_yu_videoencoder_VideoEncoder_stopEncode(JNIEnv *env, jobject instalce) {
    if (videoEncoder != NULL) {
        videoEncoder->stopEncode();
    }
}
