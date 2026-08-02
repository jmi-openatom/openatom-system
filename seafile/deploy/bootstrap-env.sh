#!/bin/sh
set -eu

if [ "$#" -ne 4 ]; then
  echo "usage: bootstrap-env.sh ENV_FILE SHARED_OAUTH_SECRET_FILE DEPLOY_DIR BACKUP_DIR" >&2
  exit 64
fi

env_file=$1
shared_secret_file=$2
deploy_dir=$3
backup_dir=$4
for path_value in "$env_file" "$shared_secret_file" "$deploy_dir" "$backup_dir"; do
  case "$path_value" in
    /*) ;;
    *) echo "all paths must be absolute" >&2; exit 64 ;;
  esac
done

for command_name in awk openssl sed; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "$command_name is required" >&2
    exit 69
  fi
done

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
if [ ! -f "$env_file" ]; then
  install -m 0600 "$script_dir/../.env.production.example" "$env_file"
else
  chmod 0600 "$env_file"
fi

value_for() {
  key=$1
  file=$2
  test -f "$file" || return 0
  awk -v wanted="$key" '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    {
      separator = index($0, "=")
      if (separator == 0) next
      name = substr($0, 1, separator - 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) print substr($0, separator + 1)
    }
  ' "$file" | tail -n 1
}

set_value() {
  key=$1
  value=$2
  file=$3
  temp_file=$(mktemp "${TMPDIR:-/tmp}/openatom-seafile-env.XXXXXX")
  chmod 0600 "$temp_file"
  awk -v wanted="$key" -v replacement="$value" '
    BEGIN { replaced = 0 }
    {
      separator = index($0, "=")
      name = separator ? substr($0, 1, separator - 1) : ""
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) {
        if (!replaced) print wanted "=" replacement
        replaced = 1
      } else {
        print
      }
    }
    END { if (!replaced) print wanted "=" replacement }
  ' "$file" > "$temp_file"
  mv "$temp_file" "$file"
}

ensure_generated() {
  key=$1
  bytes=$2
  current=$(value_for "$key" "$env_file")
  if [ -z "$current" ] || printf '%s' "$current" | grep -q 'CHANGE_ME'; then
    set_value "$key" "$(openssl rand -hex "$bytes")" "$env_file"
  fi
}

mkdir -p "$deploy_dir" "$backup_dir" "$(dirname -- "$shared_secret_file")"
chmod 0700 "$(dirname -- "$shared_secret_file")"
if [ ! -e "$shared_secret_file" ]; then
  temp_secret=$(mktemp "$(dirname -- "$shared_secret_file")/.seafile-oauth.XXXXXX")
  chmod 0600 "$temp_secret"
  openssl rand -hex 48 > "$temp_secret"
  ln "$temp_secret" "$shared_secret_file" 2>/dev/null || true
  rm -f "$temp_secret"
fi
test -s "$shared_secret_file"
chmod 0600 "$shared_secret_file"
oauth_secret=$(tr -d '\r\n' < "$shared_secret_file")
if [ "${#oauth_secret}" -lt 32 ]; then
  echo "shared OAuth secret is invalid" >&2
  exit 65
fi

set_value SEAFILE_VOLUME "$deploy_dir/data" "$env_file"
set_value SEAFILE_MYSQL_VOLUME "$deploy_dir/mysql" "$env_file"
set_value SEAFILE_BACKUP_VOLUME "$backup_dir" "$env_file"
set_value SEAFILE_OAUTH_CLIENT_SECRET "$oauth_secret" "$env_file"

# The CE 14 documentation was published before its production Docker tag.
# Repair the value persisted by an earlier failed bootstrap while preserving
# any other operator-selected image override.
current_image=$(value_for SEAFILE_IMAGE "$env_file")
case "$current_image" in
  ''|*CHANGE_ME*|seafileltd/seafile-mc:14.0-latest)
    set_value SEAFILE_IMAGE seafileltd/seafile-mc:13.0.25 "$env_file"
    ;;
esac

ensure_generated INIT_SEAFILE_MYSQL_ROOT_PASSWORD 32
ensure_generated SEAFILE_MYSQL_DB_PASSWORD 32
ensure_generated REDIS_PASSWORD 32
ensure_generated JWT_PRIVATE_KEY 48
ensure_generated INIT_SEAFILE_ADMIN_PASSWORD 32

mkdir -p "$deploy_dir/data" "$deploy_dir/mysql" "$backup_dir"
echo "Seafile environment initialized; generated secrets were not printed"
