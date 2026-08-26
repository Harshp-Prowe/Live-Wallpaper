<h1 align="center">HarshFlow</h1>

<p align="center">
  <b>Turn any photo into a living wallpaper.</b><br>
  Gyroscope parallax, touch ripples, drifting particles, aurora light, liquid warp
  and cinematic zoom — no root, no account, fully on-device.
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84">
  <img alt="Language" src="https://img.shields.io/badge/Kotlin-1.9.24-7F52FF">
  <img alt="UI" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4">
  <img alt="Version" src="https://img.shields.io/badge/version-1.0.0-blue">
  <img alt="Network" src="https://img.shields.io/badge/network-none-success">
</p>

---

## Overview

HarshFlow takes a photo from your gallery and renders it as a real **live
wallpaper** on your home and lock screen, animated by your phone's sensors and
touch input.

It is built on Android's first-party
[`WallpaperService`](https://developer.android.com/reference/android/service/wallpaper/WallpaperService)
API — nothing is faked, overlaid, or worked around. The **same rendering engine**
drives both the in-app editor preview and the actual wallpaper, so what you
design is exactly what you get.

- **Kotlin · Jetpack Compose · Material 3** — single activity, ViewModel-driven UI.
- **No third-party media libraries** — rendering is plain Android `Canvas`, motion is `SensorManager`.
- **No permissions, no network** — photos come from the system Photo Picker, and nothing ever leaves the device.

---

## Features

| | |
|---|---|
| 🎞️ **9 real motion engines** | Each with its own physics and render code, freely combinable on one photo |
| 🧩 **11 prebuilt templates** | One-tap starting points, from cinematic to love-themed |
| 🖼️ **Crop & reposition** | Pinch-zoom (1×–4×) and drag to frame the photo; the framing carries over to the wallpaper exactly |
| 🎚️ **Intensity slider** | One control scales motion amplitude, particle count and light strength |
| ✨ **3 particle styles** | Sparkles, hearts, light orbs |
| 💾 **Wallpaper library** | Save any number of configurations, re-edit or delete them, switch the active one |
| 🌗 **Light / dark / system theme** | A dark-first palette built from the same three lights the engine renders |
| ⚡ **Live updates** | Editing while HarshFlow is already your wallpaper applies instantly — no re-picking |
| 🔋 **Battery-disciplined** | Sensors and drawing run only while the wallpaper is actually visible |

### Motion effects

| Effect | What it does |
|---|---|
| **Gyro Parallax** | Tilt the phone to shift depth layers — covers tilt, gyroscope, accelerometer motion, parallax, perspective shift |
| **Floating Motion** | Gentle idle drift and breathing scale — covers floating, elastic, inertia, smooth follow, ambient animation |
| **Touch Ripple** | A glassy ripple spreading from every tap and drag — covers touch reactive, press & hold, double tap, drag, swipe |
| **Particles** | Sparkles, hearts or light orbs drifting over the photo, reacting to touch and tilt |
| **Dynamic Light** | A soft light sweep and depth vignette that moves with tilt |
| **Shake Burst** | Shake the phone for a burst of particles |
| **Cinematic Zoom** | A slow, continuously eased zoom and drift — alive on its own, with no tilt or touch needed |
| **Aurora Glow** | Large, soft colour lights drifting over the photo and blended like real light |
| **Liquid Wave** | The photo itself warps in a slow liquid ripple (mesh warp) |

> The product brief listed ~30 interaction names (Tilt Effect, Gyroscope Control,
> Parallax Layers, Motion Follow, Depth Layers, Ripple, Press & Hold, Zoom on
> Touch…). Many of them describe the *same* underlying mechanic, so they are
> grouped into the nine genuinely distinct engines above rather than duplicated
> as fake variants. See `EffectType` in
> [`WallpaperModels.kt`](app/src/main/java/com/harsh/motion/data/WallpaperModels.kt).

### Templates

| | | |
|---|---|---|
| **Cinematic Depth** | **Dreamy Float** | **Interactive Glow** |
| **Heartbeat** ❤️ | **Us Forever** ❤️ | **Together Always** ❤️ |
| **Aurora Dream** | **Cinematic Drift** | **Liquid Glass** |
| **Neon Pulse** | **Living Portrait** | |

A template supplies the effect combination and particle style; it is applied to
whatever photo you pick.

---

## How it works

1. **Pick a photo** — through Android's Photo Picker, so no storage permission is
   requested. The photo is copied into the app's private storage immediately, so
   it stays readable for good.
2. **Frame it** — pinch to zoom, drag to position.
3. **Design the motion** — toggle any combination of effects, choose a particle
   style, set the intensity, and watch it live in the editor using your phone's
   real sensors and touch.
4. **Save & set as wallpaper** — Android shows its own one-tap confirmation, and
   the photo is alive on your home and lock screen.

Editing later uses the same flow; if HarshFlow is already the active wallpaper, a
save takes effect immediately with no picker at all.

---

## Get the APK — nothing to install on your PC

No Android Studio, SDK, or Java needed locally. **GitHub Actions builds the APK
on every push to `main`.**

1. Push this project to GitHub — the **Build APK** workflow
   ([`.github/workflows/build.yml`](.github/workflows/build.yml)) runs automatically.
2. Download `HarshFlow.apk` from **Releases → `latest`**, or from the workflow
   run's **Artifacts**.
3. Copy it to your phone, open it, allow *install unknown apps* if prompted, and
   install.

The release APK is **debug-signed on purpose** so it installs directly with no
keystore setup — it is a sideload build, not a Play Store build.

### Build locally (optional)

```bash
gradle assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Requires **JDK 17**, the Android SDK (platform 34, build-tools 34), and Gradle
8.9+ on your `PATH` — this repo intentionally ships no Gradle wrapper.

---

## Project structure

```
app/src/main/java/com/harsh/motion/
├── MainActivity.kt               # Single activity + Compose NavHost (home → editor → adjust → settings)
├── MotionApp.kt                  # Application class
├── data/
│   ├── WallpaperModels.kt        # EffectType, ParticleStyle, WallpaperConfig, Templates
│   └── WallpaperRepository.kt    # JSON-in-DataStore persistence (configs, active id, theme)
├── engine/
│   ├── EffectRenderer.kt         # Shared Canvas render + physics core (all 9 effects)
│   ├── EffectPreviewView.kt      # In-app editor preview, on the same renderer
│   ├── MotionSensor.kt           # Rotation-vector tilt + linear-acceleration shake
│   ├── BitmapLoader.kt           # Downsampled, texture-limit-safe photo decode
│   └── PhotoStore.kt             # Private photo copies + pruning of unreferenced ones
├── ui/
│   ├── screens/                  # HomeScreen, EditorScreen, PhotoAdjustScreen, SettingsScreen
│   └── theme/                    # Brand.kt (brand surfaces), Theme.kt (palette, type, shapes)
├── viewmodel/
│   └── MotionViewModel.kt        # Editor state, save/activate, photo lifecycle
└── wallpaper/
    └── MotionWallpaperService.kt # The real live wallpaper engine
```

### Architecture at a glance

```
Compose UI (Home · Editor · Adjust · Settings)
        │
MotionViewModel  ──────────────  WallpaperRepository (DataStore)
        │
WallpaperConfig  (photo + effects + style + intensity + framing)
        │
EffectRenderer   ← one shared render/physics core →
        │                                     │
EffectPreviewView                  MotionWallpaperService.Engine
(editor preview)                       (live wallpaper)
        │                                     │
      Android Canvas · SensorManager · WallpaperManager
```

Full detail in **[ARCHITECTURE.md](ARCHITECTURE.md)**.

---

## Performance & battery

A live wallpaper runs whenever the home or lock screen is shown, so battery
discipline is a hard requirement rather than a nicety:

- **Nothing runs while invisible** — `onVisibilityChanged(false)` (screen off,
  another app in front) tears down both the sensor listener and the draw loop
  immediately. The editor preview follows the same rule.
- **Vsync-aligned, capped frame rate** — a `Choreographer` loop draws on every
  Nth vsync, with N derived from the panel's own refresh rate (60→60, 90→90,
  120→60, 144→72 fps). All motion advances by elapsed `dt`, so the cap changes
  how often motion is drawn, never its speed.
- **Hardware canvas** — the wallpaper surface uses `lockHardwareCanvas()`, with a
  permanent software fallback for OEM builds that refuse it, putting the
  wallpaper on the same GPU path as the editor preview.
- **Only the sensors in use are powered** — tilt and shake are registered
  independently, so a config that reads neither powers no sensor at all; tilt
  samples at 30 Hz, matching the renderer's own follow rate.
- **Allocation-light rendering** — `Paint`/`Shader`/`Path` objects are created
  once and reused, shader movement uses `setLocalMatrix`, and particles are
  capped (14–42 ambient, 70 absolute).
- **Right-sized bitmaps** — photos are decoded downsampled toward screen
  resolution and clamped to 4096 px, so they can never exceed the GPU texture
  limit.

Mechanisms and measurement targets in **[PERFORMANCE.md](PERFORMANCE.md)**.

---

## Privacy

- **No network code at all** — no analytics, ads, accounts, or trackers.
- **No runtime permissions** — photo selection goes through the system Photo
  Picker, so the app only ever sees the one file you pick.
- The picked photo is copied into the app's **private internal storage**, and
  abandoned copies are pruned automatically. Only that path plus your effect
  settings are stored, as small JSON in the app's own DataStore.
- `allowBackup="false"` keeps even that off cloud backups.
- The only declared Android capability is the `android.software.live_wallpaper`
  feature, plus the `BIND_WALLPAPER` system permission every live wallpaper
  service must hold.

More in **[SECURITY.md](SECURITY.md)**.

---

## Compatibility

- **Android 8.0 (API 26) through the latest release** — one code path, no
  version-gated forks; the Photo Picker, live wallpaper API and rotation-vector
  sensor are all stable across that range.
- **No gyroscope? Still works.** Gyro Parallax, Dynamic Light and Aurora Glow
  simply have no tilt input to react to; Floating Motion, Touch Ripple,
  Particles, Cinematic Zoom, Liquid Wave and Shake Burst are unaffected.
- `minSdk` 26 · `compileSdk`/`targetSdk` 34 · JVM target 17.

---

## Documentation

| Document | Contents |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Layering, effect model, data flow, rendering and sensor design |
| [PERFORMANCE.md](PERFORMANCE.md) | Battery and frame-rate mechanisms, measurement targets |
| [SECURITY.md](SECURITY.md) | Data handling, permissions, privacy guarantees |

---

## License

Built for **Harsh** (harshp@prowesolution.com). All rights reserved.
