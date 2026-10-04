#!/bin/sh
# Verify and import a CI-built release before Compose can replace any containers.
set -eu

if [ "$#" -lt 4 ]; then
  echo 'usage: load-production-images.sh /path/to/image-bundle commit-sha image-prefix service...' >&2
  exit 64
fi

bundle_dir=$1
release_commit=$2
image_prefix=$3
shift 3
printf '%s\n' "$release_commit" | grep -Eq '^[0-9a-f]{40}$' || {
  echo 'Expected a full Git commit SHA' >&2
  exit 65
}
printf '%s\n' "$image_prefix" | grep -Eq '^[a-z0-9-]+$' || exit 65
for service do
  printf '%s\n' "$service" | grep -Eq '^[a-z0-9-]+$' || exit 65
done
cd "$bundle_dir"
test "$(cat release-commit)" = "$release_commit" || {
  echo 'Image bundle does not match the deployment commit' >&2
  exit 65
}
sha256sum --check checksums.sha256

for service do
  test -s "$service.tar.gz"
done
for service do
  docker load --input "$service.tar.gz"
done
if [ -f dependencies.tar.gz ]; then
  docker load --input dependencies.tar.gz
fi
for service do
  docker image inspect "$image_prefix-$service:$release_commit" >/dev/null
done

# Keep archives after a failed import for diagnosis; remove only verified bundles.
for service do
  rm -f "$service.tar.gz"
done
rm -f dependencies.tar.gz checksums.sha256 release-commit
echo "Production images imported for $release_commit"
