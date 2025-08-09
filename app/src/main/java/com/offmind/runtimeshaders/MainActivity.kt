package com.offmind.runtimeshaders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            val navController = rememberNavController()
            RuntimeShadersTheme {
                Scaffold { paddingValues ->

                    NavHost(navController, startDestination = Route.EffectsList) {
                        composable<Route.EffectsList> {
                            AllEffectsListScreen(
                                paddingValues = paddingValues,
                                effects = effects,
                                navController = navController
                            )
                        }
                        composable<Route.LampShadow> {
                            LampWithShadowScreen()
                        }
                        composable<Route.Waveshock> {
                            WaveshockOnTapScreen()
                        }
                        composable<Route.SnowedDialog> {
                            SnowDialogScreen(paddingValues = paddingValues)
                        }
                        composable<Route.TestShader> {
                            TestShaderScreen(paddingValues = paddingValues)
                        }
                        composable<Route.CircleTimer> {
                            TimerShaderScreen(paddingValues = paddingValues)
                        }
                        composable<Route.CanvasDeform> {
                            CanvasDeformScreen(paddingValues = paddingValues)
                        }
                        composable<Route.Metaballs> {
                            MetaballsShaderScreen(paddingValues = paddingValues)
                        }
                    }
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
    )
)