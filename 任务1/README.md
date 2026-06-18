# API 余额监控 PWA

一个可以添加到手机桌面的渐进式 Web 应用（PWA），用于实时监控 API 账户余额。

## ✨ 功能特性

- 📱 **移动端优化**：专为手机屏幕设计，简洁美观
- 🔄 **自动刷新**：可配置刷新间隔，实时掌握余额变化
- 🔐 **灵活认证**：支持 API Key Header 和 Bearer Token 两种认证方式
- 📝 **JSON 路径解析**：自定义字段路径，适配任意返回格式
- 💾 **离线缓存**：Service Worker 支持，断网也能查看上次余额
- ⚙️ **连接测试**：内置测试功能，帮助快速验证配置
- 🌐 **通用兼容**：支持任何返回 JSON 的余额查询 API

## 📁 文件结构

```
任务1/
├── index.html        # 主页面
├── app.js            # 核心逻辑（API调用、刷新、配置）
├── styles.css        # 样式（移动端适配）
├── manifest.json     # PWA 配置
├── service-worker.js # 离线缓存
└── README.md         # 本文件
```

## 🚀 快速开始

### 第一步：部署到 HTTPS 服务器

PWA 必须通过 HTTPS 访问（localhost 除外）。以下是几种推荐部署方式：

#### 方式 A：GitHub Pages（免费，推荐）

1. 在 GitHub 上创建一个新仓库
2. 将这 5 个文件上传到仓库
3. 进入仓库 Settings → Pages
4. Source 选择 `main` 分支，保存
5. 几分钟后访问 `https://你的用户名.github.io/仓库名/`

#### 方式 B：Netlify / Vercel（免费）

将文件夹拖拽上传到 [Netlify Drop](https://app.netlify.com/drop) 或 [Vercel](https://vercel.com) 即可。

#### 方式 C：本地测试

```bash
# 使用 Python 启动本地服务器
cd 任务1
python -m http.server 8080

# 或使用 Node.js
npx serve .
```

然后在浏览器访问 `http://localhost:8080`

### 第二步：手机访问并配置

1. 在手机 Chrome 浏览器中打开部署好的网址
2. 点击页面底部的 **⚙️ 设置** 按钮
3. 填写你的 API 配置信息：
   - **API 地址**：查询余额的完整 URL（例如 `https://api.example.com/v1/account/balance`）
   - **认证 Header 名称**：API Key 的 Header 字段名（例如 `X-API-Key` 或 `Authorization`）
   - **API Key / Token**：你的 API 密钥
   - **余额字段路径**：从返回 JSON 中提取余额的路径（见下方示例）
   - **货币单位**：显示在余额旁边（例如 `¥`、`$`、`USD`）
   - **刷新间隔**：自动刷新间隔秒数（建议 60 秒以上）
   - **Bearer Token 格式**：如果使用 `Authorization: Bearer xxx` 格式请勾选
4. 点击 **测试连接** 验证配置
5. 测试成功后点击 **保存配置**

### 第三步：添加到手机桌面

1. 在 Chrome 浏览器中，点击右上角菜单（三个点）
2. 选择 **添加到主屏幕** 或 **安装应用**
3. 确认名称后点击 **添加**
4. 现在你可以在手机桌面上看到一个独立的应用图标
5. 点击图标即可全屏查看余额，就像原生 App 一样！

## 📖 配置示例

### 示例 1：简单返回格式

假设 API 返回：
```json
{
    "balance": 128.50,
    "currency": "CNY"
}
```

配置：
- 余额字段路径：`balance`

### 示例 2：嵌套返回格式

假设 API 返回：
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "account": {
            "remaining": 2580.00
        }
    }
}
```

配置：
- 余额字段路径：`data.account.remaining`

### 示例 3：使用 Bearer Token

如果你的 API 需要 `Authorization: Bearer sk-xxxxxx` 格式：

配置：
- 认证 Header 名称：`Authorization`（实际会被忽略）
- 勾选 **使用 Bearer Token 格式**
- API Key：`sk-xxxxxx`

### 示例 4：OpenAI API

OpenAI 官方 API 当前没有公开的余额查询接口，但如果你使用代理或第三方服务：

配置：
- API 地址：`https://api.example.com/v1/dashboard/billing/credit_grants`
- 认证 Header 名称：`Authorization`
- 勾选 **使用 Bearer Token 格式**
- API Key：`sk-你的openai密钥`
- 余额字段路径：`total_available` 或 `total_granted`（取决于具体服务）

## 🔒 安全说明

⚠️ **重要**：API Key 存储在浏览器的 localStorage 中，仅保存在你自己的设备上。

- 请确保使用 HTTPS 部署，防止中间人攻击
- 不要在公共设备上使用此应用
- 如果手机丢失，建议立即撤销 API Key
- 建议使用只读权限的 API Key（如果 API 支持权限控制）

## 🔧 常见问题

**Q: 为什么添加到桌面后打不开？**
A: 确保部署使用了 HTTPS 协议。PWA 必须在 HTTPS 下才能正常工作。

**Q: 为什么余额不更新？**
A: 1. 检查网络连接；2. 确认 API Key 有效；3. 检查 JSON 路径配置是否正确（可以点击测试连接查看返回数据）。

**Q: 如何修改配置？**
A: 打开应用，点击底部的 **⚙️ 设置** 按钮即可重新配置。

**Q: CORS 跨域问题怎么办？**
A: 如果你的 API 不允许浏览器直接访问（没有 CORS 响应头），你需要：
1. 在 API 服务端配置允许跨域
2. 或使用后端代理转发请求
3. 或使用支持 CORS 的 API 代理服务

**Q: iOS 能使用吗？**
A: 可以！iOS Safari 同样支持 "添加到主屏幕"，但 PWA 的 Service Worker 缓存功能在较旧的 iOS 版本上支持有限。

## 🛠 技术栈

- 纯 HTML/CSS/JavaScript，无需编译
- Service Worker API（离线缓存）
- Web App Manifest（PWA 安装）
- Fetch API（HTTP 请求）
- localStorage（配置持久化）

## 📄 License

MIT - 自由使用、修改和分发
