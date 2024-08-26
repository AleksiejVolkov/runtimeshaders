package com.offmind.runtimeshaders.functions

/**
 * A function that converts a color from RGB (Red, Green, Blue) to HSV (Hue, Saturation, Value).
 *
 * This function takes a `vec3` representing a color in RGB space and converts it to a `vec3` representing
 * the same color in HSV space. The conversion is done using a series of mathematical operations to map
 * the RGB components to their corresponding HSV values.
 *
 * @param c A `vec3` representing the color in RGB space, where:
 *          - `c.x` is the red component (0.0 to 1.0),
 *          - `c.y` is the green component (0.0 to 1.0),
 *          - `c.z` is the blue component (0.0 to 1.0).
 * @return A `vec3` representing the color in HSV space, where:
 *          - `x` is the hue (0.0 to 1.0),
 *          - `y` is the saturation (0.0 to 1.0),
 *          - `z` is the value (0.0 to 1.0).
 */
val RGBToHSVFunction = """
    vec3 RGBtoHSV(vec3 c) {
        vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);
        vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
        vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
        
        float d = q.x - min(q.w, q.y);
        float e = 1.0e-10;
        return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
    }
""".trimIndent()

/**
 * A function that converts a color from HSV (Hue, Saturation, Value) to RGB (Red, Green, Blue).
 *
 * This function takes a `vec3` representing a color in HSV space and converts it to a `vec3` representing
 * the same color in RGB space. The conversion is done using a series of mathematical operations to map
 * the HSV components to their corresponding RGB values.
 *
 * @param c A `vec3` representing the color in HSV space, where:
 *          - `c.x` is the hue (0.0 to 1.0),
 *          - `c.y` is the saturation (0.0 to 1.0),
 *          - `c.z` is the value (0.0 to 1.0).
 * @return A `vec3` representing the color in RGB space, where each component (r, g, b) is in the range [0.0, 1.0].
 */
val HSVToRgbFunction = """
    vec3 HSVtoRGB(vec3 c) {
        vec3 p = abs(fract(c.xxx + vec3(0.0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);
        return c.z * mix(vec3(1.0), clamp(p - 1.0, 0.0, 1.0), c.y);
    }
""".trimIndent()

/**
 * A function that applies chromatic aberration to a texture.
 *
 * This function offsets the red, green, and blue channels of the texture by a specified amount,
 * creating a chromatic aberration effect. The offsets are applied along the x-axis.
 *
 * @param p The 2D vector representing the position from which to sample the texture.
 * @param d The 2D vector representing the direction for offset the red and blue channels.
 * @param r The 2D vector representing resolution
 * @return A `vec3` representing the color of the texture at the adjusted position, with chromatic aberration applied.
 */
val ChromaticAberrationFunction = """
    vec3 ChromaticAberration(vec2 p, vec2 d, vec2 r) {
        vec2 center = vec2(0.5, 0.5);
        
        vec3 imageR = GetImageTexture(p, center + d, r).rgb;
        vec3 imageG = GetImageTexture(p, center, r).rgb;
        vec3 imageB = GetImageTexture(p, center - d, r).rgb;
        
        return vec3(imageR.r, imageG.g, imageB.b);
    }
""".trimIndent()


val allColorCorrectionFunctions = mapOf(
    "RGBToHSV" to RGBToHSVFunction,
    "HSVToRgb" to HSVToRgbFunction,
    "ChromaticAberration" to ChromaticAberrationFunction
)
