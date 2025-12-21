# ALTCHA 接入指南（用于“发送注册验证码邮件”前置校验）

本文件聚焦 ALTCHA（PoW）在本项目中的接入方式，目标是：**用户点击“发送验证邮件”按钮时，先通过 ALTCHA，再允许调用后端发送邮件接口**。

> 重要：ALTCHA “客户端验证”只能提升门槛，后端依然必须校验客户端提交的 ALTCHA payload，否则可被脚本绕过。

## 1. 推荐方案：自建 ALTCHA Sentinel + 后端代理

### 1.1 为什么推荐 Sentinel
本项目后端为 Java/Spring Boot；ALTCHA 的 challenge/verify 逻辑若自行在 Java 端复刻，维护与安全风险更高。推荐：
- 自建 Sentinel（官方参考实现，提供 challenge + verify API）
- 后端提供“代理接口”给前端 widget 使用，避免把 Sentinel 的 apiKey 暴露在浏览器

### 1.2 Sentinel 需要的接口能力（概念）
Sentinel 提供两类 API：
- Create Challenge：生成 challenge（widget 用）
- Verify Solution：验证 widget 产出的 payload

本项目建议后端增加 2 个无鉴权（但限流/封禁）的接口：
- `GET /api/captcha/altcha/challenge`：后端向 Sentinel 取 challenge 并返回给前端
- `POST /api/captcha/altcha/verify`：后端向 Sentinel 验证 payload 并返回给前端

并在真正的业务接口（发送邮件）再次验证：
- `POST /api/auth/register/email-code/send`：必须验证 `altchaPayload`

## 2. 前端：ALTCHA Widget 用法（React/Vite）

### 2.1 安装与引入
建议使用 npm 包引入 web component：
- 安装：`npm install altcha`
- 引入（任一全局入口，例如 `frontend/src/main.tsx` 或你封装的组件文件中）：`import 'altcha';`

### 2.2 基本组件形态
ALTCHA 是自定义元素 `<altcha-widget>`。建议做一个 React 封装组件：
- props：`challengeUrl`、`verifyUrl`、`onVerified(payload)`、`onError(err)`
- 通过监听 `statechange` 事件拿到验证状态（widget 文档强调：事件监听要在脚本加载之后绑定）

示例（伪代码，开发时请按项目实际 UI 组件封装）：
```ts
// 关键点：监听 statechange，拿到 verified 状态与 payload
widget.addEventListener('statechange', (ev) => {
  const detail = ev.detail; // 包含 state 与 payload 等信息
});
```

### 2.3 与“发送验证邮件”按钮的组合
推荐交互：
1) 用户点击“发送验证邮件”
2) 展示 ALTCHA widget（可 inline、弹窗、overlay）
3) widget 验证成功后，前端拿到 `altchaPayload`
4) 调用后端 `POST /api/auth/register/email-code/send`（提交 `altchaPayload + email + username`）

> 不建议：仅依赖 widget 调用 `verifyurl` 的结果就直接发送邮件（仍可被脚本跳过前端）。

## 3. 后端：ALTCHA 代理接口设计

### 3.1 Challenge 代理：`GET /api/captcha/altcha/challenge`
职责：
- 获取请求来源 IP（用于封禁/限流/日志）
- 访问 Sentinel 的 challenge API，拿到 challenge JSON 原样返回给前端
- 增加响应头防缓存（避免复用 challenge）

### 3.2 Verify 代理：`POST /api/captcha/altcha/verify`
职责：
- 获取请求来源 IP（用于封禁/限流/日志）
- 接收前端传来的 `payload`（或 `altchaPayload`）
- 调用 Sentinel verify API，返回 `{ verified: true/false }`

### 3.3 在业务接口中再次验证（强制）
`POST /api/auth/register/email-code/send` 内：
- 必须调用 `AltchaService.verify(payload, ip)`，失败返回 `400/401`
- 通过后才进入“生成验证码 + 发送邮件”逻辑

## 4. ALTCHA 参数与安全建议（最小可用）
在本项目的用途是“发送邮件前置校验”，建议：
- PoW 难度不必过高，避免影响移动端/低端设备体验（并在 `security-performance.md` 给出调参建议）
- 对 `challenge` 与 `verify` 接口做 IP 限流（例如每 IP 每分钟上限）
- 结合本功能的 IP 统计与封禁：被封禁 IP 不应再获取 challenge/verify

## 5. Sentinel 部署建议（可选项）
若决定接入 Sentinel，建议作为独立容器加入 `docker-compose.yml`，并在后端通过内网访问。

需要的配置项（示例名）：
- `ALTCHA_SENTINEL_BASE_URL`
- `ALTCHA_SENTINEL_API_KEY`（仅后端持有）

> 若暂不引入 Sentinel：也可先实现“后端随机 challenge + 自验证 PoW”的简化版，但要在 `doc/issues.md` 记录风险与后续替换计划。

