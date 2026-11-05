package com.offmind.runtimeshaders.screens.effects.speedometer

import org.intellij.lang.annotations.Language

// The speed fill: a band along the rim, from the 0 mark to the (lagging) speed.
@Language("AGSL")
internal val speedFillShader = """
    uniform float2 center;
    uniform float radius;      // px, the rim
    uniform float startAngle;  // radians, where 0 km/h is; angles grow clockwise on screen
    uniform float sweep;       // radians from 0 to 240 km/h
    uniform float fill;        // 0..1 of the sweep

    const float TAU = 6.2831853;
    const float width = 0.3;    // of the band of smoke, in rims
    const float3 COLOR = float3(0.023, 0.044, 0.745); // #2A3BE0 in linear light

    half4 main(float2 xy) {
        float2 d = xy - center;
        float r = length(d) / radius;
        if (r > 1.02 || r < 1.0 - width) return half4(0.0);

        // how far along the arc this pixel is, 0 at the 0 km/h mark
        float along = mod(atan(d.y, d.x) - startAngle + TAU, TAU);
        if (along > sweep) return half4(0.0);
        float lead = fill * sweep;
        // a short soft front, about 7 degrees around the fill value; it fades out at the very
        // start of the scale, so at rest nothing is left on the dial
        float front = smoothstep(lead + 0.06, lead - 0.06, along);
        front *= smoothstep(0.0, 0.02, fill);
        if (front <= 0.0) return half4(0.0);

        // a thin soft inner edge for now, and a clean rim
        float band = smoothstep(1.0 - width, 1.02 - width, r);
        float rim = smoothstep(1.005, 0.985, r);
        float strength = band * rim * front;

        // light falls off from the rim inwards
        float depth = 1.0 - r;
        float3 linear = min(COLOR * exp(-depth * 2.2) * strength, float3(1.0));
        float3 rgb = pow(linear, float3(1.0 / 2.2)); // the shader writes sRGB-encoded values
        // the alpha only says how much light there is
        return half4(rgb, clamp(max(rgb.r, max(rgb.g, rgb.b)), 0.0, 1.0));
    }
"""
