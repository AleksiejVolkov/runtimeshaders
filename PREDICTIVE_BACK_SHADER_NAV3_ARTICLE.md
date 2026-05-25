# Navigation 3 Made This Predictive Back Shader Surprisingly Simple

This article is a breakdown of a small experiment that turned into one of the best examples of why Navigation 3 changes how we can think about navigation effects in Compose.

The effect is a predictive back transition driven by a runtime shader. While the user swipes from the edge, the current screen does not simply slide away. Instead, it behaves like a flexible sheet: the gesture pulls a soft wave from the edge, the current screen bends around the original touch position, and the previous screen is revealed underneath. If the gesture is cancelled, the shader relaxes back to zero. If the gesture completes, the shader continues a little past `1f`, finishes the reveal, and only then do we pop the navigation stack.

That last sentence is the important part: the visual transition and the navigation state are related, but they are not the same thing.

Navigation 3 makes this kind of effect practical because it gives us direct control over the back stack as application state. We can render the current destination, render the previous destination manually underneath it, drive a shader from predictive back gesture progress, and decide the exact moment when the back stack should actually be mutated.

The complete implementation in this project lives mostly in:

- `app/src/main/java/com/offmind/runtimeshaders/navigation/RuntimeShadersNavHost.kt`
- `app/src/main/java/com/offmind/runtimeshaders/navigation/AppNavDisplay.kt`
- `app/src/main/java/com/offmind/runtimeshaders/navigation/Routes.kt`
- `app/src/main/java/com/offmind/runtimeshaders/navigation/predictive/PredictiveBackShaderLayer.kt`
- `app/src/main/java/com/offmind/runtimeshaders/navigation/predictive/PredictiveBackShaderState.kt`
- `app/src/main/java/com/offmind/runtimeshaders/navigation/predictive/PredictiveBackShaderSource.kt`

## The old mental model: navigation owns the transition

In many navigation APIs, navigation feels like a black box:

1. You ask the controller to navigate.
2. The controller changes the current destination.
3. The framework runs a transition between the old and new content.

That model is fine for normal transitions: fades, slides, scales, shared-axis movement, and other destination-to-destination animations. But it becomes limiting when the visual effect needs to reason about both navigation state and low-level pixels.

This shader needs more than "animate screen A out and screen B in".

It needs:

- The current destination as an offscreen texture, because the shader samples and warps its pixels.
- The previous destination already visible underneath the current one.
- Predictive back progress before the pop actually happens.
- The gesture edge, because left-edge and right-edge swipes bend in opposite directions.
- The initial touch Y coordinate, because the wave should originate from the user's finger, not from the center of the screen.
- A delayed stack mutation, because the shader must finish visually before `backStack.removeLastOrNull()` removes the current route.

This is where Navigation 3 becomes interesting.

## The Navigation 3 mental model: navigation is state

Navigation 3 is built around a simple idea: the back stack is owned by your app.

In this project, the stack is created like this:

```kotlin
val backStack = rememberNavBackStack(Route.EffectsList)
```

Each destination is represented by a key:

```kotlin
sealed class Route : NavKey {
    @Serializable
    data object EffectsList : Route()

    @Serializable
    data class NavigationTest(
        val title: String,
        val description: String
    ) : Route()

    @Serializable
    data class NavigationTestFeed(
        val title: String,
        val description: String
    ) : Route()
}
```

Pushing a destination is just adding a key:

```kotlin
backStack.add(Route.NavigationTestFeed("Navigation Test Feed", "Mock feed"))
```

Popping is just removing the last key:

```kotlin
backStack.removeLastOrNull()
```

`NavDisplay` observes that stack and resolves each key to UI through an `entryProvider`:

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = routeEntryProvider(onEffectSelected)
)
```

That is the core difference. Navigation is not hidden behind a controller that owns the whole transition. The app has a real list-like model of where the user is. Because we own that model, we can temporarily render a destination that is not currently on top, keep the top destination alive while a shader finishes, and mutate the stack only when the visual state is ready.

Officially, Navigation 3 describes this as full control over the back stack, with `NavDisplay` rendering the app's stack and updating when that stack changes. That design is exactly what makes this shader possible without fighting the framework.

## The effect architecture

The implementation has four layers:

1. `RuntimeShadersNavHost` owns the back stack, predictive back gesture state, terminal animation state, and the underlay route.
2. `PredictiveBackNavDisplay` wraps `NavDisplay` in a shader layer.
3. `PredictiveBackShaderLayer` turns the current destination into an offscreen texture and applies an AGSL `RuntimeShader`.
4. `PredictiveBackShaderSource` contains the pixel math that bends, masks, fades, and shadows the current screen.

The key architecture looks like this:

```kotlin
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

    PredictiveBackNavDisplay(
        state = shaderState,
        backStack = backStack,
        modifier = Modifier.fillMaxSize()
    )
}
```

When no back gesture is active, only `PredictiveBackNavDisplay` matters. It shows the normal top destination.

When predictive back progress becomes greater than zero, we also render the previous route underneath:

```kotlin
activeUnderlayRoute = backStack.dropLast(1).lastOrNull() as? Route
```

That is the "prediction" part from the UI perspective. The user has not completed the back gesture yet, and the stack has not been popped yet, but we can already know what screen would be revealed if the gesture completes. Navigation 3 makes that trivial because the previous destination is just the previous key in the list.

## Why we disable NavDisplay transitions

Inside `PredictiveBackNavDisplay`, the `NavDisplay` transitions are intentionally disabled:

```kotlin
private val noNavDisplayTransition = ContentTransform(
    targetContentEnter = EnterTransition.None,
    initialContentExit = ExitTransition.None
)
```

And then passed into `NavDisplay`:

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    popTransitionSpec = { noNavDisplayTransition },
    predictivePopTransitionSpec = { noNavDisplayTransition },
    entryProvider = routeEntryProvider(onEffectSelected)
)
```

This is deliberate. `NavDisplay` can animate transitions itself, including predictive pop transitions. For this effect, however, `NavDisplay` should only render the destination content. The shader owns the visual transition.

That separation keeps the system understandable:

- Navigation 3 owns destination resolution.
- Our host owns gesture-to-navigation timing.
- The shader layer owns pixels.

If both `NavDisplay` and the shader animated at the same time, the current screen would be transformed twice. The shader would be sampling content that is already moving, which makes the effect harder to control and easier to break.

## Capturing predictive back as shader state

The shader does not know anything about Android back gestures. It only knows uniforms:

- `resolution`
- `touch`
- `progress`
- `edge`
- `image`

The adapter between predictive back and shader uniforms is `toShaderState()`:

```kotlin
internal fun NavigationEventState<NavigationEventInfo.None>.toShaderState(
    resetToken: Int
): PredictiveBackShaderState
```

It reads the latest event from the in-progress predictive back transition:

```kotlin
val latestEvent =
    (transitionState as? NavigationEventTransitionState.InProgress)?.latestEvent
```

Then it stores three values:

```kotlin
PredictiveBackShaderState(
    progress = latestEvent?.progress ?: 0f,
    touch = lastTouch.value,
    swipeEdge = lastSwipeEdge.intValue
)
```

There is one subtle but important decision here: the shader uses the starting Y position of the gesture, not the continuously changing finger Y position.

```kotlin
if (latestEvent != null && startTouchY.value == null) {
    startTouchY.value = latestEvent.touchY
}
```

That makes the deformation feel anchored. The wave originates from the point where the user started pulling, instead of wobbling vertically as the finger moves.

For right-edge swipes, the code stores `Float.POSITIVE_INFINITY` as the touch X marker:

```kotlin
val touch = Offset(
    x = when (latestEvent.swipeEdge) {
        NavigationEvent.EDGE_RIGHT -> Float.POSITIVE_INFINITY
        else -> 0f
    },
    y = startTouchY.value ?: latestEvent.touchY
)
```

Later, inside the graphics layer, that marker is converted into the actual width:

```kotlin
val centerX = if (state.touch.x.isInfinite()) size.width else state.touch.x
```

The shader does not need to know about Android constants. It receives a numeric edge and a concrete touch coordinate.

## The shader layer

The current destination is wrapped by `PredictiveBackShaderLayer`:

```kotlin
PredictiveBackShaderLayer(
    state = state,
    modifier = modifier
) {
    NavDisplay(...)
}
```

Inside the layer, the runtime shader is created once with the expected uniforms:

```kotlin
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
```

The actual effect is applied from `graphicsLayer`:

```kotlin
modifier.graphicsLayer {
    if (state.progress > 0f) {
        compositingStrategy = CompositingStrategy.Offscreen

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
```

`CompositingStrategy.Offscreen` is essential here. The shader needs the current screen as an input image. Without offscreen compositing, there is no single texture for the shader to sample and warp.

Once the layer has an `image`, the AGSL code can treat the entire destination as pixels.

## What the shader actually does

The shader starts by sampling the current screen:

```glsl
half4 color = image.eval(fragCoord);
```

Then it converts gesture progress into a more useful animation value:

```glsl
float completion = smoothstep(1.0, 1.7, progress);
float easedProgress = mix(CubicOut(clamp(progress, 0.0, 1.0)), 1.65, completion);
```

Normal predictive progress is in the `0f..1f` range. But when the user completes the back gesture, the implementation animates progress to `1.7f`. That extra range gives the shader room to finish the reveal after the system gesture has crossed the completion threshold.

The shader then figures out which edge the gesture came from:

```glsl
float edgeSign = edge < 0.5 ? 1.0 : -1.0;
float fromEdge = edge < 0.5 ? fragCoord.x : resolution.x - fragCoord.x;
```

After that, it builds a vertical profile around the original touch Y:

```glsl
float verticalDistance = abs(fragCoord.y - touch.y) / resolution.y;
float verticalProfile = 1.0 - smoothstep(0.0, verticalRange, verticalDistance);
verticalProfile = pow(verticalProfile, verticalPower);
```

This is what makes the effect look like a hump instead of a flat rectangular wipe. Pixels near the gesture origin are affected most. Pixels farther away are affected less.

The wave front determines how far the reveal has reached from the edge:

```glsl
float maxReach = resolution.x * mix(0.31, 1.45, completion) * easedProgress;
float waveFront = maxReach * verticalProfile;
float mask = 1.0 - smoothstep(waveFront - alphaFeather, waveFront + alphaFeather, fromEdge);
```

Then the current screen is warped:

```glsl
float horizontalWarp = waveFront * edgeInfluence * mix(0.62, 0.18, completion);
sampleCoord.x -= edgeSign * horizontalWarp;
```

And slightly relaxed vertically:

```glsl
float verticalDirection = sign(fragCoord.y - touch.y);
float verticalRelaxation = (1.0 - verticalProfile) * waist * edgeInfluence;
sampleCoord.y -= verticalDirection * verticalRelaxation;
```

Finally, the shader samples the warped current screen and computes final alpha:

```glsl
half4 warpedColor = image.eval(sampleCoord);
float alpha = 1.0 - mask;
float finalAlpha = max(warpedColor.a * alpha, shadow);

return half4(warpedColor.rgb * alpha, finalAlpha);
```

The result is that the current screen is not just translated. It is partially erased, partially warped, and given a soft edge shadow while the previous destination shows through from underneath.

## The most important implementation detail: delayed popping

If the back stack were popped immediately when the gesture completes, the current destination would disappear before the shader could finish. That would break the illusion.

Instead, `NavigationBackHandler` separates gesture completion from stack mutation:

```kotlin
onBackCompleted = {
    scope.launch {
        terminalProgress.animateTo(
            targetValue = BACK_COMPLETE_PROGRESS,
            animationSpec = tween(durationMillis = BACK_COMPLETE_DURATION_MS)
        )
        delay(BACK_POP_DELAY_MS.milliseconds)
        backStack.removeLastOrNull()
        repeat(BACK_RESET_FRAME_DELAY) {
            withFrameNanos { }
        }
        terminalProgress.snapTo(0f)
        activeUnderlayRoute = null
        gestureSession++
    }
}
```

The timeline is:

1. The user completes the predictive back gesture.
2. The shader continues from the current gesture progress to `BACK_COMPLETE_PROGRESS`, which is `1.7f`.
3. After a short delay, the stack is popped.
4. A few frames are allowed to pass.
5. The shader state is reset.

That creates the visual ordering we want:

- First, the current screen finishes revealing the previous screen.
- Then, the current route is removed from the back stack.
- Finally, temporary shader and underlay state is cleared.

Cancellation follows the same principle in reverse:

```kotlin
onBackCancelled = {
    scope.launch {
        terminalProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = BACK_CANCEL_DURATION_MS)
        )
        activeUnderlayRoute = null
        gestureSession++
    }
}
```

The shader animates back to rest, the underlay is removed, and `gestureSession` resets cached touch state.

## Why Navigation 3 is the enabling piece

This implementation is not just "a shader plus navigation". It depends on the Navigation 3 model in several concrete ways.

First, the back stack is readable. During the gesture, we can inspect `backStack.dropLast(1).lastOrNull()` to find the destination that should appear underneath the current one. We do not need to ask a controller what destination is behind the current screen. It is plain state.

Second, the back stack is mutable by the app. We can choose not to pop immediately. The visual effect can complete first, and only then can we call `removeLastOrNull()`.

Third, destinations are type-safe keys. Each route implements `NavKey`, and the `entryProvider` maps those keys to composable content. That makes it easy to render a route through `RouteContent()` both inside `NavDisplay` and manually as the underlay.

Fourth, `NavDisplay` is composable. Because it is just UI, we can wrap it in `PredictiveBackShaderLayer`. The entire active destination becomes shader input.

Fifth, Navigation 3 does not force the transition model. It provides built-in transition hooks, including predictive pop transitions, but we can disable them and take over rendering ourselves. That is important for effects that are not regular layout animations.

In a controller-first navigation API, this would likely require much more glue: custom transition objects, destination snapshots, manual lifecycle work, or duplicated UI. In this Navigation 3 approach, the implementation stays local and explicit:

- The route list tells us what is current and what is behind it.
- The predictive back event tells us how far the user has pulled.
- Compose gives us a graphics layer.
- AGSL transforms the pixels.

## A minimal recipe for this pattern

If you want to build a similar effect, the recipe is:

1. Model every destination as a `NavKey`.
2. Keep your app back stack in Compose state with `rememberNavBackStack`.
3. Render the top destination with `NavDisplay`.
4. Disable `NavDisplay` transitions if your shader owns the visual transition.
5. Listen to predictive back through `NavigationBackHandler` and `rememberNavigationEventState`.
6. Convert the latest predictive back event into shader uniforms.
7. When progress starts, render the previous back stack route underneath the current one.
8. Wrap the current destination in a `graphicsLayer` with offscreen compositing and a `RuntimeShader`.
9. On cancel, animate shader progress back to zero and clear temporary state.
10. On complete, animate shader progress to a terminal value, then pop the stack.

The important rule is: do not let the back stack mutation happen before the visual effect has enough time to finish.

## What I like about this approach

The implementation is powerful because it is made of small, understandable pieces.

`RuntimeShadersNavHost` is responsible for navigation state and gesture timing. `PredictiveBackShaderState` is a tiny data model. `PredictiveBackShaderLayer` knows how to turn Compose content into shader input. The AGSL shader only knows about pixels and uniforms.

Navigation 3 is the reason those pieces can stay separate. The back stack is not hidden, the destination model is not coupled to a controller, and `NavDisplay` does not have to own the final transition. We can use Navigation 3 for what it is very good at: resolving typed navigation state into composable content. Then we can build a completely custom rendering effect around that content.

That is why Navigation 3 feels like a game changer for this class of UI work. It does not only make normal navigation simpler. It makes navigation state available as something creative code can use.

## References

- [Navigation 3 overview](https://developer.android.com/guide/navigation/navigation-3)
- [Navigation 3 basics](https://developer.android.com/guide/navigation/navigation-3/basics)
- [Animate between destinations with NavDisplay](https://developer.android.com/guide/navigation/navigation-3/animate-destinations)
- [NavDisplay API reference](https://developer.android.com/reference/kotlin/androidx/navigation3/ui/NavDisplay)
