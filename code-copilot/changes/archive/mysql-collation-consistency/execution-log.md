# MySQL 排序规则一致性修复执行记录

> status: complete
> created: 2026-08-25

## 2026-08-25 实施启动

- 新建分支 `fix/mysql-collation-consistency`。
- 工作区已有 `.DS_Store` 修改/删除，保持不动且不纳入本次提交。
- 已确认故障链路：数据库默认 `utf8mb4_unicode_ci` 与全量初始化表的
  `utf8mb4_0900_ai_ci` 不一致，Flyway 临时表继承数据库默认值后与业务表比较失败。
- 已发布 Flyway 迁移保持只读；真实 MySQL/Flyway 验收留给用户侧环境。

## 2026-08-25 失败基线

- 新增 `forge-server/scripts/db/check-collation-consistency.sh` 后，在生产文件修改前执行失败。
- 基线识别出：`init-db.sh` 创建规则错误且未校正已有数据库、Compose 服务端规则错误、
  全量/报表/消息/通知 SQL 仍存在旧显式排序规则。
- 首次基线同时发现检查脚本对以 `--` 开头的搜索模式缺少参数终止符，修正检查脚本后再实施生产修改。

## 2026-08-25 实施内容

- `init-db.sh` 的 MySQL 客户端固定为 `utf8mb4`，创建数据库后执行 `ALTER DATABASE`，
  无论数据库为新建还是预先创建，后续临时表都继承 `utf8mb4_0900_ai_ci`。
- 两份全量 SQL 在文件开头执行 `ALTER DATABASE` 和带显式 `COLLATE` 的 `SET NAMES`。
- Docker MySQL 服务端默认排序规则改为 `utf8mb4_0900_ai_ci`，移除官方镜像不识别且容易误导的
  `MYSQL_CHARSET` 环境变量。
- 统一两份全量 SQL 的 3 处遗留声明，以及报表、消息和通知插件活动 SQL 的旧声明。
- 统一项目内开发者建库示例；公开文档已由提交 `c9ba817` 统一并完成构建验证。
- 不修改 `V1.0.114` 或其他已发布迁移；历史迁移中的旧声明和描述历史故障的注释按兼容边界保留。

## 2026-08-25 自动化验证

- `bash forge-server/scripts/db/check-collation-consistency.sh`：通过。
- `bash -n forge-server/scripts/db/init-db.sh`：通过。
- `bash -n forge-server/scripts/db/check-collation-consistency.sh`：通过。
- `forge-server/scripts/db/init-db.sh --help`：通过。
- `docker compose -f docker-forge-admin/docker-compose.yml config --quiet`：通过。
- 公开文档 `CREATE DATABASE` / `DEFAULT COLLATE` / `collation-server` 旧规则命令扫描：通过。
- `git diff --quiet -- forge-server/db/migration`：通过，已发布 Flyway 文件无改动。
- `placeholder-replacement: false` 配置确认：通过，迁移内已有业务消息模板变量无需改动。
- `git diff --check`：通过。

## 跳过与环境说明

- 未启动真实 MySQL、Admin 或 Flyway，避免影响本地数据和服务。
- 本机 MySQL 客户端为 5.7；目标 `utf8mb4_0900_ai_ci` 要求 MySQL 8.0+，真实初始化留给用户环境验收。
- ShellCheck 未安装，已用 `bash -n` 覆盖语法检查。
