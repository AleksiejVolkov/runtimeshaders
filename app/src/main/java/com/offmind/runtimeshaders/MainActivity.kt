package com.offmind.runtimeshaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import com.offmind.runtimeshaders.generated.ShaderFunction
import com.offmind.runtimeshaders.navigation.Route
import com.offmind.runtimeshaders.screens.AllEffectsListScreen
import com.offmind.runtimeshaders.screens.EffectScreenData
import com.offmind.runtimeshaders.screens.effects.*
import com.offmind.runtimeshaders.shaders.Shader
import com.offmind.runtimeshaders.shaders.Uniform
import com.offmind.runtimeshaders.ui.theme.RuntimeShadersTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val backStack = rememberNavBackStack(Route.EffectsList)
            RuntimeShadersTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val backEventState = rememberNavigationEventState(
                        currentInfo = NavigationEventInfo.None,
                        backInfo = if (backStack.size > 1) {
                            listOf(NavigationEventInfo.None)
                        } else {
                            emptyList()
                        }
                    )
                    var gestureSession by remember { mutableIntStateOf(0) }
                    var activeUnderlayRoute by remember { mutableStateOf<Route?>(null) }
                    val terminalProgress = remember { Animatable(0f) }
                    val scope = rememberCoroutineScope()
                    val gestureShaderState = backEventState.toShaderState(resetToken = gestureSession)

                    LaunchedEffect(gestureShaderState.progress) {
                        if (gestureShaderState.progress > 0f) {
                            if (activeUnderlayRoute == null) {
                                activeUnderlayRoute = backStack.dropLast(1).lastOrNull() as? Route
                            }
                            terminalProgress.snapTo(gestureShaderState.progress)
                        }
                    }

                    val shaderState = gestureShaderState.copy(
                        progress = maxOf(gestureShaderState.progress, terminalProgress.value)
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        val underlayRoute = if (shaderState.progress > 0f) {
                            activeUnderlayRoute ?: backStack.dropLast(1).lastOrNull() as? Route
                        } else {
                            null
                        }

                        if (underlayRoute != null) {
                            RouteContent(
                                route = underlayRoute,
                                onEffectSelected = { backStack.add(it) }
                            )
                        }

                        PredictiveBackShaderLayer(
                            state = shaderState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            NavDisplay(
                                backStack = backStack,
                                onBack = { backStack.removeLastOrNull() },
                                popTransitionSpec = { noNavDisplayTransition },
                                predictivePopTransitionSpec = { noNavDisplayTransition },
                                entryProvider = entryProvider {
                                    entry<Route.EffectsList> { route ->
                                        RouteContent(
                                            route = route,
                                            onEffectSelected = { backStack.add(it) }
                                        )
                                    }
                                    entry<Route.LampShadow> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.Waveshock> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.SnowedDialog> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.TestShader> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.TapePlaneTest> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.CircleTimer> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.CanvasDeform> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.Metaballs> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.ColorfulToggle> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.NavigationTest> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                    entry<Route.NavigationTestFeed> { route ->
                                        RouteContent(route, { backStack.add(it) })
                                    }
                                }
                            )
                        }
                    }

                    NavigationBackHandler(
                        state = backEventState,
                        isBackEnabled = backStack.size > 1,
                        onBackCancelled = {
                            scope.launch {
                                terminalProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = BACK_CANCEL_DURATION_MS)
                                )
                                activeUnderlayRoute = null
                                gestureSession++
                            }
                        },
                        onBackCompleted = {
                            scope.launch {
                                terminalProgress.animateTo(
                                    targetValue = BACK_COMPLETE_PROGRESS,
                                    animationSpec = tween(durationMillis = BACK_COMPLETE_DURATION_MS)
                                )
                                delay(BACK_POP_DELAY_MS)
                                backStack.removeLastOrNull()
                                repeat(BACK_RESET_FRAME_DELAY) {
                                    withFrameNanos { }
                                }
                                terminalProgress.snapTo(0f)
                                activeUnderlayRoute = null
                                gestureSession++
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteContent(
    route: Route,
    onEffectSelected: (Route) -> Unit
) {
    when (route) {
        is Route.EffectsList -> AllEffectsListScreen(
            effects = effects,
            onEffectSelected = onEffectSelected
        )

        is Route.LampShadow -> LampWithShadowScreen()
        is Route.Waveshock -> WaveshockOnTapScreen()
        is Route.SnowedDialog -> SnowDialogScreen()
        is Route.TestShader -> TestShaderScreen()
        is Route.TapePlaneTest -> TapePlaneTestScreen()
        is Route.CircleTimer -> TimerShaderScreen()
        is Route.CanvasDeform -> CanvasDeformScreen()
        is Route.Metaballs -> MetaballsShaderScreen()
        is Route.ColorfulToggle -> ColorfulToggleScreen()
        is Route.NavigationTest -> NavigationTestScreen(
            onLogin = {
                onEffectSelected(
                    Route.NavigationTestFeed(
                        "Navigation Test Feed",
                        "Mock feed"
                    )
                )
            }
        )
        is Route.NavigationTestFeed -> NavigationTestFeedScreen()
    }
}

private data class PredictiveBackShaderState(
    val progress: Float,
    val touch: Offset,
    val swipeEdge: Int
)

@Composable
private fun androidx.navigationevent.compose.NavigationEventState<NavigationEventInfo.None>.toShaderState(
    resetToken: Int
): PredictiveBackShaderState {
    val transitionState = transitionState
    val latestEvent = (transitionState as? NavigationEventTransitionState.InProgress)?.latestEvent
    val startTouchY = remember { mutableStateOf<Float?>(null) }
    val lastTouch = remember { mutableStateOf(Offset.Zero) }
    val lastSwipeEdge = remember { mutableIntStateOf(NavigationEvent.EDGE_NONE) }

    LaunchedEffect(resetToken) {
        startTouchY.value = null
        lastTouch.value = Offset.Zero
        lastSwipeEdge.intValue = NavigationEvent.EDGE_NONE
    }

    if (latestEvent != null && startTouchY.value == null) {
        startTouchY.value = latestEvent.touchY
    }
    if (latestEvent != null) {
        val touch = Offset(
            x = when (latestEvent.swipeEdge) {
                NavigationEvent.EDGE_RIGHT -> Float.POSITIVE_INFINITY
                else -> 0f
            },
            y = startTouchY.value ?: latestEvent.touchY
        )
        lastTouch.value = touch
        lastSwipeEdge.intValue = latestEvent.swipeEdge
    }

    return PredictiveBackShaderState(
        progress = latestEvent?.progress ?: 0f,
        touch = lastTouch.value,
        swipeEdge = lastSwipeEdge.intValue
    )
}

@Composable
private fun PredictiveBackShaderLayer(
    state: PredictiveBackShaderState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shader = remember {
        Shader(predictiveBackHumpMaskShader).getRuntimeShader(
            uniforms = listOf(
                Uniform(Uniform.Type.SHADER, "image"),
                Uniform(Uniform.Type.VEC2, "resolution"),
                Uniform(Uniform.Type.VEC2, "touch"),
                Uniform(Uniform.Type.FLOAT, "progress"),
                Uniform(Uniform.Type.FLOAT, "edge")
            ),
            customFunctions = setOf(
                ShaderFunction.CUBICOUT,
                ShaderFunction.HASH21
            )
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            if (state.progress > 0f) {
                val centerX = if (state.touch.x.isInfinite()) size.width else state.touch.x
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = 1f - ((state.progress - 1f) / (BACK_COMPLETE_PROGRESS - 1f))
                    .coerceIn(0f, 1f)
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("touch", centerX, state.touch.y)
                shader.setFloatUniform("progress", state.progress)
                shader.setFloatUniform("edge", state.swipeEdge.toFloat())
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            } else {
                alpha = 1f
                renderEffect = null
            }
        }
    ) {
        content()
    }
}

private val predictiveBackAlphaCircleShader = """
    half4 main(float2 fragCoord) {
        half4 color = image.eval(fragCoord);
        float minResolution = min(resolution.x, resolution.y);
        float2 uv = (fragCoord - touch) / minResolution;
        float easedProgress = CubicOut(progress);

        float distanceFromTouch = length(uv);
        float radius = easedProgress * 1.85;
        float feather = mix(0.04, 0.16, easedProgress);
        float circleCore = 1.0 - smoothstep(radius - feather, radius, distanceFromTouch);
        float edgeBand = smoothstep(radius - feather * 1.4, radius - feather * 0.2, distanceFromTouch)
            * (1.0 - smoothstep(radius - feather * 0.15, radius + feather * 0.9, distanceFromTouch));
        vec2 particleCell = floor((fragCoord - touch) / max(3.0, minResolution * 0.008));
        float particle = step(0.40 + edgeBand * 0.32, Hash21(particleCell));
        float circle = mix(circleCore, circleCore * particle, edgeBand);
        float alphaCut = circle * smoothstep(0.0, 0.95, easedProgress);
        float alpha = 1.0 - alphaCut;

        return half4(color.rgb * alpha, color.a * alpha);
    }
""".trimIndent()

private val predictiveBackHumpMaskShader = """
    half4 main(float2 fragCoord) {
        half4 color = image.eval(fragCoord);
        float completion = smoothstep(1.0, 1.7, progress);
        float easedProgress = mix(CubicOut(clamp(progress, 0.0, 1.0)), 1.65, completion);
        float edgeSign = edge < 0.5 ? 1.0 : -1.0;
        float fromEdge = edge < 0.5 ? fragCoord.x : resolution.x - fragCoord.x;

        float verticalDistance = abs(fragCoord.y - touch.y) / resolution.y;
        float verticalRange = mix(0.46, 1.35, completion);
        float verticalPower = mix(2.4, 0.62, completion);
        float verticalProfile = 1.0 - smoothstep(0.0, verticalRange, verticalDistance);
        verticalProfile = pow(verticalProfile, verticalPower);

        float maxReach = resolution.x * mix(0.34, 1.45, completion) * easedProgress;
        float waveFront = maxReach * verticalProfile;
        float alphaFeather = mix(2.5, 7.0, easedProgress);
        float mask = 1.0 - smoothstep(waveFront - alphaFeather, waveFront + alphaFeather, fromEdge);
        mask *= smoothstep(0.0, 0.08, easedProgress);

        float2 anchor = vec2(
            edge < 0.5 ? waveFront : resolution.x - waveFront,
            touch.y
        );
        float2 toAnchor = anchor - fragCoord;
        float2 normalizedToAnchor = vec2(
            toAnchor.x / resolution.x,
            toAnchor.y / resolution.y
        );
        float pullDistance = length(normalizedToAnchor);
        float pullInfluence = 1.0 - smoothstep(0.0, mix(0.55, 1.15, completion), pullDistance);
        pullInfluence *= smoothstep(0.0, 0.12, easedProgress);
        pullInfluence *= edge < 0.5
            ? 1.0 - smoothstep(anchor.x, resolution.x, fragCoord.x)
            : smoothstep(0.0, anchor.x, fragCoord.x);
        float pullStrength = mix(0.18, 0.55, completion) * easedProgress * pullInfluence;
        float2 sampleCoord = fragCoord;
        sampleCoord -= toAnchor * pullStrength;
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

private const val BACK_COMPLETE_DURATION_MS = 260
private const val BACK_CANCEL_DURATION_MS = 140
private const val BACK_COMPLETE_PROGRESS = 1.7f
private const val BACK_POP_DELAY_MS = 128L
private const val BACK_RESET_FRAME_DELAY = 5
private val noNavDisplayTransition = ContentTransform(
    targetContentEnter = EnterTransition.None,
    initialContentExit = ExitTransition.None
)

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
