package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas.VertexMode
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Trace
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.intellij.lang.annotations.Language
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// The particle model, the same on both sides. The bubble is cut into square cells; each cell
// starts to fly after a delay that sweeps left to right, along one direction (up and to the
// right) with its own speed and a little sideways jitter, fading out as it goes.
private const val SWEEP = 0.55f      // share of the timeline the left-to-right sweep takes
private const val SPREAD = 0.12f     // random extra delay per cell
private const val LIFE = 0.45f       // how long one cell flies, in timeline units
private const val END = 1.12f        // after this every cell is gone
private val DirX = 0.55f / sqrt(0.55f * 0.55f + 1f)
private val DirY = -1f / sqrt(0.55f * 0.55f + 1f)

private fun fract(x: Float) = x - floor(x)

/** Per-cell random number in 0…1, the same formula as `hash` in the shader. */
private fun hash(cx: Float, cy: Float, k: Float) =
    fract(sin(cx * (127.1f + k) + cy * (311.7f - k)) * 43758.5453f)

@Language("AGSL")
private const val DUST_SHADER = """
uniform shader content;
uniform float4 rect;      // the bubble in layer pixels: left, top, right, bottom
uniform float cell;       // particle size, px
uniform float maxDrift;   // longest flight, px
uniform float t;          // timeline, 0 … 1.12
uniform float2 dir;       // flight direction (unit)

float hash(float2 c, float k) { return fract(sin(dot(c, float2(127.1 + k, 311.7 - k))) * 43758.5453); }

half4 main(float2 p) {
    if (t >= 1.12) return half4(0);
    float2 perp = float2(-dir.y, dir.x);
    // A pixel cannot push a particle; it can only ask "which particle has landed on me?".
    // Walk back along the flight direction and test each cell met on the way.
    for (int i = 0; i < 64; i++) {
        float s = float(i) * cell * 0.7;
        if (s > maxDrift + cell) break;
        for (int k = -1; k <= 1; k++) {
            float2 c = floor((p - dir * s - perp * float(k) * cell - rect.xy) / cell);
            float2 cc = rect.xy + (c + 0.5) * cell;
            if (cc.x < rect.x || cc.x > rect.z || cc.y < rect.y || cc.y > rect.w) continue;
            float delay = 0.55 * (cc.x - rect.x) / (rect.z - rect.x) + 0.12 * hash(c, 0.0);
            float u = clamp((t - delay) / 0.45, 0.0, 1.0);
            float2 disp = dir * maxDrift * (0.35 + 0.65 * hash(c, 1.0)) * pow(u, 1.6)
                        + perp * (hash(c, 2.0) * 2.0 - 1.0) * cell * u;
            float2 local = p - (cc + disp);
            if (abs(local.x) <= cell * 0.5 && abs(local.y) <= cell * 0.5) {
                return content.eval(cc + local) * half(1.0 - u);
            }
        }
    }
    return half4(0);
}
"""

@Composable
internal fun DustVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(Impl.Shader) }
    val t = rememberInfiniteTransition(label = "dust")
        .animateFloat(-0.15f, 1.35f, infiniteRepeatable(tween(3_000, easing = LinearEasing)), label = "t")
    val frames by rememberFrameSummary("screen=vs-dust variant=${impl.name}")
    val alloc = recordAllocations("vs-dust", impl.name)

    EngineScreen(
        title = "Turn a view into dust",
        subtitle = "A message bubble disintegrates, like a deleted message in a chat app.",
    ) {
        EngineChoice("Implementation", Impl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        val stage = Modifier.fillMaxWidth().height(300.dp)
        when (impl) {
            Impl.Shader -> ShaderDust(t, stage)
            Impl.NoShader -> VertexDust(t, stage)
        }
        Text(
            if (impl == Impl.Shader) "RenderEffect on the live bubble: each pixel finds the particle that landed on it"
            else "snapshot once → ~20 000 textured quads, positions on the CPU every frame → one drawVertices",
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

/** The view being deleted: an ordinary chat bubble. */
@Composable
private fun MessageBubble(modifier: Modifier) {
    Column(
        modifier
            .width(280.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF3A6EA5))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Are we still on for the shader night?", color = Color.White, fontSize = 15.sp)
        Text("I'll bring the GPU 🔥", color = Color.White, fontSize = 15.sp)
        Text("21:47", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
    }
}

/** Where the bubble sits inside the stage, in px; plain fields, read while drawing. */
private class BubbleBounds {
    var rect = Rect.Zero
}

@Composable
private fun ShaderDust(t: State<Float>, modifier: Modifier) {
    val shader = remember { newRuntimeShader(DUST_SHADER) }
    val bounds = remember { BubbleBounds() }
    Box(
        modifier.graphicsLayer {
            val r = bounds.rect
            shader.setFloatUniform("rect", r.left, r.top, r.right, r.bottom)
            shader.setFloatUniform("cell", 1.dp.toPx())
            shader.setFloatUniform("maxDrift", 40.dp.toPx())
            shader.setFloatUniform("t", t.value)
            shader.setFloatUniform("dir", DirX, DirY)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        },
    ) {
        MessageBubble(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp)
                .onPlaced { bounds.rect = it.boundsInParent() },
        )
    }
}

/**
 * The best version without a custom shader, the way it is usually done: take a snapshot of the
 * view once, replace the view with particles, and move them on the CPU. Each particle is a
 * textured quad (two triangles) cut from the snapshot by a BitmapShader; all of them go to the
 * GPU in one Canvas.drawVertices call, with per-vertex alpha for the fade.
 */
@Composable
private fun VertexDust(t: State<Float>, modifier: Modifier) {
    val layer = rememberGraphicsLayer()
    val bounds = remember { BubbleBounds() }
    var particles by remember { mutableStateOf<DustParticles?>(null) }
    val cellPx = with(LocalDensity.current) { 1.dp.toPx() }

    LaunchedEffect(layer, cellPx) {
        while (layer.size.width == 0) delay(16)
        val snapshot = layer.toImageBitmap().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        particles = DustParticles(snapshot, cellPx)
    }

    Box(
        modifier.drawWithContent {
            val p = particles
            if (p == null) {
                drawContent()
                return@drawWithContent
            }
            p.update(t.value, bounds.rect, 40.dp.toPx())
            drawIntoCanvas { p.draw(it.nativeCanvas) }
        },
    ) {
        MessageBubble(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp)
                .onPlaced { bounds.rect = it.boundsInParent() }
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
        )
    }
}

private class DustParticles(snapshot: Bitmap, private val cell: Float) {
    private val cols = (snapshot.width / cell).toInt()
    private val rows = (snapshot.height / cell).toInt()
    private val cellX: FloatArray
    private val cellY: FloatArray
    val count: Int
    private val verts: FloatArray
    private val texs: FloatArray
    private val colors: IntArray
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(snapshot, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    init {
        // Keep only the cells the bubble covers (its rounded corners are transparent).
        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        for (j in 0 until rows) for (i in 0 until cols) {
            val px = ((i + 0.5f) * cell).toInt().coerceIn(0, snapshot.width - 1)
            val py = ((j + 0.5f) * cell).toInt().coerceIn(0, snapshot.height - 1)
            if (snapshot.getPixel(px, py) ushr 24 > 8) { xs += i.toFloat(); ys += j.toFloat() }
        }
        count = xs.size
        cellX = xs.toFloatArray()
        cellY = ys.toFloatArray()
        verts = FloatArray(count * 12)
        texs = FloatArray(count * 12)
        colors = IntArray(count * 6)
        for (n in 0 until count) {
            val x0 = cellX[n] * cell
            val y0 = cellY[n] * cell
            quad(texs, n, x0, y0)
        }
    }

    private fun quad(out: FloatArray, n: Int, x0: Float, y0: Float) {
        val x1 = x0 + cell
        val y1 = y0 + cell
        val o = n * 12
        out[o] = x0; out[o + 1] = y0; out[o + 2] = x1; out[o + 3] = y0; out[o + 4] = x1; out[o + 5] = y1
        out[o + 6] = x0; out[o + 7] = y0; out[o + 8] = x1; out[o + 9] = y1; out[o + 10] = x0; out[o + 11] = y1
    }

    /** Moves every particle for timeline value [t]; runs on the UI thread, every frame. */
    fun update(t: Float, rect: Rect, maxDrift: Float) {
        Trace.beginSection("ComposeEngine#dustUpdate")
        val w = cols * cell
        for (n in 0 until count) {
            val cx = cellX[n]
            val cy = cellY[n]
            val ccx = (cx + 0.5f) * cell
            val delay = SWEEP * ccx / w + SPREAD * hash(cx, cy, 0f)
            val u = ((t - delay) / LIFE).coerceIn(0f, 1f)
            val m = maxDrift * (0.35f + 0.65f * hash(cx, cy, 1f)) * u.pow(1.6f)
            val j = (hash(cx, cy, 2f) * 2f - 1f) * cell * u
            val dx = DirX * m - DirY * j
            val dy = DirY * m + DirX * j
            quad(verts, n, rect.left + cx * cell + dx, rect.top + cy * cell + dy)
            val alpha = ((1f - u) * 255f).toInt() shl 24 or 0xFFFFFF
            val c = n * 6
            for (v in 0 until 6) colors[c + v] = alpha
        }
        Trace.endSection()
    }

    fun draw(canvas: android.graphics.Canvas) {
        canvas.drawVertices(VertexMode.TRIANGLES, verts.size, verts, 0, texs, 0, colors, 0, null, 0, 0, paint)
    }
}
