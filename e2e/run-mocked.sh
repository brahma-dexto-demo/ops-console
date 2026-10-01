#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
node "$ROOT/e2e/mock-api.js" &
api_pid=$!
SERVER_PORT=18101 ACCOUNTS_API_URL=http://127.0.0.1:18102 java -jar "$ROOT/target/ops-console.jar" >/tmp/ops-console-risk-e2e.log 2>&1 &
console_pid=$!
trap 'kill "$api_pid" "$console_pid" 2>/dev/null || true' EXIT
for i in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:18101/healthz >/dev/null 2>&1; then break; fi
  sleep 1
done
BASE_URL=http://127.0.0.1:18101 npx playwright test risk.spec.js
