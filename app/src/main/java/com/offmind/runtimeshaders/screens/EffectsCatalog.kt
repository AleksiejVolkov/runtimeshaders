package com.offmind.runtimeshaders.screens

import com.offmind.runtimeshaders.navigation.Route

internal val effectsCatalog = listOf(
    EffectScreenData(
        title = "Lamp with Shadow",
        description = "A lamp with a shadow effect",
        screenRoute = Route.LampShadow(
            "Lamp with Shadow",
            "A lamp with a shadow effect"
        )
    ),
    EffectScreenData(
        title = "Waveshock",
        description = "A waveshock on tap effect",
        screenRoute = Route.Waveshock(
            "Waveshock",
            "A waveshock on tap effect"
        )
    ),
    EffectScreenData(
        title = "Snowed Dialog",
        description = "",
        screenRoute = Route.SnowedDialog(
            "Christmas",
            ""
        )
    ),
    EffectScreenData(
        title = "OpenGL glass",
        description = "OpenGL Cube with glass effect",
        screenRoute = Route.TestShader(
            "Test Shader",
            "Shader for tests"
        )
    ),
    EffectScreenData(
        title = "Tape plane test",
        description = "Segmented OpenGL tape plane",
        screenRoute = Route.TapePlaneTest(
            "Tape Plane Test",
            "Segmented OpenGL tape plane"
        )
    ),
    EffectScreenData(
        title = "Circular Timer",
        description = "Shader for tests",
        screenRoute = Route.CircleTimer(
            "Test Shader",
            "Shader for tests"
        )
    ),
    EffectScreenData(
        title = "Canvas deform",
        description = "Canvas deform on drag",
        screenRoute = Route.CanvasDeform(
            "Test Shader",
            "Shader for tests"
        )
    ),
    EffectScreenData(
        title = "Metaballs",
        description = "Metaballs via uv distortion",
        screenRoute = Route.Metaballs(
            "Test Shader",
            "Shader for tests"
        )
    ),
    EffectScreenData(
        title = "Colorful Toggle",
        description = "",
        screenRoute = Route.ColorfulToggle(
            "Colorful Toggle",
            ""
        )
    ),
    EffectScreenData(
        title = "Neon Fog Button",
        description = "Rounded button with an expanding neon fog burst",
        screenRoute = Route.NeonFogButton(
            "Neon Fog Button",
            "Rounded button with an expanding neon fog burst"
        )
    ),
    EffectScreenData(
        title = "Dual Light Texture",
        description = "Pink and blue light shared across UI elements",
        screenRoute = Route.DualLightTexture(
            "Dual Light Texture",
            "Pink and blue light shared across UI elements"
        )
    ),
    EffectScreenData(
        title = "Illuminate UI",
        description = "Dashboard UI layout for lighting shader demo",
        screenRoute = Route.IlluminateUi(
            "Illuminate UI",
            "Dashboard UI layout for lighting shader demo"
        )
    ),
    EffectScreenData(
        title = "Navigation Test",
        description = "Login to feed mock flow",
        screenRoute = Route.NavigationTest(
            "Navigation Test",
            "Login to feed mock flow"
        )
    )
)
