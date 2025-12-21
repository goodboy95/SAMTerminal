# AdminEmailVerificationController 接口

## SMTP 配置
- **GET /api/admin/email-verification/smtp**：获取 SMTP 列表。
- **POST /api/admin/email-verification/smtp**：新增 SMTP。
  - 请求体：`{ name?, host, port, username?, password?, fromAddress, useTls?, useSsl?, enabled?, maxPerMinute?, maxPerDay? }`
- **PUT /api/admin/email-verification/smtp/{id}**：更新 SMTP（password 留空表示不变）。
- **DELETE /api/admin/email-verification/smtp/{id}**：删除 SMTP。
- **POST /api/admin/email-verification/smtp/{id}/test**：测试发送。
  - 请求体：`{ toEmail }`

## 发送日志
- **GET /api/admin/email-verification/logs?start=YYYY-MM-DD&end=YYYY-MM-DD&page=0&size=20&sort=sentAt,desc**
  - 返回：`{ items, total, page, size }`
  - items 字段：`id, username, ip, email, codeMasked, sentAt, smtpId, status`
- **POST /api/admin/email-verification/logs/{id}/decrypt**：解密查看验证码（记录审计日志）。
  - 返回：`{ code }`

## IP 统计与封禁
- **GET /api/admin/email-verification/ip-stats?date=YYYY-MM-DD&page=0&size=20&sortField=unverifiedToday&sortDir=desc**
  - 返回：`{ items, total, page, size }`
  - items 字段：`ip, requestedToday, unverifiedToday, requestedTotal, unverifiedTotal, banStatus, bannedUntil`
- **POST /api/admin/email-verification/ip-bans**：手动封禁。
  - 请求体：`{ ip, bannedUntil, reason? }`
- **DELETE /api/admin/email-verification/ip-bans/{ip}**：解封手动封禁。
