#include <jni.h>
#include <android/log.h> // Android logging
#include <arm_neon.h>
#include <string>
#include "noop_types.h"
#include "profiler.cpp"

#define APP_NAME "jni_playground"
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, APP_NAME, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, APP_NAME, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, APP_NAME, __VA_ARGS__))
#define logTime(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, APP_NAME, __VA_ARGS__))

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
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
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

extern "C" JNIEXPORT void JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_reverseC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  jsize left = 0, right = size - 1;
  jint tmp;
  while(left < right){ tmp = body[left]; body[left++] = body[right]; body[right--] = tmp; }
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_inasweaterpoorlyknit_jniplayground_MainActivity_plusOneCNeon(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  int32x4_t plusOnex4 = vdupq_n_s32(1);
  int32x4_t sum = vdupq_n_s32(0);
  for(size_t i = 0; i < (size / 4); i++) {
    int32_t* ptr = body + (i*4);
    int32x4_t arrayVals = vld1q_s32(ptr);
    vst1q_s32(ptr, vaddq_s32(arrayVals, plusOnex4));
  }
  // plus one the remainder
  if(size & 3) {
    for(jsize i = size - (size & 3); i < size; i++) { body[i] += 1; }
  }
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
}
