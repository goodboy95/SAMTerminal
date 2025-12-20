#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$ROOT_DIR"

if command -v mvn >/dev/null 2>&1 && mvn -v >/dev/null 2>&1; then
  MVN_CMD=mvn
elif command -v docker >/dev/null 2>&1; then
  DOCKER_CMD="docker"
  if ! docker info >/dev/null 2>&1; then
    DOCKER_CMD="sudo docker"
  fi
  MVN_CMD="$DOCKER_CMD run --rm -v $ROOT_DIR/backend:/app -w /app maven:3.9-eclipse-temurin-17 mvn"
else
  echo "Neither Maven nor Docker found. Please install one of them or set MVN_CMD manually." >&2
  exit 1
fi

echo "[1/6] Building backend tests"
(cd backend && $MVN_CMD -q test)

echo "[2/6] Packaging backend"
(cd backend && $MVN_CMD -q -DskipTests package)

echo "[3/6] Installing frontend deps"
(cd frontend && pnpm install --frozen-lockfile)

echo "[4/6] Building frontend"
(cd frontend && pnpm build)

echo "[5/6] Docker compose rebuild"
if command -v docker-compose >/dev/null 2>&1; then
  DCMD=docker-compose
  SUDO_PREFIX=""
  if ! docker-compose ps >/dev/null 2>&1; then
    SUDO_PREFIX="sudo "
  fi
  DCMD="${SUDO_PREFIX}${DCMD}"
elif docker compose version >/dev/null 2>&1; then
  DCMD="docker compose"
  if ! docker compose ps >/dev/null 2>&1; then
    DCMD="sudo docker compose"
  fi
else
  echo "docker compose 未安装，请安装 Docker Compose 插件或 docker-compose" >&2
  exit 1
fi

$DCMD down --remove-orphans || true
$DCMD build

echo "[6/6] Starting stack"
$DCMD up -d

echo "Deployment completed. Frontend: http://localhost:4173  Backend: http://localhost:8081"
