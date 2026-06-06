#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME_LOCAL="$ROOT_DIR/.jdks/jdk-17.0.19+10/Contents/Home"

load_env_file() {
  local file_path="$1"
  if [ -f "$file_path" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$file_path"
    set +a
  fi
}

if [ ! -x "$JAVA_HOME_LOCAL/bin/java" ]; then
  echo "[ERROR] 로컬 JDK를 찾을 수 없습니다: $JAVA_HOME_LOCAL"
  echo "필요 시 .jdks 디렉터리 안에 JDK 17을 배치해 주세요."
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_LOCAL"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_USER_HOME="$ROOT_DIR/.m2"

# Load OAuth/app env from optional local files.
# Precedence: project root oauth.env < backend/oauth.env
load_env_file "$ROOT_DIR/../oauth.env"
load_env_file "$ROOT_DIR/oauth.env"

exec "$ROOT_DIR/mvnw" spring-boot:run
