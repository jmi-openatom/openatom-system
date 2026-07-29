#!/bin/sh
set -u

mail_domain=${MAIL_DOMAIN:-jmi-openatom.cn}
mail_hostname=${MAIL_HOSTNAME:-mx1.jmi-openatom.cn}
mail_web_host=${MAIL_WEB_HOST:-mail.jmi-openatom.cn}
oauth_issuer=${MAIL_OAUTH_ISSUER:-https://oauth.jmi-openatom.cn/api/v1}
dkim_selector=${DKIM_SELECTOR:-stalwart}
expected_ip=${EXPECTED_PUBLIC_IP:-}

pass_count=0
fail_count=0

pass() {
  pass_count=$((pass_count + 1))
  printf 'PASS  %s\n' "$1"
}

fail() {
  fail_count=$((fail_count + 1))
  printf 'FAIL  %s\n' "$1" >&2
}

need_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf '%s is required\n' "$1" >&2
    exit 69
  fi
}

for command_name in curl dig jq nc openssl swaks; do
  need_command "$command_name"
done

tmp_dir=$(mktemp -d "${TMPDIR:-/tmp}/openatom-mail-preflight.XXXXXX")
trap 'rm -f "$tmp_dir/discovery.json" "$tmp_dir/jwks.json" "$tmp_dir/tls.pem"; rmdir "$tmp_dir"' EXIT HUP INT TERM

a_records=$(dig +short A "$mail_hostname")
if [ -n "$a_records" ]; then
  pass "$mail_hostname has an A record: $(printf '%s' "$a_records" | tr '\n' ' ')"
else
  fail "$mail_hostname has no A record"
fi

if [ -n "$expected_ip" ]; then
  if printf '%s\n' "$a_records" | grep -Fxq "$expected_ip"; then
    pass "$mail_hostname resolves to EXPECTED_PUBLIC_IP"
  else
    fail "$mail_hostname does not resolve to EXPECTED_PUBLIC_IP=$expected_ip"
  fi
fi

mx_hosts=$(dig +short MX "$mail_domain" | awk '{print $2}' | sed 's/\.$//')
if printf '%s\n' "$mx_hosts" | grep -Fxq "$mail_hostname"; then
  pass "$mail_domain MX points to $mail_hostname"
else
  fail "$mail_domain MX does not point to $mail_hostname"
fi

ptr_ip=$expected_ip
if [ -z "$ptr_ip" ]; then
  ptr_ip=$(printf '%s\n' "$a_records" | sed -n '1p')
fi
if [ -n "$ptr_ip" ]; then
  ptr_name=$(dig +short -x "$ptr_ip" | sed -n '1{s/\.$//;p;}')
  if [ "$ptr_name" = "$mail_hostname" ]; then
    pass "$ptr_ip PTR points back to $mail_hostname"
  else
    fail "$ptr_ip PTR is '${ptr_name:-missing}', expected $mail_hostname"
  fi
fi

spf=$(dig +short TXT "$mail_domain" | tr -d '"' | grep 'v=spf1' || true)
[ -n "$spf" ] && pass "SPF record exists" || fail "SPF record is missing"

dkim=$(dig +short TXT "$dkim_selector._domainkey.$mail_domain" | tr -d '"' | grep 'v=DKIM1' || true)
[ -n "$dkim" ] && pass "DKIM record exists for selector $dkim_selector" || fail "DKIM record is missing for selector $dkim_selector"

dmarc=$(dig +short TXT "_dmarc.$mail_domain" | tr -d '"' | grep 'v=DMARC1' || true)
[ -n "$dmarc" ] && pass "DMARC record exists" || fail "DMARC record is missing"

mta_sts=$(dig +short TXT "_mta-sts.$mail_domain" | tr -d '"' | grep 'v=STSv1' || true)
[ -n "$mta_sts" ] && pass "MTA-STS DNS record exists" || fail "MTA-STS DNS record is missing"

tls_rpt=$(dig +short TXT "_smtp._tls.$mail_domain" | tr -d '"' | grep 'v=TLSRPTv1' || true)
[ -n "$tls_rpt" ] && pass "TLS-RPT DNS record exists" || fail "TLS-RPT DNS record is missing"

if curl --fail --silent --show-error --proto '=https' --tlsv1.2 \
    "https://$mail_web_host/" >/dev/null; then
  pass "mail website HTTPS is reachable"
else
  fail "mail website HTTPS is not reachable"
fi

if curl --fail --silent --show-error --proto '=https' --tlsv1.2 \
    "https://mta-sts.$mail_domain/.well-known/mta-sts.txt" \
    | grep -q '^mode: enforce'; then
  pass "MTA-STS policy is reachable and enforced"
else
  fail "MTA-STS policy is missing or not in enforce mode"
fi

discovery_url="${oauth_issuer%/}/.well-known/openid-configuration"
if curl --fail --silent --show-error "$discovery_url" -o "$tmp_dir/discovery.json" \
    && jq -e --arg issuer "$oauth_issuer" '.issuer == $issuer and (.jwks_uri | type == "string")' \
      "$tmp_dir/discovery.json" >/dev/null; then
  pass "OIDC discovery issuer and JWKS URI are valid"
  jwks_uri=$(jq -r '.jwks_uri' "$tmp_dir/discovery.json")
  if curl --fail --silent --show-error "$jwks_uri" -o "$tmp_dir/jwks.json" \
      && jq -e '
        (.keys | length > 0) and
        all(.keys[];
          .kty == "RSA" and has("n") and has("e") and has("kid") and
          (has("d") | not) and (has("k") | not) and
          (has("p") | not) and (has("q") | not)
        )
      ' "$tmp_dir/jwks.json" >/dev/null; then
    pass "JWKS contains public RSA keys only"
  else
    fail "JWKS is unavailable, empty, non-RSA, or exposes private key material"
  fi
else
  fail "OIDC discovery is unavailable or issuer does not match exactly"
fi

for mail_port in 25 465 587 993; do
  if nc -z -w 5 "$mail_hostname" "$mail_port" >/dev/null 2>&1; then
    pass "$mail_hostname:$mail_port is reachable"
  else
    fail "$mail_hostname:$mail_port is not reachable"
  fi
done

for admin_port in 8080 18081 19090 19093 19100 19115; do
  if nc -z -w 5 "$mail_hostname" "$admin_port" >/dev/null 2>&1; then
    fail "management port $mail_hostname:$admin_port is exposed publicly"
  else
    pass "management port $mail_hostname:$admin_port is not publicly reachable"
  fi
done

if openssl s_client -starttls smtp -connect "$mail_hostname:25" \
    -servername "$mail_hostname" </dev/null 2>/dev/null > "$tmp_dir/tls.pem" \
    && openssl x509 -in "$tmp_dir/tls.pem" -noout -checkend 1209600 >/dev/null 2>&1; then
  pass "SMTP STARTTLS certificate is valid for at least 14 days"
else
  fail "SMTP STARTTLS certificate is unavailable or expires within 14 days"
fi

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if "$script_dir/open-relay-check.sh" "$mail_hostname"; then
  pass "Open Relay check"
else
  fail "Open Relay check"
fi

printf '\nResult: %s passed, %s failed\n' "$pass_count" "$fail_count"
[ "$fail_count" -eq 0 ]
