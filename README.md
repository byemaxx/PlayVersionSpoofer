# PlayVersionSpoofer
[中文](README_zh.md) | [English](README.md)

**Permanently Stop Google Play Store Self-Updates.**

An Xposed module that prevents the Google Play Store from automatically updating **itself**.

## 🚀 How it works

The module dynamically locates the Play Store's self-update scheduling decision and returns **no update scheduled**. This makes the manual update check report that the store is up to date without changing its real `PackageInfo`, network User-Agent, or installed-package state.

The resolved method descriptor is cached together with the installed Play Store `versionCode`. Normal launches restore the hook from this cache without starting DexKit; a new search only occurs after the Play Store version changes or the cache becomes invalid.

The previous global `99999999` / `999.999.999` spoof remains available as a **Legacy PackageInfo fallback** in the module app. It is disabled by default because it can interfere with Play Store network requests, especially on older releases.

## 🔍 How to Verify PlayVersionSpoofer is Working

1. Open the Google Play Store
2. Tap the profile icon in the top-right corner
3. Select "Settings" from the menu
4. Scroll down to find the "About" section
5. Tap **"Update Play Store"**. If it says "**Google Play Store is up to date**", the module is working correctly.

<p align="center">
  <img src="imgs/img1.jpg" width="45%" alt="Google Play Settings" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="imgs/img2.jpg" width="45%" alt="Version Information" />
</p>

> **📝 Note**: The default method keeps the Play Store's real version visible everywhere. If dynamic detection does not support a particular Play Store build, enable **Legacy PackageInfo fallback** in the module app, then force stop and reopen Play Store.

## 📋 Requirements

- Rooted Android device
- Xposed Framework (LSPosed recommended)
- Android 9.0+

## 🔧 Installation

1. Download and install the APK file from [Releases](https://github.com/byemaxx/PlayVersionSpoofer/releases)
2. Enable the module in your LSPosed manager
3. Select "Google Play Store" as the target scope
4. Open the PlaySpoofer app to verify activation status

## ⚠️ Disclaimer

Educational use only. Use at your own risk.

**Please [Star](https://github.com/byemaxx/PlayVersionSpoofer) the project if you like this module!** 🌟
Any contribution or suggestion is welcome.
