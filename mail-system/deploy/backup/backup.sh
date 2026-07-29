#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "usage: backup.sh /absolute/backup/directory /absolute/path/to/mail.env" >&2
  exit 64
fi

backup_root=$1
env_file=$2
case "$backup_root" in
  /*) ;;
  *) echo "backup directory must be absolute" >&2; exit 64 ;;
esac
case "$env_file" in
  /*) ;;
  *) echo "environment file path must be absolute" >&2; exit 64 ;;
esac
test -f "$env_file"

for command_name in age docker sha256sum; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "$command_name is required" >&2
    exit 69
  fi
done

backup_recipient=$(awk -F= '
  $1 == "MAIL_BACKUP_AGE_RECIPIENT" { print substr($0, index($0, "=") + 1) }
' "$env_file" | tail -n 1)
case "$backup_recipient" in
  age1*) ;;
  *) echo "MAIL_BACKUP_AGE_RECIPIENT must contain an age recipient" >&2; exit 65 ;;
esac

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_dir="$backup_root/$timestamp"
mkdir -p "$backup_dir"

compose_file=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)/docker-compose.mail.yml
sql_file="$backup_dir/openatom_mail.sql"
stalwart_file="$backup_dir/stalwart.tar.gz"
stalwart_stopped=false

cleanup() {
  rm -f "$sql_file" "$stalwart_file"
  if [ "$stalwart_stopped" = true ]; then
    docker compose --env-file "$env_file" -f "$compose_file" start stalwart >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT HUP INT TERM

docker compose --env-file "$env_file" -f "$compose_file" exec -T mail-db \
  sh -c 'exec mysqldump --single-transaction --routines --triggers -u root -p"$MYSQL_ROOT_PASSWORD" openatom_mail' \
  > "$sql_file"

# Stalwart local stores require a filesystem-consistent snapshot. This short stop is explicit.
docker compose --env-file "$env_file" -f "$compose_file" stop stalwart
stalwart_stopped=true
docker run --rm \
  -v openatom-mail_stalwart_etc:/source/etc:ro \
  -v openatom-mail_stalwart_data:/source/data:ro \
  -v "$backup_dir":/backup \
  alpine:3.21 tar -czf /backup/stalwart.tar.gz -C /source etc data
docker compose --env-file "$env_file" -f "$compose_file" start stalwart
stalwart_stopped=false

age --encrypt --recipient "$backup_recipient" \
  --output "$backup_dir/openatom_mail.sql.age" "$sql_file"
age --encrypt --recipient "$backup_recipient" \
  --output "$backup_dir/stalwart.tar.gz.age" "$stalwart_file"
rm -f "$sql_file" "$stalwart_file"

sha256sum "$backup_dir/openatom_mail.sql.age" "$backup_dir/stalwart.tar.gz.age" \
  > "$backup_dir/SHA256SUMS"
trap - EXIT HUP INT TERM
echo "$backup_dir"
