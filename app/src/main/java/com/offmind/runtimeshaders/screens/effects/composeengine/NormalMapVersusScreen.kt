package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private val Albedo = floatArrayOf(0.62f, 0.64f, 0.68f) // brushed steel
private const val AMBIENT = 0.18f

private enum class LightImpl(val label: String) {
    ShaderPoint("AGSL: point light + highlight"),
    ShaderDirectional("AGSL: directional, diffuse"),
    NoShader("Without a shader: ColorMatrix"),
}

@Language("AGSL")
private const val NORMAL_LIGHT_SHADER = """
uniform shader normals;     // the normal map, xyz packed as rgb
uniform half3 albedo;
uniform float3 light;       // point light: position (px, px, height px); directional: unit vector
uniform float isPoint;
uniform float ambient;

half4 main(float2 p) {
    float3 n = normalize(float3(normals.eval(p).rgb) * 2.0 - 1.0);
    float3 l = isPoint > 0.5 ? normalize(light - float3(p, 0.0)) : light;
    float diffuse = max(dot(n, l), 0.0);
    float highlight = 0.0;
    if (isPoint > 0.5) {
        float3 h = normalize(l + float3(0.0, 0.0, 1.0));      // viewer straight above
        highlight = pow(max(dot(n, h), 0.0), 48.0) * 0.7;
    }
    return half4(albedo * half(ambient + diffuse) + half(highlight), 1.0);
}
"""

@Composable
internal fun NormalMapVersusScreen() {
    var impl by rememberSaveable { mutableStateOf(LightImpl.ShaderPoint) }
    val angle = rememberInfiniteTransition(label = "light")
        .animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(5_000, easing = LinearEasing)), label = "a")
    val frames by rememberFrameSummary("screen=vs-normalmap variant=${impl.name}")
    val alloc = recordAllocations("vs-normalmap", impl.name)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var normals by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(size) {
        if (size.width == 0) return@LaunchedEffect
        normals = withContext(Dispatchers.Default) { scratchedNormalMap(size.width, size.height) }
    }

    EngineScreen(
        title = "Scratches and cracks under a moving light",
        subtitle = "A normal map lit per pixel. Without a shader only a far, directional light is possible.",
    ) {
        EngineChoice("Implementation", LightImpl.entries, impl, { it.label }) { impl = it }
        FrameReadout(frames)
        Text(alloc?.toDisplay() ?: "", style = MonoSmall.copy(color = EngineDim))
        val box = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .onSizeChanged { size = it }
        val map = normals
        when {
            map == null -> Box(box)
            impl == LightImpl.NoShader -> ColorMatrixLighting(angle, map, box)
            else -> ShaderLighting(angle, map, point = impl == LightImpl.ShaderPoint, box)
        }
        Text(
            when (impl) {
                LightImpl.ShaderPoint -> "per pixel: its own light direction, diffuse + a specular highlight"
                LightImpl.ShaderDirectional -> "per pixel: one light direction for all, diffuse only — what the matrix can match"
                LightImpl.NoShader -> "the normal map drawn through a ColorMatrix: each row is albedo × (N · L) + ambient"
            },
            color = EngineDim,
            fontSize = 11.sp,
        )
    }
}

/** A light far away, turning around the card and leaning in at 45°. */
private fun directionalLight(angle: Float): FloatArray {
    val s = sqrt(0.5f)
    return floatArrayOf(cos(angle) * s, sin(angle) * s, s)
}

@Composable
private fun ShaderLighting(angle: State<Float>, map: Bitmap, point: Boolean, modifier: Modifier) {
    val shader = remember(map) {
        newRuntimeShader(NORMAL_LIGHT_SHADER).apply {
            setInputShader("normals", BitmapShader(map, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
            setFloatUniform("albedo", Albedo[0], Albedo[1], Albedo[2])
            setFloatUniform("ambient", AMBIENT)
        }
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    Box(
        modifier.drawBehind {
            if (point) {
                // a lamp circling just above the card
                val x = size.width * (0.5f + 0.4f * cos(angle.value))
                val y = size.height * (0.5f + 0.4f * sin(angle.value))
                shader.setFloatUniform("light", x, y, size.minDimension * 0.35f)
                shader.setFloatUniform("isPoint", 1f)
            } else {
                val l = directionalLight(angle.value)
                shader.setFloatUniform("light", l[0], l[1], l[2])
                shader.setFloatUniform("isPoint", 0f)
            }
            drawRect(brush)
        },
    )
}

/**
 * Lighting without a shader. A normal map stores each pixel's normal as color: n = 2 · rgb − 1.
 * Diffuse light is n · l, a weighted sum of r, g and b plus a constant — exactly what one row of
 * a color matrix computes. So drawing the normal map through a matrix whose every row is
 * albedo × (2·lx, 2·ly, 2·lz) with offset albedo × (ambient − lx − ly − lz) gives a lit surface in
 * one draw call. What it cannot do: a light whose direction differs per pixel (a point light), or
 * anything non-linear such as a highlight; and the result is clamped after the ambient is added,
 * not before (back-facing pixels get slightly darker than ambient).
 */
@Composable
private fun ColorMatrixLighting(angle: State<Float>, map: Bitmap, modifier: Modifier) {
    val image: ImageBitmap = remember(map) { map.asImageBitmap() }
    val values = remember { FloatArray(20) }
    Box(
        modifier.drawBehind {
            val l = directionalLight(angle.value)
            val dot0 = l[0] + l[1] + l[2]
            for (row in 0 until 3) {
                val a = Albedo[row]
                values[row * 5] = a * 2f * l[0]
                values[row * 5 + 1] = a * 2f * l[1]
                values[row * 5 + 2] = a * 2f * l[2]
                values[row * 5 + 3] = 0f
                values[row * 5 + 4] = a * (AMBIENT - dot0) * 255f
            }
            values[18] = 1f // alpha stays
            drawImage(image, colorFilter = ColorFilter.colorMatrix(ColorMatrix(values)))
        },
    )
}

/**
 * A procedural normal map of scratched, cracked steel: fine brushed grain, a few dozen straight
 * scratches and three branching cracks, as a height field turned into normals. Built once, on a
 * background thread.
 */
private fun scratchedNormalMap(w: Int, h: Int): Bitmap {
    val height = FloatArray(w * h)
    val rnd = Random(7)
    // brushed grain: noise stretched along x
    fun valueNoise(x: Float, y: Float, seed: Int): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val fx = x - xi
        val fy = y - yi
        fun h(i: Int, j: Int): Float {
            var n = i * 374761393 + j * 668265263 + seed * 1442695041
            n = (n xor (n ushr 13)) * 1274126177
            return ((n xor (n ushr 16)) and 0xFFFF) / 65535f
        }
        val u = fx * fx * (3 - 2 * fx)
        val v = fy * fy * (3 - 2 * fy)
        val a = h(xi, yi) + (h(xi + 1, yi) - h(xi, yi)) * u
        val b = h(xi, yi + 1) + (h(xi + 1, yi + 1) - h(xi, yi + 1)) * u
        return a + (b - a) * v
    }
    for (y in 0 until h) for (x in 0 until w) {
        height[y * w + x] = 0.35f * valueNoise(x / 90f, y / 1.6f, 1) + 0.25f * valueNoise(x / 9f, y / 9f, 2)
    }
    // grooves: stamp a rounded V along a segment, keeping the deepest value
    fun groove(x0: Float, y0: Float, x1: Float, y1: Float, radius: Float, depth: Float) {
        val len = hypot(x1 - x0, y1 - y0)
        var s = 0f
        while (s <= len) {
            val cx = x0 + (x1 - x0) * s / len
            val cy = y0 + (y1 - y0) * s / len
            val r = radius.toInt() + 1
            for (j in -r..r) for (i in -r..r) {
                val px = cx.toInt() + i
                val py = cy.toInt() + j
                if (px !in 0 until w || py !in 0 until h) continue
                val d = hypot(px - cx, py - cy) / radius
                if (d < 1f) {
                    val k = py * w + px
                    height[k] = min(height[k], -depth * (1f - d * d))
                }
            }
            s += 0.5f
        }
    }
    repeat(45) {
        val x = rnd.nextFloat() * w
        val y = rnd.nextFloat() * h
        val a = (rnd.nextFloat() - 0.5f) * 0.6f + if (rnd.nextBoolean()) 0f else PI.toFloat() / 2
        val l = 40f + rnd.nextFloat() * 260f
        groove(x, y, x + cos(a) * l, y + sin(a) * l, 1.4f, 0.5f + rnd.nextFloat() * 0.5f)
    }
    repeat(3) {
        var x = rnd.nextFloat() * w
        var y = rnd.nextFloat() * h
        var a = rnd.nextFloat() * 2f * PI.toFloat()
        repeat(60) {
            a += (rnd.nextFloat() - 0.5f) * 1.1f
            val nx = x + cos(a) * 9f
            val ny = y + sin(a) * 9f
            groove(x, y, nx, ny, 2.6f, 1.6f)
            if (rnd.nextFloat() < 0.06f) groove(x, y, x + cos(a + 1.2f) * 40f, y + sin(a + 1.2f) * 40f, 1.6f, 1.0f)
            x = nx
            y = ny
        }
    }
    // height → normal (central differences), packed as rgb
    val pixels = IntArray(w * h)
    val strength = 2.2f
    for (y in 0 until h) for (x in 0 until w) {
        val l = height[y * w + max(x - 1, 0)]
        val r = height[y * w + min(x + 1, w - 1)]
        val u = height[max(y - 1, 0) * w + x]
        val d = height[min(y + 1, h - 1) * w + x]
        var nx = (l - r) * strength
        var ny = (u - d) * strength
        var nz = 1f
        val inv = 1f / sqrt(nx * nx + ny * ny + nz * nz)
        nx *= inv; ny *= inv; nz *= inv
        val rr = ((nx * 0.5f + 0.5f) * 255f).toInt()
        val gg = ((ny * 0.5f + 0.5f) * 255f).toInt()
        val bb = ((nz * 0.5f + 0.5f) * 255f).toInt()
        pixels[y * w + x] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}
