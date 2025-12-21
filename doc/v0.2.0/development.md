# V0.2.0 详细开发文档

## 1. 目标与范围
本版本聚焦于「大模型 API 池、负载与熔断、会话绑定、管理员配置」以及「前端图片复用」能力，确保在并发、资源成本和稳定性方面具备可控、可观测、可运维的能力。

## 2. 现状分析（基于当前代码）

### 2.1 后端
- LLM 连接配置仅支持单条 `LlmSetting`，`GameService` 与 `Memory` 回忆调用固定使用第一条配置。
- Admin 账号在 `DataInitializer` 中硬编码创建（admin/admin123），不可配置多个管理员。
- Token 统计为「用户维度」的 `UserTokenUsage`，未覆盖「API 维度」。
- `LlmService` 采用 RestTemplate 直接调用 `/v1/chat/completions`，没有 API 级负载控制、熔断或轮询策略。

### 2.2 前端
- `GamePage` 会在状态与资源变更时使用 `new Image()` 进行预加载，但未做 URL 去重。
- 管理后台「模型连接设置」页面仅支持单条模型配置。

## 3. 设计目标与原则
1. **可扩展**：支持多条 API 配置（主/备）、动态维护、平滑迁移。
2. **稳定性优先**：引入熔断、健康检查、负载均衡策略，避免雪崩。
3. **可观测**：管理员能够看到调用数、Token 消耗、状态、失败次数。
4. **可回滚**：支持在不破坏现有功能的前提下逐步上线。
5. **低耦合**：新增逻辑通过独立服务/模块组织，避免耦合到业务流程。

## 4. 功能拆解与最佳实现方式

### 4.1 前端图片复用（减少重复加载）
**问题**：同一 URL 在多个位置被 `new Image()` 或 `<img>` 重复触发加载。

**方案**：新增前端图片缓存层。
- 新增 `src/lib/imageCache.ts`，维护 `Map<string, Promise<HTMLImageElement>>`。
- `preloadImage(url)`：若缓存存在则直接返回同一个 Promise；若不存在则创建 Image 并缓存。
- `useEffect` 预加载时改为 `preloadImage(url)`。
- `CharacterLayer` 与其它图片组件可复用缓存，不重复创建 Image 实例。
- 若 `backgroundUrl` 或 `assetUrl` 更新为相同 URL，不触发重复预加载。

**补充**：
- 后端可为 `/uploads/**` 设置合理的 `Cache-Control`（如 `public, max-age=31536000`）以利用浏览器缓存。
- 如果有相同图片以不同 URL 访问（带 query）会被认为不同资源，需在业务上统一 URL。

### 4.2 LLM API 池（多 API 配置）
**目标**：允许配置多个 API（主/备用），记录 Token 消耗、状态与失败次数。

**核心实体**：`LlmApiConfig`
- `id`
- `name`（可选，用于展示）
- `baseUrl`
- `apiKey`（建议加密存储或脱敏返回）
- `modelName`
- `temperature`
- `role`：`PRIMARY` | `BACKUP`
- `tokenLimit`（可为 null 表示不限）
- `tokenUsed`
- `status`：`ACTIVE` | `CIRCUIT_OPEN` | `DISABLED`
- `failureCount`（连续失败次数）
- `lastFailureAt` / `lastSuccessAt` / `circuitOpenedAt`
- `maxLoad`（30s 最大负载）
- `createdAt` / `updatedAt`（可选）

### 4.3 压力检测与负载均衡
**规则**：每个 API 记录最近 30 秒调用次数，并可配置 `maxLoad`。

**实现建议**：
- 内存滑动窗口计数器（单实例部署）：
  - 使用 `ConcurrentLinkedDeque<Instant>` 或 30 个桶（秒级）进行滑动窗口计数。
  - `recordCall(apiId)` 时追加时间戳并清理 30s 前的数据。
- 若未来横向扩展，需要改为 Redis 或数据库聚合表。

**输出**：
- 管理后台显示 `currentLoad`（过去 30s 调用次数）与 `maxLoad`。

### 4.4 API 选择与会话绑定
**约束**：用户开启新会话时预先绑定一个 API；会话过程中保持使用该 API，除非该 API 不可用。

#### 4.4.1 会话定义（建议）
- 增加 `ChatSession`（或 `UserSession`）表：
  - `id`
  - `userId`
  - `activeApiId`
  - `createdAt` / `lastActiveAt`
  - `status`（ACTIVE/EXPIRED）
- 前端生成 `sessionId`（可存 localStorage），每次聊天/回忆请求带上。
- 若没有 `sessionId` 或超时（例如 30 分钟无操作），后端创建新会话并选择 API。

#### 4.4.2 选择与切换规则
- **规则1**：优先选择「主 API」中 `token` 充足且 `maxLoad - currentLoad` 最大的 API。
- **规则2**：若所有主 API `token` 将要耗尽，则在主 API 中选择 `maxLoad - currentLoad` 最大者（即便 token 不足但仍可用）。
- **规则3**：若全部主 API 不可用（熔断/用尽/禁用），才考虑备用 API。
- **规则4**：禁止使用 `token` 已耗尽或 `status=CIRCUIT_OPEN` 的 API。
- **规则5**：API 失败则立即切换（遵循相同算法），新 API 正常后不自动回切旧 API。

**“token 充足/将要耗尽”判定建议**：
- `remainingTokens = tokenLimit - tokenUsed`；`tokenLimit` 为空则视为无限。
- 设定阈值 `minRemainingTokens`（如 2000）或 `minRemainingPercent`（如 5%）。
- 充足：`remainingTokens >= threshold`；将要耗尽：`remainingTokens < threshold`。

#### 4.4.3 失败计数与熔断
- API 调用失败计为一次连续失败；成功则失败计数清零。
- 连续 3 次失败 -> `status = CIRCUIT_OPEN`，记录 `circuitOpenedAt`。
- 熔断状态每 10 分钟进行一次健康检查（发送 `hi` 测试消息）。
- 健康检查成功则恢复为 `ACTIVE` 且 `failureCount = 0`。

### 4.5 管理员账号从 application.yml 配置
**目标**：允许配置多个管理员账号，并支持密码更新。

**实现建议**：
- 新增 `app.admins` 配置项：
  ```yaml
  app:
    admins:
      - username: admin
        password: admin123
        email: admin@example.com
      - username: ops
        password: ops123
        email: ops@example.com
  ```
- 启动时扫描配置：
  - 不存在则创建 `role=ADMIN`。
  - 存在且密码不匹配则更新密码（使用 BCrypt 编码）。
  - 角色不是 ADMIN 则纠正为 ADMIN。
- 移除 DataInitializer 的硬编码 admin 创建逻辑或改为读取配置。

## 5. 后端结构调整建议

### 5.1 新增实体/仓库/服务
- `LlmApiConfig` / `LlmApiConfigRepository`
- `ChatSession` / `ChatSessionRepository`
- `LlmPoolService`：
  - 选择 API
  - 记录调用与 Token 统计
  - 处理失败与熔断
  - 健康检查调度
- `ApiLoadTracker`（内存）：维护 30s 调用量

### 5.2 主要服务调整
- `GameService.handleChat` / `recallMemory`：
  - 获取或创建会话（绑定 API）
  - 调用 `LlmPoolService` 发起请求
  - 成功时记录 Token 使用
  - 失败时触发切换逻辑

### 5.3 线程安全与一致性
- `LlmApiConfig` 可加 `@Version` 字段实现乐观锁。
- 更新 Token/失败数时在同一事务中完成。

## 6. API 接口设计（建议）

### 6.1 管理后台 API
- `GET /api/admin/system/llm-apis`：查询 API 池列表（apiKey 返回脱敏）。
- `POST /api/admin/system/llm-apis`：新增 API。
- `PUT /api/admin/system/llm-apis/{id}`：更新 API。
- `DELETE /api/admin/system/llm-apis/{id}`：删除 API。
- `POST /api/admin/system/llm-apis/{id}/reset-tokens`：重置 tokenUsed。
- `POST /api/admin/system/llm-apis/{id}/test`：手动测试 API。

> 现有 `/api/admin/system/llm` 标记为 deprecated，保留一段时间以兼容旧前端。

### 6.2 客户端请求扩展
- `POST /api/game/chat` / `POST /api/game/memory/recall`：新增可选 `sessionId`。
- `POST /api/game/session`：显式创建会话（返回 sessionId，可选）。

## 7. 前端改动建议

### 7.1 管理端 UI
- 将「模型连接设置」升级为「LLM API 池管理」页面：
  - 列表展示：名称、baseUrl、模型名、tokenLimit/tokenUsed、30s调用次数、maxLoad、状态、角色（主/备）。
  - 操作：新增/编辑/删除、测试连接、重置 tokenUsed、切换主/备。
  - 失败次数、熔断时间可在详情中展示。

### 7.2 游戏端图片复用
- 引入 `imageCache`，在 `GamePage` 中对 `locations` 与 `fireflyAssets` 做去重预加载。
- `CharacterLayer` 中使用已缓存图片（不重复 new Image）。

## 8. 数据库变更（建议 SQL）

```sql
CREATE TABLE IF NOT EXISTS llm_api_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100),
  base_url VARCHAR(255) NOT NULL,
  api_key VARCHAR(255),
  model_name VARCHAR(100) NOT NULL,
  temperature DOUBLE,
  role VARCHAR(20) DEFAULT 'PRIMARY',
  token_limit BIGINT,
  token_used BIGINT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  failure_count INT DEFAULT 0,
  last_failure_at TIMESTAMP NULL,
  last_success_at TIMESTAMP NULL,
  circuit_opened_at TIMESTAMP NULL,
  max_load INT DEFAULT 30
);

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  active_api_id BIGINT,
  created_at TIMESTAMP,
  last_active_at TIMESTAMP,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_session_api FOREIGN KEY (active_api_id) REFERENCES llm_api_config(id)
);
```

**迁移策略**：
- 读取旧 `llm_setting`，写入 `llm_api_config` 第一条（role=PRIMARY）。
- 保留旧表以兼容读取；待稳定后再清理。

## 9. 配置项建议

```yaml
app:
  admins:
    - username: admin
      password: admin123
      email: admin@example.com
  llm:
    min-remaining-tokens: 2000
    min-remaining-percent: 5
    session-timeout-minutes: 30
    circuit-breaker:
      failure-threshold: 3
      probe-interval-minutes: 10
```

## 10. 单元测试与关键用例
- `LlmPoolServiceTest`：API 选择逻辑、熔断、健康检查、token 计数。
- `SessionServiceTest`：会话绑定/超时创建。
- `ImageCacheTest`（前端）：重复 URL 只加载一次。

## 11. 上线与回滚建议
- **上线步骤**：
  1. 数据库迁移（新增表）。
  2. 后端上线（兼容旧接口）。
  3. 前端后台页面更新。
  4. 配置 `app.admins` & LLM 池。
- **回滚策略**：
  - 若新 LLM 池不可用，可临时回退使用旧 `llm_setting`。

