# Motion by Harsh

Turn any photo into a living wallpaper — with gyroscope parallax, touch
ripples, drifting particles, dynamic light, and floating motion. No root, no
account, fully on-device.

Built with **Kotlin + Jetpack Compose + Material 3** on Android's official
**Live Wallpaper** API.

---

## How it works

1. Pick a photo from your gallery (no storage permission needed — uses
   Android's Photo Picker).
2. Turn on any combination of motion effects and preview them live, right in
   the editor, using your phone's actual sensors and touch input.
3. Tap **Save & set as wallpaper** — Android shows its own one-tap "Set
   wallpaper" confirmation, then your photo is alive on your home/lock screen.

### Effects
| Effect | What it does |
|---|---|
| **Gyro Parallax** | Tilt your phone to shift depth — covers tilt, gyroscope, accelerometer motion, parallax layers, perspective shift |
| **Floating Motion** | A gentle idle drift/breathing motion — covers floating, elastic, inertia, smooth follow, ambient animation |
| **Touch Ripple** | Tap and drag ripple across the photo — covers touch reactive, press & hold, double tap, drag, swipe, zoom on touch |
| **Particles** | Sparkles, hearts, or light orbs drifting and reacting to touch/tilt |
| **Dynamic Light** | A soft light sweep and depth vignette that shifts with tilt |
| **Shake Burst** | Shake your phone for a burst of particles |

Multiple effects can be combined on one photo. Six **prebuilt templates** are
included — three general-purpose (Cinematic Depth, Dreamy Float, Interactive
Glow) and three **love-themed** (Heartbeat, Us Forever, Together Always) with
heart particles.

---

## Get the APK without installing anything

No Android Studio, SDK, or Java needed on your PC — GitHub builds the APK:

1. Push this project to GitHub. The **Build APK** Action runs automatically.
2. Download the finished APK from the repo's **Releases → latest →
   `MotionByHarsh.apk`**, or from the Actions run's **Artifacts**.
3. Copy it to your phone, open it, allow "install unknown apps" if prompted,
   install.

## Build locally (optional)
```
gradle assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```
Requires JDK 17 and the Android SDK (platform 34, build-tools 34).

---

## Compatibility

- **Android 8.0 (API 26) and up** — same code path on every version through
  the latest release; no version-specific branches needed for the core
  features (Photo Picker, live wallpaper API, and the rotation-vector sensor
  are all stable across this range).
- Devices without a gyroscope still work — Gyro Parallax simply stays still;
  every other effect (Floating, Touch, Particles, Dynamic Light) is
  independent of that sensor.

## Performance & battery
See [PERFORMANCE.md](PERFORMANCE.md) — sensors and rendering run only while the
wallpaper is actually visible, frame rate is capped, and photos are decoded at
screen resolution rather than full camera resolution.

## License
Built for **Harsh** (harshp@prowesolution.com).
