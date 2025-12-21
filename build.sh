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
if command -v mvn >/dev/null 2>&1 && mvn -v >/dev/null 2>&1 && mvn -v | rg -q "Java version: 25\\."; then
  USE_LOCAL_MVN=true
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

ensure_hosts

echo "[1/7] Building backend tests"
(cd backend && run_mvn -q test)

echo "[2/7] Packaging backend"
(cd backend && run_mvn -q -DskipTests package)

echo "[3/7] Installing frontend deps"
ensure_pnpm
fix_node_modules_permissions
(cd frontend && CI=true pnpm install --frozen-lockfile)

echo "[4/7] Running frontend tests"
(cd frontend && CI=true pnpm test)

echo "[5/7] Building frontend"
(cd frontend && CI=true pnpm build)

echo "[6/7] Docker compose rebuild"
run_compose down --remove-orphans || true
run_compose build

echo "[7/7] Starting stack"
run_compose up -d

echo "Deployment completed. Frontend: http://samproject.seekerhut.com:8090  Backend: http://samproject.seekerhut.com:8081"
