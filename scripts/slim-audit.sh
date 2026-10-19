#!/usr/bin/env bash
set -euo pipefail
IMAGE="${1:?Usage: slim-audit.sh IMAGE_TAG}"
command -v docker >/dev/null || exit 1
command -v slim >/dev/null || { echo "SlimToolkit CLI required" >&2; exit 1; }
docker image inspect --format '{{.RepoTags}} {{.Size}} bytes (virtual uncompressed)' "$IMAGE"
slim xray "$IMAGE"
echo "No production image changed. Test JDBC, native libraries and performance workload before dynamic pruning."
