# 游戏主界面 (Game Main Interface)

**对应文件**: `src/pages/GamePage.tsx`

## 1. 页面布局 (Layout)
采用 **Z-Axis Layering (Z轴层叠)** 布局策略，利用绝对定位将不同功能的层级叠加：

*   **Layer 0 (背景层)**: `div.absolute.inset-0`
    *   全屏背景，根据当前地点动态切换 CSS Class (如 `bg-gradient-to-br...`)。
    *   叠加纹理层 (`stardust.png`) 和暗角渐变。
*   **Layer 1 (角色层)**: `CharacterLayer.tsx`
    *   位于背景之上，UI 之下。
    *   展示流萤的立绘，支持根据情绪切换。
*   **Layer 2 (UI 层)**: `div.relative.z-20`
    *   **顶部**: `StatusBar.tsx` (悬浮状态栏)。
    *   **右上**: `GameMenu.tsx` (功能菜单按钮)。
    *   **右侧/底部**: `ChatInterface.tsx` (对话框)。
        *   *响应式适配*: 桌面端位于右侧固定宽度，移动端位于底部占据半屏。
*   **Layer 3 (模态框层)**:
    *   `MapInterface` 和 `InventoryInterface`，通过条件渲染覆盖在最上层。

## 2. 功能实现 (Features)
*   **状态栏 (StatusBar)**:
    *   显示当前地点、流萤心情 (Emoji)、游戏时间。
    *   **Tooltip 交互**: 鼠标悬停地点可查看环境描述 (`locationDynamicState`)，悬停心情可查看状态 (`fireflyStatus`) 和心声 (`fireflyMoodDetails`)。
*   **对话系统 (ChatInterface)**:
    *   展示历史消息流。
    *   支持用户输入文本。
    *   显示 "对方正在输入" 的动画状态。
    *   自动滚动到底部。
*   **场景切换**: 背景随 `gameState.currentLocation` 变化而平滑过渡。

## 3. 模拟数据与效果 (Mock Implementation)
*   **Agent 模拟**: `src/lib/simulation.ts` 中的 `mockAgentResponse` 函数。
    *   使用 `setTimeout` 模拟 AI 思考时间。
    *   简单的关键词匹配逻辑 (如检测 "travel_to:", "去筑梦边境") 来触发状态变更。
    *   随机回复逻辑：从预设的回复库中随机抽取文本和心情。
*   **数据源**: `INITIAL_STATE` 包含了硬编码的初始状态。

## 4. 真实开发替换指南 (Migration to Real)
*   **长连接**:
    *   建立 WebSocket 或 SSE (Server-Sent Events) 连接，用于接收 AI 的流式回复 (Streaming Response)。
*   **状态同步**:
    *   `StatusBar` 的数据应来自 `GET /api/game/status`，或通过 WebSocket 推送更新。
*   **资源加载**:
    *   背景层: 替换为 `<img src={currentLocation.bgUrl} />`，并实现图片预加载以防止闪烁。
    *   角色层: 接入 Live2D Web SDK 或 Spine Web Player，实现更生动的动作交互。