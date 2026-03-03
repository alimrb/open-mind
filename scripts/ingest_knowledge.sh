#!/bin/bash
# Upload markdown knowledge files to HAGAP API
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
KNOWLEDGE_DIR="${1:-$SCRIPT_DIR/output/knowledge}"
API_BASE="${HAGAP_API_BASE:-http://localhost:8080}"
WORKSPACE_ID="${HAGAP_WORKSPACE_ID:-}"

if [ -z "$WORKSPACE_ID" ]; then
    echo "ERROR: HAGAP_WORKSPACE_ID is required"
    echo "Usage: HAGAP_WORKSPACE_ID=<uuid> $0 [knowledge_dir]"
    echo "  or:  $0 <knowledge_dir> (with HAGAP_WORKSPACE_ID env var set)"
    exit 1
fi

if [ ! -d "$KNOWLEDGE_DIR" ]; then
    echo "ERROR: Knowledge directory not found: $KNOWLEDGE_DIR"
    exit 1
fi

API_URL="$API_BASE/api/workspaces/$WORKSPACE_ID/knowledge"
echo "Ingesting knowledge files into workspace $WORKSPACE_ID"
echo "API: $API_URL"
echo "Source: $KNOWLEDGE_DIR"
echo ""

count=0
failed=0

for md_file in "$KNOWLEDGE_DIR"/*.md; do
    [ -f "$md_file" ] || continue

    filename=$(basename "$md_file")
    echo -n "  Uploading: $filename ... "

    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$API_URL" \
        -F "file=@$md_file" \
        --max-time 30)

    if [ "$http_code" = "202" ]; then
        echo "OK (202)"
        count=$((count + 1))
    else
        echo "FAILED (HTTP $http_code)"
        failed=$((failed + 1))
    fi

    # Small delay to avoid overwhelming the API
    sleep 0.5
done

echo ""
echo "Done. Uploaded: $count, Failed: $failed"
