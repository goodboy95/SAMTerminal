# 功能操作与测试步骤

## 1. 用户端（/）
1. 打开 `http://samproject.seekerhut.com:8090/`。
2. 在“接入系统”输入用户名/密码，点击“初始化连接”触发 `/api/auth/login`；或切换“注册识别码”进行注册。
3. 注册流程：填写代号与邮箱 → 点击“发送验证邮件”并完成 CAP → 输入验证码并点击“验证验证码” → 验证通过后提交档案。
   - 仅验证“发送验证码”时：完成 CAP 后点击“发送验证邮件”，提示提交成功即可视为发送流程通过。
4. 登录或注册成功自动跳转 `/game`。

## 2. 游戏主界面（/game）
1. 页面加载后自动请求 `/api/game/status` 与 `/api/world/map`。
2. 首次进入会创建会话（`/api/game/session`），后续聊天携带 `sessionId`。
3. 聊天栏输入文本并发送，调用 `/api/game/chat`，应看到回复与状态栏心情变化。
3. 点击右上菜单 → “星轨航图”，在地图选择区域并点击“开始跃迁”，应关闭模态并收到 travel_to 指令回复、背景变化。
4. 点击菜单 → “背包/记忆”，核对背包物品、记忆列表来自 `/api/player/*`；点击记忆卡片触发 `/api/game/memory/recall` 并出现回忆回复。

## 3. 管理后台
1. 访问 `/admin`，使用 `app.admins` 配置的管理员账号登录，成功后进入 `/admin/dashboard`（默认示例账号：`admin/AdminPass123456`、`operator/OperatorPass123456`）。
2. **立绘管理**：修改某情绪 URL 或上传图片，点击“保存配置”，调用 `/api/admin/assets/firefly`。
3. **区域与星球**：在“区域场景/星域”页编辑字段后点击“保存”或“保存所有更改”，对应 POST `/api/admin/world/*`；可执行批量导入或删除。
4. **角色档案**：新增角色、上传头像并保存，调用 `/api/admin/world/characters`；可执行批量导入或删除。
5. **用户 Token 监测**：进入“用户 Token 监测”，调整全局或单用户上限，调用 `/api/admin/settings/global-limit` 与 `/api/admin/users/{id}/limit`。
6. **LLM API 池**：新增/编辑 API（BaseURL、API Key、模型、角色、Token 限额、负载），调用 `/api/admin/system/llm-apis`；可执行测试连接与重置 Token。
7. **邮件验证管理**：进入“邮件验证管理”页，配置 SMTP、查看发送日志并解密验证码、查看 IP 统计及手动封禁/解封。

## 4. 部署验证
1. 使用非交互 sudo 运行：`echo "123456" | sudo -S ./build.sh`，脚本会执行后端测试、前端构建并启动 docker-compose（前后端产物挂载到运行时镜像）。
2. 确认 `/etc/hosts` 含 `127.0.0.1 samproject.seekerhut.com`（build.sh 会自动补齐）。
3. 部署后验证前端 `http://samproject.seekerhut.com:8090` 可访问，后端接口 `http://samproject.seekerhut.com:8081` 可响应。
