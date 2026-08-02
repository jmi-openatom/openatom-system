#!/bin/sh
set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "usage: install-nginx.sh /absolute/path/to/seafile.env [nginx-conf-directory]" >&2
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

nginx_conf_dir=${2:-$(value_for NGINX_CONF_DIR)}
nginx_conf_dir=${nginx_conf_dir:-/www/server/panel/vhost/nginx}
case "$nginx_conf_dir" in
  /*) ;;
  *) echo "nginx config directory must be absolute" >&2; exit 65 ;;
esac

if command -v nginx >/dev/null 2>&1; then
  nginx_bin=$(command -v nginx)
elif [ -x /www/server/nginx/sbin/nginx ]; then
  nginx_bin=/www/server/nginx/sbin/nginx
else
  echo "nginx is required" >&2
  exit 69
fi

http_port=$(value_for SEAFILE_HTTP_PORT)
http_port=${http_port:-18083}
acme_email=$(value_for SEAFILE_ACME_EMAIL)
acme_email=${acme_email:-postmaster@jmi-openatom.cn}
certbot_image=$(value_for SEAFILE_CERTBOT_IMAGE)
certbot_image=${certbot_image:-certbot/certbot:latest}
acme_webroot=/var/www/acme
certificate_dir=/etc/letsencrypt/live/cloud.jmi-openatom.cn
destination="$nginx_conf_dir/cloud.jmi-openatom.cn.conf"
script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
mkdir -p "$nginx_conf_dir" "$acme_webroot"

render() {
  template=$1
  output=$2
  sed \
    -e "s|__SEAFILE_HTTP_PORT__|$http_port|g" \
    -e "s|__ACME_WEBROOT__|$acme_webroot|g" \
    -e "s|__CERTIFICATE_DIR__|$certificate_dir|g" \
    "$template" > "$output"
}

temp_file=$(mktemp "${TMPDIR:-/tmp}/openatom-seafile-nginx.XXXXXX")
render "$script_dir/nginx/cloud.jmi-openatom.cn.http.conf.template" "$temp_file"
install -m 0644 "$temp_file" "$destination"
"$nginx_bin" -t
"$nginx_bin" -s reload

if [ ! -s "$certificate_dir/fullchain.pem" ] || [ ! -s "$certificate_dir/privkey.pem" ]; then
  if command -v certbot >/dev/null 2>&1; then
    certbot certonly \
      --non-interactive \
      --agree-tos \
      --keep-until-expiring \
      --webroot \
      --webroot-path "$acme_webroot" \
      --email "$acme_email" \
      --cert-name cloud.jmi-openatom.cn \
      --domain cloud.jmi-openatom.cn
  else
    if ! command -v docker >/dev/null 2>&1; then
      rm -f "$temp_file"
      echo "certbot or docker is required to provision cloud.jmi-openatom.cn" >&2
      exit 69
    fi
    mkdir -p /etc/letsencrypt /var/lib/letsencrypt
    echo "Host certbot not found; provisioning TLS with $certbot_image"
    docker run --rm \
      --volume "$acme_webroot:/var/www/acme" \
      --volume /etc/letsencrypt:/etc/letsencrypt \
      --volume /var/lib/letsencrypt:/var/lib/letsencrypt \
      "$certbot_image" certonly \
      --non-interactive \
      --agree-tos \
      --keep-until-expiring \
      --webroot \
      --webroot-path /var/www/acme \
      --email "$acme_email" \
      --cert-name cloud.jmi-openatom.cn \
      --domain cloud.jmi-openatom.cn
  fi
fi

render "$script_dir/nginx/cloud.jmi-openatom.cn.https.conf.template" "$temp_file"
install -m 0644 "$temp_file" "$destination"
rm -f "$temp_file"
"$nginx_bin" -t
"$nginx_bin" -s reload

echo "Nginx HTTPS proxy installed for cloud.jmi-openatom.cn"
