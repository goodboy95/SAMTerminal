# WorldController 接口

## GET /api/world/map
- 描述：获取星域与区域的层级数据。
- 响应：
  - `domains`: StarDomainDto 列表（id/code, name, description, x, y, color）
  - `locations`: LocationDto 列表（id/code, name, description, backgroundStyle, backgroundUrl, x, y, unlocked, domainId）
- 用途：前端地图模态、状态栏地点描述。

## GET /api/world/assets/firefly
- 描述：获取流萤表情立绘配置（emotion -> url）。
