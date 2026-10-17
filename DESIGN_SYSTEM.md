# 🎨 ChromaStudio & macOS Interface — Permanent Design System

This document is the canonical, permanent specification for UI components, interaction physics, and design tokens across the web and native Android (Jetpack Compose) environments.

---

## 1. Interactive Controls Specification

### A. Liquid Glass Toggle Switch

The Liquid Glass Toggle provides a tactile, skeuomorphic iOS/macOS toggle with glossy refraction highlights, an ambient light rim, and liquid jelly elastic stretch on press.

#### Proportions & Geometry
| Property | Value | Notes |
| :--- | :--- | :--- |
| **Track Dimensions** | `44px` × `22px` (`44.dp` × `22.dp`) | Compact pill shape |
| **Track Border Radius** | `100px` (`CircleShape`) | Full round ends |
| **Knob Dimensions (Resting)** | `24px` × `18px` (`24.dp` × `18.dp`) | Elliptical pill knob |
| **Knob Dimensions (Active/Pressed)**| `28px` × `18px` (`28.dp` × `18.dp`) | Horizontal jelly stretch |
| **Knob Active Scale** | `1.26x` | Knob expands and pops outward |
| **Off Position** | `translateX(0px)` | Resting at left border |
| **Off Active Position** | `translateX(-3px)` | Overshoot into left track border |
| **On Position** | `translateX(16px)` | Resting at right border |
| **On Active Position** | `translateX(15px)` | Elastic compression on press |

#### Color Tokens
- **Track (Off)**: `#e5e5ea` (with inset shadow `inset 0 2px 5px rgba(0,0,0,0.15)`)
- **Track (On)**: `#34c759` (Apple System Green, inset shadow `inset 0 2px 5px rgba(0,0,0,0.2)`)
- **Knob Base**: `rgba(255, 255, 255, 0.1)` with `backdrop-filter: blur(5px)`
- **Knob Top Gloss Highlight (`::before`)**:
  - Gradient: `linear-gradient(to bottom, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.1) 100%)`
  - Height: `48%`
- **Knob Bottom Ambient Light Rim (`::after`)**:
  - Gradient: `linear-gradient(to top, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0) 100%)`
  - Height: `35%`

#### HTML / CSS Implementation
```html
<label class="toggle-wrapper">
    <input type="checkbox" class="toggle-input" checked>
    <div class="toggle-track"></div>
    <div class="toggle-knob"></div>
</label>
```

#### Jetpack Compose Implementation
- File: `app/src/main/java/com/chroma/studio/ui/components/AppleComponents.kt` (`AppleSwitch`)
- Animation: Spring with `dampingRatio = 0.5f, stiffness = 600f`.

---

### B. Settings Range Slider (Liquid Jelly Knob)

The Settings Range Slider reproduces the macOS / iOS Settings tactile slider featuring a dual-gradient liquid crystal knob that dynamically scales and elongates when dragged.

#### Proportions & Geometry
| Property | Value | Notes |
| :--- | :--- | :--- |
| **Container Height** | `22px` (`22.dp` - `28.dp`) | Full touch target |
| **Track Height** | `6px` (`6.dp`) | Slim pill track |
| **Track Border Radius** | `100px` (`CircleShape`) | Continuous rounded end-caps |
| **Thumb Dimensions (Resting)** | `24px` × `18px` (`24.dp` × `18.dp`) | Pill-shaped glass knob |
| **Thumb Dimensions (Active/Dragged)**| `28px` × `18px` (`28.dp` × `18.dp`)| 4px horizontal stretch |
| **Thumb Active Scale** | `1.15x` | Smooth pop-out scale on touch |
| **Track Margins** | Thumb `margin-top: -6px` | Perfectly centered on 6px track |

#### Color Tokens
- **Track (Active)**: `#0a84ff` (or `#2997ff` to `#0071e3` horizontal gradient)
- **Track (Inactive)**: `#e5e5ea` (Light) / `rgba(255, 255, 255, 0.15)` (Dark)
- **Track Shadow**: `inset 0 2px 5px rgba(0, 0, 0, 0.15), inset 0 0 0 0.5px rgba(0, 0, 0, 0.05)`
- **Thumb Gradient Stack**:
  1. Top Specular: `linear-gradient(to bottom, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.1) 45%, transparent 50%)`
  2. Bottom Ambient: `linear-gradient(to top, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0) 40%, transparent 50%)`
  3. Base: `rgba(255, 255, 255, 0.9)` with `backdrop-filter: blur(5px)`
- **Thumb Shadow**: `0 2px 5px rgba(0, 0, 0, 0.2), 0 0 0 0.5px rgba(0, 0, 0, 0.1), inset 0 1px 2px rgba(255, 255, 255, 0.8), inset 0 -1px 2px rgba(0, 0, 0, 0.1)`

#### HTML / CSS Implementation
```html
<input type="range" class="settings-slider" min="0" max="100" value="70" 
       style="--value: 70%;" 
       oninput="this.style.setProperty('--value', this.value + '%')">
```

#### Jetpack Compose Implementation
- File: `app/src/main/java/com/chroma/studio/ui/components/AppleComponents.kt` (`AppleSlider`)
- Thumb composable: `LiquidGlassKnob(width = thumbWidth, height = 18.dp, scale = thumbScale, baseAlpha = 1.0f)`

---

## 2. Global Design Tokens

### System Palette
- **System Blue (Accent)**: `#0a84ff` / `#0071e3`
- **System Green (Success/On)**: `#34c759` / `#30d158`
- **System Red (Destructive)**: `#ff453a` / `#ff3b30`
- **System Orange**: `#ff9f0a` / `#ff9500`
- **Inactive Track**: `#e5e5ea` (Light) / `#38383a` (Dark)
- **Background**: `#f2f2f7` (Light) / `#121316` (Dark)

### Typography
- Primary: `-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Inter", sans-serif`
- Code/Monospace: `"SF Mono", "JetBrains Mono", Menlo, Consolas, monospace`

---

## 3. Reference Files & Locations
- **Interactive Component Template**: `e:\MacOS-Ineterface\ui-components.html`
- **Master UI Design System Showcase**: `e:\MacOS-Ineterface\macOS-design-system.html`
- **Global Web Styles**: `e:\MacOS-Ineterface\style.css`
- **Jetpack Compose Components**: `app/src/main/java/com/chroma/studio/ui/components/AppleComponents.kt`
