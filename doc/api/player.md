# PlayerController 接口

## GET /api/player/inventory
- 描述：获取当前用户背包。
- 鉴权：需要 JWT。
- 响应：ItemDto 数组。

## GET /api/player/memories
- 描述：获取记忆回廊列表。
- 鉴权：需要 JWT。
- 响应：MemoryDto 数组。

## GET /api/player/progress
- 描述：获取当前用户已解锁的地点 code 列表。
- 鉴权：需要 JWT。
