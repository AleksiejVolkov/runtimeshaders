package com.offmind.runtimeshaders.screens.effects.speedometer

import org.intellij.lang.annotations.Language

// The speed fill: a band along the rim, from the 0 mark to the (lagging) speed. Hot and almost
// white at the rim, coloured further in, and smoke towards the inner edge, which is torn. The
// smoke is fbm in polar coordinates, (angle, radius), so it runs along the arc.
@Language("AGSL")
internal val speedFillShader = """
    uniform float2 center;
    uniform float radius;      // px, the rim
    uniform float inner;       // inner edge of the band, in rims (0.75)
    uniform float startAngle;  // radians, where 0 km/h is; angles grow clockwise on screen
    uniform float sweep;       // radians from 0 to 240 km/h
    uniform float fill;        // 0..1 of the sweep, already smoothed
    uniform float hue;         // 0..1
    uniform float time;        // s

    const float TAU = 6.2831853;

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

    float3 hsv(float h, float s, float v) {
        float3 k = clamp(abs(fract(h + float3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
        return v * mix(float3(1.0), k, s);
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

        // smoke in polar coordinates, flowing forward along the arc
        float coarse = fbm(float2(along * 5.0 - time * 1.3, r * 7.0));
        float fine = fbm(float2(along * 13.0 - time * 2.2, r * 16.0) + 5.3);

        // the inner edge is torn by the smoke, the outer one is clean
        float edge = inner + (coarse - 0.5) * 0.16 + (fine - 0.5) * 0.06;
        float body = smoothstep(edge - 0.01, edge + 0.08, r);
        float rim = smoothstep(1.005, 0.985, r);

        // heat falls off exponentially from the rim inwards
        float depth = 1.0 - r;
        float core = exp(-depth * 22.0);
        float glow = exp(-depth * 4.5);
        // clumps of smoke with brighter flecks in them
        float smoke = mix(0.3, 1.35, coarse) * mix(0.6, 1.4, fine) + pow(fine, 6.0) * 2.5;

        float3 colour = hsv(hue, 0.9, 1.0);
        float3 light = mix(colour * glow * smoke, float3(1.0), core * 0.85);
        float strength = body * rim * front;
        float3 rgb = light * strength;
        // additive: the alpha only says how much light there is
        return half4(rgb, clamp(max(rgb.r, max(rgb.g, rgb.b)), 0.0, 1.0));
    }
"""
