LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := ffmpeg
LOCAL_SRC_FILES := ffmpeg-jni.c helpers.c queue.c player.c convert.cpp
LOCAL_LDLIBS := -llog -ljnigraphics -lz -landroid
LOCAL_SHARED_LIBRARIES := libavformat libavcodec libswscale libavutil libswresample libyuv_static
include $(BUILD_SHARED_LIBRARY)
$(call import-module,libyuv)
$(call import-module,ffmpeg-2.0.1/android/arm)