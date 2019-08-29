//
// Created by yc on 19-8-26.
//

#include <jni.h>
#include <sys/param.h>
#include "video_decoder.h"
#include "libavfilter/avfilter.h"

VideoDecoder::VideoDecoder(void *ctx, const char *url) {
    this->ctx = ctx;
    this->url = static_cast<char *>(malloc(strlen(url) + 1));
    memcpy(this->url, url, strlen(url) + 1);
}

void VideoDecoder::registerDecodeCallback(videoProductCallback videoCallback,
                                          audioProductCallback audioCallback) {
    this->productVideoFrameCallback = videoCallback;
    this->productAudioFrameCallback = audioCallback;
}

void VideoDecoder::initialize() {
    int result = initFFmpagContext();
    if (result < 0) {
        LOGE("initFFmpagContext fail");
        return;
    }
    result = openVideoStream();
    if (result < 0) {
        LOGE("openVideoStream fail");
        return;
    }
    result = openAudioStream();
    if (result < 0) {
        LOGE("openAudioStream fail");
        return;
    }
}

int VideoDecoder::initFFmpagContext() {
    // 注册复合器和编解码器
    av_register_all();
    // 由于传过来的可能是网络资源，所以这里对网络初始化
    avformat_network_init();
    //打开资源文件
    //参数一：资源上下文（如果打开成功）
    //参数二：资源地址
    //参数三：资源格式，如果传入了就不会在打开资源时再去检测资源格式
    //参数四：对某种格式的一些操作，是为了在命令行中可以对不同的格式传入特殊的操作参数而建的，一般传NULL
    int formatOpenInputRes = avformat_open_input(&pFormatContext, url, NULL, NULL);
    if (formatOpenInputRes != 0) {
        LOGE("format open input error: %s", av_err2str(formatOpenInputRes));
        return -1;
    }
    // 由于avformat_open_input有些参数获取不到
    // 查找资源中的流信息
    int formatFindStreamInfoRes = avformat_find_stream_info(pFormatContext, NULL);
    if (formatFindStreamInfoRes < 0) {
        LOGE("format find stream info error: %s", av_err2str(formatFindStreamInfoRes));
        return -1;
    }
    return 0;
}

int VideoDecoder::openVideoStream() {
    // 查找音频流的 index
    videoStramIndex = av_find_best_stream(pFormatContext, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
    if (videoStramIndex < 0) {
        LOGE("format audio stream error: ");
        return -1;
    }
    // 获取视频流
    AVStream *videoStream = pFormatContext->streams[videoStramIndex];
    // 获取音频流中的解码参数
    videoCodecParameters = videoStream->codecpar;
    // 根据获取的音频流信息获取相应的解码器
    videoCodec = avcodec_find_decoder(videoCodecParameters->codec_id);
    if (videoCodec == NULL) {
        LOGE("codec find audio decoder error");
        return -1;
    }
    // 获取解码器上下文
    videoCodecContext = avcodec_alloc_context3(videoCodec);
    if (videoCodecContext == NULL) {
        LOGE("codec alloc context error");
        return -1;
    }

    int width = videoCodecContext->width;

    // 将音频流参数拷贝到，解码器上下文中
    int codecParametersToContextRes = avcodec_parameters_to_context(videoCodecContext,
                                                                    videoCodecParameters);
    if (codecParametersToContextRes < 0) {
        LOGE("codec parameters to context error: %s", av_err2str(codecParametersToContextRes));
        return -1;
    }
    // 打开音频流解码器
    int codecOpenRes = avcodec_open2(videoCodecContext, videoCodec, NULL);
    if (codecOpenRes != 0) {
        LOGE("codec audio open error: %s", av_err2str(codecOpenRes));
        return -1;
    }

    // 获取base time
    if (videoStream->time_base.den && videoStream->time_base.num) {
        videoTimeBase = av_q2d(videoStream->time_base);
    } else if (videoCodecContext->time_base.den && videoCodecContext->time_base.num) {
        videoTimeBase = av_q2d(videoCodecContext->time_base);
    } else {
        videoTimeBase = 0.25;
    }

    // 获取fps
    if (videoStream->avg_frame_rate.num && videoStream->avg_frame_rate.den) {
        videoFps = av_q2d(videoStream->avg_frame_rate);
    } else if (videoStream->r_frame_rate.den && videoStream->r_frame_rate.num) {
        videoFps = av_q2d(videoStream->r_frame_rate);
    } else {
        videoFps = 1.0 / videoTimeBase;
    }

//    LOGE("timeBase->%lf; fps->%lf", videoTimeBase, videoFps);

    // ---------- 重采样 start ----------
    int in_width = videoCodecContext->width;
    int in_height = videoCodecContext->height;
    AVPixelFormat in_format = videoCodecContext->pix_fmt;
    int out_width = videoCodecContext->width;
    int out_height = videoCodecContext->height;
    AVPixelFormat out_format = AV_PIX_FMT_YUV420P;
    // 获取重采样上下文
    swsContext = sws_getContext(in_width, in_height, in_format, out_width, out_height, out_format,
                                SWS_BILINEAR, NULL, NULL, NULL);
    if (swsContext == NULL) {
        // 提示错误
        return -1;
    }
    // ---------重采样 end -------------
    return 0;
}

int VideoDecoder::openAudioStream() {
    // 查找音频流的 index
    audioStreamIndex = av_find_best_stream(pFormatContext, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0);
    if (audioStreamIndex < 0) {
        LOGE("format audio stream error: ");
        return -1;
    }
    // 获取音频流
    audioStream = pFormatContext->streams[audioStreamIndex];
    // 获取音频流中的解码参数
    audioCodecParameters = audioStream->codecpar;
    // 根据获取的音频流信息获取相应的解码器
    audioCodec = avcodec_find_decoder(audioCodecParameters->codec_id);
    if (audioCodec == NULL) {
        LOGE("codec find audio decoder error");
        return -1;
    }
    // 获取解码器上下文
    audioCodecContext = avcodec_alloc_context3(audioCodec);
    if (audioCodecContext == NULL) {
        LOGE("codec alloc context error");
        return -1;
    }
    // 将音频流参数拷贝到，解码器上下文中
    int codecParametersToContextRes = avcodec_parameters_to_context(audioCodecContext,
                                                                    audioCodecParameters);
    if (codecParametersToContextRes < 0) {
        LOGE("codec parameters to context error: %s", av_err2str(codecParametersToContextRes));
        return -1;
    }
    // 打开音频流解码器
    int codecOpenRes = avcodec_open2(audioCodecContext, audioCodec, NULL);
    if (codecOpenRes != 0) {
        LOGE("codec audio open error: %s", av_err2str(codecOpenRes));
        return -1;
    }

    // 获取base time
    if (audioStream->time_base.den && audioStream->time_base.num) {
        audioTimeBase = av_q2d(audioStream->time_base);
    } else if (videoCodecContext->time_base.den && audioCodecContext->time_base.num) {
        audioTimeBase = av_q2d(audioCodecContext->time_base);
    } else {
        audioTimeBase = 0.25;
    }

    // 获取fps
    if (audioStream->avg_frame_rate.num && audioStream->avg_frame_rate.den) {
        audioFps = av_q2d(audioStream->avg_frame_rate);
    } else if (audioStream->r_frame_rate.den && audioStream->r_frame_rate.num) {
        audioFps = av_q2d(audioStream->r_frame_rate);
    } else {
        audioFps = 1.0 / audioTimeBase;
    }

    // ---------- 重采样 start ----------
    // 输出的采样率
    int out_sample_rate = AUDIO_SAMPLE_RATE;
    // 输出的通道数
    int64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    // 输出的格式
    AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_S16;
    // 输入的通道数
    int64_t in_ch_layout = audioCodecContext->channel_layout;
    // 输入的格式
    enum AVSampleFormat in_sample_fmt = audioCodecContext->sample_fmt;
    // 输入的采样率
    int in_sample_rate = audioCodecContext->sample_rate;
    // 获取重采样上下文
    swrContext = swr_alloc_set_opts(NULL, out_ch_layout, out_sample_fmt, out_sample_rate,
                                    in_ch_layout, in_sample_fmt, in_sample_rate, 0, NULL);
    if (swrContext == NULL) {
        // 提示错误
        LOGE("swr alloc set opts error");
        return -1;
    }
    // 初始化重采样
    int swrInitRes = swr_init(swrContext);
    if (swrInitRes < 0) {
        LOGE("swr context swr init error");
        return -1;
    }
    // ---------重采样 end -------------
    return 0;
}

void VideoDecoder::start() {
    pthread_create(&decodeVideoThread, NULL, threadStartDecoderVideo, this);
    pthread_detach(decodeVideoThread);
}

void *VideoDecoder::threadStartDecoderVideo(void *ctx) {
    VideoDecoder *decoder = (VideoDecoder *) ctx;
    decoder->decodeVideo();
    return 0;
}

void VideoDecoder::decodeVideo() {
    isDecodeVideoEOF = false;
    // 获取通道数
    int channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    // 帧大小
    int frame_size = audioCodecParameters->frame_size;
    // 格式
    AVSampleFormat sample_fmt = AV_SAMPLE_FMT_S16;
    // 获取一帧数据大小
    audioFrameSize = av_samples_get_buffer_size(NULL, channels, frame_size, sample_fmt, 0);
    // 开辟用于存储重采样后数据的存储空间
    audioResampleBuffer = (uint8_t *) malloc(audioFrameSize);
    // 分配packet(未解码数据)
    AVPacket *pPacket = av_packet_alloc();
    // 分配frame(以解码数据)
    AVFrame *pFrame = av_frame_alloc();
    // 读取解码数据
    while (av_read_frame(pFormatContext, pPacket) >= 0) {
        // 判断是否是音频数据
        if (pPacket->stream_index == videoStramIndex) {
            // Packet 包，压缩的数据，解码成 pcm 数据
            // 将解码数据放入到解码队列中
            int codecSendPacketRes = avcodec_send_packet(videoCodecContext, pPacket);
            if (codecSendPacketRes == 0) {
                // AVPacket -> AVFrame
                // 从解码队列中获取以解码数据
                int codecReceiveFrameRes = avcodec_receive_frame(videoCodecContext, pFrame);
                if (codecReceiveFrameRes == 0) {
                    VideoFrame *yuvFrame = handleVideoFrame(pFrame);
                    if (yuvFrame) {
                        productVideoFrameCallback(&yuvFrame, this->ctx);
                    }
                }
            }
        } else if (pPacket->stream_index == audioStreamIndex) {
            int result = avcodec_send_packet(audioCodecContext, pPacket);
            if (result == 0) {
                result = avcodec_receive_frame(audioCodecContext, pFrame);
                if (result == 0) {
                    AudioFrame *audioFrame = handleAudioFrame(pFrame);
                    if (audioFrame) {
                        productAudioFrameCallback(&audioFrame, this->ctx);
                    }
                }
            }
        }
        // 解引用
        av_packet_unref(pPacket);
        av_frame_unref(pFrame);
    }
    isDecodeVideoEOF = true;
    LOGE("视频帧播放完毕");
    audioFrameSize = 0;
    free(audioResampleBuffer);
    // 1. 解引用数据 data ， 2. 销毁 pPacket 结构体内存  3. pPacket = NULL
    av_packet_free(&pPacket);
    av_frame_free(&pFrame);
}

VideoFrame *VideoDecoder::handleVideoFrame(AVFrame *pFrame) {
    if (!pFrame->data[0]) {
        LOGE("videoFrame->data[0] is 0... why...");
        return NULL;
    }
    VideoFrame *yuvFrame = new VideoFrame();
    int width = MIN(videoCodecContext->width, pFrame->linesize[0]);
    int height = videoCodecContext->height;
    int lumaLenght = width * height;
    uint8_t *luma = new uint8_t[lumaLenght];
    copyFrameData(luma, pFrame->data[0], width, height, pFrame->linesize[0]);
    yuvFrame->luma = luma;

    width = MIN(pFrame->linesize[1], videoCodecContext->width / 2);
    height = videoCodecContext->height / 2;
    int chromaBLength = width * height;
    uint8_t *chromaB = new uint8_t[chromaBLength];
    copyFrameData(chromaB, pFrame->data[1], width, height, pFrame->linesize[1]);
    yuvFrame->chromaB = chromaB;

    width = MIN(pFrame->linesize[2], videoCodecContext->width / 2);
    height = videoCodecContext->height / 2;
    int chromaRLength = width * height;
    uint8_t *chromaR = new uint8_t[chromaRLength];
    copyFrameData(chromaR, pFrame->data[2], width, height, pFrame->linesize[2]);
    yuvFrame->chromaR = chromaR;

    yuvFrame->position = av_frame_get_best_effort_timestamp(pFrame) * videoTimeBase;

    int64_t duration = av_frame_get_pkt_duration(pFrame);
    if (duration) {
        yuvFrame->duration = duration * videoTimeBase;
        yuvFrame->duration += pFrame->repeat_pict * videoTimeBase * 0.5;
    } else {
        yuvFrame->duration = 1.0 / videoFps;
    }

    yuvFrame->width = pFrame->width;
    yuvFrame->height = pFrame->height;
    return yuvFrame;
}

void VideoDecoder::copyFrameData(uint8_t *dst, uint8_t *src, int width, int height, int linesize) {
    for (int i = 0; i < height; ++i) {
        memcpy(dst, src, width);
        dst += width;
        src += linesize;
    }
}

AudioFrame *VideoDecoder::handleAudioFrame(AVFrame *pFrame) {
    swr_convert(swrContext, &audioResampleBuffer, pFrame->nb_samples,
                (const uint8_t **) pFrame->data, pFrame->nb_samples);
    AudioFrame *audioFrame = new AudioFrame();
    audioFrame->samples = malloc(audioFrameSize);
    memcpy(audioFrame->samples, audioResampleBuffer, audioFrameSize);
    audioFrame->size = audioFrameSize;
    audioFrame->position = av_frame_get_best_effort_timestamp(pFrame) * audioTimeBase;
    int64_t duration = av_frame_get_pkt_duration(pFrame) * audioTimeBase;
    if (duration) {
        duration = audioFrame->size / (sizeof(float) * audioCodecContext->channels * 2 *
                                       audioCodecContext->sample_rate);
    }
    audioFrame->duration = duration;
    return audioFrame;
}

void VideoDecoder::release() {

}

int VideoDecoder::getVideoWidth() {
    return videoCodecContext->width;
}

int VideoDecoder::getVideoHeight() {
    return videoCodecContext->height;
}

VideoDecoder::~VideoDecoder() {

}

void VideoDecoder::stop() {

}
