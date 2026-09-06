# DarQ

[![Latest Release](https://img.shields.io/github/v/release/rove24/DarQ?label=Latest%20Release)](https://github.com/rove24/DarQ/releases)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![API Level](https://img.shields.io/badge/API-29%2B-brightgreen.svg)](https://developer.android.com)
[![Target API](https://img.shields.io/badge/Target-Android%2012L%20(API%2032)-orange.svg)](https://developer.android.com)
[![Xposed](https://img.shields.io/badge/Xposed-Module-purple.svg)](https://github.com/LSPosed/LSPosed)
[![Shizuku](https://img.shields.io/badge/Shizuku-Supported-blue.svg)](https://shizuku.rikka.app/)

为每个应用独立强制开启深色模式，支持按设定时间自动切换。

基于原版 DarQ 深度重构，全面引入 **Material Design 3** 设计语言，完美支持 **LSPosed (Xposed) 模块**、**Shizuku (免 Root)** 与 **Root** 三种工作模式，适配 Android 10 至 Android 14+ 最新系统。

[English README](./README.md)

---

## ✨ 核心特性

- 🌙 **单应用独立强制深色 (Per-App Force Dark)**：
  - 利用系统原生底层 Force Dark 渲染机制，为尚未适配暗色模式的第三方应用强制开启深色。
  - 颜色反转自然、界面对比度舒适，告别刺眼白底。
- 🔄 **多元化工作模式**：
  - 🛡️ **LSPosed / Xposed 模式**：通过 Xposed 框架无缝注入，无需任何常驻后台服务与进程保活，开机即用，极致省电。
  - ⚡ **Shizuku (ADB) 模式**：无需 Root 权限，通过 Shizuku 进行系统级高权限通信，摆脱无障碍辅助服务的卡顿与繁琐。
  - 👑 **Root 模式**：通过系统 Root 权限直接驱动后台服务。
- ⏰ **智能自动深色模式 (Auto Dark Theme)**：
  - **日出日落**：基于地理位置算法（Commons SunCalc），根据本地真实的日落与日出时间精准切换。
  - **自定义时间段**：自由设定自动开启与关闭深色模式的具体起止时间。
  - **Google 时钟同款 MD3 时间选择器**：采用药丸胶囊高亮块、圆形指针交互与 28dp 大圆角原生设计。
- 🎨 **现代化 Material Design 3 界面**：
  - 深度支持 Android 12+ Monet 原生动态取色（Material You），随系统壁纸自适应流动。
  - 分组圆角卡片、MD3 Switch 开关、平滑交互与精准触控响应。
  - “显示没有界面的应用”平滑折叠动画与触控隔离。
  - MD3 药丸胶囊风格对话框（规范的圆角、颜色与对比度）。
- 💾 **数据配置备份与还原**：
  - 支持一键将 DarQ 配置完整备份至本地存储。
  - 支持从本地备份文件轻松恢复应用配置。
  - 提供安全的全部数据重置选项，配备二次确认弹窗。
- 🛠️ **开发者与高级选项**：
  - 一键关闭重复服务（在 Shizuku 与 Root 模式之间切换时使用）。
  - 实时监控 DarQ 服务运行状态与架构信息。

---

## 📱 运行模式说明

| 运行模式 | 是否需要 Root | 后台进程常驻 | 推荐场景 |
| :--- | :---: | :---: | :--- |
| **LSPosed 模块** | 否（需框架） | ❌ 无需常驻 | 已安装 LSPosed 框架的设备（最省电、最无感） |
| **Shizuku (ADB)** | ❌ 免 Root | ⚠️ 依赖服务 | 未 Root 设备，已配置 Shizuku / 无线调试 |
| **Root 模式** | 是 需要 | ⚠️ 依赖服务 | 已获取 Magisk / KernelSU / APatch 的设备 |

> [!TIP]
> 如果您使用了 **LSPosed** 框架，仅需在 LSPosed 管理器中启用本模块，勾选系统框架及所需作用域，无需开启应用内的 Root 或 Shizuku 服务即可直接生效。

---

## 🛠️ 兼容性

- **系统要求**：Android 10 (API 29) ～ Android 14+ (API 34)
- **支持框架**：LSPosed、EdXposed、太极·Magisk、传统 Xposed
- **系统适配**：原生 AOSP、Google Pixel、LineageOS、HyperOS、OriginOS、ColorOS 等主流类原生及定制系统

---

## 📥 下载安装

- **[GitHub Releases](https://github.com/rove24/DarQ/releases)**：下载最新版 APK 安装包。
- 在 `release/` 目录下亦提供构建完成的最新稳定安装包。

---

## 🔨 从源码构建

本项目基于标准 Gradle 构建系统：

```bash
# 1. 克隆本仓库
git clone https://github.com/rove24/DarQ.git
cd DarQ

# 2. 构建 Release APK
./gradlew assembleRelease
```

构建生成的 APK 产物位于 `app/build/outputs/apk/release/DarQ-v1.4.apk`。

---

## 📜 更新日志

详见 [更新日志 (LOG-CN.md)](./LOG-CN.md)。

---

## 🙏 鸣谢与致敬

- 原作者 [KieronQuinn/DarQ](https://github.com/KieronQuinn/DarQ)
- [LSPosed](https://github.com/LSPosed/LSPosed)
- [Shizuku](https://github.com/RikkaApps/Shizuku)
- [Commons SunCalc](https://shredzone.org/maven-sites/commons-suncalc/)

---

## 📄 开源协议

本项目采用 [Apache-2.0 License](LICENSE) 协议开源。
