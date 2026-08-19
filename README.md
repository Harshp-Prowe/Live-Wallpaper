# Dual by Harsh

A lightweight, no-root **second-space / app-cloning** Android app. Create an
isolated private space on your phone, clone supported apps into it, and run them
with **separate accounts and data** — without touching your original apps.

Built with **Kotlin + Jetpack Compose + Material 3**.

---

## How it works (honest version)

On modern Android (10+), the old "run a clone inside a container" trick used by
apps like Parallel Space / VirtualApp **no longer works without rooting the
phone**. This app refuses to fake cloning, so it uses the one mechanism that is
real, supported, and needs no root:

> **Android's managed Work Profile** — the same approach as the open-source apps
> *Island* and *Shelter*.

- Your clones live in a sealed, OS-managed space next to your normal phone.
- Cloned apps appear in your app drawer with a small **briefcase badge**.
- Your personal apps, settings, photos and accounts are **never modified**.
- Removing the space deletes the clones cleanly — nothing else changes.

### What you get
```
Install DualByHarsh.apk
        ↓
Open the app → tap "Create Space"  (one Android confirmation, no manual settings)
        ↓
Tap "Add App" → pick an installed app → it's cloned into the space
        ↓
Tap the clone to launch it → log in with a different account
        ↓
Original app + original account stay untouched
```

---

## Get the APK without installing anything

You do **not** need Android Studio, the SDK, or Java on your PC. GitHub builds
the APK for you:

1. Push this project to GitHub (see below). The **Build APK** GitHub Action runs
   automatically.
2. When it finishes (green check under the **Actions** tab), download the APK:
   - From the **Releases** page → `latest` → **DualByHarsh.apk** (direct download), or
   - From the Actions run → **Artifacts** → `DualByHarsh-apk`.
3. Copy `DualByHarsh.apk` to your phone and open it. Allow "install unknown
   apps" when prompted, then install.

---

## Build locally (optional, only if you *want* the SDK)

```
gradle assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Requires JDK 17 and the Android SDK (platform 34, build-tools 34). The APK is
debug-signed so it installs directly.

---

## Supported Android versions & devices

- **Android 8.0 (API 26) and newer.**
- The device must allow a managed profile. Most standard phones do; a few
  manufacturers disable it, in which case the app tells you clearly.

## Known limitations

- Some apps (banking, UPI, DRM/streaming, hardware-key authenticators) detect
  the work profile and refuse to run. This is by design and **cannot** be
  bypassed safely — the app reports it instead of pretending.
- Google Play Services features (push, Sign-In) may be limited inside the space.
- Only one private space is supported (Android allows one managed profile).

## What this app never does

No root, no ADB, no system modification, no credential capture, no TLS/DRM/Play
Integrity bypass. See [SECURITY.md](SECURITY.md).

---

## Documentation
- [ARCHITECTURE.md](ARCHITECTURE.md) — engine choice and design
- [COMPATIBILITY.md](COMPATIBILITY.md) — per-app compatibility matrix
- [SECURITY.md](SECURITY.md) — security posture
- [PERFORMANCE.md](PERFORMANCE.md) — performance notes

## License
See project owner. Built for **Harsh** (harshp@prowesolution.com).
