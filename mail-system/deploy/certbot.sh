#!/bin/sh
set -eu

if [ "$#" -ne 3 ]; then
  echo "usage: certbot.sh /absolute/path/to/mail.env /absolute/webroot [certonly|renew]" >&2
  exit 64
fi
env_file=$1
webroot=$2
operation=$3
script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
for path in "$env_file" "$webroot" "$script_dir"; do
  case "$path" in /*) ;; *) echo "paths must be absolute" >&2; exit 64 ;; esac
  case "$path" in *[!A-Za-z0-9_./-]*) echo "path contains unsupported characters" >&2; exit 64 ;; esac
done
test -f "$env_file"

if ! command -v nginx >/dev/null 2>&1 && [ -x /www/server/nginx/sbin/nginx ]; then
  PATH="$PATH:/www/server/nginx/sbin"
  export PATH
fi
nginx_bin=$(command -v nginx)
host_hook="$script_dir/stalwart/certbot-deploy-hook.sh $env_file"
deploy_hook="$host_hook && $nginx_bin -t && $nginx_bin -s reload"

case "$operation" in
  certonly)
    acme_email=$(awk -F= '$1 == "MAIL_ACME_EMAIL" { print substr($0, index($0, "=") + 1) }' "$env_file" | tail -n 1)
    case "$acme_email" in *@*.*) ;; *) echo "MAIL_ACME_EMAIL is required" >&2; exit 65 ;; esac
    set -- certonly --webroot --webroot-path "$webroot" \
      --cert-name openatom-mail \
      --domain mail.jmi-openatom.cn --domain mta-sts.jmi-openatom.cn --domain mx1.jmi-openatom.cn \
      --email "$acme_email" --agree-tos --non-interactive --keep-until-expiring
    ;;
  renew) set -- renew --cert-name openatom-mail --non-interactive ;;
  *) echo "operation must be certonly or renew" >&2; exit 64 ;;
esac

if command -v certbot >/dev/null 2>&1; then
  certbot "$@" --deploy-hook "$deploy_hook"
  # --keep-until-expiring skips hooks when an existing certificate is current.
  if [ "$operation" = certonly ]; then
    "$script_dir/stalwart/certbot-deploy-hook.sh" "$env_file" /etc/letsencrypt/live/openatom-mail
    "$nginx_bin" -t
    "$nginx_bin" -s reload
  fi
  exit 0
fi

command -v docker >/dev/null 2>&1 || { echo "certbot or docker is required" >&2; exit 69; }
if [ "$operation" = certonly ]; then
  # Cron runs the host-side hook; no Docker socket or host programs enter the container.
  command -v cron >/dev/null 2>&1 || command -v crond >/dev/null 2>&1 || {
    echo "cron or crond is required for Docker Certbot automatic renewal" >&2
    exit 69
  }
fi
install -d -m 755 /etc/letsencrypt /var/lib/letsencrypt /var/log/letsencrypt "$webroot"
pending=/var/lib/letsencrypt/openatom-mail-deploy-pending
if [ "$operation" = certonly ]; then
  install -d -m 755 /etc/cron.d
  cron_file=$(mktemp /etc/cron.d/.openatom-mail-certbot.XXXXXX)
  trap 'rm -f "$cron_file"' EXIT HUP INT TERM
  {
    echo 'SHELL=/bin/sh'
    echo 'PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/www/server/nginx/sbin'
    printf '23 3,15 * * * root %s/certbot.sh %s %s renew >> /var/log/letsencrypt/openatom-mail-renew.log 2>&1\n' \
      "$script_dir" "$env_file" "$webroot"
  } > "$cron_file"
  chmod 644 "$cron_file"
  mv -f "$cron_file" /etc/cron.d/openatom-mail-certbot
  trap - EXIT HUP INT TERM
  echo "Docker Certbot renewal installed in /etc/cron.d/openatom-mail-certbot"
fi
set -- "$@" --no-directory-hooks --deploy-hook "touch $pending"
if [ "${OPENATOM_MAIL_PREBUILT:-0}" = 1 ]; then
  set -- --pull never certbot/certbot:v5.8.0 "$@"
else
  set -- certbot/certbot:v5.8.0 "$@"
fi
echo "Host certbot is unavailable; using certbot/certbot:v5.8.0"
docker run --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/lib/letsencrypt:/var/lib/letsencrypt \
  -v /var/log/letsencrypt:/var/log/letsencrypt \
  -v "$webroot:$webroot" "$@"

if [ "$operation" = certonly ]; then
  touch "$pending"
fi

# Keep the marker until both consumers have applied the certificate. A failed
# host hook is retried even if Certbot decides no new certificate is due.
if [ "$operation" = certonly ] || [ -f "$pending" ]; then
  "$script_dir/stalwart/certbot-deploy-hook.sh" "$env_file" /etc/letsencrypt/live/openatom-mail
  "$nginx_bin" -t
  "$nginx_bin" -s reload
  rm -f "$pending"
fi
