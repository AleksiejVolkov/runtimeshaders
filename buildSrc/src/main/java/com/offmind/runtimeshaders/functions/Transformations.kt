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
val RotateMatrixFunction = """
    vec2 RotateMatrix(vec2 p, vec2 pivot, float angle) {
        // Translate the UV coordinates to the origin relative to the pivot
        vec2 translated = p - pivot;

        // Calculate the sine and cosine of the angle
        float cosAngle = cos(angle);
        float sinAngle = sin(angle);

        // Perform the rotation
        vec2 rotated;
        rotated.x = translated.x * cosAngle - translated.y * sinAngle;
        rotated.y = translated.x * sinAngle + translated.y * cosAngle;

        // Translate the rotated UV coordinates back to the original position
        rotated += pivot;

        return rotated;
    }
""".trimIndent()

val allTransformationsFunctions = mapOf(
    "RotateMatrix" to RotateMatrixFunction
)
