package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.Locale

/**
 * Chapter 11: the four prices of a shader, measured on this device. "Create" and "update" are
 * timed in-process on the main thread, where composition would create them. "First draw" swaps
 * the shader of a static preview and logs the frames that follow: a new program is compiled on
 * RenderThread for the first frame that uses it.
 */
@Composable
internal fun ShaderCostScreen() {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<String>() }
    var preview by remember { mutableStateOf(PreviewShader(newRuntimeShader(GLOW_SHADER), "initial")) }

    // Frames after a swap; the listener runs on the frame-metrics thread.
    val pending = remember { PendingSwap() }
    rememberFrameSummary("screen=shader-cost variant=${preview.label}") { sample ->
        pending.offer(sample)?.let { line -> scope.launch { results.add(0, line) } }
    }

    EngineScreen(
        title = "What a shader costs",
        subtitle = "Create, first draw, update. Numbers from this phone; run a release build.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CostButton("CPU: create & update", enabled = !running) {
                running = true
                scope.launch {
                    measureCpuCosts().forEach { results.add(0, it) }
                    running = false
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CostButton("First draw: new program", enabled = !running) {
                val salt = SystemClock.elapsedRealtimeNanos()
                pending.arm("newProgram")
                preview = PreviewShader(newRuntimeShader(saltedGlowShader(salt)), "newProgram")
            }
            CostButton("Same program, new object", enabled = !running) {
                pending.arm("sameProgram")
                preview = PreviewShader(newRuntimeShader(GLOW_SHADER), "sameProgram")
            }
        }

        val shader = preview.shader
        val brush = remember(shader) { ShaderBrush(shader) }
        Box(
            Modifier
                .width(280.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .drawBehind {
                    shader.setFloatUniform("size", size.width, size.height)
                    shader.setFloatUniform("time", 0.45f) // static: frames happen only on a swap
                    drawRect(brush)
                },
        )

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (running) Text("measuring…", style = MonoSmall.copy(color = EngineAccent))
            results.forEach { Text(it, style = MonoSmall) }
        }
    }
}

private class PreviewShader(val shader: RuntimeShader, val label: String)

/** Collects the first frames whose vsync is after a swap, then reports them once. */
private class PendingSwap {
    private val lock = Any()
    private var label: String? = null
    private var armedAtNs = 0L
    private val frames = mutableListOf<FrameSample>()

    fun arm(label: String) = synchronized(lock) {
        this.label = label
        armedAtNs = System.nanoTime()
        frames.clear()
    }

    fun offer(sample: FrameSample): String? = synchronized(lock) {
        val current = label ?: return null
        if (sample.intendedVsyncNs < armedAtNs) return null
        frames += sample
        if (frames.size < FRAMES_AFTER_SWAP) return null
        val line = "firstDraw $current " + frames.mapIndexed { i, f -> "f$i[${f.toLog()}]" }.joinToString(" ")
        Log.i(ENGINE_LOG_TAG, line)
        label = null
        line
    }

    private companion object {
        const val FRAMES_AFTER_SWAP = 3
    }
}

@Composable
private fun CostButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (enabled) EngineText else EngineDim,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.10f else 0.04f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

private inline fun timeNs(block: () -> Unit): Long {
    val start = System.nanoTime()
    block()
    return System.nanoTime() - start
}

private fun List<Long>.stats(perCall: Int = 1): String {
    val us = map { it / 1_000.0 / perCall }.sorted()
    return String.format(
        Locale.US, "med=%.2fus p10=%.2fus p90=%.2fus n=%d",
        us[us.size / 2], us[us.size / 10], us[us.size * 9 / 10], us.size,
    )
}

/** Runs on the main thread, yielding between samples so the UI keeps drawing. */
private suspend fun measureCpuCosts(): List<String> {
    // Async: the section spans suspension points, with frames drawn in between.
    Trace.beginAsyncSection("ComposeEngine#measureCpuCosts", 0)
    try {
        repeat(WARMUP) { RuntimeShader(GLOW_SHADER); yield() }

        val sameSource = List(SAMPLES) { timeNs { sink = RuntimeShader(GLOW_SHADER) }.also { yield() } }
        val uniqueSource = List(SAMPLES) {
            val source = saltedGlowShader(SystemClock.elapsedRealtimeNanos() + it)
            timeNs { sink = RuntimeShader(source) }.also { yield() }
        }
        val shader = RuntimeShader(GLOW_SHADER)
        val uniform = List(SAMPLES) {
            timeNs { repeat(BATCH) { i -> shader.setFloatUniform("time", i * 0.001f) } }.also { yield() }
        }
        val effectShader = RuntimeShader(areaShaderSource(sampleContent = true))
        val effect = List(SAMPLES) {
            timeNs { repeat(BATCH) { sink = RenderEffect.createRuntimeShaderEffect(effectShader, "content") } }
                .also { yield() }
        }
        val brush = List(SAMPLES) { timeNs { repeat(BATCH) { sink = ShaderBrush(shader) } }.also { yield() } }

        return listOf(
            "new RuntimeShader, same source     ${sameSource.stats()}",
            "new RuntimeShader, unique source   ${uniqueSource.stats()}",
            "setFloatUniform (per call)         ${uniform.stats(BATCH)}",
            "createRuntimeShaderEffect (per call) ${effect.stats(BATCH)}",
            "ShaderBrush(shader) (per call)     ${brush.stats(BATCH)}",
        ).onEach { Log.i(ENGINE_LOG_TAG, "cpuCost $it") }
    } finally {
        Trace.endAsyncSection("ComposeEngine#measureCpuCosts", 0)
    }
}

// Every timed result is stored here, so R8 cannot drop an allocation whose result is unused.
@Volatile
private var sink: Any? = null

private const val WARMUP = 20
private const val SAMPLES = 100
private const val BATCH = 1_000
