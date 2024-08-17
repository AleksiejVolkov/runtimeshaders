package com.offmind.runtimeshaders.shaders

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

val Uniforms = """
        uniform shader image;
        uniform vec2 resolution;
        uniform float time;
        uniform float2 pointer;
        uniform float percentage;
""".trimIndent()

val Functions = """
        $GetViewTextureFunction
        $CircleSDFFunction
""".trimIndent()

val initial_shader = Uniforms + Functions + """
       vec4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution - 0.5;
            uv.y *= resolution.y / resolution.x;
            
            float2 normalizedPointer = pointer / resolution - .5;
            normalizedPointer.y *= resolution.y / resolution.x;
            
            float coef = percentage;
            float r = 0.17 * coef;
            float sdf = CircleSDF(uv - normalizedPointer, r);
            float mask = smoothstep(r + 0.1, r, sdf) * smoothstep(r - 0.1, r, sdf);
            
            // Chromatic Aberration Offsets
            float chromaOffset = 0.1 * (1.0 - coef);  // Adjust the intensity of the effect
            float2 uvR = uv + sdf * mask * (1.0 - coef) + chromaOffset * vec2(1.0, 0.0);
            float2 uvG = uv + sdf * mask * (1.0 - coef);
            float2 uvB = uv + sdf * mask * (1.0 - coef) - chromaOffset * vec2(1.0, 0.0);
        
            // Sample the texture with the chromatic offsets
            vec3 imageR = GetImageTexture(uvR.xy, vec2(0.5, 0.5)).rgb;
            vec3 imageG = GetImageTexture(uvG.xy, vec2(0.5, 0.5)).rgb;
            vec3 imageB = GetImageTexture(uvB.xy, vec2(0.5, 0.5)).rgb;
        
            vec3 image = GetImageTexture(uv.xy, vec2(0.5, 0.5)).rgb;
            vec3 ripple = vec3(imageR.r, imageG.g, imageB.b);
            // Combine the channels with the chromatic aberration
            vec3 finalColor = mix(image, ripple, mask*(1.-coef));
            
            return vec4(finalColor, 1.0);
       }
""".trimIndent()

