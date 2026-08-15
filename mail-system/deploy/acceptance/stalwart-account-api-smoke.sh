#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: stalwart-account-api-smoke.sh /absolute/path/to/mail.env" >&2
  exit 64
fi

env_file=$1
case "$env_file" in /*) ;; *) echo "environment file path must be absolute" >&2; exit 64 ;; esac
test -f "$env_file"

for command_name in curl jq; do
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
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) print substr($0, separator + 1)
    }
  ' "$env_file" | tail -n 1
}

api_token=$(value_for STALWART_API_TOKEN)
domain_id=$(value_for STALWART_DOMAIN_ID)
setup_port=$(value_for STALWART_SETUP_PORT)
setup_port=${setup_port:-18081}
case "$api_token" in API_*) ;; *) echo "generated STALWART_API_TOKEN is required" >&2; exit 65 ;; esac
case "$domain_id" in ''|*[!A-Za-z0-9._~-]*) echo "valid STALWART_DOMAIN_ID is required" >&2; exit 65 ;; esac

smoke_suffix=${GITHUB_RUN_ID:-$$}
smoke_name="ci-smoke-$smoke_suffix"
case "$smoke_name" in *[!a-z0-9.-]*) echo "invalid smoke account name" >&2; exit 65 ;; esac

tmp_dir=$(mktemp -d "${TMPDIR:-/tmp}/openatom-stalwart-smoke.XXXXXX")
chmod 700 "$tmp_dir"
trap 'rm -f "$tmp_dir"/*.json; rmdir "$tmp_dir"' EXIT HUP INT TERM

jmap_call() {
  request_file=$1
  response_file=$2
  curl --fail --silent --show-error \
    --connect-timeout 5 --max-time 20 \
    -H "Authorization: Bearer $api_token" \
    -H 'Content-Type: application/json' \
    --data-binary "@$request_file" \
    "http://127.0.0.1:$setup_port/jmap" > "$response_file"
  chmod 600 "$response_file"
  if ! jq -e '
      (.methodResponses | type == "array") and
      (.methodResponses | length == 1) and
      (.methodResponses[0][0] != "error")
    ' "$response_file" >/dev/null; then
    echo "Stalwart management API returned a JMAP method error" >&2
    exit 1
  fi
}

jq -n \
  --arg name "$smoke_name" \
  --arg domainId "$domain_id" \
  '{
    using: ["urn:ietf:params:jmap:core", "urn:stalwart:jmap"],
    methodCalls: [["x:Account/set", {
      create: {smoke: {
        "@type": "User",
        name: $name,
        domainId: $domainId,
        description: "OpenAtom CI account API smoke test",
        credentials: {},
        memberGroupIds: {},
        roles: {"@type": "User"},
        permissions: {"@type": "Inherit"},
        quotas: {maxDiskQuota: 1048576},
        aliases: {},
        encryptionAtRest: {"@type": "Disabled"}
      }}
    }, "create"]]
  }' > "$tmp_dir/create.json"
jmap_call "$tmp_dir/create.json" "$tmp_dir/create-response.json"
account_id=$(jq -r '.methodResponses[0][1].created.smoke.id // empty' "$tmp_dir/create-response.json")
case "$account_id" in ''|*[!A-Za-z0-9._~-]*) echo "Stalwart did not create the smoke account" >&2; exit 1 ;; esac

jq -n \
  --arg name "$smoke_name" \
  --arg domainId "$domain_id" \
  '{
    using: ["urn:ietf:params:jmap:core", "urn:stalwart:jmap"],
    methodCalls: [["x:Account/query", {
      filter: {name: $name, domainId: $domainId}, limit: 2
    }, "query"]]
  }' > "$tmp_dir/query.json"
jmap_call "$tmp_dir/query.json" "$tmp_dir/query-response.json"
if ! jq -e --arg id "$account_id" \
    '.methodResponses[0][1].ids == [$id]' "$tmp_dir/query-response.json" >/dev/null; then
  echo "Stalwart account query did not return exactly the created account" >&2
  exit 1
fi

jq -n \
  --arg id "$account_id" \
  --arg domainId "$domain_id" \
  '{
    using: ["urn:ietf:params:jmap:core", "urn:stalwart:jmap"],
    methodCalls: [["x:Account/set", {
      update: {($id): {
        aliases: {"0": {name: "ci-smoke-alias", domainId: $domainId, enabled: true}},
        permissions: {"@type": "Replace", enabledPermissions: {}, disabledPermissions: {}}
      }}
    }, "update"]]
  }' > "$tmp_dir/update.json"
jmap_call "$tmp_dir/update.json" "$tmp_dir/update-response.json"
if ! jq -e --arg id "$account_id" \
    '(.methodResponses[0][1].updated | has($id)) and
     (.methodResponses[0][1].notUpdated[$id] == null)' \
    "$tmp_dir/update-response.json" >/dev/null; then
  echo "Stalwart did not update aliases or suspension permissions" >&2
  exit 1
fi

echo "PASS: least-privilege account API token created, queried and updated a Stalwart account"
