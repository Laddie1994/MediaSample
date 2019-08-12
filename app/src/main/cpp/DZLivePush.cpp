//
// Created by 曾辉 on 2019-07-03.
//

#include "DZLivePush.h"

DZLivePush::DZLivePush(const char *url, DZJNICall *pJniCall) {
    this->url = (char *) (malloc(strlen(url) + 1));
    strcpy(this->url, url);
    pPacketQueue = new DZPacketQueue();
    this->pJniCall = pJniCall;
}

DZLivePush::~DZLivePush() {
    if (pPacketQueue != NULL) {
        delete pPacketQueue;
        pPacketQueue = NULL;
    }

    if (url != NULL) {
        free(url);
        url = NULL;
    }

    if (pRtmp != NULL) {
        RTMP_Close(pRtmp);
        RTMP_Free(pRtmp);
    }
}

/**
 * 初始化连接流媒体服务器
 */
void *initConnectFun(void *context) {
    DZLivePush *pLivePush = (DZLivePush *) context;
    // 创建 RTMP
    pLivePush->pRtmp = RTMP_Alloc();
    // 初始化 RTMP
    RTMP_Init(pLivePush->pRtmp);
    // 设置连接超时
    pLivePush->pRtmp->Link.timeout = 20;
    pLivePush->pRtmp->Link.lFlags |= RTMP_LF_LIVE;
    RTMP_SetupURL(pLivePush->pRtmp, pLivePush->url);
    RTMP_EnableWrite(pLivePush->pRtmp);

    if (!RTMP_Connect(pLivePush->pRtmp, NULL)) {
        LOGE("connect url error");
        pLivePush->pJniCall->callConnectError(THREAD_CHILD, RTMP_CONNECT_ERROR_CODE,
                                              "connect url error");
        return (void *) RTMP_CONNECT_ERROR_CODE;
    }
    if (!RTMP_ConnectStream(pLivePush->pRtmp, 0)) {
        LOGE("connect stream url error");
        pLivePush->pJniCall->callConnectError(THREAD_CHILD, RTMP_STREAM_CONNECT_ERROR_CODE,
                                              "connect stream url error");
        return (void *) RTMP_STREAM_CONNECT_ERROR_CODE;
    }

    LOGE("connect succeed");
    pLivePush->pJniCall->callConnectSuccess(THREAD_CHILD);
    return (void *) 0;
}

/**
 * 初始化连接
 */
void DZLivePush::initConnect() {
    pthread_t initConnectTid;
    pthread_create(&initConnectTid, NULL, initConnectFun, this);
    pthread_detach(initConnectTid);
}
