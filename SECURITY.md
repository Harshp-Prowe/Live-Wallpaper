# Security & Privacy

## Data handling
- The app is **fully offline** — no network calls, no analytics, no account.
- Photos are referenced by their picked `content://` URI; the app never copies
  the original file elsewhere, and only stores that URI plus the chosen effect
  settings (as small JSON) in its own private DataStore.
- `allowBackup="false"` keeps that metadata off cloud backups.

## Permissions
- The app requests **no storage/media permission**. Photo selection uses
  Android's Photo Picker, which grants read access to just the one file the
  user picks — the app never sees the rest of the gallery.
- The only Android-declared capability is `android.software.live_wallpaper`
  (a feature declaration, not a runtime permission) plus the standard
  `BIND_WALLPAPER` system permission required of every live wallpaper service.

## What this app never does
No root, no device-admin, no background data collection, no ads SDK, no
third-party trackers.
