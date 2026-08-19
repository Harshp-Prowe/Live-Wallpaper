# Performance

## Design choices
- **UI:** Jetpack Compose with `LazyVerticalGrid` (clones) and `LazyColumn`
  (app list) so only visible items are composed.
- **Icons:** loaded on demand from `PackageManager` and drawn via
  `Drawable.toBitmap()`; no full-resolution preloading of every app icon.
- **Threading:** all package inspection, discovery, and clone/remove operations
  run on `Dispatchers.IO` through `viewModelScope`. The Compose UI thread is
  never blocked.
- **No polling loops:** state is read on demand and on `RESUME`; there is no
  `while(true)` scanning. Battery impact is negligible when idle.
- **Cold start:** `DualApp` does no heavy work; first frame shows a themed
  surface immediately, then navigates.

## Real progress, not fake
The clone flow reflects **actual** work: the engine issues the cross-profile
install and then polls `LauncherApps` until the package really appears in the
work profile (or a timeout is hit and an honest failure is reported). There are
no fabricated percentage bars.

## To measure on device (per plan §49)
Fill in with real numbers from Android Studio Profiler / Macrobenchmark:

| Metric | Target | Measured |
|---|---|---|
| Cold start | < 500 ms |  |
| Clone create (typical app) | reported as it happens |  |
| Frame rendering | 60 fps min (90/120 where available) |  |
| APK size (release) | lightweight |  |
| Idle battery | negligible |  |

## APK size
Reported by the GitHub Actions build log after `assembleRelease`.
