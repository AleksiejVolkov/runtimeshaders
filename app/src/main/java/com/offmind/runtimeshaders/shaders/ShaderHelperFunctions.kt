package com.offmind.runtimeshaders.shaders

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
 * Retrieves the color of the image at a specified position, adjusted by a pivot point.
 *
 * This function first adjusts the y-coordinate of the position `p` to maintain the aspect ratio
 * of the image based on the `resolution`. It then applies a pivot transformation to `p`, scales
 * the position by the `resolution`, and finally samples the color of the image at this adjusted
 * position.
 *
 * @param p The original position vector from which to sample the image, typically representing UV coordinates.
 * @param pivot A vector representing the pivot point used to adjust the position `p` before sampling.
 * @return The color of the image at the adjusted position as a `vec4`.
 */
val GetViewTextureFunction = """
        vec4 GetImageTexture(vec2 p, vec2 pivot) {
          p.y /= resolution.y / resolution.x;
          p+=pivot;
          p*=resolution;
          return image.eval(p);
        }   
""".trimIndent()

/**
 * A function that applies chromatic aberration to a texture.
 *
 * This function offsets the red, green, and blue channels of the texture by a specified amount,
 * creating a chromatic aberration effect. The offsets are applied along the x-axis.
 *
 * @param p The 2D vector representing the position from which to sample the texture.
 * @param chromaOffset The amount by which to offset the red and blue channels.
 * @return A `vec3` representing the color of the texture at the adjusted position, with chromatic aberration applied.
 */
val ChromaticAberrationFunction = """
        vec3 ChromaticAberration(vec2 p, float chromaOffset) {
            float2 uvR = p + chromaOffset * vec2(1.0, 0.0);
            float2 uvG = p;
            float2 uvB = p - chromaOffset * vec2(1.0, 0.0);
        
            // Sample the texture with the chromatic offsets
            vec3 imageR = GetImageTexture(uvR.xy, vec2(0.5, 0.5)).rgb;
            vec3 imageG = GetImageTexture(uvG.xy, vec2(0.5, 0.5)).rgb;
            vec3 imageB = GetImageTexture(uvB.xy, vec2(0.5, 0.5)).rgb;
           
            return vec3(imageR.r, imageG.g, imageB.b);
        }
""".trimIndent()