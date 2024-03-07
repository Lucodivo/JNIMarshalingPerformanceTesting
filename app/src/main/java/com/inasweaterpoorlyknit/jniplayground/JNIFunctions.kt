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
        @JvmStatic
        external fun reverseStringC(str: String): String

        @FastNative
        @JvmStatic
        external fun getClocks(): Int
        @FastNative
        @JvmStatic
        external fun clocksToSeconds(clocks: Int): Double
    }
}