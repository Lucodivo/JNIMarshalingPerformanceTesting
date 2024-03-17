package com.inasweaterpoorlyknit.jniplayground

import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative
import java.lang.annotation.Native

class JNIFunctions {
    companion object {
        const val NANOSECONDS_PER_MICROSECOND = 1_000
        const val NANOSECONDS_PER_MILLISECOND = 1_000_000
        const val NANOSECONDS_PER_SECOND = 1_000_000_000
        @JvmStatic
        external fun nopNormalC()
        @FastNative
        @JvmStatic
        external fun nopFastC()
        @CriticalNative
        @JvmStatic
        external fun nopCriticalC()
        @FastNative
        @JvmStatic
        external fun stringFromJni(): String

        @FastNative
        @JvmStatic
        external fun intArrayNopC(nums: IntArray)
        @FastNative
        @JvmStatic
        external fun intArrayArgIsCopyC(nums: IntArray): Boolean

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
        external fun rotateRightIntArrayC(nums: IntArray, rotateCount: Int)

        @FastNative
        @JvmStatic
        external fun reverseStringC(str: String): String
    }
}