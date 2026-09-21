#!/usr/bin/env bash
set -euo pipefail

APP_DIR=/home/ubuntu/app
STATE_DIR="$APP_DIR/.blue-green"
ACTIVE_COLOR_FILE="$STATE_DIR/active-color"
UPSTREAM_FILE="$APP_DIR/nginx/shared/api-upstream.conf"
HEALTH_TIMEOUT_SECONDS=180

cd "$APP_DIR"
mkdir -p "$STATE_DIR"

if [[ -f "$ACTIVE_COLOR_FILE" ]]; then
  active_color=$(tr -d '[:space:]' < "$ACTIVE_COLOR_FILE")
else
  active_color=legacy
fi

if [[ "$active_color" == "blue" ]]; then
  next_color=green
else
  next_color=blue
fi

next_service="hanyang-api-$next_color"
previous_service="hanyang-api-$active_color"

case "$active_color" in
  blue|green)
    previous_upstream="proxy_pass http://hanyang-api-$active_color:8080;"
    ;;
  legacy)
    previous_upstream='proxy_pass http://hanyang-api:8080;'
    docker rm -f hanyang-api-blue hanyang-api-green >/dev/null 2>&1 || true
    ;;
  *)
    printf 'Unexpected active color: %s\n' "$active_color" >&2
    exit 1
    ;;
esac

printf '%s\n' "$active_color"
printf '%s\n' "$next_color"

docker compose --profile blue-green build "$next_service"
docker compose --profile blue-green up -d --no-deps "$next_service"

deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
until [[ "$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$next_service")" == "healthy" ]]; do
  if [[ $SECONDS -ge $deadline ]]; then
    docker logs --tail 200 "$next_service" || true
    docker compose --profile blue-green stop "$next_service" || true
    exit 1
  fi

  sleep 2
done

printf 'proxy_pass http://hanyang-api-%s:8080;\n' "$next_color" > "$UPSTREAM_FILE"

if ! docker exec nginx nginx -t; then
  printf '%s\n' "$previous_upstream" > "$UPSTREAM_FILE"
  docker compose --profile blue-green stop "$next_service" || true
  exit 1
fi

docker exec nginx nginx -s reload

if ! curl -fsSk --resolve api.hanyang.life:443:127.0.0.1 \
  --max-time 10 https://api.hanyang.life/api/v1/shuttle >/dev/null; then
  printf '%s\n' "$previous_upstream" > "$UPSTREAM_FILE"
  docker exec nginx nginx -t
  docker exec nginx nginx -s reload
  docker compose --profile blue-green stop "$next_service" || true
  exit 1
fi

printf '%s\n' "$next_color" > "$ACTIVE_COLOR_FILE"

if [[ "$active_color" == "blue" || "$active_color" == "green" ]]; then
  docker compose --profile blue-green stop "$previous_service"
  docker compose --profile blue-green rm -f "$previous_service"
else
  docker stop hanyang-api || true
  docker rm hanyang-api || true
fi

docker image prune -f
