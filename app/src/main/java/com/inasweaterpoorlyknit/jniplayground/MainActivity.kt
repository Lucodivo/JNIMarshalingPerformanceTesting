package com.inasweaterpoorlyknit.jniplayground

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.NANOSECONDS_PER_MICROSECOND
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.NANOSECONDS_PER_MILLISECOND
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.NANOSECONDS_PER_SECOND
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.intArrayArgIsCopyC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.nopCriticalC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.nopFastC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.nopNormalC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.rotateRightIntArrayC
import com.inasweaterpoorlyknit.jniplayground.JNIFunctions.Companion.stringFromJni
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

const val ITERATIVE_TESTING_COUNT = 20

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
        concatStr.append("${iter.next()}$separator")
        concatCount++
    }
    if(iter.hasNext()) concatStr.append(iter.next())
    return concatStr.toString()
}

fun nanosecondsToString(nanoseconds: Long): String  {
    return when(nanoseconds) {
        in 0..<1_000 -> { "${nanoseconds}ns" }
        in 0..<1_000_000 -> {
            val microseconds = nanoseconds.toDouble() / NANOSECONDS_PER_MICROSECOND.toDouble()
            "${"%.2f".format(microseconds)}µs"
        }
        in 0..<1_000_000_000 -> {
            val milliseconds = nanoseconds.toDouble() / NANOSECONDS_PER_MILLISECOND.toDouble()
            "${"%.2f".format(milliseconds)}ms"
        }
        else -> {
            val seconds = nanoseconds.toDouble() / NANOSECONDS_PER_SECOND.toDouble()
            "${"%.2f".format(seconds)}s"
        }
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

            logTwoColumn("$title (min)", nanosecondsToString(minNanoseconds))
            logTwoColumn("$title (max)", nanosecondsToString(maxNanoseconds))
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
/*
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
*/
            val cpuInfo = fetchCpuInfo()
//            fetchRamInfo()
//            cpuInfo.log()
//
//            TimedWork.logTimeHeader()
//
            val timerOverhead = iterationTiming(50){
                val start = System.nanoTime()
                val end = System.nanoTime()
                end - start
            }.apply{ log("System.nanoTime() Timer Overhead") }

            val nopNormalCOverhead = iterationTiming(50){
                val start = System.nanoTime()
                nopNormalC()
                val end = System.nanoTime()
                end - start
            }.apply{ log("C: Nop Normal") }

            val nopFastCOverhead = iterationTiming(50){
                val start = System.nanoTime()
                nopFastC()
                val end = System.nanoTime()
                end - start
            }.apply{ log("C: Nop @FastNative") }

            val nopCriticalCOverhead = iterationTiming(50){
                val start = System.nanoTime()
                nopCriticalC()
                val end = System.nanoTime()
                end - start
            }.apply{ log("C: Nop @CriticalNative") }

            val stringFromCTimedWork = iterationTiming(50){
                val start = System.nanoTime()
                stringFromJni()
                val end = System.nanoTime()
                end - start
            }.apply { log("C: string from JNI") }

            val testNumberOfElements = arrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000)
            val timedWork = HashMap<String, ArrayList<Pair<Int, TimedWork>>>()
            val intArrayIsCopy = BooleanArray(testNumberOfElements.size)

            fun addTimedWork(tag: String, dataSize: Int, work: TimedWork) {
                if(!timedWork.containsKey(tag)) { timedWork[tag] = ArrayList() }
                timedWork.getOrPut(tag){ ArrayList() }.add(Pair(dataSize, work))
            }

            testNumberOfElements.forEachIndexed { index, numElements ->
                val randomNumbers = IntArray(numElements) { rand.nextInt() }
                val numElementsString = "[${"%,d".format(numElements)}]"
                val randomString = randomASCIIString(numElements)
                val sumNumbers = IntArray(numElements) { rand.nextInt(-10, 11) }

                intArrayIsCopy[index] = intArrayArgIsCopyC(randomNumbers)
                Log.i(DEBUG_LOG_TAG, "intArrayArgIsCopyC ($numElements): ${intArrayIsCopy[index]}")

//                val rotateRightCTag = "C: Rotate Right Int Array"
//                val rotateCount = numElements / 3
//                val rotateRightCTimedWork = iterationTiming{
//                    val start = System.nanoTime()
//                    rotateRightIntArrayC(randomNumbers, rotateCount)
//                    val end = System.nanoTime()
//                    end - start
//                }.apply { log("$rotateRightCTag $numElementsString") }
//                addTimedWork(rotateRightCTag, numElements, rotateRightCTimedWork)
//
//                val rotateRightKotlinTag = "Kotlin: Rotate Right Int Array"
//                val rotateRightKotlinTimedWork = iterationTiming{
//                    val start = System.nanoTime()
//                    randomNumbers.rotateRight(rotateCount)
//                    val end = System.nanoTime()
//                    end - start
//                }.apply { log("$rotateRightKotlinTag $numElementsString") }
//                addTimedWork(rotateRightKotlinTag, numElements, rotateRightKotlinTimedWork)
/*
                val copyIntArrayCTag = "C: Copy Int Array"
                val copyIntArrayCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    copyIntArrayC(randomNumbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$copyIntArrayCTag $numElementsString") }
                addTimedWork(copyIntArrayCTag, numElements, copyIntArrayCTimedWork)

                val copyIntArrayKotlinTag = "Kotlin: Copy Int Array"
                val copyIntArrayKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    randomNumbers.copyOf()
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$copyIntArrayKotlinTag $numElementsString") }
                addTimedWork(copyIntArrayKotlinTag, numElements, copyIntArrayKotlinTimedWork)

                val plusOneMapKotlinTag = "Kotlin: +1 Map"
                val plusOneMapKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    randomNumbers.map { it + 1 }
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$plusOneMapKotlinTag $numElementsString") }
                addTimedWork(plusOneMapKotlinTag, numElements, plusOneMapKotlinTimedWork)

                val sortKotlinTag = "Kotlin: Sort"
                val sortKotlinTimedWork = iterationTiming{
                    val numbers = randomNumbers.copyOf()
                    val start = System.nanoTime()
                    numbers.sort()
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$sortKotlinTag $numElementsString") }
                addTimedWork(sortKotlinTag, numElements, sortKotlinTimedWork)

                val sortCTag = "C: Sort"
                val sortCTimedWork = iterationTiming{
                    val numbers = randomNumbers.copyOf()
                    val start = System.nanoTime()
                    sortC(numbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$sortCTag $numElementsString") }
                addTimedWork(sortCTag, numElements, sortCTimedWork)

                val reverseIntArrayCTag = "C: Reverse Int Array"
                val reverseIntArrayCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    reverseIntArrayC(randomNumbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseIntArrayCTag $numElementsString") }
                addTimedWork(reverseIntArrayCTag, numElements, reverseIntArrayCTimedWork)

                val reverseStringKotlinTag = "Kotlin: Reverse String"
                var reversedKotlinString: String
                val reverseStringKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    reversedKotlinString = randomString.reversed()
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseStringKotlinTag $numElementsString") }
                addTimedWork(reverseStringKotlinTag, numElements, reverseStringKotlinTimedWork)

                // reverse string in C
                val reverseStringCTag = "C: Reverse String"
                var reversedCString: String
                val reverseStringCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    reversedCString = reverseStringC(randomString)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseStringCTag $numElementsString") }
                addTimedWork(reverseStringCTag, numElements, reverseStringCTimedWork)

                val nopIntArrayCTag = "C: Nop Int Array Paremeter (@FastNative)"
                val nopIntArrayCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    intArrayNopC(randomNumbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$nopIntArrayCTag $numElementsString") }
                addTimedWork(nopIntArrayCTag, numElements, nopIntArrayCTimedWork)

                val numbersPlusOneCopy = randomNumbers.copyOf()
                val plusOneCTag = "C: +1"
                val plusOneCTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    plusOneC(randomNumbers)
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$plusOneCTag $numElementsString") }
                addTimedWork(plusOneCTag, numElements, plusOneCTimedWork)

                val plusOneCNeonTag = "C: +1 Neon"
                for(i in 0..numbersPlusOneCopy.lastIndex){numbersPlusOneCopy[i] = randomNumbers[i]}
                val plusOneCNeonTimedWork = iterationTiming {
                    val start = System.nanoTime()
                    plusOneCNeon(numbersPlusOneCopy)
                    val end = System.nanoTime()
                    end - start
                }.apply{ log("$plusOneCNeonTag $numElementsString") }
                addTimedWork(plusOneCNeonTag, numElements, plusOneCNeonTimedWork)

                val plusOneKotlinTag = "Kotlin: +1 In-Place"
                for(i in 0..numbersPlusOneCopy.lastIndex){numbersPlusOneCopy[i] = randomNumbers[i]}
                val plusOneKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    for(i in numbersPlusOneCopy.indices) numbersPlusOneCopy[i] += 1
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$plusOneKotlinTag $numElementsString") }
                addTimedWork(plusOneKotlinTag, numElements, plusOneKotlinTimedWork)

                var cSum = 0
                val sumCTag = "C: Sum"
                val sumCTimedWork = iterationTiming {
                    val start = System.nanoTime()
                    var sum = 0
                    sum = sumC(sumNumbers)
                    val end = System.nanoTime()
                    cSum = sum
                    end - start
                }.apply { log("$sumCTag $numElementsString") }
                addTimedWork(sumCTag, numElements, sumCTimedWork)

                var kotlinSum = 0
                val sumCStyleKotlinTag = "Kotlin: Sum (C-style)"
                val sumCStyleKotlinTimedWork = iterationTiming {
                    val start = System.nanoTime()
                    var sum = 0
                    for(i in 0..sumNumbers.lastIndex) { sum += sumNumbers[i] }
                    val end = System.nanoTime()
                    kotlinSum = sum
                    end - start
                }.apply { log("$sumCStyleKotlinTag $numElementsString") }
                addTimedWork(sumCStyleKotlinTag, numElements, sumCStyleKotlinTimedWork)

                var kotlinSumSum = 0
                val sumKotlinIntArraySumTag = "Kotlin: Sum (IntArray.Sum)"
                val sumKotlinIntArraySumTimedWork = iterationTiming {
                    val start = System.nanoTime()
                    val sum = sumNumbers.sum()
                    val end = System.nanoTime()
                    kotlinSumSum = sum
                    end - start
                }.apply { log("$sumKotlinIntArraySumTag $numElementsString") }
                addTimedWork(sumKotlinIntArraySumTag, numElements, sumKotlinIntArraySumTimedWork)

                Log.w(DEBUG_LOG_TAG, "C Sum: $cSum")
                Log.w(DEBUG_LOG_TAG, "Kotlin Sum: $kotlinSum")
                Log.w(DEBUG_LOG_TAG, "Kotlin Sum Sum: $kotlinSumSum")

                val reverseIntArrayKotlinTag = "Kotlin: Reverse Int Array (default)"
                val reverseIntArrayKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    randomNumbers.reverse()
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseIntArrayKotlinTag $numElementsString") }
                addTimedWork(reverseIntArrayKotlinTag, numElements, reverseIntArrayKotlinTimedWork)

                val reverseCStyleKotlinTag = "Kotlin: Reverse Int Array (C-style)"
                val reverseCStyleKotlinTimedWork = iterationTiming{
                    val start = System.nanoTime()
                    var left = 0; var right = randomNumbers.lastIndex; var tmp: Int;
                    while(left < right) {
                        tmp = randomNumbers[left]
                        randomNumbers[left++] = randomNumbers[right]
                        randomNumbers[right--] = tmp
                    }
                    val end = System.nanoTime()
                    end - start
                }.apply { log("$reverseCStyleKotlinTag $numElementsString") }
                addTimedWork(reverseCStyleKotlinTag, numElements, reverseCStyleKotlinTimedWork)
*/
            }

            launch(Dispatchers.IO){
                val path = getExternalFilesDir(null)
                if(path == null) Log.e(DEBUG_LOG_TAG, "Couldn't create file to output .csv")
                val file = File(path, "jni_performance_data-${System.currentTimeMillis()}.csv")
                val fileContents = StringBuilder()
                fileContents.append("Manufacturer,Model,Processor Count\n")
                fileContents.append("${cpuInfo.manufacturer},${cpuInfo.model},${cpuInfo.processorCount},\n")
                fileContents.append("\nTimer Overhead (Min), no-op normal C (Min), no-op fast C (Min), no-op native C (Min), 'Hello, World!' String from C (Min)\n")
                fileContents.append("${nanosecondsToString(timerOverhead.minNanoseconds)}, ${nanosecondsToString(nopNormalCOverhead.minNanoseconds)}, ${nanosecondsToString(nopFastCOverhead.minNanoseconds)}, ${nanosecondsToString(nopCriticalCOverhead.minNanoseconds)}, ${nanosecondsToString(stringFromCTimedWork.minNanoseconds)}\n")
                fileContents.append("\nTimer Overhead (Max), no-op normal C (Max), no-op fast C (Max), no-op native C (Max), 'Hello, World!' String from C (Max)\n")
                fileContents.append("${nanosecondsToString(timerOverhead.maxNanoseconds)}, ${nanosecondsToString(nopNormalCOverhead.maxNanoseconds)}, ${nanosecondsToString(nopFastCOverhead.maxNanoseconds)}, ${nanosecondsToString(nopCriticalCOverhead.maxNanoseconds)}, ${nanosecondsToString(stringFromCTimedWork.maxNanoseconds)}\n")
                fileContents.append("\nElement Count,")
                testNumberOfElements.forEach { fileContents.append("$it,") }
                fileContents.replace(fileContents.lastIndex, fileContents.lastIndex+1, "\n")
                timedWork.map { (workTag: String, sizedTimedWork: ArrayList<Pair<Int, TimedWork>>) ->
                    val csvRowStringBuilder = StringBuilder()
                    csvRowStringBuilder.append("$workTag,")
                    sizedTimedWork.forEach{(size: Int, work: TimedWork) ->
                        csvRowStringBuilder.append("${nanosecondsToString(work.minNanoseconds)},")
                    }
                    csvRowStringBuilder.removeSuffix(",")
                    csvRowStringBuilder.toString()
                }.sorted().forEach{ fileContents.append("${it}\n") }
                file.writeText(fileContents.toString())
            }

            launch(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Performance test complete!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun iterationTiming(maxIterationsNoChange: Int = ITERATIVE_TESTING_COUNT, measuredWorkInNanoseconds: () -> Long): TimedWork {
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