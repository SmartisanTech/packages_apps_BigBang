LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := csopensdk android-support-v4 okhttp-2.7.5 okio-1.7.0
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := TextBoom
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_USE_FRAMEWORK_SMARTISANOS := true

LOCAL_32_BIT_ONLY := true
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := csopensdk:libs/csopensdk.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += okhttp-2.7.5:libs/okhttp-2.7.5.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += okio-1.7.0:libs/okio-1.7.0.jar

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
