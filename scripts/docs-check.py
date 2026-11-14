#!/usr/bin/env python3
"""Check local Markdown links and agent-ready documentation topology.

Scoped to the agent-ready surface so legacy long-form docs with stale
absolute file:// links do not block CI. Topology rules still apply to
CONTEXT-MAP.md when present.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path.cwd()
LINK_RE = re.compile(r"(?<!!)\[[^\]]*\]\(([^)]+)\)")

# Progressive-disclosure entry points + docs created/owned by agent-ready.
SCOPED_FILES = (
    "AGENTS.md",
    "ARCHITECTURE.md",
    "CLAUDE.md",
    "CONTEXT.md",
    "CONTEXT-MAP.md",
    "AGENT_READY_ASSESSMENT.md",
)
SCOPED_DIR_PREFIXES = (
    "docs/guides/",
    "docs/references/",
    "docs/architecture/",
    "docs/adr/",
    "docs/DOMAIN.md",
    "docs/README.md",
)


def is_scoped(path: Path) -> bool:
    rel = path.relative_to(ROOT).as_posix()
    if rel in SCOPED_FILES:
        return True
    return any(rel == p or rel.startswith(p) for p in SCOPED_DIR_PREFIXES)


def markdown_files() -> list[Path]:
    return sorted(path for path in ROOT.rglob("*.md") if is_scoped(path))


def local_target(raw_target: str) -> str | None:
    target = raw_target.strip().split(maxsplit=1)[0].split("#", 1)[0]
    if not target or target.startswith(
        ("http://", "https://", "mailto:", "file://", "#")
    ):
        return None
    return target.strip("<>")


def check_links(errors: list[str]) -> None:
    for markdown_file in markdown_files():
        try:
            content = markdown_file.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            errors.append(f"{markdown_file.relative_to(ROOT)}: is not valid UTF-8")
            continue

        for target in LINK_RE.findall(content):
            relative_target = local_target(target)
            if relative_target is None:
                continue
            resolved = (markdown_file.parent / relative_target).resolve()
            if not resolved.exists():
                errors.append(
                    f"{markdown_file.relative_to(ROOT)}: broken link `{target}`"
                )


def check_context_map(errors: list[str]) -> None:
    context_map = ROOT / "CONTEXT-MAP.md"
    if not context_map.exists():
        return
    content = context_map.read_text(encoding="utf-8")
    context_targets = [
        target
        for target in LINK_RE.findall(content)
        if (local_target(target) or "").endswith("CONTEXT.md")
    ]
    if not context_targets:
        errors.append("CONTEXT-MAP.md must link to at least one CONTEXT.md file")


def main() -> int:
    errors: list[str] = []
    check_links(errors)
    check_context_map(errors)

    if errors:
        print("Documentation check failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(f"Documentation check passed ({len(markdown_files())} scoped files).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
