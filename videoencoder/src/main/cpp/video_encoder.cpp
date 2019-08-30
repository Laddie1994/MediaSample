//
// Created by yc on 19-8-30.
//

#include "video_encoder.h"

VideoEncoder::VideoEncoder(const char *outPath, int width, int height, int bitRate) {
    this->outPath = outPath;
    this->width = width;
    this->height = height;
    this->bitRate = bitRate;
    this->frameRate = {fps, 1};
    this->timeBase = {1, fps};
}

int VideoEncoder::prepare() {
    // 注册编解码器
    av_register_all();

    // 结果码
    int result;

    // 获取上下文
    if ((result = avformat_alloc_output_context2(&pFormatContext, NULL, "flv", outPath) < 0)) {
        LOGE("avformat_alloc_output_context2 error: %s", av_err2str(result));
        return -1;
    }

    // 获取编码器
    pCodec = avcodec_find_encoder(AV_CODEC_ID_H264);
    if (!pCodec) {
        LOGE("avcodec_find_encoder error");
        return -1;
    }

    // 分配编码器上下文
    pCodecContext = avcodec_alloc_context3(pCodec);
    // 设置编码器ID
    pCodecContext->codec_id = pCodec->id;
    // 设置编码类型，这里是视频编码
    pCodecContext->codec_type = AVMEDIA_TYPE_VIDEO;
    // 设置宽度
    pCodecContext->width = height;
    // 设置高度
    pCodecContext->height = width;
    // 设置码率，即每秒传送的比特数，比特率越高传送速度越快
    pCodecContext->bit_rate = bitRate;
    // 设置帧率
    pCodecContext->framerate = frameRate;
    // 设置时间基数
    pCodecContext->time_base = timeBase;
    // 设置视频格式，这里编码成YUV420P格式
    pCodecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    // 设置每51插入一个关键帧，关键帧越少视频越小
    pCodecContext->gop_size = 50;
    // 设置B帧最大值
    pCodecContext->max_b_frames = 0;
    // 设置最大量化系数，默认=51
    pCodecContext->qmax = 51;
    // 设置最小量化系数，默认=10
    pCodecContext->qmin = 10;
    pCodecContext->level = 41;
    pCodecContext->refs = 1;
    pCodecContext->qcompress = 0.6;

    if (pFormatContext->oformat->flags & AVFMT_GLOBALHEADER) {
        pCodecContext->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

    // 打开解码器
    AVDictionary *dictionary = NULL;
    // 设置预备参数，slow=慢、superfast=超快
    av_dict_set(&dictionary, "preset", "superfast", 0);
    // 设置调优参数，zerolatency=零延迟
    av_dict_set(&dictionary, "tune", "zerolatency", 0);
    if ((result = avcodec_open2(pCodecContext, pCodec, &dictionary)) != 0) {
        LOGE("avcodec_open2 error: %s", av_err2str(result));
        return -1;
    }
    // 分配输出流
    pStream = avformat_new_stream(pFormatContext, pCodec);
    if (pStream == NULL) {
        LOGE("avformat_new_stream error: %s", av_err2str(result));
        return -1;
    }
    pStream->time_base.num = 1;
    pStream->time_base.den = fps;
    pStream->codecpar->codec_tag = 0;
    if ((result = avcodec_parameters_from_context(pStream->codecpar, pCodecContext)) < 0) {
        LOGE("avcodec_parameters_to_context error: %s", av_err2str(result));
        return -1;
    }

    // 打开输出文件
    if ((result = avio_open(&pFormatContext->pb, outPath, AVIO_FLAG_READ_WRITE)) < 0) {
        LOGE("avio_open error: %s", av_err2str(result));
        return -1;
    }

    // 写入头文件
    if ((result = avformat_write_header(pFormatContext, NULL)) < 0) {
        LOGE("avformat_write_header error: %s", av_err2str(result));
        return -1;
    }

    // 初始化视频帧缓冲区
    pFrame = av_frame_alloc();
    pFrame->width = pCodecContext->width;
    pFrame->height = pCodecContext->height;
    pFrame->format = pCodecContext->pix_fmt;
    // 获取一帧大小
    int bufferSize = av_image_get_buffer_size(pCodecContext->pix_fmt, pCodecContext->width,
                                             pCodecContext->height, 1);
    // 开辟缓存区
    frameBuffer = (uint8_t *) av_malloc(bufferSize);
    av_image_fill_arrays(pFrame->data, pFrame->linesize, frameBuffer, pCodecContext->pix_fmt,
                         pCodecContext->width, pCodecContext->height, 1);

    // 开辟压缩视频帧
    av_new_packet(&pPacket, bufferSize * 3);
    return 0;
}

int VideoEncoder::encodeFrame(AVFrame *pFrame, AVPacket *pPacket) {
    int result;
    if ((result = avcodec_send_frame(pCodecContext, pFrame)) != 0) {
        LOGE("avcodec_send_frame error: %s", av_err2str(result));
        return -1;
    }
    while (!result) {
        result = avcodec_receive_packet(pCodecContext, pPacket);
        if (result == AVERROR(EAGAIN) || result == AVERROR_EOF) {
            return 0;
        } else if (result < 0) {
            //error during encoding
            LOGE("avcodec_receive_packet error: %s", av_err2str(result));
            return -1;
        }
        pFrame->pts = index;
//        AVRational timeBase = pFormatContext->streams[0]->time_base;
        pPacket->stream_index = pStream->index;
        pPacket->pts = index * (pStream->time_base.den) / ((pStream->time_base.num) * fps);
        pPacket->dts = pPacket->pts;
        pPacket->duration = (pStream->time_base.den) / ((pStream->time_base.num) * fps);
        pPacket->pos = -1;
        //写入文件
        if ((result = av_interleaved_write_frame(pFormatContext, pPacket)) != 0) {
            LOGE("av_interleaved_write_frame error: %s", av_err2str(result));
        }
        av_packet_unref(pPacket);
        index++;
    }
    return 0;
}

int VideoEncoder::encodeBuffer(unsigned char *buffer, int length) {
    uint8_t *buffer_y = frameBuffer;
    uint8_t *buffer_u = frameBuffer + width * height;
    uint8_t *buffer_v = frameBuffer + width * height + ((width >> 1) * (height >> 1));
    libyuv::ConvertToI420(buffer, width * height, buffer_y, height, buffer_u, height >> 1, buffer_v,
                          height >> 1, 0, 0, width, height, width, height, libyuv::kRotate90,
                          libyuv::FOURCC_NV21);
    pFrame->data[0] = buffer_y;
    pFrame->data[1] = buffer_u;
    pFrame->data[2] = buffer_v;

    encodeFrame(pFrame, &pPacket);
    return 0;
}

void VideoEncoder::stopEncode() {
    int result = encodeFrame(NULL, &pPacket);
    if (result < 0){
        return;
    }
    av_write_trailer(pFormatContext);
    av_free(&pPacket);
    av_free(&pFrame);
    free(frameBuffer);
    avcodec_close(pCodecContext);
    avcodec_free_context(&pCodecContext);
    avio_close(pFormatContext->pb);
    avformat_free_context(pFormatContext);
}
