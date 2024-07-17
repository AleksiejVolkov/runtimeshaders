package com.offmind.runtimeshaders.shaders

val disappear_shader = """
        uniform shader image;
        uniform vec2 resolution;
        uniform float time;
        uniform float percentage;
          
        float rand(vec2 c){
            return fract(sin(dot(c.xy ,vec2(12.9898,78.233))) * 43758.5453);
        }

        float noise(vec2 p, float freq){
            float unit = resolution.y/freq;
            vec2 ij = floor(p/unit);
            vec2 xy = mod(p,unit)/unit;
            //xy = 3.*xy*xy-2.*xy*xy*xy;
            xy = .5*(1.-cos(3.1415*xy));
            float a = rand((ij+vec2(0.,0.)));
            float b = rand((ij+vec2(1.,0.)));
            float c = rand((ij+vec2(0.,1.)));
            float d = rand((ij+vec2(1.,1.)));
            float x1 = mix(a, b, xy.x);
            float x2 = mix(c, d, xy.x);
            return mix(x1, x2, xy.y);
        }
            
        vec4 main(float2 fragCoord) {
         float2 uv = fragCoord / resolution - 0.5;
         float2 sv = uv * (1.-percentage*0.5);
        
         float scale = (1.-percentage*0.2)-(length(uv)*percentage)*.6;
         uv *= scale;
         float n =step(percentage,noise(uv,20000.));
         
         vec2 scaledFragCoords = fragCoord*scale;
         vec2 translation = vec2(
            0.5*(resolution.x-resolution.x*scale), 
            0.5*(resolution.y-resolution.y*scale)
            );
         
         vec4 parent = image.eval(scaledFragCoords+translation);
         vec3 col = parent.rgb*n;
         
         float borders = smoothstep(.5,0.48,length(sv.x))* smoothstep(.5,0.48,length(sv.y));
         col*=borders;
         
         float alpha = 1.0-percentage;//1.-percentage*length(uv*(1.+percentage));
                  
         return vec4(col*parent.a, parent.a*alpha*borders);
        }
""".trimIndent()
