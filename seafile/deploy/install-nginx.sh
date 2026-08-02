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
acme_webroot=/var/www/acme
destination="$nginx_conf_dir/cloud.jmi-openatom.cn.conf"
script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
mkdir -p "$nginx_conf_dir" "$acme_webroot"

render() {
  template=$1
  output=$2
  sed \
    -e "s|__SEAFILE_HTTP_PORT__|$http_port|g" \
    -e "s|__ACME_WEBROOT__|$acme_webroot|g" \
    "$template" > "$output"
}

temp_file=$(mktemp "${TMPDIR:-/tmp}/openatom-seafile-nginx.XXXXXX")
render "$script_dir/nginx/cloud.jmi-openatom.cn.http.conf.template" "$temp_file"
install -m 0644 "$temp_file" "$destination"
"$nginx_bin" -t
"$nginx_bin" -s reload
rm -f "$temp_file"

echo "Nginx HTTP proxy installed for cloud.jmi-openatom.cn; configure SSL separately"
