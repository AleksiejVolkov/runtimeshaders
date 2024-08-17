package com.offmind.runtimeshaders.shaders

val shockwave = """
       vec4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution - 0.5;
            uv.y *= resolution.y / resolution.x;
            
            float2 normalizedPointer = pointer / resolution - .5;
            normalizedPointer.y *= resolution.y / resolution.x;
            
            float coef = percentage;
            float r = 0.17 * coef;
            float sdf = CircleSDF(uv - normalizedPointer, r);
            float mask = smoothstep(r + 0.1, r, sdf) * smoothstep(r - 0.1, r, sdf);
           
            vec3 image = GetImageTexture(uv.xy, vec2(0.5, 0.5)).rgb;
            vec3 ripple = ChromaticAberration(uv + sdf * mask * (1.0 - coef),  0.1 * (1.0 - coef));
            
            // Combine the channels with the chromatic aberration
            vec3 finalColor = mix(image, ripple, mask*(1.-coef));
            
            return vec4(finalColor, 1.0);
       }
""".trimIndent()