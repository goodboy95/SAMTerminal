# LLM API 池模块

## 目标
- 支持多条 LLM API 配置（主/备），并在会话内固定使用。
- 提供负载控制、熔断、健康检查与 token 统计，保证可观测与稳定性。

## 关键能力
- **API 池管理**：管理员可新增/编辑/删除 API，设置角色、负载上限、token 限额与状态。
- **负载统计**：30 秒滑动窗口统计 `currentLoad`，用于选路与展示。
- **熔断机制**：连续失败达到阈值自动熔断，定时健康检查成功后恢复。
- **会话绑定**：新会话绑定单一 API，失败时切换，正常后不自动回切。

## 选路规则
1. 优先主 API，且 token 充足、`maxLoad-currentLoad` 最大。
2. 若主 API 均将耗尽，仍选主 API 中负载差值最大的。
3. 主 API 均不可用时再考虑备用 API。
4. token 已耗尽或熔断/禁用的 API 不参与选路。

## 配置项
- `app.llm.min-remaining-tokens`
- `app.llm.min-remaining-percent`
- `app.llm.session-timeout-minutes`
- `app.llm.request-timeout-seconds`
- `app.llm.circuit-breaker.failure-threshold`
- `app.llm.circuit-breaker.probe-interval-minutes`

## 安全
- API Key 仅脱敏返回，不打印到日志。
- baseUrl 仅允许 http/https，拦截内网/回环地址以防 SSRF。
