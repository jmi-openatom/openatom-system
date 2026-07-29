#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "usage: render-plan.sh /absolute/path/to/mail.env /absolute/path/to/plan.ndjson" >&2
  exit 64
fi

env_file=$1
output_file=$2
case "$env_file:$output_file" in
  /*:/*) ;;
  *) echo "input and output paths must be absolute" >&2; exit 64 ;;
esac
test -f "$env_file"

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

mail_domain=$(value_for MAIL_DOMAIN)
oauth_issuer=$(value_for MAIL_OAUTH_ISSUER)
oauth_audience=$(value_for MAIL_OAUTH_AUDIENCE)
mail_hostname=$(value_for MAIL_HOSTNAME)
max_message_bytes=$(value_for MAIL_MAX_MESSAGE_BYTES)

mail_domain=${mail_domain:-jmi-openatom.cn}
oauth_issuer=${oauth_issuer:-https://oauth.jmi-openatom.cn/api/v1}
oauth_audience=${oauth_audience:-stalwart}
mail_hostname=${mail_hostname:-mx1.jmi-openatom.cn}
max_message_bytes=${max_message_bytes:-26214400}

case "$mail_domain" in *[!A-Za-z0-9.-]*|.*|*..*|*.) echo "invalid MAIL_DOMAIN" >&2; exit 65 ;; esac
case "$mail_hostname" in *[!A-Za-z0-9.-]*|.*|*..*|*.) echo "invalid MAIL_HOSTNAME" >&2; exit 65 ;; esac
case "$oauth_issuer" in https://*) ;; *) echo "MAIL_OAUTH_ISSUER must use https" >&2; exit 65 ;; esac
case "$oauth_issuer" in *[!A-Za-z0-9:/?._~-]*) echo "MAIL_OAUTH_ISSUER contains unsupported characters" >&2; exit 65 ;; esac
case "$oauth_audience" in ''|*[!A-Za-z0-9._:/-]*) echo "invalid MAIL_OAUTH_AUDIENCE" >&2; exit 65 ;; esac
case "$max_message_bytes" in ''|*[!0-9]*) echo "MAIL_MAX_MESSAGE_BYTES must be an integer" >&2; exit 65 ;; esac

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
template="$script_dir/plan.ndjson.template"
umask 077
awk \
  -v domain="$mail_domain" \
  -v issuer="$oauth_issuer" \
  -v audience="$oauth_audience" \
  -v hostname="$mail_hostname" \
  -v max_bytes="$max_message_bytes" '
    {
      gsub(/__MAIL_DOMAIN__/, domain)
      gsub(/__MAIL_OAUTH_ISSUER__/, issuer)
      gsub(/__MAIL_OAUTH_AUDIENCE__/, audience)
      gsub(/__MAIL_HOSTNAME__/, hostname)
      gsub(/__MAIL_MAX_MESSAGE_BYTES__/, max_bytes)
      print
    }
  ' "$template" > "$output_file"

echo "rendered Stalwart plan: $output_file"
