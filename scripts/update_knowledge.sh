#!/bin/bash
# Master script: Convert, post-process, and ingest OpenMind website knowledge
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
API_BASE="${HAGAP_API_BASE:-http://localhost:8080}"
WORKSPACE_ID="${HAGAP_WORKSPACE_ID:-}"

usage() {
    echo "Usage: HAGAP_WORKSPACE_ID=<uuid> $0 [--skip-convert] [--skip-delete]"
    echo ""
    echo "Options:"
    echo "  --skip-convert  Skip HTML->MD conversion (reuse existing output)"
    echo "  --skip-delete   Skip deleting existing knowledge before ingestion"
    echo ""
    echo "Environment variables:"
    echo "  HAGAP_WORKSPACE_ID  (required) UUID of the target workspace"
    echo "  HAGAP_API_BASE      (optional) API base URL (default: http://localhost:8080)"
    exit 1
}

if [ -z "$WORKSPACE_ID" ]; then
    usage
fi

SKIP_CONVERT=false
SKIP_DELETE=false

for arg in "$@"; do
    case "$arg" in
        --skip-convert) SKIP_CONVERT=true ;;
        --skip-delete) SKIP_DELETE=true ;;
        --help|-h) usage ;;
    esac
done

echo "=== OpenMind Knowledge Update ==="
echo "Workspace: $WORKSPACE_ID"
echo "API: $API_BASE"
echo ""

# Step 1: Convert HTML to Markdown
if [ "$SKIP_CONVERT" = false ]; then
    echo "--- Step 1: Converting HTML to Markdown ---"
    "$SCRIPT_DIR/convert_html_to_md.sh"
    echo ""

    echo "--- Step 2: Post-processing Markdown ---"
    python3 "$SCRIPT_DIR/postprocess_md.py"
    echo ""
else
    echo "--- Skipping conversion (--skip-convert) ---"
    echo ""
fi

# Step 2: Delete existing knowledge
if [ "$SKIP_DELETE" = false ]; then
    echo "--- Step 3: Deleting existing knowledge ---"
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X DELETE "$API_BASE/api/workspaces/$WORKSPACE_ID/knowledge" \
        --max-time 30)

    if [ "$http_code" = "204" ]; then
        echo "  Existing knowledge deleted successfully."
    elif [ "$http_code" = "404" ]; then
        echo "  No existing knowledge found (workspace may be new)."
    else
        echo "  WARNING: Delete returned HTTP $http_code"
    fi
    echo ""
else
    echo "--- Skipping delete (--skip-delete) ---"
    echo ""
fi

# Step 3: Ingest new knowledge
echo "--- Step 4: Ingesting knowledge files ---"
"$SCRIPT_DIR/ingest_knowledge.sh"

echo ""
echo "=== Knowledge update complete ==="
