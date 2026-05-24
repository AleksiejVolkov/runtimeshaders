package com.offmind.runtimeshaders.navigation.predictive

internal val predictiveBackHumpMaskShader = """
    half4 main(float2 fragCoord) {
        half4 color = image.eval(fragCoord);
        float completion = smoothstep(1.0, 1.7, progress);
        float easedProgress = mix(CubicOut(clamp(progress, 0.0, 1.0)), 1.65, completion);
        float edgeSign = edge < 0.5 ? 1.0 : -1.0;
        float fromEdge = edge < 0.5 ? fragCoord.x : resolution.x - fragCoord.x;

        float verticalDistance = abs(fragCoord.y - touch.y) / resolution.y;
        float verticalRange = mix(0.46, 1.35, completion);
        float verticalPower = mix(2.4, 0.62, completion);
        float verticalProfile = 1.0 - smoothstep(0.0, verticalRange, verticalDistance);
        verticalProfile = pow(verticalProfile, verticalPower);

        float maxReach = resolution.x * mix(0.34, 1.45, completion) * easedProgress;
        float waveFront = maxReach * verticalProfile;
        float alphaFeather = mix(2.5, 7.0, easedProgress);
        float mask = 1.0 - smoothstep(waveFront - alphaFeather, waveFront + alphaFeather, fromEdge);
        mask *= smoothstep(0.0, 0.08, easedProgress);

        float2 anchor = vec2(
            edge < 0.5 ? waveFront : resolution.x - waveFront,
            touch.y
        );
        float2 toAnchor = anchor - fragCoord;
        float2 normalizedToAnchor = vec2(
            toAnchor.x / resolution.x,
            toAnchor.y / resolution.y
        );
        float pullDistance = length(normalizedToAnchor);
        float pullInfluence = 1.0 - smoothstep(0.0, mix(0.55, 1.15, completion), pullDistance);
        pullInfluence *= smoothstep(0.0, 0.12, easedProgress);
        pullInfluence *= edge < 0.5
            ? 1.0 - smoothstep(anchor.x, resolution.x, fragCoord.x)
            : smoothstep(0.0, anchor.x, fragCoord.x);
        float pullStrength = mix(0.18, 0.55, completion) * easedProgress * pullInfluence;
        float2 sampleCoord = fragCoord;
        sampleCoord -= toAnchor * pullStrength;
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
