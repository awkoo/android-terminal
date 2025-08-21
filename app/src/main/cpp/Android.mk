LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := termux
LOCAL_SRC_FILES := termux.c
LOCAL_CONLYFLAGS := -std=c11 -Wall -Wextra -Werror -Os -fno-stack-protector -Wl,--gc-sections
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog
LOCAL_MODULE := local-socket
LOCAL_SRC_FILES := local-socket.cpp
include $(BUILD_SHARED_LIBRARY)
