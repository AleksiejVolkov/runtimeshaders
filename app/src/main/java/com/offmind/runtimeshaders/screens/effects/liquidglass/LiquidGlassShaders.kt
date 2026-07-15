package com.offmind.runtimeshaders.screens.effects.liquidglass

/**
 * Liquid-glass material shader. Receives the *backdrop* region (what lies behind the glass
 * element) as the `image` input — already blurred by a chained [android.graphics.RenderEffect]
 * blur pass — and applies:
 *
 * 1. Edge refraction: fragments inside a band along the rounded-rect rim sample the backdrop
 *    displaced along the SDF normal, bending the content like the edge of a convex lens.
 * 2. Vibrancy: a slight saturation boost of the sampled backdrop.
 * 3. Material tint: a translucent wash over the glass.
 * 4. Rim lighting: specular highlights on the top-left and bottom-right edges.
 *
 * Expected uniforms (declared by the `Shader` wrapper):
 * `image`, `resolution`, `cornerRadius`, `refractionHeight`, `refractionAmount`,
 * `tint` (vec4 rgba), `vibrancy`.
 */
internal val liquidGlassShaderSource = """
    float sdRoundedBox(vec2 p, vec2 halfSize, float radius) {
        vec2 q = abs(p) - halfSize + radius;
        return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - radius;
    }

    vec2 sdRoundedBoxNormal(vec2 p, vec2 halfSize, float radius) {
        float dx = sdRoundedBox(p + vec2(1.0, 0.0), halfSize, radius)
                 - sdRoundedBox(p - vec2(1.0, 0.0), halfSize, radius);
        float dy = sdRoundedBox(p + vec2(0.0, 1.0), halfSize, radius)
                 - sdRoundedBox(p - vec2(0.0, 1.0), halfSize, radius);
        return normalize(vec2(dx, dy));
    }

    half4 main(vec2 fragCoord) {
        vec2 halfSize = resolution * 0.5;
        vec2 p = fragCoord - halfSize;
        float sd = sdRoundedBox(p, halfSize, cornerRadius);

        // 0 outside the refraction band -> 1 at the very rim.
        float band = max(refractionHeight, 1.0);
        float t = clamp(1.0 + sd / band, 0.0, 1.0);
        float eased = t * t * (3.0 - 2.0 * t);

        vec2 normal = vec2(0.0);
        vec2 sampleCoord = fragCoord;
        if (t > 0.0) {
            normal = sdRoundedBoxNormal(p, halfSize, cornerRadius);
            sampleCoord = fragCoord - normal * (refractionAmount * eased);
        }
        sampleCoord = clamp(sampleCoord, vec2(0.5), resolution - 0.5);

        vec4 color = vec4(image.eval(sampleCoord));

        float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        color.rgb = mix(vec3(luma), color.rgb, 1.0 + vibrancy);

        color.rgb = mix(color.rgb, tint.rgb, tint.a);

        if (t > 0.0) {
            float topLight = clamp(dot(normal, normalize(vec2(-0.5, -0.85))), 0.0, 1.0);
            float bottomLight = clamp(dot(normal, normalize(vec2(0.5, 0.85))), 0.0, 1.0);
            float rim = pow(t, 3.0);
            color.rgb += vec3(rim * (topLight * 0.55 + bottomLight * 0.28));
        }

        return half4(color);
    }
"""
