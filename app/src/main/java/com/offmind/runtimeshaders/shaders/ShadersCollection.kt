package com.offmind.runtimeshaders.shaders

val shockwave = """
       vec4 main(float2 fragCoord) {
            float2 uv = NormalizeCoordinates(fragCoord, resolution);
            float2 normalizedPointer = NormalizeCoordinates(pointer, resolution);
            
            float coef = 0.5;
            float r = 0.17 * coef;
            float sdf = CircleSDF(uv - normalizedPointer, r);
            float mask = smoothstep(r + 0.1, r, sdf) * smoothstep(r - 0.1, r, sdf);
           
            vec3 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution).rgb;
            vec3 ripple = ChromaticAberration(uv, vec2(0.01,0.0), resolution);
            
            // Combine the channels with the chromatic aberration
            vec3 finalColor = mix(image, ripple, (1.-coef));
            
            return vec4(finalColor, 1.0);
       }
""".trimIndent()

val chromaticAberrationEffect = """
       vec4 main(float2 fragCoord) {
            float2 uv = NormalizeCoordinates(fragCoord, resolution);
          
            float coef = 0.5;
            float r = 0.17 * coef;
          
            vec4 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
            vec3 ripple = ChromaticAberration(uv, vec2(0.01,0.0)*sin(time), resolution);
            
            // Combine the channels with the chromatic aberration
            vec3 finalColor = mix(image.rgb, ripple, (1.-coef));
            
            return vec4(finalColor, image.a);
       }
""".trimIndent()