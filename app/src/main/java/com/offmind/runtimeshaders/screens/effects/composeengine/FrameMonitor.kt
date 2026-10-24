package com.offmind.runtimeshaders.screens.effects.composeengine

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * One frame as the platform reports it (`Window.OnFrameMetricsAvailableListener`), in milliseconds.
 * [uiMs] is the UI thread: input, animation (Compose recomposition runs here), measure/layout and
 * draw recording. [renderThreadMs] is sync + command issue + swap on RenderThread; [gpuMs] is the
 * GPU's own work for the frame.
 */
internal data class FrameSample(
    val intendedVsyncNs: Long,
    val uiMs: Double,
    val animationMs: Double,
    val layoutMs: Double,
    val drawMs: Double,
    val syncMs: Double,
    val issueMs: Double,
    val renderThreadMs: Double,
    val gpuMs: Double,
    val totalMs: Double,
    val deadlineMs: Double,
) {
    val missed get() = totalMs > deadlineMs

    fun toLog() = String.format(
        Locale.US,
        "ui=%.2f anim=%.2f layout=%.2f draw=%.2f sync=%.2f issue=%.2f rt=%.2f gpu=%.2f total=%.2f",
        uiMs, animationMs, layoutMs, drawMs, syncMs, issueMs, renderThreadMs, gpuMs, totalMs,
    )
}

/** Medians and 90th percentiles over one second of frames. */
internal data class FrameSummary(
    val frames: Int,
    val missed: Int,
    val uiMedian: Double,
    val renderThreadMedian: Double,
    val gpuMedian: Double,
    val gpuP90: Double,
    val totalP90: Double,
) {
    fun toLog() = String.format(
        Locale.US,
        "frames=%d missed=%d ui_med=%.2f rt_med=%.2f gpu_med=%.2f gpu_p90=%.2f total_p90=%.2f",
        frames, missed, uiMedian, renderThreadMedian, gpuMedian, gpuP90, totalP90,
    )

    fun toDisplay() = String.format(
        Locale.US,
        "%d fps · UI %.1f ms · RenderThread %.1f ms · GPU %.1f ms (p90 %.1f)",
        frames, uiMedian, renderThreadMedian, gpuMedian, gpuP90,
    )
}

private fun FrameMetrics.ms(id: Int) = getMetric(id) / 1_000_000.0

private fun FrameMetrics.toSample(): FrameSample {
    val ui = ms(FrameMetrics.INPUT_HANDLING_DURATION) + ms(FrameMetrics.ANIMATION_DURATION) +
        ms(FrameMetrics.LAYOUT_MEASURE_DURATION) + ms(FrameMetrics.DRAW_DURATION)
    val sync = ms(FrameMetrics.SYNC_DURATION)
    val issue = ms(FrameMetrics.COMMAND_ISSUE_DURATION)
    return FrameSample(
        intendedVsyncNs = getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP),
        uiMs = ui,
        animationMs = ms(FrameMetrics.ANIMATION_DURATION),
        layoutMs = ms(FrameMetrics.LAYOUT_MEASURE_DURATION),
        drawMs = ms(FrameMetrics.DRAW_DURATION),
        syncMs = sync,
        issueMs = issue,
        renderThreadMs = sync + issue + ms(FrameMetrics.SWAP_BUFFERS_DURATION),
        gpuMs = ms(FrameMetrics.GPU_DURATION),
        totalMs = ms(FrameMetrics.TOTAL_DURATION),
        deadlineMs = ms(FrameMetrics.DEADLINE),
    )
}

private fun List<Double>.percentile(p: Double): Double =
    if (isEmpty()) 0.0 else sorted()[((size - 1) * p).toInt()]

/**
 * Listens to the window's frame metrics while composed. Every frame goes to [onFrame] (on a
 * background thread); once a second a [FrameSummary] is logged under [ENGINE_LOG_TAG] with
 * [label] and published in the returned state.
 */
@Composable
internal fun rememberFrameSummary(
    label: String,
    onFrame: (FrameSample) -> Unit = {},
): State<FrameSummary?> {
    val window: Window? = LocalActivity.current?.window
    val summary = remember { mutableStateOf<FrameSummary?>(null) }
    val currentLabel by rememberUpdatedState(label)
    val currentOnFrame by rememberUpdatedState(onFrame)

    DisposableEffect(window) {
        if (window == null) return@DisposableEffect onDispose { }
        val thread = HandlerThread("ComposeEngineFrameMetrics").apply { start() }
        val main = Handler(Looper.getMainLooper())
        val window1s = mutableListOf<FrameSample>()
        var windowStartNs = 0L
        var windowLabel = ""
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            val sample = metrics.toSample()
            currentOnFrame(sample)
            // A new variant starts a new window: one summary never mixes two variants.
            if (windowLabel != currentLabel) {
                windowLabel = currentLabel
                window1s.clear()
                windowStartNs = 0L
            }
            if (windowStartNs == 0L) windowStartNs = sample.intendedVsyncNs
            window1s += sample
            if (sample.intendedVsyncNs - windowStartNs >= 1_000_000_000L) {
                val s = FrameSummary(
                    frames = window1s.size,
                    missed = window1s.count { it.missed },
                    uiMedian = window1s.map { it.uiMs }.percentile(0.5),
                    renderThreadMedian = window1s.map { it.renderThreadMs }.percentile(0.5),
                    gpuMedian = window1s.map { it.gpuMs }.percentile(0.5),
                    gpuP90 = window1s.map { it.gpuMs }.percentile(0.9),
                    totalP90 = window1s.map { it.totalMs }.percentile(0.9),
                )
                Log.i(ENGINE_LOG_TAG, "frames $currentLabel ${s.toLog()}")
                main.post { summary.value = s }
                window1s.clear()
                windowStartNs = sample.intendedVsyncNs
            }
        }
        window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        onDispose {
            window.removeOnFrameMetricsAvailableListener(listener)
            thread.quitSafely()
        }
    }
    return summary
}
