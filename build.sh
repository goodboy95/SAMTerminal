#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$ROOT_DIR"

SUDO_PASS=${SUDO_PASS:-123456}

sudo_exec() {
  echo "$SUDO_PASS" | sudo -S "$@"
}

ensure_hosts() {
  if ! grep -q "samproject.seekerhut.com" /etc/hosts; then
    sudo_exec sh -c "echo '127.0.0.1 samproject.seekerhut.com' >> /etc/hosts"
  fi
}

USE_LOCAL_MVN=false
if command -v mvn >/dev/null 2>&1 && mvn -v >/dev/null 2>&1; then
  if command -v rg >/dev/null 2>&1; then
    if mvn -v | rg -q "Java version: 25\\."; then
      USE_LOCAL_MVN=true
    fi
  else
    if mvn -v | grep -E -q "Java version: 25\\."; then
      USE_LOCAL_MVN=true
    fi
  fi
fi

DOCKER_USE_SUDO=false
if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    DOCKER_USE_SUDO=false
  elif sudo_exec docker info >/dev/null 2>&1; then
    DOCKER_USE_SUDO=true
  fi
fi

run_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker 未安装，请先安装 Docker" >&2
    exit 1
  fi
  if [ "$DOCKER_USE_SUDO" = true ]; then
    sudo_exec docker "$@"
  else
    docker "$@"
  fi
}

run_mvn() {
  if [ "$USE_LOCAL_MVN" = true ]; then
    mvn "$@"
    return
  fi
  if ! command -v docker >/dev/null 2>&1; then
    echo "Neither Maven(Java 25) nor Docker found. Please install JDK 25+Maven or Docker, or set MVN_CMD manually." >&2
    exit 1
  fi
  run_docker run --rm -v "$ROOT_DIR/backend:/app" -w /app maven:3.9.11-eclipse-temurin-25 mvn "$@"
}

run_compose() {
  if command -v docker-compose >/dev/null 2>&1; then
    if [ "$DOCKER_USE_SUDO" = true ]; then
      sudo_exec docker-compose "$@"
    else
      docker-compose "$@"
    fi
    return
  fi
  if run_docker compose version >/dev/null 2>&1; then
    if [ "$DOCKER_USE_SUDO" = true ]; then
      sudo_exec docker compose "$@"
    else
      docker compose "$@"
    fi
    return
  fi
  echo "docker compose 未安装，请安装 Docker Compose 插件或 docker-compose" >&2
  exit 1
}

cleanup_named_containers() {
  for name in sam_mysql sam_backend sam_frontend sam_chroma sam_cap sam_mailhog; do
    run_docker rm -f "$name" >/dev/null 2>&1 || true
  done
}

ensure_pnpm() {
  if command -v pnpm >/dev/null 2>&1; then
    return 0
  fi
  if command -v corepack >/dev/null 2>&1; then
    corepack enable >/dev/null 2>&1 || true
    corepack prepare pnpm@9.15.4 --activate
    return 0
  fi
  if command -v npm >/dev/null 2>&1; then
    npm install -g pnpm@9.15.4
    return 0
  fi
  echo "pnpm 未安装且无法通过 corepack/npm 安装，请先安装 Node.js (含 corepack) 或 pnpm。" >&2
  exit 1
}

fix_node_modules_permissions() {
  if [ -d "$ROOT_DIR/frontend/node_modules" ]; then
    local owner
    owner=$(stat -c "%u" "$ROOT_DIR/frontend/node_modules" || echo "")
    if [ -n "$owner" ] && [ "$owner" != "$(id -u)" ]; then
      sudo_exec chown -R "$(id -u):$(id -g)" "$ROOT_DIR/frontend/node_modules"
    fi
  fi
  if [ -d "$ROOT_DIR/frontend/dist" ]; then
    local dist_owner
    dist_owner=$(stat -c "%u" "$ROOT_DIR/frontend/dist" || echo "")
    if [ -n "$dist_owner" ] && [ "$dist_owner" != "$(id -u)" ]; then
      sudo_exec chown -R "$(id -u):$(id -g)" "$ROOT_DIR/frontend/dist"
    fi
  fi
}

ensure_cap_keys() {
  local cap_dir="$ROOT_DIR/.cap"
  local cap_keys_file="$cap_dir/keys.env"
  local cap_port="${CAP_PORT:-8091}"

  CAP_ADMIN_KEY="${CAP_ADMIN_KEY:-cap-admin-key-change-me-1234567890}"
  export CAP_ADMIN_KEY

  if [ -f "$cap_keys_file" ]; then
    # shellcheck disable=SC1090
    source "$cap_keys_file"
  fi

  if [ -n "${CAP_SITE_KEY:-}" ] && [ -n "${CAP_SITE_SECRET:-}" ]; then
    export CAP_SITE_KEY CAP_SITE_SECRET CAP_ADMIN_KEY
    export VITE_CAP_SITE_KEY="${VITE_CAP_SITE_KEY:-$CAP_SITE_KEY}"
    return 0
  fi

  echo "[cap] Bootstrapping CAP Standalone keys"
  run_compose up -d cap

  local ready=false
  for _ in {1..30}; do
    if curl -fsS "http://localhost:${cap_port}/" >/dev/null 2>&1; then
      ready=true
      break
    fi
    sleep 1
  done
  if [ "$ready" = false ]; then
    echo "CAP Standalone 未能启动，请检查 docker-compose cap 服务。" >&2
    exit 1
  fi

  local login_json=""
  for _ in {1..3}; do
    login_json=$(curl -sS -X POST "http://localhost:${cap_port}/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"admin_key\":\"${CAP_ADMIN_KEY}\"}" || true)
    if [ -n "$login_json" ]; then
      break
    fi
    sleep 1
  done
  if [ -z "$login_json" ]; then
    echo "CAP 登录响应为空，请检查 CAP Standalone 是否可用。" >&2
    exit 1
  fi

  local session_token hashed_token
  session_token=$(echo "$login_json" | python3 - <<'PY'
import json, sys
try:
    data = json.load(sys.stdin)
    print(data.get("session_token", ""))
except Exception:
    print("")
PY
)
  hashed_token=$(echo "$login_json" | python3 - <<'PY'
import json, sys
try:
    data = json.load(sys.stdin)
    print(data.get("hashed_token", ""))
except Exception:
    print("")
PY
)
  if [ -z "$session_token" ] || [ -z "$hashed_token" ]; then
    echo "CAP 登录失败，请检查 CAP_ADMIN_KEY 是否正确且长度 >= 30。" >&2
    exit 1
  fi

  local auth_header
  auth_header=$(CAP_SESSION_TOKEN="$session_token" CAP_HASHED_TOKEN="$hashed_token" python3 - <<'PY'
import base64
import json
import os

token = os.environ.get("CAP_SESSION_TOKEN", "")
hashed = os.environ.get("CAP_HASHED_TOKEN", "")
payload = json.dumps({"token": token, "hash": hashed}).encode()
print(base64.b64encode(payload).decode())
PY
)

  local create_json
  create_json=$(curl -fsS -H "Authorization: Bearer ${auth_header}" \
    -H "Content-Type: application/json" \
    -d '{"name":"samproject"}' \
    "http://localhost:${cap_port}/server/keys")

  CAP_SITE_KEY=$(echo "$create_json" | python3 - <<'PY'
import json, sys
data = json.load(sys.stdin)
print(data.get("siteKey", ""))
PY
)
  CAP_SITE_SECRET=$(echo "$create_json" | python3 - <<'PY'
import json, sys
data = json.load(sys.stdin)
print(data.get("secretKey", ""))
PY
)

  if [ -z "$CAP_SITE_KEY" ] || [ -z "$CAP_SITE_SECRET" ]; then
    echo "CAP 站点 Key 创建失败，请检查 CAP Standalone 日志。" >&2
    exit 1
  fi

  mkdir -p "$cap_dir"
  cat > "$cap_keys_file" <<EOF
CAP_SITE_KEY=${CAP_SITE_KEY}
CAP_SITE_SECRET=${CAP_SITE_SECRET}
CAP_ADMIN_KEY=${CAP_ADMIN_KEY}
EOF

  export CAP_SITE_KEY CAP_SITE_SECRET CAP_ADMIN_KEY
  export VITE_CAP_SITE_KEY="${VITE_CAP_SITE_KEY:-$CAP_SITE_KEY}"
}

ensure_hosts

echo "[1/7] Building backend tests"
(cd backend && run_mvn -q test)

echo "[2/7] Packaging backend"
(cd backend && run_mvn -q -DskipTests package)

echo "[3/7] Preparing backend runtime jar"
(
  shopt -s nullglob
  jars=("$ROOT_DIR"/backend/target/*.jar)
  runtime_jar=""
  for jar in "${jars[@]}"; do
    case "$jar" in
      *.original|*-sources.jar|*-javadoc.jar) ;;
      *) runtime_jar="$jar"; break ;;
    esac
  done
  if [ -z "$runtime_jar" ]; then
    echo "未找到可用的后端 jar，请确认后端已成功打包。" >&2
    exit 1
  fi
  if [ "$runtime_jar" != "$ROOT_DIR/backend/target/app.jar" ]; then
    cp "$runtime_jar" "$ROOT_DIR/backend/target/app.jar"
  fi
)

echo "[4/7] Installing frontend deps"
ensure_pnpm
fix_node_modules_permissions
ensure_cap_keys
(cd frontend && CI=true pnpm install --frozen-lockfile)

echo "[5/7] Running frontend tests"
(cd frontend && CI=true pnpm test)

echo "[6/7] Building frontend"
(cd frontend && CI=true pnpm build)

echo "[7/7] Restarting docker compose"
run_compose down --remove-orphans || true
cleanup_named_containers
run_compose up -d

echo "Deployment completed. Frontend: http://samproject.seekerhut.com:8090  Backend: http://samproject.seekerhut.com:8081"
