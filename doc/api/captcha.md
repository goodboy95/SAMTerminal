# CaptchaController 接口

## GET /api/captcha/altcha/challenge
- 描述：获取 ALTCHA challenge（Sentinel 代理）。
- 请求：无
- 响应：Sentinel challenge JSON（透传），或 `{ testMode: true }`（测试模式）。
- 错误：
  - 429：触发限流
  - 403：IP 被封禁
  - 503：ALTCHA 未配置或服务不可用

## POST /api/captcha/altcha/verify
- 描述：验证 ALTCHA payload（Sentinel 代理）。
- 请求体：`{ payload: string }`
- 响应：`{ verified: boolean }`
- 错误：
  - 429：触发限流
  - 403：IP 被封禁
