# 后台管理系统 (Admin System)

**对应路径**: `/admin` 及 `/admin/dashboard/*`

## 1. 页面布局 (Layout)
*   **登录页**: 独立的卡片式布局，背景与主页类似。
*   **管理后台 (`AdminLayout`)**:
    *   **侧边栏 (Sidebar)**: 固定在左侧，包含 Logo、导航菜单、退出按钮。
    *   **顶部栏 (Header)**: 显示当前页面标题和系统状态。
    *   **内容区 (Main)**: 滚动区域，渲染子路由内容。

## 2. 功能模块 (Modules)

### 2.1 流萤立绘管理 (`FireflyArtManager`)
*   **功能**: 配置流萤在不同情绪 (`Emotion` 枚举: smile, sad, shy 等) 下显示的资源。
*   **UI**: 网格展示每个情绪的卡片，包含预览区和 URL 输入框。
*   **API 需求**:
    *   `GET /api/admin/assets/firefly`
    *   `POST /api/admin/assets/firefly` (Body: `{ [emotion]: url }`)

### 2.2 区域与星球管理 (`LocationManager`)
*   **功能**: 管理 `StarDomain` (星域) 和 `Location` (区域) 数据。
*   **UI**: 使用 Tabs 切换区域和星域列表。
*   **关键字段**:
    *   `description`: **UI 展示用**。简短，给玩家看。
    *   `aiDescription`: **AI 专用**。详细的环境描写、可交互物体设定，用于 System Prompt。
*   **API 需求**:
    *   `GET/POST /api/admin/world/locations`
    *   `GET/POST /api/admin/world/domains`

### 2.3 角色档案管理 (`CharacterManager`)
*   **功能**: 管理 NPC 角色数据。
*   **UI**: 卡片列表展示角色，支持添加新角色。
*   **关键字段**:
    *   `prompt`: **详细人设**。包含性格、语气、背景，用于 AI 角色扮演。
*   **API 需求**:
    *   `GET/POST /api/admin/world/characters`

### 2.4 模型设置 (`LLMSettings`)
*   **功能**: 配置后端 Spring AI 连接的大模型参数。
*   **UI**: 表单配置 Base URL, API Key, Model Name, Temperature。
*   **API 需求**:
    *   `GET /api/admin/system/llm` (返回脱敏的 Key)
    *   `POST /api/admin/system/llm`
    *   `POST /api/admin/system/llm/test` (测试连接)

## 3. 模拟数据与效果 (Mock Implementation)
*   **权限验证**: 使用 `localStorage.getItem('is_admin_logged_in')` 模拟 Session。
*   **数据操作**: 所有 "保存" 按钮仅触发 `toast.success` 提示，不实际持久化数据。
*   **表单回显**: 使用硬编码的默认值或 `simulation.ts` 中的数据进行回显。

## 4. 真实开发替换指南 (Migration to Real)
*   **安全认证**:
    *   后台接口应受到严格的权限控制 (RBAC)。
    *   登录应换取具有 Admin 权限的 Token。
*   **数据持久化**:
    *   所有配置应存储在 MySQL 数据库中。
    *   AI 相关的设定 (Prompt) 可能需要同步更新到向量数据库或 AI Agent 的配置中心。