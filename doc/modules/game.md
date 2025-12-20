# 游戏主界面模块（/game）

- **状态管理**：通过 `/api/game/status` 获取初始状态，`/api/game/chat` 发送消息并返回状态增量（含 stateUpdate）。
- **地图模态**：`MapInterface` 从 `/api/world/map` 获取星域/区域，支持用户解锁状态与 travel_to 指令。
- **背包/记忆**：`InventoryInterface` 使用 `/api/player/inventory`、`/api/player/memories`，点击记忆触发 `/api/game/memory/recall`。
- **角色层**：表情由后端状态中的 `fireflyEmotion` 控制；资源从 `/api/world/assets/firefly` 读取并预加载。
- **响应式**：Tailwind 断点 `md` 分割 PC/移动布局。
