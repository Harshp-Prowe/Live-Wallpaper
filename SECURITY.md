# Security

## Principles
Dual by Harsh does **not**, and will never:
- Root the device, use Magisk, or require a custom ROM
- Use ADB or modify the system partition
- Steal or capture credentials, passwords, or session tokens
- Disable TLS verification or intercept network traffic
- Bypass DRM, Play Integrity, anti-cheat, or hardware-key authentication
- Access private data of unrelated apps

The purpose is **legitimate app isolation and multi-account usage**, using
first-party Android APIs.

## Data handling
- The app stores only clone **metadata** (package name, label, timestamp) in its
  own private DataStore. It never stores cloned apps' private data — that lives
  in the OS-managed work profile sandbox.
- `allowBackup="false"` to keep metadata off cloud backups.
- No analytics, no network calls, no account/server. Fully offline.

## Privilege model
- The app becomes **profile owner of a managed profile only** — never device
  owner, never device admin over your personal profile.
- The single admin policy declared is `wipe-data`, used solely to remove the
  private space on request (and its clones). It cannot wipe your personal
  profile.

## Removal / reversibility
"Remove private space" (or removing the work profile from Android Settings)
deletes the space and all clones. The personal profile is unaffected.

## Provisioning consent
Creating the space always goes through Android's built-in confirmation screen.
By OS design, no app can create a work profile silently; this app does not and
cannot bypass that consent.
