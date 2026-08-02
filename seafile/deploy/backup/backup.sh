#!/bin/sh
set -eu

for name in \
  SEAFILE_MYSQL_DB_HOST \
  SEAFILE_MYSQL_DB_USER \
  SEAFILE_MYSQL_DB_PASSWORD \
  SEAFILE_MYSQL_DB_CCNET_DB_NAME \
  SEAFILE_MYSQL_DB_SEAFILE_DB_NAME \
  SEAFILE_MYSQL_DB_SEAHUB_DB_NAME; do
  value=$(printenv "$name" 2>/dev/null || true)
  if [ -z "$value" ]; then
    echo "$name is required" >&2
    exit 65
  fi
done

test -d /source/seafile || {
  echo "/source/seafile is missing" >&2
  exit 66
}
mkdir -p /backups

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
partial_dir="/backups/.partial-$timestamp-$$"
backup_dir="/backups/$timestamp"
mkdir -m 0700 "$partial_dir"

cleanup() {
  rm -rf -- "$partial_dir"
}
trap cleanup EXIT HUP INT TERM
MYSQL_PWD=$SEAFILE_MYSQL_DB_PASSWORD
export MYSQL_PWD

dump_database() {
  database=$1
  output=$2
  mariadb-dump \
    --host="$SEAFILE_MYSQL_DB_HOST" \
    --user="$SEAFILE_MYSQL_DB_USER" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    --events \
    --hex-blob \
    --default-character-set=utf8mb4 \
    "$database" > "$output"
  gzip -9 "$output"
}

# Seafile's documented safe order is databases first, library data second.
dump_database "$SEAFILE_MYSQL_DB_CCNET_DB_NAME" "$partial_dir/ccnet_db.sql"
dump_database "$SEAFILE_MYSQL_DB_SEAFILE_DB_NAME" "$partial_dir/seafile_db.sql"
dump_database "$SEAFILE_MYSQL_DB_SEAHUB_DB_NAME" "$partial_dir/seahub_db.sql"

for data_dir in conf seafile-data seahub-data; do
  test -d "/source/seafile/$data_dir" || {
    echo "/source/seafile/$data_dir is missing" >&2
    exit 66
  }
done
tar -czf "$partial_dir/seafile-data.tar.gz" \
  -C /source/seafile conf seafile-data seahub-data
sha256sum "$partial_dir"/*.gz > "$partial_dir/SHA256SUMS"
mv "$partial_dir" "$backup_dir"
trap - EXIT HUP INT TERM

retention_days=${SEAFILE_BACKUP_RETENTION_DAYS:-14}
case "$retention_days" in
  ''|*[!0-9]*) echo "SEAFILE_BACKUP_RETENTION_DAYS must be a non-negative integer" >&2; exit 65 ;;
esac
if [ "$retention_days" -gt 0 ]; then
  find /backups -mindepth 1 -maxdepth 1 -type d -name '20??????T??????Z' \
    -mtime "+$retention_days" -exec rm -rf -- {} \;
fi

echo "Seafile backup completed: $backup_dir"
