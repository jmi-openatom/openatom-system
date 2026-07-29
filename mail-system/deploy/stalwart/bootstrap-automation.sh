#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: bootstrap-automation.sh /absolute/path/to/mail.env" >&2
  exit 64
fi

env_file=$1
case "$env_file" in /*) ;; *) echo "environment file path must be absolute" >&2; exit 64 ;; esac
test -f "$env_file"

for command_name in docker openssl awk sed; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "$command_name is required" >&2
    exit 69
  }
done

value_for() {
  key=$1
  awk -v wanted="$key" '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    {
      line = $0
      sub(/^[[:space:]]*export[[:space:]]+/, "", line)
      separator = index(line, "=")
      if (separator == 0) next
      name = substr(line, 1, separator - 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) print substr(line, separator + 1)
    }
  ' "$env_file" | tail -n 1
}

set_value() {
  key=$1
  value=$2
  temp_file=$(mktemp "${TMPDIR:-/tmp}/openatom-mail-env.XXXXXX")
  chmod 600 "$temp_file"
  awk -v wanted="$key" '
    BEGIN { replaced = 0 }
    {
      line = $0
      candidate = line
      sub(/^[[:space:]]*export[[:space:]]+/, "", candidate)
      separator = index(candidate, "=")
      name = separator ? substr(candidate, 1, separator - 1) : ""
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) {
        if (!replaced) print wanted "=__OPENATOM_VALUE__"
        replaced = 1
      } else {
        print line
      }
    }
    END { if (!replaced) print wanted "=__OPENATOM_VALUE__" }
  ' "$env_file" > "$temp_file"
  escaped_value=$(printf '%s' "$value" | sed 's/[&|\\]/\\&/g')
  sed "s|__OPENATOM_VALUE__|$escaped_value|" "$temp_file" > "$temp_file.final"
  chmod 600 "$temp_file.final"
  mv "$temp_file.final" "$env_file"
  rm -f "$temp_file"
}

config_token=$(value_for STALWART_CONFIG_TOKEN)
api_token=$(value_for STALWART_API_TOKEN)
domain_id=$(value_for STALWART_DOMAIN_ID)
recovery_admin=$(value_for STALWART_RECOVERY_ADMIN)
recovery_mode=$(value_for STALWART_RECOVERY_MODE)
if [ -n "$config_token" ] && [ -n "$api_token" ] && [ -n "$domain_id" ] \
    && ! printf '%s%s%s' "$config_token" "$api_token" "$domain_id" | grep -q 'CHANGE_ME' \
    && [ -z "$recovery_admin" ] && [ "${recovery_mode:-0}" = "0" ]; then
  echo "Stalwart automation credentials already exist; bootstrap skipped"
  exit 0
fi

case "$recovery_admin" in *:*) ;; *) echo "STALWART_RECOVERY_ADMIN must use user:password format" >&2; exit 65 ;; esac
recovery_user=${recovery_admin%%:*}
recovery_password=${recovery_admin#*:}
test -n "$recovery_user"
test -n "$recovery_password"

mail_domain=$(value_for MAIL_DOMAIN)
mail_domain=${mail_domain:-jmi-openatom.cn}
automation_name=$(value_for STALWART_AUTOMATION_ACCOUNT)
automation_name=${automation_name:-openatom-automation}
cli_image=$(value_for STALWART_CLI_IMAGE)
cli_image=${cli_image:-ghcr.io/stalwartlabs/cli:latest}
network_name=$(value_for STALWART_DOCKER_NETWORK)
network_name=${network_name:-openatom-mail_mail-internal}

runtime_dir=$(mktemp -d "${TMPDIR:-/tmp}/openatom-stalwart-bootstrap.XXXXXX")
chmod 700 "$runtime_dir"
cleanup() {
  rm -f "$runtime_dir"/* 2>/dev/null || true
  rmdir "$runtime_dir" 2>/dev/null || true
}
trap cleanup EXIT HUP INT TERM

write_auth_file() {
  auth_file=$1
  auth_kind=$2
  auth_identity=$3
  auth_secret=$4
  {
    printf 'STALWART_URL=http://stalwart:8080\n'
    printf 'NO_COLOR=1\n'
    if [ "$auth_kind" = token ]; then
      printf 'STALWART_TOKEN=%s\n' "$auth_secret"
    else
      printf 'STALWART_USER=%s\n' "$auth_identity"
      printf 'STALWART_PASSWORD=%s\n' "$auth_secret"
    fi
  } > "$auth_file"
  chmod 600 "$auth_file"
}

recovery_auth="$runtime_dir/recovery.env"
automation_auth="$runtime_dir/automation.env"
config_auth="$runtime_dir/config.env"
api_auth="$runtime_dir/api.env"
write_auth_file "$recovery_auth" basic "$recovery_user" "$recovery_password"

run_cli() {
  auth_file=$1
  shift
  docker run --rm --network "$network_name" --env-file "$auth_file" \
    "$cli_image" "$@"
}

domain_records=$(run_cli "$recovery_auth" query Domain \
  --where "name=$mail_domain" --fields id,name --json)
domain_id=$(printf '%s\n' "$domain_records" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
case "$domain_id" in
  ''|*[!A-Za-z0-9._~-]*)
    echo "could not uniquely resolve Stalwart domain id for $mail_domain" >&2
    exit 65
    ;;
esac
if [ "$(printf '%s\n' "$domain_records" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | wc -l | tr -d ' ')" != 1 ]; then
  echo "expected exactly one Stalwart domain named $mail_domain" >&2
  exit 65
fi

temporary_password=$(openssl rand -hex 48)
account_records=$(run_cli "$recovery_auth" query Account \
  --where "name=$automation_name" --where "domainId=$domain_id" \
  --fields id,name,domainId --json)
automation_account_id=$(printf '%s\n' "$account_records" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

password_json="$runtime_dir/password.json"
printf '{"0":{"@type":"Password","secret":"%s"}}\n' "$temporary_password" > "$password_json"
chmod 600 "$password_json"

if [ -z "$automation_account_id" ]; then
  account_json="$runtime_dir/account.json"
  printf '{"@type":"User","name":"%s","domainId":"%s","description":"OpenAtom deployment automation; no mailbox login","credentials":{"0":{"@type":"Password","secret":"%s"}},"memberGroupIds":{},"roles":{"@type":"Admin"},"permissions":{"@type":"Inherit"},"quotas":{},"aliases":{},"encryptionAtRest":{"@type":"Disabled"}}\n' \
    "$automation_name" "$domain_id" "$temporary_password" > "$account_json"
  chmod 600 "$account_json"
  docker run --rm --network "$network_name" --env-file "$recovery_auth" \
    -v "$account_json:/work/account.json:ro" "$cli_image" \
    create Account --file /work/account.json >/dev/null
else
  case "$automation_account_id" in *[!A-Za-z0-9._~-]*) echo "invalid automation account id" >&2; exit 65 ;; esac
  credentials_value=$(tr -d '\n' < "$password_json")
  run_cli "$recovery_auth" update Account "$automation_account_id" \
    --field "credentials=$credentials_value" \
    --field 'roles={"@type":"Admin"}' \
    --field 'permissions={"@type":"Inherit"}' >/dev/null
fi

account_records=$(run_cli "$recovery_auth" query Account \
  --where "name=$automation_name" --where "domainId=$domain_id" \
  --fields id,name,domainId --json)
automation_account_id=$(printf '%s\n' "$account_records" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
case "$automation_account_id" in ''|*[!A-Za-z0-9._~-]*) echo "could not resolve automation account" >&2; exit 65 ;; esac

run_cli "$recovery_auth" create Action --json '{"@type":"InvalidateCaches"}' >/dev/null
write_auth_file "$automation_auth" basic "$automation_name@$mail_domain" "$temporary_password"

# This account exists only to own these two keys. Remove stale keys on an
# interrupted retry, then capture each newly generated one exactly once.
existing_key_ids=$(run_cli "$automation_auth" query ApiKey --json | sed -n 's/^"\([^"]*\)"$/\1/p')
for key_id in $existing_key_ids; do
  case "$key_id" in *[!A-Za-z0-9._~-]*) echo "invalid stale API key id" >&2; exit 65 ;; esac
  run_cli "$automation_auth" delete ApiKey --ids "$key_id" >/dev/null
done

config_permissions='{"@type":"Replace","permissions":["sysDirectoryGet","sysDirectoryQuery","sysDirectoryCreate","sysDirectoryUpdate","sysDomainGet","sysDomainQuery","sysDomainCreate","sysDomainUpdate","sysMtaInboundThrottleGet","sysMtaInboundThrottleQuery","sysMtaInboundThrottleCreate","sysMtaInboundThrottleUpdate","sysCertificateGet","sysCertificateQuery","sysCertificateCreate","sysCertificateUpdate","sysAuthenticationUpdate","sysSystemSettingsUpdate","sysMtaStageAuthUpdate","sysMtaStageRcptUpdate","sysMtaInboundSessionUpdate","sysMtaStageDataUpdate","sysMtaOutboundStrategyUpdate","sysMetricsUpdate","sysJmapUpdate","sysEmailUpdate"]}'
api_permissions='{"@type":"Replace","permissions":["sysAccountGet","sysAccountQuery","sysAccountCreate","sysAccountUpdate"]}'

config_output=$(run_cli "$automation_auth" create ApiKey \
  --field description='OpenAtom configuration automation' \
  --field "permissions=$config_permissions" --field 'allowedIps={}')
config_token=$(printf '%s\n' "$config_output" | sed -n 's/^[[:space:]]*Secret:[[:space:]]*//p')

api_output=$(run_cli "$automation_auth" create ApiKey \
  --field description='OpenAtom account provisioning' \
  --field "permissions=$api_permissions" --field 'allowedIps={}')
api_token=$(printf '%s\n' "$api_output" | sed -n 's/^[[:space:]]*Secret:[[:space:]]*//p')

case "$config_token" in API_*) ;; *) echo "Stalwart did not return the configuration API key" >&2; exit 65 ;; esac
case "$api_token" in API_*) ;; *) echo "Stalwart did not return the account API key" >&2; exit 65 ;; esac

write_auth_file "$config_auth" token '' "$config_token"
write_auth_file "$api_auth" token '' "$api_token"
run_cli "$config_auth" query Domain --where "name=$mail_domain" --fields id --json >/dev/null
run_cli "$api_auth" query Account --where "domainId=$domain_id" --fields id --json >/dev/null

# Persist the generated state before revoking the temporary password. If the
# process is interrupted before cleanup, recovery mode remains available and a
# retry can rotate the keys safely instead of losing the only usable secret.
set_value STALWART_CONFIG_TOKEN "$config_token"
set_value STALWART_API_TOKEN "$api_token"
set_value STALWART_DOMAIN_ID "$domain_id"

# Remove the temporary password while retaining the two API key credentials.
run_cli "$recovery_auth" update Account "$automation_account_id" \
  --field 'credentials/0=null' >/dev/null

set_value STALWART_RECOVERY_MODE 0
set_value STALWART_RECOVERY_ADMIN ''
chmod 600 "$env_file"

echo "Stalwart configuration token, account token and domain id were generated automatically"
