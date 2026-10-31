package com.offmind.runtimeshaders.screens.effects.speedometer

import org.intellij.lang.annotations.Language

// The speed fill: a band along the rim, from the 0 mark to the (lagging) speed. Hot at the
// rim, coloured further in, and smoke towards the inner edge, which is torn. The smoke is fbm
// in polar coordinates, (angle, radius), so it runs along the arc.
@Language("AGSL")
internal val speedFillShader = """
    uniform float2 center;
    uniform float radius;      // px, the rim
    uniform float inner;       // inner edge of the band, in rims (0.75)
    uniform float startAngle;  // radians, where 0 km/h is; angles grow clockwise on screen
    uniform float sweep;       // radians from 0 to 240 km/h
    uniform float fill;        // 0..1 of the sweep
    uniform float hue;         // 0..1
    uniform float exposure;    // grows with speed: dim at low speed, overexposed near the top
    uniform float model;       // 0: paint, 1: light
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
        float3 k = abs(fract(h + float3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0) - 1.0;
        return v * mix(float3(1.0), clamp(k, 0.0, 1.0), s);
    }

    // OKLCH -> OKLab -> linear sRGB: two matrices and a cube in between
    float3 oklchToLinear(float l, float c, float h) {
        float a = c * cos(h * TAU);
        float b = c * sin(h * TAU);
        float lms1 = l + 0.3963377774 * a + 0.2158037573 * b;
        float lms2 = l - 0.1055613458 * a - 0.0638541728 * b;
        float lms3 = l - 0.0894841775 * a - 1.2914855480 * b;
        float3 lms = float3(lms1 * lms1 * lms1, lms2 * lms2 * lms2, lms3 * lms3 * lms3);
        float3 rgb = float3(
            4.0767416621 * lms.x - 3.3077115913 * lms.y + 0.2309699292 * lms.z,
            -1.2684380046 * lms.x + 2.6097574011 * lms.y - 0.3413193965 * lms.z,
            -0.0041960863 * lms.x - 0.7034186147 * lms.y + 1.7076147010 * lms.z
        );
        return max(rgb, float3(0.0)); // outside sRGB: just clip
    }

    // filmic curve (ACES fit by Narkowicz): a toe that keeps the dark parts deep and a soft
    // shoulder, per channel
    float3 tonemap(float3 x) {
        return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
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
        float band = smoothstep(edge - 0.01, edge + 0.08, r);
        float rim = smoothstep(1.005, 0.985, r);
        float strength = band * rim * front;

        // heat falls off exponentially from the rim inwards
        float depth = 1.0 - r;
        float3 rgb;
        if (model < 0.5) {
            // paint: a colour, and white mixed in near the rim
            float core = exp(-depth * 22.0);
            float glow = exp(-depth * 4.5);
            float3 colour = hsv(hue, 0.9, 1.0);
            float smoke = mix(0.3, 1.35, coarse) * mix(0.6, 1.4, fine) + pow(fine, 6.0) * 2.5;
            rgb = mix(colour * glow * smoke, float3(1.0), core * 0.85) * strength * exposure;
        } else {
            // light: a colour times an intensity that is far above 1 only in a thin band at
            // the rim; the smoke leaves dark gaps in the body. The tonemap lets the strong
            // channel saturate and pulls the others up: white only at the rim, and only at
            // speed.
            float3 colour = oklchToLinear(0.47, 0.19, hue);
            float density = smoothstep(0.28, 0.72, coarse) * mix(0.55, 1.3, fine);
            density += pow(fine, 6.0) * 1.5;
            float rimHeat = exp(-depth * 28.0) * mix(0.75, 1.0, fine);
            float body = exp(-depth * 3.5) * density;
            // the rim heats up faster than the body: white appears only at speed
            float intensity = (30.0 * exposure * rimHeat + 1.8 * body) * strength * exposure;
            // inset (as in AgX): every channel gets a little of the others, the way the eye's
            // sensitivities overlap; without it a colour at the gamut edge has zeros that no
            // intensity can lift, and the core never turns white
            float3 inset = mix(colour, float3((colour.r + colour.g + colour.b) / 3.0), 0.12);
            float3 linear = tonemap(inset * intensity);
            rgb = pow(linear, float3(1.0 / 2.2)); // the shader writes sRGB-encoded values
        }
        // the alpha only says how much light there is
        return half4(rgb, clamp(max(rgb.r, max(rgb.g, rgb.b)), 0.0, 1.0));
    }
"""
