---
name: forge-flyway-migration
description: Add or review Forge Flyway schema migrations, backfills, index changes, and dictionary/menu/API resource seeds. Use for migration numbering, MySQL/PostgreSQL/SQL Server pairing, plugin migration history, upgrading existing databases, clean initialization, or checksum and migration-location failures.
---

# Forge Flyway 迁移

1. 读 [AGENTS.md](../../../AGENTS.md)、[关键约定的 SQL 章节](../../../docs/guides/critical-conventions.md#513-数据库脚本维护规范)和 [Flyway 踩坑记录](../../../code-copilot/memory/pitfalls/db-flyway.md)。先确认变更属于 Admin 主库、已注册插件还是独立服务，定位生效的数据源、方言、迁移目录与历史表。
2. 刷新远端，给主库增量运行 `node forge-server/scripts/db/next-migration-version.mjs --desc <动作>`。工具以 MySQL 目录及可选 MySQL 历史取水位。维护 PostgreSQL、SQL Server 环境时，同时核对这些目录及获准读取的历史。确认编号后按仓库约定生成三库脚本。插件走自身描述符约定的迁移目录和独立历史。
3. 追加新的 `V<版本>__<描述>.sql`，保留已经执行的旧文件及其内容。按方言实现相同业务结果。审查已有列、索引、旧数据和逻辑删除状态的演进，写显式列名及防重复条件；种子数据租户为 `1`。涉及菜单或角色读 [Forge 权限](../forge-resource-permissions/SKILL.md)。增量会扩大权限时把授予对象与撤回方式写进 Spec。
4. 执行[迁移验证与故障定位](references/validation.md)。至少核对三库配对、重复版本和已执行历史，测试空库与已安装上一版的库，再重跑校验。在隔离库验证真实 DataScope、字典或权限业务结果。按实际验证过的数据库分别记证据。
5. 如遇校验和冲突，先按[部署说明](../../../forge-server/scripts/deployment/admin-flyway.md)比对当前数据库历史、运行包内 SQL 和配置覆盖。恢复实际已经执行的历史文件或正确来源，业务修正加新迁移；保留历史和校验机制。
