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
  #define LOGI(...)
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
      {"nopNormalC", "()V", reinterpret_cast<void*>(nopNormalC)},
      {"nopFastC", "()V", reinterpret_cast<void*>(nopFastC)},
      {"nopCriticalC", "()V", reinterpret_cast<void*>(nopCriticalC)},
      {"stringFromJni", "()Ljava/lang/String;", reinterpret_cast<void*>(stringFromJni)},
      {"intArrayNopC", "([I)V", reinterpret_cast<void *>(nopIntArrayC)},
      {"intArrayArgIsCopyC", "([I)Z", reinterpret_cast<void *>(arrayArgIsCopyC)},
      {"copyIntArrayC", "([I)[I", reinterpret_cast<void*>(copyIntArrayC)},
      {"sumC", "([I)I", reinterpret_cast<void*>(sumC)},
      {"sortC", "([I)V", reinterpret_cast<void*>(sortC)},
      {"plusOneC", "([I)V", reinterpret_cast<void*>(plusOneC)},
      {"reverseIntArrayC", "([I)V", reinterpret_cast<void*>(reverseIntArrayC)},
      {"rotateRightIntArrayC", "([II)V", reinterpret_cast<void*>(rotateRightC)},
      {"plusOneCNeon", "([I)V", reinterpret_cast<void*>(plusOneCNeon)},
      {"reverseStringC", "(Ljava/lang/String;)Ljava/lang/String;", reinterpret_cast<void*>(reverseStringC)},
  };
  jint rc = env->RegisterNatives(c, methods, sizeof(methods)/sizeof(JNINativeMethod));
  if (rc != JNI_OK) return rc;

  return JNI_VERSION_1_6;
}

void nopNormalC(JNIEnv* env, jclass){}
void nopFastC(JNIEnv* env, jclass){}
void nopCriticalC(){}

jstring stringFromJni(JNIEnv* env, jclass) {
    return env->NewStringUTF("Hello, World!");
}

void nopIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayHandle){
  jsize size = env->GetArrayLength(javaIntArrayHandle);
  jint* body = env->GetIntArrayElements(javaIntArrayHandle, NULL);
  env->ReleaseIntArrayElements(javaIntArrayHandle, body, 0);
}

jboolean arrayArgIsCopyC(JNIEnv* env, jclass, jintArray javaIntArrayHandle){
  jboolean isCopy = false;
  jint* body = env->GetIntArrayElements(javaIntArrayHandle, &isCopy);
  env->ReleaseIntArrayElements(javaIntArrayHandle, body, 0);
  return isCopy;
}

jintArray copyIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayHandle){
  jsize size = env->GetArrayLength(javaIntArrayHandle);
  jint* body = env->GetIntArrayElements(javaIntArrayHandle, NULL);
  jintArray result;
  result = env->NewIntArray(size);
  env->SetIntArrayRegion(result, 0, size, body);
  env->ReleaseIntArrayElements(javaIntArrayHandle, body, 0);
  return result;
}

jint sumC(JNIEnv* env, jclass, jintArray javaIntArrayHandle){
  jsize size = env->GetArrayLength(javaIntArrayHandle);
  jint* javaIntArray = new jint[size];
  env->GetIntArrayRegion(javaIntArrayHandle, jsize{0}, size, javaIntArray);
  jint* body = env->GetIntArrayElements(javaIntArrayHandle, NULL);
  jint sum = 0;
  for(jsize i = 0; i < size; i++) {
    sum += body[i];
  }
  return sum;
}

void sortC(JNIEnv* env, jclass, jintArray javaIntArrayHandle){
  jsize size = env->GetArrayLength(javaIntArrayHandle);
  jint* body = env->GetIntArrayElements(javaIntArrayHandle, NULL);
  std::sort(body, body + size);
  env->ReleaseIntArrayElements(javaIntArrayHandle, body, 0);
}

void rotateRightC(JNIEnv* env, jclass, jintArray javaIntArrayHandle, jint rotateCount){
  jsize size = env->GetArrayLength(javaIntArrayHandle);
  jint* body = env->GetIntArrayElements(javaIntArrayHandle, NULL);

  jsize rotateInBounds = (rotateCount % size);
  if(rotateInBounds == 0){ return; }

  // reverse entire int array
  // [1, 2, 3, 4, 5] -> [5, 4, 3, 2, 1]
  jsize left = 0, right = size - 1;
  jint tmp;
  while(left < right){
    tmp = body[left];
    body[left++] = body[right];
    body[right--] = tmp;
  }

  // reverse both sides with rotateCount as pivot (pivot index is included as part of the right side)
  // rotate right 3: [5, 4, 3, {2} , 1] -> [3, 4, 5, 1, 2]
  left = 0; right = rotateInBounds - 1;
  while(left < right){
    tmp = body[left];
    body[left++] = body[right];
    body[right--] = tmp;
  }
  left = rotateInBounds; right = size - 1;
  while(left < right){
    tmp = body[left];
    body[left++] = body[right];
    body[right--] = tmp;
  }

  env->ReleaseIntArrayElements(javaIntArrayHandle, body, 0);
}

void plusOneC(JNIEnv *env, jclass, jintArray javaIntArrayHandle) {
  jsize size = env->GetArrayLength(javaIntArrayHandle);
  jint *body = env->GetIntArrayElements(javaIntArrayHandle, NULL);
  for (jsize i = 0;
       i < size;
       i++) {
    body[i] += 1;
  }
  env->ReleaseIntArrayElements(javaIntArrayHandle, body, 0);
}

void plusOneCNeon(JNIEnv* env, jclass, jintArray javaIntArrayHandle){
    jsize size = env->GetArrayLength(javaIntArrayHandle);
    jint* body = env->GetIntArrayElements(javaIntArrayHandle, NULL);
    jsize remainingElements = size % 4;
    jsize i = 0;
    for(; i < size - remainingElements; i += 4) {
      int32x4_t neonVector = vld1q_s32(&body[i]);
      neonVector = vaddq_s32(neonVector, vdupq_n_s32(1));
      vst1q_s32(&body[i], neonVector);
    }
    for (; i < size; ++i) {
      body[i] += 1;
    }
    env->ReleaseIntArrayElements(javaIntArrayHandle, body, 0);
}

void reverseIntArrayC(JNIEnv* env, jclass, jintArray javaIntArrayHandle){
  jsize size = env->GetArrayLength(javaIntArrayHandle);
  jint* body = env->GetIntArrayElements(javaIntArrayHandle, NULL);
  jsize left = 0, right = size - 1;
  jint tmp;
  while(left < right){
    tmp = body[left];
    body[left++] = body[right];
    body[right--] = tmp;
  }
  env->ReleaseIntArrayElements(javaIntArrayHandle, body, 0);
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