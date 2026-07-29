#!/bin/sh
set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "usage: apply-plan.sh /absolute/path/to/mail.env [--dry-run|--domain-only]" >&2
  exit 64
fi

env_file=$1
apply_mode=${2:-apply}
case "$env_file" in /*) ;; *) echo "environment file path must be absolute" >&2; exit 64 ;; esac
case "$apply_mode" in apply|--dry-run|--domain-only) ;; *) echo "second argument must be --dry-run or --domain-only" >&2; exit 64 ;; esac
test -f "$env_file"
command -v docker >/dev/null 2>&1

value_for() {
  key=$1
  awk -v wanted="$key" '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    {
      separator = index($0, "=")
      if (separator == 0) next
      name = substr($0, 1, separator - 1)
      sub(/^[[:space:]]*export[[:space:]]+/, "", name)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) print substr($0, separator + 1)
    }
  ' "$env_file" | tail -n 1
}

config_token=$(value_for STALWART_CONFIG_TOKEN)
recovery_admin=$(value_for STALWART_RECOVERY_ADMIN)
if [ -z "$config_token" ] && [ -z "$recovery_admin" ]; then
  echo "STALWART_CONFIG_TOKEN or STALWART_RECOVERY_ADMIN is required" >&2
  exit 65
fi

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
plan_file=$(mktemp "${TMPDIR:-/tmp}/openatom-stalwart-plan.XXXXXX")
auth_file=$(mktemp "${TMPDIR:-/tmp}/openatom-stalwart-auth.XXXXXX")
chmod 600 "$auth_file"
trap 'rm -f "$plan_file" "$auth_file"' EXIT HUP INT TERM
"$script_dir/render-plan.sh" "$env_file" "$plan_file"
if [ "$apply_mode" = "--domain-only" ]; then
  domain_plan="$plan_file.domain"
  awk '/"object":"Domain"/' "$plan_file" > "$domain_plan"
  test -s "$domain_plan"
  mv "$domain_plan" "$plan_file"
fi
# The plan contains no credentials. Docker preserves the runner's numeric file
# ownership on the bind mount, while the CLI image runs as a different UID, so
# it needs an explicit read bit inside the container.
chmod 644 "$plan_file"

cli_image=$(value_for STALWART_CLI_IMAGE)
cli_image=${cli_image:-ghcr.io/stalwartlabs/cli:latest}
network_name=$(value_for STALWART_DOCKER_NETWORK)
network_name=${network_name:-openatom-mail_mail-internal}

if [ -n "$config_token" ]; then
  {
    printf 'STALWART_URL=http://stalwart:8080\n'
    printf 'STALWART_TOKEN=%s\n' "$config_token"
    printf 'NO_COLOR=1\n'
  } > "$auth_file"
else
  case "$recovery_admin" in *:*) ;; *) echo "STALWART_RECOVERY_ADMIN must use user:password format" >&2; exit 65 ;; esac
  recovery_user=${recovery_admin%%:*}
  recovery_password=${recovery_admin#*:}
  test -n "$recovery_user"
  test -n "$recovery_password"
  {
    printf 'STALWART_URL=http://stalwart:8080\n'
    printf 'STALWART_USER=%s\n' "$recovery_user"
    printf 'STALWART_PASSWORD=%s\n' "$recovery_password"
    printf 'NO_COLOR=1\n'
  } > "$auth_file"
fi

set -- docker run --rm \
  --network "$network_name" \
  --env-file "$auth_file" \
  -v "$plan_file:/work/plan.ndjson:ro" \
  -w /work \
  "$cli_image" apply --file /work/plan.ndjson --json
if [ "$apply_mode" = "--dry-run" ]; then
  set -- "$@" --dry-run
fi
"$@"
