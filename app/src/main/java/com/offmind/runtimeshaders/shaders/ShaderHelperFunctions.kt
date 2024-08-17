package com.offmind.runtimeshaders.shaders

val CircleSDFFunction = """
        float CircleSDF(vec2 p, float r) {
            return length(p) - r;
        }   
""".trimIndent()