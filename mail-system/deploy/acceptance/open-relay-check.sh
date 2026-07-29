#!/bin/sh
set -eu

mx_host=${1:-mx1.jmi-openatom.cn}

if ! command -v swaks >/dev/null 2>&1; then
  echo "swaks is required for this operator-run acceptance check" >&2
  exit 69
fi

# This unauthenticated external-to-external transaction MUST be rejected at RCPT.
if swaks --server "$mx_host" --port 25 \
  --from relay-test@example.net --to relay-target@example.org \
  --quit-after RCPT 2>&1 | grep -Eq '<-  (250|251)'; then
  echo "FAIL: server accepted unauthenticated relay" >&2
  exit 1
fi

echo "PASS: unauthenticated external relay was rejected"
