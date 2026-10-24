package com.offmind.runtimeshaders.screens.effects.composeengine

import android.os.Debug
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** The two sides of a "with / without a shader" demo. */
internal enum class Impl(val label: String) {
    Shader("AGSL shader"),
    NoShader("Without a shader"),
}

/** Java-heap allocation rate of the whole app, from ART's own counters. */
internal data class AllocRate(val kbPerSecond: Long, val gcPerSecond: Long) {
    fun toLog() = "allocKBps=$kbPerSecond gcPerSec=$gcPerSecond"
    fun toDisplay() = "Java heap: $kbPerSecond KB/s allocated · $gcPerSecond GC/s"
}

private fun runtimeStat(name: String): Long = Debug.getRuntimeStat(name)?.toLongOrNull() ?: 0L

/**
 * Logs (and returns) how many bytes the app allocates per second on the Java heap. Native memory,
 * such as a Bitmap's pixels, is not counted here.
 */
@Composable
internal fun recordAllocations(screen: String, variant: String): AllocRate? {
    var rate by remember { mutableStateOf<AllocRate?>(null) }
    LaunchedEffect(screen, variant) {
        var bytes = runtimeStat("art.gc.bytes-allocated")
        var gcs = runtimeStat("art.gc.gc-count")
        while (true) {
            delay(1_000)
            val nowBytes = runtimeStat("art.gc.bytes-allocated")
            val nowGcs = runtimeStat("art.gc.gc-count")
            val r = AllocRate((nowBytes - bytes) / 1024, nowGcs - gcs)
            Log.i(ENGINE_LOG_TAG, "alloc screen=$screen variant=$variant ${r.toLog()}")
            rate = r
            bytes = nowBytes
            gcs = nowGcs
        }
    }
    return rate
}
