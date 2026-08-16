#!/bin/sh
# Generates the HedgeDoc config.json from environment variables.
# Usage: HEDGEDOC_DOMAIN=... HEDGEDOC_SESSION_SECRET=... HEDGEDOC_DB_PASS=...
#        OAUTH2_CLIENT_ID=... OAUTH2_CLIENT_SECRET=... ./generate-config.sh [output]
set -eu
out="${1:-config.json}"
: "${HEDGEDOC_SESSION_SECRET:?HEDGEDOC_SESSION_SECRET required}"
: "${HEDGEDOC_DB_PASS:?HEDGEDOC_DB_PASS required}"
: "${OAUTH2_CLIENT_SECRET:?OAUTH2_CLIENT_SECRET required}"
domain="${HEDGEDOC_DOMAIN:-https://md.jmi-openatom.cn}"
client_id="${OAUTH2_CLIENT_ID:-openatom-hedgedoc}"
issuer="${OAUTH2_ISSUER:-https://oauth.jmi-openatom.cn/api/v1}"
cat > "$out" <<JSON
{
  "production": {
    "domain": "$domain",
    "sessionSecret": "$HEDGEDOC_SESSION_SECRET",
    "db": {
      "dialect": "postgres",
      "host": "hedgedoc-db",
      "port": 5432,
      "username": "hedgedoc",
      "password": "$HEDGEDOC_DB_PASS",
      "database": "hedgedoc"
    },
    "oauth2": {
      "providerName": "OpenAtom",
      "clientID": "$client_id",
      "clientSecret": "$OAUTH2_CLIENT_SECRET",
      "authorizationURL": "$issuer/oauth/authorize",
      "tokenURL": "$issuer/oauth/token",
      "scope": "openid profile email"
    }
  }
}
JSON
chmod 644 "$out"
echo "generated $out"
