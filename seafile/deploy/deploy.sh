#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: deploy.sh /absolute/path/to/seafile.env" >&2
  exit 64
fi

env_file=$1
case "$env_file" in
  /*) ;;
  *) echo "environment file path must be absolute" >&2; exit 64 ;;
esac
test -f "$env_file"

for command_name in docker curl; do
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
      separator = index($0, "=")
      if (separator == 0) next
      name = substr($0, 1, separator - 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) print substr($0, separator + 1)
    }
  ' "$env_file" | tail -n 1
}

require_value() {
  name=$1
  value=$(value_for "$name")
  if [ -z "$value" ] || printf '%s' "$value" | grep -q 'CHANGE_ME'; then
    echo "$name is missing or still contains CHANGE_ME" >&2
    exit 65
  fi
}

for name in \
  SEAFILE_VOLUME \
  SEAFILE_MYSQL_VOLUME \
  SEAFILE_BACKUP_VOLUME \
  INIT_SEAFILE_MYSQL_ROOT_PASSWORD \
  SEAFILE_MYSQL_DB_PASSWORD \
  REDIS_PASSWORD \
  JWT_PRIVATE_KEY \
  INIT_SEAFILE_ADMIN_EMAIL \
  INIT_SEAFILE_ADMIN_PASSWORD \
  SEAFILE_OAUTH_CLIENT_SECRET; do
  require_value "$name"
done

for path_name in SEAFILE_VOLUME SEAFILE_MYSQL_VOLUME SEAFILE_BACKUP_VOLUME; do
  path_value=$(value_for "$path_name")
  case "$path_value" in
    /*) mkdir -p "$path_value" ;;
    *) echo "$path_name must be an absolute path" >&2; exit 65 ;;
  esac
done

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
compose_file="$script_dir/../docker-compose.yml"
compose() {
  docker compose --env-file "$env_file" -f "$compose_file" "$@"
}

compose config --quiet
# Resolve the application image first so an invalid Seafile tag fails before
# downloading or changing any supporting service image.
compose pull seafile
compose pull db redis backup
compose up -d --remove-orphans db redis seafile

http_port=$(value_for SEAFILE_HTTP_PORT)
http_port=${http_port:-18083}
ready=false
attempt=1
while [ "$attempt" -le 60 ]; do
  if curl --fail --silent "http://127.0.0.1:$http_port/" >/dev/null; then
    ready=true
    break
  fi
  attempt=$((attempt + 1))
  sleep 5
done
if [ "$ready" != true ]; then
  compose ps
  compose logs --tail=250 db redis seafile
  echo "Seafile did not become ready within 300 seconds" >&2
  exit 1
fi

"$script_dir/configure-oauth.sh" "$env_file"
compose restart seafile

ready=false
attempt=1
while [ "$attempt" -le 36 ]; do
  if curl --fail --silent "http://127.0.0.1:$http_port/" >/dev/null; then
    ready=true
    break
  fi
  attempt=$((attempt + 1))
  sleep 5
done
if [ "$ready" != true ]; then
  compose logs --tail=250 seafile
  echo "Seafile did not become ready after applying OAuth settings" >&2
  exit 1
fi

compose up -d --remove-orphans backup
compose ps

published_port=$(compose port seafile 80)
case "$published_port" in
  127.0.0.1:*) ;;
  *) echo "refusing deployment: Seafile HTTP must be bound to 127.0.0.1" >&2; exit 1 ;;
esac

echo "Seafile deployment is healthy on 127.0.0.1:$http_port"
