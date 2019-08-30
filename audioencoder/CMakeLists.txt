cmake_minimum_required(VERSION 3.4.1)

# 需要引入我们头文件,以这个配置的目录为基准
include_directories(src/main/jniLibs/include)


# 添加共享库搜索路径
LINK_DIRECTORIES(${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})


# 指定源文件目录
AUX_SOURCE_DIRECTORY(${CMAKE_SOURCE_DIR}/src/main/cpp SRC_LIST)


add_library(
        # Sets the name of the library.
        native-invoke
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        ${SRC_LIST}
)


# 添加c++11支持
SET(CMAKE_C_COMPILER g++)
add_compile_options(-std=c++11)

# 连接库
target_link_libraries( # Specifies the target library.
        # 链接额外的 ffmpeg 的编译
        native-invoke
        # 编解码(最重要的库)
        avcodec
        # 设备信息
        avdevice
        # 滤镜特效处理库
        avfilter
        # 封装格式处理库
        avformat
        # 工具库(大部分库都需要这个库的支持)
        avutil
        # 音频采样数据格式转换库
        swresample
        # 视频像素数据格式转换
        swscale
        # 链接 android ndk 自带的一些库
        android
        # Links the target library to the log library
        # included in the NDK.
        log)