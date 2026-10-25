package com.offmind.runtimeshaders.screens.effects.composeengine.bulb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.offmind.runtimeshaders.screens.effects.composeengine.EngineChoice
import com.offmind.runtimeshaders.screens.effects.composeengine.PhaseCounter
import com.offmind.runtimeshaders.screens.effects.composeengine.RecordPhases
import com.offmind.runtimeshaders.screens.effects.composeengine.hero.ClimateKnob
import com.offmind.runtimeshaders.screens.effects.composeengine.hero.KnobRead
import com.offmind.runtimeshaders.screens.effects.composeengine.hero.climateKnobShader
import com.offmind.runtimeshaders.screens.effects.composeengine.newRuntimeShader
import com.offmind.runtimeshaders.screens.effects.composeengine.recordAllocations
import com.offmind.runtimeshaders.screens.effects.composeengine.rememberFrameSummary

internal enum class HaloMode(val label: String) { Shader("Halo: AGSL"), Stack("Halo: stack"), None("Halo: none") }

private val HaloBlur = 60.dp

/** The dimmer: the steel knob on a dark pod, lit from above by the bulb. */
private val DimmerShader = climateKnobShader
    .replace("const vec3 BACKGROUND = vec3(0.918, 0.918, 0.928);", "const vec3 BACKGROUND = vec3(0.085, 0.070, 0.085);")
    .replace("vec3 platform = vec3(0.94, 0.94, 0.95)", "vec3 platform = vec3(0.20, 0.18, 0.20)")
    .replace(
        "vec3 ink = mix(vec3(0.66, 0.66, 0.68), vec3(0.16, 0.16, 0.18), active);",
        "vec3 ink = mix(vec3(0.32, 0.29, 0.30), vec3(1.0, 0.72, 0.40), active);",
    )
    // the knob catches the bulb's light: warmer and brighter with power, most on the side facing up
    .replace(
        "return half4(col, 1.0);",
        """float fromBulb = knobValue * (0.55 + 0.45 * smoothstep(0.0, 1.0, -p.y));
    col *= mix(vec3(1.0), vec3(1.40, 1.06, 0.76), fromBulb);
    col += vec3(1.0, 0.62, 0.30) * knobValue * 0.12 * knobMask * smoothstep(0.2, 1.0, -p.y);
    return half4(col, 1.0);""",
    )
private val LightFromBulb = floatArrayOf(0f, -0.80f, 0.60f)

@Composable
internal fun BulbHeroScreen() {
    var halo by rememberSaveable { mutableStateOf(HaloMode.Shader) }
    var knobRead by rememberSaveable { mutableStateOf(KnobRead.Draw) }
    var sweep by rememberSaveable { mutableStateOf(false) }
    var lab by rememberSaveable { mutableStateOf(false) }
    var capture by rememberSaveable { mutableStateOf(false) }

    val power = remember { mutableFloatStateOf(0f) }
    val knobCounter = remember { PhaseCounter("dimmer") }
    val time = rememberInfiniteTransition(label = "time")
        .animateFloat(0f, 1_000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")

    // auto-sweep: power 0 → 1 → 0, for measuring and recording
    val sweepValue = rememberInfiniteTransition(label = "sweep")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    if (sweep && !capture) LaunchedEffect(Unit) { while (true) withFrameNanos { power.floatValue = sweepValue.value } }
    val captureTime = rememberCaptureClock(capture, power)

    val variant = "halo=${halo.name}/knob=${knobRead.name}/sweep=$sweep"
    RecordPhases("hero-bulb", variant, listOf(knobCounter))
    rememberFrameSummary("screen=hero-bulb variant=$variant")
    recordAllocations("hero-bulb", variant)

    Box(Modifier.fillMaxSize().background(Color(0xFF120E14))) {
        BulbScene(power, if (capture) captureTime else time, halo, Modifier.fillMaxSize())

        ClimateKnob(
            power, knobRead, knobCounter,
            modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 12.dp),
            radius = 56.dp,
            shaderSource = DimmerShader,
            lightDir = LightFromBulb,
        )

        Text(
            "⋯",
            color = if (capture) Color.Transparent else Color(0xFF8A8090),
            fontSize = 22.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(8.dp)
                .clip(CircleShape)
                .clickable { lab = !lab }
                .padding(horizontal = 14.dp, vertical = 4.dp),
        )
        if (lab) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEE111318))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EngineChoice("Halo", HaloMode.entries, halo, { it.label }) { halo = it }
                EngineChoice("Knob angle", KnobRead.entries, knobRead, { it.label }) { knobRead = it }
                EngineChoice("Sweep", listOf(false, true), sweep, { if (it) "Sweep: on" else "Sweep: off" }) { sweep = it }
                EngineChoice("Capture", listOf(false, true), capture, { if (it) "Capture (adb)" else "Live" }) { capture = it; lab = false }
                Text("close", color = Color(0xFF8A90A3), fontSize = 12.sp, modifier = Modifier.clickable { lab = false })
            }
        }
    }
}

/**
 * The background, drawn by one shader; on its layer, one RenderEffect: the bulb (reads the
 * background and bends it), and in the shader halo mode, the bright parts of the result blurred
 * with a radius that grows away from the bulb and added back. In the stack mode the halo is a few
 * copies of the recorded scene instead, each with one platform blur, a brightness matrix and a
 * ring mask.
 */
@Composable
private fun BulbScene(power: State<Float>, time: State<Float>, halo: HaloMode, modifier: Modifier) {
    val background = remember { newRuntimeShader(BACKGROUND_SHADER) }
    val backgroundBrush = remember(background) { ShaderBrush(background) }
    val bulb = remember { newRuntimeShader(BULB_SHADER) }
    val bright = remember { newRuntimeShader(BRIGHT_SHADER) }
    val haloH = remember { newRuntimeShader(haloPassSource(vertical = false)) }
    val haloV = remember { newRuntimeShader(haloPassSource(vertical = true)) }
    val scene = rememberGraphicsLayer()

    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    scene.record { this@drawWithContent.drawContent() }
                    drawLayer(scene)
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val centre = Offset(size.width * BulbGeometry.CENTER_X, size.height * BulbGeometry.CENTER_Y)
                        val radius = size.width * BulbGeometry.RADIUS
                        bulb.setFloatUniform("centre", centre.x, centre.y)
                        bulb.setFloatUniform("radius", radius)
                        bulb.setFloatUniform("power", power.value)        // read here: no recomposition
                        bulb.setFloatUniform("time", time.value)
                        val lit = RenderEffect.createRuntimeShaderEffect(bulb, "content")
                        renderEffect = if (halo == HaloMode.Shader) {
                            bright.setFloatUniform("power", power.value)
                            for (s in listOf(haloH, haloV)) {
                                s.setFloatUniform("centre", centre.x, centre.y)
                                s.setFloatUniform("radius", radius)
                                s.setFloatUniform("maxBlur", HaloBlur.toPx())
                            }
                            val glow = RenderEffect.createChainEffect(
                                RenderEffect.createRuntimeShaderEffect(haloV, "content"),
                                RenderEffect.createChainEffect(
                                    RenderEffect.createRuntimeShaderEffect(haloH, "content"),
                                    RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(bright, "content"), lit),
                                ),
                            )
                            RenderEffect.createBlendModeEffect(lit, glow, android.graphics.BlendMode.PLUS)
                        } else {
                            lit
                        }.asComposeRenderEffect()
                    }
                    .drawBehind {
                        background.setFloatUniform("size", size.width, size.height)
                        background.setFloatUniform("time", time.value)
                        drawRect(backgroundBrush)
                    },
            )
        }
        if (halo == HaloMode.Stack) HaloStack(scene, power)
    }
}

/**
 * The halo without a custom shader: copies of the recorded scene, each blurred with one platform
 * radius, passed through a colour matrix that keeps only the bright light, masked to a ring around
 * the bulb (wider blur further out) and added on top.
 */
@Composable
private fun HaloStack(scene: GraphicsLayer, power: State<Float>) {
    val steps = 5
    for (i in 1..steps) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    blendMode = BlendMode.Plus
                    alpha = power.value
                }
                .drawWithCache {
                    val centre = Offset(size.width * BulbGeometry.CENTER_X, size.height * BulbGeometry.CENTER_Y)
                    val radius = size.width * BulbGeometry.RADIUS
                    // the ring this copy owns: from 0.15 R to 1.6 R in equal steps, cross-faded
                    val inner = 0.15f + (i - 1) * (1.45f / steps)
                    val outer = inner + 1.45f / steps
                    val maxR = radius * 2.2f
                    val mask = Brush.radialGradient(
                        ((inner - 1.45f / steps) * radius / maxR).coerceIn(0f, 1f) to Color.Transparent,
                        (inner * radius / maxR).coerceIn(0f, 1f) to Color.Black,
                        (outer * radius / maxR).coerceIn(0f, 1f) to Color.Black,
                        ((outer + 1.45f / steps) * radius / maxR).coerceIn(0f, 1f) to Color.Transparent,
                        center = centre, radius = maxR,
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(mask, blendMode = BlendMode.DstIn)
                    }
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val r = HaloBlur.toPx() * i / steps
                        // keep the bright light: rgb' = 2.2 · rgb − 1.2 (clamped)
                        val m = ColorMatrix(
                            floatArrayOf(
                                2.2f, 0f, 0f, 0f, -1.2f * 255f,
                                0f, 2.2f, 0f, 0f, -1.2f * 255f,
                                0f, 0f, 2.2f, 0f, -1.2f * 255f,
                                0f, 0f, 0f, 1f, 0f,
                            ),
                        )
                        renderEffect = RenderEffect.createChainEffect(
                            RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP),
                            RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(m)),
                        ).asComposeRenderEffect()
                    }
                    .drawBehind { drawLayer(scene) },
            )
        }
    }
}

/**
 * For frame-exact screen captures: time and power as a pure function of a frame number that a shell
 * script sends: `am broadcast -a com.offmind.runtimeshaders.ENGINE_FRAME --ei frame N`. One frame =
 * 1/60 s: a second off, the knob turned up over four seconds, then on. The capture is a smooth 60 fps
 * clip whatever the device's speed.
 */
@Composable
private fun rememberCaptureClock(enabled: Boolean, power: androidx.compose.runtime.MutableFloatState): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        fun at(frame: Int) {
            val seconds = frame / 60f
            time.floatValue = 3f + seconds
            power.floatValue = FastOutSlowInEasing.transform(((seconds - 1f) / 4f).coerceIn(0f, 1f))
        }
        at(0)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = at(intent.getIntExtra("frame", 0))
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter("com.offmind.runtimeshaders.ENGINE_FRAME"), ContextCompat.RECEIVER_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return time
}
