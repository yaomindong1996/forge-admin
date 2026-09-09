# Framework Security Gap Hardening Execution Log

## 2026-08-02 初始化

- 阶段：Proposal -> Apply。
- 已读取：根 `AGENTS.md`、项目记忆、编码规范、安全规则、自动化测试标准。
- 当前分支：`main`。为避免影响并行 Agent，不切分支、不提交。
- 并行冻结文件：capability 三个 Java 文件、能力客户端页面、`TenantInterceptor.java`、`LogProperties.java`、`OperationLogAspect.java` 及 `.DS_Store` 用户改动。
- 已完成：18 项源码静态核验；确认 app-server YAML 冲突当前不存在，X-Tenant-Id 回退正常依赖图不可达，AES-128 与 WorkerId 耗尽描述需要降级。
- 服务：本轮尚未启动任何服务。

## 2026-08-02 实施与收尾验证

- 完成范围：Crypto、WebSocket/Auth、Tenant handler、LocalFileStorage、DataScope、Job、登录密码、Message、Idempotent、WorkerId、Flow 未实现能力安全收口。
- 后端目标测试：JDK 17 下执行 15 个相关测试类，58 条全部通过；`DefaultIdempotentKeyGeneratorTest` 收尾复跑 2/2 通过。
- 消息边界复核：新增 SKIPPED/零成功断言后先失败，修正聚合规则为“零成功=失败、部分送达=部分成功、全部送达=成功”，`MessageServiceImplTest` 最终 2/2 通过。
- 聚合构建：`mvn -pl forge-admin-server -am package -DskipTests` 47 模块通过；并行 capability 模块只参与编译，没有修改其源码。
- Admin UI：Node 20.19.0 下 `pnpm build` 通过；保留既有组件命名、CSS 注释和动态导入警告。
- H5：首次 `pnpm build:h5` 暴露 `node_modules` 缺失以及 `glob`/`sass` 未声明；补齐直接开发依赖、对齐 Glob 11 命名 API 后构建通过。仍有既有 peer dependency、`vue-i18n` 过期和 uni-app 版本提示。
- 环境纠偏：首次幂等测试误用系统 JDK 8，报 `无效的目标发行版: 17`；显式使用 `/opt/homebrew/Cellar/openjdk@17/17.0.13/...` 后通过，确认不是源码失败。
- Flyway：`V1.0.80__add_message_partial_send_status.sql` 当前版本唯一且最高；新脚本无 `${...}`，防重复查询限定 `tenant_id=1`、业务键和 `del_flag=0`。全目录扫描命中的 `V1.0.72` 为已提交历史模板变量，本轮未修改。
- 静态检查：`git diff --check` 通过，无 Git 冲突标记；开始与结束的 capability/`TenantInterceptor`/日志切面/`.DS_Store` 冻结清单一致，本变更未新增这些文件的修改。
- 服务与外部状态：未启动服务，未连接 MySQL/Redis，未执行真实 Flyway、HTTP 或 STOMP 握手；没有需要清理的 PID。
- 上线门禁：配置真实内部调用来源 IP/CIDR、剥离外部 `X-Inner-Call`、配置生产 WebSocket Origin、启用 Job Open API 前注入至少 32 字符 pepper，并完成超管目标租户/审计语义设计。
