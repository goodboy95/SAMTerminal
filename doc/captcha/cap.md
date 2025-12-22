# CAP 接入指南（用于“发送注册验证码邮件”前置校验）

本文件聚焦 CAP（PoW）在本项目中的接入方式，目标是：**用户点击“发送验证邮件”按钮时，先通过 CAP，再允许调用后端发送邮件接口**。

> 重要：CAP 的客户端验证只提升门槛，后端必须再使用 `siteverify` 校验客户端 token，否则可被脚本绕过。

## 1. 推荐方案：CAP Standalone + 前端直连 + 后端二次校验

### 1.1 为什么选 Standalone
CAP Standalone 提供完整的 challenge/verify 与 token 存储能力，且官方建议直接使用 Standalone。前端只需设置 `data-cap-api-endpoint`，后端通过 `/{key_id}/siteverify` 做二次校验即可。

### 1.2 本项目使用方式
- **前端**：使用 `@cap.js/widget`，`data-cap-api-endpoint` 指向 Standalone 的 `/{site_key}/` 路径。
- **后端**：在发送邮件前调用 Standalone 的 `POST /{site_key}/siteverify` 校验 token。
- **部署**：Standalone 以容器方式运行；前端通过 Nginx `/cap/` 反代到 Standalone，避免额外跨域配置。

## 2. 前端：CAP Widget 用法（React/Vite）

### 2.1 安装与引入
- 安装：`pnpm add @cap.js/widget`
- 引入：在组件文件中 `import '@cap.js/widget';`

### 2.2 基本组件形态
CAP 是自定义元素 `<cap-widget>`。建议封装 React 组件：
- props：`apiEndpoint`、`onSolved(token)`、`onError(err)`
- 监听 `solve` 事件拿到 `token`

示例（伪代码）：
```ts
capEl.addEventListener('solve', (ev) => {
  const token = ev.detail?.token;
});
```

### 2.3 与“发送验证邮件”按钮的组合
推荐交互：
1) 用户点击“发送验证邮件”
2) 展示 CAP widget（inline / 弹窗）
3) CAP 完成后得到 `capToken`
4) 调用 `POST /api/auth/register/email-code/send`（提交 `capToken + email + username`）

## 3. 后端：CAP 二次校验

### 3.1 校验接口
- 使用 Standalone 的 `POST /{site_key}/siteverify`
- 请求体：`{ "secret": "<site_secret>", "response": "<token>" }`
- 响应：`{ "success": true/false }`

### 3.2 发送验证码接口必须校验
`POST /api/auth/register/email-code/send` 内：
- 必须调用 `CapService.verifyToken(token, ip)`，失败返回 400

## 4. CAP 参数与安全建议（最小可用）
- 前端只负责获取 token；后端必须校验。
- 建议保持 `send` 接口的频控与 IP 统计/封禁。
- Standalone 需要创建 `site_key` 与 `site_secret`，由后端持有 `site_secret`。

## 5. Standalone 部署建议
- 推荐镜像：`tiago2/cap:latest`
- 启动后可访问管理界面创建站点 key/secret（必要时参考 Standalone Swagger API 自动化创建）。
- 若使用 Nginx 反代，建议配置 `/cap/` 反向代理到 Standalone 根路径。

需要的配置项（示例名）：
- `CAP_BASE_URL`（如 `http://cap:3000`）
- `CAP_SITE_KEY`
- `CAP_SITE_SECRET`
- `CAP_TEST_MODE`（测试环境可为 true）
