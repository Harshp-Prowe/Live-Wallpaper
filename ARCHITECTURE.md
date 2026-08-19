# Architecture

## 1. Selected virtualization engine: Android Managed Work Profile

After evaluating the candidates from the plan, the chosen engine is Android's
**managed profile** (Work Profile), driven via `DevicePolicyManager`.

| Candidate | Verdict |
|---|---|
| **VirtualApp** | Rejected. Unmaintained; broken on Android 10+ without root; relies on hidden APIs blocked by non-SDK restrictions and stricter SELinux. |
| **BlackBox** | Rejected. Same class of non-root container; modern builds are commercial/obfuscated and still fragile on Android 12+. Shipping it would mean unstable/fake cloning. |
| **Android Work Profile** | **Selected.** First-party, no root, no ADB, real data isolation, actively supported by the OS. Same mechanism as the open-source apps Island and Shelter. |

### Why
The plan (§54) demands honesty over pretend-cloning. Non-root in-app
virtualization cannot deliver a real, stable clone on current Android. The
managed profile is the only mechanism that is **real, no-root, and maintained**.

### License
Uses only the Android SDK (`android.app.admin.*`, `android.content.pm.LauncherApps`).
No third-party engine is bundled, so no external engine license applies.

## 2. Android compatibility
- **Min SDK 26** (Android 8.0). The cross-profile install path
  (`installExistingPackage`, cross-profile intent forwarding, `LauncherApps`
  profile enumeration) is reliable from 26 up.
- Target SDK 34. ARM64 / ARMv7 / x86 are all supported — the app carries no
  native code of its own; clones keep their own native libraries.

## 3. Architecture layers
```
Jetpack Compose UI  (ui/screens, ui/components, ui/theme)
        │
ViewModel / State   (viewmodel/DualViewModel)
        │
Domain / Data       (data/*: AppDiscovery, CloneRepository, Models)
        │
VirtualizationEngine interface  (engine/VirtualizationEngine)
        │
WorkProfileEngine  ── forwarded cross-profile intent ──▶  AgentActivity
(personal profile)                                        (work profile,
                                                           profile owner)
        │
Android Framework (DevicePolicyManager, LauncherApps, PackageInstaller)
```

## 4. Package management strategy
- Discovery: `PackageManager.queryIntentActivities(MAIN/LAUNCHER)` in the
  personal profile, honoring Android 11+ package-visibility via a `<queries>`
  entry.
- Cloning: the profile owner calls `DevicePolicyManager.installExistingPackage`,
  which re-installs an app **already present on the device** into the work
  profile. No APK copying, no repackaging, no package renaming.

## 5. Process / cross-profile model
The personal UI cannot call profile-owner APIs directly. It fires a **forwarded
cross-profile intent** (`com.harsh.dual.action.AGENT`), enabled in
`onProfileProvisioningComplete` via
`addCrossProfileIntentFilter(FLAG_PARENT_CAN_ACCESS_MANAGED)`. Android routes it
to `AgentActivity` running inside the work profile, which performs the
privileged operation and finishes. Results are confirmed by re-scanning the work
profile through `LauncherApps` (forwarded intents do not return results).

> Note: the cross-profile install/remove path is the piece most dependent on
> real-device behavior and OEM policy. It is implemented against documented APIs;
> device testing may surface OEM-specific quirks (documented in COMPATIBILITY.md).

## 6. Storage isolation
Handled entirely by the OS: the work profile has its own user ID and fully
separate app-data sandbox. This app stores only clone **metadata** (labels,
timestamps) in its own DataStore — never cloned app data.

## 7. Permission model
Cloned apps request their own runtime permissions inside the profile through the
normal Android dialogs. This app never auto-grants dangerous permissions.

## 8. Known limitations
See [COMPATIBILITY.md](COMPATIBILITY.md) and README. Hardware-key / Play
Integrity / DRM apps are intentionally not bypassed.
