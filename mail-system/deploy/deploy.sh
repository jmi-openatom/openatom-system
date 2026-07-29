#!/bin/sh
set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "usage: deploy.sh /absolute/path/to/mail.env [auto|bootstrap|tls|full]" >&2
  exit 64
fi

env_file=$1
mode=${2:-full}
case "$env_file" in
  /*) ;;
  *) echo "environment file path must be absolute" >&2; exit 64 ;;
esac
case "$mode" in
  auto|bootstrap|tls|full) ;;
  *) echo "mode must be auto, bootstrap, tls or full" >&2; exit 64 ;;
esac
test -f "$env_file"

for command_name in docker curl openssl; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "$command_name is required" >&2
    exit 69
  fi
done
docker compose version >/dev/null

value_for() {
  key=$1
  awk -v wanted="$key" '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    {
      line = $0
      sub(/^[[:space:]]*export[[:space:]]+/, "", line)
      separator = index(line, "=")
      if (separator == 0) next
      name = substr(line, 1, separator - 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) print substr(line, separator + 1)
    }
  ' "$env_file" | tail -n 1
}

set_value() {
  key=$1
  value=$2
  temp_file=$(mktemp "${TMPDIR:-/tmp}/openatom-mail-env.XXXXXX")
  chmod 600 "$temp_file"
  awk -v wanted="$key" '
    BEGIN { replaced = 0 }
    {
      line = $0
      candidate = line
      sub(/^[[:space:]]*export[[:space:]]+/, "", candidate)
      separator = index(candidate, "=")
      name = separator ? substr(candidate, 1, separator - 1) : ""
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) {
        if (!replaced) print wanted "=__OPENATOM_VALUE__"
        replaced = 1
      } else {
        print line
      }
    }
    END { if (!replaced) print wanted "=__OPENATOM_VALUE__" }
  ' "$env_file" > "$temp_file"
  escaped_value=$(printf '%s' "$value" | sed 's/[&|\\]/\\&/g')
  sed "s|__OPENATOM_VALUE__|$escaped_value|" "$temp_file" > "$temp_file.final"
  chmod 600 "$temp_file.final"
  mv "$temp_file.final" "$env_file"
  rm -f "$temp_file"
}

require_value() {
  variable_name=$1
  variable_value=$(value_for "$variable_name")
  if [ -z "$variable_value" ] || printf '%s' "$variable_value" | grep -q 'CHANGE_ME'; then
    echo "$variable_name is missing or still contains CHANGE_ME" >&2
    exit 65
  fi
}

require_value MAIL_DB_PASSWORD
require_value MAIL_DB_ROOT_PASSWORD
require_value MAIL_REDIS_PASSWORD

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
compose_file="$script_dir/../docker-compose.mail.yml"
tls_host_dir=$(value_for STALWART_TLS_HOST_DIR)
tls_host_dir=${tls_host_dir:-$script_dir/../.runtime/stalwart-tls}
case "$tls_host_dir" in /*) ;; *) tls_host_dir="$script_dir/../$tls_host_dir" ;; esac

if [ "$mode" = bootstrap ]; then
  mkdir -p "$tls_host_dir"
elif [ "$mode" = tls ]; then
  require_value STALWART_CONFIG_TOKEN
  require_value MAIL_ACME_EMAIL
elif [ "$mode" = full ]; then
  require_value MAIL_ADDRESS_SALT
  require_value MAIL_INTERNAL_SERVICE_TOKEN
  require_value MAIL_OAUTH_CLIENT_SECRET
  require_value STALWART_CONFIG_TOKEN
  require_value STALWART_API_TOKEN
  require_value STALWART_DOMAIN_ID
  test -r "$tls_host_dir/fullchain.pem" || {
    echo "Stalwart TLS fullchain.pem is missing; run certbot-deploy-hook.sh first" >&2
    exit 65
  }
  test -r "$tls_host_dir/privkey.pem" || {
    echo "Stalwart TLS privkey.pem is missing; run certbot-deploy-hook.sh first" >&2
    exit 65
  }
  recovery_admin=$(value_for STALWART_RECOVERY_ADMIN)
  recovery_mode=$(value_for STALWART_RECOVERY_MODE)
  if [ -n "$recovery_admin" ] || [ "${recovery_mode:-0}" = "1" ]; then
    echo "full deployment refuses STALWART_RECOVERY_ADMIN and STALWART_RECOVERY_MODE=1" >&2
    exit 65
  fi
else
  require_value MAIL_ADDRESS_SALT
  require_value MAIL_INTERNAL_SERVICE_TOKEN
  require_value MAIL_OAUTH_CLIENT_SECRET
  mkdir -p "$tls_host_dir"
fi

docker compose --env-file "$env_file" -f "$compose_file" config --quiet

bootstrap_stalwart() {
  recovery_admin=$(value_for STALWART_RECOVERY_ADMIN)
  if [ -z "$recovery_admin" ]; then
    recovery_admin="openatom-recovery:$(openssl rand -hex 48)"
    set_value STALWART_RECOVERY_ADMIN "$recovery_admin"
  fi
  set_value STALWART_RECOVERY_MODE 1
  STALWART_RECOVERY_MODE=1 docker compose --env-file "$env_file" -f "$compose_file" \
    up -d --remove-orphans mail-db mail-redis stalwart
  setup_port=$(value_for STALWART_SETUP_PORT)
  setup_port=${setup_port:-18081}
  attempt=1
  until curl --fail --silent "http://127.0.0.1:$setup_port/healthz/ready" >/dev/null; do
    if [ "$attempt" -ge 36 ]; then
      docker compose --env-file "$env_file" -f "$compose_file" logs --tail=200 stalwart
      echo "Stalwart recovery API did not become ready within 180 seconds" >&2
      exit 1
    fi
    attempt=$((attempt + 1))
    sleep 5
  done
  "$script_dir/stalwart/apply-plan.sh" "$env_file" --domain-only
  "$script_dir/stalwart/bootstrap-automation.sh" "$env_file"
  "$script_dir/stalwart/apply-plan.sh" "$env_file"
  docker compose --env-file "$env_file" -f "$compose_file" \
    up -d --remove-orphans stalwart
}

config_token=$(value_for STALWART_CONFIG_TOKEN)
api_token=$(value_for STALWART_API_TOKEN)
domain_id=$(value_for STALWART_DOMAIN_ID)
generated_values_missing=false
if [ -z "$config_token" ] || [ -z "$api_token" ] || [ -z "$domain_id" ] \
    || printf '%s%s%s' "$config_token" "$api_token" "$domain_id" | grep -q 'CHANGE_ME'; then
  generated_values_missing=true
fi
if [ "$mode" = bootstrap ] || { [ "$mode" = auto ] && [ "$generated_values_missing" = true ]; }; then
  bootstrap_stalwart
fi

if [ "$mode" = bootstrap ]; then
  echo "Stalwart infrastructure, least-privilege API keys and domain id are ready"
  exit 0
fi

if [ "$mode" = tls ]; then
  "$script_dir/provision-tls.sh" "$env_file"
  exit 0
fi

if [ "$mode" = auto ] && { [ ! -r "$tls_host_dir/fullchain.pem" ] || [ ! -r "$tls_host_dir/privkey.pem" ]; }; then
  require_value MAIL_ACME_EMAIL
  "$script_dir/provision-tls.sh" "$env_file"
fi

require_value STALWART_CONFIG_TOKEN
require_value STALWART_API_TOKEN
require_value STALWART_DOMAIN_ID
test -r "$tls_host_dir/fullchain.pem" || {
  echo "Stalwart TLS fullchain.pem is missing" >&2
  exit 65
}
test -r "$tls_host_dir/privkey.pem" || {
  echo "Stalwart TLS privkey.pem is missing" >&2
  exit 65
}

docker compose --env-file "$env_file" -f "$compose_file" \
  up -d --remove-orphans mail-db mail-redis stalwart

setup_port=$(value_for STALWART_SETUP_PORT)
setup_port=${setup_port:-18081}
attempt=1
until curl --fail --silent "http://127.0.0.1:$setup_port/healthz/ready" >/dev/null; do
  if [ "$attempt" -ge 36 ]; then
    docker compose --env-file "$env_file" -f "$compose_file" logs --tail=200 stalwart
    echo "Stalwart API did not become ready within 180 seconds" >&2
    exit 1
  fi
  attempt=$((attempt + 1))
  sleep 5
done
"$script_dir/stalwart/apply-plan.sh" "$env_file"
"$script_dir/stalwart/sync-tls-certificate.sh" "$env_file"

docker compose --env-file "$env_file" -f "$compose_file" \
  up -d --build --remove-orphans

api_port=$(value_for MAIL_API_INTERNAL_PORT)
web_port=$(value_for MAIL_WEB_INTERNAL_PORT)
prometheus_port=$(value_for MAIL_PROMETHEUS_INTERNAL_PORT)
alertmanager_port=$(value_for MAIL_ALERTMANAGER_INTERNAL_PORT)
blackbox_port=$(value_for MAIL_BLACKBOX_INTERNAL_PORT)
node_exporter_port=$(value_for MAIL_NODE_EXPORTER_INTERNAL_PORT)
api_port=${api_port:-18090}
web_port=${web_port:-18082}
prometheus_port=${prometheus_port:-19090}
alertmanager_port=${alertmanager_port:-19093}
blackbox_port=${blackbox_port:-19115}
node_exporter_port=${node_exporter_port:-19100}
ready=false
attempt=1
while [ "$attempt" -le 36 ]; do
  if curl --fail --silent "http://127.0.0.1:$api_port/actuator/health/readiness" >/dev/null \
      && curl --fail --silent "http://127.0.0.1:$web_port/" >/dev/null \
      && curl --fail --silent "http://127.0.0.1:$prometheus_port/-/ready" >/dev/null \
      && curl --fail --silent "http://127.0.0.1:$alertmanager_port/-/ready" >/dev/null \
      && curl --fail --silent "http://127.0.0.1:$blackbox_port/-/healthy" >/dev/null \
      && curl --fail --silent "http://127.0.0.1:$node_exporter_port/metrics" >/dev/null; then
    ready=true
    break
  fi
  attempt=$((attempt + 1))
  sleep 5
done

if [ "$ready" != true ]; then
  docker compose --env-file "$env_file" -f "$compose_file" ps
  docker compose --env-file "$env_file" -f "$compose_file" \
    logs --tail=200 mail-api mail-web stalwart
  echo "mail stack did not become ready within 180 seconds" >&2
  exit 1
fi

published_admin=$(docker compose --env-file "$env_file" -f "$compose_file" port stalwart 8080)
case "$published_admin" in
  127.0.0.1:*) ;;
  *) echo "refusing deployment: Stalwart administration is not loopback-only" >&2; exit 1 ;;
esac

for binding in \
  "prometheus 9090" \
  "alertmanager 9093" \
  "blackbox-exporter 9115" \
  "node-exporter 9100"; do
  service_name=${binding% *}
  container_port=${binding#* }
  published_port=$(docker compose --env-file "$env_file" -f "$compose_file" \
    port "$service_name" "$container_port")
  case "$published_port" in
    127.0.0.1:*) ;;
    *) echo "refusing deployment: $service_name is not loopback-only" >&2; exit 1 ;;
  esac
done

docker compose --env-file "$env_file" -f "$compose_file" ps
echo "mail deployment is ready"
