LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_JAVA_LIBRARIES := raidl
LOCAL_MODULE := raidl
LOCAL_MODULE_TAGS := optional
include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := raidl
LOCAL_MODULE_PATH := $(TARGET_OUT)/bin
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE := raidl
LOCAL_MODULE_TAGS := optional
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)