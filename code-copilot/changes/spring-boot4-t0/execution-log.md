# T0 兼容性验证执行日志

## 2026-10-11：初始化

- 基线提交：`719a2fc0b28d929edef62769dc2adc5e75ef557e`；分支 `codex/spring-boot4-t0-compatibility`。
- 发行版本：1.2.0，不升版、不发版、不部署。
- 开始时已有 `.DS_Store`、multicolor-navigation-icons 四份记录及 production-runbook 的改动，保持原样。
- 已读取 AGENTS、自动化测试标准、流程 Skill 与采购参考 / 状态 / BPMN / 验证清单。
- 默认 shell 没有可用 mvn / Java；只读定位已有工具，未安装或改写系统环境。
- 可用工具：Maven 3.9.11；Temurin Java 17.0.20.1，位于已有 `/private/tmp` 工具目录。
- 沙箱 DNS 无法解析 Central；提升权限后只读元数据查询成功，未访问业务环境。
- Central 已确认：Boot 4.1.1 / 4.0.8、Flowable 8.0.0、Spring AI 2.0.1 和 Alibaba 2.0.0-M1.1。
- Alibaba 主 BOM / Extensions BOM 的 2.x 均为 milestone；尚未运行编译与测试，不宣称兼容。

## 2026-10-11 20:04–20:25 +08:00：隔离验证与 clean 复跑

### 变更与命令

只新增变更目录的 Spec / Task / Test / 报告和独立 Maven 验证工程。
直接编译真实生产源文件，不修改正式 POM，不使用已安装旧 Forge JAR；没有修改真实审批/权限数据。

工具与 settings：

```bash
FORGE_T0_MVN=/private/tmp/apache-maven-3.9.11/bin/mvn \
FORGE_T0_JAVA_HOME=/private/tmp/forgepro-build-tools.5kNsIl/jdk-17.0.20.1+1/Contents/Home \
FORGE_T0_SETTINGS=/private/tmp/apache-maven-3.9.11/conf/settings.xml \
bash code-copilot/changes/spring-boot4-t0/verification/run-matrix.sh
```

使用已有工具目录和干净 Maven settings，不修改用户默认镜像 / 凭据配置。
最终 runner 逐项执行 `mvn -P<profile>,enable-tests -pl <module> clean test`。
仅清理隔离验证模块当前 profile 生成目录，不执行 `install` / `deploy`。

### 结果

最终日志目录 `verification/target/logs/matrix.ekBXnV/`：

| profile / module | 测试数量 / 错误 | 退出 |
|---|---|---|
| baseline / flow | 8 / 0 | 0 |
| baseline / AI | 4 / 0 | 0 |
| baseline / MCP | 2 / 0 | 0 |
| target / flow | 8 / 0 | 0 |
| target / AI | compile：OpenAiApi 包已不存在 | 1 |
| target / MCP | testCompile：旧 JSON mapper / transport 不存在 | 1 |
| target / AI runtime | 7 / 2（通义缺 ToolExecutionEligibilityPredicate） | 1 |
| old-AI / AI | compile：HttpHeaders.entrySet 不存在 | 1 |
| Alibaba-upstream / AI | compile：HttpHeaders.entrySet 不存在 | 1 |
| Alibaba-upstream / AI runtime | 通义相同三项 3 / 0 | 0 |

所有执行到 Surefire 的项断言失败为 0，跳过为 0；通义 target 的两项是运行错误。
目标新 OpenAI SDK 4 项全部通过，原生通义两项在模型构造阶段失败，无 HTTP 请求到供应商。
矩阵退出 1、5 个失败构建条目，兼容性结论保留为阻塞，不写“全部通过”。

首轮流程工程误引旧 `BpmnJsonConverter`，日志 `flow-boot3-baseline.log` 保留；
纠正隔离测试后 `flow-boot3-baseline-r2.log` 8 项通过，最终 baseline / target 又各 clean 8 项通过。
旧 JSON converter 错误不属于本项目生产源码，不列入升级阻塞。

### 依赖树

Java / Maven / settings 同上，分别使用 `boot3-baseline` 和 `boot4-target`：

```bash
mvn -B -ntp -f code-copilot/changes/spring-boot4-t0/verification/pom.xml \
  -Pboot4-target -pl flow-probe,ai-probe,mcp-probe dependency:tree \
  '-Dincludes=org.springframework:*,org.springframework.boot:*,org.springframework.ai:*,com.alibaba.cloud.ai:*,io.modelcontextprotocol.sdk:*,tools.jackson.core:*,com.fasterxml.jackson.core:*'
```

两组命令均退出 0。日志 `boot3-baseline-dependency-tree.log` / `boot4-target-dependency-tree.log`。
实际 Spring 6.2.17 → 7.0.9；MCP 0.18.3 → 2.0.0；目标 Jackson 3.1.5 与部分 Jackson 2.21.5 共存。
dependency:tree 的成功只代表解析，不代表上面失败的源码/运行测试成功。

### 警告与边界

- 冷缓存解析公共依赖较慢，部分构件来源重新核验，不是源码故障；没有清理用户 Maven 仓库。
- ai-probe 只带 MP 注解，产生缺少 MyBatis JdbcType 元信息警告，两组均存在；不加载旧 ORM Starter。
  单独 runtime probe 加入对应 MyBatis 类型依赖，缺类错误仍真实重现，不属于此警告。
- 基线 MCP Jackson mapper 已标记移除；目标 ChatModel.getDefaultOptions 有弃用警告，列入 T1 改造。
- 真实 MySQL 7→8迁移、真实模型、完整 Forge MCP/AI/RAG、PC/H5、全部服务打包启动未跑，均属 T1。
- 未调用数据库修复、真实审批、外部计费 API、安装/发布操作；无生产状态变更。

### 收尾

- `bash -n verification/run-matrix.sh` 退出 0；Java / POM / shell 新代码单行长度检查无超 120 字符项。
- `git diff --check` 退出 0；正式 POM 和 `forge-server/db/migration` 与基线对比退出 0。
- 测试只在自身 JVM 使用 H2 和 loopback HTTP；HTTP 逐项 close，所有本轮 Maven / Surefire JVM 已退出。
  收尾 ps 只匹配诊断命令本身，没有遗留 T0 服务。没有停止用户其它服务。
- 在 backend pitfalls 追加本次真实版本混配故障，供下次升级复用；不改用户已有其它变更记录。
- 未 stage / commit / push / deploy，未修改 ForgePro。分支仍为 `codex/spring-boot4-t0-compatibility`。

### 两阶段自查

1. Spec 一致性：独立分支与验证工程、不修改生产版本/数据库/SQL、真实源文件编译、
   Boot 3 基线先跑、负面结果保留；全部符合本轮授权。T0 完成不解除正式升级门禁。
2. 代码质量：POM 的源文件白名单与编译日志数量一致；新 API PoC 与旧适配器失败分开报告；
   本地 HTTP 只绑定 loopback，逐项关闭，合成 Key 不是真实凭据；类/方法规模在规范内。
   最终 clean 复验覆盖整理后的 POM / 测试 / runner，未用旧 class 或跳过测试制造绿色结果。
