# 背包与记忆系统 (Inventory & Memory)

**对应文件**: `src/components/game/InventoryInterface.tsx`

## 1. 页面布局 (Layout)
*   **容器**: 居中模态框，样式与地图系统保持一致 (S.A.M. 终端风格)。
*   **导航**: 顶部使用 `Tabs` 组件切换 "背包物品" 和 "记忆回廊"。
*   **内容区**: 使用 `ScrollArea` 实现内部滚动，保持外部容器高度固定。

## 2. 功能实现 (Features)
*   **物品栏 (Items Tab)**:
    *   Grid 布局 (2列或3列)。
    *   展示物品 Icon、名称、简介、数量。
    *   Hover 效果：边框高亮，图标轻微缩放。
*   **记忆回廊 (Memories Tab)**:
    *   List 布局 (垂直列表)。
    *   展示记忆卡片：标题、日期、文本内容、标签 (`Badge`)。
    *   这是 RAG (检索增强生成) 的可视化展示窗口。

## 3. 模拟数据与效果 (Mock Implementation)
*   **数据源**: `src/lib/simulation.ts` 中的 `INITIAL_ITEMS` 和 `INITIAL_MEMORIES` 数组。
*   **图标**: 使用 Emoji 代替真实的物品图片。

## 4. 真实开发替换指南 (Migration to Real)
*   **物品接口**:
    *   `GET /api/player/inventory`: 获取当前持有的道具列表。
*   **记忆接口 (RAG)**:
    *   `GET /api/player/memories`: 获取记忆列表。
    *   **核心逻辑**: 后端应根据当前的对话上下文或特定的查询，从向量数据库 (Vector DB) 中检索相关的记忆片段返回给前端。
    *   **交互扩展**: 点击某条记忆，可以触发 "回忆" 功能，让流萤针对该段记忆发表评论。