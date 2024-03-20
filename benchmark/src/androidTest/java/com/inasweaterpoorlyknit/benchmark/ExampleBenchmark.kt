package com.inasweaterpoorlyknit.benchmark

import android.util.Log
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun log() {
        benchmarkRule.measureRepeated {
            Log.d("LogBenchmark", "the cost of writing this log method will be measured")
        }
    }

    val testNumberOfElements = arrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000)
    val rand = Random(123)
    val randomSumNumbers = IntArray(1_000_000) { rand.nextInt(-10,11) }

    @Test
    fun sumKotlin() {
        var sum = 0
        benchmarkRule.measureRepeated {
            for(i in 0..randomSumNumbers.lastIndex) { sum += randomSumNumbers[i] }
        }
        Assert.assertNotEquals(0, sum)
    }

/*
    @Test
    fun benchmarkStringFromJNI() {
        var s: String = ""
        benchmarkRule.measureRepeated {
            s = JNIFunctions.stringFromJni()
        }
        Assert.assertEquals("Hello, World!", s)
    }

    companion object {
        init {
            System.loadLibrary("jniplayground")
        }
    }
*/
}