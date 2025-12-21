# 已知问题记录

## 1. build.sh 需 sudo 但环境阻止提权
- 现象：执行 `sudo -S ./build.sh` 返回 `The "no new privileges" flag is set`，导致无法完成部署脚本要求的 sudo 运行。
- 影响：无法按要求执行构建/部署/测试流程。
- 建议处理：请调整容器/环境配置，允许 sudo 提权（关闭 no_new_privileges 或使用 root 账号执行）。

## 2. Docker CLI 无响应
- 现象：`docker ps` 在当前环境中超时无响应。
- 影响：无法确认容器状态，影响部署与测试。
- 建议处理：检查 Docker daemon 状态或权限；如需 sudo，请先解决问题 1。
