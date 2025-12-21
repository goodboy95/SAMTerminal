# 注册邮件验证：测试内容与方案

更新时间：2025-12-21

## 1. 测试范围
本功能新增/改动点覆盖：
- 用户侧：注册页 UI + ALTCHA + 发送验证码 + 注册校验
- 后端：ALTCHA 校验、验证码生成/过期/重发间隔、SMTP 池与熔断、发送日志、IP 统计与封禁、管理端接口
- 管理端：SMTP 配置页、日志查询页、IP 统计与封禁操作

## 2. 测试环境建议

### 2.1 SMTP 测试服务
建议在本地/CI 使用“假 SMTP 服务”：
- 方案 A：MailHog（容器化，简单直观，能在 Web UI 查看邮件）
- 方案 B：GreenMail（Java 测试库，适合单元/集成测试）

目的：
- 发送真实邮件不稳定、且会造成误发
- 方便断言“邮件是否发送/内容是否正确/是否切换 SMTP”

### 2.2 时间相关测试
验证码有效期 5 分钟、重发间隔 1 分钟、跨日解封：
- 后端建议对时间获取做抽象（例如 `Clock` 注入），便于测试中“快进时间”

## 3. 后端单元测试（建议新增）

### 3.1 EmailVerificationService
用例：
- 首次发送成功：生成 requestId、写入过期时间（now+5m）、写入可重发时间（now+1m）
- 1 分钟内重发：返回 429，并返回 `resendAvailableAt`
- 过期后校验：返回“过期”
- 重发生成不同验证码：同 requestId 不复用；旧验证码应失效（或标记 superseded）
- 校验成功后不可重复使用：二次校验失败
- 预验证接口：verify 成功后 request 状态切换为 `VERIFIED_PENDING_REGISTER`（注册后消费为 `USED`）

### 3.2 SmtpPoolService（随机 + 熔断 + 超限）
用例：
- 多 SMTP 可用时：随机选择（可通过固定随机种子/注入 Random 便于可测）
- 某 SMTP 发送失败：标记熔断，自动换另一个发送成功
- 全部 SMTP 失败：返回“无可用 SMTP”
- 超限：达到分钟/日限额后不再被选中，直到窗口恢复/跨日恢复

### 3.3 EmailIpStatsService + EmailIpBanService
用例：
- 发送一次：today requested+1、today unverified+1、total requested+1、total unverified+1
- 验证一次：today/total unverified -1（不允许减到负数）
- unverifiedToday 从 50 → 51：触发 AUTO ban
- AUTO ban 后 unverifiedToday 从 51 → 50：自动解封
- 跨日：AUTO ban 次日自动解封（定时任务）
- MANUAL ban：不因 unverified 下降自动解封

## 4. 后端集成测试（建议）
用 H2 或 Testcontainers（MySQL）覆盖：
- `POST /api/auth/register/email-code/send`：
  - ALTCHA 失败 → 400
  - 触发封禁 → 403
  - SMTP 全不可用 → 503
  - 正常发送 → 200，返回 requestId + 时间字段
- `POST /api/auth/register/email-code/verify`：
  - requestId 不存在/不匹配邮箱 → 400
  - code 错误/过期 → 400
  - code 正确 → 200，`verified=true`
- `POST /api/auth/register`：
  - requestId 不存在/不匹配邮箱 → 400
  - code 错误/过期 → 400
  - code 正确 → 注册成功 + 返回 token
- 管理端接口：
  - SMTP CRUD
  - logs 按时间范围分页、按时间排序
  - logs 解密查看：列表仅返回 `codeMasked`；调用 `/logs/{id}/decrypt` 返回明文，并写入审计记录
  - ip-stats 默认排序与自定义排序
  - manual ban/unban 行为
 - 限流：
   - `/api/captcha/altcha/challenge`、`/api/captcha/altcha/verify`、`/api/auth/register/email-code/send`、`/api/auth/register/email-code/verify` 超阈值返回 429

## 5. 前端功能测试（手动 + E2E）

### 5.1 用户侧（手动）
场景：
- ALTCHA 未完成：点击“发送验证邮件”不能调用后端（或后端返回 altcha invalid）
- ALTCHA 完成：成功发送邮件，按钮进入 60s 倒计时
- 60s 内重复点击：提示“重发间隔未到”
- “验证验证码”动作：输入验证码后验证成功才允许点击“提交档案”
- 输入错误验证码：注册失败提示
- 输入正确验证码：注册成功并自动登录（现有行为）
- 过期验证码（>5m）：注册失败提示“过期”（或统一错误文案）

### 5.2 管理端（手动）
场景：
- SMTP 配置：新增 2 条 SMTP，停用其中 1 条，验证发送时只会选可用的
- SMTP 熔断：故意配置 1 条错误密码，触发失败后自动切换另一条；错误那条应显示熔断状态
- 日志：选择时间范围，分页翻页，按时间正/倒序
- 日志解密查看：列表中验证码为脱敏显示；点击“解密查看”后才显示明文（并提示会产生审计记录）
- IP 统计：默认按“当天未验证”倒序；切换排序字段与方向
- 手动封禁：设置解封时间；封禁后用户侧发送接口返回 403；到期自动解封

### 5.3 Playwright（系统测试）
建议新增 E2E 脚本覆盖：
- 注册页：完成 ALTCHA（可通过测试模式/Mock）→ 触发发送 → 输入验证码 → 注册成功
- 管理端：登录 → 进入“邮件验证管理”→ 检查 SMTP/日志/IP 统计页面可用、排序/分页/封禁动作可用

> 若 ALTCHA 无法在 E2E 稳定完成：建议为测试环境提供“ALTCHA test 模式”或后端 mock 开关（仅测试环境启用），并在文档/配置中明确禁止生产开启。

执行时的访问入口提醒：
- 前端：`http://samproject.seekerhut.com:8090`
- 后端：`http://samproject.seekerhut.com:8081`
