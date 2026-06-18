# DeepSeek 余额监控 Android 小组件

一个能在 Android **系统桌面直接显示** DeepSeek API 余额的原生小组件 App，无需打开应用即可看到余额。

## ✨ 功能特性

- 📱 **系统级桌面小组件** —— 像时钟/天气小组件一样直接显示在桌面上
- 🔄 **自动定时刷新** —— 后台定时更新余额（默认 30 分钟）
- 👆 **点击交互** —— 点击余额打开配置页，点击状态指示器手动刷新
- 🎨 **深色渐变设计** —— 与现代 Android 主题完美融合
- 🔐 **本地存储 API Key** —— 数据完全本地化，不上传任何服务器
- 📊 **智能路径解析** —— 支持嵌套 JSON 和数组下标（如 `balance_infos[0].total_balance`）
- 🛡️ **错误处理** —— 网络异常时显示离线状态

## 📱 效果预览

```
┌───────────────────────┐
│ DeepSeek          ●  │  ← 品牌 + 状态点
│                       │
│    ¥ 128.50           │  ← 大字号显示余额
│       余额            │
│                       │
│      14:32 更新       │  ← 更新时间
└───────────────────────┘
```

## 🚀 快速开始（手机用户）

### 第一步：下载并安装 APK

1. 在手机上打开浏览器，访问 GitHub Releases 页面（构建后会生成链接）
2. 下载 `DeepSeekBalance-latest.apk`
3. 在手机文件管理中找到下载的 APK，点击安装
4. 如果提示"未知来源"，到 **设置 → 安全 → 允许安装未知来源应用**

### 第二步：申请 DeepSeek API Key

1. 访问 [https://platform.deepseek.com](https://platform.deepseek.com)
2. 注册并登录账号
3. 在 **API Keys** 页面创建一个新的 API Key
4. **复制保存** 你的 Key（形如 `sk-xxxxxxxxxxxx`）

### 第三步：配置 App

1. 打开手机上的 **DeepSeek 余额** App
2. 在 **API Key** 输入框粘贴你的 Key
3. 点击 **测试连接** 验证配置正确
4. 看到 "✓ 连接成功！余额：¥ xxx.xx" 后点击 **保存并刷新**

### 第四步：添加桌面小组件

1. **长按桌面空白处** → 选择 **小组件**
2. 找到 **DeepSeek 余额** 分类
3. 选择一个尺寸（推荐 2x1 或 4x2）拖到桌面
4. **完成！** 现在你可以直接在桌面上看到余额了 🎉

### 日常使用

- **查看余额**：直接在桌面上看，无需打开 App
- **手动刷新**：点击小组件上的状态文字（"在线"/"离线"）
- **修改配置**：点击余额数字打开 App
- **自动刷新**：默认每 30 分钟自动更新一次

## 🔧 高级配置

在 App 配置页可以调整：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| API 地址 | `https://api.deepseek.com/user/balance` | 一般无需修改 |
| API Key | - | **必填** |
| 货币单位 | `¥` | 显示在余额前的符号 |
| 刷新间隔 | 30 分钟 | 范围 5-1440 分钟 |

## 🛠️ 自己构建 APK

如果需要自己构建 APK：

```bash
# 克隆代码
git clone <your-repo-url>
cd android-app

# 构建 Release APK
./gradlew assembleRelease

# 生成的 APK 在：
# app/build/outputs/apk/release/app-release.apk
```

## ❓ 常见问题

**Q: 安装时提示"未知来源"？**
A: 设置 → 应用 → 安装未知应用 → 选择你的浏览器/文件管理器 → 允许。

**Q: 添加小组件时找不到？**
A: 部分手机需要在桌面长按 → "小组件"，或从应用列表中找到 DeepSeek 余额 App。

**Q: 余额一直显示 "!" 离线？**
A: 点击 App 中的"测试连接"检查 API Key 是否正确，网络是否正常。

**Q: 小组件不更新？**
A: Android 系统的电池优化可能限制后台刷新，到 设置 → 电池 → 应用电池管理 → DeepSeek 余额 → 选择"不优化"。

**Q: 可以监控其他 API 吗？**
A: 暂时只内置了 DeepSeek，但可通过修改代码适配其他 API（修改 `ConfigManager.kt` 的默认值和 `BalanceRepository.kt` 的请求头）。

## 🔒 隐私与安全

- ✅ API Key 仅存储在本地 SharedPreferences
- ✅ 所有网络请求直接发往 DeepSeek，不经过任何第三方服务器
- ✅ 不收集任何使用数据
- ⚠️ 建议在正规渠道下载 APK，谨防钓鱼应用

## 📂 项目结构

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/deepseek/balance/
│   │   │   ├── config/
│   │   │   │   ├── ConfigManager.kt      # 配置管理
│   │   │   │   └── ConfigActivity.kt     # 配置页面
│   │   │   ├── network/
│   │   │   │   └── BalanceRepository.kt  # 网络请求
│   │   │   └── widget/
│   │   │       ├── BalanceWidgetProvider.kt  # 小组件
│   │   │       └── WidgetUpdateReceiver.kt   # 定时刷新
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── widget_balance.xml    # 小组件布局
│   │   │   │   └── activity_config.xml   # 配置页布局
│   │   │   ├── drawable/                  # 背景、按钮、图标
│   │   │   ├── values/                    # 颜色、主题、字符串
│   │   │   ├── xml/balance_widget_info.xml
│   │   │   └── mipmap-*/                  # 应用图标
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/
├── gradlew / gradlew.bat
└── .github/workflows/build.yml           # 自动构建
```

## 📄 License

MIT License
