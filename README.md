# Community Cordova Plugin AdMob

[![NPM version](https://img.shields.io/npm/v/community-cordova-plugin-admob)](https://www.npmjs.com/package/community-cordova-plugin-admob)
[![Downloads](https://img.shields.io/npm/dm/community-cordova-plugin-admob)](https://www.npmjs.com/package/community-cordova-plugin-admob)

A powerful and reliable Google AdMob plugin for Cordova/Ionic applications.

## Support This Plugin

I dedicate a considerable amount of my free time to developing and maintaining many Cordova plugins for the community ([See the list with all my maintained plugins][community_plugins]).

To help ensure this plugin is kept updated, new features are added and bugfixes are implemented quickly, please donate a couple of dollars (or a little more if you can stretch) as this will help me to afford to dedicate time to its maintenance.

Please consider donating if you're using this plugin in an app that makes you money, or if you're asking for new features or priority bug fixes. Thank you!

[![Sponsor Me](https://img.shields.io/static/v1?label=Sponsor%20Me&style=for-the-badge&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86)](https://github.com/sponsors/eyalin)

## Credits & Acknowledgments

This plugin was originally forked from [admob-plus](https://github.com/admob-plus/admob-plus) by [Ratson](https://github.com/niceonedaviddevs).

A huge thank you to [Ratson](https://github.com/niceonedaviddevs) for creating and maintaining the original admob-plus plugin. The original work laid the foundation for this plugin, and we are grateful for their contributions to the Cordova community.

Due to the original plugin no longer being actively maintained, this standalone repository was created to continue development, provide updates, and ensure compatibility with the latest AdMob SDK versions.

## Features

- Banner Ads
- Interstitial Ads
- Rewarded Ads
- Rewarded Interstitial Ads
- App Open Ads
- Native Ads
- WebView Ads
- Full TypeScript support
- iOS and Android support

## Installation

```bash
cordova plugin add community-cordova-plugin-admob --variable APP_ID_ANDROID=ca-app-pub-xxx~yyy --variable APP_ID_IOS=ca-app-pub-xxx~yyy
```

Or with Ionic:

```bash
ionic cordova plugin add community-cordova-plugin-admob --variable APP_ID_ANDROID=ca-app-pub-xxx~yyy --variable APP_ID_IOS=ca-app-pub-xxx~yyy
```

## SDK Versions

| Platform | SDK | Version |
|----------|-----|---------|
| Android | play-services-ads | 24.7.0 |
| iOS | Google-Mobile-Ads-SDK | 12.12.0 |

## Basic Usage

### Initialize AdMob

```typescript
document.addEventListener('deviceready', async () => {
  await admob.start();
}, false);
```

### Banner Ad

```typescript
const banner = new admob.BannerAd({
  adUnitId: 'ca-app-pub-xxx/yyy',
});
await banner.show();
```

### Interstitial Ad

```typescript
const interstitial = new admob.InterstitialAd({
  adUnitId: 'ca-app-pub-xxx/yyy',
});
await interstitial.load();
await interstitial.show();
```

### Rewarded Ad

```typescript
const rewarded = new admob.RewardedAd({
  adUnitId: 'ca-app-pub-xxx/yyy',
});
await rewarded.load();
await rewarded.show();
```

## Contributing

- Star this repository
- Open issue for feature requests
- [Sponsor this project](https://github.com/sponsors/eyalin)

## License

This project is [MIT licensed](LICENSE).

[community_plugins]: https://github.com/niceonedaviddevs?tab=repositories&q=community&type=&language=&sort=
