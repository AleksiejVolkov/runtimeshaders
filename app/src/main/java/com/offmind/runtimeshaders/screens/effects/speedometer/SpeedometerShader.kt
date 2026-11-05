package com.offmind.runtimeshaders.screens.effects.speedometer

import org.intellij.lang.annotations.Language

// The speed fill: a band along the rim, from the 0 mark to the (lagging) speed.
// Smoke towards the inner edge, which is torn; the outer edge stays clean.
@Language("AGSL")
internal val speedFillShader = """
    uniform float2 center;
    uniform float radius;      // px, the rim
    uniform float inner;       // inner edge of the band, in rims (0.7)
    uniform float startAngle;  // radians, where 0 km/h is; angles grow clockwise on screen
    uniform float sweep;       // radians from 0 to 240 km/h
    uniform float fill;        // 0..1 of the sweep
    uniform float3 ramp[25];   // linear light every 10 km/h: each point has its speed's color
    uniform float exposure;    // grows with speed: dim at low speed, overexposed near the top
    uniform float flow;        // accumulated: how far the smoke has moved along the arc
    uniform float stretch;     // >= 1: wisps drawn out along the arc
    uniform float warp;        // how far one noise pushes the other
    uniform float tear;        // depth of the torn inner edge, in rims
    uniform float time;        // s

    const float TAU = 6.2831853;
    const float MIDDLE = 0.875; // the middle of the band, in rims

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

    // two octaves: enough to push the coordinates of another noise
    float fbm2(float2 p) {
        return (noise(p) + 0.5 * noise(p * 2.03 + float2(17.1, 3.7))) / 1.5 * 0.9375;
    }

    // filmic curve (ACES fit by Narkowicz): a toe that keeps the dark parts deep and a soft
    // shoulder, per channel
    float3 tonemap(float3 x) {
        return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
    }

    half4 main(float2 xy) {
        float2 d = xy - center;
        float r = length(d) / radius;
        if (r > 1.02 || r < inner - 0.3) return half4(0.0);

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
        float2 p = float2(along * MIDDLE / stretch * 6.0 - flow, r * 6.0);
        // domain warping: one noise moves the coordinates of the other
        float2 q = float2(fbm2(p + time * 0.15), fbm2(p + float2(5.2, 1.3) - time * 0.12));
        float coarse = fbm(p + warp * q);
        float fine = fbm(p * 2.6 + warp * 2.0 * q + 5.3);

        // the inner edge is torn by the smoke, the outer one is clean
        float edge = inner + (coarse - 0.5) * tear * 3.0 + (fine - 0.5) * 0.06;
        float band = smoothstep(edge - 0.01, edge + 0.05, r);
        float rim = smoothstep(1.005, 0.985, r);
        float strength = band * rim * front;

        // the color of the speed this point of the arc stands for; AGSL can index an array
        // only with a loop counter
        float3 c = ramp[0];
        float x = along / sweep * 24.0;
        for (int i = 0; i < 24; i++) {
            if (x >= float(i)) c = mix(ramp[i], ramp[i + 1], min(x - float(i), 1.0));
        }

        // light falls off from the rim inwards
        float depth = 1.0 - r;
        // the smoke leaves dark gaps in the body
        float wisps = smoothstep(0.30, 0.62, coarse);
        float density = wisps * mix(0.35, 1.6, smoothstep(0.25, 0.7, fine));
        density += pow(fine, 5.0) * 2.0;
        float body = exp(-depth * 2.2) * density;
        // far above 1 only in a thin band at the rim, and only at speed: the smoke is seen at
        // any speed, the rim heats up as the car goes faster
        float rimHeat = exp(-depth * 40.0) * mix(0.75, 1.0, fine);
        float intensity = (11.0 * exposure * exposure * rimHeat
            + 0.95 * (0.4 + 0.3 * exposure) * body) * strength;
        // inset (as in AgX): every channel gets a little of the others, the way the eye's
        // sensitivities overlap; without it a color at the gamut edge has zeros that no
        // intensity can lift, and the rim never turns white
        float3 inset = mix(c, float3((c.r + c.g + c.b) / 3.0), 0.12);
        float3 linear = tonemap(inset * intensity);
        float3 rgb = pow(linear, float3(1.0 / 2.2)); // the shader writes sRGB-encoded values
        // the alpha only says how much light there is
        return half4(rgb, clamp(max(rgb.r, max(rgb.g, rgb.b)), 0.0, 1.0));
    }
"""

// Past the redline the whole dial starts to come apart. One effect on the dial's layer: the
// color channels split and tremble, the split direction wanders between the radius and the
// tangent, short bursts kick in more often towards 240, thin arc bands of the dial slip around
// the centre, the whole dial shakes by a pixel or two and its brightness flickers. k is 0 at
// 200 km/h and 1 at 240.
@Language("AGSL")
internal val breakdownShader = """
    uniform shader image;
    uniform float2 center;
    uniform float radius;      // px, the rim
    uniform float k;           // 0..1
    uniform float time;        // s

    const float TAU = 6.2831853;

    float hash(float n) { return fract(sin(n * 12.9898) * 43758.5453); }

    float noise(float t) {
        float i = floor(t);
        float f = fract(t);
        return mix(hash(i), hash(i + 1.0), f * f * (3.0 - 2.0 * f));
    }

    half4 main(float2 xy) {
        // already clearly on the edge at 215-221 km/h, still growing towards 240
        float e = sqrt(k);
        // time in short slots: a burst or a band lives for one slot
        float slot = floor(time * 14.0);
        float burst = step(1.0 - (0.05 + 0.4 * e * e), hash(slot + 7.1));

        // the whole dial shakes a little
        float shake = 1.2 * e * (1.0 + 1.5 * burst);
        float2 jitter = float2(noise(time * 31.0), noise(time * 29.0 + 40.0)) - 0.5;
        float2 p = xy - jitter * 2.0 * shake;
        float2 d = p - center;
        float r = length(d);

        // thin arcs of the dial, between two radii and two angles, turn a little for a moment
        float2 q = p;
        float inBand = 0.0;
        for (int i = 0; i < 2; i++) {
            float seed = slot * 3.0 + float(i) * 17.0;
            float on = step(1.0 - (0.15 + 0.55 * e) * (0.4 + burst), hash(seed));
            if (on > 0.5) {
                float r0 = mix(0.3, 1.08, hash(seed + 1.0)) * radius;
                float w = mix(0.015, 0.05, hash(seed + 2.0)) * radius;
                float from = hash(seed + 3.0) * TAU;
                float span = mix(0.4, 1.4, hash(seed + 4.0));
                float along = mod(atan(d.y, d.x) - from + TAU, TAU);
                if (abs(r - r0) < w && along < span) {
                    float slip = (hash(seed + 5.0) - 0.5) * 0.12 * e;
                    float c = cos(slip);
                    float s = sin(slip);
                    q = center + float2(c * d.x - s * d.y, s * d.x + c * d.y);
                    inBand = 1.0;
                }
            }
        }

        // the channel split: a tremor and a slow wave; it grows towards the rim
        float2 radial = d / max(r, 1.0);
        float2 tangent = float2(-radial.y, radial.x);
        float phi = 0.8 * sin(time * 1.7) + (noise(time * 23.0) - 0.5) * 1.2;
        float2 dir = radial * cos(phi) + tangent * sin(phi);
        // steady part stays under ~3.7 px, thinner than the strokes of the numbers
        float amount = e * (2.6 + 0.6 * sin(time * 2.3) + (noise(time * 37.0) - 0.5) * 1.0);
        amount += burst * e * 4.0;
        amount *= mix(0.4, 1.0, clamp(r / radius, 0.0, 1.0)) * (1.0 + inBand * 2.0);
        float2 o = dir * amount;

        half4 g = image.eval(q);
        half4 red = image.eval(q + o);
        half4 blue = image.eval(q - o);
        float flicker = 1.0 + e * 0.1 * (noise(time * 19.0) - 0.5) * 2.0 - burst * e * 0.08;
        return half4(half3(red.r, g.g, blue.b) * flicker, max(g.a, max(red.a, blue.a)));
    }
"""
