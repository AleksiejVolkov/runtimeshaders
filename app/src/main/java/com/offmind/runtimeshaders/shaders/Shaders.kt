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
""".trimIndent()

val Functions = """
        $GetViewTextureFunction
""".trimIndent()

val initial_shader = Uniforms + Functions + """
       vec4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution - 0.5;
            uv.y *= resolution.y / resolution.x;
            vec3 checker = GetImageTexture(uv, vec2(0.5, 0.5)).rgb;
            
            uv.y += 0.9*sin(time);
            float circle = smoothstep(0.3,0.29,length(uv));
            float circle2 = smoothstep(0.29,0.28,length(uv));

            vec3 upsideDownChecker = GetImageTexture(vec2(-1.)*uv*length(uv)*4.,vec2(0.5,0.5+0.3*sin(time))).rgb; 
            
            upsideDownChecker *= vec3(0.9);
            upsideDownChecker += vec3(1.)*0.2*smoothstep(.8,0.,length(uv+0.2));
            vec3 finlcol = circle*upsideDownChecker;
             
            vec3 col2 = mix(checker,finlcol,circle2);
            
            return vec4(col2, 1.0);
       }
""".trimIndent()

