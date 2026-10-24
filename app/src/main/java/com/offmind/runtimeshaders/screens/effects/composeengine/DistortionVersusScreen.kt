package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.os.Trace
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.intellij.lang.annotations.Language
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

private val ListHeight = 420.dp
private const val MESH_W = 24
private const val MESH_H = 32

// Ripple: radial displacement around a moving centre. The same formula on both sides.
private const val RIPPLE_FREQ = 0.045f
private const val RIPPLE_SPEED = 1.5f
private const val RIPPLE_FALLOFF = 0.0035f
private const val RIPPLE_AMP = 14f

private fun rippleCenter(t: Float, w: Float, h: Float): Pair<Float, Float> =
    w * (0.5f + 0.25f * cos(2f * PI.toFloat() * t)) to h * (0.5f + 0.25f * sin(4f * PI.toFloat() * t))

@Language("AGSL")
private const val RIPPLE_SHADER = """
uniform shader content;
uniform float2 center;
uniform float time;
uniform float amp;

half4 main(float2 p) {
    float2 d = p - center;
    float r = length(d);
    float w = sin(r * 0.045 - time * 6.2831853 * 1.5) * amp * exp(-r * 0.0035);
    return content.eval(p + d / (r + 0.001) * w);   // read the pixel from somewhere else
}
"""

@Composable
internal fun DistortionVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(Impl.Shader) }
    val t = rememberInfiniteTransition(label = "ripple")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(8_000, easing = LinearEasing)), label = "t")
    val frames by rememberFrameSummary("screen=vs-distortion variant=${impl.name}")
    val alloc = recordAllocations("vs-distortion", impl.name)

    EngineScreen(
        title = "Distort what is underneath",
        subtitle = "A ripple over a live, scrolling list: the shader reads other pixels; without it, a snapshot.",
    ) {
        EngineChoice("Implementation", Impl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        val box = Modifier.fillMaxWidth().height(ListHeight).clipToBounds()
        when (impl) {
            Impl.Shader -> ShaderRipple(t, box)
            Impl.NoShader -> SnapshotMeshRipple(t, box)
        }
        Text(
            if (impl == Impl.Shader) "RenderEffect: content.eval(p + displacement) per pixel"
            else "GraphicsLayer.toImageBitmap() every frame → Canvas.drawBitmapMesh (24×32 grid)",
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

/** The live content: an endless list that scrolls by itself (read in placement, so it is cheap). */
@Composable
private fun ScrollingList(t: State<Float>) {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Top, unbounded = true)
            .offset { IntOffset(0, -((t.value * 8f % 1f) * 16 * 64.dp.toPx()).toInt()) },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(32) { i ->
            val hue = (i % 16 * 37 % 360).toFloat() // repeats every 16 rows, like the scroll
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.hsv(hue, 0.45f, 0.35f))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(50)).background(Color.hsv(hue, 0.6f, 0.9f)))
                Text("Message ${i % 16 + 1} · the list keeps scrolling", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ShaderRipple(t: State<Float>, modifier: Modifier) {
    val shader = remember { newRuntimeShader(RIPPLE_SHADER).apply { setFloatUniform("amp", RIPPLE_AMP) } }
    Box(
        modifier.graphicsLayer {
            val (cx, cy) = rippleCenter(t.value, size.width, size.height)
            shader.setFloatUniform("center", cx, cy)
            shader.setFloatUniform("time", t.value * 8f)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        },
    ) { ScrollingList(t) }
}

/**
 * The best route without a shader: record the live list into a GraphicsLayer, read it back into a
 * Bitmap every frame, and warp that bitmap with a vertex grid (drawBitmapMesh moves vertices, not
 * pixels — the closest thing to a displacement the Canvas has). The screen shows the latest
 * snapshot, so it lags behind the list by the readback.
 */
@Composable
private fun SnapshotMeshRipple(t: State<Float>, modifier: Modifier) {
    val layer = rememberGraphicsLayer()
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    val verts = remember { FloatArray((MESH_W + 1) * (MESH_H + 1) * 2) }
    val stats = remember { ReadbackStats() }

    LaunchedEffect(layer) {
        while (true) {
            withFrameNanos { }
            if (layer.size.width == 0) continue
            val start = System.nanoTime()
            Trace.beginAsyncSection("ComposeEngine#toImageBitmap", 0)
            val bitmap = layer.toImageBitmap().asAndroidBitmap()
            Trace.endAsyncSection("ComposeEngine#toImageBitmap", 0)
            stats.add(System.nanoTime() - start, bitmap.allocationByteCount)
            snapshot = bitmap
        }
    }

    Box(
        modifier.drawWithContent {
            // The live list goes into the layer only; what the screen shows is the warped snapshot.
            layer.record { this@drawWithContent.drawContent() }
            val bitmap = snapshot ?: return@drawWithContent drawLayer(layer)
            val (cx, cy) = rippleCenter(t.value, size.width, size.height)
            val phase = t.value * 8f * 2f * PI.toFloat() * RIPPLE_SPEED
            var k = 0
            for (j in 0..MESH_H) {
                for (i in 0..MESH_W) {
                    val x = size.width * i / MESH_W
                    val y = size.height * j / MESH_H
                    val dx = x - cx
                    val dy = y - cy
                    val r = hypot(dx, dy)
                    val w = sin(r * RIPPLE_FREQ - phase) * RIPPLE_AMP * exp(-r * RIPPLE_FALLOFF)
                    // a vertex moves against the displacement, so the pixel it carries lands where
                    // the shader would read it from
                    verts[k++] = x - dx / (r + 0.001f) * w
                    verts[k++] = y - dy / (r + 0.001f) * w
                }
            }
            drawIntoCanvas { it.nativeCanvas.drawBitmapMesh(bitmap, MESH_W, MESH_H, verts, 0, null, 0, null) }
        },
    ) { ScrollingList(t) }
}

/** Once a second: how long a readback takes and how many bytes of pixels it allocates. */
private class ReadbackStats {
    private val lock = Any()
    private val durations = mutableListOf<Long>()
    private var bytes = 0L
    private var windowStart = System.nanoTime()

    fun add(durationNs: Long, allocated: Int) = synchronized(lock) {
        durations += durationNs
        bytes += allocated
        val now = System.nanoTime()
        if (now - windowStart < 1_000_000_000L) return
        val ms = durations.map { it / 1e6 }.sorted()
        Log.i(
            ENGINE_LOG_TAG,
            String.format(
                Locale.US,
                "readback screen=vs-distortion count=%d med_ms=%.2f p90_ms=%.2f pixelMBps=%.1f",
                ms.size, ms[ms.size / 2], ms[ms.size * 9 / 10], bytes / 1048576.0,
            ),
        )
        durations.clear()
        bytes = 0
        windowStart = now
    }
}
