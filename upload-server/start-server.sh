#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR"

JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_BIN" ]]; then
  JAVA_BIN="java"
fi

if ! command -v "$JAVA_BIN" >/dev/null 2>&1; then
  echo "Java runtime was not found. Please install JDK 8 or set JAVA_HOME." >&2
  exit 1
fi

JAR_PATH="${UPLOAD_SERVER_JAR:-$APP_DIR/target/upload-server.jar}"
if [[ ! -f "$JAR_PATH" && -f "$APP_DIR/upload-server.jar" ]]; then
  JAR_PATH="$APP_DIR/upload-server.jar"
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "upload-server.jar was not found." >&2
  echo "Expected: $JAR_PATH" >&2
  echo "Build with: mvn -pl upload-server -am -DskipTests package" >&2
  exit 1
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export UPLOAD_FLOWER_PATH="${UPLOAD_FLOWER_PATH:-/data/MLS_Pic/mls_pic}"
export UPLOAD_COLOR_PATH="${UPLOAD_COLOR_PATH:-/data/MLS_Pic/mls_pic/color_scheme}"
export UPLOAD_LOG_FILE="${UPLOAD_LOG_FILE:-/data/logs/upload-server/application.log}"

mkdir -p "$UPLOAD_FLOWER_PATH" "$UPLOAD_COLOR_PATH" "$(dirname "$UPLOAD_LOG_FILE")"

DEFAULT_JAVA_OPTS="-Xms512m -Xmx2g -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
JAVA_OPTS="${JAVA_OPTS:-$DEFAULT_JAVA_OPTS}"

echo "Starting upload-server"
echo "  jar: $JAR_PATH"
echo "  profile: $SPRING_PROFILES_ACTIVE"
echo "  flower path: $UPLOAD_FLOWER_PATH"
echo "  color path: $UPLOAD_COLOR_PATH"
echo "  log file: $UPLOAD_LOG_FILE"

# shellcheck disable=SC2086
exec "$JAVA_BIN" $JAVA_OPTS -jar "$JAR_PATH" "$@"
