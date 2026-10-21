# Runtime Shaders

Android shader playground for AGSL runtime shader effects, Compose examples, OpenGL scenes, and predictive-back shader transitions. The app opens to grouped collections and navigates through them to individual demos using **Navigation 3**.

Three rendering techniques coexist:

1. **AGSL `RuntimeShader`** — most effects, via the shared `Shader` wrapper and `ShadedBox`.
2. **OpenGL ES** (`gl/`) — 3D scenes drawn on an embedded surface, optionally sampling a captured Compose background as a texture.
3. **Predictive-back shader transitions** (`navigation/predictive/`) — the back gesture dissolves the outgoing screen with a selectable AGSL effect.

## Project Map

- `app/src/main/java/com/offmind/runtimeshaders/screens/effects/` contains the example effect screens.
- `app/src/main/java/com/offmind/runtimeshaders/screens/EffectsCatalog.kt` defines the collection tree and registers each demo screen in one place.
- `app/src/main/java/com/offmind/runtimeshaders/navigation/Routes.kt` defines Navigation 3 route keys.
- `app/src/main/java/com/offmind/runtimeshaders/navigation/AppNavDisplay.kt` maps routes to composable screens and drives the predictive-back gesture.
- `app/src/main/java/com/offmind/runtimeshaders/shaders/Shader.kt` wraps AGSL code, declares uniforms, and injects generated helper functions.
- `app/src/main/java/com/offmind/runtimeshaders/shaders/ShadersCollection.kt` stores shared AGSL effect snippets used by several screens.
- `app/src/main/java/com/offmind/runtimeshaders/composables/Uitls.kt` contains `ShadedBox`, `provideTimeAsState`, and shader uniform application helpers.
- `buildSrc/src/main/java/com/offmind/runtimeshaders/functions/` contains reusable AGSL helper functions such as noise, SDF, easing, color correction, and transformations.
- `buildSrc/src/main/java/com/offmind/runtimeshaders/scripts/` generates the shader dependency map used by `Shader`.
- `app/src/main/java/com/offmind/runtimeshaders/gl/` contains the OpenGL renderer, scenes, geometry, embedded-surface composables, and Compose background capture.
- `app/src/main/java/com/offmind/runtimeshaders/screens/effects/lighting/` contains the `LightingScope` DSL (scoped `lightSource` / `receivesLight` modifiers and their shaders).
- `app/src/main/java/com/offmind/runtimeshaders/navigation/predictive/` contains predictive-back shader effects and state; `data/AppSettingsRepository.kt` persists the chosen effect.

For the full architecture, file-by-file breakdown, and tech-stack versions, see [`PROJECT_MAP.md`](PROJECT_MAP.md). Agents should start with [`AGENTS.md`](AGENTS.md).

## App Appearance

Open **Settings → Appearance** to choose **Neumorphism**, **Glass**, or the original **Classic**
style. The catalog, nested collections, settings and pickers update immediately. The choice is
saved between launches independently of the predictive-back effect. Demo screens keep their own
design so their materials can still be compared. Shared chrome components in
`ui/components/AppChrome.kt` reuse the same glass and neumorphic shaders as the widget demos.

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
6. Add an `EffectScreenData` entry to the appropriate category in `EffectsCatalog.kt`:

   ```kotlin
   EffectScreenData(
       id = "my-effect",
       title = "My Effect",
       description = "What the demo shows",
   ) { MyEffectScreen() }
   ```

7. Build and verify with `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.

The home screen groups demos into **Widgets**, **OpenGL scenes**, **Visual effects** and
**Navigation**. Widgets contains **Glass**, **Neumorphism**, **Lighting** and **Controls**.
Nest an `EffectCategory` to add another collection at any depth. New demos and collections need
no changes in `Routes.kt` or `AppNavDisplay.kt`. Keep IDs unique and stable when changing titles or
moving entries, since restored navigation uses those IDs. Extend the expected demo set in
`EffectCatalogTest` when adding a demo.

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
