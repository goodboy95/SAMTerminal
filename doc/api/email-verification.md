# EmailVerificationController 接口

## POST /api/auth/register/email-code/send
- 描述：发送注册验证码邮件（CAP 校验后入队发送）。
- 请求体：`{ username: string, email: string, capToken: string }`
- 响应：`{ requestId: string, resendAvailableAt: string, expiresAt: string, sendStatus: "PENDING" }`
- 错误：
  - 400：CAP 失败 / 参数错误 / 邮箱不支持
  - 403：IP 被封禁
  - 429：重发间隔未到（返回 `resendAvailableAt`）或触发频控
  - 503：无可用 SMTP

## POST /api/auth/register/email-code/verify
- 描述：预验证验证码，成功后允许注册。
- 请求体：`{ emailRequestId: string, email: string, emailCode: string }`
- 响应：`{ verified: boolean, expiresAt: string, attemptsRemaining: number }`
- 错误：
  - 400：验证码不存在/不匹配/已过期/已失效
  - 429：触发频控

## GET /api/auth/register/email-code/send-status
- 描述：查询异步发送任务状态。
- 查询参数：`requestId`
- 响应：`{ status: "PENDING"|"SENT"|"FAILED", lastError?: string }`
- 错误：404：发送记录不存在
