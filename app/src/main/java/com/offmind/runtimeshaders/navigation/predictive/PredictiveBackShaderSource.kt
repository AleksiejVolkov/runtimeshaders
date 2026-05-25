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

internal val predictiveBackParticleDissolveShader = """
    half4 main(float2 fragCoord) {
        float p = clamp(progress / 1.7, 0.0, 1.0);
        float reveal = CubicOut(p);
        float completion = smoothstep(0.58, 1.0, p);

        float2 center = resolution * 0.5;
        float shortestSide = min(resolution.x, resolution.y);
        float2 aspect = vec2(resolution.x / shortestSide, resolution.y / shortestSide);
        float2 centered = ((fragCoord - center) / shortestSide) * aspect;
        float distanceFromCenter = length(centered);
        float maxDistance = length((resolution * 0.5 / shortestSide) * aspect);

        float ripple = Hash21(floor(fragCoord / 36.0)) * 0.035;
        float front = mix(-0.08, maxDistance + 0.28, reveal) + ripple;
        float affected = 1.0 - smoothstep(front - 0.12, front + 0.08, distanceFromCenter);

        if (affected <= 0.001) {
            return image.eval(fragCoord);
        }

        float cellSize = mix(5.8, 3.6, completion);
        float2 cell = floor(fragCoord / cellSize);
        float2 local = fract(fragCoord / cellSize);
        float seed = Hash21(cell);
        float seedB = Hash21(cell + vec2(17.0, 41.0));
        float seedC = Hash21(cell + vec2(73.0, 11.0));
        float phase = time * mix(2.15, 4.15, seedC) + seed * 6.28318;
        float2 drift = vec2(cos(phase), sin(phase * 1.27 + seedB * 6.28318));
        float shimmer = 0.5 + 0.5 * sin(phase * 1.71 + seedC * 6.28318);

        float2 cellJitter = vec2(seed - 0.5, seedB - 0.5) * 0.58;
        float2 dotCenter = vec2(0.5) + cellJitter * affected;
        dotCenter += drift * affected * (1.0 - completion) * 0.34;
        float dotDistance = length(local - dotCenter);
        float dotRadius = mix(0.43, 0.17, completion) * mix(1.0, 0.68, seedC);
        dotRadius *= mix(0.84, 1.18, shimmer * affected);
        float particle = 1.0 - smoothstep(dotRadius, dotRadius + 0.16, dotDistance);

        float2 source = (cell + dotCenter) * cellSize;
        float2 sourceCentered = ((source - center) / shortestSide) * aspect;
        float radialDistance = max(length(sourceCentered), 0.0001);
        float2 radialDirection = sourceCentered / radialDistance;
        float angle = (seed - 0.5) * 1.9;
        float2 direction = normalize(vec2(
            radialDirection.x * cos(angle) - radialDirection.y * sin(angle),
            radialDirection.x * sin(angle) + radialDirection.y * cos(angle)
        ));

        float waveAge = clamp((front - radialDistance + 0.14) / 0.42, 0.0, 1.0);
        float travel = waveAge * waveAge * mix(18.0, shortestSide * 0.34, completion);
        float2 sampleCoord = source - direction * travel / aspect;
        sampleCoord += (vec2(seedB, seedC) - 0.5) * cellSize * affected * mix(0.8, 3.2, completion);
        sampleCoord += drift * cellSize * affected * waveAge * (1.0 - completion) * 4.4;
        sampleCoord = clamp(sampleCoord, vec2(0.0), resolution);

        half4 color = image.eval(sampleCoord);
        float dust = Hash21(cell + floor(progress * 24.0));
        float sparkle = smoothstep(0.82, 1.0, dust) * affected * (1.0 - completion) * 0.28;
        float alpha = mix(1.0, particle, affected);
        alpha *= mix(1.0, mix(0.78, 1.0, shimmer), affected * (1.0 - completion));
        alpha *= mix(1.0, 1.0 - smoothstep(0.42, 1.0, waveAge) * 0.82, affected);
        alpha *= 1.0 - completion * affected * 0.72;

        color.rgb += half3(sparkle);
        return half4(color.rgb * alpha, color.a * alpha);
    }
""".trimIndent()
