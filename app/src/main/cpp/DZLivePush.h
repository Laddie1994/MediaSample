//
// Created by 曾辉 on 2019-07-03.
//

#ifndef NDK_DAY03_DZRTMP_H
#define NDK_DAY03_DZRTMP_H

#include "DZPacketQueue.h"
#include <malloc.h>
#include <string.h>
#include <pthread.h>
#include "DZConstDefine.h"
#include "DZJNICall.h"

extern "C" {
#include "rtmp/rtmp.h"
};

class DZLivePush {
public:
    char *url = NULL;
    DZPacketQueue *pPacketQueue = NULL;
    RTMP *pRtmp = NULL;
    DZJNICall *pJniCall = NULL;
public:
    DZLivePush(const char *url, DZJNICall *pJniCall);

    ~DZLivePush();

public:
    /**
     * 初始化连接
     */
    void initConnect();
};


#endif //NDK_DAY03_DZRTMP_H
