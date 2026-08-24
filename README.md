# 防息屏助手 AntiSleep

> 🛡️ 让 VIVO / Android 手机在后台定时自动点击屏幕，防止自动息屏
> Keep your Android screen awake with periodic auto-taps (Accessibility Service based)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen)](https://developer.android.com/about/versions/oreo)
![Size](https://img.shields.io/badge/APK-26KB-blue)

一款**纯离线、零依赖、免 root** 的安卓防息屏工具：通过系统无障碍服务定时模拟点击屏幕，重置系统息屏计时器，让手机在挂机（游戏、看视频、挂任务）时保持常亮。支持悬浮窗实时状态与定时自动退出。

## ✨ 功能特性

- ⏱️ **定时自动点击**：可调间隔（5 秒 ~ 1 小时），默认 30 秒，触摸事件重置系统息屏计时器
- 🖱️ **三种点击模式**：屏幕中央 / 中央附近随机（推荐）/ 自定义百分比坐标
- 🪟 **悬浮窗实时状态**：显示运行状态、剩余时间、已点击次数；可拖动、点击打开主界面
- ⏲️ **定时自动退出**：设定运行时长（分钟），到点自动停止点击并移除悬浮窗，手机自然息屏
- 📢 **前台服务保活**：常驻通知 + START_STICKY 自动重启，状态栏可见运行状态
- 🔄 **开机自启**：重启手机后自动恢复运行（需配合 VIVO 自启动白名单）
- 🧪 **一键测试**：立即模拟一次点击，快速验证无障碍手势是否生效
- ⚡ **一键延长息屏时间**：授予"修改系统设置"权限后，一键将系统息屏超时设为 30 分钟（双保险）

## 📱 支持的设备

- Android 8.0（API 26）及以上，适配至 Android 15
- VIVO（Funtouch OS / OriginOS）、小米、华为、OPPO 等主流机型均可
- 免 root、免 ADB、完全离线、无广告、无网络权限、不收集任何数据

## 🚀 快速开始

1. 下载 **[`AntiSleep-release.apk`](AntiSleep-release.apk)**（或从 [Releases](../../releases) 获取）并安装
2. 打开 App → 点击 **"去开启无障碍服务"** → 开启 **"防息屏自动点击"**
3. 返回 App → 点击 **"启动防息屏"**（状态栏出现常驻通知即成功）
4. （可选）设置 → 开启悬浮窗 / 设定定时自动停止时长

### VIVO 专属设置（必看）

VIVO 省电策略激进，不设置几分钟内会杀进程：

| 设置项 | 路径 |
|--------|------|
| 后台耗电白名单 | i管家 → 应用管理 → 后台耗电管理 → 防息屏助手 → 允许后台高耗电 |
| 自启动 | i管家 → 应用管理 → 自启动 → 打开防息屏助手 |
| 通知权限 | 设置 → 通知与状态栏 → 应用通知管理 → 防息屏助手 → 允许通知 |
| 锁定后台 | 最近任务界面 → 下拉应用卡片锁定 |

## 🏗️ 项目结构

```
AntiSleep/
├── app/
│   └── src/main/
│       ├── java/com/antisleep/keepscreen/
│       │   ├── MainActivity.java          # 主界面（设置 + 启动/停止）
│       │   ├── AutoClickService.java      # 无障碍服务（模拟点击核心）
│       │   ├── KeepAwakeService.java      # 前台服务（保活 + 定时 + 悬浮窗刷新）
│       │   ├── OverlayView.java           # 悬浮窗（实时状态）
│       │   ├── BootReceiver.java          # 开机自启
│       │   └── Prefs.java                 # 配置存储
│       └── res/                           # 布局 / 资源 / 无障碍配置
└── build.gradle.kts                       # Gradle 构建（AGP 8.5.2）
```

纯 Java 实现，**零第三方依赖**，构建简单、兼容性最佳。

## 🔧 自行构建

```bash
# 环境：JDK 17+ / Android SDK 34
./gradlew assembleRelease   # 或 gradle assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

> 国内网络环境构建提示：如无法访问 Google Maven，可将 `settings.gradle.kts` 中的仓库源
> 替换为腾讯云镜像 `https://mirrors.cloud.tencent.com/nexus/repository/maven-public/`。

## 🔒 隐私与安全

- 应用**不申请网络权限**，完全离线运行
- 无广告、无统计、无数据上传
- 仅使用系统无障碍 API 模拟触摸，不读取任何屏幕内容（`canRetrieveWindowContent=false`）
- 停止后无任何后台行为

## 📄 许可证

[MIT](LICENSE) © 2026 MLXIAOCHANGAN

---

*From the workbench of [塔斯汀王子](https://github.com/MLXIAOCHANGAN) 🍔*
