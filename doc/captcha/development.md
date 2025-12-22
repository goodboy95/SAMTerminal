# 注册邮件验证功能：详细开发文档

更新时间：2025-12-21  
适用范围：本仓库 `frontend/`（React18+Vite）与 `backend/`（Spring Boot 3）

## 1. 目标与约束（需求对齐）

### 1.1 前台（用户侧）
注册页增加：
- 邮件验证码输入框（右侧按钮“发送验证邮件”）。
- 点击“发送验证邮件”时：**先做客户端 CAP 验证**；通过后才调用后端发送验证码邮件。
- 验证码规则：
  - 有效期 **5 分钟**。
  - 重发间隔 **1 分钟**。
  - 每次重发都生成 **不同**验证码。
- 用户输入验证码后，**验证码正确才允许注册**（可在“提交档案”时验证，也可先单独验证后再允许提交，见 4.2）。

### 1.2 后台（管理员侧 /admin）
新增“邮件验证管理”Tab，包含：
1) **SMTP 服务配置**
- 支持配置多个 SMTP 服务。
- 发送邮件时随机选取一个可用服务（未熔断、未超限）。
- 某个服务发送失败：标记为熔断，自动随机选取另一个服务重试。
- 全部服务不可用：返回错误给用户。

2) **邮件发送日志**
- 日历组件选择起止时间范围。
- 分页查看该时间段内日志。
- 日志字段：待注册用户名、来源 IP、邮箱、验证码、发送时间。
- 允许按发送时间排序。

3) **IP 统计与封禁**
- 统计每个 IP 请求验证码：
  - 当天请求总数
  - 当天未验证验证码数
  - 累计请求总数
  - 累计未验证验证码数
  - + IP 字段（合计 5 字段）
- 默认按“当天未验证验证码数”倒序。
- 允许管理员选择任意统计字段正序/倒序排序。
- 自动封禁规则：
  - 若某天某 IP 的“未验证验证码” **超过 50**：自动临时封禁；
  - **第二天自动解封**；
  - 若自动封禁后，该 IP 后续完成验证导致未验证数降到 50 以下：**自动解封**；
  - **手动封禁**不因未验证数下降而自动解封（仅到达手动设置的自动解封时间或管理员手动解封）。
- 管理员可手动封禁/解封 IP：封禁需可选择自动解封时间。

## 2. 现状与改造点（结合本项目）

### 2.1 现有注册入口
- 前端注册页：`frontend/src/pages/Index.tsx`（Tab “注册识别码”）。
- 前端 API：`frontend/src/lib/api.ts` 调用 `POST /api/auth/register`。
- 后端注册接口：`backend/src/main/java/com/samterminal/backend/controller/AuthController.java` 的 `POST /api/auth/register`，实际逻辑在 `AuthService.register()`。
- 后端端口：默认 `http://samproject.seekerhut.com:8081`（前端 `VITE_API_BASE` 默认值）。
- 站点访问：浏览器通过 `http://samproject.seekerhut.com:8090` 访问前端（hosts 指向 127.0.0.1）。

### 2.2 需要新增的后端能力（建议拆分）
建议新增三个相对独立的后端子系统：
1) **CAP 校验**：为“发送验证码邮件”提供反滥用门槛（见 `doc/captcha/cap.md`）。
2) **邮件验证码**：生成、发送、校验、过期/重发/限频。
3) **管理端**：SMTP 配置、日志查询、IP 统计与封禁。

## 3. 数据与表结构（概要）
详细建议见 `doc/captcha/cap.md`（CAP 相关）与 `doc/captcha/security-performance.md`（敏感字段处理）与 `doc/captcha/testing.md`（测试数据准备）。

建议新增/调整的表（MySQL，最终需同步到 `sql/schema.sql`）：
- `email_smtp_config`：SMTP 服务配置（多条）。
- `email_verification_request`：注册验证码请求记录（用于校验/过期/重发间隔）。
- `email_send_log`：发送日志（可按时间范围分页查询）。
- `email_ip_stats_total`、`email_ip_stats_daily`：IP 统计汇总（避免每次管理端查询做大表聚合）。
- `email_ip_ban`：IP 封禁记录（区分 AUTO/MANUAL）。

## 4. 接口设计（建议）

### 4.1 用户侧接口（建议新增）
为保持与现有 `AuthController` 一致，建议放在 `/api/auth` 下：

1) **发送验证码邮件**
- `POST /api/auth/register/email-code/send`
- 请求体（建议）：
  - `username: string`
  - `email: string`
  - `capToken: string`（CAP widget 的 token；后端必须再验一次）
- 返回（建议）：
  - `requestId: string`（UUID，用于后续注册时绑定该次发送）
  - `resendAvailableAt: string`（ISO 时间；前端用于倒计时）
  - `expiresAt: string`（ISO 时间；前端用于提示）
- 错误（建议）：
  - `429`：重发间隔未到 / 触发频控
  - `403`：IP 被封禁（AUTO 或 MANUAL）
  - `400`：CAP 校验失败 / 参数错误
  - `503`：无可用 SMTP

2) **注册（增强校验）**
- 继续使用：`POST /api/auth/register`
- 请求体新增（建议）：
  - `emailCode: string`
  - `emailRequestId: string`
- 后端在创建用户前必须验证：
  - `emailRequestId` 存在、未过期、属于该 `email + username + ip`（至少绑定 email；是否强绑 username 由产品决定）。
  - `emailCode` 正确且未使用。
  - 通过后立刻将该 request 标记为 `VERIFIED/USED`，避免并发重复注册复用同一验证码。

3) **验证码预验证（用于“验证通过后才允许注册”）**
- `POST /api/auth/register/email-code/verify`
- 请求体（建议）：`{ emailRequestId, email, emailCode }`
- 返回（建议）：`{ verified: boolean, expiresAt, attemptsRemaining? }`
- 说明：
  - 该接口只做“校验是否正确/是否过期/是否匹配”，不创建用户；
  - 为避免“先 verify 再 register”导致重复校验窗口问题，建议 verify 成功后将 request 标记为 `VERIFIED_PENDING_REGISTER`（仍可设置短 TTL），注册接口只接受该状态并消费为 `USED`。

### 4.2 管理员侧接口（建议新增）
建议统一挂在 `/api/admin/email-verification/*`：

1) SMTP 配置
- `GET /api/admin/email-verification/smtp`：列表
- `POST /api/admin/email-verification/smtp`：新增
- `PUT /api/admin/email-verification/smtp/{id}`：更新
- `DELETE /api/admin/email-verification/smtp/{id}`：删除
- （可选）`POST /api/admin/email-verification/smtp/{id}/test`：测试发送

2) 发送日志
- `GET /api/admin/email-verification/logs?start=...&end=...&page=...&size=...&sort=sentAt,desc`
- 返回：分页结构（items + total + page/size）
- items 字段（建议默认不返回明文验证码）：`id, username, ip, email, codeMasked, sentAt, smtpId, status`
- **验证码解密查看（按需）**
  - `POST /api/admin/email-verification/logs/{id}/decrypt`
  - 返回：`{ code }`
  - 要求：
    - 仅 ADMIN 可调用；
    - 需要记录审计日志（谁在什么时间解密查看了哪条记录）。

3) IP 统计
- `GET /api/admin/email-verification/ip-stats?date=YYYY-MM-DD&page=...&size=...&sortField=unverifiedToday&sortDir=desc`
- 返回字段（每行）：
  - `ip`
  - `requestedToday`
  - `unverifiedToday`
  - `requestedTotal`
  - `unverifiedTotal`
  - （建议额外返回）`banStatus`（NONE/AUTO/MANUAL）、`bannedUntil`

4) IP 封禁管理
- `POST /api/admin/email-verification/ip-bans`：手动封禁
  - 请求体：`{ ip, bannedUntil, reason? }`
- `DELETE /api/admin/email-verification/ip-bans/{ip}`：解封（仅解 MANUAL；AUTO 由系统自动处理）
- （可选）`GET /api/admin/email-verification/ip-bans`：查看封禁列表

## 5. 后端实现建议（Spring Boot 3）

### 5.1 关键服务拆分（建议类名）
- `CapService`：校验 capToken（见 `doc/captcha/cap.md`）。
- `EmailVerificationService`：
  - `sendRegisterCode(username, email, ip, capToken)`
  - `verifyForRegister(requestId, email, code, ip)`
- `SmtpPoolService`：
  - 随机选择可用 SMTP
  - 失败熔断、超限判定
  - 全失败时返回“无可用 SMTP”
- `EmailSendLogService`：写入发送日志、提供管理端查询
- `EmailIpStatsService`：
  - 发送时：增加 requested/unverified（daily + total）
  - 验证成功时：减少 unverified（daily + total）
  - 触发/解除 AUTO ban
- `EmailIpBanService`：
  - `isBanned(ip)`（MANUAL 优先）
  - `banAutoIfNeeded(ip, dateStats)`
  - `unbanAutoIfRecovered(ip, dateStats)`
  - 定时任务：跨日清理 AUTO（建议每日 00:05 执行）

### 5.2 自动封禁的判定时机（建议）
为满足“超过 50 立刻封禁 / 降回 50 以下自动解封”：
- 在“发送验证码邮件”接口中：
  - 先检查是否已封禁（MANUAL/AUTO）
  - 更新 `requested/unverified` 后，若 `unverifiedToday > 50` → 写入/更新 AUTO ban（`bannedUntil = 次日 00:00:00 + 安全余量`）
- 在“验证码验证成功”逻辑中：
  - 将对应 request 标记 VERIFIED/USED
  - 对 IP 的 `unverifiedToday` 进行 `-1`
  - 若存在 AUTO ban 且 `unverifiedToday <= 50` → 自动解封

### 5.3 发送验证码的幂等与并发（建议）
并发风险点：同一 IP / 同一邮箱多次点“发送验证邮件”。
- DB 层建议：
  - `email_verification_request` 记录 `resendAvailableAt`，发送前 `SELECT ... FOR UPDATE` 锁定该邮箱（或邮箱+ip）。
  - 对“重发间隔未到”直接返回 429，并把可重发时间回给前端。
  - 每次重发生成新 code，并使旧 code 失效（建议旧记录标记 `SUPERSEDED`）。

## 6. 前端实现建议（React）

### 6.1 用户注册页（`frontend/src/pages/Index.tsx`）
建议交互：
- 在“通讯频段 (Email)”和“设置密钥”之间插入“邮箱验证码”输入区：
  - 左侧：验证码输入框
  - 右侧：`发送验证邮件` 按钮（带倒计时：60s）
- 点击“发送验证邮件”：
  1) 弹出或展示 CAP widget（见 `doc/captcha/cap.md`）。
  2) CAP 验证成功后，调用 `POST /api/auth/register/email-code/send`。
  3) 成功后开始倒计时，并把 `emailRequestId` 缓存到 state（后续注册时提交）。
- “提交档案”（注册）按钮的 enable 条件（按本需求优化后）：
  - 必须已获取 `emailRequestId`；
  - 用户输入 `emailCode` 后先调用 `POST /api/auth/register/email-code/verify`，`verified=true` 才允许点击“提交档案”；
  - 若 verify 失败或过期：提示用户重新发送或重新输入。

### 6.2 管理端新增 Tab（`frontend/src/pages/admin/*`）
在 `frontend/src/pages/admin/AdminLayout.tsx` 的 `navItems` 增加：
- `邮件验证管理`
并新增页面组件（建议文件）：
- `frontend/src/pages/admin/EmailVerificationManager.tsx`
页面包含 3 个子块（可用 Tabs 或 Accordion）：
1) SMTP 配置（增删改/启用停用/限额/熔断状态）
2) 发送日志（日期范围 + 分页表格 + 时间排序 + “解密查看验证码”操作）
3) IP 统计（默认按“当天未验证”倒序，可切换排序字段/方向；支持手动封禁/解封）

## 7. 开发落地步骤（建议）
1) DB：先落表（含索引）+ 更新 `sql/schema.sql`
2) 后端：实现“发送验证码 + 注册校验 + IP 统计/封禁”最小闭环
3) 后端：实现 SMTP 池（随机 + 熔断 + 超限）与发送日志查询
4) 前端：注册页接入 CAP + 发送按钮 + 注册请求携带验证码字段
5) 前端：/admin 新增“邮件验证管理”页并对接接口
6) 测试：补齐单元/集成测试；E2E 用例见 `doc/captcha/testing.md`

## 8. 两项优化点：分析与落地方案（建议纳入一期）

### 8.1 异步发送（队列）+ “待发送”UX（避免 SMTP 延迟阻塞 API）

#### 背景问题
SMTP 发送链路的常见问题：
- 网络抖动/握手慢导致接口 RT 飙升，用户侧“发送验证邮件”按钮卡住；
- SMTP 服务偶发超时会放大“熔断 + 换池重试”的尾延迟；
- 若未来接入真实邮箱服务商，限流/灰度策略也更适合放在后台任务里。

#### 目标
将 `POST /api/auth/register/email-code/send` 的“生成验证码 + 入队”变成快速路径（几十毫秒级），把“实际发信”放到后台异步 worker 执行，并在前端提供明确的“发送中”状态展示。

#### 推荐实现（不引入外部 MQ，使用 DB Outbox/Task 表）
在当前项目已有 MySQL 的前提下，优先采用 DB 队列（可水平扩展，重启不丢任务）：
1) 新增表 `email_send_task`（或复用 `email_send_log` 扩展状态也可以，但推荐单独 task 表）：
   - `id`、`requestId`、`email`、`username`、`ip`
   - `payload`（如需记录模板参数；注意敏感字段）
   - `status`：`PENDING|SENDING|SENT|FAILED|CANCELED`
   - `attemptCount`、`nextAttemptAt`、`lastError`
   - `createdAt/updatedAt`
2) `POST /api/auth/register/email-code/send` 流程调整：
   - 仍先做：IP 封禁检查 → CAP 后端校验 → 邮箱/域名过滤（见 8.2）→ 重发间隔检查
   - 生成验证码与 `email_verification_request` 记录（用于后续校验/过期/重发）
   - 写入 `email_send_task(PENDING, nextAttemptAt=now)` 与 `email_send_log(status=PENDING, codeEncrypted=...)`
   - **立即返回**：`{ requestId, resendAvailableAt, expiresAt, sendStatus: "PENDING" }`
3) 后台 worker（Spring）：
   - `@Scheduled(fixedDelay=...)` 轮询 `PENDING/FAILED(nextAttemptAt<=now)` 任务并 `SELECT ... FOR UPDATE SKIP LOCKED`（或等价方案）抢占
   - 调用 `SmtpPoolService`：随机可用 SMTP → 失败熔断 → 自动换池重试（单任务内建议最多尝试 3 个不同 SMTP，避免尾延迟过长）
   - 发送成功：task→`SENT`，log→`SENT`
   - 发送失败：task→`FAILED` 并设置 `nextAttemptAt`（指数退避 + 抖动），log→`FAILED`（保留失败原因供管理员排查）
4) 用户侧“待发送”UX（建议）：
   - 点击发送后立即 toast：`已提交发送请求，正在投递...`
   - 按钮进入 60s 重发倒计时（仍以服务端 `resendAvailableAt` 为准）
   - 可选：提供轻量状态查询接口 `GET /api/auth/register/email-code/send-status?requestId=...`，返回 `PENDING|SENT|FAILED`，前端每 2~3 秒轮询，直到 `SENT/FAILED` 或超时（避免用户误以为没发出）

#### 边界与一致性
- 需求“每次重发产生不同验证码”：依旧成立；重发时创建新的 `email_verification_request` 与新的 `email_send_task`，并将旧 request 标记 `SUPERSEDED`。
- 若上一条仍 `PENDING` 且重发间隔未到：建议返回 429 + `resendAvailableAt`；不要创建新任务，避免队列膨胀。
- 邮件最终一致性：API 返回成功≠一定送达；因此推荐增加 sendStatus 查询与管理端日志状态展示。

### 8.2 发送时增加邮箱域名 allow/deny + 一次性邮箱（disposable）过滤

#### 背景问题
注册验证码邮件是“资源消耗型动作”（SMTP 配额、队列、日志、风控），常见滥用来源：
- 一次性邮箱/临时邮箱批量请求验证码，导致 SMTP 配额被耗尽；
- 特定域名（或企业域名）需要强制允许/禁止。

#### 目标
在 `POST /api/auth/register/email-code/send` **入队/发信前**做邮箱策略判定：
- 支持域名 allowlist/denylist（策略可配置）
- 拦截 disposable email 域名（本地列表或可配置）
- 将“过滤命中”记入审计/日志（便于风控回溯），但对外错误信息保持克制，避免被用作探测信号

#### 推荐实现
1) 域名解析规范化（必须）：
   - `email` lower-case，提取 `@` 后的 domain
   - 去除首尾空白，拒绝非法格式
   - 统一处理 IDN（如引入 punycode 规范化）后再比较
2) 策略优先级（建议）：
   - 先判断 `denylist`：命中则拒绝
   - 若配置了 `allowlist`（非空）：不在 allowlist 中则拒绝
   - 再判断 disposable：命中则拒绝
3) 配置形态（建议二选一）：
   - **A. 管理端可配置（推荐）**：新增后台“域名策略”子面板与表 `email_domain_policy`：
     - 字段：`domain`、`type(ALLOW|DENY)`、`enabled`、`note`、`createdAt`
   - **B. 配置文件/环境变量（快速）**：
     - `EMAIL_DOMAIN_ALLOWLIST=example.com,corp.com`
     - `EMAIL_DOMAIN_DENYLIST=bad.com,spam.net`
4) disposable 列表来源（建议）：
   - 在仓库维护一份 `disposable-email-domains.txt`（按行一个域名），随版本更新
   - 后端启动时加载为 HashSet（O(1) 查询）
5) 返回码与文案（建议）：
   - HTTP：`400` 或 `422`
   - message：`邮箱不支持，请更换邮箱`（避免暴露“该域名被禁用/被识别为一次性邮箱”等细节）
6) 与 IP 统计/封禁的关系（建议）：
   - 过滤在“计数入库/入队”之前执行，避免把无效请求也计入未验证数量
   - 但可单独记录一条风控日志（可选），用于观察攻击面（不计入“验证码请求统计”）
