package com.offmind.runtimeshaders.screens.effects.glassbutton

/**
 * Glass surface shader for the "Understanding the Glass" demo.
 *
 * The material is driven by a rounded-rect SDF. The SDF gives us a clean mask and, more
 * importantly, a bevel normal around the rim. Lighting that normal from the upper-left creates
 * the same vocabulary as the reference: a white leading highlight, a darker lower-right edge,
 * a brighter glass body near the light, and a warm caustic-like rim near the bottom.
 *
 * Expected uniforms, declared by the Shader wrapper:
 * image, resolution, cornerRadius, lightPosition, lightColor, shadowStrength, time, press.
 */
internal val glassButtonShaderSource = """
    float clamp01(float value) {
        return clamp(value, 0.0, 1.0);
    }

    float sdRoundedBox(vec2 p, vec2 halfSize, float radius) {
        vec2 q = abs(p) - halfSize + radius;
        return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - radius;
    }

    vec2 sdNormal(vec2 p, vec2 halfSize, float radius) {
        float dx = sdRoundedBox(p + vec2(1.0, 0.0), halfSize, radius)
                 - sdRoundedBox(p - vec2(1.0, 0.0), halfSize, radius);
        float dy = sdRoundedBox(p + vec2(0.0, 1.0), halfSize, radius)
                 - sdRoundedBox(p - vec2(0.0, 1.0), halfSize, radius);
        return normalize(vec2(dx, dy));
    }

    half4 over(half4 dst, half3 color, float alpha) {
        half a = half(clamp01(alpha));
        return half4(dst.rgb * (1.0 - a) + color * a, max(dst.a, a));
    }

    half4 main(vec2 fragCoord) {
        vec2 halfSize = resolution * 0.5;
        vec2 p = fragCoord - halfSize;
        float radius = min(cornerRadius, min(halfSize.x, halfSize.y) - 1.0);
        float sd = sdRoundedBox(p, halfSize, radius);
        float mask = 1.0 - smoothstep(-1.25, 1.25, sd);

        if (mask <= 0.0) {
            return half4(0.0);
        }

        float insideDistance = max(-sd, 0.0);
        float bevelWidth = max(min(resolution.y * 0.34, radius * 0.95), 10.0);
        float bevel = 1.0 - smoothstep(0.0, bevelWidth, insideDistance);
        float bevelSoft = bevel * bevel * (3.0 - 2.0 * bevel);

        vec2 edgeNormal = sdNormal(p, halfSize, radius);
        vec2 lightVec = lightPosition - fragCoord;
        vec3 lightDir = normalize(vec3(lightVec / max(resolution.y, 1.0), 0.72));

        vec3 normal = normalize(vec3(
            edgeNormal.x * bevelSoft * 1.34,
            edgeNormal.y * bevelSoft * 1.08,
            1.0 - bevelSoft * 0.28
        ));

        float ndotl = clamp01(dot(normal, lightDir));
        vec3 viewDir = vec3(0.0, 0.0, 1.0);
        float specular = pow(clamp01(dot(reflect(-lightDir, normal), viewDir)), 42.0);

        float x = fragCoord.x / max(resolution.x, 1.0);
        float y = fragCoord.y / max(resolution.y, 1.0);
        float upperLeft = (1.0 - x) * (1.0 - y);
        float lowerRight = x * y;

        float edgeGlow = pow(bevelSoft, 1.45);
        float litEdge = edgeGlow * pow(ndotl, 1.55);
        float shadeEdge = edgeGlow * pow(clamp01(dot(normal, -lightDir)), 1.55);

        float topBand = smoothstep(0.64, 0.08, y);
        float bottomRim = smoothstep(0.54, 1.0, y) * smoothstep(0.08, 0.92, x);
        float leadingStreak = exp(-pow((x - 0.16) * 8.0, 2.0) - pow((y - 0.19) * 16.0, 2.0));
        float topStreak = exp(-pow((y - 0.13) * 28.0, 2.0)) * smoothstep(0.06, 0.18, x) * smoothstep(0.58, 0.18, x);
        float lowerWarm = exp(-pow((y - 0.82) * 9.0, 2.0)) * smoothstep(0.42, 0.96, x);

        float refraction = bevelSoft * (2.5 + press * 1.4);
        vec2 sampleCoord = fragCoord - edgeNormal * refraction;
        sampleCoord += vec2(
            sin((fragCoord.y + time * 22.0) * 0.035),
            cos((fragCoord.x - time * 18.0) * 0.026)
        ) * bevelSoft * 0.42;
        sampleCoord = clamp(sampleCoord, vec2(0.5), resolution - 0.5);

        half4 color = image.eval(sampleCoord);

        half3 coolGlass = half3(0.82, 0.89, 0.93);
        half3 whiteGlass = half3(1.0, 1.0, 1.0);
        half3 warmGlass = half3(1.0, 0.87, 0.68);
        half3 shadowTint = half3(0.47, 0.56, 0.61);
        half3 lightTint = half3(lightColor);

        float baseMist = 0.18 + topBand * 0.10 + upperLeft * 0.10;
        color = over(color, coolGlass, baseMist);
        color = over(color, whiteGlass, topBand * 0.24 + leadingStreak * 0.70 + topStreak * 0.46);
        color = over(color, lightTint, litEdge * 0.42 + specular * 0.44);
        color = over(color, warmGlass, lowerWarm * 0.22 + bottomRim * 0.09);

        float occlusion = shadeEdge * (0.11 + shadowStrength * 0.08) + lowerRight * 0.04;
        color.rgb *= half(1.0 - clamp01(occlusion));

        float hairline = exp(-pow(sd / 1.45, 2.0));
        float innerLine = exp(-pow((insideDistance - bevelWidth * 0.58) / 2.4, 2.0)) * 0.26;
        color = over(color, half3(1.0), hairline * (0.30 + ndotl * 0.24) + innerLine * topBand);
        color.rgb *= half(1.0 - hairline * shadeEdge * 0.16);

        float alpha = clamp01(mask * (0.48 + baseMist * 0.24 + edgeGlow * 0.14));
        color.a = half(max(float(color.a), alpha));
        return color * half(mask);
    }
"""
