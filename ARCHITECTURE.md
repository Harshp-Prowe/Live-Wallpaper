# Architecture

## Overview
HarshFlow is built on Android's official **Live Wallpaper** framework
(`android.service.wallpaper.WallpaperService`) — a first-party, fully-supported
mechanism with no OS-level restrictions to work around (unlike app-cloning,
Live Wallpapers are a standard user-facing feature on every Android device).

```
Jetpack Compose UI  (Home, Editor, Settings)
        │
ViewModel / State   (MotionViewModel)
        │
Domain / Data       (WallpaperConfig, WallpaperRepository, Templates)
        │
Shared render/physics engine   (EffectRenderer)
        │              │
EffectPreviewView   MotionWallpaperService.Engine
(in-app editor)     (actual live wallpaper)
        │              │
Android Canvas / SensorManager / WallpaperManager
```

The **same `EffectRenderer`** draws both the in-app editor preview and the real
wallpaper, so what the user designs is exactly what they get — no separate,
divergent "preview mode."

## Effect model
The product brief lists ~30 interaction names (Tilt Effect, Gyroscope Control,
Parallax Layers, Motion Follow, Perspective Shift, Depth Layers, Motion
Tracking, Touch Reactive, Ripple, Press & Hold, Double Tap, Drag, Swipe, Zoom on
Touch...). Many of these describe the *same underlying mechanic* viewed from
different angles. Rather than faking distinct implementations for each name,
they are grouped into six real, independently-toggleable engines in
`EffectType`: Gyro Parallax, Floating Motion, Touch Ripple, Particles, Dynamic
Light, Shake Burst. Each has genuinely different rendering/physics code — see
`WallpaperModels.kt` for the mapping.

## Data flow
- `WallpaperConfig` (photo URI + chosen effects + particle style + intensity)
  is the single source of truth, persisted as JSON in DataStore
  (`WallpaperRepository`).
- Applying a wallpaper marks a config "active"; `MotionWallpaperService` reads
  the active config at engine creation and on each surface change.
- The photo itself is referenced by its picked `content://` URI — the system
  Photo Picker grants persistent read access automatically, so no copy of the
  original file is made and no storage permission is requested.

## Rendering & sensors
- `EffectRenderer`: allocation-light Canvas drawing + physics (particles,
  ripples, parallax offset, light sweep). Paint/Shader objects are created once
  and reused; shader movement uses `setLocalMatrix` instead of recreating
  shaders every frame.
- `MotionSensor`: wraps the single hardware-fused rotation-vector sensor
  (cheaper than combining accelerometer + magnetometer manually) at
  `SENSOR_DELAY_GAME`, plus an optional linear-accelerometer listener for shake
  detection.
- `BitmapLoader`: decodes photos downsampled to screen resolution
  (`inSampleSize`), never at full camera resolution.

## Battery & lifecycle discipline
See [PERFORMANCE.md](PERFORMANCE.md) for the specific mechanisms — the summary
is: sensors and the draw loop run *only* while the wallpaper (or editor
preview) is actually visible, and stop immediately otherwise.

## Compatibility
- Min SDK 26 (Android 8.0) through the latest release, one code path — no
  version-gated feature forks were needed for Photo Picker, the live wallpaper
  API, or the rotation-vector sensor.
- Devices lacking a gyroscope degrade gracefully: Gyro Parallax has no input to
  react to (photo stays still under that effect), while Floating, Touch,
  Particles, Dynamic Light and Shake are unaffected.
