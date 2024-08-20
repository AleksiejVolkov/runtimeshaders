package com.offmind.runtimeshaders.shaders

val shockwave = """
     vec4 drop(vec2 d, vec2 p) {
            float coef = percentage;
            float r = 0.17 * coef;
            float sdf = CircleSDF(d - p, r);
            float mask = smoothstep(r + 0.1, r, sdf) * smoothstep(r - 0.1, r, sdf);
           
            vec4 image = GetImageTexture(d, vec2(0.5, 0.5), resolution);
            vec3 ripple = ChromaticAberration(d+sdf * mask, vec2(0.1, 0.0), resolution);
            
            // Combine the channels with the chromatic aberration
            return vec4(mix(image.rgb, ripple, mask*(1.-coef)),mask*(1.-coef));
       }
       
       vec4 main(float2 fragCoord) {
            float2 uv = NormalizeCoordinates(fragCoord, resolution);
            float2 normalizedPointer = NormalizeCoordinates(pointer, resolution);
            
            vec4 drop1 = drop(uv, normalizedPointer);
           
            vec4 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
            
            vec3 finalColor = mix(image.rgb, drop1.rgb, drop1.a);
          //  finalColor = mix(finalColor, drop2.rgb, drop2.a);
            
            return vec4(finalColor, image.a);
       }
""".trimIndent()

val chromaticAberrationEffect = """
       vec4 main(float2 fragCoord) {
            float2 uv = NormalizeCoordinates(fragCoord, resolution);
          
            float coef = percentage;
            float r = 0.17 * coef;
          
            vec4 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
            vec3 ripple = ChromaticAberration(uv, vec2(0.01,0.0)*sin(time), resolution);
            
            // Combine the channels with the chromatic aberration
            vec3 finalColor = mix(image.rgb, ripple, (1.-coef));
            
            return vec4(finalColor, image.a);
       }
""".trimIndent()

val washDownToDisappear = """
       vec4 main(float2 fragCoord) {
            float2 uv = NormalizeCoordinates(fragCoord, resolution);
            
            float coef = percentage;
            float r = 0.17 * coef;
         
            // Apply vertical distance based on the lower half of the screen
            float vertical = smoothstep(0.0, .2, uv.y);
            float random = vertical*0.2 + SNoise(vec2(uv.x, uv.x * .5)) * vertical;
            
            // Stretch effect: more movement at the bottom, less at the top
            vec2 verticalDistance = vec2(0., uv.y) * coef * random;
            vec4 image = GetImageTexture((uv - verticalDistance), vec2(0.5, 0.5), resolution);
            
            float alpha = (1.0 - coef);
            alpha *= smoothstep(1., 0.4, uv.y);
            vec3 finalColor = image.rgb*alpha;
            return vec4(finalColor, image.a * alpha);
       }
""".trimIndent()