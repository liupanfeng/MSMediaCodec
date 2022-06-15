
cmake_minimum_required(VERSION 3.10.2)


project("msmediacodec")

file(GLOB ALL_CPP *.cpp)
# ffmpeg的路径
set(FFMPEG ${CMAKE_SOURCE_DIR}/ffmpeg)
# rtmp的路径
set(RTMP ${CMAKE_SOURCE_DIR}/rtmp)
# 导入ffmpeg的头文件
include_directories(${FFMPEG}/include)

#ffmpeg库指定
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${FFMPEG}/libs/${CMAKE_ANDROID_ARCH_ABI}")
#rtmp库指定
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${RTMP}/libs/${CMAKE_ANDROID_ARCH_ABI}")


add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${ALL_CPP})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native-lib

        # 忽略顺序的方式，导入
        -Wl,--start-group
        avcodec avfilter avformat avutil swresample swscale
        -Wl,--end-group
        z   #libz.so 是FFMpeg 额外需要的库文件
        rtmp
        android  #ANativeWindow 用来渲染画面
        OpenSLES  #OpenSLES 用来播放PCM格式的音频
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})