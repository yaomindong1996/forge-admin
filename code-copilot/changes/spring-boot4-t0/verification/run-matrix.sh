#!/usr/bin/env bash
set -u

probe_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
probe_mvn="${FORGE_T0_MVN:-mvn}"
probe_failures=0

if [[ -n "${FORGE_T0_JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$FORGE_T0_JAVA_HOME"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
mkdir -p "$probe_dir/target/logs"
# 复跑也保留上一轮诊断，不覆盖原始失败证据。
probe_logs="$(mktemp -d "$probe_dir/target/logs/matrix.XXXXXX")"

run_probe() {
    local profile="$1"
    local module="$2"
    local log="$probe_logs/$module-$profile-matrix.log"
    local args=(-B -ntp -f "$probe_dir/pom.xml" -P"$profile,enable-tests" -pl "$module" clean test)
    if [[ -n "${FORGE_T0_SETTINGS:-}" ]]; then
        args=(-s "$FORGE_T0_SETTINGS" "${args[@]}")
    fi
    printf '验证 %s / %s\n' "$profile" "$module"
    "$probe_mvn" "${args[@]}" > "$log" 2>&1
    local result=$?
    printf '退出状态 %s；日志 %s\n' "$result" "$log"
    tail -n 12 "$log"
    if [[ "$result" -ne 0 ]]; then
        probe_failures=$((probe_failures + 1))
    fi
}

# 串行执行，避免相同模块的 target 或 Maven 缓存发生并发构建干扰。
run_probe boot3-baseline flow-probe
run_probe boot3-baseline ai-probe
run_probe boot3-baseline mcp-probe
run_probe boot4-target flow-probe
run_probe boot4-target ai-probe
run_probe boot4-target mcp-probe
run_probe boot4-target ai-runtime-probe
run_probe boot4-old-ai ai-probe
run_probe boot4-alibaba-upstream ai-probe
run_probe boot4-alibaba-upstream ai-runtime-probe

printf '矩阵结束，失败项：%s。失败是调研证据，不代表可以发布。\n' "$probe_failures"
[[ "$probe_failures" -eq 0 ]]
