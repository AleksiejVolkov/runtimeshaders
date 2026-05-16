# OpenGL + Compose Integration Notes

Generated: 2026-05-12

This document describes the current OpenGL integration used by `TestShaderScreen` so future agents can continue without rediscovering the architecture and constraints.

## Goal

Render real OpenGL ES 2 geometry inside Compose while sampling a Compose-rendered background as a texture. The current demo uses a rotating glass cube over a scrollable Compose `LazyColumn`.

The important result: the GL layer does not use `PixelCopy(window)` for the active demo path. It captures only the Compose background subtree via `GraphicsLayer`, which avoids feedback where the GL output captures itself.

## Current Screen

Entry point:

```text
app/src/main/java/com/offmind/runtimeshaders/screens/effects/TestShaderScreen.kt
```

The screen now stays intentionally thin:

- creates `RotatingGlassCubeScene`
- creates `LazyListState`
- increments `captureVersion` when the list scroll position changes
- calls `CapturedBackgroundGlBox`
- provides full-screen `LazyColumn` background content
- positions the GL surface at the center with `surfaceModifier = Modifier.size(glassSurfaceSize)`

`captureVersion` is the key state-driven invalidation signal. Static content does not continuously capture. Scrolling updates the texture because `snapshotFlow` watches:

```kotlin
listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
```

## Main Composable Wrapper

Core wrapper:

```text
app/src/main/java/com/offmind/runtimeshaders/gl/compose/CapturedBackgroundGlBox.kt
```

Responsibilities:

- owns a Compose `GraphicsLayer`
- owns `BackgroundCapture`
- renders caller-provided `backgroundContent`
- records only the background region under the GL surface
- converts the recorded layer to `ImageBitmap`, then Android `Bitmap`
- submits the bitmap to the GL renderer through `BackgroundCapture`
- places `EmbeddedGlSurface` on top

Important parameters:

```kotlin
CapturedBackgroundGlBox(
    scene: GlScene,
    surfaceSize: DpSize,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier,
    surfaceAlignment: Alignment = Alignment.Center,
    captureVersion: Long = 0L,
    captureIntervalMillis: Long = 0L,
    backgroundContent: @Composable BoxScope.() -> Unit
)
```

Default capture is state-driven. `captureIntervalMillis` defaults to `0L`, meaning no polling. Use polling only for debug or continuously animated backgrounds.

## Offset-Aware Capture

The wrapper supports different background and surface sizes.

It tracks the GL surface position inside the parent:

```kotlin
coordinates.boundsInParent().topLeft
```

Then records the background layer with inverse translation:

```kotlin
backgroundLayer.record(size = surfaceSizePx) {
    translate(
        left = -surfaceOffset.x,
        top = -surfaceOffset.y
    ) {
        drawContent()
    }
}
```

This means the texture sent to GL is the background region directly under the GL surface, not the top-left region of the full background.

## Blank Snapshot Guard

Sometimes `GraphicsLayer.toImageBitmap()` can briefly return an invalid blank/near-black image while layer recording is between frames. The wrapper samples a 3x3 grid and rejects snapshots that appear blank:

```text
Bitmap.hasVisibleContent()
```

If a snapshot is rejected, the previous GL texture remains active. This avoids periodic black cube flashes.

## GL Surface Wrapper

File:

```text
app/src/main/java/com/offmind/runtimeshaders/gl/compose/EmbeddedGlSurface.kt
```

Wraps `AndroidEmbeddedExternalSurface(isOpaque = false)`.

Inside `onSurface`, it:

- creates `GLRenderer`
- starts EGL and scene setup
- handles resize
- runs render loop with `withFrameNanos`
- releases renderer on destroy/finally

## Renderer And Texture Handoff

File:

```text
app/src/main/java/com/offmind/runtimeshaders/gl/GLRenderer.kt
```

Responsibilities:

- starts `EglSurfaceSession`
- configures GL state
- owns the background texture ID
- polls `BackgroundCapture` for submitted bitmaps
- uploads new bitmaps with `GLUtils.texImage2D`
- clears the external surface transparent every frame
- calls `scene.onDrawFrame(...)`
- swaps buffers

The GL background texture is normal `GL_TEXTURE_2D`, not `samplerExternalOES`.

Important behavior:

- `isBackgroundTextureReady` stays false until the first valid bitmap upload.
- old texture remains active until a new valid bitmap is uploaded.
- renderer still contains legacy support for `BackgroundCapture.shouldCapture()` / PixelCopy fallback, but the current graphics-layer path does not use that.

## BackgroundCapture

File:

```text
app/src/main/java/com/offmind/runtimeshaders/gl/BackgroundCapture.kt
```

Currently acts as a small thread-safe bitmap handoff queue:

```kotlin
submitBitmap(bitmap)
pollLatestBitmap()
release()
```

It still contains optional legacy PixelCopy logic if constructed with a `Window`, but current `CapturedBackgroundGlBox` constructs it without a `Window`.

## Scene Interface

File:

```text
app/src/main/java/com/offmind/runtimeshaders/gl/scene/GlScene.kt
```

Scene contract:

```kotlin
interface GlScene {
    fun onSurfaceCreated()
    fun onSurfaceChanged(width: Int, height: Int) = Unit
    fun onDrawFrame(frameInfo: GlFrameInfo)
    fun release()
}
```

`GlFrameInfo` includes:

- frame time
- surface width/height
- background texture ID
- background texture readiness flag

## Current Glass Cube Scene

File:

```text
app/src/main/java/com/offmind/runtimeshaders/gl/scene/RotatingGlassCubeScene.kt
```

Uses:

- `CubeGeometry.createGlassCube()`
- positions
- normals
- GL ES 2 shaders
- sampler2D background texture

Shader behavior:

- samples captured Compose texture using `gl_FragCoord / resolution`
- flips Y for Android bitmap orientation
- offsets UVs by cube normal for refraction
- adds subtle blue tint
- adds front-face reflection
- adds internal/back-edge reflection based on cube object-space edge proximity and glancing angle

This is an approximation, not physically correct ray tracing.

## Geometry And Shader Utilities

Geometry:

```text
app/src/main/java/com/offmind/runtimeshaders/gl/geometry/
```

Important files:

- `GlMesh.kt`: FloatBuffer and attribute binding.
- `VertexLayout.kt`: position/normal/color stride metadata.
- `CubeGeometry.kt`: cube vertices with normals/colors.
- `SphereGeometry.kt`: older generated sphere mesh kept for future experiments.

Shader helper:

```text
app/src/main/java/com/offmind/runtimeshaders/gl/shader/GlProgram.kt
```

Compiles/link shaders, exposes:

- `use()`
- `getAttribute(...)`
- `setFloat(...)`
- `setInt(...)`
- `setVec2(...)`
- `setMat4(...)`
- `release()`

## EGL Setup

File:

```text
app/src/main/java/com/offmind/runtimeshaders/gl/EglSurfaceSession.kt
```

Uses EGL14 directly:

- `EGL_ALPHA_SIZE = 8`
- `EGL_OPENGL_ES2_BIT`
- window surface from `AndroidEmbeddedExternalSurface`
- GLES 2 context

Transparent composition depends on:

- `AndroidEmbeddedExternalSurface(isOpaque = false)`
- EGL config with alpha
- renderer clearing with alpha 0
- GL blending enabled

## Validated Lessons

1. `PixelCopy(window)` is fragile for this use case.
   It can capture the GL overlay itself and create feedback.

2. Capturing the Compose background subtree through `GraphicsLayer` avoids GL self-capture.

3. Capturing by polling works but wastes work on static screens.
   Current default is state-driven capture via `captureVersion`.

4. For scrolling content, increment `captureVersion` from `LazyListState` changes.

5. Offset-aware recording is required when background and surface sizes differ.

6. Keep the previous GL texture if a new capture is blank.

## How To Use For A New Screen

Minimal pattern:

```kotlin
val scene = remember { RotatingGlassCubeScene() }
var captureVersion by remember { mutableLongStateOf(0L) }

CapturedBackgroundGlBox(
    scene = scene,
    surfaceSize = DpSize(300.dp, 300.dp),
    modifier = Modifier.fillMaxSize(),
    surfaceAlignment = Alignment.Center,
    surfaceModifier = Modifier.size(300.dp),
    captureVersion = captureVersion
) {
    // Full background content here.
}
```

When background state changes:

```kotlin
captureVersion++
```

For `LazyColumn`:

```kotlin
LaunchedEffect(listState) {
    snapshotFlow {
        listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
    }.collect {
        captureVersion++
    }
}
```

For animated backgrounds, either:

- increment `captureVersion` per relevant animation tick, or
- pass `captureIntervalMillis = 33L` temporarily while animation is active.

## Known Limitations

- `GraphicsLayer.toImageBitmap()` still creates a bitmap copy. This is not zero-copy GPU texture sharing.
- Frequent captures can be expensive for large surfaces.
- `AndroidEmbeddedExternalSurface` and Compose render independently; keep synchronization explicit.
- Current shader samples a 2D captured texture; it does not know about true scene depth behind the cube.
- HardwareBuffer zero-copy import was investigated but not implemented because local public `PixelCopy` stubs expose bitmap destinations, not hardware-buffer destinations.

## Recommended Next Improvements

- Rename `BackgroundCapture` to something like `BackgroundTextureSource` because it now mostly acts as bitmap handoff.
- Add metrics/logging for capture count, rejected blank captures, bitmap size, and upload cadence.
- Add a reusable `CaptureInvalidationStrategy` instead of raw `captureVersion` and optional interval.
- Add multiple `GlScene` implementations to validate the architecture beyond cube glass.
- Consider downscaling captured texture for performance when high fidelity is not needed.
