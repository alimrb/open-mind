#!/usr/bin/env python3
"""Post-process converted Markdown files from OpenMind website.

1. Strip navigation/footer boilerplate
2. Rewrite relative URLs to absolute
3. Add YAML front matter (title, source_url, section)
"""

import os
import re
import sys
from pathlib import Path

BASE_URL = "https://www.openmind-tech.com"
EN_BASE = f"{BASE_URL}/en"

# Patterns that mark the start of footer boilerplate
FOOTER_MARKERS = [
    r"^#{1,5}\s+Member of\s*$",
    r"^#{1,5}\s+Connect with us\s*$",
    r"^\*\s+\[Home\]",
    r"^©\s+\d{4}\s+OPEN MIND",
    r"^\*\*mobile-s\s+mobile-m",
]

# Navigation patterns (lines before actual content)
NAV_PATTERNS = [
    r"^\[!\[OPEN MIND\]",              # Logo link
    r"^\*\s+\[(News|Events|About us|Partners|Contact|Career|my \*hyper\*MILL)\]",
    r"^\[Search\]",
    r"^\[Contact\]",
    r"^English\s*$",
    r"^\[(Deutsch|Français|Italiano|Español|Nederlands|Pусский|Türkçe|Portugues|日本語|简体中文|繁體中文|한국어)\]",
    r"^\[English \|",                   # Language selector
    r"^\s*\*\s+\[CAM\]",               # Main nav start
    r"^\s*\*\s+\[CAD\]",
    r"^\s*\*\s+\[Industries\]",
    r"^\s*\*\s+\[References\]",
    r"^\s*\*\s+\[Service\]",
    r"^\s*\+\s+\[",                     # Sub-nav items
    r"^\s*-\s+\[.*\]\(.*index\.html\)", # Deep nav items with relative links
]


def extract_section(filename: str) -> str:
    """Extract section from filename (first segment before __)."""
    name = filename.replace(".md", "")
    if name == "homepage":
        return "general"
    parts = name.split("__")
    return parts[0] if parts else "general"


def extract_title(content: str) -> str:
    """Extract first H1 heading as title."""
    for line in content.split("\n"):
        m = re.match(r"^#\s+(.+)$", line)
        if m:
            # Clean markdown formatting from title
            title = m.group(1).strip()
            title = re.sub(r"\*([^*]+)\*", r"\1", title)  # Remove italics
            return title
    return ""


def derive_source_url(filename: str) -> str:
    """Convert filename back to source URL."""
    name = filename.replace(".md", "")
    if name == "homepage":
        return f"{EN_BASE}/"
    path = name.replace("__", "/")
    return f"{EN_BASE}/{path}/"


def rewrite_urls(content: str) -> str:
    """Rewrite relative URLs to absolute."""
    # Rewrite fileadmin references: ../../fileadmin/... -> https://www.openmind-tech.com/fileadmin/...
    content = re.sub(
        r"\((?:\.\./)*fileadmin/",
        f"({BASE_URL}/fileadmin/",
        content,
    )

    # Rewrite relative page links: ../something/index.html -> absolute URL
    # Also handles ../../en.html -> homepage
    content = re.sub(
        r"\((?:\.\./)*en\.html\)",
        f"({EN_BASE}/)",
        content,
    )

    # Rewrite assets references
    content = re.sub(
        r"\((?:\.\./)*assets/",
        f"({BASE_URL}/assets/",
        content,
    )

    # Rewrite relative links like (../cam/5-axis-milling/index.html)
    # and (../../industries/index.html) to absolute
    def replace_relative_link(m):
        rel_path = m.group(1)
        # Strip ../ prefixes
        clean = re.sub(r"^(\.\./)+", "", rel_path)
        # Remove index.html but keep anchors and title attributes
        clean = re.sub(r"index\.html", "", clean)
        # Build absolute URL
        return f"({EN_BASE}/{clean})"

    content = re.sub(
        r"\(((?:\.\./)+[^)]*index\.html[^)]*)\)",
        lambda m: replace_relative_link(m),
        content,
    )

    return content


def is_nav_line(line: str) -> bool:
    """Check if a line is part of navigation boilerplate."""
    for pattern in NAV_PATTERNS:
        if re.match(pattern, line.strip()):
            return True
    return False


def is_footer_marker(line: str) -> bool:
    """Check if a line marks the start of footer."""
    for pattern in FOOTER_MARKERS:
        if re.search(pattern, line.strip()):
            return True
    return False


def strip_boilerplate(content: str) -> str:
    """Remove navigation header and footer boilerplate."""
    lines = content.split("\n")

    # Find where actual content starts (first H1 heading)
    content_start = 0
    for i, line in enumerate(lines):
        if re.match(r"^#\s+", line.strip()):
            content_start = i
            break

    # Find where footer starts
    content_end = len(lines)
    for i in range(len(lines) - 1, content_start, -1):
        if is_footer_marker(lines[i]):
            # Walk back to find the earliest footer marker in this block
            j = i
            while j > content_start and (
                is_footer_marker(lines[j])
                or lines[j].strip() == ""
                or re.match(r"^\*\s+\[", lines[j].strip())
                or re.match(r"^\[!\[", lines[j].strip())
            ):
                j -= 1
            content_end = j + 1
            break

    result = "\n".join(lines[content_start:content_end]).strip()
    return result


def add_front_matter(content: str, filename: str) -> str:
    """Add YAML front matter to markdown content."""
    title = extract_title(content)
    source_url = derive_source_url(filename)
    section = extract_section(filename)

    front_matter = f"""---
title: "{title}"
source_url: "{source_url}"
section: "{section}"
---

"""
    return front_matter + content


def process_file(filepath: Path) -> None:
    """Process a single markdown file."""
    content = filepath.read_text(encoding="utf-8")

    if not content.strip():
        print(f"  SKIP (empty): {filepath.name}")
        return

    # Strip boilerplate
    content = strip_boilerplate(content)

    if not content.strip():
        print(f"  SKIP (no content after cleanup): {filepath.name}")
        return

    # Rewrite URLs
    content = rewrite_urls(content)

    # Add front matter
    content = add_front_matter(content, filepath.name)

    filepath.write_text(content, encoding="utf-8")


def main():
    input_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent / "output" / "knowledge"

    if not input_dir.exists():
        print(f"ERROR: Directory not found: {input_dir}")
        sys.exit(1)

    md_files = sorted(input_dir.glob("*.md"))
    if not md_files:
        print(f"No .md files found in {input_dir}")
        sys.exit(1)

    print(f"Post-processing {len(md_files)} files in {input_dir}")

    for filepath in md_files:
        print(f"  Processing: {filepath.name}")
        process_file(filepath)

    print(f"\nDone. Processed {len(md_files)} files.")


if __name__ == "__main__":
    main()
