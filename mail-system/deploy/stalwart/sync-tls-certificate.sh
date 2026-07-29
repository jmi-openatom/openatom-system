#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: sync-tls-certificate.sh /absolute/path/to/mail.env" >&2
  exit 64
fi

env_file=$1
case "$env_file" in /*) ;; *) echo "environment file path must be absolute" >&2; exit 64 ;; esac
test -f "$env_file"

for command_name in docker openssl sed; do
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
      separator = index($0, "=")
      if (separator == 0) next
      name = substr($0, 1, separator - 1)
      sub(/^[[:space:]]*export[[:space:]]+/, "", name)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) print substr($0, separator + 1)
    }
  ' "$env_file" | tail -n 1
}

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
mail_root=$(CDPATH= cd -- "$script_dir/../.." && pwd)
tls_host_dir=$(value_for STALWART_TLS_HOST_DIR)
tls_host_dir=${tls_host_dir:-$mail_root/.runtime/stalwart-tls}
case "$tls_host_dir" in /*) ;; *) tls_host_dir="$mail_root/$tls_host_dir" ;; esac
cert_file="$tls_host_dir/fullchain.pem"
key_file="$tls_host_dir/privkey.pem"
test -r "$cert_file"
test -r "$key_file"

mail_hostname=$(value_for MAIL_HOSTNAME)
mail_hostname=${mail_hostname:-mx1.jmi-openatom.cn}
openssl x509 -in "$cert_file" -noout -checkhost "$mail_hostname" >/dev/null
openssl x509 -in "$cert_file" -noout -checkend 1209600 >/dev/null

cert_public=$(openssl x509 -in "$cert_file" -pubkey -noout | openssl pkey -pubin -outform DER 2>/dev/null | openssl sha256)
key_public=$(openssl pkey -in "$key_file" -pubout -outform DER 2>/dev/null | openssl sha256)
if [ "$cert_public" != "$key_public" ]; then
  echo "TLS certificate and private key do not match" >&2
  exit 65
fi

config_token=$(value_for STALWART_CONFIG_TOKEN)
recovery_admin=$(value_for STALWART_RECOVERY_ADMIN)
if [ -z "$config_token" ] && [ -z "$recovery_admin" ]; then
  echo "STALWART_CONFIG_TOKEN or STALWART_RECOVERY_ADMIN is required" >&2
  exit 65
fi
cli_image=$(value_for STALWART_CLI_IMAGE)
cli_image=${cli_image:-ghcr.io/stalwartlabs/cli:latest}
network_name=$(value_for STALWART_DOCKER_NETWORK)
network_name=${network_name:-openatom-mail_mail-internal}
auth_file=$(mktemp "${TMPDIR:-/tmp}/openatom-stalwart-tls-auth.XXXXXX")
chmod 600 "$auth_file"
trap 'rm -f "$auth_file"' EXIT HUP INT TERM

if [ -n "$config_token" ]; then
  {
    printf 'STALWART_URL=http://stalwart:8080\n'
    printf 'STALWART_TOKEN=%s\n' "$config_token"
    printf 'NO_COLOR=1\n'
  } > "$auth_file"
else
  case "$recovery_admin" in *:*) ;; *) echo "invalid STALWART_RECOVERY_ADMIN" >&2; exit 65 ;; esac
  {
    printf 'STALWART_URL=http://stalwart:8080\n'
    printf 'STALWART_USER=%s\n' "${recovery_admin%%:*}"
    printf 'STALWART_PASSWORD=%s\n' "${recovery_admin#*:}"
    printf 'NO_COLOR=1\n'
  } > "$auth_file"
fi

stalwart_cli() {
  docker run --rm --network "$network_name" --env-file "$auth_file" \
    "$cli_image" "$@"
}

query_output=$(stalwart_cli query Certificate \
  --where "subjectAlternativeNames=$mail_hostname" --json)
certificate_id=$(printf '%s\n' "$query_output" | sed -n '1{s/^"//;s/"$//;p;}')
second_id=$(printf '%s\n' "$query_output" | sed -n '2p')
if [ -n "$second_id" ]; then
  echo "multiple Stalwart certificates match $mail_hostname; refusing ambiguous update" >&2
  exit 65
fi

certificate_json='{"certificate":{"@type":"File","filePath":"/run/stalwart-tls/fullchain.pem"},"privateKey":{"@type":"File","filePath":"/run/stalwart-tls/privkey.pem"}}'
if [ -n "$certificate_id" ]; then
  stalwart_cli update Certificate "$certificate_id" --json "$certificate_json"
else
  stalwart_cli create Certificate --json "$certificate_json"
  query_output=$(stalwart_cli query Certificate \
    --where "subjectAlternativeNames=$mail_hostname" --json)
  certificate_id=$(printf '%s\n' "$query_output" | sed -n '1{s/^"//;s/"$//;p;}')
fi

case "$certificate_id" in ''|*[!A-Za-z0-9._~-]*)
  echo "could not resolve the installed Stalwart certificate id" >&2
  exit 65
esac

stalwart_cli update SystemSettings singleton \
  --json "{\"defaultCertificateId\":\"$certificate_id\"}"
echo "Stalwart TLS certificate synchronized for $mail_hostname"
