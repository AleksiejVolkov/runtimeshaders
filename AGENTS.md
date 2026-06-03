# Agent Guide

This repository is an Android RuntimeShader playground. Prefer small, local changes that follow the existing Compose, Navigation 3, and AGSL patterns.

## What Exists

- Shared shader wrapper: `app/src/main/java/com/offmind/runtimeshaders/shaders/Shader.kt`
- Shared shader container: `app/src/main/java/com/offmind/runtimeshaders/composables/Uitls.kt`
- Shared AGSL snippets: `app/src/main/java/com/offmind/runtimeshaders/shaders/ShadersCollection.kt`
- Generated reusable function map: `app/build/generated/src/main/java/com/offmind/runtimeshaders/generated/ShaderDependencyMap.kt`
- Function source for generation: `buildSrc/src/main/java/com/offmind/runtimeshaders/functions/`
- Generator task: `generateShaderDependencyMap`
- Effects list: `app/src/main/java/com/offmind/runtimeshaders/screens/EffectsCatalog.kt`
- Routes: `app/src/main/java/com/offmind/runtimeshaders/navigation/Routes.kt`
- Route-to-screen mapping: `app/src/main/java/com/offmind/runtimeshaders/navigation/AppNavDisplay.kt`
- Effect screens: `app/src/main/java/com/offmind/runtimeshaders/screens/effects/`
- OpenGL scenes and renderer: `app/src/main/java/com/offmind/runtimeshaders/gl/`
- Predictive-back shader system: `app/src/main/java/com/offmind/runtimeshaders/navigation/predictive/`

## Shader Wrapper Rules

Use `Shader(source).getRuntimeShader(...)` for AGSL effects unless there is a specific reason to instantiate `android.graphics.RuntimeShader` directly.

`Shader` prepends uniform declarations and generated helper functions before the shader source. Default uniforms come from `basicUniformList`:

```text
uniform shader image;
uniform vec2 resolution;
uniform float time;
uniform float percentage;
```

Use `basicUniformList.addUniform(Uniform.Type... to "name")` for shader-specific uniforms. Use `removeUniform(...)` only when a shader intentionally does not need one of the defaults.

`ShaderTypedValue` supports `FloatType`, `Vec2Type`, `Vec3Type`, `Vec4Type`, `IntType`, and `Vec2Array`.

## ShadedBox Rules

Use `ShadedBox` from `com.offmind.runtimeshaders.composables`.

- With child content, `ShadedBox` applies a runtime shader render effect to that content and uses the `image` shader uniform.
- Without child content, `ShadedBox` draws the shader directly into the box.
- `resolution` is set automatically from the composable size.
- Set `includeTime = true` when the shader should animate from the built-in time state.
- Pass all other uniforms through `shaderUniforms`.

Do not add duplicate local versions of `ShadedBox`; extend the shared implementation only if the behavior is broadly useful.

## Generated Function Rules

Reusable AGSL helpers are stored in `buildSrc/src/main/java/com/offmind/runtimeshaders/functions/`.

The generation flow is:

1. Function strings are collected from `allColorCorrectionFunctions`, `allSdfFunctions`, `allEasingFunctions`, `allNoisesFunctions`, `allCommonFunctions`, and `allTransformationsFunctions`.
2. `createDependencies.kt` scans function bodies for references to other function names.
3. `generateShaderDependencyMap` writes `ShaderDependencyMap.kt`.
4. `Shader.kt` resolves requested functions and dependencies before appending the effect source.

When adding a reusable function:

1. Add a Kotlin string with the AGSL function body.
2. Add it to the relevant `all...Functions` map using the exact AGSL function name as the key.
3. If you create a new aggregate map, import and append it to `allFunctions` in `createDependencies.kt`.
4. Build or run `./gradlew generateShaderDependencyMap`.

Avoid ad hoc string injection in effect screens when the helper should be shared.

## Adding A New Effect

1. Add a composable screen under `app/src/main/java/com/offmind/runtimeshaders/screens/effects/`.
2. Put the AGSL source near the screen if it is local-only, or in `ShadersCollection.kt` if other screens may reuse it.
3. Create the runtime shader with `remember { Shader(source).getRuntimeShader(...) }`.
4. Render it with `ShadedBox`.
5. Add custom uniforms to the shader declaration list and pass matching `ShaderTypedValue` entries.
6. Add a route type to `Route` in `Routes.kt`.
7. Register the route in `routeEntryProvider(...)` in `AppNavDisplay.kt`.
8. Add the `RouteContent(...)` branch that calls the new screen.
9. Add an `EffectScreenData` entry in `EffectsCatalog.kt`.
10. Verify with `./gradlew :app:compileDebugKotlin`.

Keep effect titles and descriptions consistent between `EffectsCatalog.kt` and route construction.

## Predictive Back Effects

Predictive-back effects live under `navigation/predictive/`. They are not normal catalog effects. When adding one, follow the existing `PredictiveBackEffect` and `PredictiveBackShaderSource` patterns, then verify the picker and navigation gesture flow.

## Build And Verification

Preferred checks:

```bash
./gradlew :app:compileDebugKotlin
./gradlew assembleDebug
```

The Gradle wrapper is already approved for local use in this environment. The project targets Java 17.

## Documentation Notes

- `README.md` is the human-facing project map.
- `SHADER_BOX_USAGE.md` contains an older focused guide for `ShadedBox`.
- `BUILD_SETUP.md` contains local IDE and Java setup notes.
- Keep this file updated when the effect registration path, shader wrapper, generated function flow, or shared `ShadedBox` behavior changes.
