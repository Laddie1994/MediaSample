//
// Created by hcDarren on 2019/6/16.
//

#include "audio_decoder.h"
#include "DZConstDefine.h"

AudioDecoder::AudioDecoder(DZJNICall *pJniCall, const char *url) {
    this->pJniCall = pJniCall;
    this->url = static_cast<char *>(malloc(strlen(url) + 1));
    memcpy(this->url, url, strlen(url) + 1);

    audioFrameQueue = new std::queue<AudioPcm *>();
}

AudioDecoder::~AudioDecoder() {
    release();
}

void AudioDecoder::callPlayerJniError(int code, const char *msg) {
    // 释放资源
    release();
    // 回调给 java 层调用
    pJniCall->callPlayerError(code, msg);
}

void AudioDecoder::release() {
    if (pCodecContext != NULL) {
        avcodec_close(pCodecContext);
        avcodec_free_context(&pCodecContext);
        pCodecContext = NULL;
    }

    if (pFormatContext != NULL) {
        avformat_close_input(&pFormatContext);
        avformat_free_context(pFormatContext);
        pFormatContext = NULL;
    }

    if (swrContext != NULL) {
        swr_free(&swrContext);
        free(swrContext);
        swrContext = NULL;
    }

    avformat_network_deinit();

    free(url);
}

void AudioDecoder::prepare() {
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
        callPlayerJniError(formatOpenInputRes, av_err2str(formatOpenInputRes));
        return;
    }

    // 由于avformat_open_input有些参数获取不到
    // 查找资源中的流信息
    int formatFindStreamInfoRes = avformat_find_stream_info(pFormatContext, NULL);
    if (formatFindStreamInfoRes < 0) {
        LOGE("format find stream info error: %s", av_err2str(formatFindStreamInfoRes));
        callPlayerJniError(formatFindStreamInfoRes, av_err2str(formatFindStreamInfoRes));
        return;
    }
    // 查找音频流的 index
    audioStramIndex = av_find_best_stream(pFormatContext, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0);
    if (audioStramIndex < 0) {
        LOGE("format audio stream error: ");
        callPlayerJniError(FIND_STREAM_ERROR_CODE, "format audio stream error");
        return;
    }
    // 获取音频流中的解码参数
    pCodecParameters = pFormatContext->streams[audioStramIndex]->codecpar;
    // 根据获取的音频流信息获取相应的解码器
    pCodec = avcodec_find_decoder(pCodecParameters->codec_id);
    if (pCodec == NULL) {
        LOGE("codec find audio decoder error");
        callPlayerJniError(CODEC_FIND_DECODER_ERROR_CODE, "codec find audio decoder error");
        return;
    }
    // 获取解码器上下文
    pCodecContext = avcodec_alloc_context3(pCodec);
    if (pCodecContext == NULL) {
        LOGE("codec alloc context error");
        callPlayerJniError(CODEC_ALLOC_CONTEXT_ERROR_CODE, "codec alloc context error");
        return;
    }
    // 将音频流参数拷贝到，解码器上下文中
    int codecParametersToContextRes = avcodec_parameters_to_context(pCodecContext, pCodecParameters);
    if (codecParametersToContextRes < 0) {
        LOGE("codec parameters to context error: %s", av_err2str(codecParametersToContextRes));
        callPlayerJniError(codecParametersToContextRes, av_err2str(codecParametersToContextRes));
        return;
    }
    // 打开音频流解码器
    int codecOpenRes = avcodec_open2(pCodecContext, pCodec, NULL);
    if (codecOpenRes != 0) {
        LOGE("codec audio open error: %s", av_err2str(codecOpenRes));
        callPlayerJniError(codecOpenRes, av_err2str(codecOpenRes));
        return;
    }

    // ---------- 重采样 start ----------
    // 输出的采样率
    int out_sample_rate = AUDIO_SAMPLE_RATE;
    // 输出的通道数
    int64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    // 输出的格式
    AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_S16;
    // 输入的通道数
    int64_t in_ch_layout = pCodecContext->channel_layout;
    // 输入的格式
    enum AVSampleFormat in_sample_fmt = pCodecContext->sample_fmt;
    // 输入的采样率
    int in_sample_rate = pCodecContext->sample_rate;
    // 获取重采样上下文
    swrContext = swr_alloc_set_opts(NULL
            , out_ch_layout, out_sample_fmt, out_sample_rate
            , in_ch_layout, in_sample_fmt, in_sample_rate
            , 0, NULL);
    if (swrContext == NULL) {
        // 提示错误
        callPlayerJniError(SWR_ALLOC_SET_OPTS_ERROR_CODE, "swr alloc set opts error");
        return;
    }
    // 初始化重采样
    int swrInitRes = swr_init(swrContext);
    if (swrInitRes < 0) {
        callPlayerJniError(SWR_CONTEXT_INIT_ERROR_CODE, "swr context swr init error");
        return;
    }
    // ---------重采样 end -------------

}

int AudioDecoder::audioFillData(AudioPcm **buffer) {
    int size = audioFrameQueue->size();
    if (size > 0) {
        (*buffer) = audioFrameQueue->front();
        audioFrameQueue->pop();
        return (*buffer)->dataSize;
    }
    return 0;
}

void *threadPlay(void *context) {
    AudioDecoder *pFFmpeg = (AudioDecoder *) context;
    pFFmpeg->decodeAudio();
    return 0;
}

void AudioDecoder::start() {
    // 开启一个线程去解码音频数据
    pthread_create(&playThreadT, NULL, threadPlay, this);
    // 线程分离，防止当前线程阻塞
    pthread_detach(playThreadT);
}

void AudioDecoder::play() {

}


void AudioDecoder::pause() {
}

void AudioDecoder::stop() {

}

void AudioDecoder::decodeAudio() {
    // 获取通道数
    int channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    // 帧大小
    int frame_size = pCodecParameters->frame_size;
    // 格式
    AVSampleFormat sample_fmt = AV_SAMPLE_FMT_S16;
    // 获取一帧数据大小
    int dataSize = av_samples_get_buffer_size(NULL, channels, frame_size, sample_fmt, 0);
    // 开辟用于存储重采样后数据的存储空间
    uint8_t *resampleOutBuffer = (uint8_t *) malloc(dataSize);
    // 分配packet(未解码数据)
    AVPacket *pPacket = av_packet_alloc();
    // 分配frame(以解码数据)
    AVFrame *pFrame = av_frame_alloc();
    // 读取解码数据
    while (av_read_frame(pFormatContext, pPacket) >= 0) {
        // 判断是否是音频数据
        if (pPacket->stream_index == audioStramIndex) {
            // Packet 包，压缩的数据，解码成 pcm 数据
            // 将解码数据放入到解码队列中
            int codecSendPacketRes = avcodec_send_packet(pCodecContext, pPacket);
            if (codecSendPacketRes == 0) {
                // AVPacket -> AVFrame
                // 从解码队列中获取以解码数据
                int codecReceiveFrameRes = avcodec_receive_frame(pCodecContext, pFrame);
                if (codecReceiveFrameRes == 0) {
                    // 调用重采样的方法，对原数据进行重采样，以符合输出标准
                    swr_convert(swrContext, &resampleOutBuffer, pFrame->nb_samples,
                                (const uint8_t **) pFrame->data, pFrame->nb_samples);
                    AudioPcm *audioPcm = static_cast<AudioPcm *>(malloc(sizeof(AudioPcm)));
                    audioPcm->data = malloc(dataSize);
                    memcpy(audioPcm->data, resampleOutBuffer, dataSize);
                    audioPcm->dataSize = dataSize;
                    audioFrameQueue->push(audioPcm);
                }
            }
        }
        // 解引用
        av_packet_unref(pPacket);
        av_frame_unref(pFrame);
    }

    free(resampleOutBuffer);
    // 1. 解引用数据 data ， 2. 销毁 pPacket 结构体内存  3. pPacket = NULL
    av_packet_free(&pPacket);
    av_frame_free(&pFrame);
}
