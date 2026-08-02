#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: configure-oauth.sh /absolute/path/to/seafile.env" >&2
  exit 64
fi

env_file=$1
case "$env_file" in
  /*) ;;
  *) echo "environment file path must be absolute" >&2; exit 64 ;;
esac
test -f "$env_file"

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

seafile_volume=$(value_for SEAFILE_VOLUME)
case "$seafile_volume" in
  /*) ;;
  *) echo "SEAFILE_VOLUME must be an absolute path" >&2; exit 65 ;;
esac

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
source_file="$script_dir/../config/openatom_oauth_settings.py"
settings_dir="$seafile_volume/seafile/conf"
settings_file="$settings_dir/seahub_settings.py"
fragment_file="$settings_dir/openatom_oauth_settings.py"

test -f "$settings_file" || {
  echo "Seafile has not generated $settings_file yet" >&2
  exit 66
}

install -m 0640 "$source_file" "$fragment_file"

begin_marker="# BEGIN OPENATOM OAUTH (managed)"
end_marker="# END OPENATOM OAUTH (managed)"
temp_file=$(mktemp "${TMPDIR:-/tmp}/openatom-seafile-settings.XXXXXX")
awk -v begin="$begin_marker" -v end="$end_marker" '
  $0 == begin { managed = 1; next }
  $0 == end { managed = 0; next }
  !managed { print }
' "$settings_file" > "$temp_file"
{
  printf '\n%s\n' "$begin_marker"
  printf '%s\n' "exec(open('/shared/seafile/conf/openatom_oauth_settings.py', encoding='utf-8').read())"
  printf '%s\n' "$end_marker"
} >> "$temp_file"
chmod --reference="$settings_file" "$temp_file" 2>/dev/null || chmod 0640 "$temp_file"
chown --reference="$settings_file" "$temp_file" 2>/dev/null || true
mv "$temp_file" "$settings_file"

echo "OpenAtom OAuth settings installed in $settings_file"
