# Chroma Studio — Android (Kotlin + Jetpack Compose + Haze)

A native Android port of `index.html`'s design system and editing workflow: color tokens,
glassmorphism via [Haze](https://github.com/chrisbanes/haze), the app bar, layer stack,
HSV color picker, draggable gradient stops, mesh/blob handles, Global FX panel with Apple
presets, the mobile bottom drawer, undo/redo, color-blind simulation, contrast checking,
and post-FX overlays.

## Open it
1. Unzip, open the `ChromaStudioAndroid` folder in Android Studio (Koala+).
2. Let Gradle sync — it pulls Compose BOM 2024.06.00 and `dev.chrisbanes.haze:haze:0.7.3`.
3. Run on a device/emulator with **API 26+** (blur looks best on API 31+, where Haze uses
   real `RenderEffect`; below that it falls back to the tinted-glass background color so it
   never looks broken, just less blurry).

## What's implemented
- **Colors** (`ui/theme/Color.kt`) — every `--bg-color`, `--glass-*`, `--primary*`,
  `--text-*` hex value for both `:root` and `body.dark`, animated on toggle (400ms) like
  the CSS `transition: background-color 0.4s ease`.
- **Glassmorphism** (`ui/components/GlassComponents.kt`) — `.glass-panel`'s
  `background + backdrop-filter: blur(16px) + border + shadow` via `Modifier.hazeChild`.
- **HSV color picker** (`ColorPickerDialog.kt`) — saturation/value drag square, hue slider,
  alpha slider, live hex field, tap a color stop to open it.
- **Draggable color stops** (`ColorStopEditor.kt`) — drag to reposition, tap to edit color,
  double-tap empty track to insert a stop, long-press to remove one.
- **Layer stack** (`LayersPanel.kt`, `LayerCard.kt`) — type grid, blend-mode pills, opacity
  and angle sliders, long-press-drag to reorder, delete with confirmation modal.
- **Mesh/blob handles** (`MeshBlobHandlesOverlay.kt`) — draggable control points over the
  canvas for MESH/BLOB layers (simplified — see below).
- **Global FX panel** (`GlobalFxPanel.kt`) — per-layer animate/speed/intensity sliders, the
  8 Apple-style gradient presets, color-blind mode picker, contrast-checker and post-FX
  toggles.
- **Mobile bottom drawer** (`BottomDrawer.kt`) — drag-handle that snaps between
  collapsed/mid/full heights, with a Layers / Global FX tab switcher.
- **Undo/redo** — full layer-stack history snapshots wired to the app bar.
- **Color-blind simulation & contrast checker** (`CanvasPreview.kt`, `ContrastChecker.kt`) —
  approximate protanopia/deuteranopia/tritanopia color matrices, live WCAG contrast ratio
  badge with an auto-fix wand.
- **Halftone/dither post-FX** — dot-grid and noise overlays drawn on top of the composited
  gradient.
- **Canvas shapes** — rounded/circle/full, plus a text-masked shape that clips the gradient
  to glyph shapes via `BlendMode.DstIn`.
- **Gradient rendering** — linear/radial/conic brushes composited with real `BlendMode`s,
  matching `mix-blend-mode` per layer.

## What's intentionally simplified (flag if you want these tightened up)
A few pieces are approximations rather than pixel-exact ports, because the source behavior
needs its own dedicated engine, not just a Compose equivalent:
- **Mesh gradient warp**: the real app interpolates color per-pixel across an arbitrary mesh
  of points; this port renders a single radial gradient centered on the *average* of the
  mesh points, with real draggable handles, rather than a true per-point mesh interpolation
  shader.
- **Blob feather/opacity handles**: only position is draggable; per-point feather, opacity,
  and width/height aren't exposed yet.
- **Halftone/dither**: fixed-pattern approximations (a dot grid and a seeded noise field)
  rather than true image-space post-processing shaders.
- **Color-blind matrices**: standard simplified Brettel/Viénot-style approximations, applied
  per-layer at draw time rather than to the final composited frame.
- **Fonts**: `Inter` / `JetBrains Mono` aren't bundled (network-restricted build
  environment) — falls back to system sans/mono. Drop `.ttf` files into `res/font/` and wire
  them up in `ui/theme/Theme.kt` (`InterFontFamily` / `MonoFontFamily`) for pixel-exact type.

Happy to tighten any of these further — the mesh interpolation shader and full blob gizmo
inspector are the biggest remaining chunks.
