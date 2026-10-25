package com.offmind.runtimeshaders.screens.effects.composeengine.hero

import org.intellij.lang.annotations.Language
import kotlin.math.floor
import kotlin.math.sin

/*
 * A procedural city map, entirely made up: no map data, no tiles, no real names. World units are
 * roughly metres. Streets are a grid of 110 m blocks, gently bent by a smooth noise field; every
 * fourth street is a main road; a river winds through; some blocks are parks, the rest carry
 * building footprints. The route follows main roads: east along the first, then north.
 *
 * The same math lives in [warp] below (Kotlin), so the screen can keep the car on the route.
 */

internal const val BLOCK = 110f          // block pitch, world units
internal const val MAJOR_EVERY = 4f      // every 4th street is a main road
internal const val WARP = 22f            // how far streets bend, world units
internal const val ROUTE_TURN_X = BLOCK * MAJOR_EVERY * 2f   // the route turns north here

private fun hash(x: Float, y: Float): Float {
    val h = sin(x * 127.1f + y * 311.7f) * 43758.5453f
    return h - floor(h)
}

private fun noise(x: Float, y: Float): Float {
    val ix = floor(x)
    val iy = floor(y)
    val fx = x - ix
    val fy = y - iy
    val ux = fx * fx * (3f - 2f * fx)
    val uy = fy * fy * (3f - 2f * fy)
    val a = hash(ix, iy) + (hash(ix + 1f, iy) - hash(ix, iy)) * ux
    val b = hash(ix, iy + 1f) + (hash(ix + 1f, iy + 1f) - hash(ix, iy + 1f)) * ux
    return a + (b - a) * uy
}

/** World → street space: the same bend as `warp()` in the shader. */
internal fun warp(x: Float, y: Float): Pair<Float, Float> =
    x + (noise(x * 0.004f, y * 0.004f) - 0.5f) * 2f * WARP to
        y + (noise(x * 0.004f + 7.3f, y * 0.004f + 1.7f) - 0.5f) * 2f * WARP

/**
 * The world point whose street-space position is ([qx], [qy]) — one fixed-point step back through
 * [warp], close enough to keep the car on its road.
 */
internal fun unwarp(qx: Float, qy: Float): Pair<Float, Float> {
    var x = qx
    var y = qy
    repeat(3) {
        val (wx, wy) = warp(x, y)
        x += qx - wx
        y += qy - wy
    }
    return x to y
}

/** The route in street space, by distance travelled: east along y = 0, then north along x = ROUTE_TURN_X. */
internal fun routePoint(distance: Float): Pair<Float, Float> {
    val start = -BLOCK * MAJOR_EVERY * 2f
    val firstLeg = ROUTE_TURN_X - start
    return if (distance < firstLeg) start + distance to 0f else ROUTE_TURN_X to -(distance - firstLeg)
}

@Language("AGSL")
internal const val NAV_MAP_SHADER = """
uniform float2 size;      // px
uniform float2 pan;       // world point under the screen's focus
uniform float2 focus;     // px: where on screen the pan point sits
uniform float scale;      // px per world unit
uniform float routeDone;  // street-space distance already driven along the route

const float BLOCK = 110.0;
const float MAJOR = 440.0;          // BLOCK * 4
const float WARP = 22.0;
const float TURN_X = 880.0;         // MAJOR * 2
const float START_X = -880.0;

float hash(float2 p) { return fract(sin(p.x * 127.1 + p.y * 311.7) * 43758.5453); }

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1, 0)), u.x), mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), u.x), u.y);
}

float2 warp(float2 w) {
    return w + (float2(noise(w * 0.004), noise(w * 0.004 + float2(7.3, 1.7))) - 0.5) * 2.0 * WARP;
}

// distance to the nearest line of a grid with the given pitch, along one axis
float lineDist(float v, float pitch) { return abs(fract(v / pitch + 0.5) - 0.5) * pitch; }

// distance to the route (two legs) in street space
float routeDist(float2 q) {
    float a = length(q - float2(clamp(q.x, START_X, TURN_X), 0.0));
    float b = length(q - float2(TURN_X, clamp(q.y, -4000.0, 0.0)));
    return min(a, b);
}

// how far along the route the nearest route point is
float routeAlong(float2 q) {
    float a = length(q - float2(clamp(q.x, START_X, TURN_X), 0.0));
    float b = length(q - float2(TURN_X, clamp(q.y, -4000.0, 0.0)));
    return a < b ? clamp(q.x, START_X, TURN_X) - START_X : (TURN_X - START_X) - clamp(q.y, -4000.0, 0.0);
}

half3 paint(half3 base, half3 c, float d, float halfWidth, float aa) {
    return mix(base, c, half(clamp(halfWidth - d + aa * 0.5, 0.0, aa) / aa));
}

half4 main(float2 p) {
    float px = 1.0 / scale;                       // one pixel in world units
    float2 w = (p - focus) * px + pan;
    float2 q = warp(w);

    half3 land = half3(0.953, 0.945, 0.925);
    half3 col = land;

    // blocks: parks, or a few building footprints
    float2 cell = floor(q / BLOCK);
    float2 inCell = q - (cell + 0.5) * BLOCK;     // -55 … 55
    float h = hash(cell);
    if (h < 0.10) {
        col = half3(0.80, 0.90, 0.76);            // park
    } else {
        float2 sub = floor((inCell + 55.0) / 36.0);
        float2 subC = (sub + 0.5) * 36.0 - 55.0;
        float hb = hash(cell * 3.1 + sub);
        float2 halfB = float2(10.0 + 6.0 * hb, 9.0 + 7.0 * fract(hb * 7.0));
        float2 d = abs(inCell - subC) - halfB;
        float building = max(d.x, d.y);
        col = mix(col, half3(0.875, 0.860, 0.835), half(clamp(0.5 - building / px, 0.0, 1.0)));
    }

    // the river: a band along a winding curve
    float riverY = 520.0 + 260.0 * sin(q.x / 700.0) + 160.0 * (noise(q * 0.0015) - 0.5);
    float river = abs(q.y - riverY);
    col = mix(col, half3(0.66, 0.82, 0.94), half(clamp((62.0 - river) / px, 0.0, 1.0)));
    bool onWater = river < 62.0;

    // streets: minor ones stop at the water, main roads cross it as bridges
    float dMinor = min(lineDist(q.x, BLOCK), lineDist(q.y, BLOCK));
    float dMajor = min(lineDist(q.x, MAJOR), lineDist(q.y, MAJOR));
    if (!onWater) {
        col = paint(col, half3(0.85, 0.83, 0.79), dMinor, 5.5, px * 1.5);   // casing
        col = paint(col, half3(1.0), dMinor, 4.0, px * 1.5);
    }
    col = paint(col, half3(0.88, 0.76, 0.46), dMajor, 9.5, px * 1.5);
    col = paint(col, half3(1.0, 0.92, 0.70), dMajor, 7.5, px * 1.5);

    // the route: blue ahead, grey where already driven
    float dr = routeDist(q);
    bool driven = routeAlong(q) < routeDone;
    col = paint(col, half3(1.0), dr, 9.0, px * 1.5);
    col = paint(col, driven ? half3(0.66, 0.70, 0.78) : half3(0.20, 0.47, 0.96), dr, 6.5, px * 1.5);

    return half4(col, 1.0);
}
"""
