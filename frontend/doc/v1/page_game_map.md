# 地图系统 (Map System)

**对应文件**: `src/components/game/MapInterface.tsx`

## 1. 页面布局 (Layout)
*   **容器**: 全屏模态框 (`fixed inset-0`)，黑色半透明背景模糊 (`backdrop-blur`)。
*   **主体**: 居中的 16:9 容器，模拟终端屏幕。
*   **视图层级**:
    1.  **星域视图 (Galaxy View)**: 显示所有星球/星域 (如匹诺康尼、雅利洛-VI)。
    2.  **区域视图 (Area View)**: 显示特定星域下的具体地点 (如黄金的时刻、筑梦边境)。
*   **详情面板**: 选中区域后，右侧滑出侧边栏，显示详细信息和操作按钮。

## 2. 功能实现 (Features)
*   **层级导航**:
    *   点击 "星轨航图" 返回上一级。
    *   点击星域图标进入下一级。
*   **区域交互**:
    *   点击区域图标 (`MapPin`) 选中该区域。
    *   根据 `isUnlocked` 状态显示不同的图标样式 (锁/解锁)。
    *   当前所在地会有特殊标记 ("当前位置")。
*   **跃迁指令**:
    *   点击 "开始跃迁" 按钮，触发 `onTravel` 回调，关闭地图并向对话系统发送指令。

## 3. 模拟数据与效果 (Mock Implementation)
*   **数据结构**: 依赖 `src/lib/simulation.ts` 中的 `STAR_DOMAINS` 和 `LOCATIONS` 常量。
*   **坐标系统**: 使用百分比 (`left: 50%, top: 50%`) 模拟地图上的相对坐标。
*   **视觉效果**: 使用 CSS 绘制的圆形节点和 SVG 虚线模拟星际航线。

## 4. 真实开发替换指南 (Migration to Real)
*   **数据获取**:
    *   `GET /api/world/map`: 获取所有星域和区域的层级结构。
    *   `GET /api/user/progress`: 获取用户当前的解锁状态 (`isUnlocked`)。
*   **地图渲染**:
    *   建议使用 `Canvas` 或专门的地图库 (如 Leaflet, PixiJS) 替换 DOM 节点渲染，以支持缩放、拖拽等高级交互。
    *   底图替换为美术提供的精细地图切片。