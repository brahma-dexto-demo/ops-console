#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CONSOLE_PORT=${CONSOLE_PORT:-28171}
MOCK_API_PORT=${MOCK_API_PORT:-28172}
LOG=$(mktemp /tmp/ops-console-risk-e2e.XXXXXX)
MOCK_API_PORT="$MOCK_API_PORT" node "$ROOT/e2e/mock-api.js" &
api_pid=$!
SERVER_PORT="$CONSOLE_PORT" ACCOUNTS_API_URL="http://127.0.0.1:$MOCK_API_PORT" java -jar "$ROOT/target/ops-console.jar" >"$LOG" 2>&1 &
console_pid=$!
trap 'kill "$api_pid" "$console_pid" 2>/dev/null || true; rm -f "$LOG"' EXIT
for i in $(seq 1 60); do
  if ! kill -0 "$api_pid" 2>/dev/null || ! kill -0 "$console_pid" 2>/dev/null; then
    cat "$LOG" >&2
    echo 'Mock API or console exited before ready' >&2
    exit 1
  fi
  if curl -fsS "http://127.0.0.1:$CONSOLE_PORT/healthz" >/dev/null 2>&1; then break; fi
  sleep 1
done
if ! curl -fsS "http://127.0.0.1:$CONSOLE_PORT/healthz" >/dev/null; then
  cat "$LOG" >&2
  exit 1
fi
MOCK_API=1 BASE_URL="http://127.0.0.1:$CONSOLE_PORT" npx playwright test risk.spec.js
