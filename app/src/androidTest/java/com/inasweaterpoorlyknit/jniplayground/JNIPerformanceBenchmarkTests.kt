package com.inasweaterpoorlyknit.jniplayground

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class JNIPerformanceBenchmarkTests {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val testNumberOfElements = arrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000)
    val rand = Random(123)
    val randomSumNumbers = IntArray(1_000_000) { rand.nextInt(-10,11) }

    @Test
    fun benchmarkStringFromJNI() {
        var s: String = ""
        benchmarkRule.measureRepeated {
            s = JNIFunctions.stringFromJni()
        }
        assertEquals("Hello, World!", s)
    }

    @Test
    fun sumKotlin() {
        var sum = 0
        benchmarkRule.measureRepeated {
            for(i in 0..randomSumNumbers.lastIndex) { sum += randomSumNumbers[i] }
        }
        assertNotEquals(0, sum)
    }

    companion object {
        init {
            System.loadLibrary("jniplayground")
        }
    }
}