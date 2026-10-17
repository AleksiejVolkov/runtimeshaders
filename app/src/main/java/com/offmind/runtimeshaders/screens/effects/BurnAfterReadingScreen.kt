package com.offmind.runtimeshaders.screens.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.offmind.runtimeshaders.R
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

// "Burn After Reading" tutorial: the note burns away from the long-press point.

@Composable
fun BurnAfterReadingScreen() {
    val shader = remember { RuntimeShader(burnShader) }
    val progress = remember { Animatable(0f) }
    var touch by remember { mutableStateOf(Offset.Zero) }
    var burnt by remember { mutableStateOf(false) }
    val time = rememberShaderTime()
    val scope = rememberCoroutineScope()

    // smoke is a separate full-screen layer: the note's render effect is clipped by its bounds,
    // and the smoke has to rise above the note
    val smokeShader = remember { RuntimeShader(burnSmokeShader) }
    val smokeBrush = remember { ShaderBrush(smokeShader) }
    val smokeFade = remember { Animatable(0f) }
    var cardOrigin by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(Size.Zero) }
    val smokeVisible by remember { derivedStateOf { progress.value > 0f && smokeFade.value < 1f } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0B1E)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.burn_after_read_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (!burnt) {
            SecretNote(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        cardOrigin = coords.positionInParent()
                        cardSize = coords.size.toSize()
                    }
                    .onSizeChanged { size ->
                        shader.setFloatUniform(
                            "resolution",
                            size.width.toFloat(),
                            size.height.toFloat()
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                if (!progress.isRunning) {
                                    touch = offset // <-- local to the note, in px
                                    scope.launch {
                                        progress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(durationMillis = 2600, easing = EaseIn)
                                        )
                                        burnt = true
                                        smokeFade.animateTo(1f, tween(durationMillis = 1200))
                                    }
                                }
                            }
                        )
                    }
                    .graphicsLayer {
                        shader.setFloatUniform("touch", touch.x, touch.y)
                        shader.setFloatUniform("progress", progress.value)
                        shader.setFloatUniform("time", time.value)
                        renderEffect = RenderEffect
                            .createRuntimeShaderEffect(shader, "image")
                            .asComposeRenderEffect()
                    }
            )
        }

        if (smokeVisible) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        smokeShader.setFloatUniform("cardOrigin", cardOrigin.x, cardOrigin.y)
                        smokeShader.setFloatUniform("cardSize", cardSize.width, cardSize.height)
                        smokeShader.setFloatUniform("touch", touch.x, touch.y)
                        // keeps going a bit after the note is gone, so the smoke detaches and drifts away
                        smokeShader.setFloatUniform("progress", progress.value + smokeFade.value * 0.5f)
                        smokeShader.setFloatUniform("fade", smokeFade.value)
                        smokeShader.setFloatUniform("time", time.value)
                        drawRect(smokeBrush)
                    }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        ) {
            if (burnt) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            progress.snapTo(0f)
                            smokeFade.snapTo(0f)
                            burnt = false
                        }
                    }
                ) {
                    Text("Write it again", color = Color.White)
                }
            } else {
                Text(
                    text = "Hold the note to burn it",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SecretNote(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(320.dp)
            .background(Color(0xFFF2E6CF), RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("SECRET NOTE", fontSize = 11.sp, letterSpacing = 2.sp, color = Faded)
            Text("23:47", fontSize = 11.sp, color = Faded)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Meet me at the old lighthouse at midnight. Come alone.",
            fontFamily = Caveat,
            fontSize = 27.sp,
            color = Ink
        )
        Text(
            text = "— A.",
            modifier = Modifier.align(Alignment.End),
            fontFamily = Caveat,
            fontSize = 24.sp,
            color = Ink
        )
        Text("hold to burn", fontSize = 10.sp, color = Faded)
    }
}

private val Ink = Color(0xFF2A2018)
private val Faded = Ink.copy(alpha = 0.55f)
private val Caveat = FontFamily(Font(R.font.caveat_bold))

@Composable
private fun rememberShaderTime(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                time.floatValue = (frameTime - start) / 1_000_000_000f
            }
        }
    }
    return time
}

@Language("AGSL")
private val burnShader = """
    uniform shader image;
    uniform float2 resolution;
    uniform float2 touch;
    uniform float progress;
    uniform float time;

    const float PI = 3.14159265;
    const float NOISE_AMP = 0.45;
    const float EMBER_WIDTH = 0.03;
    const float CHAR_WIDTH = 0.16;

    float Hash21(vec2 p) {
        return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
    }

    float ValueNoise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        float a = Hash21(i);
        float b = Hash21(i + vec2(1.0, 0.0));
        float c = Hash21(i + vec2(0.0, 1.0));
        float d = Hash21(i + vec2(1.0, 1.0));
        vec2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }

    float Fbm(vec2 p) {
        float sum = 0.0;
        float amp = 0.5;
        for (int i = 0; i < 5; i++) {
            sum += ValueNoise(p) * amp;
            p *= 2.0;
            amp *= 0.5;
        }
        return sum;
    }

    vec3 HSVtoRGB(vec3 c) {
        vec3 p = abs(fract(c.xxx + vec3(0.0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);
        return c.z * mix(vec3(1.0), clamp(p - 1.0, 0.0, 1.0), c.y);
    }

    vec4 main(float2 fragCoord) {
        float ratio = resolution.x / resolution.y;
        vec2 uv = fragCoord / resolution - 0.5;
        uv.x *= ratio;
        vec2 t = touch / resolution - 0.5;
        t.x *= ratio;
        vec2 halfSize = vec2(0.5 * ratio, 0.5);

        // burn field: distance to the touch, deformed by noise
        float field = length(uv - t) + Fbm(uv * 4.0) * NOISE_AMP;
        float maxReach = length(abs(t) + halfSize) + NOISE_AMP;
        float front = mix(-CHAR_WIDTH, maxReach, progress);
        float d = field - front;

        // heat shimmer
        float heat = exp(-d * d * 60.0) * sin(progress * PI);
        vec2 wobble = vec2(
            ValueNoise(uv * 10.0 + vec2(0.0, time * 3.0)),
            ValueNoise(uv * 10.0 + vec2(5.2, time * 3.0))) - 0.5;
        vec4 paper = image.eval(fragCoord + wobble * heat * 12.0);

        // hole, ember and char
        float alpha   = smoothstep(0.0, 0.006, d);
        float ember   = 1.0 - smoothstep(0.0, EMBER_WIDTH, d);
        float charred = 1.0 - smoothstep(0.0, CHAR_WIDTH, d);

        vec3 charColor = mix(vec3(0.55, 0.33, 0.14), vec3(0.05, 0.03, 0.02), charred);
        vec3 col = mix(paper.rgb, charColor * paper.a, charred);

        float flicker = 0.7 + 0.6 * ValueNoise(uv * 25.0 + vec2(0.0, time * 4.0));
        vec3 emberColor = HSVtoRGB(vec3(0.07, 0.9, 2.2)) * flicker;
        col += emberColor * ember * paper.a;

        vec4 result = vec4(clamp(col, 0.0, 1.0), paper.a) * alpha;

        // halo and sparks inside the hole
        float fadeOut = 1.0 - smoothstep(0.85, 1.0, progress);
        vec2 toBorder = halfSize - abs(uv);
        float borderFade = smoothstep(0.0, 0.08, min(toBorder.x, toBorder.y));
        float halo = exp(-d * d * 400.0) * (1.0 - alpha) * paper.a * borderFade * fadeOut;
        result += vec4(HSVtoRGB(vec3(0.05, 1.0, 1.0)), 1.0) * halo * 0.55;

        vec2 sp = uv * 14.0 + vec2(0.0, time * 1.2);
        vec2 cell = floor(sp);
        vec2 jitter = vec2(Hash21(cell + 1.7), Hash21(cell + 9.3)) - 0.5;
        vec2 q = fract(sp) - 0.5 - jitter * 0.6;
        float spark = exp(-dot(q, q) * 120.0) * step(0.75, Hash21(cell));
        float sparkZone = (1.0 - smoothstep(0.0, 0.25, -d)) * (1.0 - alpha) * paper.a * fadeOut;
        result += vec4(1.0, 0.75, 0.4, 1.0) * spark * sparkZone;

        return clamp(result, 0.0, 1.0);
    }
""".trimIndent()

@Language("AGSL")
private val burnSmokeShader = """
    uniform float2 cardOrigin;   // note position inside the screen box, px
    uniform float2 cardSize;     // note size, px
    uniform float2 touch;        // long-press point, local to the note, px
    uniform float progress;      // burn progress, the same as for the note
    uniform float fade;          // 0..1, fades the smoke out after the note is gone
    uniform float time;

    const float NOISE_AMP = 0.45;
    const float CHAR_WIDTH = 0.16;
    const int PUFFS = 8;
    const float RISE = 0.9;      // how high the oldest smoke gets, in note heights
    const float LAG = 0.2;       // how much older the oldest smoke is, in progress units

    float Hash21(vec2 p) {
        return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
    }

    float ValueNoise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        float a = Hash21(i);
        float b = Hash21(i + vec2(1.0, 0.0));
        float c = Hash21(i + vec2(0.0, 1.0));
        float d = Hash21(i + vec2(1.0, 1.0));
        vec2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }

    // 3 octaves are enough for smoke, it is blurry anyway
    float SmokeFbm(vec2 p) {
        float sum = 0.0;
        float amp = 0.5;
        for (int i = 0; i < 3; i++) {
            sum += ValueNoise(p) * amp;
            p *= 2.0;
            amp *= 0.5;
        }
        return sum;
    }

    // the same burn front as in the note shader, for any point of the note at any progress
    float BurnDistance(vec2 notePx, float prog) {
        float ratio = cardSize.x / cardSize.y;
        vec2 uv = notePx / cardSize - 0.5;
        uv.x *= ratio;
        vec2 t = touch / cardSize - 0.5;
        t.x *= ratio;
        float field = length(uv - t) + SmokeFbm(uv * 4.0) * NOISE_AMP;
        float maxReach = length(abs(t) + vec2(0.5 * ratio, 0.5)) + NOISE_AMP;
        return field - mix(-CHAR_WIDTH, maxReach, prog);
    }

    vec4 main(float2 fragCoord) {
        vec2 local = fragCoord - cardOrigin;
        float h = cardSize.y;

        // smoke lives only around and above the note, skip everything else
        if (local.x < -0.4 * h || local.x > cardSize.x + 0.4 * h ||
            local.y < -1.4 * h || local.y > cardSize.y) {
            return vec4(0.0);
        }

        vec2 p = local / h;   // 1.0 = note height
        float jitter = ValueNoise(p * 9.0 + vec2(0.0, time * 2.0));   // hides the steps between puffs

        float density = 0.0;
        float warmth = 0.0;
        for (int i = 0; i < PUFFS; i++) {
            float age = (float(i) + jitter) / float(PUFFS);   // 0 = just born, 1 = the oldest
            float sway = ValueNoise(vec2(p.y * 2.5 - time * 0.8, float(i) * 3.7)) - 0.5;
            float drift = sway * 0.35 * age;                  // smoke sways more while it rises
            vec2 src = (p + vec2(drift, age * RISE)) * h;    // where this puff came from, px
            if (src.x < 0.0 || src.y < 0.0 || src.x > cardSize.x || src.y > cardSize.y) continue;
            // soft edges of the note, otherwise the smoke gets cut by straight lines
            float edge = min(min(src.x, cardSize.x - src.x), min(src.y, cardSize.y - src.y));
            float inside = smoothstep(0.0, 0.15 * h, edge);

            // older smoke was emitted by an older, smaller front
            float d = BurnDistance(src, max(progress - age * LAG, 0.0));
            float width = mix(400.0, 60.0, age);              // smoke spreads while it rises
            float puff = exp(-d * d * width) * smoothstep(0.0, 0.2, age) * (1.0 - age) * inside;
            density += puff;
            warmth += puff * (1.0 - smoothstep(0.0, 0.45, age));
        }

        // break it into wisps that crawl up
        float wisps = smoothstep(0.25, 0.75, SmokeFbm(p * 4.0 + vec2(time * 0.15, time * 0.6)));
        float alpha = (1.0 - exp(-density * 1.4)) * wisps * 0.45 * (1.0 - fade);

        // young smoke right above the fire is lit by the ember
        float warm = clamp(warmth / max(density, 0.001), 0.0, 1.0);
        vec3 color = mix(vec3(0.42, 0.42, 0.45), vec3(0.95, 0.55, 0.28), warm * 0.6);
        return vec4(color, 1.0) * alpha;
    }
""".trimIndent()
