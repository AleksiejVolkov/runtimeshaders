package com.offmind.runtimeshaders.screens.effects.speedometer

import org.intellij.lang.annotations.Language

// The speed fill: a band along the rim, from the 0 mark to the (lagging) speed.
// Smoke towards the inner edge, which is torn; the outer edge stays clean.
@Language("AGSL")
internal val speedFillShader = """
    uniform float2 center;
    uniform float radius;      // px, the rim
    uniform float startAngle;  // radians, where 0 km/h is; angles grow clockwise on screen
    uniform float sweep;       // radians from 0 to 240 km/h
    uniform float fill;        // 0..1 of the sweep
    uniform float time;        // s

    const float TAU = 6.2831853;
    const float MIDDLE = 0.875; // the radius where one noise cell along the arc is measured
    const float width = 0.3;    // of the band of smoke, in rims
    const float stretch = 2.0;  // wisps drawn out along the arc
    const float tear = 1.7;     // depth of the torn inner edge, as a share of the width
    const float3 COLOR = float3(0.023, 0.044, 0.745); // #2A3BE0 in linear light

    float hash(float2 p) {
        p = fract(p * float2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(
            mix(hash(i), hash(i + float2(1.0, 0.0)), u.x),
            mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x),
            u.y
        );
    }

    float fbm(float2 p) {
        float value = 0.0;
        float amplitude = 0.5;
        for (int i = 0; i < 4; i++) {
            value += amplitude * noise(p);
            p = p * 2.03 + float2(17.1, 3.7);
            amplitude *= 0.5;
        }
        return value;
    }

    half4 main(float2 xy) {
        float2 d = xy - center;
        float r = length(d) / radius;
        if (r > 1.02 || r < 1.0 - width * 2.0) return half4(0.0);

        // how far along the arc this pixel is, 0 at the 0 km/h mark
        float along = mod(atan(d.y, d.x) - startAngle + TAU, TAU);
        if (along > sweep) return half4(0.0);
        float lead = fill * sweep;
        // a short soft front, about 7 degrees around the fill value; it fades out at the very
        // start of the scale, so at rest nothing is left on the dial
        float front = smoothstep(lead + 0.06, lead - 0.06, along);
        front *= smoothstep(0.0, 0.02, fill);
        if (front <= 0.0) return half4(0.0);

        // both coordinates in rims, so one frequency means round wisps
        float2 p = float2(along * MIDDLE / stretch * 6.0 - time * 1.6, r * 6.0);
        float coarse = fbm(p);
        float fine = fbm(p * 2.6 + 5.3);

        // the inner edge is torn by the smoke, the outer one is clean
        float edge = 1.0 - width * (1.0 - (coarse - 0.5) * tear - (fine - 0.5) * 0.2);
        float band = smoothstep(edge - 0.01, edge + 0.05, r);
        float rim = smoothstep(1.005, 0.985, r);
        float strength = band * rim * front;

        // light falls off from the rim inwards
        float depth = 1.0 - r;
        // the smoke leaves dark gaps in the body
        float wisps = smoothstep(0.30, 0.62, coarse);
        float density = wisps * mix(0.35, 1.6, smoothstep(0.25, 0.7, fine));
        density += pow(fine, 5.0) * 2.0;
        float body = exp(-depth * 2.2) * density;
        float3 linear = min(COLOR * body * strength, float3(1.0));
        float3 rgb = pow(linear, float3(1.0 / 2.2)); // the shader writes sRGB-encoded values
        // the alpha only says how much light there is
        return half4(rgb, clamp(max(rgb.r, max(rgb.g, rgb.b)), 0.0, 1.0));
    }
"""
