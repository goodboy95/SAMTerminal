# 后台管理模块（/admin, /admin/dashboard/*）

- **登录**：调用 `/api/admin/login`，要求返回 role=ADMIN。
- **立绘管理**：对接 `/api/admin/assets/firefly`，表单保存情绪对应资源 URL。
- **星域/区域管理**：`/api/admin/world/domains`、`/api/admin/world/locations`，支持新增/更新/删除/批量导入，附带 AI 描述与背景图上传。
- **角色档案**：`/api/admin/world/characters`，支持简介、头像 URL、批量导入与删除。
- **Token 监测**：`/api/admin/users/usage`、`/api/admin/settings/global-limit`、`/api/admin/users/{id}/limit`。
- **LLM 设置**：`/api/admin/system/llm` 读取/保存；`/test` 校验连通性。
