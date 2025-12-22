# 注册邮件验证（CAP + 邮件验证码）文档索引

本目录文档用于实现“注册时邮件验证”功能，覆盖：
- 前台（用户注册）流程：CAP 校验后发送验证码邮件，输入验证码后允许注册。
- 后台（/admin）“邮件验证管理”页面：SMTP 配置、发送日志、IP 统计与封禁。

## 建议阅读顺序
1. `doc/captcha/development.md`：总体方案与端到端开发步骤（前台、后台、后端、DB）。
2. `doc/captcha/cap.md`：CAP 在本项目的接入方式（含 Standalone 部署与后端校验方式）。
3. `doc/captcha/security-performance.md`：安全与性能注意事项（必须项 + 推荐项）。
4. `doc/captcha/testing.md`：单元测试/集成测试/E2E（Playwright）测试方案与用例清单。

## 本目录文件说明
- `doc/captcha/development.md`：详细开发文档（功能拆分、数据结构、接口、关键逻辑、前端页面改造点）。
- `doc/captcha/cap.md`：CAP（PoW）接入指南（前端 widget、Standalone、与验证码发送接口的组合方式）。
- `doc/captcha/security-performance.md`：安全/性能注意事项与默认参数建议。
- `doc/captcha/testing.md`：应测试内容、测试环境建议、测试用例与自动化建议。

## 已确认的优化点（已写入文档）
- “验证码正确验证后才允许注册”：新增 `POST /api/auth/register/email-code/verify` 预验证接口与前端按钮 enable 逻辑。
- “验证码在日志中可查看但默认安全”：发送日志中验证码默认加密存储，列表脱敏，管理员按条解密查看并留审计。
- “额外限流阈值”：对 send/verify-code 等接口给出建议阈值与实现建议。
