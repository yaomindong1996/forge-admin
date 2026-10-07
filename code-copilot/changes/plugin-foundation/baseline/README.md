# T0 脚手架基线

本目录只保存证据和比对工具，不保存两套完整工程，不修改生产生成器。

- `full.json`：8324 个文件；`minimal-admin.json`：4866 个文件。
- 每条记录包含相对路径、字节数、原始内容 SHA-256；包含点文件、空文件和二进制，无内容归一化或忽略规则。
- `provenance.json`：固定源提交、版本、生成器/目录清单/版本补丁和清单摘要。
- `version-bump.patch`：源提交上仅 3 个 POM 的 T0 版本链变更。
- `manifest.mjs`：`record` 使用排他写入，不覆盖基线；`verify` 对缺失、新增、内容差异返回非零退出码。

## 固定输入及参数

源：`e416f7902834763ef43989c4525738441e49bd4c`，先应用版本补丁，再生成。
使用 Git archive 排除工作区、本地环境、未跟踪文件和本变更后续修改；不是复制当前工作区作为模板。

固定 `--base-package com.acme.demo`，预设分别为 `full` / `minimal-admin`。未指定项使用源生成器默认值：

- 项目名/displayName/artifact 前缀：固定末级目录名 `forge-baseline-full` / `forge-baseline-min`。
- Java 类前缀：`ForgeBaselineFull` / `ForgeBaselineMin`；groupId 为 `com.acme.demo`。
- 数据库名：`forge_baseline_full` / `forge_baseline_min`；不连接数据库。
- include 为空、`exclude-log-data=false`；前端默认端口 5173、API 前缀 `/api`、代理 `http://localhost:8580`。
- 此处是已有 test-spec 的合成测试工程，不是为用户新建业务项目，因此沿用固定测试参数，不猜测业务命名。

## 安全复跑

从模板仓库根目录执行。只在 `mktemp` 分配的专用目录中生成，不使用 `--force`，不执行初始化库命令。
不需要安装生成工程依赖；需要本机 Node/pnpm 和当前仓库 Git 历史。

```bash
source /Users/mini32g/.nvm/nvm.sh && nvm use v20.19.0
repo_dir="$PWD"
baseline_dir="$(mktemp -d /private/tmp/forge-plugin-t0.XXXXXX)"
mkdir "$baseline_dir/template"
git archive e416f7902834763ef43989c4525738441e49bd4c | tar -x -C "$baseline_dir/template"
git -C "$baseline_dir/template" apply "$repo_dir/code-copilot/changes/plugin-foundation/baseline/version-bump.patch"
```

接着在隔离模板中生成；末级目录名必须保持不变，上层临时目录可以不同：

```bash
(
  cd "$baseline_dir/template"
  pnpm forge:create -- "$baseline_dir/forge-baseline-full" --base-package com.acme.demo --preset full
  pnpm forge:create -- "$baseline_dir/forge-baseline-min" --base-package com.acme.demo --preset minimal-admin
)
node code-copilot/changes/plugin-foundation/baseline/manifest.mjs verify \
  "$baseline_dir/forge-baseline-full" code-copilot/changes/plugin-foundation/baseline/full.json
node code-copilot/changes/plugin-foundation/baseline/manifest.mjs verify \
  "$baseline_dir/forge-baseline-min" code-copilot/changes/plugin-foundation/baseline/minimal-admin.json
```

## T5/T6 的对比边界

在同一个冻结模板上复制本轮生成器及其 `scripts/forge-shared/` 等依赖，然后重新生成到新的专用目录。
**不复制当前 forge-server、AGENTS、code-copilot 等生产/上下文文件**，否则 T1–T4 的源码变化会污染改名回归。
T5 差异必须为 0；T6 按 test-spec 检查工具/config/gitignore 的明确差异，禁止重新记录覆盖 T0。

同时要在实时工作区另生成 full/minimal-admin 做底座集成验证；冻结输入回归不能代替新模块装配测试。
若生成器新增了依赖文件，应完整带入冻结模板并记录，不得仅复制入口脚本导致缺依赖后降低验证标准。

## 本轮结果

- 两个预设均独立生成两次，新增/缺失/变化文件均为 0。
- 基线工具测试 5 项、模板数据库脚本桩测试 18 项、生成 full 数据库脚本桩测试 18 项通过。
- 修改的 3 个 POM、生成 full 的 53 个 POM、生成 minimal-admin 的 34 个 POM 均通过 XML 解析。
- 模板及两套生成工程的版本链静态检查通过；Maven/JDK 缺失，Java 编译和真实迁移没有执行。

本轮临时工程：`/private/tmp/forge-plugin-t0.rmLIiB/`，保留供检查，未作为提交内容；没有启动服务。
