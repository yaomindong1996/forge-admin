#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# 仓库已不再固定维护 community-export / seed；默认输出到可忽略的本地目录。
EXPORT_DIR="$SCRIPT_DIR/../../db/mysql/community-export"
DRY_RUN=""

for arg in "$@"; do
  case "$arg" in
    --dry-run)
      DRY_RUN="--dry-run"
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      echo "Usage: $0 [--dry-run]" >&2
      exit 1
      ;;
  esac
done

mkdir -p "$EXPORT_DIR"
node "$SCRIPT_DIR/export-community-schema.js" $DRY_RUN
node "$SCRIPT_DIR/export-community-seed.js" $DRY_RUN
node "$SCRIPT_DIR/check-sensitive-data.js" "$EXPORT_DIR"

cat > "$EXPORT_DIR/summary.md" <<SUMMARY
# Forge Community Database Export Summary

- Mode: ${DRY_RUN:-export}
- Migration summary: schema-summary.json
- Seed summary: seed-summary.json
- Sensitive data check: passed
SUMMARY

echo "Community database export summary written to $EXPORT_DIR/summary.md"
