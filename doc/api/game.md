# GameController 接口

## GET /api/game/status
- 描述：获取当前用户的游戏状态（地点、情绪、背包、记忆、时间等）。
- 鉴权：需要 JWT。
- 响应字段：
  - `currentLocation`：地点 code
  - `currentLocationName`
  - `locationDynamicState`
  - `fireflyEmotion` / `fireflyStatus` / `fireflyMoodDetails`
  - `gameTime`
  - `items`: ItemDto 数组（id, name, description, icon, quantity）
  - `memories`: MemoryDto 数组（id, title, content, date, tags）
  - `userName`

## POST /api/game/chat
- 描述：发送聊天消息，返回 AI 回复与更新后的状态。
- 请求体：`{ "message": string, "sessionId"?: string }`
- 响应：
  - `messages`: 回复消息列表（id, sender, npcName, content, narration, timestamp）
  - `state`: 同 status 接口的状态快照
  - `stateUpdate`: 可选，包含 location/firefly/inventory_change 的更新指令
  - `sessionId`: 会话 ID（如会话过期或首次创建时返回新的 sessionId）
- 业务：包含 LLM JSON 解析、intent 处理、Token 限额校验。

## POST /api/game/memory/recall
- 描述：触发记忆回廊的“回忆”对话。
- 请求体：`{ "memoryId": number, "sessionId"?: string }`
- 响应：同 `/api/game/chat`

## POST /api/game/session
- 描述：创建新的会话并绑定 LLM API。
- 请求体：空
- 响应：`{ sessionId }`
