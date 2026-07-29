#!/bin/sh
set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "usage: certbot-deploy-hook.sh /absolute/path/to/mail.env [/etc/letsencrypt/live/hostname]" >&2
  exit 64
fi

env_file=$1
lineage=${2:-${RENEWED_LINEAGE:-}}
case "$env_file:$lineage" in /*:/*) ;; *) echo "environment and certificate lineage paths must be absolute" >&2; exit 64 ;; esac
test -f "$env_file"
test -r "$lineage/fullchain.pem"
test -r "$lineage/privkey.pem"

value_for() {
  key=$1
  awk -v wanted="$key" -F= '$1 == wanted { print substr($0, index($0, "=") + 1) }' "$env_file" | tail -n 1
}

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
mail_root=$(CDPATH='' cd -- "$script_dir/../.." && pwd)
tls_host_dir=$(value_for STALWART_TLS_HOST_DIR)
tls_host_dir=${tls_host_dir:-$mail_root/.runtime/stalwart-tls}
case "$tls_host_dir" in /*) ;; *) tls_host_dir="$mail_root/$tls_host_dir" ;; esac

install -d -m 700 "$tls_host_dir"
cert_tmp=$(mktemp "$tls_host_dir/.fullchain.XXXXXX")
key_tmp=$(mktemp "$tls_host_dir/.privkey.XXXXXX")
trap 'rm -f "$cert_tmp" "$key_tmp"' EXIT HUP INT TERM
install -m 644 "$lineage/fullchain.pem" "$cert_tmp"
install -m 600 "$lineage/privkey.pem" "$key_tmp"
mv -f "$cert_tmp" "$tls_host_dir/fullchain.pem"
mv -f "$key_tmp" "$tls_host_dir/privkey.pem"
trap - EXIT HUP INT TERM

"$script_dir/sync-tls-certificate.sh" "$env_file"
