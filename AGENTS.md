# Agent Guide

Quick-start for a coding agent working in this repo. Read this first, then dip into
[`PROJECT_MAP.md`](PROJECT_MAP.md) for the full architecture. Prefer small, local changes that
follow the existing Compose, **Navigation 3**, AGSL, and OpenGL patterns.

## What This Is

An Android playground for GPU visual effects in Jetpack Compose. Three rendering techniques coexist:

1. **AGSL `RuntimeShader`** — most effects, via the shared `Shader` wrapper + `ShadedBox`.
2. **OpenGL ES** (`gl/`) — 3D scenes drawn on an embedded surface, can sample a captured Compose
   background as a texture.
3. **Predictive-back shader transitions** (`navigation/predictive/`) — the back gesture dissolves
   the outgoing screen with a selectable AGSL effect.

Single module `:app`; `buildSrc` code-generates the shader-function dependency map at build time.

## Stack (verify before assuming)

AGP `8.9.1` · Kotlin `2.1.0` · Gradle wrapper `8.11.1` · compileSdk `36` / targetSdk `35` /
minSdk `33` · Java `17` · Compose BOM `2024.12.01` · Navigation 3 `1.0.1` · DataStore `1.1.1`.
Koin `4.0.0` + Ktor `3.0.1` are declared and Koin is started in `MyApplication`, but
`di/AppModule.kt` is empty — they are scaffolding, not active infra.

## Key Files

| Concern | File |
|---|---|
| Entry | `MainActivity.kt` → `RuntimeShadersApp.kt` → `navigation/RuntimeShadersNavHost.kt` |
| Navigation (Nav3) | `navigation/AppNavDisplay.kt`, `navigation/Routes.kt` |
| Effect catalog (groups + screens) | `screens/EffectsCatalog.kt`, `screens/EffectCatalog.kt` |
| Catalog UI (all levels) | `screens/EffectsCatalogScreen.kt` |
| App appearance / shared chrome | `ui/theme/AppAppearance.kt`, `ui/components/AppChrome.kt` |
| App settings persistence | `data/AppSettingsRepository.kt` |
| Effect screens | `screens/effects/` |
| Shader wrapper | `shaders/Shader.kt` |
| Shared shader container | `composables/Uitls.kt` (sic — composable is `ShadedBox`) |
| Shared AGSL snippets | `shaders/ShadersCollection.kt` |
| Generated function map | `app/build/generated/.../generated/ShaderDependencyMap.kt` (task `generateShaderDependencyMap`) |
| Reusable AGSL functions (source) | `buildSrc/.../functions/` |
| OpenGL layer | `gl/` (renderer, scenes, geometry, embedded surface, background capture) |
| Lighting DSL | `screens/effects/lighting/` |
| Predictive-back transitions | `navigation/predictive/`, `data/AppSettingsRepository.kt` |

## Shader Wrapper Rules

Use `Shader(source).getRuntimeShader(...)` for AGSL unless there's a specific reason to instantiate
`android.graphics.RuntimeShader` directly. It prepends uniform declarations and the transitively
resolved generated helper functions before the body.

Default uniforms come from `basicUniformList`:

```glsl
uniform shader image;
uniform vec2  resolution;
uniform float time;
uniform float percentage;
```

- `basicUniformList.addUniform(Uniform.Type.X to "name")` for extra uniforms; `removeUniform("name")`
  only when a shader intentionally drops a default.
- `customFunctions` defaults to *all* generated functions; pass a smaller `Set<ShaderFunction>` for
  narrow shaders (see `PredictiveBackDissolve.kt`, which uses only `CubicOut` + `Hash21`).
- `ShaderTypedValue`: `FloatType`, `Vec2Type`, `Vec3Type`, `Vec4Type`, `IntType`, `Vec2Array`.

## ShadedBox Rules

Use `ShadedBox` from `com.offmind.runtimeshaders.composables`. Full API in
[`SHADER_BOX_USAGE.md`](SHADER_BOX_USAGE.md).

- **With `content`** → applies a runtime-shader render effect to the children; shader must declare
  and sample `uniform shader image`.
- **Without `content`** → draws the shader directly into the box.
- `resolution` is set automatically; set `includeTime = true` to animate from the built-in time.
- Pass other uniforms through `shaderUniforms`.
- `remember { ... }` the shader so it isn't recreated each recomposition.
- Don't fork local `ShadedBox` copies. For per-entry / scoped effects, apply
  `RenderEffect.createRuntimeShaderEffect(...).asComposeRenderEffect()` in a `graphicsLayer`
  directly (see the predictive-back and lighting code).

## Generated Function Rules

Reusable AGSL helpers live in `buildSrc/.../functions/`, each file exporting an
`all<Category>Functions` map keyed by the **exact AGSL function name**. The generator
(`createDependencies.kt`) combines the maps, scans bodies for references to other names, and emits
the `ShaderFunction` enum + dependency map. `Shader.kt` resolves dependencies (and detects cycles)
before appending the effect source.

To add a function:

1. Add the AGSL body as a Kotlin string to the relevant map, keyed by the exact function name.
2. If you create a new aggregate map, append it in `createDependencies.kt`.
3. Run `./gradlew generateShaderDependencyMap` (or any build).
4. Call it by name in shader source — ordering/deps are automatic.

Keep names distinct: dependency detection is substring-based.

## Adding A New Effect

1. Create a composable screen in `screens/effects/`.
2. Keep AGSL source near the screen (local) or in `ShadersCollection.kt` (shared).
3. `remember { Shader(source).getRuntimeShader(...) }`; render with `ShadedBox`.
4. Add an `EffectScreenData(id, title, description) { MyScreen() }` to the appropriate
   `EffectCategory.children` in `EffectsCatalog.kt`. The entry registers both the menu item and its
   screen; **no effect-specific route or `AppNavDisplay.kt` edit is needed**.
5. Verify: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.

The catalog is a tree: Widgets → Glass / Neumorphism / Lighting / Controls; OpenGL scenes;
Visual effects; Navigation. To add a group at any depth, nest another `EffectCategory`.
`EffectsCatalogScreen` renders every level and derives counts and breadcrumbs from the tree.

Keep IDs unique across the entire tree and stable when renaming or moving items. Navigation 3
stores only IDs in `Route.Category` / `Route.Effect`; titles and descriptions belong solely to the
catalog. `EffectCatalog` validates nonempty categories, IDs and titles and indexes the tree.
`EffectCatalogTest` covers grouping, reachability and restored IDs; extend the expected demo set
when adding a new demo. Screens with a flow can use the content lambda's `onNavigate` callback
(see Navigation Test). Only destinations outside the catalog, such as its feed or settings, need
an explicit `Route` and `AppNavDisplay` registration.

There is no `NavHost`/`composable<>` registration in `MainActivity` — navigation is Nav3 via
`AppNavDisplay.kt`. Catalog entries use the same saveable-state and predictive-back wrappers as
other screens, preserving each level's scroll position on return.

## App Appearance

Settings → Appearance switches the catalog (including nested groups), settings and both pickers
between Classic, Neumorphism and Glass. Effect demos retain their own design. `AppAppearance`
contains stable IDs and picker metadata; `AppChrome.kt` provides `AppScreen`, `AppMenuRow` and
`AppIconButton`, reusing the existing widget `neumorphic` / `glass` modifiers. Use these components
for new app chrome so colors, pressed states and materials follow the selected style. Keep demo
materials scoped to their screens. The host sets system-bar contrast only for the active chrome
route, not for background entries or appearance previews.

`AppSettingsRepository` stores appearance and predictive-back choice independently in DataStore.
It retains the original `back_effect_settings` file and `selected_back_effect` key to preserve
existing installations. `RuntimeShadersNavHost` waits for the first settings value, then provides
the appearance without rebuilding the back stack. Add an appearance to `AppAppearance` and its
rendering in `AppChrome.kt`; the picker derives its options from the enum. Persistence tests use
real temporary DataStore files and cover reopening, old settings and independent updates.

## OpenGL Effects

3D scenes implement `GlScene` (`gl/scene/`) and render through `EmbeddedGlSurface` /
`CapturedBackgroundGlBox` (`gl/compose/`). When the scene should refract/sample the Compose UI
behind it, wrap with `CapturedBackgroundGlBox` and pass `backgroundContent`. Reuse existing geometry
helpers in `gl/geometry/`. See `RotatingGlassCubeScene` and `TapePlaneScene` for the pattern.

## Lighting Scope

`LightingScope { ... }` (`screens/effects/lighting/`) exposes scoped modifiers
`Modifier.lightSource(...)` and `Modifier.receivesLight(...)`. Light geometry is pushed into bloom
and receiver shaders as fixed-size arrays capped at `MAX_LIGHTS`. `LightingState.uniforms` packs
and caches these arrays (including normalized glare colors) once per light change for all receivers.
The shaders are time-independent: invalidate layers only on light, size or position changes; do
not add a frame timer. Empty lighting/bloom passes use no render effect. Follow `IlluminateUiScreen`
for this DSL; `DualLightTextureScreen` uses its own animated lighting model. New scoped modifiers
go on `LightingScopeReceiver`.

For an opaque card, `receivesLight(opaqueInset = cornerRadius)` skips edge-mask texture samples
only inside the guaranteed opaque rectangle, retaining the full edge kernel near its outline.
Leave this hint null for translucent content or holes. Receiver offscreen bounds are preserved
when lights turn off, so shadows do not change shape as the identity shader is bypassed.

## Predictive Back Effects

Live under `navigation/predictive/`, hook the global back gesture, and are **not** catalog effects.
To add one: add a `PredictiveBackEffect` enum entry + its AGSL in `PredictiveBackShaderSource.kt`,
then verify the picker (`BackEffectPickerScreen`), DataStore persistence
(`AppSettingsRepository`), and the gesture flow in `AppNavDisplay.kt`. Background:
[`PREDICTIVE_BACK_SHADER_NAV3_ARTICLE.md`](PREDICTIVE_BACK_SHADER_NAV3_ARTICLE.md).

## Build & Verify

```bash
./gradlew :app:compileDebugKotlin   # fast check
./gradlew assembleDebug             # full build
./gradlew generateShaderDependencyMap  # regenerate the function map
```

Gradle wrapper is approved for local use; project targets Java 17 (see `BUILD_SETUP.md`).

## Gotchas

- `composables/Uitls.kt` is misspelled — renaming touches many imports.
- Catalog IDs are saved navigation keys: changing an ID can invalidate a restored destination.
- `WashDownViewScreen.kt`, `EdgeFadeModifier.kt`, `SampleUiElements.kt` are demo/support, not all
  wired into the catalog.
- Substring-based dependency detection in the generator — avoid helper-name collisions.

## Keeping Docs Current

Update this file and [`PROJECT_MAP.md`](PROJECT_MAP.md) when the effect-registration path, shader
wrapper, generated-function flow, navigation model, GL layer, lighting scope, or `ShadedBox`
behavior changes. `README.md` is the human-facing overview; `SHADER_BOX_USAGE.md` is the focused
`ShadedBox` API.
