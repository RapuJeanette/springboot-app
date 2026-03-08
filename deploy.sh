#!/usr/bin/env bash
set -euo pipefail

APP_NAME="spring-boot-app"
APP_DIR="/opt/spring-boot-app"
JAR_NAME="app.jar"
JAVA_OPTS="-Xms512m -Xmx1024m"
SPRING_PROFILE="prod"


PORT="${2:-8080}"

NEW_JAR_PATH="$1"

if [[ -z "${NEW_JAR_PATH:-}" ]]; then
  echo "Usage: deploy.sh <path-to-new-jar>"
  exit 1
fi

echo "🚀 Deploying ${APP_NAME}"
echo "➡️  New artifact: ${NEW_JAR_PATH}"

cd "$APP_DIR"

# --- Stop running app ---
PID=$(sudo lsof -t -i:$PORT || true)

if [[ -n "$PID" ]]; then
  echo "🛑 Stopping running app (PID=$PID)"
  kill "$PID"

  for i in {1..15}; do
    if ! kill -0 "$PID" 2>/dev/null; then
      echo "✅ App stopped"
      break
    fi
    sleep 1
  done

  if kill -0 "$PID" 2>/dev/null; then
    echo "❌ App did not stop gracefully, killing"
    kill -9 "$PID"
  fi
else
  echo "ℹ️  No running app found"
fi

# --- Backup current jar ---
if [[ -f "$JAR_NAME" ]]; then
  TIMESTAMP=$(date +%Y%m%d%H%M%S)
  cp "$JAR_NAME" "versions/${APP_NAME}-${TIMESTAMP}.jar"
  cp "$JAR_NAME" "${JAR_NAME}.bak"
fi