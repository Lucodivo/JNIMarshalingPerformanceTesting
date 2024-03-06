package com.inasweaterpoorlyknit.jniplayground

import dalvik.annotation.optimization.FastNative

class JNIFunctions {
    companion object {
        @JvmStatic
        external fun stringFromJni(): String

        @JvmStatic
        external fun sumC(nums: IntArray): Int
        @JvmStatic
        external fun sortC(nums: IntArray)
        @FastNative
        @JvmStatic
        external fun plusOneC(nums: IntArray)
        @FastNative
        @JvmStatic
        external fun plusOneCNeon(nums: IntArray)
        @JvmStatic
        external fun reverseIntArrayC(nums: IntArray)

        @FastNative
        @JvmStatic
        external fun startTime()
        @FastNative
        @JvmStatic
        external fun initialize()
        @FastNative
        @JvmStatic
        external fun endTime(): Int
        @FastNative
        @JvmStatic
        external fun clocksToSeconds(clocks: Int): Double
    }
}