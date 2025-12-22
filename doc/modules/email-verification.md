# 邮件验证码模块说明

更新时间：2025-12-21

## 模块职责
- 用户注册时发送/校验邮箱验证码（5 分钟有效、1 分钟重发）。
- CAP 防刷校验（前端 widget + 后端二次校验）。
- SMTP 多配置随机发送、失败熔断与限额控制。
- 发送日志记录、验证码加密存储与审计解密。
- IP 统计与自动/手动封禁。

## 关键流程
1. 前端完成 CAP 验证后调用 `POST /api/auth/register/email-code/send`。
2. 后端校验 CAP + 域名策略 + 频控，生成验证码并入队发送。
3. 用户输入验证码后调用 `POST /api/auth/register/email-code/verify` 预验证。
4. 注册 `POST /api/auth/register` 时消费验证码（状态变为 USED）。

## 数据结构
- `email_verification_request`：验证码请求（hash 校验、状态与过期时间）。
- `email_send_task`：异步发送任务队列表。
- `email_send_log`：发送日志（验证码 AES-GCM 加密存储）。
- `email_ip_stats_daily/total`：IP 统计与风控计数。
- `email_ip_ban`：IP 封禁记录（AUTO/MANUAL）。
- `email_smtp_config`：SMTP 服务池配置。
- `email_send_log_audit`：日志解密审计。

## 配置项
- `email.code-ttl`：验证码有效期（默认 5m）。
- `email.resend-interval`：重发间隔（默认 1m）。
- `email.rate-limit.*`：发送/校验的限流阈值。
- `email.cap.*`：CAP Standalone 地址、站点 Key/Secret、测试模式。
- `email.domain-policy.*`：域名 allow/deny/disposable 过滤。
- `email.smtp.*`：SMTP 熔断阈值与超时。
- `email.send-task.*`：异步发送任务的批次、重试与退避。
- `email.encryption.*`：验证码/SMTP 密码加密密钥与哈希盐。

## 注意事项
- 生产环境应关闭 `CAP_TEST_MODE` 并配置站点 Key/Secret。
- 需配置 SMTP 才能真实发送，可用 MailHog 做本地测试。
