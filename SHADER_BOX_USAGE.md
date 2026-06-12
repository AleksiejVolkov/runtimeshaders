# ShadedBox Composable

`ShadedBox` is the shared Compose component for applying an AGSL `RuntimeShader` to UI. It lives in
`app/src/main/java/com/offmind/runtimeshaders/composables/Uitls.kt` (filename is misspelled — the
composable itself is `ShadedBox`).

> Pair it with the `Shader` wrapper (`shaders/Shader.kt`) rather than building a `RuntimeShader`
> by hand — see [`README.md`](README.md) and [`PROJECT_MAP.md`](PROJECT_MAP.md) for the shader
> plumbing and the generated helper-function system.

## What It Does

- Applies an AGSL shader to a `Box`, either as a post-process **render effect over child content**
  or as a **direct shader draw** with no content.
- Manages the `resolution` uniform automatically from the layout size.
- Optionally advances a `time` uniform for animation.
- Maps typed uniform values (`ShaderTypedValue`) to the shader before each draw.

## Signature

```kotlin
@Composable
fun ShadedBox(
    modifier: Modifier = Modifier,
    shader: RuntimeShader,
    shaderUniforms: Map<String, ShaderTypedValue> = emptyMap(),
    includeTime: Boolean = false,
    content: (@Composable () -> Unit)? = null,
)
```

## Two Modes

**With `content`** — the shader post-processes the composable children via
`RenderEffect.createRuntimeShaderEffect(shader, "image")` inside a `graphicsLayer`. The shader
**must** declare `uniform shader image;` and sample it (the children are the `image` input).

```kotlin
@Composable
fun ShaderOverContent() {
    val shader = remember { Shader(myAgsl).getRuntimeShader() }
    ShadedBox(
        shader = shader,
        includeTime = true,
        shaderUniforms = mapOf(
            "percentage" to ShaderTypedValue.FloatType(0.5f),
        ),
    ) {
        // any Compose UI — this is what the shader receives as `image`
        Text("Hello")
    }
}
```

**Without `content`** — the shader is drawn directly into the box with `drawBehind` + a framework
`Paint`. Use this for generative backgrounds that don't sample UI. Give the box a size via
`modifier`.

```kotlin
ShadedBox(
    modifier = Modifier.fillMaxSize(),
    shader = remember { Shader(backgroundAgsl).getRuntimeShader() },
    includeTime = true,
)
```

## Uniforms

`resolution` is set automatically on size change. `time` is advanced only when
`includeTime = true` (driven by `provideTimeAsState`, which increments `0.01f` every 10 ms). Pass
everything else through `shaderUniforms` using `ShaderTypedValue`:

| `ShaderTypedValue` | AGSL type | Setter used |
|---|---|---|
| `FloatType(value)` | `float` | `setFloatUniform` |
| `Vec2Type(x, y)` | `vec2` | `setFloatUniform` |
| `Vec3Type(x, y, z)` | `vec3` | `setFloatUniform` |
| `Vec4Type(x, y, z, w)` | `vec4` | `setFloatUniform` |
| `IntType(value)` | `int` | `setIntUniform` |
| `Vec2Array(values)` | `vec2[N]` | `setVec2ArrayUniform` (padded to `maxSize`, default 10) |

The default uniforms declared by `Shader` (`basicUniformList`) are:

```glsl
uniform shader image;
uniform vec2  resolution;
uniform float time;
uniform float percentage;
```

Add shader-specific uniforms before building the `RuntimeShader`:

```kotlin
val shader = remember {
    Shader(myAgsl).getRuntimeShader(
        uniforms = basicUniformList
            .addUniform(Uniform.Type.VEC2 to "pointer")
            .removeUniform("percentage"),
    )
}
```

Then supply matching runtime values via `shaderUniforms` (e.g. `"pointer" to Vec2Type(x, y)`).

## Notes

- The shader is mutated in place each frame; `ShadedBox` does not recreate it. Wrap creation in
  `remember` so it survives recomposition.
- For effects that need finer control than `ShadedBox` offers (e.g. per-entry render effects, the
  predictive-back dissolve, or the lighting scope), apply
  `RenderEffect.createRuntimeShaderEffect(...).asComposeRenderEffect()` directly in a
  `graphicsLayer` — see `navigation/predictive/PredictiveBackDissolve.kt` and
  `screens/effects/lighting/LightingScope.kt` for examples.
- Don't fork local copies of `ShadedBox`; extend the shared one only if the behavior is broadly
  useful.
