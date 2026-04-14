#!/usr/bin/env bash
set -Eeuo pipefail

# ── Variables ───────────────────────────────────────────────────────

BACKEND_UPSTREAM_PORT="${BACKEND_UPSTREAM_PORT:-8081}"
SF_APP_PORT="$BACKEND_UPSTREAM_PORT"
ENABLE_TLS="${ENABLE_TLS:-false}"
LETSENCRYPT_DOMAIN="${LETSENCRYPT_DOMAIN:-}"
LETSENCRYPT_EMAIL="${LETSENCRYPT_EMAIL:-}"
JAVA_OPTS="${JAVA_OPTS:-}"

export BACKEND_UPSTREAM_PORT SF_APP_PORT

# ── Folders ──────────────────────────────────────────────────────

mkdir -p /var/www/letsencrypt /etc/nginx/conf.d /run/nginx

# ── Helpers ──────────────────────────────────────────────

apply_http_config() {
  envsubst '${BACKEND_UPSTREAM_PORT}' \
    < /etc/nginx/templates/shared-finances-http.conf.template \
    > /etc/nginx/conf.d/default.conf
}

apply_https_config() {
  envsubst '${BACKEND_UPSTREAM_PORT} ${LETSENCRYPT_DOMAIN}' \
    < /etc/nginx/templates/shared-finances-https.conf.template \
    > /etc/nginx/conf.d/default.conf
}

certs_exist() {
  [[ -f "/etc/letsencrypt/live/$LETSENCRYPT_DOMAIN/fullchain.pem" ]] \
    && [[ -f "/etc/letsencrypt/live/$LETSENCRYPT_DOMAIN/privkey.pem" ]]
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

# ── TLS / Certbot ───────────────────────────────────────────────────

setup_tls() {
  if [[ "$ENABLE_TLS" != "true" ]]; then
    echo "[entrypoint] TLS disabled, using HTTP config"
    apply_http_config
    return
  fi

  if [[ -z "$LETSENCRYPT_DOMAIN" ]]; then
    echo "[entrypoint] WARN: ENABLE_TLS=true but LETSENCRYPT_DOMAIN is empty. Falling back to HTTP."
    apply_http_config
    return
  fi

  # If the certificates already exist (persistent volume), use directly.
  if certs_exist; then
    echo "[entrypoint] Existing certificates found for $LETSENCRYPT_DOMAIN"
    apply_https_config
    return
  fi

  # Certificates do not exist → generate via certbot
  echo "[entrypoint] No certificates found. Starting nginx temporarily for ACME challenge..."

  # Temporarily start nginx with HTTP configuration (for the challenge)
  apply_http_config
  nginx &
  local tmp_nginx_pid=$!
  sleep 2

  echo "[entrypoint] Requesting certificate for $LETSENCRYPT_DOMAIN..."

  local certbot_args=(
    certonly
    --webroot
    --webroot-path /var/www/letsencrypt
    --domain "$LETSENCRYPT_DOMAIN"
    --non-interactive
    --agree-tos
    --no-eff-email
  )

  if [[ -n "$LETSENCRYPT_EMAIL" ]]; then
    certbot_args+=(--email "$LETSENCRYPT_EMAIL")
  else
    certbot_args+=(--register-unsafely-without-email)
  fi

  if certbot "${certbot_args[@]}"; then
    echo "[entrypoint] Certificate obtained successfully"
  else
    echo "[entrypoint] WARN: certbot failed. Continuing with HTTP only."
    nginx -s quit 2>/dev/null || kill "$tmp_nginx_pid" 2>/dev/null || true
    wait_for_stop "$tmp_nginx_pid" 5 || kill -9 "$tmp_nginx_pid" 2>/dev/null || true
    apply_http_config
    return
  fi

  # Temporarily stop nginx and switch to HTTPS configuration.
  nginx -s quit 2>/dev/null || kill "$tmp_nginx_pid" 2>/dev/null || true
  wait_for_stop "$tmp_nginx_pid" 5 || kill -9 "$tmp_nginx_pid" 2>/dev/null || true

  if certs_exist; then
    apply_https_config
  else
    echo "[entrypoint] WARN: Certificates still not found after certbot. Using HTTP."
    apply_http_config
  fi
}

# ── Automatic renewal (background) ──────────────────────────────

start_renewal_timer() {
  if [[ "$ENABLE_TLS" != "true" ]] || [[ -z "$LETSENCRYPT_DOMAIN" ]]; then
    return
  fi

  (
    while true; do
      sleep 43200  # 12 horas
      echo "[entrypoint] Running certbot renew..."
      if certbot renew --quiet --webroot --webroot-path /var/www/letsencrypt; then
        nginx -s reload 2>/dev/null || true
      fi
    done
  ) &
}

# ── Main ────────────────────────────────────────────────────────────

setup_tls

# Backend (Spring Boot)
java $JAVA_OPTS -jar /opt/shared-finances/shared-finances.jar &
BACKEND_PID=$!

# Nginx in foreground
nginx -g 'daemon off;' &
NGINX_PID=$!

# Automatic renewal in the background
start_renewal_timer

trap shutdown INT TERM

set +e
wait -n "$BACKEND_PID" "$NGINX_PID"
EXIT_CODE=$?
set -e

shutdown
wait "$BACKEND_PID" 2>/dev/null || true
wait "$NGINX_PID" 2>/dev/null || true

exit "$EXIT_CODE"