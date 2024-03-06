package com.inasweaterpoorlyknit.jniplayground

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class JNITests {
    fun randomIntArray(count: Int, min: Int? = null, max: Int? = null, seed: Long = 123): IntArray {
        val rand = Random(seed)
        return if(min != null && max != null) IntArray(count) { rand.nextInt(min, max) }
        else IntArray(count) { rand.nextInt() }
    }

    @Test
    fun stringFromJNI() {
        assertEquals("Hello from C++", JNIFunctions.stringFromJni())
    }

    @Test
    fun plusOneC() {
        val numbersC = randomIntArray(10_000, -100_000, 100_000)
        val numbersKotlin = numbersC.copyOf()
        for(i in numbersKotlin.indices) { numbersKotlin[i] += 1 }
        JNIFunctions.plusOneC(numbersC)
        assertArrayEquals(numbersC, numbersKotlin)
    }

    @Test
    fun plusOneCNeon() {
        val numbersC = randomIntArray(10_000, -100_000, 100_000)
        val numbersKotlin = numbersC.copyOf()
        for(i in numbersKotlin.indices) { numbersKotlin[i] += 1 }
        JNIFunctions.plusOneCNeon(numbersC)
        assertArrayEquals(numbersC, numbersKotlin)
    }

    @Test
    fun sortC() {
        val numbersC = randomIntArray(10_000)
        val numbersKotlin = numbersC.copyOf()
        numbersKotlin.sort()
        JNIFunctions.sortC(numbersC)
        assertArrayEquals(numbersC, numbersKotlin)
    }

    @Test
    fun reverseC() {
        val numbersC = randomIntArray(10_000)
        val numbersKotlin = numbersC.copyOf()
        numbersKotlin.reverse()
        JNIFunctions.reverseIntArrayC(numbersC)
        assertArrayEquals(numbersC, numbersKotlin)
    }

    companion object {
        init {
            System.loadLibrary("jniplayground")
        }
    }
}