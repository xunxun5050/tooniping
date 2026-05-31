#!/usr/bin/env bash
set -euo pipefail

NODE_BIN="/Users/jadekim/Documents/webtoon-damoa/.tools/node/bin"
export PATH="$NODE_BIN:$PATH"

if [ "$#" -eq 0 ]; then
  echo "Node path: $NODE_BIN"
  node -v
  npm -v
  exit 0
fi

exec "$@"
