# plugin-foundation 执行记录

## 2026-10-06：提案

### 背景

确定开源版 + 企业版插件的整体模式：

- 开源版和企业版都交付完整源码；
- 不使用制品库；
- 企业版放在阿里云效私有仓库；
- 永久授权 + 年度维保。

本变更只做开源仓库内的插件化底座。

### 调研结论

- 插件源码按工程同规则改名后，可直接被 `scanBasePackages` / `@MapperScan` 扫描，不需要 AutoConfiguration。
- 超级管理员权限是 `*` 和 `/**`，企业版接口必须靠 `@RequiresFeature` 拦截。
- `clean-db.sh` 的 `_history$` 规则会清空插件迁移历史表，需要加保留规则。
- 生成工程没有 `scripts/`、根 `package.json`，插件安装工具需要随工程一起生成。
- `create-project.mjs` 没有自动化测试，抽取改名规则前必须先做输出基线。

### 待澄清确认（HARD-GATE）

- `revision` 升为 `1.1.0`，此后每次开源版发版都递增。
- 插件目录：后端 `<后端根目录>/plugins/`，前端 `src/views/plugins/<插件ID>/`，路由前缀 `/plugins/<插件ID>`。
- 模块名：`forge-starter-plugin`。

### 验证

- 仅文档变更：`git diff --check` 通过，未执行代码验证。
