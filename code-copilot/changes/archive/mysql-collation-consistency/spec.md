# MySQL 排序规则一致性修复

> status: complete
> created: 2026-08-25
> complexity: 🟡中等

## 1. 背景

Forge Admin 新环境同时存在 `utf8mb4_0900_ai_ci`、`utf8mb4_unicode_ci` 和
`utf8mb4_general_ci`。文档按旧排序规则创建数据库时，全量初始化表使用 MySQL 8 默认排序
规则，后续 Flyway 临时表又继承数据库默认值，字符串列比较可能在
`V1.0.114__seed_presale_business_process.sql` 报 `Illegal mix of collations`。

## 2. 目标

- [x] 文档、`init-db.sh`、Docker Compose 和全量初始化 SQL 统一使用
  `utf8mb4` + `utf8mb4_0900_ai_ci`。
- [x] 报表、消息、通知等仍用于新环境初始化的独立 SQL 不再显式声明旧排序规则。
- [x] 初始化脚本在数据库已存在时也校正数据库默认排序规则，避免临时表继续继承旧值。
- [x] 提供可重复执行的静态一致性检查，阻止旧排序规则重新进入活动初始化入口。

## 3. 兼容边界

- 运行环境为 MySQL 8.0+；`utf8mb4_0900_ai_ci` 不兼容 MySQL 5.7。
- 不修改任何已发布的 `db/migration/V*.sql`，避免已执行环境发生 Flyway checksum mismatch。
- 不批量转换已有业务库的表或列；有数据的历史库需先备份，再按文档排查并由 DBA 制定转换方案。
- `db/backup`、历史变更 Spec 和描述历史故障原因的注释不纳入活动初始化门禁。

## 4. 验收标准

- `init-db.sh` 创建并校正数据库默认排序规则为 `utf8mb4_0900_ai_ci`。
- Docker MySQL 服务端默认排序规则为 `utf8mb4_0900_ai_ci`。
- 两份全量初始化 SQL 及活动独立 SQL 中不存在显式
  `utf8mb4_unicode_ci` / `utf8mb4_general_ci` 声明。
- Forge 项目内面向开发者的建库示例与公开文档保持一致。
- Shell 语法、Docker Compose 解析、静态一致性脚本和差异检查通过。
- 不启动真实 MySQL、Admin 或 Flyway；真实初始化由用户侧环境验收。

## 5. 实施结果

- 手工初始化、Docker 服务端、两份全量 SQL、活动独立 SQL 和项目内建库说明已统一到
  `utf8mb4_0900_ai_ci`。
- 全量 SQL 会先校正当前数据库默认值，再设置连接排序规则；Flyway 临时表因此继承相同默认值。
- 已发布 Flyway 文件无工作区改动，避免 checksum 回归。
- 静态一致性、Shell 语法、Compose 配置解析、文档命令扫描和补丁检查通过。
