package com.offmind.runtimeshaders.navigation.predictive

internal val predictiveBackHumpMaskShader = """
    half4 main(float2 fragCoord) {
        half4 color = image.eval(fragCoord);
        float completion = smoothstep(1.0, 1.7, progress);
        float easedProgress = mix(CubicOut(clamp(progress, 0.0, 1.0)), 1.65, completion);
        float edgeSign = edge < 0.5 ? 1.0 : -1.0;
        float fromEdge = edge < 0.5 ? fragCoord.x : resolution.x - fragCoord.x;
        float normalizedFromEdge = fromEdge / resolution.x;

        float verticalDistance = abs(fragCoord.y - touch.y) / resolution.y;
        float verticalRange = mix(0.46, 1.35, completion);
        float verticalPower = mix(2.4, 0.62, completion);
        float verticalProfile = 1.0 - smoothstep(0.0, verticalRange, verticalDistance);
        verticalProfile = pow(verticalProfile, verticalPower);

        float maxReach = resolution.x * mix(0.31, 1.45, completion) * easedProgress;
        float waveFront = maxReach * verticalProfile;
        float alphaFeather = mix(2.5, 7.0, easedProgress);
        float mask = 1.0 - smoothstep(waveFront - alphaFeather, waveFront + alphaFeather, fromEdge);
        mask *= smoothstep(0.0, 0.08, easedProgress);

        float2 sampleCoord = fragCoord;
        float edgeInfluence = 1.0 - smoothstep(0.02, mix(0.82, 1.0, completion), normalizedFromEdge);
        edgeInfluence = pow(edgeInfluence, mix(1.55, 0.72, completion));
        float waist = verticalProfile * smoothstep(0.0, 0.14, easedProgress);
        float horizontalWarp = waveFront * edgeInfluence * mix(0.62, 0.18, completion);
        sampleCoord.x -= edgeSign * horizontalWarp;

        float verticalDirection = sign(fragCoord.y - touch.y);
        float verticalRelaxation = (1.0 - verticalProfile) * waist * edgeInfluence;
        verticalRelaxation *= resolution.y * mix(0.018, 0.006, completion) * easedProgress;
        sampleCoord.y -= verticalDirection * verticalRelaxation;
        sampleCoord = clamp(sampleCoord, vec2(0.0), resolution);

        half4 warpedColor = image.eval(sampleCoord);
        float alpha = 1.0 - mask;
        float shadowDistance = waveFront - fromEdge;
        float shadowBand = 1.0 - smoothstep(-alphaFeather * 2.0, alphaFeather * 22.0, shadowDistance);
        float softShadow = shadowBand * shadowBand * (3.0 - 2.0 * shadowBand);
        float shadow = softShadow * mask * (1.0 - completion) * 0.16;
        float finalAlpha = max(warpedColor.a * alpha, shadow);

        return half4(warpedColor.rgb * alpha, finalAlpha);
    }
""".trimIndent()
