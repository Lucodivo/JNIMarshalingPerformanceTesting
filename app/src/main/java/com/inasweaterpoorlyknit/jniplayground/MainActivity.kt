package com.inasweaterpoorlyknit.jniplayground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.inasweaterpoorlyknit.jniplayground.databinding.ActivityMainBinding
import kotlin.math.min
import kotlin.random.Random

val DEBUG_LOG_TAG = "jni_playground"
fun logd(msg: String) = Log.d(DEBUG_LOG_TAG, msg)

val MAX_PRINT_SIZE = 65
fun logTimeHeader() {
    val toPrint = CharArray(MAX_PRINT_SIZE){' '}
    "=== Title ===".forEachIndexed { i, c ->
        toPrint[i] = c
    }
    "=== Seconds ===".forEachIndexed { i, c ->
        toPrint[30 + i] = c
    }
    Log.d(DEBUG_LOG_TAG, toPrint.joinToString(""))
}
fun logTime(title: String, seconds: Double) {
    val toPrint = CharArray(MAX_PRINT_SIZE){' '}
    for(i in title.indices){
        if(i == 30) break
        toPrint[i] = title[i]
    }
    seconds.toString().let{
        it.substring(0, min(30, it.length)).forEachIndexed { i, c ->
            toPrint[i + 30] = c
        }
    }
    Log.d(DEBUG_LOG_TAG, toPrint.joinToString(""))
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialize()
        val rand = Random(123)
        logTimeHeader()

        // timer overhead
        val timerOverheadDuration = time{}
        logTime("C Timer overhead", timerOverheadDuration)

        val numbersKotlin = IntArray(100_000) { rand.nextInt() }
        val numbersC = numbersKotlin.copyOf()

        // plus one in-place Kotlin
        val plusOneInPlaceKotlinDuration = time{ for(i in numbersKotlin.indices){ numbersKotlin[i] += 1 }}
        logTime("Plus one in-place Kotlin", plusOneInPlaceKotlinDuration)

        // plus one map Kotlin
        var result: List<Int>
        val plusOneCopyMapKotlinDuration = time{ result = numbersKotlin.map { it + 1 } }
        logTime("Plus one copy (map) Kotlin",  plusOneCopyMapKotlinDuration)

        // plus one C
        val plusOneCDuration = time{ plusOneC(numbersC) }
        logTime("Plus one C",  plusOneCDuration)

        // default sort in Kotlin
        val sortInKotlinDuration = time{ numbersKotlin.sort() }
        logTime("Sorting numbers in Kotlin",  sortInKotlinDuration)

        // default sort in C
        val sortInCDuration = time{ sortC(numbersC) }
        logTime("Sorting numbers in C",  sortInCDuration)

        binding.sampleText.text = numbersC.take(100).printable()
    }

    private fun time(work: () -> Unit): Double { startTime(); work(); return endTime() }

    private fun List<Int>.printable(): String {
        val s = StringBuilder()
        s.append("[")
        for(i in 0..<size-1){
            s.append("${this[i]}, ")
        }
        s.append("${this[size-1]}]")
        return s.toString()
    }

    /**
     * A native method that is implemented by the 'jniplayground' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
    external fun sumC(nums: IntArray): Int
    external fun sortC(nums: IntArray)
    external fun plusOneC(nums: IntArray)
    external fun startTime()
    external fun initialize()
    external fun endTime(): Double

    companion object {
        // Used to load the 'jniplayground' library on application startup.
        init {
            System.loadLibrary("jniplayground")
        }
    }
}