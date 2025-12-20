# 功能操作与测试步骤

## 1. 用户端（/）
1. 打开 `http://localhost:4173/`。
2. 在“接入系统”输入用户名/密码，点击“初始化连接”触发 `/api/auth/login`；或切换“注册识别码”填写代号、邮箱、密码提交注册。
3. 登录成功自动跳转 `/game`。

## 2. 游戏主界面（/game）
1. 页面加载后自动请求 `/api/game/status` 与 `/api/world/map`。
2. 聊天栏输入文本并发送，调用 `/api/game/chat`，应看到流式回复与状态栏心情变化。
3. 点击右上菜单 → “星轨航图”，在地图选择区域并点击“开始跃迁”，应关闭模态并收到 travel_to 指令回复、背景变化。
4. 点击菜单 → “背包/记忆”，核对背包物品、记忆列表来自 `/api/player/*`；点击记忆卡片触发 `/api/game/memory/recall` 并出现回忆回复。

## 3. 管理后台
1. 访问 `/admin`，使用 `admin / admin123` 登录，成功后进入 `/admin/dashboard`。
2. **立绘管理**：修改某情绪 URL 或上传图片，点击“保存配置”，调用 `/api/admin/assets/firefly`。
3. **区域与星球**：在“区域场景/星域”页编辑字段后点击“保存”或“保存所有更改”，对应 POST `/api/admin/world/*`；可执行批量导入或删除。
4. **角色档案**：新增角色、上传头像并保存，调用 `/api/admin/world/characters`；可执行批量导入或删除。
5. **用户 Token 监测**：进入“用户 Token 监测”，调整全局或单用户上限，调用 `/api/admin/settings/global-limit` 与 `/api/admin/users/{id}/limit`。
6. **模型设置**：填写 BaseURL、API Key、模型名、温度，点击“保存并测试连接”，调用 `/api/admin/system/llm` 与 `/test`。

## 4. 部署验证
1. 运行 `./build.sh`，脚本会执行后端测试、前端构建并启动 docker-compose。
2. 部署后验证前端 `http://localhost:4173` 可访问，后端接口 `http://localhost:8081` 可响应。
