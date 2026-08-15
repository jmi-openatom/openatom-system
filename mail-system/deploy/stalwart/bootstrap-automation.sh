#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: bootstrap-automation.sh /absolute/path/to/mail.env" >&2
  exit 64
fi

env_file=$1
case "$env_file" in /*) ;; *) echo "environment file path must be absolute" >&2; exit 64 ;; esac
test -f "$env_file"

for command_name in docker curl openssl awk sed; do
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

set_value() {
  key=$1
  value=$2
  temp_file=$(mktemp "${TMPDIR:-/tmp}/openatom-mail-env.XXXXXX")
  chmod 600 "$temp_file"
  awk -v wanted="$key" '
    BEGIN { replaced = 0 }
    {
      line = $0
      candidate = line
      sub(/^[[:space:]]*export[[:space:]]+/, "", candidate)
      separator = index(candidate, "=")
      name = separator ? substr(candidate, 1, separator - 1) : ""
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name == wanted) {
        if (!replaced) print wanted "=__OPENATOM_VALUE__"
        replaced = 1
      } else {
        print line
      }
    }
    END { if (!replaced) print wanted "=__OPENATOM_VALUE__" }
  ' "$env_file" > "$temp_file"
  escaped_value=$(printf '%s' "$value" | sed 's/[&|\\]/\\&/g')
  sed "s|__OPENATOM_VALUE__|$escaped_value|" "$temp_file" > "$temp_file.final"
  chmod 600 "$temp_file.final"
  mv "$temp_file.final" "$env_file"
  rm -f "$temp_file"
}

config_token=$(value_for STALWART_CONFIG_TOKEN)
api_token=$(value_for STALWART_API_TOKEN)
domain_id=$(value_for STALWART_DOMAIN_ID)
recovery_admin=$(value_for STALWART_RECOVERY_ADMIN)
recovery_mode=$(value_for STALWART_RECOVERY_MODE)
script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
compose_file="$script_dir/../../docker-compose.mail.yml"
setup_port=$(value_for STALWART_SETUP_PORT)
setup_port=${setup_port:-18081}
docker compose version >/dev/null

restart_stalwart() {
  requested_recovery_admin=$1
  STALWART_RECOVERY_MODE=0 STALWART_RECOVERY_ADMIN="$requested_recovery_admin" \
    docker compose --env-file "$env_file" -f "$compose_file" \
    up -d --force-recreate stalwart
}

wait_for_stalwart() {
  attempt=1
  until curl --fail --silent "http://127.0.0.1:$setup_port/healthz/ready" >/dev/null; do
    if [ "$attempt" -ge 36 ]; then
      docker compose --env-file "$env_file" -f "$compose_file" logs --tail=200 stalwart
      echo "Stalwart API did not become ready within 180 seconds" >&2
      exit 1
    fi
    attempt=$((attempt + 1))
    sleep 5
  done
}

if [ -n "$config_token" ] && [ -n "$api_token" ] && [ -n "$domain_id" ] \
    && ! printf '%s%s%s' "$config_token" "$api_token" "$domain_id" | grep -q 'CHANGE_ME' \
    && [ -z "$recovery_admin" ] && [ "${recovery_mode:-0}" = "0" ]; then
  # Converge the running container as well as the environment file. This also
  # recovers safely if a previous run persisted credentials but was interrupted
  # while removing the recovery administrator from the container.
  restart_stalwart ''
  wait_for_stalwart
  echo "Stalwart automation credentials already exist; bootstrap skipped"
  exit 0
fi

case "$recovery_admin" in *:*) ;; *) echo "STALWART_RECOVERY_ADMIN must use user:password format" >&2; exit 65 ;; esac
recovery_user=${recovery_admin%%:*}
recovery_password=${recovery_admin#*:}
test -n "$recovery_user"
test -n "$recovery_password"

mail_domain=$(value_for MAIL_DOMAIN)
mail_domain=${mail_domain:-jmi-openatom.cn}
automation_name=$(value_for STALWART_AUTOMATION_ACCOUNT)
automation_name=${automation_name:-openatom-automation}
case "$mail_domain" in *[!A-Za-z0-9.-]*|.*|*..*|*.) echo "invalid MAIL_DOMAIN" >&2; exit 65 ;; esac
case "$automation_name" in ''|*[!a-z0-9.-]*|.*|*..*|*.) echo "invalid STALWART_AUTOMATION_ACCOUNT" >&2; exit 65 ;; esac
cli_image=$(value_for STALWART_CLI_IMAGE)
cli_image=${cli_image:-ghcr.io/stalwartlabs/cli:latest}
network_name=$(value_for STALWART_DOCKER_NETWORK)
network_name=${network_name:-openatom-mail_mail-internal}

runtime_dir=$(mktemp -d "${TMPDIR:-/tmp}/openatom-stalwart-bootstrap.XXXXXX")
chmod 700 "$runtime_dir"
cleanup() {
  rm -f "$runtime_dir"/* 2>/dev/null || true
  rmdir "$runtime_dir" 2>/dev/null || true
}
trap cleanup EXIT HUP INT TERM

write_auth_file() {
  auth_file=$1
  auth_kind=$2
  auth_identity=$3
  auth_secret=$4
  {
    printf 'STALWART_URL=http://stalwart:8080\n'
    printf 'NO_COLOR=1\n'
    if [ "$auth_kind" = token ]; then
      printf 'STALWART_TOKEN=%s\n' "$auth_secret"
    else
      printf 'STALWART_USER=%s\n' "$auth_identity"
      printf 'STALWART_PASSWORD=%s\n' "$auth_secret"
    fi
  } > "$auth_file"
  chmod 600 "$auth_file"
}

recovery_auth="$runtime_dir/recovery.env"
automation_auth="$runtime_dir/automation.env"
config_auth="$runtime_dir/config.env"
api_auth="$runtime_dir/api.env"
write_auth_file "$recovery_auth" basic "$recovery_user" "$recovery_password"

run_cli() {
  auth_file=$1
  shift
  docker run --rm --network "$network_name" --env-file "$auth_file" \
    "$cli_image" "$@"
}

temporary_password=$(openssl rand -hex 48)
rendered_plan="$runtime_dir/rendered.ndjson"
automation_plan="$runtime_dir/automation.ndjson"
"$script_dir/render-plan.sh" "$env_file" "$rendered_plan" >/dev/null
# Normal mode needs its singleton defaults and service configuration before the
# first restart. Keep OIDC out of this bootstrap plan so the temporary local
# administrator can authenticate long enough to generate the API keys. The
# regular full apply enables OIDC immediately after bootstrap completes.
awk '!/"object":"Directory"/ && !/"object":"Authentication"/' \
  "$rendered_plan" > "$automation_plan"
printf '{"@type":"upsert","object":"Account","matchOn":["name","domainId"],"value":{"openatom-automation":{"@type":"User","name":"%s","domainId":"#openatom-domain","description":"OpenAtom deployment automation; no mailbox login","credentials":{"0":{"@type":"Password","secret":"%s"}},"memberGroupIds":{},"roles":{"@type":"Admin"},"permissions":{"@type":"Inherit"},"quotas":{},"aliases":{},"encryptionAtRest":{"@type":"Disabled"}}}}\n' \
  "$automation_name" "$temporary_password" >> "$automation_plan"
test -s "$automation_plan"
# This file contains a temporary password, but its parent directory is 0700.
# The file needs a read bit for the non-root UID in the direct CLI bind mount.
chmod 644 "$automation_plan"
docker run --rm --network "$network_name" --env-file "$recovery_auth" \
  -v "$automation_plan:/work/automation.ndjson:ro" -w /work "$cli_image" \
  apply --file /work/automation.ndjson --json >/dev/null

# Make a newly created or password-updated account visible before its first
# authentication. Recovery credentials are used only for this one-shot action.
run_cli "$recovery_auth" create Action --json '{"@type":"InvalidateCaches"}' >/dev/null

# Recovery mode only authorizes the built-in recovery principal. Recreate the
# server in normal mode before the newly created directory administrator makes
# any management request. Keep the recovery credential until both API keys are
# generated and verified so an interrupted run remains recoverable.
set_value STALWART_RECOVERY_MODE 0
restart_stalwart "$recovery_admin"
wait_for_stalwart
write_auth_file "$automation_auth" basic "$automation_name@$mail_domain" "$temporary_password"

# ID-only queries avoid the additional Object/get call that `--fields` causes.
# In normal mode the recovery principal can resolve the server-assigned ids,
# while the directory administrator remains responsible for API-key lifecycle.
# The CLI prints one JSON object per line for objects that define a default
# column list (such as Domain) and one bare quoted id per line otherwise
# (such as Account). Extract ids from either representation.
extract_cli_ids() {
  sed -n -e 's/^"\([^"]*\)"$/\1/p' -e 's/.*"id":"\([^"]*\)".*/\1/p' \
    | sed '/^$/d'
}
domain_records=$(run_cli "$recovery_auth" query Domain \
  --where "name=$mail_domain" --json)
domain_ids=$(printf '%s\n' "$domain_records" | extract_cli_ids)
domain_id=$(printf '%s\n' "$domain_ids" | tail -n 1)
case "$domain_id" in
  ''|*[!A-Za-z0-9._~-]*)
    echo "could not uniquely resolve Stalwart domain id for $mail_domain" >&2
    exit 65
    ;;
esac
if [ "$(printf '%s\n' "$domain_ids" | wc -l | tr -d ' ')" != 1 ]; then
  echo "expected exactly one Stalwart domain named $mail_domain" >&2
  exit 65
fi

account_records=$(run_cli "$recovery_auth" query Account \
  --where "name=$automation_name" --where "domainId=$domain_id" \
  --json)
automation_account_id=$(printf '%s\n' "$account_records" | extract_cli_ids | tail -n 1)
case "$automation_account_id" in ''|*[!A-Za-z0-9._~-]*) echo "could not resolve automation account" >&2; exit 65 ;; esac
if [ "$(printf '%s\n' "$account_records" | extract_cli_ids | wc -l | tr -d ' ')" != 1 ]; then
  echo "expected exactly one Stalwart automation account" >&2
  exit 65
fi

# This account exists only to own these two keys. Remove stale keys on an
# interrupted retry, then capture each newly generated one exactly once.
existing_key_ids=$(run_cli "$automation_auth" query ApiKey --json | sed -n 's/^"\([^"]*\)"$/\1/p')
for key_id in $existing_key_ids; do
  case "$key_id" in *[!A-Za-z0-9._~-]*) echo "invalid stale API key id" >&2; exit 65 ;; esac
  run_cli "$automation_auth" delete ApiKey --ids "$key_id" >/dev/null
done

config_permissions='{"@type":"Replace","permissions":{"authenticate":true,"sysDirectoryGet":true,"sysDirectoryQuery":true,"sysDirectoryCreate":true,"sysDirectoryUpdate":true,"sysDomainGet":true,"sysDomainQuery":true,"sysDomainCreate":true,"sysDomainUpdate":true,"sysMtaInboundThrottleGet":true,"sysMtaInboundThrottleQuery":true,"sysMtaInboundThrottleCreate":true,"sysMtaInboundThrottleUpdate":true,"sysCertificateGet":true,"sysCertificateQuery":true,"sysCertificateCreate":true,"sysCertificateUpdate":true,"sysAuthenticationUpdate":true,"sysSystemSettingsUpdate":true,"sysMtaStageAuthUpdate":true,"sysMtaStageRcptUpdate":true,"sysMtaInboundSessionUpdate":true,"sysMtaStageDataUpdate":true,"sysMtaOutboundStrategyUpdate":true,"sysMetricsUpdate":true,"sysJmapUpdate":true,"sysEmailUpdate":true}}'
api_permissions='{"@type":"Replace","permissions":{"authenticate":true,"authenticateWithAlias":true,"calendarAlarmsSend":true,"calendarSchedulingReceive":true,"calendarSchedulingSend":true,"davCalAcl":true,"davCalCopy":true,"davCalDelete":true,"davCalFreeBusyQuery":true,"davCalGet":true,"davCalLock":true,"davCalMkCol":true,"davCalMove":true,"davCalMultiGet":true,"davCalPropFind":true,"davCalPropPatch":true,"davCalPut":true,"davCalQuery":true,"davCardAcl":true,"davCardCopy":true,"davCardDelete":true,"davCardGet":true,"davCardLock":true,"davCardMkCol":true,"davCardMove":true,"davCardMultiGet":true,"davCardPropFind":true,"davCardPropPatch":true,"davCardPut":true,"davCardQuery":true,"davExpandProperty":true,"davFileAcl":true,"davFileCopy":true,"davFileDelete":true,"davFileGet":true,"davFileLock":true,"davFileMkCol":true,"davFileMove":true,"davFilePropFind":true,"davFilePropPatch":true,"davFilePut":true,"davPrincipalAcl":true,"davPrincipalList":true,"davPrincipalMatch":true,"davPrincipalSearch":true,"davPrincipalSearchPropSet":true,"davSyncCollection":true,"emailReceive":true,"emailSend":true,"imapAclGet":true,"imapAclSet":true,"imapAppend":true,"imapAuthenticate":true,"imapCapability":true,"imapCopy":true,"imapCreate":true,"imapDelete":true,"imapEnable":true,"imapExamine":true,"imapExpunge":true,"imapFetch":true,"imapId":true,"imapIdle":true,"imapList":true,"imapListRights":true,"imapLsub":true,"imapMove":true,"imapMyRights":true,"imapNamespace":true,"imapRename":true,"imapSearch":true,"imapSelect":true,"imapSort":true,"imapStatus":true,"imapStore":true,"imapSubscribe":true,"imapThread":true,"interactAi":true,"jmapAddressBookChanges":true,"jmapAddressBookCreate":true,"jmapAddressBookDestroy":true,"jmapAddressBookGet":true,"jmapAddressBookUpdate":true,"jmapBlobCopy":true,"jmapBlobGet":true,"jmapBlobLookup":true,"jmapBlobUpload":true,"jmapCalendarChanges":true,"jmapCalendarCreate":true,"jmapCalendarDestroy":true,"jmapCalendarEventChanges":true,"jmapCalendarEventCopy":true,"jmapCalendarEventCreate":true,"jmapCalendarEventDestroy":true,"jmapCalendarEventGet":true,"jmapCalendarEventNotificationChanges":true,"jmapCalendarEventNotificationCreate":true,"jmapCalendarEventNotificationDestroy":true,"jmapCalendarEventNotificationGet":true,"jmapCalendarEventNotificationQuery":true,"jmapCalendarEventNotificationQueryChanges":true,"jmapCalendarEventNotificationUpdate":true,"jmapCalendarEventParse":true,"jmapCalendarEventQuery":true,"jmapCalendarEventQueryChanges":true,"jmapCalendarEventUpdate":true,"jmapCalendarGet":true,"jmapCalendarUpdate":true,"jmapContactCardChanges":true,"jmapContactCardCopy":true,"jmapContactCardCreate":true,"jmapContactCardDestroy":true,"jmapContactCardGet":true,"jmapContactCardParse":true,"jmapContactCardQuery":true,"jmapContactCardQueryChanges":true,"jmapContactCardUpdate":true,"jmapCoreEcho":true,"jmapEmailChanges":true,"jmapEmailCopy":true,"jmapEmailCreate":true,"jmapEmailDestroy":true,"jmapEmailGet":true,"jmapEmailImport":true,"jmapEmailParse":true,"jmapEmailQuery":true,"jmapEmailQueryChanges":true,"jmapEmailSubmissionChanges":true,"jmapEmailSubmissionCreate":true,"jmapEmailSubmissionDestroy":true,"jmapEmailSubmissionGet":true,"jmapEmailSubmissionQuery":true,"jmapEmailSubmissionQueryChanges":true,"jmapEmailSubmissionUpdate":true,"jmapEmailUpdate":true,"jmapFileNodeChanges":true,"jmapFileNodeCopy":true,"jmapFileNodeCreate":true,"jmapFileNodeDestroy":true,"jmapFileNodeGet":true,"jmapFileNodeQuery":true,"jmapFileNodeQueryChanges":true,"jmapFileNodeUpdate":true,"jmapIdentityChanges":true,"jmapIdentityCreate":true,"jmapIdentityDestroy":true,"jmapIdentityGet":true,"jmapIdentityUpdate":true,"jmapMailboxChanges":true,"jmapMailboxCreate":true,"jmapMailboxDestroy":true,"jmapMailboxGet":true,"jmapMailboxQuery":true,"jmapMailboxQueryChanges":true,"jmapMailboxUpdate":true,"jmapParticipantIdentityChanges":true,"jmapParticipantIdentityCreate":true,"jmapParticipantIdentityDestroy":true,"jmapParticipantIdentityGet":true,"jmapParticipantIdentityUpdate":true,"jmapPrincipalChanges":true,"jmapPrincipalCreate":true,"jmapPrincipalDestroy":true,"jmapPrincipalGet":true,"jmapPrincipalGetAvailability":true,"jmapPrincipalQuery":true,"jmapPrincipalQueryChanges":true,"jmapPrincipalUpdate":true,"jmapPushSubscriptionCreate":true,"jmapPushSubscriptionDestroy":true,"jmapPushSubscriptionGet":true,"jmapPushSubscriptionUpdate":true,"jmapQuotaChanges":true,"jmapQuotaGet":true,"jmapQuotaQuery":true,"jmapQuotaQueryChanges":true,"jmapSearchSnippetGet":true,"jmapShareNotificationChanges":true,"jmapShareNotificationCreate":true,"jmapShareNotificationDestroy":true,"jmapShareNotificationGet":true,"jmapShareNotificationQuery":true,"jmapShareNotificationQueryChanges":true,"jmapShareNotificationUpdate":true,"jmapSieveScriptCreate":true,"jmapSieveScriptDestroy":true,"jmapSieveScriptGet":true,"jmapSieveScriptQuery":true,"jmapSieveScriptUpdate":true,"jmapSieveScriptValidate":true,"jmapThreadChanges":true,"jmapThreadGet":true,"jmapVacationResponseCreate":true,"jmapVacationResponseDestroy":true,"jmapVacationResponseGet":true,"jmapVacationResponseUpdate":true,"pop3Authenticate":true,"pop3Dele":true,"pop3List":true,"pop3Retr":true,"pop3Stat":true,"pop3Uidl":true,"sieveAuthenticate":true,"sieveCheckScript":true,"sieveDeleteScript":true,"sieveGetScript":true,"sieveHaveSpace":true,"sieveListScripts":true,"sievePutScript":true,"sieveRenameScript":true,"sieveSetActive":true,"sysAccountCreate":true,"sysAccountGet":true,"sysAccountPasswordGet":true,"sysAccountPasswordUpdate":true,"sysAccountQuery":true,"sysAccountSettingsGet":true,"sysAccountSettingsUpdate":true,"sysAccountUpdate":true,"sysApiKeyCreate":true,"sysApiKeyDestroy":true,"sysApiKeyGet":true,"sysApiKeyQuery":true,"sysApiKeyUpdate":true,"sysAppPasswordCreate":true,"sysAppPasswordDestroy":true,"sysAppPasswordGet":true,"sysAppPasswordQuery":true,"sysAppPasswordUpdate":true,"sysArchivedItemCreate":true,"sysArchivedItemDestroy":true,"sysArchivedItemGet":true,"sysArchivedItemQuery":true,"sysArchivedItemUpdate":true,"sysMaskedEmailCreate":true,"sysMaskedEmailDestroy":true,"sysMaskedEmailGet":true,"sysMaskedEmailQuery":true,"sysMaskedEmailUpdate":true,"sysPublicKeyCreate":true,"sysPublicKeyDestroy":true,"sysPublicKeyGet":true,"sysPublicKeyQuery":true,"sysPublicKeyUpdate":true,"sysSpamTrainingSampleDestroy":true,"sysSpamTrainingSampleGet":true,"sysSpamTrainingSampleQuery":true,"sysSpamTrainingSampleUpdate":true}}'

config_output=$(run_cli "$automation_auth" create ApiKey \
  --field description='OpenAtom configuration automation' \
  --field "permissions=$config_permissions" --field 'allowedIps={}')
config_token=$(printf '%s\n' "$config_output" | sed -n 's/^[[:space:]]*Secret:[[:space:]]*//p')

api_output=$(run_cli "$automation_auth" create ApiKey \
  --field description='OpenAtom account provisioning' \
  --field "permissions=$api_permissions" --field 'allowedIps={}')
api_token=$(printf '%s\n' "$api_output" | sed -n 's/^[[:space:]]*Secret:[[:space:]]*//p')

case "$config_token" in API_*) ;; *) echo "Stalwart did not return the configuration API key" >&2; exit 65 ;; esac
case "$api_token" in API_*) ;; *) echo "Stalwart did not return the account API key" >&2; exit 65 ;; esac

write_auth_file "$config_auth" token '' "$config_token"
write_auth_file "$api_auth" token '' "$api_token"
run_cli "$config_auth" query Domain --where "name=$mail_domain" --json >/dev/null
run_cli "$api_auth" query Account --where "domainId=$domain_id" --json >/dev/null

# Persist the generated state before revoking the temporary password. If the
# process is interrupted before cleanup, recovery mode remains available and a
# retry can rotate the keys safely instead of losing the only usable secret.
set_value STALWART_CONFIG_TOKEN "$config_token"
set_value STALWART_API_TOKEN "$api_token"
set_value STALWART_DOMAIN_ID "$domain_id"

# Remove the temporary password while retaining the two API key credentials.
run_cli "$automation_auth" update Account "$automation_account_id" \
  --field 'credentials/0=null' >/dev/null

set_value STALWART_RECOVERY_MODE 0
set_value STALWART_RECOVERY_ADMIN ''
chmod 600 "$env_file"
restart_stalwart ''
wait_for_stalwart

echo "Stalwart configuration token, account token and domain id were generated automatically"
