# Jelly Slider Research Notes

Generated: 2026-05-12

This document summarizes the Jelly Slider component/effect and how it may map to the current Android Compose + OpenGL integration.

## References

- TypeGPU live example: https://docs.swmansion.com/TypeGPU/examples/#example=rendering--jelly-slider
- Source code: https://github.com/software-mansion/TypeGPU/blob/main/apps/typegpu-docs/src/examples/rendering/jelly-slider/index.ts
- Slider physics source: https://github.com/software-mansion/TypeGPU/blob/main/apps/typegpu-docs/src/examples/rendering/jelly-slider/slider.ts
- Constants source: https://github.com/software-mansion/TypeGPU/blob/main/apps/typegpu-docs/src/examples/rendering/jelly-slider/constants.ts
- Webleb mirror/description: https://www.web-leb.com/en/code/2883
- 80.lv article about Jelly Slider/Switch: https://80.lv/articles/you-ll-want-to-play-with-this-delicious-typegpu-jelly-switch-all-day-long
- Reddit/WebGPU thread with implementation notes: https://www.reddit.com/r/webgpu/comments/1otbsu5/jelly_slider_in_typegpu/
- Reddit/GraphicsProgramming thread with refraction note: https://www.reddit.com/r/GraphicsProgramming/comments/1otfas5/jelly_slider_in_typegpu/
- TypeGPU repo: https://github.com/software-mansion/TypeGPU

## What It Is

The popular "Jelly Slider" is a WebGPU/TypeGPU example by Software Mansion/Konrad Reczko, inspired by Voicu Apostol's jelly UI design work.

It is not primarily a Three.js scene. It is a custom WebGPU renderer built with TypeGPU, SDF helpers, compute-generated data textures, and raymarching.

The visual target:

- draggable jelly-like slider body
- elastic deformation under drag
- translucent/refractive material
- soft internal scattering/tint
- fake/real-ish shadows
- background/ground interaction
- number/percentage rendering on the ground
- high-end "liquid glass / gummy" look

## Core Implementation Ideas In The Reference

### 1. Slider Shape Is A Simulated Polyline

The `Slider` class creates a chain of points from start to end.

Current reference values:

- `NUM_POINTS = 17`
- endpoints are pinned
- interior points move
- drag controls the end cap X
- Verlet-like integration is used
- distance constraints keep segments near a rest length
- bending constraints resist sharp folding
- arch force raises the middle when compressed

Important physics parameters from `slider.ts`:

```text
iterations = 16
substeps = 6
damping = 0.01
bendingStrength = 0.1
archStrength = 2
endFlatCount = 1
endFlatStiffness = 0.05
bendingExponent = 1.2
archEdgeDeadzone = 0.01
```

The point chain is converted into Bezier control points each frame.

### 2. Bezier Field Is Precomputed Into A Texture

The reference computes a texture over the slider bounding box.

Texture size:

```text
BEZIER_TEXTURE_SIZE = 256 x 128
format = rgba16float
```

Each texel stores:

```text
R = unsigned distance to nearest Bezier segment
G = progress along slider
B/A = approximate 2D normal
```

This texture is then sampled by rendering/raymarching code to evaluate the slider body quickly.

### 3. Slider Becomes A 3D SDF

The 2D Bezier distance field is extruded into 3D:

- `opExtrudeZ(...)`
- line radius
- half thickness
- special end cap SDF for the final rounded/pie cap

Relevant constants:

```text
LINE_RADIUS = 0.024
LINE_HALF_THICK = 0.17
```

This makes the jelly slider a real volumetric SDF object rather than a normal polygon mesh.

### 4. Rendering Uses Raymarching

Main rendering constants:

```text
MAX_STEPS = 64
MAX_DIST = 10
SURF_DIST = 0.001
```

The scene raymarches:

- ground/background
- slider SDF
- slider bounding box for early reject
- ambient occlusion
- fake shadows
- internal/refraction logic

### 5. Refraction Is More Physical Than Our Cube Approximation

The Reddit/GraphicsProgramming thread includes a concise explanation from the authors/community:

> When light hits the slider, it calculates the refraction using Snell's law, does a short internal march through the jelly, and then continues marching without the jelly to find where it hits the background.

The source uses:

- `JELLY_IOR = 1.42`
- Fresnel Schlick approximation
- `refract`-style direction computation
- Beer-Lambert absorption
- scatter tint
- second march outside the jelly to sample the environment/background

Jelly material constants:

```text
JELLY_IOR = 1.42
JELLY_SCATTER_STRENGTH = 3
SPECULAR_POWER = 10
SPECULAR_INTENSITY = 0.6
AMBIENT_INTENSITY = 0.6
```

### 6. Performance Is Nontrivial

The TypeGPU demo is optimized and still expensive. The Reddit/WebGPU discussion mentions high GPU load on desktop in some cases, and the source includes an auto-quality mode using timestamp queries.

The source renders internally at a `qualityScale`, initially around `0.5`, and auto-selects quality based on measured GPU frame time.

Takeaway: a direct Android OpenGL ES 2 raymarching port could be expensive, especially if we also capture Compose textures frequently.

## What We Can Reuse Conceptually

We should not port TypeGPU itself. Instead, we can reuse the design:

1. Simulate a draggable polyline/chain on CPU or GPU.
2. Convert the chain into renderable geometry or a small distance field.
3. Render a translucent 3D-ish jelly body in OpenGL.
4. Sample our captured Compose texture as background.
5. Use simplified refraction first.
6. Add higher quality internal/refraction passes only if performance allows.

## Mapping To Current Android Architecture

Current integration documented in:

```text
OPENGL_COMPOSE_INTEGRATION.md
```

Useful existing pieces:

- `CapturedBackgroundGlBox`: captures Compose background region under GL surface.
- `EmbeddedGlSurface`: owns `AndroidEmbeddedExternalSurface`.
- `GLRenderer`: owns EGL, GL texture upload, render loop.
- `GlScene`: scene lifecycle abstraction.
- `GlMesh`: mesh vertex buffer abstraction.
- `GlProgram`: shader/program helper.

For a Jelly Slider prototype, likely new files:

```text
gl/scene/JellySliderScene.kt
gl/geometry/JellySliderMesh.kt
gl/physics/JellySliderPhysics.kt
gl/material/JellyGlassMaterial.kt   (optional; shader source could live in scene first)
```

## Practical Android/OpenGL Plan

### Phase 1: Mesh-Based Jelly Slider

Avoid full SDF/raymarching first.

Build the slider as a deformable mesh:

- CPU-side point chain, 12-24 points.
- Verlet integration or spring constraints.
- Generate a rounded ribbon mesh from centerline points.
- Use normals/tangents for fake thickness/refraction.
- Add rounded caps.
- Feed vertices to `GlMesh`.

This is much cheaper than raymarching and fits our current OpenGL ES 2 setup.

Visual approximation:

- ribbon has width/thickness
- color is translucent orange/amber
- surface normals come from 2D centerline normal + fake z curvature
- fragment shader samples Compose background texture with UV offset from normal
- add Fresnel/rim/specular/internal edge tint like current glass cube

This should be enough to validate interaction and feel.

### Phase 2: Better "Jelly" Physics

Implement a CPU physics class inspired by TypeGPU `Slider`:

```kotlin
class JellySliderPhysics(
    pointCount: Int,
    start: Vec2,
    end: Vec2
) {
    fun setDragX(x: Float)
    fun update(dt: Float)
    val points: List<Vec2>
    val normals: List<Vec2>
}
```

Needed:

- pinned first point
- dragged last point
- previous positions for Verlet
- inverse mass per point
- segment length constraints
- bending constraints
- arch/compression force
- endpoint flattening

Start with fewer iterations than TypeGPU:

```text
substeps = 2-4
iterations = 4-8
```

Increase only if needed.

### Phase 3: Texture/Background Integration

Use `CapturedBackgroundGlBox` exactly like the cube:

- `JellySliderScene` receives background texture ID through `GlFrameInfo`
- fragment shader samples `uBackgroundTexture`
- refraction offsets should be local to the jelly body
- `captureVersion` should be driven by actual Compose state changes, not polling

For interaction:

- touch/drag state can be passed into `JellySliderScene`
- background capture should only update if background changes
- slider movement does not require recapturing background if background is static

This is an important difference from earlier PixelCopy experiments.

### Phase 4: Optional SDF/Distance Field

If mesh approximation is not good enough:

- generate a small distance texture on CPU or GL
- store distance/progress/normal like TypeGPU
- draw a full-screen quad over the slider bounds
- fragment shader evaluates distance field and material

This is closer to TypeGPU but still avoids full 3D raymarching.

Potential compromise:

- 2D SDF for silhouette/soft edges
- fake 3D normal from distance gradient
- no volumetric internal march

### Phase 5: Full Raymarch Only If Necessary

A full raymarching port would need:

- SDF for slider body
- ground/background model
- normal estimation
- Fresnel
- Beer-Lambert absorption
- short internal ray march
- environment/background ray continuation

This could look best, but it is the most expensive and least aligned with the current OpenGL ES 2/mobile target.

## Key Design Decision For This Project

Do not start with AGSL raymarching.
Do not start with full TypeGPU-style raymarching.

Start with:

```text
CPU jelly physics + dynamic GL mesh + Compose background texture + glass/refraction shader
```

This matches the project direction:

- real geometry
- composable integration
- transparent GL surface
- captured Compose texture
- no huge AGSL shader
- easier iteration than SDF raymarching

## Open Questions Before Implementation

1. Is the slider meant to be horizontal only at first?
2. Should it be a real input control with value output, or visual-only first?
3. Should the draggable thumb/end cap be part of the same jelly body?
4. What surface size should the GL slider occupy?
5. Should the Compose background behind it be static, scrollable, or controlled demo content?
6. Should the first prototype use the captured Compose texture, or a simple generated checker/gradient texture for easier debugging?

## Risks

- Dynamic mesh generation every frame may create allocations if not carefully implemented.
- Transparent GL + Compose capture texture is already a copy-based path, so avoid extra full-screen captures.
- OpenGL ES 2 has limitations versus WebGPU; avoid depending on texture arrays, compute shaders, storage textures, or advanced derivatives in the first prototype.
- High-quality volumetric jelly is expensive.

## First Prototype Recommendation

Implement `JellySliderScene` as:

- CPU physics chain with 15-17 points.
- Dynamic triangle strip/ribbon mesh.
- Rounded caps approximated with fan geometry.
- Fragment shader:
  - sample background texture
  - distort by fake normal
  - tint amber/orange
  - add rim/specular
  - alpha around 0.85-0.95
- Touch input:
  - drag maps to end point X
  - physics updates every GL frame

This should produce a believable jelly control without committing to expensive raymarching.
