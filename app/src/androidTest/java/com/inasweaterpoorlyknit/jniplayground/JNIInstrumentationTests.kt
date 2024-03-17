package com.inasweaterpoorlyknit.jniplayground

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class JNIInstrumentationTests {
    fun randomIntArray(count: Int, min: Int? = null, max: Int? = null, seed: Long = 123): IntArray {
        val rand = Random(seed)
        return if(min != null && max != null) IntArray(count) { rand.nextInt(min, max) }
        else IntArray(count) { rand.nextInt() }
    }

    fun randomASCIIString(count: Int, seed: Long = 123): String {
        val bytes = Random(seed).nextBytes(count)
        for (i in bytes.indices) { bytes[i] = abs(bytes[i].toInt()).toByte() }
        return String(bytes, Charsets.US_ASCII)
    }

    @Test
    fun stringFromJNI() {
        assertEquals("Hello, World!", JNIFunctions.stringFromJni())
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

    @Test
    fun rotateC() {
        val arraySize = 10_000
        val numbers = randomIntArray(arraySize)
        val numbersC = numbers.copyOf()
        val rotateCount = arraySize / 3
        val rotateExpected = IntArray(arraySize){
            if(it < rotateCount){
                numbers[arraySize - rotateCount + it]
            } else {
                numbers[it - rotateCount]
            }
        }

        JNIFunctions.rotateRightIntArrayC(numbersC, rotateCount)

        assertArrayEquals(rotateExpected, numbersC)
    }

    @Test
    fun rotateC_equalToKotlinMirror() {
        val arraySize = 10_000
        val numbers = randomIntArray(arraySize)
        val numbersC = numbers.copyOf()
        val rotateCount = arraySize / 3

        JNIFunctions.rotateRightIntArrayC(numbersC, rotateCount)

        assertArrayEquals(numbers.apply { rotateRight(rotateCount) }, numbersC)
    }

    @Test
    fun reverseStringC_smallToTriggerCopy(){
        val aStr = randomASCIIString(50)
        val expectedReverseStr = aStr.reversed()
        val actualReverseStr = JNIFunctions.reverseStringC(aStr)
        assertEquals(expectedReverseStr, actualReverseStr)
    }

    @Test
    fun reverseStringC_bigToNotTriggerCopy(){
        val aStr = randomASCIIString(10_000)
        val expectedReverseStr = aStr.reversed()
        val actualReverseStr = JNIFunctions.reverseStringC(aStr)
        assertEquals(expectedReverseStr, actualReverseStr)
    }

    companion object {
        init {
            System.loadLibrary("jniplayground")
        }
    }
}