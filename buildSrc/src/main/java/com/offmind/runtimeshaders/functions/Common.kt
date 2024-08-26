package com.offmind.runtimeshaders.functions

/**
 * A function that generates a pseudo-random float value based on a 2D vector input.
 *
 * This function uses a hash function to compute a pseudo-random value from the input vector `c`.
 * The result is a float value in the range [0, 1).
 *
 * @param c The 2D vector input used to generate the pseudo-random value.
 * @return A pseudo-random float value in the range [0, 1).
 */
val Hash21Function = """
    float Hash21(vec2 c){
        return fract(sin(dot(c.xy, vec2(12.9898,78.233))) * 43758.5453);
    }
""".trimIndent()

/**
 * Retrieves the color of the image at a specified position, adjusted by a pivot point.
 *
 * This function first adjusts the y-coordinate of the position `p` to maintain the aspect ratio
 * of the image based on the `resolution`. It then applies a pivot transformation to `p`, scales
 * the position by the `resolution`, and finally samples the color of the image at this adjusted
 * position.
 *
 * @param p The original position vector from which to sample the image, typically representing UV coordinates.
 * @param pivot A vector representing the pivot point used to adjust the position `p` before sampling.
 * @param r A resolution vector used to normalize the coordinates.
 * @return The color of the image at the adjusted position as a `vec4`.
 */
val GetViewTextureFunction = """
    vec4 GetImageTexture(vec2 p, vec2 pivot, vec2 r) {
        if (r.x > r.y) {
            p.x /= r.x / r.y;
        } else {
            p.y /= r.y / r.x;
        }
        p += pivot;
        p *= r;
        return image.eval(p);
    }   
""".trimIndent()

/**
 * A function that normalizes coordinates based on the resolution.
 *
 * This function adjusts the input coordinates `o` by the resolution `r` to normalize them.
 * It ensures that the aspect ratio is maintained by scaling the coordinates differently
 * depending on whether the width is greater than the height or vice versa.
 *
 * @param o The original coordinates to be normalized.
 * @param r The resolution used to normalize the coordinates.
 * @return The normalized coordinates as a `vec2`.
 */
val NormalizeCoordinatesFunction = """
    vec2 NormalizeCoordinates(vec2 o, vec2 r) {
        float2 uv = o / r - 0.5;
        if (r.x > r.y) {
            uv.x *= r.x / r.y;
        } else {
            uv.y *= r.y / r.x;
        } 
        return uv;
    }
""".trimIndent()

/**
 * A function that remaps a value from one range to another.
 *
 * This function takes a float value and remaps it from the range \[low1, high1\] to the range \[low2, high2\].
 * The remapping is done using a linear interpolation formula.
 *
 * @param value The float value to be remapped.
 * @param low1 The lower bound of the original range.
 * @param high1 The upper bound of the original range.
 * @param low2 The lower bound of the target range.
 * @param high2 The upper bound of the target range.
 * @return The remapped float value in the range \[low2, high2\].
 */
val RemapFunction = """
    float Remap(float value, float low1, float high1, float low2, float high2) {
        return low2 + (value - low1) * (high2 - low2) / (high1 - low1);
    }
""".trimIndent()

/**
 * A function that remaps a normalized value to a specified range.
 *
 * This function takes a float value in the range \[0, 1\] and remaps it to the range \[low1, high1\].
 * The remapping is done using a linear interpolation formula.
 *
 * @param value The normalized float value to be remapped (0.0 to 1.0).
 * @param low The lower bound of the target range.
 * @param high The upper bound of the target range.
 * @return The remapped float value in the range \[low1, high1\].
 */
val RemapNormalizedFunction = """
    float RemapNormalized(float value, float low, float high) {
        return low + value * (high - low);
    }
""".trimIndent()

val DegreesToRadiansFunction = """
    float DegreesToRadians(float degrees) {
        return degrees * 0.0174533;
    }
""".trimIndent()

val RadianToDegreesFunction = """
    float RadiansToDegrees(float radians) {
        return radians * 57.2958;
    }
""".trimIndent()

val allCommonFunctions = mapOf(
    "Hash21" to Hash21Function,
    "GetImageTexture" to GetViewTextureFunction,
    "NormalizeCoordinates" to NormalizeCoordinatesFunction,
    "DegreesToRadians" to DegreesToRadiansFunction,
    "RadiansToDegrees" to RadianToDegreesFunction,
    "Remap" to RemapFunction,
    "RemapNormalized" to RemapNormalizedFunction
)
