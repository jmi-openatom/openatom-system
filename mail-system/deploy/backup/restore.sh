#!/bin/sh
set -eu

if [ "${CONFIRM_MAIL_RESTORE:-}" != "RESTORE" ]; then
  echo "refusing restore: set CONFIRM_MAIL_RESTORE=RESTORE after verifying the target" >&2
  exit 65
fi
if [ "$#" -ne 3 ] || [ ! -d "$1" ]; then
  echo "usage: CONFIRM_MAIL_RESTORE=RESTORE restore.sh /absolute/backup/timestamp /absolute/path/to/mail.env /absolute/path/to/age-identity.txt" >&2
  exit 64
fi

backup_dir=$1
env_file=$2
identity_file=$3
case "$backup_dir" in
  /*) ;;
  *) echo "backup directory must be absolute" >&2; exit 64 ;;
esac
case "$env_file:$identity_file" in
  /*:/*) ;;
  *) echo "environment and identity paths must be absolute" >&2; exit 64 ;;
esac
test -f "$env_file"
test -f "$identity_file"
test -f "$backup_dir/openatom_mail.sql.age"
test -f "$backup_dir/stalwart.tar.gz.age"

for command_name in age docker sha256sum; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "$command_name is required" >&2
    exit 69
  fi
done

(cd "$backup_dir" && sha256sum -c SHA256SUMS)

compose_file=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)/docker-compose.mail.yml
restore_tmp=$(mktemp -d "${TMPDIR:-/tmp}/openatom-mail-restore.XXXXXX")
trap 'rm -f "$restore_tmp/openatom_mail.sql" "$restore_tmp/stalwart.tar.gz"; rmdir "$restore_tmp"' EXIT HUP INT TERM
age --decrypt --identity "$identity_file" --output "$restore_tmp/openatom_mail.sql" \
  "$backup_dir/openatom_mail.sql.age"
age --decrypt --identity "$identity_file" --output "$restore_tmp/stalwart.tar.gz" \
  "$backup_dir/stalwart.tar.gz.age"

docker compose --env-file "$env_file" -f "$compose_file" stop mail-web mail-api stalwart

docker run --rm \
  -v openatom-mail_stalwart_etc:/target/etc \
  -v openatom-mail_stalwart_data:/target/data \
  -v "$restore_tmp":/restore:ro \
  alpine:3.21 sh -c 'find /target/etc /target/data -mindepth 1 -delete && tar -xzf /restore/stalwart.tar.gz -C /target'

docker compose --env-file "$env_file" -f "$compose_file" exec -T mail-db \
  sh -c 'exec mysql -u root -p"$MYSQL_ROOT_PASSWORD" openatom_mail' \
  < "$restore_tmp/openatom_mail.sql"

docker compose --env-file "$env_file" -f "$compose_file" start stalwart mail-api mail-web
echo "restore completed; run the restore drill checklist before reopening traffic"
