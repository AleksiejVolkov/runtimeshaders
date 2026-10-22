package com.offmind.runtimeshaders.screens.effects.metalknob

import org.intellij.lang.annotations.Language

// Scene units: 1.0 is the knob radius. Everything is lit by `lightDir` (screen space, y down)
// plus a weaker fill light turned 90° around the view axis, so the brushed face shows four lobes.
@Language("AGSL")
internal val metalKnobShader = """
    const float PI = 3.14159265;
    const float TICK_SWEEP = 4.71238898;     // 270°
    const float TICK_COUNT = 54.0;           // intervals, 55 marks
    const float FACE_RADIUS = 0.905;
    const float PLATFORM_RADIUS = 1.26;
    const vec3 BACKGROUND = vec3(0.918, 0.918, 0.928);
    const vec3 STEEL = vec3(0.80, 0.815, 0.84);

    float hash11(float n) {
        return fract(sin(n * 127.1 + 11.3) * 43758.5453);
    }

    // Kajiya-Kay: brushed grooves reflect along a cone around their tangent, which turns
    // circular brushing into the bright wedges that rotate with the light.
    float brushed(vec3 tangent, vec3 l, float power) {
        vec3 h = normalize(l + vec3(0.0, 0.0, 1.0));
        float th = dot(tangent, h);
        return pow(sqrt(max(0.0, 1.0 - th * th)), power);
    }

    float ring(float r, float center, float width) {
        float d = (r - center) / width;
        return exp(-d * d);
    }

    vec3 tickMarks(vec3 col, vec2 p, float r, float a, float px) {
        float ta = a + TICK_SWEEP * 0.5;
        float stepA = TICK_SWEEP / TICK_COUNT;
        float index = floor(ta / stepA + 0.5);
        if (index < 0.0 || index > TICK_COUNT) return col;

        bool major = mod(index, 6.0) < 0.5;
        float halfWidth = (major ? 1.3 : 0.75) * px;
        float across = abs(ta - index * stepA) * r;
        float inner = major ? 1.36 : 1.40;
        float outer = major ? 1.49 : 1.46;
        float mask = (1.0 - smoothstep(halfWidth - px * 0.5, halfWidth + px * 0.5, across))
            * smoothstep(inner - px, inner + px, r)
            * (1.0 - smoothstep(outer - px, outer + px, r));
        float active = step(index, floor(knobValue * TICK_COUNT + 0.5));
        vec3 ink = mix(vec3(0.66, 0.66, 0.68), vec3(0.16, 0.16, 0.18), active);
        return mix(col, ink, mask);
    }

    vec3 metalFace(vec2 p, float r, vec2 tangential, vec3 l, vec3 fill) {
        // Every ring of the lathe gets its own random brightness.
        float ringId = floor(r * knobRadius / 1.15);
        float grain = hash11(ringId);

        // The face is still flat, but its grooves run around the centre.
        vec3 n = vec3(0.0, 0.0, 1.0);
        vec3 t = vec3(tangential, 0.0);

        float spec = brushed(t, l, 100.0) + 0.45 * brushed(t, fill, 100.0);
        float sheen = 0.38 * brushed(t, l, 7.0) + 0.16 * brushed(t, fill, 7.0);
        float diffuse = max(dot(n, l), 0.0);

        vec3 col = STEEL * (0.30 + 0.28 * diffuse + sheen);
        col *= 0.955 + 0.09 * grain;
        col += vec3(0.95, 0.97, 1.0) * spec * 0.48;

        // Indicator notch.
        vec2 dir = vec2(sin(knobAngle), -cos(knobAngle));
        vec2 perp = vec2(cos(knobAngle), sin(knobAngle));
        float along = dot(p, dir);
        float across = dot(p, perp);
        float halfWidth = 0.016;
        float notch = smoothstep(0.585, 0.595, along) * (1.0 - smoothstep(0.735, 0.745, along))
            * (1.0 - smoothstep(halfWidth - 0.004, halfWidth, abs(across)));
        vec2 wallNormal = -sign(across) * perp;
        float wallLight = max(dot(wallNormal, normalize(l.xy + vec2(1e-4))), 0.0);
        float wall = smoothstep(halfWidth * 0.35, halfWidth * 0.9, abs(across));
        col = mix(col, vec3(0.16, 0.165, 0.18) + wall * wallLight * 0.55, notch);

        // Dark machined gap where the face meets the rim.
        col *= 1.0 - 0.75 * ring(r, FACE_RADIUS, 0.006);
        return col;
    }

    vec3 chromeRim(float r) {
        return vec3(0.42, 0.43, 0.46);
    }

    half4 main(vec2 fragCoord) {
        vec2 p = (fragCoord - resolution * 0.5) / knobRadius;
        float r = length(p);
        float px = 1.0 / knobRadius;
        vec2 radial = r > 1e-4 ? p / r : vec2(0.0, -1.0);
        vec2 tangential = vec2(-radial.y, radial.x);
        float a = atan(p.x, -p.y);      // 0 at the top, clockwise

        vec3 l = normalize(lightDir);
        vec3 fill = normalize(vec3(-l.y, l.x, l.z * 1.4));
        vec2 shadowShift = l.xy / max(l.z, 0.2);

        // Background with the soft shadow of the raised platform.
        vec3 col = BACKGROUND;
        float platformShadow = 1.0 - smoothstep(PLATFORM_RADIUS - 0.04, PLATFORM_RADIUS + 0.16,
            length(p + shadowShift * 0.035));
        col *= 1.0 - 0.10 * platformShadow;
        col = tickMarks(col, p, r, a, px);

        // Platform: flat disc with a rounded edge lit from the light side.
        float platformMask = 1.0 - smoothstep(PLATFORM_RADIUS - px, PLATFORM_RADIUS + px, r);
        float edge = smoothstep(PLATFORM_RADIUS - 0.06, PLATFORM_RADIUS, r);
        vec3 platformNormal = normalize(vec3(radial * edge * 1.6, 1.0));
        float platformLight = dot(platformNormal, l) / l.z;
        vec3 platform = vec3(0.94, 0.94, 0.95) * clamp(0.93 + 0.07 * platformLight, 0.8, 1.06);

        // Knob drop shadow and contact occlusion on the platform.
        vec2 knobShadowShift = shadowShift * 0.11;
        float shift = length(knobShadowShift);
        if (shift > 0.22) knobShadowShift *= 0.22 / shift;
        float knobShadow = 1.0 - smoothstep(0.9, 1.22, length(p + knobShadowShift));
        float contact = 1.0 - smoothstep(1.0, 1.07, r);
        platform *= (1.0 - 0.42 * knobShadow) * (1.0 - 0.22 * contact);
        col = mix(col, platform, platformMask);

        // The knob itself.
        vec3 knob = r < FACE_RADIUS + 0.01
            ? metalFace(p, r, tangential, l, fill)
            : chromeRim(r);
        float knobMask = 1.0 - smoothstep(1.0 - px, 1.0 + px, r);
        knob *= 1.0 - 0.5 * ring(r, 0.995, 0.008);
        col = mix(col, knob, knobMask);

        return half4(col, 1.0);
    }
""".trimIndent()
