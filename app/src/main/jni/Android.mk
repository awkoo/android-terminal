LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := terminal
LOCAL_SRC_FILES := terminal.c
LOCAL_CONLYFLAGS := -std=c23 -Wall -Wextra -Werror -O3 -fno-stack-protector -Wl,--gc-sections
include $(BUILD_SHARED_LIBRARY)
