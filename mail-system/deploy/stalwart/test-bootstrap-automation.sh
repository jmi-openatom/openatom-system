#!/bin/sh
set -eu

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
test_dir=$(mktemp -d "${TMPDIR:-/tmp}/openatom-bootstrap-test.XXXXXX")
cleanup() {
  rm -f "$test_dir/bin/curl" "$test_dir/bin/docker" "$test_dir/mail.env"
  rm -f "$test_dir/state/commands.log" "$test_dir/state/api-key-count"
  rm -f "$test_dir/state/automation.ndjson"
  rmdir "$test_dir/bin" "$test_dir/state" "$test_dir" 2>/dev/null || true
}
trap cleanup EXIT HUP INT TERM

mkdir "$test_dir/bin" "$test_dir/state"
ln -s "$script_dir/test-fixtures/curl" "$test_dir/bin/curl"
ln -s "$script_dir/test-fixtures/docker" "$test_dir/bin/docker"
env_file="$test_dir/mail.env"
{
  printf 'STALWART_RECOVERY_ADMIN=admin:test-recovery-password\n'
  printf 'STALWART_RECOVERY_MODE=1\n'
  printf 'MAIL_DOMAIN=jmi-openatom.cn\n'
  printf 'MAIL_HOSTNAME=mx1.jmi-openatom.cn\n'
  printf 'MAIL_OAUTH_ISSUER=https://oauth.jmi-openatom.cn/api/v1\n'
  printf 'MAIL_OAUTH_AUDIENCE=stalwart\n'
  printf 'STALWART_AUTOMATION_ACCOUNT=openatom-automation\n'
} > "$env_file"
chmod 600 "$env_file"

PATH="$test_dir/bin:$PATH" FAKE_DOCKER_STATE_DIR="$test_dir/state" \
  "$script_dir/bootstrap-automation.sh" "$env_file"

grep -q '^STALWART_CONFIG_TOKEN=API_config-test-secret$' "$env_file"
grep -q '^STALWART_API_TOKEN=API_account-test-secret$' "$env_file"
grep -q '^STALWART_DOMAIN_ID=domain-id$' "$env_file"
grep -q '^STALWART_RECOVERY_MODE=0$' "$env_file"
grep -q '^STALWART_RECOVERY_ADMIN=$' "$env_file"
test "$(find "$env_file" -perm 600 -print)" = "$env_file"

ruby -rjson -e '
  lines = File.readlines(ARGV.fetch(0)).map { |line| JSON.parse(line) }
  abort "expected Domain followed by Account" unless lines.map { |line| line["object"] } == %w[Domain Account]
  account = lines.fetch(1)
  abort "account upsert is not idempotent" unless account["@type"] == "upsert" && account["matchOn"] == %w[name domainId]
  value = account.fetch("value").fetch("openatom-automation")
  abort "domain reference missing" unless value["domainId"] == "#openatom-domain"
  abort "automation account is not admin" unless value["roles"] == { "@type" => "Admin" }
  abort "temporary password missing" unless value.dig("credentials", "0", "secret")&.match?(/\A[0-9a-f]{96}\z/)
' "$test_dir/state/automation.ndjson"

if grep -q '^recovery|.* query ' "$test_dir/state/commands.log"; then
  echo "recovery credentials must not query directory objects" >&2
  exit 1
fi
grep -q '^automation|.* query Domain ' "$test_dir/state/commands.log"
grep -q '^automation|.* query Account ' "$test_dir/state/commands.log"
grep -q '^config|.* query Domain ' "$test_dir/state/commands.log"
grep -q '^api|.* query Account ' "$test_dir/state/commands.log"
test "$(grep -c '^compose|.* up -d --force-recreate stalwart|mode=0|admin=' "$test_dir/state/commands.log")" = 2
test "$(grep -c '|mode=0|admin=set$' "$test_dir/state/commands.log")" = 1
test "$(grep -c '|mode=0|admin=empty$' "$test_dir/state/commands.log")" = 1
normal_restart_line=$(grep -n '|mode=0|admin=set$' "$test_dir/state/commands.log" | cut -d: -f1)
automation_query_line=$(grep -n '^automation|.* query Domain ' "$test_dir/state/commands.log" | cut -d: -f1)
test "$normal_restart_line" -lt "$automation_query_line"

env_state_before=$(cksum "$env_file")
PATH="$test_dir/bin:$PATH" FAKE_DOCKER_STATE_DIR="$test_dir/state" \
  "$script_dir/bootstrap-automation.sh" "$env_file"
env_state_after=$(cksum "$env_file")
test "$env_state_before" = "$env_state_after"
test "$(grep -c '^compose|.* up -d --force-recreate stalwart|mode=0|admin=' "$test_dir/state/commands.log")" = 3
test "$(grep -c '|mode=0|admin=empty$' "$test_dir/state/commands.log")" = 2

echo "bootstrap automation regression test passed"
