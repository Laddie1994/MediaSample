//
// Created by yc on 19-8-26.
//
#include <jni.h>
#include "const_util.h"
#include "video_renderer_controller.h"

void videoRenderer(JNIEnv *env, jobject instance, jstring path, jobject surface, jint width, jint height) {
    LOGE("native register success");
    const char* cPath = env->GetStringUTFChars(path, JNI_FALSE);
    VideoRendererController* controller = new VideoRendererController(env, surface, cPath);
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    controller->onCreateSurface(window, width, height);
    controller->start();
    env->ReleaseStringUTFChars(path, cPath);
}

jint RegisterNatives(JNIEnv *env) {
    jclass clazz = env->FindClass("com/yu/videorenderer/VideoRenderer");
    if (clazz == NULL) {
        return JNI_ERR;
    }
    JNINativeMethod native_methods[] = {
            {"nativeRenderer", "(Ljava/lang/String;Landroid/view/Surface;II)V", (void *) videoRenderer}
    };
    return env->RegisterNatives(clazz, native_methods,
                                sizeof(native_methods) / sizeof(native_methods[0]));
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {

    JNIEnv *jniEnv = NULL;

    if (vm->GetEnv((void **) &jniEnv, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jint result = RegisterNatives(jniEnv);

    return JNI_VERSION_1_6;

}