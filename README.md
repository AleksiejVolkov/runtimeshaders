# Runtime Shaders

Android shader playground for AGSL runtime shader effects, Compose examples, OpenGL scenes, and predictive-back shader transitions.

## Project Map

- `app/src/main/java/com/offmind/runtimeshaders/screens/effects/` contains the example effect screens.
- `app/src/main/java/com/offmind/runtimeshaders/screens/EffectsCatalog.kt` controls what appears in the effects list.
- `app/src/main/java/com/offmind/runtimeshaders/navigation/Routes.kt` defines Navigation 3 route keys.
- `app/src/main/java/com/offmind/runtimeshaders/navigation/AppNavDisplay.kt` maps routes to composable screens.
- `app/src/main/java/com/offmind/runtimeshaders/shaders/Shader.kt` wraps AGSL code, declares uniforms, and injects generated helper functions.
- `app/src/main/java/com/offmind/runtimeshaders/shaders/ShadersCollection.kt` stores shared AGSL effect snippets used by several screens.
- `app/src/main/java/com/offmind/runtimeshaders/composables/Uitls.kt` contains `ShadedBox`, `provideTimeAsState`, and shader uniform application helpers.
- `buildSrc/src/main/java/com/offmind/runtimeshaders/functions/` contains reusable AGSL helper functions such as noise, SDF, easing, color correction, and transformations.
- `buildSrc/src/main/java/com/offmind/runtimeshaders/scripts/` generates the shader dependency map used by `Shader`.
- `app/src/main/java/com/offmind/runtimeshaders/gl/` contains OpenGL renderer, scene, geometry, and GL shader support.
- `app/src/main/java/com/offmind/runtimeshaders/navigation/predictive/` contains predictive-back shader effects and state.

## Shared Shader Helpers

Most Compose shader screens should use `Shader(...).getRuntimeShader(...)` instead of creating `RuntimeShader` directly. The `Shader` wrapper prepends uniforms and injects reusable AGSL helper functions from the generated `ShaderDependencyMap`.

The generated file is created at build time:

```text
app/build/generated/src/main/java/com/offmind/runtimeshaders/generated/ShaderDependencyMap.kt
```

Generation is registered by `registerGenerateShaderFunctionsTask(tasks)` in `app/build.gradle.kts`, and `preBuild` depends on `generateShaderDependencyMap`.

Helper functions live in:

```text
buildSrc/src/main/java/com/offmind/runtimeshaders/functions/
```

Each function file exports an `all...Functions` map. The generator combines those maps, scans references between functions, and emits both `ShaderFunction` enum values and dependency lists. If a shader calls `FBM`, for example, its dependency on `Noise` and `Hash21` is detected and generated automatically.

By default, `Shader.getRuntimeShader()` includes all generated functions. For narrower shader sources, pass a smaller `customFunctions` set.

## Shared Shaded Box

Use `ShadedBox` from:

```text
app/src/main/java/com/offmind/runtimeshaders/composables/Uitls.kt
```

`ShadedBox` supports two modes:

- With `content`: applies `RenderEffect.createRuntimeShaderEffect(shader, "image")` to the composable children. The shader must include an `image` shader uniform.
- Without `content`: draws the shader directly behind the box using Compose canvas.

`ShadedBox` always updates `resolution` from layout size. Set `includeTime = true` when the shader expects `time` to be advanced automatically. Other uniforms are passed through `shaderUniforms` using `ShaderTypedValue`.

The default uniforms declared by `Shader` are:

```text
uniform shader image;
uniform vec2 resolution;
uniform float time;
uniform float percentage;
```

Use `basicUniformList.addUniform(...)` and `basicUniformList.removeUniform(...)` for shader-specific uniform lists.

## Adding A New Compose Shader Effect

1. Create a screen in `app/src/main/java/com/offmind/runtimeshaders/screens/effects/`.
2. Put the AGSL source either in that screen for local-only shaders or in `ShadersCollection.kt` if it is shared.
3. Create the shader with `remember { Shader(source).getRuntimeShader(...) }`.
4. Use `ShadedBox` to render the shader over content or as a direct shader surface.
5. Add custom uniforms with `basicUniformList.addUniform(Uniform.Type... to "name")`, then pass runtime values with `shaderUniforms`.
6. Add a route in `Routes.kt`.
7. Add the route entry in `routeEntryProvider(...)` and the screen mapping in `RouteContent(...)` in `AppNavDisplay.kt`.
8. Add a visible item in `EffectsCatalog.kt`.
9. Build with `./gradlew :app:compileDebugKotlin` or `./gradlew assembleDebug`.

## Adding A New Reusable AGSL Function

1. Add the function string to the closest file in `buildSrc/src/main/java/com/offmind/runtimeshaders/functions/`, or create a new file in that package.
2. Add the function to that file's `all...Functions` map.
3. If the new file has a new aggregate map, include it in `allFunctions` inside `createDependencies.kt`.
4. Run `./gradlew generateShaderDependencyMap` or any normal app build.
5. Use the function name directly inside shader source. The generated dependency map handles function ordering and dependencies.

Function names should match the AGSL function name exactly in the map key, because the generator uses those keys for dependency scanning and enum generation.

## Build

The project uses Gradle wrapper, Android Gradle Plugin, Kotlin, Compose, and Java 17.

```bash
./gradlew :app:compileDebugKotlin
./gradlew assembleDebug
```

See `BUILD_SETUP.md` for local IDE and Java setup notes.

## Related Docs

- `AGENTS.md` gives coding-agent instructions and a concise project workflow.
- `SHADER_BOX_USAGE.md` describes basic `ShadedBox` usage.
- `PREDICTIVE_BACK_SHADER_NAV3_ARTICLE.md` documents the predictive-back shader work.
