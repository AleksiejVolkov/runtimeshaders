package com.offmind.runtimeshaders.screens

import com.offmind.runtimeshaders.navigation.Route
import com.offmind.runtimeshaders.screens.effects.BurnAfterReadingScreen
import com.offmind.runtimeshaders.screens.effects.CanvasDeformScreen
import com.offmind.runtimeshaders.screens.effects.ColorfulToggleScreen
import com.offmind.runtimeshaders.screens.effects.DualLightTextureScreen
import com.offmind.runtimeshaders.screens.effects.IlluminateUiScreen
import com.offmind.runtimeshaders.screens.effects.LampWithShadowScreen
import com.offmind.runtimeshaders.screens.effects.MetaballsShaderScreen
import com.offmind.runtimeshaders.screens.effects.NavigationTestScreen
import com.offmind.runtimeshaders.screens.effects.NeonFogButtonScreen
import com.offmind.runtimeshaders.screens.effects.SnowDialogScreen
import com.offmind.runtimeshaders.screens.effects.TapePlaneTestScreen
import com.offmind.runtimeshaders.screens.effects.TestShaderScreen
import com.offmind.runtimeshaders.screens.effects.TimerShaderScreen
import com.offmind.runtimeshaders.screens.effects.WaveshockOnTapScreen
import com.offmind.runtimeshaders.screens.effects.composeengine.DeferredReadsScreen
import com.offmind.runtimeshaders.screens.effects.composeengine.DistortionVersusScreen
import com.offmind.runtimeshaders.screens.effects.composeengine.GlowButtonsScreen
import com.offmind.runtimeshaders.screens.effects.composeengine.MetaballsVersusScreen
import com.offmind.runtimeshaders.screens.effects.composeengine.ShaderAreaScreen
import com.offmind.runtimeshaders.screens.effects.composeengine.ShaderCostScreen
import com.offmind.runtimeshaders.screens.effects.composeengine.ShadowsVersusScreen
import com.offmind.runtimeshaders.screens.effects.glass.GlassBasicsScreen
import com.offmind.runtimeshaders.screens.effects.glass.GlassSetScreen
import com.offmind.runtimeshaders.screens.effects.metalknob.MetalKnobScreen
import com.offmind.runtimeshaders.screens.effects.neumorphism.NeumorphismAccentsScreen
import com.offmind.runtimeshaders.screens.effects.neumorphism.NeumorphismBasicsScreen
import com.offmind.runtimeshaders.screens.effects.neumorphism.NeumorphismThermostatScreen

// Add an effect and its screen together in the appropriate category. Categories can nest freely;
// neither new effects nor new categories need their own Route or AppNavDisplay registration.
internal val effectsCatalog = EffectCatalog(
    root = EffectCategory(
        id = "effects",
        title = "Effects",
        description = "Explore materials, scenes and shader experiments",
        children = listOf(
            EffectCategory(
                id = "widgets",
                title = "Widgets",
                description = "Glass, soft surfaces, lighting and interactive controls",
                children = listOf(
                    EffectCategory(
                        id = "glass",
                        title = "Glass",
                        description = "Translucent surfaces and a shared glass material",
                        children = listOf(
                            EffectScreenData(
                                id = "glass-basics",
                                title = "Glass · basics",
                                description = "A pill, a circle and a card: highlights step by step",
                            ) { GlassBasicsScreen() },
                            EffectScreenData(
                                id = "glass-set",
                                title = "Glass · set",
                                description = "A whole component set from one glass shader",
                            ) { GlassSetScreen() },
                        ),
                    ),
                    EffectCategory(
                        id = "neumorphism",
                        title = "Neumorphism",
                        description = "Soft shapes, inset controls and glowing accents",
                        children = listOf(
                            EffectScreenData(
                                id = "neumorphism-basics",
                                title = "Neumorphism · basics",
                                description = "Flat, concave and pressed forms from one light",
                            ) { NeumorphismBasicsScreen() },
                            EffectScreenData(
                                id = "neumorphism-accents",
                                title = "Neumorphism · accents",
                                description = "Glowing color inside soft UI widgets",
                            ) { NeumorphismAccentsScreen() },
                            EffectScreenData(
                                id = "neumorphism-thermostat",
                                title = "Neumorphism · thermostat",
                                description = "A real screen where every element is one material",
                            ) { NeumorphismThermostatScreen() },
                        ),
                    ),
                    EffectCategory(
                        id = "lighting",
                        title = "Lighting",
                        description = "Shared light sources illuminating a whole interface",
                        children = listOf(
                            EffectScreenData(
                                id = "dual-light-texture",
                                title = "Dual Light Texture",
                                description = "Pink and blue light shared across UI elements",
                            ) { DualLightTextureScreen() },
                            EffectScreenData(
                                id = "illuminate-ui",
                                title = "Illuminate UI",
                                description = "A dashboard with shared lighting across its widgets",
                            ) { IlluminateUiScreen() },
                        ),
                    ),
                    EffectCategory(
                        id = "controls",
                        title = "Controls",
                        description = "Buttons, toggles and animated indicators",
                        children = listOf(
                            EffectScreenData(
                                id = "colorful-toggle",
                                title = "Colorful Toggle",
                                description = "A colorful shader-driven switch",
                            ) { ColorfulToggleScreen() },
                            EffectScreenData(
                                id = "neon-fog-button",
                                title = "Neon Fog Button",
                                description = "A rounded button with an expanding neon fog burst",
                            ) { NeonFogButtonScreen() },
                            EffectScreenData(
                                id = "circular-timer",
                                title = "Circular Timer",
                                description = "An animated circular countdown",
                            ) { TimerShaderScreen() },
                            EffectScreenData(
                                id = "metal-knob",
                                title = "Metal Knob",
                                description = "A brushed steel knob lit by tilting the phone",
                            ) { MetalKnobScreen() },
                        ),
                    ),
                ),
            ),
            EffectCategory(
                id = "opengl-scenes",
                title = "OpenGL scenes",
                description = "3D geometry, glass refraction and flexible surfaces",
                children = listOf(
                    EffectScreenData(
                        id = "opengl-glass",
                        title = "OpenGL glass",
                        description = "A rotating glass cube sampling the UI behind it",
                    ) { TestShaderScreen() },
                    EffectScreenData(
                        id = "tape-plane",
                        title = "Tape plane test",
                        description = "A segmented OpenGL tape plane",
                    ) { TapePlaneTestScreen() },
                ),
            ),
            EffectCategory(
                id = "visual-effects",
                title = "Visual effects",
                description = "Distortion, particles, shadows and dissolving content",
                children = listOf(
                    EffectScreenData(
                        id = "waveshock",
                        title = "Waveshock",
                        description = "A shockwave spreading from the tap point",
                    ) { WaveshockOnTapScreen() },
                    EffectScreenData(
                        id = "canvas-deform",
                        title = "Canvas deform",
                        description = "Canvas deformation controlled by dragging",
                    ) { CanvasDeformScreen() },
                    EffectScreenData(
                        id = "metaballs",
                        title = "Metaballs",
                        description = "Merging shapes through UV distortion",
                    ) { MetaballsShaderScreen() },
                    EffectScreenData(
                        id = "lamp-shadow",
                        title = "Lamp with Shadow",
                        description = "A lamp with a shadow effect",
                    ) { LampWithShadowScreen() },
                    EffectScreenData(
                        id = "snowed-dialog",
                        title = "Snowed Dialog",
                        description = "A winter dialog with falling snow",
                    ) { SnowDialogScreen() },
                    EffectScreenData(
                        id = "burn-after-reading",
                        title = "Burn After Reading",
                        description = "A note that burns away from the long-press point",
                    ) { BurnAfterReadingScreen() },
                ),
            ),
            EffectCategory(
                id = "navigation",
                title = "Navigation",
                description = "Screen flows and transitions between interfaces",
                children = listOf(
                    EffectScreenData(
                        id = "navigation-test",
                        title = "Navigation Test",
                        description = "A mock login-to-feed flow",
                    ) { onNavigate ->
                        NavigationTestScreen(onLogin = { onNavigate(Route.NavigationTestFeed) })
                    },
                ),
            ),
            EffectCategory(
                id = "compose-engine",
                title = "Compose engine",
                description = "Phases, deferred reads and what a shader costs, measured live",
                children = listOf(
                    EffectScreenData(
                        id = "engine-glow-buttons",
                        title = "Two glow buttons",
                        description = "Agent code next to the fixed version, fix by fix",
                    ) { GlowButtonsScreen() },
                    EffectScreenData(
                        id = "engine-deferred-reads",
                        title = "Deferred reads",
                        description = "One animation read in composition, placement or a layer",
                    ) { DeferredReadsScreen() },
                    EffectScreenData(
                        id = "engine-shader-area",
                        title = "Shader area",
                        description = "A button versus the whole screen; brush versus RenderEffect",
                    ) { ShaderAreaScreen() },
                    EffectScreenData(
                        id = "engine-shader-cost",
                        title = "Shader cost",
                        description = "Create, first draw and uniform update, timed on this device",
                    ) { ShaderCostScreen() },
                    EffectScreenData(
                        id = "engine-vs-metaballs",
                        title = "Metaballs · with and without a shader",
                        description = "A distance field versus blur and an alpha threshold",
                    ) { MetaballsVersusScreen() },
                    EffectScreenData(
                        id = "engine-vs-shadows",
                        title = "Soft shadows · with and without a shader",
                        description = "A neumorphic star under a moving light: distance field or blurred masks",
                    ) { ShadowsVersusScreen() },
                    EffectScreenData(
                        id = "engine-vs-distortion",
                        title = "Distortion · with and without a shader",
                        description = "A ripple over a live list: per-pixel reads or a snapshot every frame",
                    ) { DistortionVersusScreen() },
                ),
            ),
        ),
    ),
)
