# 屏幕翻译 (ScreenTranslator)

一款安卓实时屏幕翻译 App，能够截取屏幕内容、识别文字（OCR）、并通过悬浮窗实时显示翻译结果。**支持完全离线翻译**（模型下载后）。

---

## 功能特性

| 功能 | 说明 |
|------|------|
| 📸 实时屏幕捕获 | 使用 MediaProjection API，可配置截图间隔（1–10 秒） |
| 🔍 多语言 OCR | 使用 Google ML Kit 识别拉丁、中、日、韩等文字 |
| 🌐 离线翻译 | 使用 ML Kit Translation，支持 19 种语言双向翻译 |
| 🪟 可拖动悬浮窗 | 半透明悬浮窗覆盖在屏幕上，可拖动、最小化、关闭 |
| ⚙️ 丰富设置 | 可调透明度、字体大小、截图间隔 |
| 🔔 前台服务通知 | 支持从通知栏一键停止服务 |

---

## 技术架构

```
app/
├── ui/
│   └── MainActivity.kt          # 主界面：语言选择、参数配置、启停控制
├── service/
│   └── ScreenTranslationService.kt  # 前台服务：截图→OCR→翻译→通知悬浮窗
├── overlay/
│   └── OverlayManager.kt        # 悬浮窗：WindowManager 管理、拖动、最小化
└── utils/
    ├── LanguageConfig.kt         # 支持的语言列表
    └── PreferenceManager.kt      # SharedPreferences 持久化用户设置
```

### 核心技术栈

- **MediaProjection API** — 屏幕录制权限与截图
- **ImageReader** — 高效获取屏幕帧（半分辨率，降低性能开销）
- **ML Kit Text Recognition** — 设备端 OCR，支持拉丁/中/日/韩
- **ML Kit Translation** — 设备端翻译，首次需联网下载模型（~30-50MB/语言对），之后完全离线
- **WindowManager TYPE_APPLICATION_OVERLAY** — 系统级悬浮窗
- **Kotlin Coroutines** — 异步截图/OCR/翻译流水线，不阻塞主线程

---

## 构建步骤

### 环境要求

- Android Studio Hedgehog (2023.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.0+

### 步骤

1. **克隆/导入项目**
   ```bash
   # 将整个 ScreenTranslator 目录导入 Android Studio
   File → Open → 选择 ScreenTranslator 目录
   ```

2. **同步依赖**
   ```
   Android Studio 会自动提示 Sync Project with Gradle Files，点击同步即可
   ```

3. **运行**
   ```
   连接安卓设备（Android 8.0+），点击 Run ▶
   ```

---

## 权限说明

| 权限 | 用途 |
|------|------|
| `SYSTEM_ALERT_WINDOW` | 显示悬浮窗（需用户手动开启） |
| `FOREGROUND_SERVICE` | 运行前台服务保持截图持续运行 |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Android 14+ 媒体投影前台服务类型 |
| 屏幕录制权限 | 通过 MediaProjectionManager 动态申请，每次启动服务时弹窗 |

---

## 使用流程

```
启动 App
  → 选择识别语言（屏幕上文字的语言）
  → 选择翻译目标语言
  → 调整截图间隔 / 透明度 / 字体大小
  → 点击【开始实时翻译】
  → 授予「悬浮窗」权限（仅首次）
  → 授予「屏幕录制」权限（每次启动服务时）
  → 悬浮窗出现在屏幕上，实时显示翻译
  → 可拖动悬浮窗标题栏移动位置
  → 点击 — 最小化 / × 停止服务
```

---

## 注意事项

1. **首次使用**需要网络连接以下载 ML Kit 翻译模型（约 30–50 MB/语言对）。下载完成后完全离线可用。
2. 建议截图间隔设置为 **2–3 秒**，以平衡实时性与电池消耗。
3. 在 **Android 14 (API 34)** 上，`mediaProjection` 前台服务类型需要在 `AndroidManifest.xml` 中声明，本项目已包含。
4. 部分系统/厂商（如 MIUI、ColorOS）可能需要额外在系统设置中开启「后台弹出界面」权限。
5. 本 App 使用半分辨率截图（宽高各缩小一半）以降低内存和 CPU 占用，对文字识别效果影响极小。

---

## 支持的语言

英文、简体中文、日语、韩语、西班牙语、法语、德语、葡萄牙语、俄语、阿拉伯语、印地语、意大利语、印尼语、越南语、泰语、土耳其语、荷兰语、波兰语、瑞典语

---

## License

MIT License
