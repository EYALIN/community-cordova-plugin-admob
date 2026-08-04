# Changelog

All notable changes to this project will be documented in this file.

## [1.2.0] - 2026-08-04

### Fixed

- **Android: a top banner and a bottom banner can now be shown at the same time**
  ([#4](https://github.com/EYALIN/community-cordova-plugin-admob/issues/4)).
  Position-based banners (no `offset`) shared a single static layout slot, so the
  second banner evicted the first and only one was ever visible. The shared
  container now holds one slot per position and each banner mounts into the slot
  matching its own position. iOS was already unaffected.
- **Android: unmounting one banner no longer tears down another's layout.**
  Hiding, destroying, or switching a banner to `offset` mode used to dismantle the
  shared container outright. Teardown now happens only after the last banner
  detaches.
- **Android: window insets are applied per position.** The inset values are now
  read once for the shared container and applied to every mounted banner, instead
  of only the banner that happened to create the layout.
- **Android: each banner handles its own orientation change.** The
  last-seen screen width was static, so with two banners only the first one
  reloaded and re-laid out on rotation.
- **Android: banner sizes are reported in real density-independent pixels.**
  `pxToDp()` divided by `xdpi` (the panel's physical pixel density) instead of
  `density` (the bucket the platform actually scales by). On devices where those
  disagree, the `size` event, the `load` payload, and any `width`/`height`/
  `maxHeight` given in pixels for an adaptive banner were all skewed. This now
  matches the values iOS reports.

### Note

Banner sizes reported on Android may change slightly after this release. The new
values are the correct ones; if your layout compensated for the old skew, drop the
correction.

## [1.0.0] - 2025-01-17

### Initial Release

This is the first release of `community-cordova-plugin-admob`, a standalone fork of the original [admob-plus](https://github.com/admob-plus/admob-plus) plugin.

### Credits

A huge thank you to the [admob-plus](https://github.com/admob-plus) team for creating and maintaining the original plugin. Due to the original plugin no longer being actively maintained, this standalone repository was created to continue development and provide updates.

### SDK Versions

- **Android**: play-services-ads `24.7.0`
- **iOS**: Google-Mobile-Ads-SDK `12.12.0`

### Features

- Banner Ads
- Interstitial Ads
- Rewarded Ads
- Rewarded Interstitial Ads
- App Open Ads
- Native Ads
- WebView Ads
- Full TypeScript support
- iOS and Android support

### Supported Platforms

- Android (cordova-android >= 6.0.0)
- iOS (cordova-ios >= 5.0.0)
- Browser (limited support)
