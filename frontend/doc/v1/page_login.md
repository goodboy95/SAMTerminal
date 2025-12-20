# 登录与注册页面 (Login & Register)

**对应文件**: `src/pages/Index.tsx`

## 1. 页面布局 (Layout)
*   **背景**: 全屏深色背景 (`bg-slate-950`)，叠加径向渐变 (`radial-gradient`) 以营造科技感氛围。
*   **中心容器**: 垂直居中的卡片容器，最大宽度限制为 `max-w-md`。
*   **视觉元素**:
    *   顶部 Logo: 使用 Lucide `Cpu` 图标，带有呼吸动画 (`animate-pulse`)。
    *   标题: "S.A.M. Terminal"，使用渐变文字效果。
    *   副标题: 装饰性的机甲协议编号。
*   **交互区域**: 使用 Shadcn UI 的 `Tabs` 组件切换 "接入系统" (登录) 和 "注册识别码" (注册) 两个面板。

## 2. 功能实现 (Features)
*   **Tab 切换**: 在登录和注册表单之间平滑切换。
*   **表单输入**:
    *   登录: 用户名 (代号)、密码 (访问密钥)。
    *   注册: 代号、邮箱、密码。
*   **提交处理**:
    *   点击按钮后进入 `isLoading` 状态，按钮显示加载文字和 Spinner。
    *   验证通过后跳转至 `/game`。

## 3. 模拟数据与效果 (Mock Implementation)
*   **模拟延迟**: 使用 `setTimeout` 模拟 1000ms 的网络请求延迟。
*   **模拟认证**:
    *   不进行真实的密码校验。
    *   直接将输入的用户名存储到 `localStorage.getItem('firefly_user')`。
    *   跳转使用 `useNavigate('/game')`。

## 4. 真实开发替换指南 (Migration to Real)
*   **API 接口**:
    *   登录: `POST /api/auth/login` (Body: `{ username, password }`)
    *   注册: `POST /api/auth/register` (Body: `{ username, email, password }`)
*   **状态管理**:
    *   登录成功后，后端应返回 JWT Token。
    *   前端需将 Token 存储在 `HttpOnly Cookie` (推荐) 或 `localStorage` 中。
    *   使用全局状态库 (如 Zustand/Redux) 更新 `isAuthenticated` 和 `currentUser` 状态。
*   **表单验证**:
    *   引入 `zod` 和 `react-hook-form` 进行严格的表单验证 (如密码长度、邮箱格式)。