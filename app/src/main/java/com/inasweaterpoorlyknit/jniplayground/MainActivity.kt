package com.inasweaterpoorlyknit.jniplayground

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.NANOSECONDS_PER_SECOND
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.copyIntArrayC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.endNanosecondTimer
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.getNanosecondTimerDuration
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.intArrayNopC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.plusOneC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.plusOneCNeon
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.reverseIntArrayC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.reverseStringC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.sortC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.startNanosecondTimer
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.stringFromJni
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.sumC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

val DEBUG_LOG_TAG = "jni_playground"
fun logd(msg: String) = Log.d(DEBUG_LOG_TAG, msg)
fun loge(msg: String) = Log.e(DEBUG_LOG_TAG, msg)
fun logi(msg: String) = Log.i(DEBUG_LOG_TAG, msg)

fun List<String>.concatenated(separator: String = " "): String {
    val concatStr = StringBuilder()
    for(i in 0..<lastIndex) concatStr.append(this[0]).append(separator)
    if(isNotEmpty()) concatStr.append(this[lastIndex])
    return concatStr.toString()
}

fun Set<String>.concatenated(separator: String = " "): String {
    val concatStr = StringBuilder()
    val iter = iterator()
    var concatCount = 0
    while(concatCount < size){
        concatStr.append("${iter.next()} ")
        concatCount++
    }
    if(iter.hasNext()) concatStr.append(iter.next())
    return concatStr.toString()
}

fun clocksToString(clocks: Long): String  {
    val seconds = clocks.toDouble() / NANOSECONDS_PER_SECOND.toDouble()
    return when {
        seconds < 10.0e-6 -> { "${"%.2f".format(seconds*1_000_000_000)}ns" }
        seconds < 1.0e-3 -> { "${"%.2f".format(seconds*1_000_000)}µs" }
        seconds < 1.0 -> { "${"%.2f".format(seconds*1_000)}ms" }
        else -> { "${"%.2f".format(seconds)}s" }
    }
}

fun frequencyToHz(frequency: Long): String  {
    return when(frequency) {
        in 0..<1_000 -> return "${frequency}Hz"
        in 1_000..<1_000_000 -> return "${"%.2f".format(frequency / 1_000.0)}KHz"
        in 1_000_000..<1_000_000_000 -> return "${"%.2f".format(frequency / 1_000_000.0)}MHz"
        in 1_000_000_000..<Long.MAX_VALUE -> return "${"%.2f".format(frequency / 1_000_000_000.0)}GHz"
        else -> "Invalid Negative Frequency"
    }
}

class MainActivity : AppCompatActivity() {
    companion object {
        private const val MAX_PRINT_SIZE = 120
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performanceTests()
    }

    data class TimedWork(
        val minNanoseconds: Long,
        val maxNanoseconds: Long,
        val totalIterations: Int,
    ){
        companion object {
            fun logTimeHeader() {
                val toPrint = CharArray(MAX_PRINT_SIZE){' '}
                "=== Title ===".forEachIndexed { i, c ->
                    toPrint[i] = c
                }
                "=== Seconds ===".forEachIndexed { i, c ->
                    toPrint[40 + i] = c
                }
                Log.d(DEBUG_LOG_TAG, toPrint.joinToString(""))
            }
        }
        fun log(title: String) {
            fun logTwoColumn(title: String, msg: String) {
                val toPrint = CharArray(MAX_PRINT_SIZE){' '}
                for(i in title.indices){
                    if(i == 55) break
                    toPrint[i] = title[i]
                }
                msg.let{
                    it.substring(0, min(55, it.length)).forEachIndexed { i, c ->
                        toPrint[i + 55] = c
                    }
                }
                Log.d(DEBUG_LOG_TAG, toPrint.joinToString(""))
            }

            logTwoColumn("$title (min)", clocksToString(minNanoseconds))
            logTwoColumn("$title (max)", clocksToString(maxNanoseconds))
            logTwoColumn("$title (iterations)", "$totalIterations")
        }
    }


    data class CPUInfo(
        val model: String,
        val manufacturer: String,
        val supportedABIs: List<String>,
        val processors: List<SoCInfoProcessor>,
    ) {
        val processorFeatures = processors.flatMap{ it.features.toSet() }.fold(setOf<String>()){ acc, it -> acc + it }
        val processorCount = processors.count()
        fun log() {
            logd("=== CPU Information ===")
            logd("Name: ${model.ifEmpty { "unknown" }}")
            logd("Manufacturer: ${manufacturer.ifEmpty { "unknown" }}")
            logd("Supported ABIs: ${supportedABIs.concatenated()}")
            logd("Processor count: ${processors.size}")
            logd("Processor features: ${processorFeatures.concatenated()}")
        }
    }

    data class SoCInfoProcessor(
        val index: Int,
        val features: List<String>,
    )

    fun String.hasPrefix(prefix: String): Boolean {
        if(length < prefix.length) return false
        for(i in 0..prefix.lastIndex) {
            if(this[i] != prefix[i]) return false
        }
        return true
    }

    fun fetchCpuInfo(): CPUInfo {
        val cpuInfoFile = File("/proc/cpuinfo")
        if(!cpuInfoFile.isFile) {
            loge( "Could not open /proc/cpuinfo")
            return CPUInfo("", "", emptyList(), emptyList())
        }
        val cpuProcessors = ArrayList<SoCInfoProcessor>()
        var name = ""; var manufacturer = "";
        val fileLines = cpuInfoFile.readLines()
        fileLines.forEach { logi(it) }
        var lineIter = 0
        while(lineIter < fileLines.size) {
            var line = fileLines[lineIter++]
            when {
                line.hasPrefix("processor") -> {
                    val index = Regex("(0-9)+").find(line)?.value?.toInt() ?: -1
                    var model = ""; val features = ArrayList<String>();
                    line = fileLines[lineIter++]
                    while(line.isNotBlank()) {
                        when {
                            line.hasPrefix("model name") -> model = Regex("(?<=:\\s).+").find(line)?.value ?: ""
                            line.hasPrefix("Features") -> features.addAll(
                                Regex("(?<=:\\s).+").find(line)?.value?.split(regex = Regex("\\s+"))?.filter{ it.isNotEmpty() } ?: emptyList()
                            )
                        }
                        line = fileLines[lineIter++]
                    }
                    cpuProcessors.add(SoCInfoProcessor(index, features))
                }
                line.hasPrefix("Hardware") -> {
                    name = Regex("(?<=:\\s).+").find(line)?.value ?: ""
                }
            }
        }
        if(name.isEmpty() && Build.VERSION.SDK_INT >= 31) { // if name wasn't found in CPU Info, try Android.os.Bulid
            name = Build.SOC_MODEL
            manufacturer = Build.SOC_MANUFACTURER
        }
        return CPUInfo(name, manufacturer, Build.SUPPORTED_ABIS.toList(), cpuProcessors)
    }

    fun fetchRamInfo() {
        val memInfoFile = File("/proc/meminfo")
        if(!memInfoFile.isFile) {
            loge( "Could not open /proc/memInfo")
            return
        }
        memInfoFile.readLines().forEach { logi(it) }
    }

    private fun randomASCIIString(count: Int, seed: Long = 123): String {
        val bytes = Random(seed).nextBytes(count)
        for (i in bytes.indices) { bytes[i] = abs(bytes[i].toInt()).toByte() }
        return String(bytes, Charsets.US_ASCII)
    }

    private fun performanceTests() {
        lifecycleScope.launch(Dispatchers.Default) {
            val rand = Random(123)
            Log.i(DEBUG_LOG_TAG, "Supported ABIs:" + Build.SUPPORTED_ABIS.fold(StringBuilder()){ acc, str -> acc.append(" $str") }).toString()
            Log.i(DEBUG_LOG_TAG,"Model: " + Build.MODEL)
            Log.i(DEBUG_LOG_TAG,"Manufacturer: " + Build.MANUFACTURER)
            Log.i(DEBUG_LOG_TAG,"Brand: " + Build.BRAND)
            Log.i(DEBUG_LOG_TAG,"SDK: " + Build.VERSION.SDK_INT.toString())
            Log.i(DEBUG_LOG_TAG,"Board: " + Build.BOARD)
            Log.i(DEBUG_LOG_TAG,"Product: " + Build.PRODUCT)
            Log.i(DEBUG_LOG_TAG,"Device: " + Build.DEVICE)
            if(Build.VERSION.SDK_INT >= 31){
                Log.i(DEBUG_LOG_TAG,"SoC Manufacturer: " + Build.SOC_MANUFACTURER)
                Log.i(DEBUG_LOG_TAG,"SoC Model: " + Build.SOC_MODEL)
            }
            val cpuInfo = fetchCpuInfo()
            fetchRamInfo()
            cpuInfo.log()

            TimedWork.logTimeHeader()

            // 468 - 2760
            val timerOverhead = iterationTiming{
                val start = System.nanoTime()
                val end = System.nanoTime()
                end - start
            }.apply{ log("C Timer Overhead") }

            val timedWork = HashMap<String, ArrayList<Pair<Int, TimedWork>>>()
            val testNumberOfElements = arrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000)
            val testNumberOfElements_HighMemoryWork = arrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000)
            var sum = 0;
            fun addTimedWork(tag: String, dataSize: Int, work: TimedWork) {
                if(!timedWork.containsKey(tag)) { timedWork[tag] = ArrayList() }
                timedWork.getOrPut(tag){ ArrayList() }.add(Pair(dataSize, work))
            }

            val stringFromCTag = "string from C"
            val stringFromCTimedWork = iterationTiming{
                val start = System.nanoTime()
                stringFromJni()
                val end = System.nanoTime()
                end - start
            }.apply { log(stringFromCTag) }
            addTimedWork(stringFromCTag, 0, stringFromCTimedWork)

            testNumberOfElements_HighMemoryWork.forEach { numElements ->
                val randomNumbers = IntArray(numElements) { rand.nextInt() }
                val numbersCopy: () -> IntArray = { randomNumbers.copyOf() }
                val numElementsString = "[${"%,d".format(numElements)}]"
                val randomString = randomASCIIString(numElements)

                // timer overhead
                iterationTiming{
                    val start = System.nanoTime()
                    val end = System.nanoTime()
                    end - start
                }.apply{ log("C Timer Overhead") }

                val copyIntArrayCTag = "copy Int Array C"
                val copyIntArrayCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    copyIntArrayC(randomNumbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$copyIntArrayCTag $numElementsString") }
                addTimedWork(copyIntArrayCTag, numElements, copyIntArrayCTimedWork)

                val copyIntArrayKotlinTag = "copy Int Array Kotlin"
                val copyIntArrayKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    randomNumbers.copyOf()
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$copyIntArrayKotlinTag $numElementsString") }
                addTimedWork(copyIntArrayKotlinTag, numElements, copyIntArrayKotlinTimedWork)

                // plus one map Kotlin
                val plusOneKotlinMapTag = "+1 copy (map) Kotlin"
                val plusOneKotlinMapTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    randomNumbers.map { it + 1 }
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$plusOneKotlinMapTag $numElementsString") }
                addTimedWork(plusOneKotlinMapTag, numElements, plusOneKotlinMapTimedWork)

                // default sort in Kotlin
                val sortKotlinTag = "Sorting in Kotlin"
                val sortKotlinTimedWork = iterationTiming{
                    val numbers = numbersCopy()
                    val start = System.nanoTime()
                    numbers.sort()
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$sortKotlinTag $numElementsString") }
                addTimedWork(sortKotlinTag, numElements, sortKotlinTimedWork)

                // default sort in C
                val sortCTag = "Sorting in C"
                val sortCTimedWork = iterationTiming{
                    val numbers = numbersCopy()
                    val start = System.nanoTime()
                    sortC(numbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$sortCTag $numElementsString") }
                addTimedWork(sortCTag, numElements, sortCTimedWork)

                // reverse array in C
                val reverseCTag = "Reverse in C"
                val reverseCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    reverseIntArrayC(randomNumbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseCTag $numElementsString") }
                addTimedWork(reverseCTag, numElements, reverseCTimedWork)

                // reverse string in Kotlin
                val reverseStringKotlinTag = "Reverse string in Kotlin"
                var reversedKotlinString: String
                val reverseStringKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    reversedKotlinString = randomString.reversed()
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseStringKotlinTag $numElementsString") }
                addTimedWork(reverseStringKotlinTag, numElements, reverseStringKotlinTimedWork)

                // reverse string in C
                val reverseStringCTag = "Reverse string in C"
                var reversedCString: String
                val reverseStringCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    reversedCString = reverseStringC(randomString)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseStringCTag $numElementsString") }
                addTimedWork(reverseStringCTag, numElements, reverseStringCTimedWork)
            }

            testNumberOfElements.forEach { numElements ->
                val randomNumbers = IntArray(numElements) { rand.nextInt() }
                val numbersCopy: () -> IntArray = { randomNumbers.copyOf() }
                val sumNumbers = IntArray(numElements) { rand.nextInt(-10, 11) }
                val numElementsString = "[${"%,d".format(numElements)}]"
                val totalSum: Long = 0

                val nopCTag = "int array nop C"
                val nopCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    intArrayNopC(randomNumbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$nopCTag $numElementsString") }
                addTimedWork(nopCTag, numElements, nopCTimedWork)

                // plus one C
                val numbersPlusOneCopy = numbersCopy()
                val plusOneCTag = "+1 C"
                val plusOneCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    plusOneC(randomNumbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$plusOneCTag $numElementsString") }
                addTimedWork(plusOneCTag, numElements, plusOneCTimedWork)

                // plus one C Neon
                val plusOneCNeonTag = "+1 C Neon"
                for(i in 0..numbersPlusOneCopy.lastIndex){numbersPlusOneCopy[i] = randomNumbers[i]}
                val plusOneCNeonTimedWork = iterationTiming {
                    val start = System.nanoTime()
                    plusOneCNeon(numbersPlusOneCopy)
                    val end = System.nanoTime()
                    end - start
                }.apply{ log("$plusOneCNeonTag $numElementsString") }
                addTimedWork(plusOneCNeonTag, numElements, plusOneCNeonTimedWork)

                // plus one in-place Kotlin
                val plusOneKotlinTag = "+1 in-place Kotlin"
                for(i in 0..numbersPlusOneCopy.lastIndex){numbersPlusOneCopy[i] = randomNumbers[i]}
                val plusOneKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    for(i in numbersPlusOneCopy.indices) numbersPlusOneCopy[i] += 1
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$plusOneKotlinTag $numElementsString") }
                addTimedWork(plusOneKotlinTag, numElements, plusOneKotlinTimedWork)

                // sum C
                val sumCTag = "Sum C"
                val sumCTimedWork = iterationTiming {
                    val start = System.nanoTime()
                    sum += sumC(sumNumbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$sumCTag $numElementsString") }
                addTimedWork(sumCTag, numElements, sumCTimedWork)

                // sum Kotlin
                val sumKotlinIntArraySumTag = "Sum Kotlin (IntArray.Sum)"
                val sumKotlinIntArraySumTimedWork = iterationTiming {
                    val start = System.nanoTime()
                    sum += sumNumbers.sum()
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$sumKotlinIntArraySumTag $numElementsString") }
                addTimedWork(sumKotlinIntArraySumTag, numElements, sumKotlinIntArraySumTimedWork)

                // sum Kotlin C-Style
                val sumKotlinCStyleTag = "Sum Kotlin (C-style)"
                val sumKotlinCStyleTimedWork = iterationTiming {
                    val start = System.nanoTime()
                    for(i in 0..sumNumbers.lastIndex) { sum += sumNumbers[i] }
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$sumKotlinCStyleTag $numElementsString") }
                addTimedWork(sumKotlinCStyleTag, numElements, sumKotlinCStyleTimedWork)

                // default reverse in Kotlin
                val reverseKotlinTag = "Reverse in Kotlin"
                val reverseKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    randomNumbers.reverse()
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseKotlinTag $numElementsString") }
                addTimedWork(reverseKotlinTag, numElements, reverseKotlinTimedWork)

                // C-equivalent reverse in Kotlin
                val reverseKotlinCStyleTag = "C-style reverse in Kotlin"
                val reverseKotlinCStyleTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    var left = 0; var right = randomNumbers.lastIndex; var tmp: Int;
                    while(left < right) {
                        tmp = randomNumbers[left];
                        randomNumbers[left++] = randomNumbers[right];
                        randomNumbers[right--] = tmp;
                    }
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseKotlinCStyleTag $numElementsString") }
                addTimedWork(reverseKotlinCStyleTag, numElements, reverseKotlinTimedWork)
            }
            Log.d(DEBUG_LOG_TAG, "The ending sum was: $sum")

            launch(Dispatchers.IO){
                val path = getExternalFilesDir(null)
                if(path == null) Log.e(DEBUG_LOG_TAG, "Couldn't create file to output .csv")
                val file = File(path, "jni_performance_data-${System.currentTimeMillis()}.csv")
                val fileContents = StringBuilder()
                fileContents.append("Manufacturer,Model,Processor Count,Timer Overhead\n")
                fileContents.append("${cpuInfo.manufacturer},${cpuInfo.model},${cpuInfo.processorCount},${timerOverhead.maxNanoseconds}\n")
                fileContents.append("\nElement Count,")
                testNumberOfElements.forEach { fileContents.append("$it,") }
                fileContents.replace(fileContents.lastIndex, fileContents.lastIndex+1, "\n")
                timedWork.forEach{ timedWorkEntry ->
                    val testName = timedWorkEntry.key
                    fileContents.append("$testName,")
                    timedWorkEntry.value.forEach{(numElements, work) ->
                         fileContents.append("${clocksToString(work.minNanoseconds)},")
                    }
                    fileContents.replace(fileContents.lastIndex, fileContents.lastIndex+1, "\n")
                }
                file.writeText(fileContents.toString())
            }

            launch(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Performance test complete!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun iterationTiming(maxIterationsNoChange: Int = 10, measuredWorkInNanoseconds: () -> Long): TimedWork {
        var iterationsSinceChange = 0
        var totalIterations = 0
        var minNanoseconds = Long.MAX_VALUE
        var maxNanoseconds = Long.MIN_VALUE
        while(iterationsSinceChange < maxIterationsNoChange) {
            val secs = measuredWorkInNanoseconds()
            iterationsSinceChange += 1; totalIterations += 1;
            if(secs < minNanoseconds) { minNanoseconds = secs; iterationsSinceChange = 0; }
            if(secs > maxNanoseconds) { maxNanoseconds = secs; iterationsSinceChange = 0; }
        }
        return TimedWork(minNanoseconds, maxNanoseconds, totalIterations)
    }

    private fun List<Int>.printable(): String {
        val s = StringBuilder()
        s.append("[")
        for(i in 0..<size-1){
            s.append("${this[i]}, ")
        }
        s.append("${this[size-1]}]")
        return s.toString()
    }
}