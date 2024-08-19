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

val allFunctions = mapOf(
    "CircleSDF" to CircleSDFFunction,
    "Hash21" to Hash21Function,
    "GetImageTexture" to GetViewTextureFunction,
    "SNoise" to SimplexNoiseFunction,
    "ChromaticAberration" to ChromaticAberrationFunction,
    "NormalizeCoordinates" to NormalizeCoordinatesFunction
)
