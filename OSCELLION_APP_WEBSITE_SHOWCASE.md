# Oscellion — Creative Studio & Encrypted Social Network
> **Where creativity meets community.** Real-time shader rendering, generative art, and end-to-end encrypted messaging designed for creators.

---

## 📱 Release & APK Download Information

| Specification | Details |
| :--- | :--- |
| **Application Name** | **Oscellion** (Chroma Studio) |
| **Package Name** | `com.chroma.studio` |
| **Current Version** | `v1.0.0-release` (Stable) |
| **Release Type** | Official Production Signed APK |
| **Signature Scheme** | **APK Signature Scheme v2 (Verified)** |
| **File Name** | `app-release.apk` |
| **File Size** | `~171.14 MB` |
| **SHA-256 Checksum** | `A9A7CE2473361AE134DA42992D9430C231CEDA88E76F592B2D23FCCB5FCD474B` |
| **Target Android OS** | Android 8.0 (Oreo / API 26) through Android 15 (Vanilla Ice Cream / API 35) |
| **Recommended Devices** | Samsung Galaxy (One UI 5 / 6 / 7), Google Pixel, OnePlus, Xiaomi |
| **Architectures** | `arm64-v8a`, `armeabi-v7a`, `x86_64` |

---

## 🌟 Hero Overview

**Oscellion** is a next-generation Android creative platform that merges a professional-grade GPU gradient & shader design engine with a privacy-first, end-to-end encrypted creator social network. Built with Jetpack Compose, Material 3, and Samsung One UI design philosophy, Oscellion delivers a fluid, frictionless, and distraction-free creative workspace.

---

## 🎨 Core Features & Architecture

### 1. Multi-Layer Real-Time Shader Studio
- **4 GPU-Accelerated Rendering Engines**:
  - **Linear Engine**: Precision multi-stop linear gradients with customizable angle vectors ($0^\circ$ to $360^\circ$).
  - **Radial Engine**: Circular light diffusion with focal point dragging and radius controls.
  - **Conic (Sweep) Engine**: Angular chromatic wheels for prism and rainbow effects.
  - **Mesh / Noise Shaders**: Real-time generative procedural mesh noise with GPU fragment shaders.
- **Infinite Stacking Pipeline**: Stack unlimited color layers with opacity blending, visibility toggling, layer reordering, and instant layer duplication.
- **Aspect Ratio Presets**:
  - `Card` ($4:5$ and $9:16$ for social sharing)
  - `Circle` (Profile pictures and avatars)
  - `Full` ($9:19.5$ edge-to-edge mobile wallpaper)
  - `Text / Banner` (Header graphics and badges)
- **Developer Handoff**: One-tap export to CSS gradients, SVG vectors, and GLSL fragment shader code.

---

### 2. Dual Authentication & Custom Oscellion ID
- **Google One-Tap OAuth**: Authenticate in seconds with your verified Google account.
- **Custom Oscellion ID Email (`username@oscellion.com`)**:
  - During identity setup, creators choose their unique `@username`.
  - An official custom email address (`username@oscellion.com`) is generated and linked with their chosen password.
- **Flexible Dual Login**:
  - **Option A (Google Auth)**: One tap "Continue with Google" on any device with your Google account logged in.
  - **Option B (Username & Password)**: Sign in anywhere by typing your `@username` (or `username@oscellion.com`) and password.

---

### 3. End-to-End Encrypted Direct Messaging
- **Cryptographic Security**: Direct chats and voice notes are encrypted client-side using **AES-GCM** and **RSA-2048** key exchange. Servers and network intermediaries cannot inspect private messages.
- **Voice Notes with Live Waveforms**: Record high-fidelity audio notes with dynamic real-time frequency waveforms and scrubbable playback.
- **Ephemeral Messages & Media**: Send disappearing photos, self-destructing text, and rich media drops.
- **Dynamic Island & Heads-Up Alerts**: Subtle, unobtrusive notification capsules that keep you connected without cluttering your canvas.

---

### 4. 24-Hour Instant Drops & Stories
- **Daily Visual & Audio Stories**: Share ephemeral daily inspirations, gradient experiments, and community remixes.
- **Interactive Viewer**: Synchronized story progress bars, direct reactions, comment replies, and creator view counts.

---

### 5. Community Feed & Masterwork Remixing
- **Global Creator Feed**: Explore trending masterworks, video drops, and generative gradient projects.
- **Artwork Remix Engine**: Fork any public artwork into your own multi-layer studio canvas, remix colors and engine parameters, and publish back to the community with automatic attribution.
- **Creator Profiles**: Custom header banners, avatar presets, bio, social links, follower tracking, and verified badges.

---

### 6. Real-Time Activity & Notification Hub
- **Live Event Stream**: Real-time Firestore snapshot listener instantly notifies you when fellow creators like, follow, comment, mention, or remix your artwork.
- **Welcome Notification (`SYSTEM_DROP`)**: New accounts are immediately greeted with an official Oscellion onboarding notification to explore and start creating.
- **Granular Controls**: Filter activity by Likes, Comments, Mentions, Follows, or System announcements, with swipe-to-dismiss and clear-all actions.

---

### 7. Samsung One UI Aesthetic Hierarchy
- **Zero Distraction Canvas**: Elimination of harsh secondary card containers, heavy shadows, and ambient radial glow domes. All content sits naturally on the pristine primary background.
- **Thin, Neutral, Professional Borders**: Subtle hairline `0.5.dp` borders in light mode (`Color(0x1F000000)`) and dark mode (`Color(0x2EFFFFFF)`).
- **Borderless Pill Shapes**: Strict compliance with One UI standards—all action capsules and pill buttons are completely borderless with comfortable touch targets.
- **Flawless Light & Dark Mode**: True OLED dark mode (`#0B0F19`) and crisp daylight mode (`#F8FAFC`) with full accessibility contrast.

---

## 📥 How to Install the APK on Android (Sideload Guide)

If you are downloading the Oscellion APK directly from this website, follow these simple steps:

### Step 1: Download the APK
Click the **"Download Oscellion APK"** button on the website. Your browser may display a prompt saying *"File might be harmful"*—this is standard Android security for apps downloaded outside Google Play. Tap **"Download anyway"**.

### Step 2: Allow Installation from Your Browser
1. Open the downloaded file from your notification tray or **Downloads** folder.
2. If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings**.
3. Toggle on **"Allow from this source"** for your browser (Chrome, Samsung Internet, Firefox, etc.).
4. Press the **Back** button to return to the installer.

### Step 3: Install & Launch
1. Tap **Install**.
2. Once installed, tap **Open**.
3. Sign in using your **Google Account** or choose **Explore as Guest** to immediately start designing.

---

## 🔒 Security & Privacy Verification

- **Signature Integrity**: Every release build is digitally signed with our private production key using **APK Signature Scheme v2**.
- **Tamper Verification**: To verify integrity on your PC:
  ```powershell
  # Using apksigner from Android SDK:
  apksigner verify --verbose app-release.apk
  # Verify SHA-256 Hash:
  Get-FileHash app-release.apk -Algorithm SHA256
  ```
  Expected Hash: `016D9083406D2AC30BB424FE3C42DE1FCAD9A223FD15545B3D7A83CFA512174C`

---

## ❓ Frequently Asked Questions (FAQ)

#### Q: Do I need a Google Account to use Oscellion?
No. You can tap **"Explore Studio as Guest"** to immediately use the multi-layer gradient studio and export your artworks locally without an account. To participate in the global community feed and encrypted messaging, you can sign in with Google or your Oscellion ID.

#### Q: What is my Oscellion ID?
Your Oscellion ID is your custom email address in the format `username@oscellion.com`. When setting up your account, you create a password. You can use your Google Account or this Oscellion ID and password to log in anywhere.

#### Q: Are my direct chats and voice notes private?
Yes. Direct communications use end-to-end cryptographic encryption (AES-GCM & RSA-2048). Only you and the recipient hold the keys to decrypt message contents and audio files.

---

## 📄 Website Landing Page Integration Code

```html
<!-- Ready-to-use Download CTA Card for Website -->
<div class="download-card" style="text-align: center; padding: 40px 24px; max-width: 540px; margin: 0 auto; background: #121826; border-radius: 28px; border: 1px solid rgba(255, 255, 255, 0.1); color: #fff; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
  <h2 style="font-size: 28px; font-weight: 700; margin-bottom: 8px;">Download Oscellion for Android</h2>
  <p style="font-size: 15px; color: #94A3B8; margin-bottom: 24px;">Experience real-time GPU shader design, community remixing, and end-to-end encrypted messaging.</p>
  <a href="/downloads/app-release.apk" download style="display: inline-block; background: #0381FE; color: #fff; font-size: 16px; font-weight: 600; padding: 16px 36px; border-radius: 999px; text-decoration: none; box-shadow: 0 4px 20px rgba(3, 129, 254, 0.4);">
    Download APK (v1.0.0 Stable)
  </a>
  <div style="margin-top: 18px; font-size: 12px; color: #64748B;">
    <span>Size: 171 MB</span> • <span>Requires: Android 8.0+</span> • <span>v2 Verified</span>
  </div>
</div>
```

---
*Created by Joy Kumbhakar and the Oscellion Engineering Team.*
