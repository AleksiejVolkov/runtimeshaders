package com.offmind.runtimeshaders.screens.effects.lighting

// AGSL allows indexing a uniform array by a for-loop induction variable as long as the
// loop bound is a compile-time constant. We bake the array size (MAX_LIGHTS) straight into
// the shader source, then iterate the whole fixed array and skip empty slots: unused
// entries are left at 0 by the Kotlin uniform setters, so `intensity <= 0` acts as a
// natural "inactive light" sentinel.

internal fun buildGlobalBloomShader(maxLights: Int): String = """
    // scopeOrigin converts scope-local fragCoord to root/window space, matching the
    // coordinate system of lightPositions (stored via boundsInRoot). Without it the
    // bloom center drifts by however far the scope is offset from the window origin.
    uniform vec2 scopeOrigin;
    uniform vec2 lightPositions[$maxLights];
    uniform vec3 lightColors[$maxLights];
    uniform float bloomIntensities[$maxLights];  // visible-halo strength per light

    vec4 main(float2 fragCoord) {
        vec4 src = image.eval(fragCoord);
        vec2 worldPos = scopeOrigin + fragCoord;
        // resolution is the scope size, so its length is the same scene diagonal the
        // receiver shader normalizes by — the halo therefore matches the lit field exactly.
        float sceneScale = max(length(resolution), 1.0);

        vec3 bloom = vec3(0.0);
        for (int i = 0; i < $maxLights; i++) {
            if (bloomIntensities[i] <= 0.0) continue;   // inactive slot

            // The exact same falloff field the receivers see, painted in the light color.
            float nd = length(worldPos - lightPositions[i]) / sceneScale;
            float falloff = exp(-5.0 * nd);
            bloom += lightColors[i] * falloff * bloomIntensities[i];
        }

        vec3 color = src.rgb + bloom;
        return vec4(clamp(color, 0.0, 1.0), src.a);
    }
""".trimIndent()

internal fun buildElementReceiverShader(maxLights: Int): String = """
    uniform vec2 elementOrigin;
    uniform float strength;
    uniform float sceneScale;       // scene diagonal in px — normalizes distance to 0..~1
    uniform vec2 lightPositions[$maxLights];
    uniform vec3 lightColors[$maxLights];
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

            // Distance normalized by the scene size (0 at the light, ~1 across the
            // screen). Exponential falloff has no flat region near the source, so a
            // close receiver and a far one read very differently — unlike a polynomial,
            // which stays near 1.0 for a wide band around the light. The 5.0 is the knob:
            // larger = tighter pool of light (more near/far contrast), smaller = reaches
            // farther. intensity then scales overall brightness on top.
            float nd = length(worldPos - lightPositions[i]) / sceneScale;
            float falloff = exp(-5.0 * nd);

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
