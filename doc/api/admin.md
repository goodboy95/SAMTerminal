# AdminController 接口

## 管理员登录
- **POST /api/admin/login**：管理员登录（需要 ADMIN 角色）。
  - 请求体：`{ username, password }`
  - 响应：`{ token, username, role }`

## 资产管理
- **GET /api/admin/assets/firefly**：获取各情绪立绘 URL 列表。
- **POST /api/admin/assets/firefly**：保存立绘映射。
  - 请求体：`{ assets: { emotion: url } }`

## 世界 & 区域
- **GET /api/admin/world/domains**：获取星域列表。
- **POST /api/admin/world/domains**：新增/更新星域（字段：code, name, description, aiDescription, coordX, coordY, color）。
- **POST /api/admin/world/domains/batch**：批量导入星域（JSON 数组）。
- **DELETE /api/admin/world/domains/{id}**：删除星域。
- **GET /api/admin/world/locations**：获取区域列表。
- **POST /api/admin/world/locations**：新增/更新区域（code, name, description, aiDescription, backgroundStyle, backgroundUrl, coordX, coordY, unlocked, domainCode）。
- **POST /api/admin/world/locations/batch**：批量导入区域（JSON 数组）。
- **DELETE /api/admin/world/locations/{id}**：删除区域。

## 角色
- **GET /api/admin/world/characters**：获取 NPC 角色档案。
- **POST /api/admin/world/characters**：新增/更新角色（name, role, prompt, description, avatarUrl）。
- **POST /api/admin/world/characters/batch**：批量导入角色（JSON 数组）。
- **DELETE /api/admin/world/characters/{id}**：删除角色。

## LLM 设置（兼容旧接口）
- **GET /api/admin/system/llm**：读取模型连接信息（返回 baseUrl, modelName, temperature）。
- **POST /api/admin/system/llm**：保存模型连接参数（baseUrl, apiKey, modelName, temperature）。
- **POST /api/admin/system/llm/test**：测试连接（返回 `{status:"connected"}`）。

## LLM API 池管理
- **GET /api/admin/system/llm-apis**：获取 API 池列表（apiKey 返回脱敏，附带 currentLoad）。
- **POST /api/admin/system/llm-apis**：新增 API 配置。
  - 请求体：`{ name, baseUrl, apiKey, modelName, temperature, role, tokenLimit, maxLoad, status }`
- **PUT /api/admin/system/llm-apis/{id}**：更新 API 配置（apiKey 留空则保持不变）。
- **DELETE /api/admin/system/llm-apis/{id}**：删除 API 配置。
- **POST /api/admin/system/llm-apis/{id}/reset-tokens**：重置 tokenUsed 与 failureCount。
- **POST /api/admin/system/llm-apis/{id}/test**：测试 API 连通性（返回 `{status: "connected" | "failed"}`）。

## Token 监测
- **GET /api/admin/users/usage**：获取用户 Token 统计与限额。
- **POST /api/admin/settings/global-limit**：更新全局 Token 上限（请求体 `{ limit }`）。
- **POST /api/admin/users/{id}/limit**：设置用户自定义上限（请求体 `{ limit }`，可为 null）。
