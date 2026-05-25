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
        float reveal = mix(smoothstep(0.0, 1.0, p), CubicOut(p), 0.18);
        float completion = smoothstep(0.58, 1.0, p);

        float shortestSide = min(resolution.x, resolution.y);
        float2 screenAspect = vec2(resolution.x / shortestSide, resolution.y / shortestSide);
        float2 aspect = mix(vec2(1.0), screenAspect, 0.28);
        float2 origin = vec2(clamp(touch.x, 0.0, resolution.x), clamp(touch.y, 0.0, resolution.y));
        float2 centered = ((fragCoord - origin) / shortestSide) * aspect;
        float distanceFromOrigin = length(centered);
        float2 cornerA = ((vec2(0.0, 0.0) - origin) / shortestSide) * aspect;
        float2 cornerB = ((vec2(resolution.x, 0.0) - origin) / shortestSide) * aspect;
        float2 cornerC = ((vec2(0.0, resolution.y) - origin) / shortestSide) * aspect;
        float2 cornerD = ((resolution - origin) / shortestSide) * aspect;
        float maxDistance = max(max(length(cornerA), length(cornerB)), max(length(cornerC), length(cornerD)));

        float ripple = (Hash21(floor(fragCoord / 28.0)) - 0.5) * mix(0.018, 0.095, p);
        float front = mix(-0.10, maxDistance + 0.16, reveal) + ripple;
        float bandWidth = mix(0.045, 0.52, smoothstep(0.0, 0.96, p));
        float affected = 1.0 - smoothstep(front - bandWidth * 1.35, front + bandWidth * 0.96, distanceFromOrigin);

        if (affected <= 0.001) {
            return image.eval(fragCoord);
        }

        float cellSize = mix(5.8, 3.6, completion);
        float2 cell = floor(fragCoord / cellSize);
        float2 local = fract(fragCoord / cellSize);
        float seed = Hash21(cell);
        float seedB = Hash21(cell + vec2(17.0, 41.0));
        float seedC = Hash21(cell + vec2(73.0, 11.0));
        float phase = time * mix(4.4, 8.6, seedC) + seed * 6.28318;
        float2 drift = vec2(cos(phase), sin(phase * 1.27 + seedB * 6.28318));
        float shimmer = 0.5 + 0.5 * sin(phase * 1.71 + seedC * 6.28318);

        float2 cellJitter = vec2(seed - 0.5, seedB - 0.5) * 0.58;
        float2 dotCenter = vec2(0.5) + cellJitter * affected;
        dotCenter += drift * affected * (1.0 - completion * 0.35) * mix(0.34, 0.72, p);
        float dotDistance = length(local - dotCenter);
        float dotRadius = mix(0.43, 0.17, completion) * mix(1.0, 0.68, seedC);
        dotRadius *= mix(0.74, 1.32, shimmer * affected);
        float particle = 1.0 - smoothstep(dotRadius, dotRadius + 0.16, dotDistance);

        float2 source = (cell + dotCenter) * cellSize;
        float2 sourceCentered = ((source - origin) / shortestSide) * aspect;
        float radialDistance = max(length(sourceCentered), 0.0001);
        float2 radialDirection = sourceCentered / radialDistance;
        float angle = (seed - 0.5) * mix(2.3, 4.8, p) + sin(phase * 0.7) * 0.42;
        float2 direction = normalize(vec2(
            radialDirection.x * cos(angle) - radialDirection.y * sin(angle),
            radialDirection.x * sin(angle) + radialDirection.y * cos(angle)
        ));

        float waveAge = clamp((front - radialDistance + bandWidth * 1.18) / (bandWidth * 3.15 + 0.001), 0.0, 1.0);
        float travel = waveAge * waveAge * mix(shortestSide * 0.07, shortestSide * 0.48, p);
        float2 sampleCoord = source - direction * travel / aspect;
        sampleCoord += (vec2(seedB, seedC) - 0.5) * cellSize * affected * mix(2.0, 7.4, p);
        sampleCoord += drift * cellSize * affected * waveAge * (1.0 - completion * 0.25) * mix(7.0, 16.0, p);
        sampleCoord = clamp(sampleCoord, vec2(0.0), resolution);

        half4 color = image.eval(sampleCoord);
        float dust = Hash21(cell + floor(progress * 24.0));
        float sparkle = smoothstep(0.78, 1.0, dust) * affected * (1.0 - completion * 0.55) * 0.34;
        float tintMask = affected * particle * (0.38 + shimmer * 0.42) * (1.0 - completion * 0.35);
        half3 tintA = half3(0.22, 0.66, 1.00);
        half3 tintB = half3(1.00, 0.34, 0.82);
        half3 tint = mix(tintA, tintB, half(seedB));
        color.rgb = mix(color.rgb, color.rgb + tint * 0.34, half(tintMask));
        float alpha = mix(1.0, particle, affected);
        alpha *= mix(1.0, mix(0.62, 1.0, shimmer), affected * (1.0 - completion * 0.25));
        alpha *= mix(1.0, 1.0 - smoothstep(0.50, 1.0, waveAge) * 0.82, affected);
        alpha *= 1.0 - completion * affected * 0.72;

        color.rgb += half3(sparkle);
        return half4(color.rgb * alpha, color.a * alpha);
    }
""".trimIndent()

internal val predictiveBackLiquidDrainShader = """
    half4 main(float2 fragCoord) {
        float p = clamp(progress / 1.7, 0.0, 1.0);
        float eased = CubicOut(p);
        float completion = smoothstep(0.62, 1.0, p);
        float2 uv = fragCoord / resolution;

        float column = floor(uv.x * 42.0);
        float wideColumn = floor(uv.x * 13.0);
        float seed = Hash21(vec2(column, 7.0));
        float seedB = Hash21(vec2(column, 31.0));
        float wideSeed = Hash21(vec2(wideColumn, 19.0));

        float phase = time * mix(1.4, 2.8, seedB) + seed * 6.28318;
        float wobble = sin(phase + uv.y * mix(5.0, 10.0, seed)) * 0.5 + 0.5;
        float sideWave = sin(uv.y * 18.0 + time * 1.15 + wideSeed * 6.28318);

        float gravity = eased * eased;
        float bottomWeight = smoothstep(-0.08, 0.92, uv.y);
        float strand = mix(0.55, 1.35, seed) * mix(0.72, 1.18, wideSeed);
        float drain = gravity * resolution.y * (0.10 + bottomWeight * 0.82) * strand;
        drain += wobble * gravity * resolution.y * 0.055;

        float2 sampleCoord = fragCoord;
        sampleCoord.y -= drain;
        sampleCoord.x += sideWave * gravity * mix(2.0, 18.0, bottomWeight) * mix(0.45, 1.0, seedB);
        sampleCoord.x += sin(time * 1.7 + uv.x * 26.0) * gravity * 2.4;
        sampleCoord = clamp(sampleCoord, vec2(0.0), resolution);

        half4 color = image.eval(sampleCoord);

        float dripFront = eased * 1.18 - uv.y + (seed - 0.5) * 0.18 + wobble * 0.07;
        float topOpening = smoothstep(-0.10, 0.24, dripFront);
        float verticalTear = smoothstep(0.82, 1.0, wobble) * smoothstep(0.10, 0.82, eased);
        float thinGap = smoothstep(0.92, 1.0, Hash21(vec2(column, floor(time * 9.0)))) * verticalTear;

        float alpha = 1.0 - topOpening * mix(0.24, 0.88, completion);
        alpha *= 1.0 - thinGap * topOpening * 0.35;
        alpha *= 1.0 - completion * smoothstep(0.25, 1.0, uv.y) * 0.64;

        float shine = smoothstep(0.78, 1.0, wobble) * gravity * (1.0 - completion) * 0.08;
        color.rgb += half3(shine);

        return half4(color.rgb * alpha, color.a * alpha);
    }
""".trimIndent()
