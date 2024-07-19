package com.offmind.runtimeshaders.shaders

val text_from_noise = """
        uniform shader image;
        uniform vec2 resolution;
        uniform float time;
        uniform float percentage;
        
        float Hash21(vec2 p) { 
            return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
        }
            
        vec4 main(float2 fragCoord) {
            vec4 parent = image.eval(fragCoord);
            vec2 uv = fragCoord / resolution - 0.5;
            vec2 shiftedUv = uv + time*uv;
            
            vec3 noise = vec3(Hash21(shiftedUv)); // for now just black
            vec3 image = parent.rgb;
            
            vec3 col = mix(noise, image, percentage);
            float a = mix(1.0, parent.a, percentage);
            
            return vec4(col, a);
        }
""".trimIndent()
