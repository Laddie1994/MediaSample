//
// Created by 曾辉 on 2019-07-03.
//

#ifndef NDK_DAY03_DZPACKETQUEUE_H
#define NDK_DAY03_DZPACKETQUEUE_H

#include <queue>
#include <pthread.h>
#include <malloc.h>

extern "C" {
#include "rtmp/rtmp.h"
};

class DZPacketQueue {
public:
    std::queue<RTMPPacket *> *pPacketQueue;
    pthread_mutex_t packetMutex;
    pthread_cond_t packetCond;
public:
    DZPacketQueue();

    ~DZPacketQueue();

public:
    void push(RTMPPacket *pPacket);

    RTMPPacket *pop();

    /**
     * 清除整个队列
     */
    void clear();
};


#endif //NDK_DAY03_DZPACKETQUEUE_H
