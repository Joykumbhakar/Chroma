# Chroma Studio — Android (Kotlin + Jetpack Compose + Haze)

A native Android port of `index.html`'s design system: exact color tokens, glassmorphism
via [Haze](https://github.com/chrisbanes/haze), the app bar, layer stack, gradient type
picker, color stops, blend modes, and light/dark theme swap.

## Open it
1. Unzip, open the `ChromaStudioAndroid` folder in Android Studio (Koala+).
2. Let Gradle sync — it pulls Compose BOM 2024.06.00 and `dev.chrisbanes.haze:haze:0.7.3`.
3. Run on a device/emulator with **API 26+** (blur looks best on API 31+, where Haze uses
   real `RenderEffect`; below that it falls back to the tinted-glass background color so it
   never looks broken, just less blurry).

## What's a 1:1 port
- **Colors** (`ui/theme/Color.kt`): every `--bg-color`, `--glass-*`, `--primary*`, `--text-*`
  hex value for both `:root` and `body.dark` copied exactly.
- **Glassmorphism** (`ui/components/GlassComponents.kt`): `.glass-panel`'s
  `background + backdrop-filter: blur(16px) + border + shadow` → `Modifier.hazeChild` +
  matching background/border/shadow.
- **Layout**: 64dp app bar → layer stack panel, matching the `.app-bar` height and
  `--radius-md` panel corners.
- **Layer model** (`model/Models.kt`, `viewmodel/ChromaViewModel.kt`): mirrors the JS
  `Layer` class — type, blend mode, opacity, angle, color stops, `addLayer()`,
  `deleteLayer()`, and a Kotlin port of `_performRandomize()`.
- **Gradient rendering** (`ui/components/CanvasPreview.kt`): linear/radial/conic (sweep)
  brushes composited with real `BlendMode`s per layer, matching `mix-blend-mode`.
- **Dark mode toggle**: animates every token over 400ms, same as the CSS
  `transition: background-color 0.4s ease`.

## What's intentionally out of scope (flag if you want these next)
This file is a full gradient/mesh *editor* (2,800+ lines of JS) — a few advanced pieces
weren't ported because they need dedicated custom-gesture engines of their own, not just
Compose Brush/Canvas equivalents:
- The **mesh-gradient warp gizmo** and **blob feather/opacity handles** (draggable control
  points over the canvas).
- **Halftone / dither post-FX** shader overlays.
- **Color-blind simulation** and the **contrast-ratio checker + auto-fix wand**.
- **Undo/redo history stack** and **view-transition-style randomize animation**.
- Draggable color-stop repositioning and drag-to-reorder layers (the visuals are ported;
  the drag gesture handlers are stubbed as a next step).

Happy to build out any of these next — the mesh/blob gizmo and drag-reorder are the
biggest chunks of remaining work.
