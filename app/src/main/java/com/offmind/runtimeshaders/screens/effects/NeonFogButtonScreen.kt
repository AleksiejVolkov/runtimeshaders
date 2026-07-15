package com.offmind.runtimeshaders.screens.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.composables.ShadedBox
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.ShaderTypedValue
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.shaders.addUniform
import com.offmind.runtimeshaders.shaders.basicUniformList
import com.offmind.runtimeshaders.shaders.removeUniform
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

private val ButtonWidth = 320.dp
private val ButtonHeight = 76.dp
private val ButtonCornerRadius = 28.dp

@Composable
fun NeonFogButtonScreen() {
    val scope = rememberCoroutineScope()
    val pressProgress = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current
    val buttonWidthPx = with(density) { ButtonWidth.toPx() }
    val buttonHeightPx = with(density) { ButtonHeight.toPx() }
    val buttonRadiusPx = with(density) { ButtonCornerRadius.toPx() }
    val shader = remember {
        Shader(neonFogButtonShader).getRuntimeShader(
            uniforms = basicUniformList
                .removeUniform("image")
                .addUniform(Uniform.Type.VEC2 to "buttonSize")
                .addUniform(Uniform.Type.FLOAT to "buttonRadius")
                .addUniform(Uniform.Type.FLOAT to "pressProgress"),
            customFunctions = emptySet()
        )
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            burstProgress.snapTo(1f)
            pressProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
            )
        } else {
            pressProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF06070B),
                        Color(0xFF111520),
                        Color(0xFF150713),
                        Color(0xFF050507)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        ShadedBox(
            modifier = Modifier.fillMaxSize(),
            shader = shader,
            includeTime = true,
            shaderUniforms = mapOf(
                "percentage" to ShaderTypedValue.FloatType(burstProgress.value),
                "pressProgress" to ShaderTypedValue.FloatType(pressProgress.value),
                "buttonSize" to ShaderTypedValue.Vec2Type(buttonWidthPx, buttonHeightPx),
                "buttonRadius" to ShaderTypedValue.FloatType(buttonRadiusPx)
            )
        )

        Button(
            modifier = Modifier
                .width(ButtonWidth)
                .height(ButtonHeight)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(ButtonCornerRadius),
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                ),
            shape = RoundedCornerShape(ButtonCornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF030407),
                contentColor = Color(0xFFE8F6FF)
            ),
            interactionSource = interactionSource,
            onClick = {
                scope.launch {
                    burstProgress.snapTo(0f)
                    burstProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing)
                    )
                }
            }
        ) {
            Text(
                text = "Ignite Fog",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Language("AGSL")
private val neonFogButtonShader = """
    vec2 hash22(vec2 p) {
        float x = dot(p, vec2(127.1, 311.7));
        float y = dot(p, vec2(269.5, 183.3));
        return -1.0 + 2.0 * fract(sin(vec2(x, y)) * 43758.5453);
    }

    float noise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);

        float a = dot(hash22(i + vec2(0.0, 0.0)), f - vec2(0.0, 0.0));
        float b = dot(hash22(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0));
        float c = dot(hash22(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0));
        float d = dot(hash22(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0));

        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y) * 0.5 + 0.5;
    }

    float fbm(vec2 p) {
        float value = 0.0;
        float amp = 0.5;
        mat2 rotate = mat2(0.80, -0.60, 0.60, 0.80);

        for (int i = 0; i < 5; i++) {
            value += amp * noise(p);
            p = rotate * p * 2.03 + vec2(9.7, 3.1);
            amp *= 0.52;
        }

        return value;
    }

    float sdRoundRect(vec2 p, vec2 b, float r) {
        vec2 q = abs(p) - b + vec2(r);
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    vec4 main(float2 fragCoord) {
        float unit = min(resolution.x, resolution.y);
        vec2 p = (fragCoord - resolution * 0.5) / unit;
        vec2 buttonHalf = buttonSize / (unit * 2.0);
        float radius = buttonRadius / unit;
        float d = sdRoundRect(p, buttonHalf, radius);
        float outside = smoothstep(-0.004, 0.012, d);


        vec2 driftA = vec2(time * 0.09, -time * 0.06);
        vec2 driftB = vec2(-time * 0.05, time * 0.11);
        float warp = fbm(p * 3.2 + driftA * 0.7);
        vec2 warpedP = p + vec2(warp - 0.5, fbm(p * 3.8 - driftA) - 0.5) * 0.085;
        float softNoise = fbm(warpedP * 6.4 + driftA);
        float detailNoise = fbm(warpedP * 17.0 + driftB);
        float raggedDistance = d + (softNoise - 0.5) * 0.085 + (detailNoise - 0.5) * 0.035;

        float edgeFog = exp(-max(raggedDistance, 0.0) * 6.4);
        edgeFog *= 1.0 - smoothstep(0.34, 0.66, raggedDistance);
        edgeFog *= outside;

        float wispMap = softNoise * 0.68 + detailNoise * 0.32;
        float wisps = smoothstep(0.36, 0.82, wispMap) * (0.56 + 0.44 * detailNoise);
        float press = clamp(pressProgress, 0.0, 1.0);
        float burn = pow(smoothstep(0.42, 0.92, wispMap), 1.35);
        float sdfFalloff = exp(-max(d, 0.0) * mix(13.5, 4.2, burn));
        float pressShape = sdfFalloff *
            (1.0 - smoothstep(0.46, 0.82, max(d, 0.0))) *
            outside;
        float pressFlicker = 0.28 + sin(time * 2.4) * 0.035;
        float pressFog = pressShape * wisps * press * pressFlicker;

        float pulse = clamp(percentage, 0.0, 1.0);
        float waveCenter = mix(-0.075, 0.58, pulse);
        float waveWidth = mix(0.052, 0.18, pulse);
        float waveDistance = d + (softNoise - 0.5) * 0.12;
        float wave = exp(-pow((waveDistance - waveCenter) / waveWidth, 2.0));
        float releaseReveal = smoothstep(0.05, 0.18, pulse);
        float burstFade = 1.0 - smoothstep(0.38, 1.0, pulse);
        float burstBody = exp(-max(waveDistance, 0.0) * 5.2) *
            smoothstep(0.08, 0.2, pulse) *
            (1.0 - smoothstep(0.0, 0.46, pulse));
        float burstFog = (wave * 1.05 + burstBody * 0.32) *
            releaseReveal *
            burstFade *
            outside *
            (0.72 + 0.78 * detailNoise);

        float edgeFlash = exp(-max(d, 0.0) * 22.0) *
            smoothstep(0.08, 0.18, pulse) *
            (1.0 - smoothstep(0.0, 0.28, pulse)) *
            outside;

        float colorMix = smoothstep(-0.18, 0.35, p.x + sin(time * 0.55 + softNoise * 4.0) * 0.18);
        vec3 cyan = vec3(0.0, 0.9, 1.0);
        vec3 magenta = vec3(1.0, 0.0, 0.34);
        vec3 neon = mix(cyan, magenta, colorMix);

        float glow = pressFog * 0.85 + burstFog * 2.15 + edgeFlash * 0.45;
        vec3 color = neon * glow * 1.65;
        color += neon * pow(max(pressFog + burstFog, 0.0), 2.0) * 1.2;

        float alpha = clamp(pressFog * 0.62 + burstFog * 1.12 + edgeFlash * 0.2, 0.0, 0.88);
        return vec4(color, alpha);
    }
"""
