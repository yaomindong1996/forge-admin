# 增量验证计划

P0：源码只取准确提交；拒绝覆盖已有目录；拒绝主分支/脏工作区同步；不混入本地配置。
P0：旧基线不重新生成；新旧公共修改三方合并；冲突可恢复；SQL 修改/删除拦截。
P0：新库只读存在性确认后创建，所有写操作限定新库；真实凭据不进入 Git。
P1：产品版本和社区版本独立；生成选项/映射摘要准确；新克隆保留旧基线可升级。

命令：Node 测试（同步、生成器、版本、数据库脚本）；一次 full 精简生成；生成端数据库脚本测试；
新工程后端聚合 compile/package、三个前端 build、版本检查；新库迁移历史/默认身份/演示表核查。
不部署、不推送、不执行旧库升级。外部环境不足项在执行记录注明。

## 初始化实测发现的增量项

- 新包名包含旧前缀时禁止二次替换，覆盖完整包段、Java 源码路径与 POM mainClass。
- Maven groupId 与 Java basePackage 不同时，POM 坐标不得被后续包名替换覆盖。
- 同域不同路径部署时，独立工程的登录态与协商密钥必须使用独立浏览器存储键。

## Redis 独立库与 TLS 决策的增量验证

- 四服务本地示例及忽略的实际配置：Spring Redis、Redisson、Sa-Token 均为 DB 15。
- 四服务基础 Sa-Token 配置跟随 Spring Redis 库号，缺省 15；Docker 示例两处库号为 15。
- 新增配置契约断言，复跑 `node --test scripts/project-baseline.test.mjs`；
  忽略配置只检查库号、强制 TLS、忽略状态及权限，不输出其凭据。
- 复跑模板和现有 full 生成端的 `init-db.test.mjs clean-db.test.mjs`（脚本测试，不连接真实数据库）。
- `version:check`、`forge:upstream verify`、`git diff --check`；本轮不变更 Java/前端源码，不重复全量构建。
- 用户暂缓真实建库，本轮跳过 MySQL/Redis 连接、迁移、启动与登录等运行验收。
