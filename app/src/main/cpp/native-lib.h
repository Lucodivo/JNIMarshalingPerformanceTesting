#pragma once

#include <jni.h>

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved);
void initialize();
extern "C" JNIEXPORT jlong JNICALL getMeasuredFrequency(JNIEnv* env, jclass);
extern "C" JNIEXPORT jstring JNICALL stringFromJni(JNIEnv* env, jclass);
extern "C" JNIEXPORT jintArray JNICALL copyIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL nopC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL reverseIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL plusOneCNeon(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL plusOneC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL sortC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT jint JNICALL sumC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT jdouble JNICALL clocksToSeconds(JNIEnv*, jclass, jlong clocks);
extern "C" JNIEXPORT jlong JNICALL getClocks(JNIEnv*, jclass);
extern "C" JNIEXPORT jstring JNICALL reverseStringC(JNIEnv* env, jclass, jstring javaStringPtr);