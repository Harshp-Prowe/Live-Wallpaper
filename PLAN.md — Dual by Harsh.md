# Dual by Harsh
## Android Virtual Second Space / App Cloning Platform

Build a production-quality Android application named:

**Dual by Harsh**

The application is an Android-only virtual second-space/container application similar in concept to Secure Folder, Dual Apps, Parallel Space, and other legitimate Android app-isolation solutions.

The goal is to allow a user to install **one APK** called `Dual by Harsh`, select applications already installed on the Android device, create isolated virtual instances of those applications, and launch/use those instances independently from the original applications.

---

# 1. CRITICAL REQUIREMENT

This is NOT an APK renaming application.

Do NOT implement fake cloning by:

- Renaming an APK
- Changing package names and reinstalling it
- Creating shortcuts to the original app
- Opening the original application
- Modifying the original application
- Installing a second copy through the normal Android PackageManager

The cloned application must actually execute inside a **virtualized/containerized Android environment** managed by Dual by Harsh.

The original application must remain completely independent.

Example:

```text
Android Main Space

WhatsApp
└── Original account/data


Dual by Harsh

Virtual Space
└── WhatsApp Clone
    └── Independent account/data
```

If the virtualization architecture cannot support a particular application, clearly report that limitation instead of pretending it works.

---

# 2. PRIMARY TECHNOLOGY

Use:

**Kotlin + Jetpack Compose**

Do NOT use Flutter or React Native.

This is an Android-first application and the virtualization layer is inherently Android-specific.

Recommended architecture:

```text
┌───────────────────────────────────────┐
│           Jetpack Compose UI           │
├───────────────────────────────────────┤
│       ViewModel / State / Events       │
├───────────────────────────────────────┤
│          Domain / App Manager          │
├───────────────────────────────────────┤
│       Native Android Integration       │
├───────────────────────────────────────┤
│      Virtualization / Container       │
│              Engine                   │
├───────────────────────────────────────┤
│          Android Framework            │
└───────────────────────────────────────┘
```

Keep UI and virtualization engine completely separated.

---

# 3. ANDROID VERSION

Minimum supported Android:

**Android 6.0 / API 23**

Target modern Android versions while maintaining compatibility with Android 6+ where technically possible.

Support:

- API 23+
- ARM64
- ARMv7 where practical
- x86/x86_64 where practical

Test progressively against modern Android releases.

Pay particular attention to:

- Android 6 runtime permissions
- Android 7 behavior changes
- Android 8 background execution
- Android 10 storage
- Android 11 package visibility
- Android 12 exported components
- Android 13 notification permissions
- Android 14+ restrictions
- Latest Android compatibility changes

---

# 4. NO ROOT

The application must NOT require:

- Root
- Magisk
- Custom ROM
- Bootloader unlocking
- ADB
- System modification

The intended installation process is:

```text
Receive DualByHarsh.apk
        ↓
Install APK
        ↓
Open Dual by Harsh
        ↓
Create virtual space
        ↓
Clone applications
        ↓
Use cloned applications
```

The APK should be distributable locally.

No server/account should be required.

---

# 5. VIRTUALIZATION ENGINE

Before implementing the application UI, investigate available Android virtualization technologies.

Evaluate projects such as:

- VirtualApp
- BlackBox
- other actively maintained Android virtualization/container engines
- Android profile/work-profile mechanisms where useful
- other legitimate open-source approaches

Do not blindly use an old GitHub repository.

For each candidate evaluate:

- License
- Maintenance status
- Android 6 compatibility
- Current Android compatibility
- ARM64 support
- ARMv7 support
- Activity virtualization
- Service virtualization
- Broadcast virtualization
- Content provider virtualization
- Package manager virtualization
- File isolation
- Permission handling
- Native `.so` support
- Split APK support
- WebView
- Google services
- Notifications
- Background execution
- Performance
- Security implications

Prefer an actively maintainable and legally reusable solution.

Check the license before copying, modifying, or redistributing any source code.

If no existing engine is suitable, implement the required virtualization components independently or create a compatible abstraction that allows the engine to be replaced later.

---

# 6. ARCHITECTURE DOCUMENTATION FIRST

Before major implementation, create:

```text
ARCHITECTURE.md
```

It must document:

1. Selected virtualization engine
2. Why it was selected
3. License
4. Android compatibility
5. Architecture
6. Package management strategy
7. Process management
8. Storage isolation
9. Permission model
10. Intent routing
11. Service handling
12. Notification handling
13. Known limitations
14. Unsupported application categories
15. Performance risks
16. Security risks

Do this BEFORE implementing the complete UI.

---

# 7. PROJECT STRUCTURE

Use a clean modular architecture.

Suggested:

```text
DualByHarsh/
│
├── app/
│   ├── src/main/java/com/harsh/dual/
│   │
│   ├── ui/
│   │   ├── theme/
│   │   ├── navigation/
│   │   ├── components/
│   │   ├── screens/
│   │   └── animations/
│   │
│   ├── launcher/
│   │
│   ├── clone/
│   │
│   ├── package/
│   │
│   ├── compatibility/
│   │
│   ├── permissions/
│   │
│   ├── notifications/
│   │
│   ├── storage/
│   │
│   ├── virtual/
│   │
│   └── settings/
│
├── virtualization/
│
├── native/
│
├── tests/
│
├── docs/
│
├── ARCHITECTURE.md
├── COMPATIBILITY.md
├── SECURITY.md
├── PERFORMANCE.md
└── README.md
```

Use package/module names that remain maintainable as the project grows.

---

# 8. UI TECHNOLOGY

Use:

**Jetpack Compose**

Use modern Android UI architecture:

- Compose
- Material 3
- ViewModel
- Kotlin Coroutines
- StateFlow
- Navigation Compose
- Dependency injection only if useful
- Room/DataStore where appropriate

Do not add unnecessary frameworks.

The application should remain lightweight.

---

# 9. VISUAL DESIGN

Dual by Harsh should look like a polished Android system utility.

Design inspiration can include the concepts of:

- Android launcher
- Secure Folder
- Dual Apps
- modern Android system applications

But DO NOT copy another application's exact UI.

Create an original design language.

Visual characteristics:

- Clean
- Premium
- Minimal
- Smooth
- Modern
- Spacious
- Fast
- Lightweight

Use:

- Rounded surfaces
- Adaptive app grid
- Modern typography
- Subtle elevation
- Minimal shadows
- Proper dark mode
- High-quality app icons
- Smooth transitions

---

# 10. MAIN SCREEN

Main screen:

```text
Dual by Harsh

Your private second space

┌──────────┐ ┌──────────┐
│ WhatsApp │ │Instagram │
│    ◉     │ │    ◉     │
└──────────┘ └──────────┘

┌──────────┐ ┌──────────┐
│ Telegram │ │  Chrome  │
│    ◉     │ │    ◉     │
└──────────┘ └──────────┘

                    ＋
```

The exact UI should be designed professionally.

Top area:

- Application title
- Virtual space status
- Settings button

Main area:

- Clone grid
- Empty state when no apps exist

Floating action button:

**+ Add App**

---

# 11. ONBOARDING

First launch:

### Screen 1

**Dual by Harsh**

**Your private second space**

Explain:

> Run separate instances of supported Android applications without affecting their original data.

Button:

**Continue**

Keep onboarding short.

Do not show unnecessary tutorials.

---

# 12. ADD APP

User presses:

**+ Add App**

Show installed/launchable applications.

Each item:

```text
[Icon] WhatsApp
       com.whatsapp

                         Add
```

Provide:

- Search
- Sort
- Filter
- Select
- Multi-select

Do not scan the entire device repeatedly.

Use Android package APIs correctly.

Handle package visibility restrictions on Android 11+.

---

# 13. CLONE PROCESS

When the user selects an app:

```text
Inspecting application
        ↓
Preparing package
        ↓
Preparing virtual environment
        ↓
Installing clone
        ↓
Preparing isolated data
        ↓
Finalizing
```

The progress must represent actual work.

Do NOT use fake percentage progress.

Run heavy operations off the main thread.

The UI must remain responsive.

---

# 14. VIRTUAL PACKAGE MANAGER

Implement a virtual package manager abstraction.

Example model:

```kotlin
data class VirtualPackage(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String,
    val apkPath: String,
    val iconPath: String?,
    val virtualUserId: Int,
    val installedAt: Long,
    val enabled: Boolean
)
```

Responsibilities:

- Install clone
- Remove clone
- Enable/disable
- Launch clone
- Update clone
- Clear clone data
- Clear clone cache
- Query clone information

Never modify the original package installation.

---

# 15. VIRTUAL USERS

Support a virtual user/profile concept.

Example:

```kotlin
data class VirtualUser(
    val id: Int,
    val name: String,
    val createdAt: Long,
    val enabled: Boolean
)
```

Default:

```text
User 0
```

Future-ready architecture:

```text
Personal
Work
Second Account
```

Each virtual user should have independent application data.

---

# 16. MULTIPLE CLONES

If supported by the virtualization engine, allow:

```text
WhatsApp
├── Personal
├── Work
└── Second
```

Each clone must have separate data.

Removing one clone must not affect another.

---

# 17. DATA ISOLATION

This is one of the most important requirements.

The cloned app must have isolated:

- SharedPreferences
- SQLite
- Internal files
- Cache
- WebView storage
- Cookies
- Local databases
- Account/session information
- Application-specific storage

Example:

```text
Original:

/original/app/data


Clone:

/DualByHarsh/virtual/user0/com.example.app/
```

The exact implementation depends on the selected virtualization engine.

---

# 18. ORIGINAL APP MUST REMAIN UNTOUCHED

This must always be true:

```text
Clone created
       ↓
Original application unchanged
```

Removing:

```text
Clone
```

must NOT:

```text
Uninstall original
Delete original data
Clear original cache
Modify original settings
```

---

# 19. APP LAUNCH

When the user taps a cloned application:

Launch it inside the virtual environment.

The application should behave as normally as technically possible.

Virtualize:

- Activity
- Application lifecycle
- Context
- PackageManager
- Intents
- Services
- Broadcast receivers
- Content providers
- Resources
- Storage
- Permissions

Do not launch the original application accidentally.

---

# 20. INTENT ROUTING

Implement virtual intent routing.

Handle:

- ACTION_VIEW
- ACTION_SEND
- ACTION_SEND_MULTIPLE
- ACTION_PICK
- ACTION_GET_CONTENT
- ACTION_OPEN_DOCUMENT
- ACTION_IMAGE_CAPTURE
- Custom application intents

Where appropriate, route the intent into the virtual environment.

Do not break the user's normal Android intent handling outside Dual by Harsh.

---

# 21. PERMISSIONS

Handle runtime permissions for cloned applications.

Examples:

- Camera
- Microphone
- Location
- Contacts
- Files
- Notifications

Do not automatically grant dangerous permissions.

Do not bypass Android permission controls.

Use the safest supported mapping between Android permissions and the virtual application.

---

# 22. NOTIFICATIONS

Support notifications from cloned applications where technically possible.

Example:

```text
WhatsApp — Dual by Harsh
New message
```

Do not mix clone and original notifications incorrectly.

If Android restrictions prevent full virtualization for a particular application, document the limitation.

---

# 23. FILES AND SHARING

Support where technically possible:

- Gallery
- Camera
- File picker
- Share
- Open with
- Downloads

A cloned application should be able to access user-selected files through normal Android mechanisms.

Do not give unnecessary access to private files.

---

# 24. NETWORK

Cloned applications should have normal network access.

Support where possible:

- HTTP
- HTTPS
- DNS
- TCP
- UDP
- WebView networking
- IPv4
- IPv6

Do not disable TLS verification.

Do not intercept passwords or credentials.

---

# 25. GOOGLE SERVICES

Investigate support for:

- Google Play Services
- Firebase
- FCM
- Google Sign-In
- Maps

Some applications may fail because they require:

- Hardware-backed keystore
- Play Integrity
- DRM
- Device-bound authentication
- Special system services

Do NOT bypass these protections.

Instead provide compatibility information.

---

# 26. COMPATIBILITY ENGINE

For each application inspect:

- Minimum SDK
- Target SDK
- Version
- APK architecture
- Native libraries
- Split APKs
- Required permissions
- Services
- Providers
- Receivers
- Google dependencies

Display:

### High Compatibility

Expected to work.

### Partial Compatibility

Some features may not work.

### Unsupported

Cannot safely virtualize.

Show a clear reason.

---

# 27. SPLIT APK SUPPORT

Modern Android applications may contain:

```text
base.apk
config.arm64_v8a.apk
config.xxhdpi.apk
config.en.apk
```

The architecture must investigate and support split APKs where technically possible.

Do not assume every application is a single APK.

---

# 28. NATIVE LIBRARY SUPPORT

Handle native:

```text
.so
```

libraries where supported.

Support:

- ARM64
- ARMv7

Do not silently fail when an architecture is incompatible.

---

# 29. APP UPDATE

Detect when an original application has a newer version.

Show:

**Update Clone**

The update must preserve clone data where technically possible.

Do not delete the cloned database during update.

Handle split APK changes correctly.

---

# 30. CLONE MANAGEMENT

Long press an application.

Show:

```text
WhatsApp

Launch
App Info
Clear Cache
Clear Data
Update Clone
Create Shortcut
Remove Clone
```

Use a polished Compose bottom sheet.

---

# 31. BACKUP / RESTORE

Implement a future-ready architecture for:

**Backup Virtual Space**

**Restore Virtual Space**

Back up metadata and application data only where technically and legally appropriate.

Do not attempt to bypass application security mechanisms.

---

# 32. APP LOCK

Optional security feature:

- PIN
- Biometric unlock
- Hide virtual space

Use Android's official biometric/security APIs.

Do not implement custom insecure cryptography.

---

# 33. ANIMATIONS

Animations are a major part of the product.

The application must feel extremely smooth.

Target:

**60 FPS minimum**

and support:

**90/120 Hz displays** where available.

Use Compose animations such as:

- AnimatedVisibility
- AnimatedContent
- animate*AsState
- updateTransition
- SharedTransition where appropriate
- Compose Navigation transitions

Do not over-animate.

---

# 34. STARTUP ANIMATION

Startup should be approximately:

**300–500 ms**

Example:

```text
Logo
 ↓
Fade + scale
 ↓
Dashboard
```

Do not delay the actual launcher unnecessarily.

---

# 35. APP ICON ANIMATION

On app launch:

```text
Tap
 ↓
Small scale
 ↓
Expand
 ↓
Launch virtual app
```

Target approximately:

**100–250 ms**

The animation must never block actual app startup.

---

# 36. MICRO INTERACTIONS

Add subtle feedback for:

- Button presses
- App selection
- Long press
- Clone success
- Remove clone
- Settings changes

Use haptic feedback carefully.

Do not overuse vibration.

---

# 37. DARK MODE

Support:

- System
- Light
- Dark

Dark mode must be designed intentionally.

Do not simply invert colors.

---

# 38. LOW-END DEVICE MODE

The application must work reasonably on older/low-end Android devices.

Automatically reduce expensive visual effects if necessary.

Reduce:

- Blur
- Shadows
- Complex transitions
- Large image rendering

Keep basic:

- Fade
- Scale
- Slide

The application should remain responsive.

---

# 39. LIGHTWEIGHT REQUIREMENT

Keep the application lightweight.

Avoid unnecessary libraries.

Enable safe:

- R8
- Resource shrinking
- ProGuard optimizations where appropriate

Remove:

- Debug code
- Unused resources
- Unused dependencies
- Unused native binaries

Do not sacrifice virtualization compatibility solely to reduce APK size.

Provide release APK size in the final build report.

---

# 40. MEMORY OPTIMIZATION

The launcher must handle:

**100+ applications**

without loading every icon at full resolution.

Use:

- LazyVerticalGrid
- Memory-aware image caching
- Disk cache
- Bitmap resizing
- Lazy loading

Do not hold unnecessary references to Activities or Contexts.

---

# 41. STARTUP PERFORMANCE

Do not wait for all application scanning before showing the UI.

Use:

```text
Application starts
       ↓
Show cached clones immediately
       ↓
Load current state
       ↓
Refresh asynchronously
```

The launcher should feel almost instant after the first run.

---

# 42. BACKGROUND PROCESSING

Move expensive work off the main thread.

Use Kotlin:

- Coroutines
- Dispatchers.IO
- Dispatchers.Default
- Worker where appropriate

Operations such as:

- APK inspection
- Copying APKs
- Extracting native libraries
- Compatibility scanning
- Clone creation
- Data migration

must not block the Compose UI.

---

# 43. BATTERY

Do not continuously poll.

Bad:

```text
while(true) {
    scanInstalledApps()
    delay(5000)
}
```

Use Android package-change events where possible.

Do not keep cloned processes alive unnecessarily.

Stop unused virtual processes where technically possible.

---

# 44. STORAGE

Use app-private storage for the virtual environment.

Never store sensitive clone data in arbitrary public directories.

Use proper directory isolation:

```text
DualByHarsh/
    virtual/
        users/
            0/
                packages/
                    com.example.app/
```

The exact implementation should follow the selected virtualization engine.

---

# 45. SECURITY

Dual by Harsh must NOT:

- Root devices
- Modify system partitions
- Steal credentials
- Capture passwords
- Disable TLS
- Bypass authentication
- Circumvent DRM
- Circumvent Play Integrity
- Circumvent anti-cheat systems
- Access private data belonging to unrelated apps

The purpose is legitimate app isolation and multi-account usage.

---

# 46. ERROR HANDLING

Never show:

```text
Something went wrong
```

Show useful errors.

Example:

```text
Unable to clone WhatsApp

Reason:
This application requires a system service that is not supported by the virtual environment.
```

Log technical details only in debug builds.

Do not expose secrets.

---

# 47. DATABASE

Use Room or DataStore only where appropriate.

Store:

- Virtual users
- Clone metadata
- Settings
- Last known app state
- UI preferences

Do NOT store cloned application private databases in the application's normal metadata database.

The virtualization engine owns cloned application data.

---

# 48. SETTINGS

Settings should include:

## Virtual Space

- Space name
- Auto-start
- Performance mode
- Keep apps alive

## Applications

- Installed clones
- Update clones
- Clear cache
- Clear data

## Security

- App lock
- PIN
- Biometric
- Hide space

## Appearance

- Light
- Dark
- System

## About

Dual by Harsh
Version
Open-source licenses
Privacy information

---

# 49. PERFORMANCE PROFILING

Use:

- Android Studio Profiler
- Compose Layout Inspector
- Android Studio CPU profiler
- Memory profiler
- APK Analyzer
- Macrobenchmark
- Baseline Profiles where useful

Measure:

- Cold startup
- Warm startup
- First frame
- Frame rendering
- Memory
- CPU
- Battery
- APK size

Do not assume performance is good.

Measure it.

---

# 50. TESTING

Create unit tests for:

- Virtual package manager
- Virtual user management
- Clone metadata
- Install/remove
- Update
- Data isolation
- Compatibility detection

Create integration tests for:

- App discovery
- Clone creation
- Clone launch
- Clone removal
- Permission flow

Create manual compatibility tests.

Test applications from categories:

- Messaging
- Social media
- Browser
- Shopping
- Banking
- Games
- Camera
- File management
- Google services
- Firebase
- Native apps
- 32-bit applications
- 64-bit applications

Do not claim universal compatibility.

---

# 51. TESTING MATRIX

Create:

```text
COMPATIBILITY.md
```

with:

| Application | Android | Clone | Launch | Login | Network | Notification | Files | Notes |
|---|---:|---|---|---|---|---|---|---|
| Test App 1 | 6 | | | | | | | |
| Test App 2 | 10 | | | | | | | |
| Test App 3 | 14 | | | | | | | |
| Test App 4 | Latest | | | | | | | |
```

Populate it only with actual test results.

---

# 52. DEVELOPMENT PHASES

## Phase 0 — Research

Before writing the complete application:

- Research virtualization engines
- Evaluate licenses
- Evaluate Android compatibility
- Select architecture
- Write ARCHITECTURE.md

Do not skip this.

---

## Phase 1 — Android Foundation

Create:

- Kotlin project
- Compose
- Material 3
- Navigation
- ViewModels
- DataStore/Room where needed
- Theme
- App shell

Build and run successfully.

---

## Phase 2 — Launcher

Implement:

- Dashboard
- App grid
- App icons
- Empty state
- Add App
- Settings
- Search
- Long press

Make UI polished before connecting the virtualization engine.

---

## Phase 3 — Package Discovery

Implement:

- Installed app discovery
- Package metadata
- Icon extraction
- Package visibility
- Search
- Filtering

Do not scan repeatedly.

---

## Phase 4 — Virtual Engine

Integrate selected virtualization engine.

Implement:

- Virtual package manager
- Virtual user
- Clone installation
- Clone removal
- Clone launch

This is the most important phase.

---

## Phase 5 — Data Isolation

Implement:

- Private storage
- SharedPreferences isolation
- SQLite isolation
- Cache isolation
- WebView isolation
- Multiple virtual users

Verify original app remains untouched.

---

## Phase 6 — Android Integration

Implement:

- Permissions
- Intent routing
- Services
- Broadcasts
- Providers
- Notifications
- File picker
- Camera
- Sharing

---

## Phase 7 — Compatibility

Implement:

- Compatibility scanner
- Split APK support
- Native library handling
- Google services analysis
- Unsupported-app reporting

---

## Phase 8 — Performance

Optimize:

- Startup
- RAM
- CPU
- Battery
- Icon loading
- Clone startup
- Compose recomposition
- Background work

Add Macrobenchmark/Baseline Profile if beneficial.

---

## Phase 9 — Security

Review:

- Storage
- Permissions
- Logs
- Backup
- Credentials
- IPC
- Virtual package boundaries

Create:

```text
SECURITY.md
```

---

## Phase 10 — Production Build

Create:

```text
DualByHarsh-debug.apk
DualByHarsh-release.apk
```

Verify:

- Installation
- Launch
- Clone
- Remove
- Update
- Reboot
- Reopen
- Data persistence

---

# 53. DEFINITION OF DONE

The application is considered successful when the following workflow works:

```text
Install Dual by Harsh
        ↓
Open application
        ↓
See "Your private second space"
        ↓
Tap + Add App
        ↓
See installed applications
        ↓
Select application
        ↓
Clone application
        ↓
Clone appears in launcher
        ↓
Launch clone
        ↓
Login with different account
        ↓
Close clone
        ↓
Open clone again
        ↓
Account/data remains
        ↓
Open original application
        ↓
Original account/data remains untouched
        ↓
Clear clone data
        ↓
Original still works normally
        ↓
Remove clone
        ↓
Original remains installed
```

---

# 54. IMPORTANT COMPATIBILITY REALITY

Do not promise:

**"Every Android application will work."**

That is not technically realistic.

Some applications may depend on:

- Hardware-backed Keystore
- Play Integrity
- DRM
- Device-bound keys
- System-only permissions
- Special Android services
- Root detection
- Anti-tampering
- Native security mechanisms

Do not bypass these protections.

Instead:

1. Detect compatibility.
2. Explain the limitation.
3. Provide the best supported behavior.

The goal is **maximum legitimate compatibility**, not bypassing application security.

---

# 55. CODE QUALITY

Use:

- Kotlin idioms
- Coroutines
- StateFlow
- Immutable UI state
- Clean architecture principles
- SOLID where appropriate
- Small testable classes
- Meaningful names
- No unnecessary abstraction
- No giant classes
- No hardcoded paths
- No hardcoded package names

Avoid premature overengineering.

---

# 56. DOCUMENTATION

The final repository must contain:

```text
README.md
PLAN.md
ARCHITECTURE.md
COMPATIBILITY.md
SECURITY.md
PERFORMANCE.md
```

README must explain:

- What Dual by Harsh is
- How it works
- How to build
- How to install APK
- How to clone apps
- Supported Android versions
- Known limitations
- Supported architectures
- License information

---

# 57. FINAL ENGINEERING RULE

Do not spend most of the project building beautiful screens while the virtualization engine is nonfunctional.

The project has two equally important parts:

```text
                    Dual by Harsh
                          │
             ┌────────────┴────────────┐
             │                         │
        Beautiful UI              Real Cloning
       Kotlin + Compose        Virtual Android Engine
             │                         │
             └────────────┬────────────┘
                          │
                   Production APK
```

The UI must be polished.

The virtualization must be real.

Performance must be measured.

Compatibility limitations must be honest.

---

# FINAL REQUEST TO THE CODING AGENT

Start by inspecting the repository/environment.

Then:

1. Research and evaluate the available virtualization approaches.
2. Select the most realistic legally reusable architecture.
3. Write `ARCHITECTURE.md`.
4. Create the Kotlin + Jetpack Compose project.
5. Build the launcher UI.
6. Integrate the virtualization engine.
7. Implement real application cloning.
8. Implement isolated application data.
9. Implement virtual application launching.
10. Implement permissions/intents/services/providers.
11. Implement notifications and file sharing where possible.
12. Implement compatibility detection.
13. Optimize startup, RAM, CPU and battery.
14. Test on Android 6+.
15. Build release APK.
16. Document every known limitation.

Do not stop at a UI prototype.

Do not create mock cloning functionality.

Do not use fake progress.

Do not claim unsupported features work.

The final result must be a real, buildable Android application:

# **Dual by Harsh**

A lightweight, smooth, polished Android virtual second-space application that allows users to run supported cloned applications independently from their original applications, without root or a server.