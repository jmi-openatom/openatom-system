#!/bin/sh
set -eu

interval=${SEAFILE_BACKUP_INTERVAL_SECONDS:-86400}
case "$interval" in
  ''|*[!0-9]*) echo "SEAFILE_BACKUP_INTERVAL_SECONDS must be a positive integer" >&2; exit 65 ;;
esac
if [ "$interval" -lt 3600 ]; then
  echo "SEAFILE_BACKUP_INTERVAL_SECONDS must be at least 3600" >&2
  exit 65
fi

while :; do
  if ! /scripts/backup.sh; then
    echo "Seafile backup failed; retrying at the next interval" >&2
  fi
  sleep "$interval"
done
