package com.offmind.runtimeshaders.shaders

val disappear_shader = """
        uniform shader image;
        uniform vec2 resolution;
        uniform float time;
        uniform float percentage;
          
        float Hash21(vec2 c){
            return fract(sin(dot(c.xy ,vec2(12.9898,78.233))) * 43758.5453);
        }

        float Noise(vec2 p){
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
            
        vec4 main(float2 fragCoord) {
            // Normalize the fragment coordinates to range [-0.5, 0.5]
            float2 uv = fragCoord / resolution - 0.5;
            // Coords for mask, it should just slightly scale in linear way
            float2 sv = uv * (1.-percentage*0.5);
        
            // Calculate a non-linear scale factor based on the distance from the center
            // This creates an effect where the edges disperse faster than the center
            float scale = (1.-percentage*0.2)-(length(uv)*percentage)*.6;
            
            uv *= scale;
            
            // Generate noise based on the scaled UV coordinates and use the step function
            // The noise function provides pseudo-random values
            // The step function thresholds the noise based on the percentage variable
            float noise = step(percentage, Noise(uv));
            
            // Calculate the scaled fragment coordinates
            vec2 scaledFragCoords = fragCoord*scale;
            
            // Calculate the translation to keep the scaled image centered    
            vec2 translation = vec2(
                0.5*(resolution.x-resolution.x*scale), 
                0.5*(resolution.y-resolution.y*scale)
            );
      
            // Sample the image at the scaled and translated coordinates
            // The image.eval function evaluates the color of the texture at the given coordinates
            vec4 parent = image.eval(scaledFragCoords+translation);
        
            // Multiply the RGB channels by the noise value 'noise'
            // This effectively masks the image with the noise pattern
            vec3 col = parent.rgb*noise;
        
            // Smoothly blend the edges of the effect based on the percentage variable
            // The smoothstep function interpolates smoothly between 0 and 1 over a given range
            // This helps to create a smooth transition effect at the borders and
            // remove undesired artifacts:
            float borders = smoothstep(.5,.5-0.2*percentage,length(sv.x)) 
                            * smoothstep(.5,.5-0.2*percentage,length(sv.y));
            
            col*=borders;
        
            // Calculate the alpha value based on the percentage variable
            // This controls the overall transparency of the effect
            float alpha = 1.0-percentage;  
            
            return vec4(col*alpha, alpha*borders);
        }
""".trimIndent()
