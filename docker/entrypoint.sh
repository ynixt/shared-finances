#!/usr/bin/env bash
set -Eeuo pipefail

# ── Variables ───────────────────────────────────────────────────────

SF_APP_PORT="${SF_APP_PORT:-80}"
SF_APP_API_PORT="${SF_APP_API_PORT:-8081}"
JAVA_OPTS="${JAVA_OPTS:-}"

export SF_APP_PORT SF_APP_API_PORT

# ── Folders ──────────────────────────────────────────────────────

mkdir -p /etc/nginx/conf.d /run/nginx

# ── Helpers ──────────────────────────────────────────────

apply_http_config() {
  envsubst '${SF_APP_PORT} ${SF_APP_API_PORT}' \
    < /etc/nginx/templates/shared-finances-http.conf.template \
    > /etc/nginx/conf.d/default.conf
}

wait_for_stop() {
  local pid="$1" timeout_seconds="$2" elapsed=0
  while kill -0 "$pid" 2>/dev/null; do
    if [[ "$elapsed" -ge "$timeout_seconds" ]]; then
      return 1
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done
  return 0
}

shutdown() {
  echo "[entrypoint] Shutting down..."

  if kill -0 "$NGINX_PID" 2>/dev/null; then
    nginx -s quit >/dev/null 2>&1 || kill "$NGINX_PID" 2>/dev/null || true
  fi

  if kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID" 2>/dev/null || true
  fi

  wait_for_stop "$NGINX_PID" 10 || kill -9 "$NGINX_PID" 2>/dev/null || true
  wait_for_stop "$BACKEND_PID" 25 || kill -9 "$BACKEND_PID" 2>/dev/null || true
}

# ── Main ────────────────────────────────────────────────────────────

apply_http_config

# Backend (Spring Boot)
java $JAVA_OPTS -jar /opt/shared-finances/shared-finances.jar &
BACKEND_PID=$!

# Nginx in foreground
nginx -g 'daemon off;' &
NGINX_PID=$!

trap shutdown INT TERM

set +e
wait -n "$BACKEND_PID" "$NGINX_PID"
EXIT_CODE=$?
set -e

shutdown
wait "$BACKEND_PID" 2>/dev/null || true
wait "$NGINX_PID" 2>/dev/null || true

exit "$EXIT_CODE"
