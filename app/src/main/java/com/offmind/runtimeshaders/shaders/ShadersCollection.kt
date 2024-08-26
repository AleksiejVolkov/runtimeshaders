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

val lampWithShadowEffect = """
    
    vec3 GetSaturatedColor(vec3 origin, float saturation, float brightness) {
        vec3 hsvColor = RGBtoHSV(origin);
        
        hsvColor.y = hsvColor.y * saturation; 
        hsvColor.z = hsvColor.z * brightness; 
        
        return HSVtoRGB(hsvColor);
    }
    
    vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);
        vec4 originColor = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
      
        vec3 lightColor = vec3(.89,.89,0.75);
        vec3 colorInShadow = GetSaturatedColor(originColor.rgb, 0.5, 0.4);
        vec3 colorInLight = GetSaturatedColor(originColor.rgb, 1.5, 1.2);
        
        vec2 uvRotatedToLamp = RotateMatrix(uv, lampPosition, DegreesToRadians(-lampAngle));
       
        float lampVerticalDistance = (uvRotatedToLamp.y-lampPosition.y)*0.4;
        float lampLightEdge = 0.5*lampWidth+lampVerticalDistance;
        float lightMask = smoothstep(lampLightEdge,lampLightEdge*0.9,abs(uvRotatedToLamp.x-lampPosition.x))*step(lampPosition.y, uvRotatedToLamp.y);

        vec2 angleToSliderLeftEdge = lampPosition-(seekBarPosition-vec2(0.5*sliderWidth,0.));
        vec2 uvRotatedToSlider = RotateMatrix(uv, seekBarPosition, atan(angleToSliderLeftEdge.x,angleToSliderLeftEdge.y));
        float shadow = smoothstep((uv.y-seekBarPosition.y)*0.1,0.0,(uvRotatedToSlider.x-seekBarPosition.x-0.5*sliderWidth));
        
        vec2 angleToSliderRightEdge = lampPosition-(seekBarPosition+vec2(0.5*sliderWidth,0.));
        uvRotatedToSlider = RotateMatrix(uv, seekBarPosition, atan(angleToSliderRightEdge.x,angleToSliderRightEdge.y));
        shadow *= smoothstep(0.0,(uv.y-seekBarPosition.y)*0.1,(uvRotatedToSlider.x-seekBarPosition.x+0.5*sliderWidth));
        
        shadow *= step(seekBarPosition.y, uv.y);
        
        lightMask *= 1.- shadow;
        lightMask *= 0.3*percentage;
        
        lightMask *=1./(5.*length(lampPosition.y));
        
        float lampGlow = 1./(3.5*length(uvRotatedToLamp-lampPosition))*lightMask;
      
        vec3 finalColor = mix(colorInShadow, colorInLight, lightMask)+lampGlow*lightColor;
        return vec4(finalColor, originColor.a);
    }
""".trimIndent()

val fireShader = """
    // Colorize function maps the noise value to a color
    vec3 colorize(float value) {
        vec3 color;
        if (value < 0.5) {
            color = mix(vec3(1.0, 1.0, 1.0), vec3(1.0, 0.5, 0.0), value * 2.0);
        } else {
            color = mix(vec3(1.0, 0.5, 0.0), vec3(1.0, 0.0, 0.0), (value - 0.5) * 2.0);
        }
        return color;
    }
    
     vec4 main(float2 fragCoord) {
        float2 uv = NormalizeCoordinates(fragCoord, resolution);
        vec4 image = GetImageTexture(uv, vec2(0.5, 0.5), resolution);
        vec4 imageScaled = GetImageTexture(vec2(uv.x,uv.y*0.9+0.1), vec2(0.5, 0.5), resolution);
        
        vec2 fbmNoiseCoords = vec2(3.*uv.x, 2.+uv.y); 
        vec2 fbmNoiseCoords2 = vec2(8.*uv.x, 2.+3.5*uv.y+2.*time); 
        float noiseValue = FBM(fbmNoiseCoords,5);
        float noiseValue2 = FBM(fbmNoiseCoords2,5);
        
        float ground = 1.-percentage;
        float vertical = uv.y+1.0;
        
        vec3 finalColor = mix(image.rgb,colorize(noiseValue),noiseValue*(1.-ground));
        finalColor += colorize(noiseValue2)*percentage*colorize(noiseValue2)*smoothstep(-1.,.5,uv.y);
        finalColor = mix(finalColor, vec3(0.), smoothstep(0.85*ground, 0.95*ground, noiseValue)*image.a);
       
       // finalColor =
        
        float mask = 1.-step(ground,noiseValue);
        
        float alpha = mix(image.a,imageScaled.a,percentage)*mask;
        
        return vec4(finalColor*alpha, alpha);
     }
""".trimIndent()
