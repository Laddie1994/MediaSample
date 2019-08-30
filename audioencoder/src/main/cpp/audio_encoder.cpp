//
// Created by yc on 19-8-29.
//

#include "audio_encoder.h"

AudioEncoder::AudioEncoder(const char *outPath, int bitRate, int sampleRate, int channels) {
    this->outPath = (char *) malloc(strlen(outPath) + 1);
    memcpy(this->outPath, outPath, strlen(outPath) + 1);
    this->bitRate = bitRate;
    this->sampleRate = sampleRate;
    this->channles = channels;
}

int AudioEncoder::prepare() {
    // 注册所有编解码器
    av_register_all();

    int result;
    // 获取输出格式上下文
    if ((result = avformat_alloc_output_context2(&pFormatContext, NULL, NULL, outPath)) < 0) {
        LOGE("avformat_alloc_output_context2 error: %s", av_err2str(result));
        return -1;
    }

    // 获取输出格式
    pFormat = pFormatContext->oformat;
    if (!pFormat) {
        LOGE("oformat error");
        return -1;
    }

    // 打开输出文件
    if ((result = avio_open(&pFormatContext->pb, outPath, AVIO_FLAG_READ_WRITE)) < 0) {
        LOGE("avio_open error: %s", av_err2str(result));
        return -1;
    }

    // 创建输出音频流
    pStream = avformat_new_stream(pFormatContext, NULL);
    if (!pStream) {
        LOGE("avformat_new_stream error");
        return -1;
    }

    // 获取编码器
    pCodec = avcodec_find_encoder(pFormat->audio_codec);
    if (!pCodec) {
        LOGE("avcodec_find_encoder error");
        return -1;
    }

    // 创建编码器上下文并配置编码器参数
    pCodecContext = pStream->codec;
    pCodecContext->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;
    pCodecContext->codec_id = pFormat->audio_codec;
    pCodecContext->codec_type = AVMEDIA_TYPE_AUDIO;
    pCodecContext->sample_fmt = AV_SAMPLE_FMT_FLTP;
    pCodecContext->sample_rate = sampleRate;
    pCodecContext->channel_layout = AV_CH_LAYOUT_STEREO;
    pCodecContext->channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    pCodecContext->bit_rate = bitRate;
    if (pFormatContext->oformat->flags & AVFMT_GLOBALHEADER) {
        pFormatContext->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

    // 打开编码器
    if ((result = avcodec_open2(pCodecContext, pCodec, NULL)) != 0) {
        LOGE("avcodec_open2 error: %s", av_err2str(result));
        return -1;
    }

    // 分配并设置重采样参赛
    swrContext = swr_alloc_set_opts(NULL
            , AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_FLTP, sampleRate
            , AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16, sampleRate
            , 0, NULL);
    if (swrContext == NULL) {
        LOGE("swr_alloc_set_opts error");
        return -1;
    }

    //初始化重采样上下文
    if ((result = swr_init(swrContext)) < 0) {
        LOGE("swr_init error ：%s", av_err2str(result));
        return -1;
    }

    return 0;
}

int AudioEncoder::encodeFrame(AVFrame *pFrame) {
    if (avcodec_send_frame(pCodecContext, pFrame) < 0) {
        LOGE("avcodec_send_frame error");
        return -1;
    }
    while (avcodec_receive_packet(pCodecContext, &pPacket) == 0) {
        pPacket.stream_index = pStream->index;
        av_interleaved_write_frame(pFormatContext, &pPacket);
        av_packet_unref(&pPacket);
    }
    return 0;
}

int AudioEncoder::encodeBuffer(const unsigned char *buffer, int len) {
    uint8_t *outs[2];
    outs[0] = new uint8_t[len];
    outs[1] = new uint8_t[len];
    int count = swr_convert(swrContext, (uint8_t **) &outs, len * 4, &buffer, len / 4);
    pFrame->data[0] = outs[0];
    pFrame->data[1] = outs[1];
    if (count >= 0) {
        encodeFrame(pFrame);
    } else {
        char errorMessage[1024] = {0};
        av_strerror(len, errorMessage, sizeof(errorMessage));
        LOGE("error message: %s", errorMessage);
    }
    delete outs[0];
    delete outs[1];
    return 0;
}

int AudioEncoder::start() {
    int result;
    // 分配音频帧空间
    pFrame = av_frame_alloc();
    pFrame->nb_samples = pCodecContext->frame_size;
    pFrame->format = pCodecContext->sample_fmt;
    // 获取一帧数据的大小
    frameSize = av_samples_get_buffer_size(NULL, pCodecContext->channels, pCodecContext->frame_size,
                                           pCodecContext->sample_fmt, 1);
    frameBuffer = (uint8_t *) malloc(frameSize);
    if ((result = avcodec_fill_audio_frame(pFrame, pCodecContext->channels
            , pCodecContext->sample_fmt, (const uint8_t *) frameBuffer, frameSize, 1)) <
        0) {
        LOGE("avcodec_fill_audio_frame error: %s", av_err2str(result));
        return -1;
    }

    // 分配音频包空间
    av_new_packet(&pPacket, frameSize);
    if (&pPacket == NULL) {
        LOGE("av_init_packet error");
        return -1;
    }

    // 写入头文件
    if ((result = avformat_write_header(pFormatContext, NULL)) != 0) {
        LOGE("avformat_write_header error: %s", av_err2str(result));
        return -1;
    }
    return 0;
}

int AudioEncoder::stop() {
    av_write_trailer(pFormatContext);
    swr_close(swrContext);
    avcodec_close(pCodecContext);
    av_free(&pFrame);
    av_free(&pPacket);
    avio_close(pFormatContext->pb);
    avformat_free_context(pFormatContext);
    return 0;
}
