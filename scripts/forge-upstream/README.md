# 改名工程的社区同步

这是源码增量合并，不是覆盖生成、自动升级、自动部署或双向仓库同步。

## 基线

`.forge/upstream/baseline.json` 保存社区准确提交/版本、生成选项、映射版本/摘要和规范化基线提交。
`.forge/upstream/generator` 是初始化时冻结的生成规则，不能直接跟随社区改名逻辑漂移。
Git 基线提交是产品提交的祖先；每次升级必须保留 merge commit，后续普通 clone 就能恢复基线。
禁止 squash 社区升级合并；禁止删除/改写基线历史。临时候选与计划只放在 `.git/forge-upstream`。

产品版本取自己的根 POM revision；社区版本取 baseline.upstream.version。
`forge.config.json.forgeVersion` 用于社区插件兼容性，不随产品独立升版重写。
同步固定产品版本占位基线，因此上游升版不会自动替产品升版。

## 初始化（只用于不存在的新目录）

在模板仓库使用 `node scripts/forge-upstream/index.mjs init`，参数：

- `--source`：社区 Git 目录，只读使用指定提交，不复制未提交改动。
- `--ref`：经确认的准确提交或标签。
- `--target`：不存在的新工程目录。
- `--options`：JSON 生成选项文件，使用 forge:create 的长参数名。
- `--generator`：提供冻结规则的模板目录，默认当前工具目录所在模板。

必填选项：`project-name`、`artifact-prefix`、`base-package`、`group-id`、`database-name`、`preset`。
建议指定 `java-name`、`display-name`、`exclude-log-data: true`。只接受白名单选项，不接受 force。
初始化会创建两个本地 Git 提交：规范化底座与项目开发配置；不会配置 push 远端或推送。

## 升级

先在社区工程获取并审查目标提交，再回到当前产品工程：

```bash
git switch -c codex/upgrade-community
pnpm forge:upstream plan --source /绝对路径/社区仓库 --ref <准确提交>
pnpm forge:upstream apply
```

`plan` 不改工作区，报告增删改、冲突和阻断。`apply` 要求非 main/master 的干净分支，
检查 HEAD/配置未变化后执行 `git merge --no-commit --no-ff`，停下来供人工检查。
冲突正常留在 Git 中，禁止用全选 ours/theirs 掩盖协议差异。

```bash
# 解决冲突并 git add 对应文件后，先验证
pnpm forge:upstream verify
pnpm version:check
# 按本次差异运行后端编译、前端构建和相关测试
git commit -m "升级社区底座并完成兼容验证"
```

取消整个合并：`git merge --abort`。提交前不执行真实数据库迁移、不推送、不部署。
合并完成后仍需按产品发布规范审查、合并主分支和交付。

## 保护边界

- 新增社区迁移可以同步；已有迁移的任何修改/删除，以及本地篡改迁移，均被拦截。
- 专属插件、环境密钥、同步元数据不参与生成覆盖；公共文件的二次开发进入正常三方合并。
- 完整源码历史不上传社区。公共修复先提交社区，企业专属修复只留私有工程。
- 映射/模块目录调整需要专门迁移评审；不能更新摘要后假装旧基线由新规则生成。
- 工具不校验业务兼容性；编译、协议、权限、多租户、插件和数据库兼容测试仍是升级必经步骤。
