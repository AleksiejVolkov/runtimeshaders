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
    uniform float exposure;    // grows with speed: dim at low speed, overexposed near the top
    uniform float model;       // 0: paint (HSV, mix with white), 1: light (OKLCH, linear, tonemapped)

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

        float strength = body * rim * front;
        float3 rgb;
        if (model < 0.5) {
            // paint: a colour, and white mixed in near the rim
            float3 colour = hsv(hue, 0.9, 1.0);
            rgb = mix(colour * glow * smoke, float3(1.0), core * 0.85) * strength * exposure;
        } else {
            // light: a colour of constant lightness, times an intensity far above 1 at the rim;
            // the tonemap lets the strong channel saturate and pulls the others up to white
            float3 colour = oklchToLinear(0.55, 0.2, hue);
            float hot = exp(-depth * 12.0);
            float intensity = smoke * (14.0 * hot + 1.2 * glow) * strength * exposure;
            float3 linear = 1.0 - exp(-colour * intensity);
            rgb = pow(linear, float3(1.0 / 2.2)); // the shader writes sRGB-encoded values
        }
        // additive: the alpha only says how much light there is
        return half4(rgb, clamp(max(rgb.r, max(rgb.g, rgb.b)), 0.0, 1.0));
    }
"""

// Past the redline the whole dial splits: red is sampled a little further out from the centre,
// blue a little further in, more towards the rim.
@Language("AGSL")
internal val aberrationShader = """
    uniform shader image;
    uniform float2 center;
    uniform float amount;      // fraction of the distance from the centre

    half4 main(float2 xy) {
        float2 shift = (xy - center) * amount;
        half4 c = image.eval(xy);
        half4 r = image.eval(xy - shift);
        half4 b = image.eval(xy + shift);
        return half4(r.r, c.g, b.b, max(c.a, max(r.a, b.a)));
    }
"""
