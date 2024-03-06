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

extern "C" JNIEXPORT jstring JNICALL stringFromJni(JNIEnv* env, jclass);
extern "C" JNIEXPORT void JNICALL reverseIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL plusOneCNeon(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL plusOneC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT void JNICALL sortC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT jint JNICALL sumC(JNIEnv* env, jclass, jintArray javaIntArrayPtr);
extern "C" JNIEXPORT jdouble JNICALL clocksToSeconds(JNIEnv*, jclass, jint clocks);
extern "C" JNIEXPORT jint JNICALL getClocks(JNIEnv*, jclass);
void initialize();

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }

  // Find your class. JNI_OnLoad is called from the correct class loader context for this to work.
  jclass c = env->FindClass("com/inasweaterpoorlyknit/jniplayground/JNIFunctions");
  if (c == nullptr) return JNI_ERR;

  // Register your class' native methods.
  static const JNINativeMethod methods[] = {
      {"stringFromJni", "()Ljava/lang/String;", reinterpret_cast<void*>(stringFromJni)},
      {"getClocks", "()I", reinterpret_cast<void*>(getClocks)},
      {"clocksToSeconds", "(I)D", reinterpret_cast<void*>(clocksToSeconds)},
      {"sumC", "([I)I", reinterpret_cast<void*>(sumC)},
      {"sortC", "([I)V", reinterpret_cast<void*>(sortC)},
      {"plusOneC", "([I)V", reinterpret_cast<void*>(plusOneC)},
      {"reverseIntArrayC", "([I)V", reinterpret_cast<void*>(reverseIntArrayC)},
      {"plusOneCNeon", "([I)V", reinterpret_cast<void*>(plusOneCNeon)},
  };
  int rc = env->RegisterNatives(c, methods, sizeof(methods)/sizeof(JNINativeMethod));
  if (rc != JNI_OK) return rc;

  initialize();

  return JNI_VERSION_1_6;
}

jint getClocks(JNIEnv*, jclass){ return ReadCPUTimer(); }

jstring stringFromJni(JNIEnv* env, jclass) {
    return env->NewStringUTF("Hello from C++");
}

void initialize(){
  estimateCPUFrequency = EstimateCPUTimerFreq(1000);
}

jdouble clocksToSeconds(JNIEnv*, jclass, jint clocks){
  return (double)clocks / (double)estimateCPUFrequency;
}

jint sumC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
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

void sortC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  std::sort(body, body + size);
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
}

void plusOneC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  for(jsize i = 0; i < size; i++){ body[i] += 1; }
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
}

void reverseIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  jsize left = 0, right = size - 1;
  jint tmp;
  while(left < right){ tmp = body[left]; body[left++] = body[right]; body[right--] = tmp; }
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
}

void plusOneCNeon(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  int32x4_t plusOnex4 = vdupq_n_s32(1);
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

/*
extern "C" JNIEXPORT void JNICALL reverseStringC(JNIEnv* env, jclass, jstring javaStringPtr){
  jsize size = env->GetStringLength(javaStringPtr);
  const jchar* body = env->GetStringChars(javaStringPtr, NULL);
  jsize left = 0, right = size - 1;
  jint tmp;
  while(left < right){ tmp = body[left]; body[left++] = body[right]; body[right--] = tmp; }
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
}*/
