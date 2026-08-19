# Compatibility

The app rates each installed app before cloning (see `AppDiscovery.rate`). Ratings
are conservative and honest — it never claims universal compatibility.

## Rating categories
- **High** — expected to work fully in the private space.
- **Partial** — works, but some features may be limited (typically Google Play
  Services: push notifications, Google Sign-In).
- **Unsupported** — device-bound apps (banking, UPI, wallets, DRM streaming,
  hardware-key authenticators). These rely on hardware-backed keys / Play
  Integrity that a second instance cannot satisfy. The app does **not** attempt
  to bypass them.

## Test matrix (populate with real on-device results)

| Application | Android | Clone | Launch | Login | Network | Notification | Files | Notes |
|---|---:|---|---|---|---|---|---|---|
| Messaging app | 8 |  |  |  |  |  |  |  |
| Social app | 11 |  |  |  |  |  |  |  |
| Browser | 13 |  |  |  |  |  |  |  |
| Banking app | 14 |  |  |  |  |  |  | Expected: Unsupported |

> These rows are intentionally blank — fill them in only with verified results
> from a real device, as required by the plan (§51).

## OEM notes
- Some manufacturers (a minority) disable managed profiles. On those devices the
  app shows "Second space not supported" instead of failing silently.
- Cross-profile intent forwarding behavior can vary slightly by OEM; if a clone
  does not appear after "Add", it usually means the app blocks work-profile
  installation.
