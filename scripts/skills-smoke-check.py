#!/usr/bin/env python3
"""Smoke-check Forge Agent Skills: structure, routing tables, optional PRO remaps.

Usage:
  python3 scripts/skills-smoke-check.py
  python3 scripts/skills-smoke-check.py --pro /path/to/ForgePro
  python3 scripts/skills-smoke-check.py --website /path/to/forge-website
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

WAVE2 = [
    "forge-tenant-datascope",
    "forge-print-integration",
    "forge-collaboration-notify",
]

# Always verify these orchestration/docs anchors exist when present.
CORE = [
    "forge-business-module-playbook",
    "forge-message-integration",
    *WAVE2,
]

# Prompt → expected skill + keywords that should appear in description ∪ playbook.
ROUTING_CASES = [
    (
        "列表按组织隔离，别漏别人数据",
        "forge-tenant-datascope",
        ["列表隔离", "DataScope", "租户"],
    ),
    (
        "IgnoreTenant 和租户上下文怎么建",
        "forge-tenant-datascope",
        ["IgnoreTenant", "租户"],
    ),
    (
        "DataScope REGION 区划 ALL 后缀",
        "forge-tenant-datascope",
        ["DataScope", "REGION"],
    ),
    (
        "单据套打，实现 PrintBusinessDataProvider",
        "forge-print-integration",
        ["套打", "PrintBusinessDataProvider", "打印"],
    ),
    (
        "打印模板绑定页面和预览 PDF",
        "forge-print-integration",
        ["打印", "模板", "绑定", "PDF"],
    ),
    (
        "审批通过推企微",
        "forge-collaboration-notify",
        ["企微", "协作", "COLLABORATION"],
    ),
    (
        "COLLABORATION 渠道发消息",
        "forge-collaboration-notify",
        ["COLLABORATION"],
    ),
    (
        "FLOW_TODO_CARD_WECOM 回退",
        "forge-collaboration-notify",
        ["企微", "待办", "WeCom"],
    ),
    (
        "站内信 sendIfAbsent 模板",
        "forge-message-integration",
        ["sendIfAbsent", "站内信"],
    ),
]


def parse_fm(text: str) -> tuple[dict[str, str] | None, str]:
    if not text.startswith("---\n"):
        return None, text
    end = text.find("\n---\n", 4)
    if end < 0:
        return None, text
    data: dict[str, str] = {}
    for line in text[4:end].splitlines():
        if ":" in line:
            k, v = line.split(":", 1)
            data[k.strip()] = v.strip()
    return data, text[end + 5 :]


def check_skill_dir(base: Path, name: str, issues: list[str], *, remap: bool) -> None:
    sk = base / name / "SKILL.md"
    if not sk.exists():
        issues.append(f"MISSING {name}/SKILL.md")
        return
    fm, body = parse_fm(sk.read_text(encoding="utf-8"))
    if not fm or fm.get("name") != name:
        issues.append(f"{name}: frontmatter name mismatch ({fm})")
    if not fm or len(fm.get("description", "")) < 40:
        issues.append(f"{name}: description too short/missing")
    # Validate files listed under the Skill's own "## References" section only.
    ref_section = re.search(
        r"(?ms)^## References\s*\n(.*?)(?=^## |\Z)", body
    )
    if ref_section:
        refs = re.findall(r"`references/([^`]+)`", ref_section.group(1))
        for r in refs:
            if not (base / name / "references" / r).exists():
                issues.append(f"{name}: missing references/{r}")
    text = "\n".join(p.read_text(encoding="utf-8") for p in (base / name).rglob("*.md"))
    if remap:
        if "com.mdframe.forge" in text:
            issues.append(f"{name}: leftover com.mdframe.forge")
        if re.search(r"(?<!pro-)forge-admin-ui", text):
            issues.append(f"{name}: leftover forge-admin-ui")
    else:
        if "com.forgelab.forgepro" in text:
            issues.append(f"{name}: unexpected PRO package in community skill")


def check_repo(root: Path, label: str, *, remap: bool) -> list[str]:
    print(f"\n===== {label} ({root}) =====")
    base = root / ".agents" / "skills"
    agents = root / "AGENTS.md"
    issues: list[str] = []
    if not base.is_dir():
        return [f"{label}: missing .agents/skills"]
    if not agents.exists():
        issues.append(f"{label}: missing AGENTS.md")
        agents_text = ""
    else:
        agents_text = agents.read_text(encoding="utf-8")

    playbook_path = base / "forge-business-module-playbook" / "SKILL.md"
    playbook = playbook_path.read_text(encoding="utf-8") if playbook_path.exists() else ""
    if not playbook:
        issues.append(f"{label}: missing forge-business-module-playbook")

    for name in CORE:
        if not (base / name).exists():
            # message/playbook may exist; wave2 required
            if name in WAVE2 or name == "forge-business-module-playbook":
                issues.append(f"{label}: missing skill dir {name}")
            continue
        check_skill_dir(base, name, issues, remap=remap)
        if name in WAVE2:
            if name not in playbook:
                issues.append(f"{label}: playbook missing route for {name}")
            if name not in agents_text:
                issues.append(f"{label}: AGENTS.md missing {name}")

    print("  routing cases:")
    for prompt, expect, keys in ROUTING_CASES:
        sk = base / expect / "SKILL.md"
        if not sk.exists():
            issues.append(f"{label}: missing {expect} for routing case")
            print(f"    [MISS] {expect} ← {prompt}")
            continue
        desc = parse_fm(sk.read_text(encoding="utf-8"))[0].get("description", "")
        corpus = f"{desc}\n{playbook}\n{agents_text}"
        hits = [k for k in keys if k.lower() in corpus.lower()]
        routed = expect in playbook or expect in agents_text
        ok = bool(hits) and routed
        if not ok:
            issues.append(
                f"{label}: weak routing 「{prompt}」→{expect} hits={hits}"
            )
        print(f"    [{'OK' if ok else 'WEAK'}] {expect} ← {prompt} (hits={hits})")

    if issues:
        print("  ISSUES:")
        for i in issues:
            print(f"   - {i}")
    else:
        print("  all checks passed")
    return issues


def check_website(website: Path) -> list[str]:
    print(f"\n===== website ({website}) =====")
    issues: list[str] = []
    docs = website / "forge-docs" / "development"
    for rel in ("agent-skills.md", "skill-usage-tutorial.md"):
        path = docs / rel
        if not path.exists():
            issues.append(f"missing {rel}")
            continue
        text = path.read_text(encoding="utf-8")
        missing = [s for s in WAVE2 if s not in text]
        if missing:
            issues.append(f"{rel}: missing {missing}")
        else:
            print(f"  {rel}: OK")
    if issues:
        print("  ISSUES:")
        for i in issues:
            print(f"   - {i}")
    return issues


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="Community forge-project root",
    )
    parser.add_argument("--pro", type=Path, default=None, help="ForgePro root (optional)")
    parser.add_argument(
        "--website", type=Path, default=None, help="forge-website root (optional)"
    )
    args = parser.parse_args()

    issues: list[str] = []
    issues += check_repo(args.root, "community", remap=False)
    if args.pro:
        issues += check_repo(args.pro, "ForgePro", remap=True)
    if args.website:
        issues += check_website(args.website)

    print(f"\nTOTAL_ISSUES {len(issues)}")
    return 1 if issues else 0


if __name__ == "__main__":
    sys.exit(main())
