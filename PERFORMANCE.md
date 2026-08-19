# Performance & Battery

Battery discipline was a hard requirement for this app, since a live wallpaper
runs constantly whenever the home/lock screen is shown. Concrete mechanisms:

## Sensors and rendering only run while visible
- `MotionWallpaperService.Engine.onVisibilityChanged(visible)` starts the
  sensor listener and the draw loop when `visible == true`, and stops both
  immediately when `false` (screen off, app open in foreground, wallpaper
  scrolled away). No background CPU/sensor use when the wallpaper isn't shown.
- The in-app editor preview (`EffectPreviewView`) follows the same rule via
  `onAttachedToWindow` / `onDetachedFromWindow`.

## Capped, not maximal, frame rate
- The draw loop runs at **~30 fps** (33 ms interval), not 60+. This halves CPU
  and GPU work versus a naive `Choreographer`-driven loop while remaining
  visually smooth for ambient wallpaper motion.
- The rotation sensor is registered at `SENSOR_DELAY_GAME`, not
  `SENSOR_DELAY_FASTEST` — smooth tilt response without maximum sensor draw.

## No unnecessary allocation
- `EffectRenderer` creates its `Paint`/`Shader`/`Path` objects once and reuses
  them every frame. Shader movement (the dynamic-light sweep) uses
  `Shader.setLocalMatrix` instead of constructing a new shader per frame.
- Particle counts are capped (≤24 ambient, ≤40 during a burst) regardless of
  how long the wallpaper runs.

## Right-sized bitmaps
- `BitmapLoader` decodes every photo downsampled to the screen's resolution
  (`BitmapFactory.Options.inSampleSize`), never at the camera's full
  resolution. A 12 MP photo does not cost 12 MP of memory or per-frame drawing
  work — it costs exactly what the screen can show.

## Lightweight APK
- No third-party image/animation libraries — Canvas + SensorManager are part
  of the Android framework already.
- `lint { checkReleaseBuilds = false }` avoids CI-only fatal warnings; this
  does not affect runtime performance.

## To measure on device
| Metric | Target | Measured |
|---|---|---|
| Idle battery draw (wallpaper visible) | Comparable to a static wallpaper + light animation | |
| Cold app start | < 500 ms | |
| APK size (release) | Lightweight (no third-party media libs) | |
