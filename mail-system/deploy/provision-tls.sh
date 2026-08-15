#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: provision-tls.sh /absolute/path/to/mail.env" >&2
  exit 64
fi

env_file=$1
case "$env_file" in /*) ;; *) echo "environment file path must be absolute" >&2; exit 64 ;; esac
case "$env_file" in *[!A-Za-z0-9_./-]*) echo "environment path contains unsupported characters" >&2; exit 64 ;; esac
test -f "$env_file"

for command_name in certbot install nginx; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "$command_name is required" >&2
    exit 69
  }
done

value_for() {
  key=$1
  awk -v wanted="$key" -F= '$1 == wanted { print substr($0, index($0, "=") + 1) }' "$env_file" | tail -n 1
}

acme_email=$(value_for MAIL_ACME_EMAIL)
case "$acme_email" in *@*.*) ;; *) echo "MAIL_ACME_EMAIL is required" >&2; exit 65 ;; esac

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
repository_root=$(CDPATH='' cd -- "$script_dir/../.." && pwd)

# BT Panel (宝塔) detection. Its nginx loads /www/server/panel/vhost/nginx/*.conf
# and its per-site well-known snippet serves challenge files from the site
# document root or /www/wwwroot/java_node_ssl. The scripted server blocks below
# would collide with panel-managed sites (duplicate listen/server_name), so on
# BT panels we only run certbot with a webroot the panel can serve and leave
# nginx configuration to the panel.
if [ -d /www/server/panel/vhost/nginx ]; then
  acme_webroot=/www/wwwroot/java_node_ssl
  nginx_conf_dir=$(value_for NGINX_CONF_DIR)
  nginx_conf_dir=${nginx_conf_dir:-/www/server/panel/vhost/nginx}
  skip_nginx=true
else
  acme_webroot=/var/www/acme
  nginx_conf_dir=$(value_for NGINX_CONF_DIR)
  nginx_conf_dir=${nginx_conf_dir:-/etc/nginx/conf.d}
  skip_nginx=false
fi
case "$nginx_conf_dir" in /*) ;; *) echo "NGINX_CONF_DIR must be absolute" >&2; exit 65 ;; esac

install -d -m 755 "$acme_webroot" "$nginx_conf_dir"
if [ "$skip_nginx" = false ]; then
  if [ ! -f "$nginx_conf_dir/openatom-mail.conf" ]; then
    install -m 644 "$script_dir/nginx/acme-bootstrap.conf" \
      "$nginx_conf_dir/openatom-mail-acme.conf"
  fi
  nginx -t
  nginx -s reload
fi

deploy_hook="$script_dir/stalwart/certbot-deploy-hook.sh $env_file"
certbot certonly --webroot --webroot-path "$acme_webroot" \
  --cert-name openatom-mail \
  --domain mail.jmi-openatom.cn \
  --domain mta-sts.jmi-openatom.cn \
  --domain mx1.jmi-openatom.cn \
  --email "$acme_email" --agree-tos --non-interactive --keep-until-expiring \
  --deploy-hook "$deploy_hook"

"$script_dir/stalwart/certbot-deploy-hook.sh" "$env_file" \
  /etc/letsencrypt/live/openatom-mail

if [ "$skip_nginx" = true ]; then
  echo "BT panel detected: nginx sites stay under panel management; certificates are installed"
  exit 0
fi

rendered_nginx=$(mktemp "${TMPDIR:-/tmp}/openatom-mail-nginx.XXXXXX")
trap 'rm -f "$rendered_nginx"' EXIT HUP INT TERM
sed "s|__OPENATOM_REPOSITORY_ROOT__|$repository_root|g" \
  "$script_dir/nginx/mail.jmi-openatom.cn.conf" > "$rendered_nginx"
install -m 644 "$rendered_nginx" "$nginx_conf_dir/openatom-mail.conf"
rm -f "$nginx_conf_dir/openatom-mail-acme.conf"
if ! nginx -t; then
  rm -f "$nginx_conf_dir/openatom-mail.conf"
  install -m 644 "$script_dir/nginx/acme-bootstrap.conf" \
    "$nginx_conf_dir/openatom-mail-acme.conf"
  nginx -t
  nginx -s reload
  echo "full Nginx configuration failed validation; ACME-only configuration restored" >&2
  exit 1
fi
nginx -s reload
rm -f "$rendered_nginx"
trap - EXIT HUP INT TERM
echo "web and mail TLS certificates are installed and renewal is automated"
