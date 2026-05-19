package com.offmind.runtimeshaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
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
                    val backEventState = rememberNavigationEventState(
                        currentInfo = NavigationEventInfo.None,
                        backInfo = if (backStack.size > 1) {
                            listOf(NavigationEventInfo.None)
                        } else {
                            emptyList()
                        }
                    )
                    val shaderState = backEventState.toShaderState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        val previousRoute = backStack.dropLast(1).lastOrNull() as? Route

                        if (shaderState.progress > 0f && previousRoute != null) {
                            RouteContent(
                                route = previousRoute,
                                onEffectSelected = { backStack.add(it) },
                                paddingValues = paddingValues
                            )
                        }

                        PredictiveBackShaderLayer(
                            state = shaderState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            NavDisplay(
                                backStack = backStack,
                                onBack = { backStack.removeLastOrNull() },
                                entryProvider = entryProvider {
                                    entry<Route.EffectsList> { route ->
                                        RouteContent(
                                            route = route,
                                            onEffectSelected = { backStack.add(it) },
                                            paddingValues = paddingValues
                                        )
                                    }
                                    entry<Route.LampShadow> { route ->
                                        RouteContent(route, { backStack.add(it) }, paddingValues)
                                    }
                                    entry<Route.Waveshock> { route ->
                                        RouteContent(route, { backStack.add(it) }, paddingValues)
                                    }
                                    entry<Route.SnowedDialog> { route ->
                                        RouteContent(route, { backStack.add(it) }, paddingValues)
                                    }
                                    entry<Route.TestShader> { route ->
                                        RouteContent(route, { backStack.add(it) }, paddingValues)
                                    }
                                    entry<Route.TapePlaneTest> { route ->
                                        RouteContent(route, { backStack.add(it) }, paddingValues)
                                    }
                                    entry<Route.CircleTimer> { route ->
                                        RouteContent(route, { backStack.add(it) }, paddingValues)
                                    }
                                    entry<Route.CanvasDeform> { route ->
                                        RouteContent(route, { backStack.add(it) }, paddingValues)
                                    }
                                    entry<Route.Metaballs> { route ->
                                        RouteContent(route, { backStack.add(it) }, paddingValues)
                                    }
                                    entry<Route.ColorfulToggle> { route ->
                                        RouteContent(route, { backStack.add(it) }, paddingValues)
                                    }
                                }
                            )
                        }
                    }

                    NavigationBackHandler(
                        state = backEventState,
                        isBackEnabled = backStack.size > 1,
                        onBackCompleted = { backStack.removeLastOrNull() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteContent(
    route: Route,
    onEffectSelected: (Route) -> Unit,
    paddingValues: androidx.compose.foundation.layout.PaddingValues
) {
    when (route) {
        is Route.EffectsList -> AllEffectsListScreen(
            effects = effects,
            onEffectSelected = onEffectSelected
        )

        is Route.LampShadow -> LampWithShadowScreen()
        is Route.Waveshock -> WaveshockOnTapScreen()
        is Route.SnowedDialog -> SnowDialogScreen(paddingValues = paddingValues)
        is Route.TestShader -> TestShaderScreen(paddingValues = paddingValues)
        is Route.TapePlaneTest -> TapePlaneTestScreen(paddingValues = paddingValues)
        is Route.CircleTimer -> TimerShaderScreen(paddingValues = paddingValues)
        is Route.CanvasDeform -> CanvasDeformScreen(paddingValues = paddingValues)
        is Route.Metaballs -> MetaballsShaderScreen(paddingValues = paddingValues)
        is Route.ColorfulToggle -> ColorfulToggleScreen(paddingValues = paddingValues)
    }
}

private data class PredictiveBackShaderState(
    val progress: Float,
    val touch: Offset,
    val swipeEdge: Int
)

@Composable
private fun androidx.navigationevent.compose.NavigationEventState<NavigationEventInfo.None>.toShaderState(): PredictiveBackShaderState {
    val transitionState = transitionState
    val latestEvent = (transitionState as? NavigationEventTransitionState.InProgress)?.latestEvent
    val startTouchY = remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(latestEvent == null) {
        if (latestEvent == null) {
            startTouchY.value = null
        }
    }

    if (latestEvent != null && startTouchY.value == null) {
        startTouchY.value = latestEvent.touchY
    }

    return PredictiveBackShaderState(
        progress = latestEvent?.progress ?: 0f,
        touch = latestEvent?.let {
            Offset(
                x = when (it.swipeEdge) {
                    NavigationEvent.EDGE_RIGHT -> Float.POSITIVE_INFINITY
                    else -> 0f
                },
                y = startTouchY.value ?: it.touchY
            )
        } ?: Offset.Zero,
        swipeEdge = latestEvent?.swipeEdge ?: NavigationEvent.EDGE_NONE
    )
}

@Composable
private fun PredictiveBackShaderLayer(
    state: PredictiveBackShaderState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shader = remember { RuntimeShader(predictiveBackAlphaCircleShader) }

    Box(
        modifier = modifier.graphicsLayer {
            if (state.progress > 0f) {
                val centerX = if (state.touch.x.isInfinite()) size.width else state.touch.x
                compositingStrategy = CompositingStrategy.Offscreen
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("touch", centerX, state.touch.y)
                shader.setFloatUniform("progress", state.progress)
                shader.setFloatUniform("edge", state.swipeEdge.toFloat())
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            } else {
                renderEffect = null
            }
        }
    ) {
        content()
    }
}

private val predictiveBackAlphaCircleShader = """
    uniform shader image;
    uniform float2 resolution;
    uniform float2 touch;
    uniform float progress;
    uniform float edge;

    half4 main(float2 fragCoord) {
        half4 color = image.eval(fragCoord);
        float minResolution = min(resolution.x, resolution.y);
        float2 uv = (fragCoord - touch) / minResolution;

        float distanceFromTouch = length(uv);
        float radius = progress * 1.35;
        float feather = mix(0.04, 0.16, progress);
        float circle = 1.0 - smoothstep(radius - feather, radius, distanceFromTouch);
        float alphaCut = circle * smoothstep(0.0, 0.95, progress);
        float alpha = 1.0 - alphaCut;

        return half4(color.rgb * alpha, color.a * alpha);
    }
""".trimIndent()

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
