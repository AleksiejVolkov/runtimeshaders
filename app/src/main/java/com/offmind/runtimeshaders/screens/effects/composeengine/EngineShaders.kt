package com.offmind.runtimeshaders.screens.effects.composeengine

import android.graphics.RuntimeShader
import android.os.Trace
import org.intellij.lang.annotations.Language

// The demos call android.graphics.RuntimeShader directly instead of the project's Shader wrapper:
// the demos show the platform API exactly as an app (or an agent) writes it.

/** A soft highlight running across a dark button. `time` goes 0 → 1 per sweep. */
@Language("AGSL")
internal const val GLOW_SHADER = """
uniform float2 size;
uniform float time;

half4 main(float2 p) {
    float2 uv = p / size;
    half3 base = mix(half3(0.17, 0.19, 0.27), half3(0.09, 0.10, 0.16), half(uv.y));
    float band = uv.x - (time * 1.8 - 0.4) - (uv.y - 0.5) * 0.35;
    float glow = exp(-band * band * 40.0);
    half3 tint = half3(0.45, 0.62, 1.0);
    return half4(base + tint * half(glow * 0.85), 1.0);
}
"""

/**
 * The same glow with a different constant, so that it is a different program to every cache. The
 * constant changes the output as a full-precision float, so it cannot fold away into the same code.
 */
internal fun saltedGlowShader(salt: Long): String = GLOW_SHADER.replace(
    "uniform float time;",
    "uniform float time;\nconst float salt = ${salt % 100_000}.0;",
).replace("return half4(base", "return half4(half3(float3(base) * (1.0 - salt * 1e-7))")

/**
 * The glow over a noise field whose cost is set by `octaves` (1…32): area × complexity, the same
 * picture at any size. With [sampleContent] the base color comes from the layer underneath
 * (`uniform shader content`, for RenderEffect); without it the shader paints the base itself.
 */
internal fun areaShaderSource(sampleContent: Boolean): String {
    val contentUniform = if (sampleContent) "uniform shader content;" else ""
    val base = if (sampleContent) {
        "half3 base = content.eval(p).rgb;"
    } else {
        "half3 base = mix(half3(0.17, 0.19, 0.27), half3(0.09, 0.10, 0.16), half(uv.y));"
    }
    return """
$contentUniform
uniform float2 size;
uniform float time;
uniform float octaves;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1, 0)), u.x),
               mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), u.x), u.y);
}

half4 main(float2 p) {
    float2 uv = p / size;
    $base
    float n = 0.0;
    float amp = 0.5;
    float2 q = p / 90.0 + float2(time * 0.6, 0.0);
    for (int i = 0; i < 32; i++) {
        if (float(i) >= octaves) break;
        n += amp * noise(q);
        q = q * 2.03 + float2(7.1, 3.7);
        amp *= 0.5;
    }
    float band = uv.x - (time * 1.8 - 0.4) - (uv.y - 0.5) * 0.35;
    float glow = exp(-band * band * 40.0) * (0.6 + 0.8 * n);
    half3 tint = half3(0.45, 0.62, 1.0);
    return half4(base + tint * half(glow * 0.85), 1.0);
}
"""
}

/** Every shader the demos create goes through here: counted and visible in a Perfetto trace. */
internal fun newRuntimeShader(source: String, counter: PhaseCounter? = null): RuntimeShader {
    Trace.beginSection("ComposeEngine#newRuntimeShader")
    try {
        counter?.let { it.shaders++ }
        return RuntimeShader(source)
    } finally {
        Trace.endSection()
    }
}
