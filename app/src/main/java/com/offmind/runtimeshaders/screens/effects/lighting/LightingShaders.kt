package com.offmind.runtimeshaders.screens.effects.lighting

// AGSL allows indexing a uniform array by a for-loop induction variable as long as the
// loop bound is a compile-time constant. We bake the array sizes (MAX_LIGHTS, …) straight
// into the shader source, then iterate the whole fixed array and skip empty slots:
// unused entries are left at 0 by the Kotlin uniform setters, so `intensity <= 0` (lights)
// and `feather <= 0` (exclusions) act as natural "inactive" sentinels.

internal fun buildGlobalBloomShader(maxLights: Int, maxExclusions: Int): String = """
    // scopeOrigin converts scope-local fragCoord to root/window space, matching the
    // coordinate system of lightPositions (stored via boundsInRoot). Without it the
    // bloom center drifts by however far the scope is offset from the window origin.
    uniform vec2 scopeOrigin;
    uniform vec2 lightPositions[$maxLights];
    uniform vec3 lightColors[$maxLights];
    uniform float bloomRadii[$maxLights];        // halo size only — independent of receiver radius
    uniform float lightIntensities[$maxLights];

    // Exclusion zones (root space, xy = min corner, zw = max corner) carve the bloom out
    // of an element's bounds so a co-located light glows from under it instead of over it.
    uniform vec4 exclusionRects[$maxExclusions];
    uniform float exclusionFeather[$maxExclusions];

    vec4 main(float2 fragCoord) {
        vec4 src = image.eval(fragCoord);
        vec2 worldPos = scopeOrigin + fragCoord;

        vec3 bloom = vec3(0.0);
        for (int i = 0; i < $maxLights; i++) {
            if (lightIntensities[i] <= 0.0) continue;   // inactive slot

            float dist = length(worldPos - lightPositions[i]) / max(bloomRadii[i], 1.0);
            float falloff = 1.0 / (1.0 + 6.0 * dist * dist * dist);
            falloff *= smoothstep(2.0, 0.8, dist);

            vec3 hsv = RGBtoHSV(lightColors[i]);
            hsv.z = 1.0;                                 // vivid, pure-hue halo
            bloom += HSVtoRGB(hsv) * falloff * lightIntensities[i];
        }

        // Exclusion mask: 1.0 everywhere except inside a zone, where a box-SDF ramps it
        // down to 0.0 over `feather` pixels — a clean rounded falloff at the edges.
        float mask = 1.0;
        for (int i = 0; i < $maxExclusions; i++) {
            if (exclusionFeather[i] <= 0.0) continue;    // inactive slot

            vec2 zoneCenter = (exclusionRects[i].xy + exclusionRects[i].zw) * 0.5;
            vec2 zoneHalf   = (exclusionRects[i].zw - exclusionRects[i].xy) * 0.5;
            vec2 q = abs(worldPos - zoneCenter) - zoneHalf;
            float sdf = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0);
            mask = min(mask, smoothstep(-exclusionFeather[i], 0.0, sdf));
        }

        vec3 color = src.rgb + bloom * 0.38 * mask;
        return vec4(clamp(color, 0.0, 1.0), src.a);
    }
""".trimIndent()

internal fun buildElementReceiverShader(maxLights: Int): String = """
    uniform vec2 elementOrigin;
    uniform float strength;
    uniform vec2 lightPositions[$maxLights];
    uniform vec3 lightColors[$maxLights];
    uniform float lightRadii[$maxLights];
    uniform float lightIntensities[$maxLights];

    vec4 main(float2 fragCoord) {
        vec4 src = image.eval(fragCoord);
        vec2 worldPos = elementOrigin + fragCoord;

        // Approximate surface normal pointing away from the element's center, used so
        // only the edge facing a light receives its glare.
        vec2 outwardNormal = normalize(fragCoord - resolution * 0.5 + vec2(0.0001, 0.0));

        vec3 accumulated = vec3(0.0);   // soft surface tint (raw light color)
        vec3 glare = vec3(0.0);         // vivid directional edge highlight
        for (int i = 0; i < $maxLights; i++) {
            if (lightIntensities[i] <= 0.0) continue;   // inactive slot

            float dist = length(worldPos - lightPositions[i]) / max(lightRadii[i], 1.0);
            float falloff = 1.0 / (1.0 + 6.0 * dist * dist * dist);
            falloff *= smoothstep(2.0, 0.8, dist);

            accumulated += lightColors[i] * falloff * lightIntensities[i];

            vec3 hsv = RGBtoHSV(lightColors[i]);
            hsv.z = 1.0;
            vec2 toLight = normalize(lightPositions[i] - worldPos);
            float facing = max(0.0, dot(outwardNormal, toLight));
            glare += HSVtoRGB(hsv) * falloff * lightIntensities[i] * facing;
        }

        // Edge mask: a pixel is an "inner edge" if it is opaque but has a transparent
        // neighbor. Sampling outward at increasing distances gives a soft glow width.
        float edgeMask = 0.0;
        float weightSum = 0.0;
        for (int r = 1; r < 10; r += 2) {
            float rf = float(r);
            float minA = min(
                min(image.eval(fragCoord - vec2(rf, 0.0)).a, image.eval(fragCoord + vec2(rf, 0.0)).a),
                min(image.eval(fragCoord - vec2(0.0, rf)).a, image.eval(fragCoord + vec2(0.0, rf)).a));
            float w = 1.0 - rf / 10.0;
            edgeMask += src.a * (1.0 - minA) * w;
            weightSum += w;
        }
        edgeMask /= weightSum;

        vec2 localUv = fragCoord / resolution;
        float topRim    = 1.0 - smoothstep(0.0, 0.55, localUv.y);
        float bottomRim = smoothstep(0.45, 1.0, localUv.y);
        float edgeBoost = 0.72 + topRim * 0.52 + bottomRim * 0.52;

        vec3 color = src.rgb + accumulated * strength * edgeBoost * src.a;
        color += glare * edgeMask * 2.5;
        return vec4(clamp(color, 0.0, 1.0), src.a);
    }
""".trimIndent()
