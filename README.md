# DarQ

[![Latest Release](https://img.shields.io/github/v/release/rove24/DarQ?label=Latest%20Release)](https://github.com/rove24/DarQ/releases)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![API Level](https://img.shields.io/badge/API-29%2B-brightgreen.svg)](https://developer.android.com)
[![Target API](https://img.shields.io/badge/Target-Android%2012L%20(API%2032)-orange.svg)](https://developer.android.com)
[![Xposed](https://img.shields.io/badge/Xposed-Module-purple.svg)](https://github.com/LSPosed/LSPosed)
[![Shizuku](https://img.shields.io/badge/Shizuku-Supported-blue.svg)](https://shizuku.rikka.app/)

Per-app selectable force dark option for Android, with scheduled automatic switching.

Refactored with **Material Design 3**, seamlessly supporting **LSPosed (Xposed) Module**, **Shizuku (Rootless ADB)**, and **Root** operation modes on Android 10 through Android 14+.

[中文说明 (README-CN.md)](./README-CN.md)

---
<img width="8192" height="4510" alt="IMG_20260907_052102" src="https://github.com/user-attachments/assets/752d05c7-a10e-4fd1-9299-845bfa32e937" />

## ✨ Features

- 🌙 **Per-App Selectable Force Dark**:
  - Leverages Android's native system-level Force Dark rendering algorithm.
  - Forces dark mode on third-party apps that lack built-in dark themes or have incomplete dark mode implementations.
  - Clean inversion with natural contrast, eliminating glaring white screens.
- 🔄 **Multiple Working Modes**:
  - 🛡️ **LSPosed / Xposed Module**: Injected via Xposed framework. Zero background service footprint, no persistent process needed, highly power-efficient.
  - ⚡ **Shizuku (ADB) Mode**: No root required. Communicates with high-level system APIs through Shizuku, eliminating lags caused by traditional accessibility services.
  - 👑 **Root Mode**: Direct control via Root access on rooted devices.
- ⏰ **Smart Auto Dark Theme**:
  - **Sunrise / Sunset**: Uses astronomical calculations (Commons SunCalc) based on your geographic location to automatically switch themes.
  - **Custom Time Schedule**: Set custom start and end times for automatic dark mode activation.
  - **Google Clock MD3 Time Picker**: Authentic Material Design 3 time picker experience with pill capsules, circular dials, and 28dp rounded shapes.
- 🎨 **Modern Material Design 3 UI**:
  - Full support for Android 12+ Monet dynamic color extraction (Material You).
  - Grouped rounded cards, MD3 switch controls, fluid transitions, and responsive touch zones.
  - Smooth collapsible card animation for "Show apps without launcher interface".
  - MD3 capsule pill dialogs with precise contrast and color balance.
- 💾 **Configuration Backup & Restore**:
  - Export and backup your DarQ configuration to local storage with one click.
  - Restore saved configurations seamlessly from local files.
  - Full data reset option guarded by a confirmation dialog.
- 🛠️ **Developer & Advanced Tools**:
  - One-click termination of duplicate services (useful when switching between Shizuku and Root modes).
  - Real-time DarQ service status and architecture monitoring.

---

## 📱 Operation Modes

| Mode | Needs Root? | Background Service | Recommended For |
| :--- | :---: | :---: | :--- |
| **LSPosed Module** | No (Needs Framework) | ❌ None | Devices with LSPosed installed (Best battery life, seamless) |
| **Shizuku (ADB)** | ❌ No Root | ⚠️ Service required | Unrooted devices with Shizuku / Wireless Debugging configured |
| **Root Mode** | Yes | ⚠️ Service required | Devices with Magisk / KernelSU / APatch root access |

> [!TIP]
> If you have **LSPosed** installed, simply enable DarQ in the LSPosed Manager and select the system framework and target scopes. No background root/Shizuku service needs to run inside the app!

---

## 🛠️ Compatibility

- **Android Version**: Android 10 (API 29) ~ Android 14+ (API 34)
- **Frameworks**: LSPosed, EdXposed, TaiChi Magisk, classic Xposed
- **ROM Support**: AOSP, Google Pixel OS, LineageOS, HyperOS, OriginOS, ColorOS, and other custom/OEM ROMs

---

## 📥 Downloads

- **[GitHub Releases](https://github.com/rove24/DarQ/releases)**: Download the latest compiled APK.
- Pre-built APKs are also located in the `release/` directory of this repository.

---

## 🔨 Building from Source

This project uses the standard Gradle build system:

```bash
# 1. Clone repository
git clone https://github.com/rove24/DarQ.git
cd DarQ

# 2. Build Release APK
./gradlew assembleRelease
```

The output APK will be placed in `app/build/outputs/apk/release/DarQ-v1.4.apk`.

---

## 📜 Changelog

See [Changelog (LOG-CN.md)](./LOG-CN.md).

---

## 🙏 Credits & Acknowledgements

- Original Author: [KieronQuinn/DarQ](https://github.com/KieronQuinn/DarQ)
- [LSPosed](https://github.com/LSPosed/LSPosed)
- [Shizuku](https://github.com/RikkaApps/Shizuku)
- [Commons SunCalc](https://shredzone.org/maven-sites/commons-suncalc/)

---

## 📄 License

This project is licensed under the [Apache-2.0 License](LICENSE).
