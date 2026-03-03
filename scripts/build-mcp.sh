#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Building MCP Gallery Server ==="
cd "$ROOT_DIR/services/mcp-gallery"
npm install
npx tsc
echo "Gallery server built successfully."

echo ""
echo "=== Building MCP Reference Server ==="
cd "$ROOT_DIR/services/mcp-reference"
npm install
npx tsc
echo "Reference server built successfully."

echo ""
echo "=== All MCP servers built ==="
