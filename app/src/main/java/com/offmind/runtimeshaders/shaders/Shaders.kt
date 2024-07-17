package com.offmind.runtimeshaders.shaders

val initial_shader = """
        uniform shader image;
        uniform vec2 resolution;
        uniform float time;
       
        vec4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution - 0.5;
           
            vec3 col = vec3(
                abs(sin(cos(time+3.*uv.y)*2.*uv.x+time)),
                abs(cos(sin(time+2.*uv.x)*3.*uv.y+time)),
                abs(cos(cos(time+2.*uv.x)*3.*uv.y+time)));
            return vec4(col, 1.0);
       }
""".trimIndent()
