#!/bin/sh
set -eu

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
migration_dir="$script_dir/../src/main/resources/db/migration"
test -d "$migration_dir"

versions=$(
  find "$migration_dir" -maxdepth 1 -type f -name 'V*__*.sql' -exec basename {} \; \
    | sed -n 's/^V\([^_][^_]*\)__.*/\1/p' \
    | sort
)
duplicates=$(printf '%s\n' "$versions" | uniq -d)

if [ -n "$duplicates" ]; then
  echo "duplicate Flyway migration version(s):" >&2
  printf '%s\n' "$duplicates" | while IFS= read -r version; do
    find "$migration_dir" -maxdepth 1 -type f -name "V${version}__*.sql" \
      -exec basename {} \; | sort >&2
  done
  exit 1
fi

echo "Flyway migration versions are unique"
