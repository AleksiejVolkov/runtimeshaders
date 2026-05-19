package com.offmind.runtimeshaders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.material3.Scaffold
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.offmind.runtimeshaders.navigation.Route
import com.offmind.runtimeshaders.screens.AllEffectsListScreen
import com.offmind.runtimeshaders.screens.EffectScreenData
import com.offmind.runtimeshaders.screens.effects.*
import com.offmind.runtimeshaders.ui.theme.RuntimeShadersTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val backStack = rememberNavBackStack(Route.EffectsList)
            RuntimeShadersTheme {
                Scaffold { paddingValues ->
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        predictivePopTransitionSpec = { swipeEdge ->
                            ContentTransform(
                                targetContentEnter = EnterTransition.None,
                                initialContentExit = slideOutOfContainer(
                                    towards = when (swipeEdge) {
                                        NavigationEvent.EDGE_RIGHT -> SlideDirection.Left
                                        else -> SlideDirection.Right
                                    }
                                )
                            )
                        },
                        entryProvider = entryProvider {
                            entry<Route.EffectsList> {
                                AllEffectsListScreen(
                                    effects = effects,
                                    onEffectSelected = { backStack.add(it) }
                                )
                            }
                            entry<Route.LampShadow> {
                                LampWithShadowScreen()
                            }
                            entry<Route.Waveshock> {
                                WaveshockOnTapScreen()
                            }
                            entry<Route.SnowedDialog> {
                                SnowDialogScreen(paddingValues = paddingValues)
                            }
                            entry<Route.TestShader> {
                                TestShaderScreen(paddingValues = paddingValues)
                            }
                            entry<Route.TapePlaneTest> {
                                TapePlaneTestScreen(paddingValues = paddingValues)
                            }
                            entry<Route.CircleTimer> {
                                TimerShaderScreen(paddingValues = paddingValues)
                            }
                            entry<Route.CanvasDeform> {
                                CanvasDeformScreen(paddingValues = paddingValues)
                            }
                            entry<Route.Metaballs> {
                                MetaballsShaderScreen(paddingValues = paddingValues)
                            }
                            entry<Route.ColorfulToggle> {
                                ColorfulToggleScreen(paddingValues = paddingValues)
                            }
                        }
                    )
                }
            }
        }
    }
}

val effects = listOf(
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
        title = "Test shader",
        description = "Shader for tests",
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
    )
)
