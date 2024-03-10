#include "native-lib.h"

#include "noop_types.h"

#include <string>

#include <arm_neon.h>

#ifdef __ANDROID__
  #include <android/log.h>
  #define APP_NAME "jni_playground"
  #define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, APP_NAME, __VA_ARGS__))
  #define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, APP_NAME, __VA_ARGS__))
  #define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, APP_NAME, __VA_ARGS__))
  #define logTime(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, APP_NAME, __VA_ARGS__))
#elif
  define LOGI(...)
  #define LOGW(...)
  #define LOGE(...)
  #define logTime(...)
#endif

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
      {"intArrayNopC", "([I)V", reinterpret_cast<void*>(nopC)},
      {"copyIntArrayC", "([I)[I", reinterpret_cast<void*>(copyIntArrayC)},
      {"sumC", "([I)I", reinterpret_cast<void*>(sumC)},
      {"sortC", "([I)V", reinterpret_cast<void*>(sortC)},
      {"plusOneC", "([I)V", reinterpret_cast<void*>(plusOneC)},
      {"reverseIntArrayC", "([I)V", reinterpret_cast<void*>(reverseIntArrayC)},
      {"plusOneCNeon", "([I)V", reinterpret_cast<void*>(plusOneCNeon)},
      {"reverseStringC", "(Ljava/lang/String;)Ljava/lang/String;", reinterpret_cast<void*>(reverseStringC)},
  };
  jint rc = env->RegisterNatives(c, methods, sizeof(methods)/sizeof(JNINativeMethod));
  if (rc != JNI_OK) return rc;

  return JNI_VERSION_1_6;
}

jstring stringFromJni(JNIEnv* env, jclass) {
    return env->NewStringUTF("Hello from C++");
}

void nopC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
}

jintArray copyIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayPtr){
  jsize size = env->GetArrayLength(javaIntArrayPtr);
  jint* body = env->GetIntArrayElements(javaIntArrayPtr, NULL);
  jintArray result;
  result = env->NewIntArray(size);
  env->SetIntArrayRegion(result, 0, size, body);
  env->ReleaseIntArrayElements(javaIntArrayPtr, body, 0);
  return result;
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
  // TODO: Investigate. Neon does not run any faster (about the same).
  //  Is it being misused or is plusOneC being optimized to use it by compiler?
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

jstring reverseStringC(JNIEnv* env, jclass, jstring javaStringPtr){
  jsize size = env->GetStringLength(javaStringPtr);
  jboolean isCopy = false;
  const jchar* body = env->GetStringChars(javaStringPtr, &isCopy);
  jsize left = 0, right = size - 1;
  jint tmp;
  jstring result;
  if(isCopy) { // just edit the copy
    while(left < right){
      tmp = body[left];
      ((jchar*)body)[left++] = body[right];
      ((jchar*)body)[right--] = tmp;
    }
    result = env->NewString(body, size);
  } else {
    jchar* reversedBody = new jchar[size];
    while(left < right){
      tmp = body[left];
      reversedBody[left++] = body[right];
      reversedBody[right--] = tmp;
    }
    if(left == right){ reversedBody[left] = body[right]; }
    result = env->NewString(reversedBody, size);
    delete[] reversedBody;
  }
  env->ReleaseStringChars(javaStringPtr, body);
  return result;
}