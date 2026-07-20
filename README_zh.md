# PlayVersionSpoofer
[中文](README_zh.md) | [English](README.md)

**彻底禁止 Google Play 商店自动更新**

一个阻止 Google Play 商店进行**自我更新**的 Xposed 模块。

## 🚀 工作原理

模块会动态定位 Play 商店的自更新调度判断，并返回“没有安排更新”。这样手动检查更新会提示已是最新版本，同时保持真实的 `PackageInfo`、网络 User-Agent 和已安装应用状态不变。

定位到的方法描述符会与 Play 商店 `versionCode` 一起缓存。日常启动直接从缓存恢复 Hook，不会启动 DexKit；只有 Play 商店版本变化或缓存失效时才重新搜索。

原有的全局 `99999999` / `999.999.999` 伪装仍作为 **旧版 PackageInfo fallback** 保留在模块设置中。它默认关闭，因为这类全局伪装可能干扰 Play 商店的网络请求，特别是旧版本。

## 🔍 如何验证 PlayVersionSpoofer 是否正常工作

1. 打开 Google Play 商店
2. 点击右上角的个人资料图标
3. 从菜单中选择"设置"
4. 向下滚动找到"关于"部分
5. 点击 **"更新 Play 商店"**。如果提示 **"Google Play 商店已是最新版本"**，则说明模块工作正常。

<p align="center">
  <img src="imgs/img1.jpg" width="45%" alt="Google Play 设置" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="imgs/img2.jpg" width="45%" alt="版本信息" />
</p>

> **📝 注意**：默认方法会在所有位置保留 Play 商店的真实版本。如果某个 Play 商店版本不受动态检测支持，可在模块应用中开启 **旧版 PackageInfo fallback**，然后强行停止并重新打开 Play 商店。

## 📋 使用要求

- 已 Root 的 Android 设备
- Xposed 框架（推荐 LSPosed）
- Android 9.0+

## 🔧 安装方法

1. 从 [Releases](https://github.com/byemaxx/PlayVersionSpoofer/releases) 下载并安装 APK 文件
2. 在 LSPosed 管理器中启用该模块
3. 选择 "Google Play Store" 作为作用域
4. 打开 PlaySpoofer 应用检查激活状态

## ⚠️ 免责声明

仅用于个人学习目的，使用风险自负。
