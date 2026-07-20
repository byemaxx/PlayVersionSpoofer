# PlayVersionSpoofer
[中文](README_zh.md) | [English](README.md)

**Permanently Stop Google Play Store Self-Updates.**

An Xposed module that prevents the Google Play Store from automatically updating **itself**.

## 🚀 Features

- Makes the Play Store report that it is up to date
- Prevents only Play Store self-updates and does not affect normal app updates

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

> **📝 Note**: If the default mode does not work with a particular Play Store version, enable **Legacy PackageInfo fallback** in the module app, then force stop and reopen Play Store.

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
