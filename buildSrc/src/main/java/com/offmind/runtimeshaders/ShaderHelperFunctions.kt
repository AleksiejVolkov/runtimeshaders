package com.offmind.runtimeshaders

/**
 * A function that calculates the Signed Distance Function (SDF) for a circle.
 *
 * This function computes the distance from a point `p` to the edge of a circle with radius `r`.
 * The distance is positive if the point is outside the circle, negative if inside, and zero if on the edge.
 *
 * @param p The 2D vector representing the point from which the distance is calculated.
 * @param r The radius of the circle.
 * @return The signed distance from the point `p` to the edge of the circle.
 */
val CircleSDFFunction = """
    float CircleSDF(vec2 p, float r) {
        return length(p) - r;
    }   
""".trimIndent()

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
 * A function that generates 2D Simplex noise.
 *
 * This function computes a noise value based on the input vector `p` using a combination of hash functions
 * and cosine interpolation. The result is a float value representing the noise at the given position.
 *
 * @param p The 2D vector input used to generate the noise value.
 * @return A float value representing the noise at the given position.
 */
val SimplexNoiseFunction = """
    float SNoise(vec2 p) {
        float unit = 0.05;
        vec2 ij = floor(p/unit);
        vec2 xy = mod(p,unit)/unit;
        xy = .5*(1.-cos(3.1415*xy));
        float a = Hash21((ij+vec2(0.,0.)));
        float b = Hash21((ij+vec2(1.,0.)));
        float c = Hash21((ij+vec2(0.,1.)));
        float d = Hash21((ij+vec2(1.,1.)));
        float x1 = mix(a, b, xy.x);
        float x2 = mix(c, d, xy.x);
        return mix(x1, x2, xy.y);
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
    vec2 rotateMatrix(vec2 p, vec2 pivot, float angle) {
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

val allFunctions = mapOf(
    "CircleSDF" to CircleSDFFunction,
    "Hash21" to Hash21Function,
    "GetImageTexture" to GetViewTextureFunction,
    "SNoise" to SimplexNoiseFunction,
    "ChromaticAberration" to ChromaticAberrationFunction,
    "NormalizeCoordinates" to NormalizeCoordinatesFunction,
    "DegreesToRadians" to DegreesToRadiansFunction,
    "RadiansToDegrees" to RadianToDegreesFunction,
    "RotateMatrix" to RotateMatrixFunction,
    "RGBtoHSV" to RGBToHSVFunction,
    "HSVtoRGB" to HSVToRgbFunction
)