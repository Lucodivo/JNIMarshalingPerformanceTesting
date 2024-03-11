#pragma once

#include <jni.h>

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved);
extern "C" JNIEXPORT jstring JNICALL stringFromJni(JNIEnv* env, jclass);
extern "C" JNIEXPORT void JNICALL nopNormalC(JNIEnv* env, jclass);
extern "C" JNIEXPORT void JNICALL nopFastC(JNIEnv* env, jclass);
extern "C" JNIEXPORT void JNICALL nopCriticalC();
extern "C" JNIEXPORT void JNICALL nopIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT jintArray JNICALL copyIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL reverseIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL plusOneCNeon(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL plusOneC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL sortC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT jint JNICALL sumC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT jstring JNICALL reverseStringC(JNIEnv* env, jclass, jstring javaStringPtr);