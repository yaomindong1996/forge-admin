# MySQL 排序规则一致性修复 Implementation Plan

> status: complete

**Goal:** 让 Forge Admin 所有新环境初始化入口稳定采用
`utf8mb4_0900_ai_ci`，消除由数据库默认值和表/列显式值不同导致的比较错误。

## Task 1: Spec 与失败基线

- [x] 固化活动初始化范围、Flyway checksum 边界和 MySQL 8.0+ 前提。
- [x] 新增静态一致性检查脚本。
- [x] 在修改生产文件前执行检查并记录失败基线。

## Task 2: 统一初始化入口

- [x] 修改 `forge-server/scripts/db/init-db.sh` 的建库与默认值校正逻辑。
- [x] 修改 `docker-forge-admin/docker-compose.yml` 的 MySQL 服务端排序规则。
- [x] 统一服务端与 Docker 两份全量初始化 SQL。

## Task 3: 统一活动独立 SQL 与项目说明

- [x] 统一报表、消息、通知插件独立初始化 SQL。
- [x] 统一 `AGENTS.md` 与 `code-copilot/rules/project-context.md` 建库示例。
- [x] 确认公开文档仍为 `utf8mb4_0900_ai_ci`。

## Task 4: 验证与交付

- [x] 执行静态一致性、Shell 语法、Compose 解析和 Flyway 保护检查。
- [x] 执行 `git diff --check` 并复核未改写已发布 Flyway 脚本。
- [x] 回填执行日志并提交，不自动 Push。
