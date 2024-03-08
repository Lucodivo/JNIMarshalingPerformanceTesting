package com.inasweaterpoorlyknit.jniplayground

import dalvik.annotation.optimization.FastNative

class JNIFunctions {
    companion object {
        @FastNative
        @JvmStatic
        external fun stringFromJni(): String
        @FastNative
        @JvmStatic
        external fun nopC(nums: IntArray)
        @FastNative
        @JvmStatic
        external fun copyIntArrayC(nums: IntArray): IntArray

        @FastNative
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
        @FastNative
        @JvmStatic
        external fun reverseIntArrayC(nums: IntArray)
        @FastNative
        @JvmStatic
        external fun reverseStringC(str: String): String

        @FastNative
        @JvmStatic
        external fun getClocks(): Long
        @FastNative
        @JvmStatic
        external fun getMeasuredFrequency(): Long
        @FastNative
        @JvmStatic
        external fun clocksToSeconds(clocks: Long): Double
    }
}