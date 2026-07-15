package com.offmind.runtimeshaders.screens.effects.pixeldisplay

/**
 * Dot-matrix / LED-panel post-process.
 *
 * It treats whatever UI is fed in as `image` as the source of a monochrome pixel display:
 * the screen is divided into a regular grid of square cells, each cell samples the UI at its
 * centre, and is rendered as either
 *  - a faint "empty" grid dot when the underlying UI is dark/transparent, or
 *  - a solid white pixel square whose size scales with the underlying luminance.
 *
 * Because it only reads luminance, any standard Compose component placed inside the host
 * `ShadedBox` (Button, TextField, Switch, Text, …) is automatically re-rendered in the same
 * pixelised style.
 *
 * Expected uniforms, declared by the Shader wrapper: image, resolution, gridColumns.
 */
internal val pixelDisplayShaderSource = """
    half4 main(vec2 fragCoord) {
        float cols = max(gridColumns, 1.0);
        float cell = resolution.x / cols;

        // Snap to the owning grid cell and sample the UI at its centre.
        vec2 cellIndex  = floor(fragCoord / cell);
        vec2 cellCenter = (cellIndex + 0.5) * cell;
        half4 src = image.eval(cellCenter);

        // Premultiply by alpha so transparent regions read as "off".
        float lum = dot(src.rgb, half3(0.299, 0.587, 0.114)) * src.a;

        // Position inside the cell, normalised to [-0.5, 0.5]; Chebyshev distance => square cells.
        vec2 local = (fragCoord - cellCenter) / cell;
        float d = max(abs(local.x), abs(local.y));

        // Edge softness: a ~1px feather plus a fixed amount so cells read smooth, not crisp.
        float aa = clamp(1.0 / cell, 0.015, 0.10);
        float soft = aa + 0.07;

        // Empty-grid dot: a tiny faint square at every cell centre.
        float dotHalf = 0.08;
        float dotMask = 1.0 - smoothstep(dotHalf - soft, dotHalf + soft, d);
        half3 dotColor = half3(0.16);

        // Lit pixel: quantise luminance into a few discrete steps that vary BOTH the square size
        // and its shade (off -> small gray -> light gray -> full white), so the UI ramps smoothly
        // instead of hard on/off.
        float squareHalf = 0.0;
        float tone = 1.0;
        if (lum >= 0.62)      { squareHalf = 0.44; tone = 1.0;  }  // full white
        else if (lum >= 0.38) { squareHalf = 0.34; tone = 0.72; }  // light gray
        else if (lum >= 0.14) { squareHalf = 0.22; tone = 0.42; }  // gray
        float squareMask = squareHalf > 0.001
            ? 1.0 - smoothstep(squareHalf - soft, squareHalf + soft, d)
            : 0.0;

        half3 col = half3(0.0);
        col = mix(col, dotColor, dotMask);
        col = mix(col, half3(tone), squareMask);

        return half4(col, 1.0);
    }
"""
