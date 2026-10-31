package com.offmind.runtimeshaders.screens.effects.speedometer

import org.intellij.lang.annotations.Language

// The speed fill: a band along the rim, from the 0 mark to the speed. Hot at the rim and
// coloured further in.
@Language("AGSL")
internal val speedFillShader = """
    uniform float2 center;
    uniform float radius;      // px, the rim
    uniform float inner;       // inner edge of the band, in rims (0.75)
    uniform float startAngle;  // radians, where 0 km/h is; angles grow clockwise on screen
    uniform float sweep;       // radians from 0 to 240 km/h
    uniform float fill;        // 0..1 of the sweep
    uniform float hue;         // 0..1

    const float TAU = 6.2831853;

    float3 hsv(float h, float s, float v) {
        float3 k = abs(fract(h + float3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0) - 1.0;
        return v * mix(float3(1.0), clamp(k, 0.0, 1.0), s);
    }

    half4 main(float2 xy) {
        float2 d = xy - center;
        float r = length(d) / radius;
        if (r > 1.02 || r < inner - 0.15) return half4(0.0);

        // how far along the arc this pixel is, 0 at the 0 km/h mark
        float along = mod(atan(d.y, d.x) - startAngle + TAU, TAU);
        if (along > sweep) return half4(0.0);
        float lead = fill * sweep;
        // a short soft front, about 7 degrees around the fill value
        float front = smoothstep(lead + 0.06, lead - 0.06, along);
        if (front <= 0.0) return half4(0.0);

        // the band between the inner edge and the rim
        float band = smoothstep(inner - 0.01, inner + 0.08, r);
        float rim = smoothstep(1.005, 0.985, r);
        float strength = band * rim * front;

        // heat falls off exponentially from the rim inwards
        float depth = 1.0 - r;
        float core = exp(-depth * 22.0);
        float glow = exp(-depth * 4.5);

        // paint: a colour, and white mixed in near the rim
        float3 colour = hsv(hue, 0.9, 1.0);
        float3 rgb = mix(colour * glow, float3(1.0), core * 0.85) * strength;
        // the alpha only says how much light there is
        return half4(rgb, clamp(max(rgb.r, max(rgb.g, rgb.b)), 0.0, 1.0));
    }
"""
