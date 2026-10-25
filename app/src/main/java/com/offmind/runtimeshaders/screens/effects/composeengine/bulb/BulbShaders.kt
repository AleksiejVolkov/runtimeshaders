package com.offmind.runtimeshaders.screens.effects.composeengine.bulb

import org.intellij.lang.annotations.Language

/*
 * The film's hero: a plain glass light bulb over a dark pastel background, dimmed by a knob.
 * Everything is procedural. Geometry is in px of the screen; the bulb is described by [BulbGeometry]
 * fractions of the screen width so the background, the bulb and the halo agree.
 *
 *  - BACKGROUND_SHADER: a dark, warm, pastel gradient of drifting colour fields.
 *  - BULB_SHADER: a RenderEffect over the background layer. It reads the background pixels and
 *    bends them through the glass (Snell's law over a thickness profile), adds Fresnel, highlights,
 *    the support wires, the screw base and the filament, whose glow follows `power`.
 *  - BRIGHT_SHADER + halo passes: the bright parts of the lit scene, blurred with a radius that
 *    grows away from the bulb, added back — the halo.
 */

/** Bulb proportions, as fractions of the screen width (W) and height (H). */
internal object BulbGeometry {
    const val CENTER_X = 0.5f      // × W
    const val CENTER_Y = 0.34f     // × H
    const val RADIUS = 0.40f       // × W: the glass sphere
}

@Language("AGSL")
internal const val BACKGROUND_SHADER = """
uniform float2 size;
uniform float time;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), u.x), mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
}

// a soft colour field drifting on a slow loop
half3 field(float2 uv, float2 centre, float radius, half3 colour) {
    float d = length(uv - centre) / radius;
    return colour * half(exp(-d * d));
}

half4 main(float2 p) {
    float2 uv = p / size.x;                       // width = 1
    float2 still = uv;                            // unwarped, for the particles
    float t = time * 0.05;
    // a gentle warp so the fields never look like circles
    uv += (float2(noise(uv * 2.2 + t), noise(uv * 2.2 - t + 3.1)) - 0.5) * 0.18;
    float aspect = size.y / size.x;

    half3 col = half3(0.075, 0.060, 0.080);                                   // deep plum-black
    col += field(uv, float2(0.25 + 0.06 * sin(t * 1.3), 0.30 * aspect), 0.55, half3(0.30, 0.16, 0.20)); // dusty rose
    col += field(uv, float2(0.85 + 0.05 * cos(t), 0.55 * aspect), 0.60, half3(0.28, 0.17, 0.10));      // amber
    col += field(uv, float2(0.40, 0.95 * aspect + 0.04 * sin(t * 0.7)), 0.70, half3(0.16, 0.12, 0.22)); // plum
    col += field(uv, float2(0.95, 0.10 * aspect), 0.35, half3(0.10, 0.14, 0.13));                      // a little sage
    // breathing: slow silk waves, faint bands of light that the glass will bend
    float wave = sin((uv.y + 0.30 * noise(uv * 1.6 + t * 4.0)) * 34.0 + time * 0.6);
    col += half3(0.040, 0.028, 0.032) * half(smoothstep(0.35, 1.0, wave));
    // drifting particles: soft discs on a jittered grid, rising slowly and twinkling
    for (int layer = 0; layer < 2; layer++) {
        float scale = layer == 0 ? 7.0 : 13.0;
        float2 g = still * scale + float2(0.3 * float(layer), time * (0.025 + 0.02 * float(layer)));
        float2 cell = floor(g);
        float2 f = fract(g) - 0.5;
        float h = hash(cell + float(layer) * 17.0);
        float2 jitter = (float2(hash(cell + 3.1), hash(cell + 7.7)) - 0.5) * 0.5;
        float r = 0.05 + 0.09 * hash(cell + 11.3);
        float disc = smoothstep(r, r * 0.25, length(f - jitter)) * step(0.55, h);
        float twinkle = 0.55 + 0.45 * sin(time * 0.7 + h * 40.0);
        col += half3(0.22, 0.15, 0.13) * half(disc * twinkle * (layer == 0 ? 0.9 : 0.55));
    }
    // fine dither against banding
    col += half3((hash(p + time) - 0.5) / 255.0);
    return half4(col, 1.0);
}
"""

/**
 * The bulb, applied to the background layer: `content` is the background. Lengths in px.
 */
@Language("AGSL")
internal const val BULB_SHADER = """
uniform shader content;
uniform float2 centre;     // the glass sphere's centre
uniform float radius;      // the glass sphere's radius
uniform float power;       // 0 off … 1 full
uniform float time;

const float IOR = 1.5;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }

float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// The glass silhouette: a sphere and a neck narrowing towards the base, joined smoothly.
float glassSd(float2 p) {
    float2 q = p - centre;
    float sphere = length(q) - radius;
    float y0 = radius * 0.45;
    float y1 = radius * 1.38;
    float t = clamp((q.y - y0) / (y1 - y0), 0.0, 1.0);
    float halfWidth = mix(radius * 0.62, radius * 0.38, t * t * (3.0 - 2.0 * t));
    float neck = max(abs(q.x) - halfWidth, max(y0 - q.y, q.y - y1));
    return smin(sphere, neck, radius * 0.28);
}

float smax(float a, float b, float k) { return -smin(-a, -b, k); }

// The glass as a lens: how thick it is at p, 0 … 1 of the radius. A dome over the sphere and a
// round cross-section along the neck, joined smoothly so the bend has no seams.
float domeH(float2 p) {
    float2 q = (p - centre) / radius;
    float sphere = sqrt(max(1.0 - dot(q, q), 0.0));
    float t = clamp((q.y - 0.45) / (1.38 - 0.45), 0.0, 1.0);
    float w = mix(0.62, 0.38, t * t * (3.0 - 2.0 * t));
    float neck = sqrt(max(w * w - q.x * q.x, 0.0)) * smoothstep(1.42, 1.30, q.y);
    // the fillet between them (the silhouette is wider there than either) rises from the outline
    float fillet = sqrt(clamp(-glassSd(p) / (radius * 0.40), 0.0, 1.0)) * 0.55;
    return smax(smax(sphere, neck, 0.25), fillet, 0.15);
}

float2 outward(float2 p) {
    float2 e = float2(1.0, 0.0);
    return normalize(float2(glassSd(p + e.xy) - glassSd(p - e.xy), glassSd(p + e.yx) - glassSd(p - e.yx)) + 1e-5);
}

// The filament: a coiled arc between two wire tips, a little below the sphere's centre.
float filamentDist(float2 p) {
    float2 a = centre + float2(-0.20, 0.10) * radius;
    float2 b = centre + float2(0.20, 0.10) * radius;
    float len = b.x - a.x;
    float t = clamp((p.x - a.x) / len, 0.0, 1.0);
    float mid = mix(a.y, b.y, t) - radius * 0.10 * sin(3.14159 * t);
    float y = mid + radius * 0.016 * sin(t * 6.2831 * 15.0);          // the coil
    // the curve's slope, so the distance stays even where the coil is steep
    float dy = (-radius * 0.10 * 3.14159 * cos(3.14159 * t) + radius * 0.016 * 6.2831 * 15.0 * cos(t * 6.2831 * 15.0)) / len;
    // near the coil: the slope-corrected distance; further away, the distance to the coil's envelope
    float across = max(abs(p.y - y) / sqrt(1.0 + dy * dy), abs(p.y - mid) - radius * 0.016);
    float beyond = max(max(a.x - p.x, p.x - b.x), 0.0);              // past a wire tip
    return length(float2(beyond, across));
}

// Support wires: from the stem top to the filament's ends, and the glass stem itself.
float wiresDist(float2 p) {
    float2 stemTop = centre + float2(0.0, 0.62) * radius;
    float2 a = centre + float2(-0.20, 0.10) * radius;
    float2 b = centre + float2(0.20, 0.10) * radius;
    float2 pa = p - stemTop, ba = a - stemTop;
    float d1 = length(pa - ba * clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0));
    float2 bb = b - stemTop;
    float d2 = length(pa - bb * clamp(dot(pa, bb) / dot(bb, bb), 0.0, 1.0));
    return min(d1, d2);
}

// The screw base: brushed metal lit through a normal map, as on the knob. The normal turns across
// the cylinder and rolls with every thread; the room light from above and the bulb's own light
// from the glass both catch it.
half4 base(float2 p, float pw) {
    float2 q = p - centre;
    float top = radius * 1.34;
    float bottom = radius * 1.80;
    float halfW = radius * 0.40 - max(q.y - radius * 1.68, 0.0) * 0.9;
    if (q.y < top || q.y > bottom + radius * 0.06 || abs(q.x) > halfW) return half4(0.0);
    float u = clamp(q.x / halfW, -0.98, 0.98);                       // -1 … 1 across
    float phase = (q.y - top - q.x * 0.10) / radius * 70.0;          // the threads
    float roll = q.y < bottom ? cos(phase) * 0.55 : 0.0;             // the thread's slope
    float3 n = normalize(float3(u, roll, sqrt(1.0 - u * u)));
    float brush = 0.90 + 0.10 * hash(float2(floor(q.y * 1.3), 1.0)); // fine brushed rings
    float3 room = normalize(float3(-0.42, -0.58, 0.70));
    float3 bulb = normalize(float3(0.0, -0.90, 0.45));
    float3 eye = float3(0.0, 0.0, 1.0);
    float diff = max(dot(n, room), 0.0);
    float spec = pow(max(dot(n, normalize(room + eye)), 0.0), 48.0);
    float warm = pow(max(dot(n, normalize(bulb + eye)), 0.0), 24.0) * pw;
    float cavity = 0.75 + 0.25 * sin(phase);                          // the grooves sit in shade
    half3 steel = half3(0.66, 0.64, 0.60);
    half3 metal = steel * half((0.10 + 0.70 * diff) * brush * cavity) + half3(1.0) * half(spec * 0.85)
        + half3(1.0, 0.66, 0.34) * half(warm * 0.9 + pw * 0.08 * cavity);
    if (q.y > bottom) metal = half3(0.06, 0.055, 0.06) + half3(0.4) * half(spec * 0.5); // the insulator
    float rimLine = exp(-pow((q.y - top) / (radius * 0.012), 2.0));    // a bright lip under the glass
    metal += half3(0.9, 0.85, 0.8) * half(rimLine * 0.35);
    float edge = clamp(halfW - abs(q.x), 0.0, 1.0);
    return half4(metal, 1.0) * half(edge);
}

// The room lit by the bulb: warm light falling off with distance from the filament.
half3 roomLight(float2 p, float pw) {
    float r = length(p - centre - float2(0.0, 0.05) * radius) / radius;
    return half3(1.0, 0.62, 0.30) * half(pw * 0.55 / (1.0 + r * r * 1.4));
}

half4 main(float2 p) {
    float flicker = 1.0 + 0.05 * sin(time * 23.0) * sin(time * 7.3 + 1.7);
    float pw = power * flicker;
    half4 b = base(p, pw);
    float d = glassSd(p);
    half4 under = content.eval(p);
    under.rgb += under.rgb * roomLight(p, pw) * 3.0 + roomLight(p, pw) * 0.08;
    if (d > 1.0) return b.a > 0.0 ? b : under;

    // Glass: the bulb is treated as a thick lens. Its surface rises from the silhouette like a
    // dome (Snell's law through it, as in the glass refraction demo), so the background is
    // squeezed and bent strongly near the edge and magnified in the middle. Exaggerated on
    // purpose: real thin-walled glass bends less. A little dispersion splits the colours.
    float2 e = float2(1.5, 0.0);
    float2 slopeH = float2(domeH(p + e.xy) - domeH(p - e.xy), domeH(p + e.yx) - domeH(p - e.yx)) * radius / (2.0 * e.x);
    slopeH *= min(1.0, 3.0 / max(length(slopeH), 1e-3));          // the dome is steep at its foot
    float wall = clamp(1.0 + d / (radius * 0.12), 0.0, 1.0);          // and a thin rounded wall
    float rimSlope = 0.9 * wall / sqrt(max(1.0 - wall * wall, 1e-3));
    float3 normal = normalize(float3(-slopeH * 0.9 + outward(p) * rimSlope, 1.0));
    float3 ray = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / IOR);
    float height = radius * (0.05 + 0.50 * domeH(p));
    float2 shift = ray.xy * height / -ray.z;
    float2 lens = -(p - centre) * 0.22;
    float2 seen = p + shift + lens;
    half3 col = half3(
        content.eval(p + shift * 1.06 + lens).r,
        content.eval(seen).g,
        content.eval(p + shift * 0.94 + lens).b);
    col += col * roomLight(seen, pw) * 3.0;

    // Fresnel: grazing glass at the silhouette reflects more
    float fresnel = pow(1.0 - normal.z, 3.0);
    col = mix(col, half3(0.85, 0.82, 0.86), half(clamp(fresnel * 1.6, 0.0, 0.55)));

    // window highlights on the upper left, a long soft streak on the right
    float2 q = (p - centre) / radius;
    float h1 = exp(-pow(length((q - float2(-0.42, -0.48)) * float2(1.0, 1.7)) / 0.16, 2.0));
    float h2 = exp(-pow(length((q - float2(-0.26, -0.66)) * float2(1.4, 1.0)) / 0.07, 2.0));
    float h3 = exp(-pow(abs(length(q - float2(-0.1, 0.05)) - 0.86) / 0.035, 2.0)) * smoothstep(-0.2, 0.4, q.x) * smoothstep(0.6, -0.3, q.y);
    col += half3(1.0) * half(h1 * 0.55 + h2 * 0.75 + h3 * 0.18);

    // lit from inside: the rim glows warm where the glass is seen edge-on, and small glints
    // of the filament dance on the inner surface
    float rim = exp(-pow(d / (radius * 0.05), 2.0));
    col += half3(1.0, 0.70, 0.42) * half(rim * pw * 0.35);
    float2 g1 = q - float2(0.30 + 0.02 * sin(time * 1.3), -0.40);
    float2 g2 = q - float2(-0.50, 0.12 + 0.02 * cos(time * 1.1));
    float glints = exp(-dot(g1, g1) / 0.0012) + 0.7 * exp(-dot(g2, g2) / 0.0008);
    col += half3(1.0, 0.85, 0.65) * half(glints * pw * 1.2);

    // the filament and its light
    float fd = filamentDist(p);
    half3 hot = mix(half3(1.0, 0.42, 0.12), half3(1.0, 0.86, 0.62), half(clamp(pw, 0.0, 1.0)));
    float core = exp(-pow(fd / (radius * 0.010), 2.0));
    float glow = exp(-fd / (radius * (0.025 + 0.07 * pw)));
    float wires = clamp(1.5 - wiresDist(p) / (radius * 0.006), 0.0, 1.0);
    col = mix(col, half3(0.18, 0.16, 0.16), half(wires * 0.8));
    col = mix(col, half3(0.30, 0.26, 0.24), half(core * (1.0 - pw)));        // the cold coil, when off
    col += hot * half(glow * pw * 0.35);
    col = mix(col, half3(1.0, 0.97, 0.88), half(core * pw));            // the white-hot coil on top
    // the glass itself picks up the light: brighter towards the filament
    col += hot * half(pw * 0.12 * exp(-length(p - centre - float2(0.0, 0.05) * radius) / (radius * 0.9)));

    col = col / (half3(1.0) + col * 0.25);                           // soft shoulder, the coil stays readable
    half4 glass = half4(col, 1.0);
    half4 outC = mix(under, glass, half(clamp(0.5 - d, 0.0, 1.0)));
    return b.a > 0.0 ? mix(outC, b, b.a) : outC;
}
"""

/** Keeps only the brightest light of the lit scene: what should bloom into the halo. */
@Language("AGSL")
internal const val BRIGHT_SHADER = """
uniform shader content;
uniform float power;

half4 main(float2 p) {
    half3 c = content.eval(p).rgb;
    half lum = dot(c, half3(0.299, 0.587, 0.114));
    half k = smoothstep(0.55, 1.1, lum) * half(power);
    return half4(c * k, 1.0);
}
"""

/**
 * One pass of the halo blur: the same separable Gaussian as Compose's BlurRadiusSpec
 * (ui-graphics 1.13.0-beta02, blur/BlurShaders.kt; Apache License 2.0), but the radius at a pixel
 * grows with its distance from the bulb: crisp next to the glass, wide far away.
 */
internal fun haloPassSource(vertical: Boolean): String {
    val offset = if (vertical) "vec2(0.0, i + weightH / weight)" else "vec2(i + weightH / weight, 0.0)"
    return """
uniform shader content;
uniform float2 centre;
uniform float radius;          // the bulb's radius
uniform float maxBlur;         // px
const float maxRadius = 150.0;

float gaussian(float x, float sigma) { return exp(-(x * x) / (2.0 * sigma * sigma)); }

half4 main(float2 coord) {
    float distance = length(coord - centre) / radius;
    float r = floor(maxBlur * smoothstep(0.15, 1.6, distance));
    if (r < 1.0) { return content.eval(coord); }
    float sigma = max(r / 2.0, 1.0);
    float weightSum = 1.0;
    vec4 result = content.eval(coord);
    for (float i = 1.0; i < maxRadius; i += 2.0) {
        if (i >= r) { break; }
        float weightL = gaussian(i, sigma);
        float weightH = gaussian(i + 1.0, sigma);
        float weight = weightL + weightH;
        vec2 offset = $offset;
        result += weight * (content.eval(coord - offset) + content.eval(coord + offset));
        weightSum += 2.0 * weight;
    }
    return half4(result / weightSum);
}
"""
}
