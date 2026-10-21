#!/bin/sh
# One-shot destructive operation, invoked only by an explicit manual deployment.
set -eu

if [ "$#" -ne 2 ] || [ "$2" != --confirm-delete-mail-data ]; then
  echo 'usage: reset-data.sh /absolute/path/to/mail.env --confirm-delete-mail-data' >&2
  exit 64
fi
env_file=$1
case "$env_file" in /*) ;; *) echo 'environment path must be absolute' >&2; exit 64 ;; esac
test -f "$env_file"
script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
compose_file="$script_dir/../docker-compose.mail.yml"

compose() {
  docker compose -p openatom-mail --env-file "$env_file" -f "$compose_file" "$@"
}

# Validate dependencies and volume ownership before stopping anything. Never use
# `down -v` or prune: those can remove unrelated volumes or cached signatures.
compose config --quiet
for image in $(compose config --images | sort -u); do
  docker image inspect "$image" >/dev/null
done
volumes='mail_db_data mail_redis_data stalwart_etc stalwart_data'
for logical_name in $volumes; do
  volume="openatom-mail_$logical_name"
  labels=$(docker volume inspect --format '{{ index .Labels "com.docker.compose.project" }}|{{ index .Labels "com.docker.compose.volume" }}' "$volume")
  if [ "$labels" != "openatom-mail|$logical_name" ]; then
    echo "Refusing to remove volume with unexpected ownership: $volume" >&2
    exit 65
  fi
done

# Stop the public API before deleting either account metadata or message data.
compose stop mail-web mail-api
compose rm --stop --force mail-web mail-api stalwart mail-db mail-redis
for logical_name in $volumes; do
  docker volume rm "openatom-mail_$logical_name" >/dev/null
done

# Only Stalwart-generated bootstrap state is discarded. Shared OAuth secrets,
# database passwords, certificate files and all main-site data stay in place.
temp_file=$(mktemp "${TMPDIR:-/tmp}/openatom-mail-reset.XXXXXX")
chmod 600 "$temp_file"
trap 'rm -f "$temp_file"' EXIT HUP INT TERM
awk '
  {
    candidate = $0
    sub(/^[[:space:]]*export[[:space:]]+/, "", candidate)
    split(candidate, parts, "=")
    key = parts[1]
    gsub(/^[[:space:]]+|[[:space:]]+$/, "", key)
    if (key !~ /^(MAIL_DOMAIN|STALWART_CONFIG_TOKEN|STALWART_API_TOKEN|STALWART_DOMAIN_ID|STALWART_RECOVERY_ADMIN|STALWART_RECOVERY_MODE)$/) print
  }
  END {
    print "MAIL_DOMAIN=mailer.jmi-openatom.cn"
    print "STALWART_RECOVERY_MODE=0"
  }
' "$env_file" > "$temp_file"
cat "$temp_file" > "$env_file"
chmod 600 "$env_file"
echo 'Mail account database, sessions and Stalwart data cleared; deploy in auto mode to rebuild.'
