#include <jni.h>
#include <string>
#include "noop_types.h"
#include "profiler.cpp"

global_variable u64 estimateCPUFrequency;
global_variable u64 startClocks;

extern "C" JNIEXPORT jstring JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_stringFromJNI(JNIEnv* env, jclass) {
    return env->NewStringUTF("Hello from C++");
}

extern "C" JNIEXPORT void JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_initialize(JNIEnv*, jclass){
  estimateCPUFrequency = EstimateCPUTimerFreq(1000);
}

extern "C" JNIEXPORT void JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_startTime(JNIEnv*, jclass){
  startClocks = ReadCPUTimer();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_endTime(JNIEnv*, jclass){
  u64 endClocks = ReadCPUTimer();
  return endClocks - startClocks;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_clocksToSeconds(JNIEnv*, jclass, jint clocks){
  return (double)clocks / (double)estimateCPUFrequency;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_sumC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* javaIntArray = new jint[size];
  env->GetIntArrayRegion(javaIntArrayPtr, jsize{0}, size, javaIntArray);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, 0);
  jint sum = 0;
  for(jsize i = 0; i < size; i++) {
    sum += body[i];
  }
  return sum;
}

extern "C" JNIEXPORT void JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_sortC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  std::sort(body, body + size);
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_plusOneC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  for(jsize i = 0; i < size; i++){ body[i] += 1; }
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
}