#pragma once

#include <jni.h>

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved);
void initialize();
extern "C" JNIEXPORT jstring JNICALL stringFromJni(JNIEnv* env, jclass);
extern "C" JNIEXPORT void JNICALL reverseIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL plusOneCNeon(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL plusOneC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL sortC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT jint JNICALL sumC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT jdouble JNICALL clocksToSeconds(JNIEnv*, jclass, jint clocks);
extern "C" JNIEXPORT jint JNICALL getClocks(JNIEnv*, jclass);
extern "C" JNIEXPORT jstring JNICALL reverseStringC(JNIEnv* env, jclass, jstring javaStringPtr);