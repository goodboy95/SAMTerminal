# 已知问题记录

## 1. Playwright MCP 仍无法连接
- 现象：调用 `mcp__playwright__browser_*` 系列工具仍返回 `Transport closed`，无法启动浏览器。
- 影响：无法按要求执行 Playwright 系统测试（注册、SMTP、后台等）。
- 建议处理：请确认 Playwright MCP 服务已启动并可被当前会话访问；必要时重启相关 MCP 服务后再试。
