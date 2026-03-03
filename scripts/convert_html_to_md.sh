#!/bin/bash
# Convert crawled OpenMind HTML pages to Markdown using markitdown
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INPUT_DIR="${1:-$SCRIPT_DIR/../openmind-tech-en.com/www.openmind-tech.com/en}"
OUTPUT_DIR="$SCRIPT_DIR/output/knowledge"

if [ ! -d "$INPUT_DIR" ]; then
    echo "ERROR: Input directory not found: $INPUT_DIR"
    echo "Usage: $0 [input_dir]"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

# Skip non-content pages
SKIP_PAGES="search|sitemap|privacy|legal-information|customer-portal"

count=0
skipped=0

find "$INPUT_DIR" -name "index.html" -not -name "*.tmp" | sort | while read -r html_file; do
    # Derive flat filename from path: en/cam/3d-milling/index.html -> cam__3d-milling.md
    rel_path="${html_file#$INPUT_DIR/}"
    dir_path=$(dirname "$rel_path")

    # Root index.html -> homepage.md
    if [ "$dir_path" = "." ]; then
        md_name="homepage.md"
    else
        md_name=$(echo "$dir_path" | sed 's|/|__|g').md
    fi

    # Skip non-content pages
    base_name=$(basename "$dir_path")
    if echo "$base_name" | grep -qE "^($SKIP_PAGES)$"; then
        echo "SKIP: $md_name"
        skipped=$((skipped + 1))
        continue
    fi

    echo "Converting: $md_name"
    if uvx markitdown "$html_file" > "$OUTPUT_DIR/$md_name" 2>/dev/null; then
        count=$((count + 1))
    else
        echo "  WARN: markitdown failed for $html_file, trying with cat fallback"
        # Minimal fallback - just extract text
        cat "$html_file" | sed 's/<[^>]*>//g' | sed '/^$/d' > "$OUTPUT_DIR/$md_name"
        count=$((count + 1))
    fi
done

echo ""
echo "Done. Converted files in: $OUTPUT_DIR"
echo "Run postprocess_md.py next to clean up the output."
