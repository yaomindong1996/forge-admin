# T3 执行记录

## 2026-10-12：开始

- 当前分支 codex/spring-boot4-t0-compatibility，HEAD 719a2fc0b28d929edef62769dc2adc5e75ef557e。
- 保留 307 条已有状态；main、其它项目、历史 SQL 和用户无关修改不动。
- 当前 1.2.0 / Boot 4.1.1 / AI 2.0.1 / Flowable 8.0.0，不自动升版/提交/推送/部署。
- 读取规则、T2 Spec/Tasks/Test/Log/Report，复用现有 665 + 独立 28 项记录。
- 使用 OpenAI Docs 核对函数调用文档（Java 参考获取超时，函数调用页成功），没有 API Key 工具。
- 使用 Forge 流程技能读取采购/状态/验证参考；本轮没有新 BPMN/业务表或真实状态操作。
- 询问 Agent 普通登录与开放客户端委托的选择；未确认前保留现有鉴权，不自动提升客户端权限。

后续命令、失败和修复在实际执行后追加，不预填通过。

## 基线失败与增量修复

| 日志（/private/tmp/forge-boot4-t3- 前缀） | 实际结果与处置 |
| --- | --- |
| baseline1.log | 根 Reactor 在 generator 中止：1,233 项中 10 失败、73 错误；未执行的下游不计通过 |
| generator2.log | 临时 JDK 的 Mockito 自附加失败，是测试环境错误；预加载已有 Byte Buddy Agent，不关闭 MockMaker/断言 |
| generator3.log | 恢复五项排除后暴露旧测试乱码和缺 ObjectMapper 构造参数；只规范编码/显示文案并补真实依赖 |
| generator4.log | 1,267 项中 7 失败/27 错误；修反射祖先字段、应用/打印 DTO、H2 新列、租户列 fixture、审计缓存隔离 |
| full2/full3.log | 收敛至一项 H2 打印 Binding 初始化缺列；补当前业务来源和固定版本列，不改已执行迁移 |
| full4.log | AI 模型治理旧 Mock 缺 getOptions，3 项断言失败；补 AI 2 默认选项，保留调用/健康/审计恰一次断言 |
| full5.log | 幂等夹具无关 stub/旧 hash API，1 失败9错误；stub 下放实际使用用例，状态缺失/未知新增失败关闭测试 |
| full6.log | 53 模块 package 全通过，3,397 项；未指定子集，0 失败/错误/跳过 |
| clean-final.log | 53 模块 clean package 全通过，同为 3,397 项，完成于 09:08:03，耗时 5:49 |

首轮打印故障不能只归因 Jackson：isDefault wire 名字固定后，扩展 PrintSourceRequest 的两个 null 字段
仍被旧快照键白名单拒绝。只兼容这两个已知 null，注入非空独立业务来源仍拒绝。
公式错误收集器遗漏 FORMULA_ERROR，违反已有公式发布阻断规范；恢复阻断而非改弱测试。
DataAudit 的首项失败是上一测试类遗留全局租户索引，fixture 的 before/after 都清理当前合成租户。
逻辑删除、授权、只读/可写和存量发布版本负面断言没有删掉。

## PC/H5/Report 与工具回归

- admin-ui-test1.log：pnpm 11 运行脚本触发依赖检查并要求重建 modules，因无 TTY 中止。
  保留已有 modules，不批准清空，改为用 Node 20.19.0 直接运行已安装的 Vitest/Vite。
- admin-ui-test2.log：316 文件，4 个收集失败、10 个断言失败（已收集 2,258 项）。
  组件单测隔离完整路由；属性/画布操作条与已存在的公共组件契约对齐；完整 24 个挂件逐键锁定。
  lazy 运行面板等待实际加载；权威 uiDocument 不补回画布外旧字段；不恢复旧白色 hover 或不一致骨架。
- focused1/2/3/4：路由恢复后发现嵌套初始选择早于布局 hydrate、空抽屉读 null，做真实实现修复；
  mount 子面板而非 shallow stub，数据绑定的独立消息组件才 stub；无效/删除节点与手动选择均补回归。
- admin-ui-final.log：316 文件 / 2,279 项通过。
- admin-ui-final2.log：最后的选择防覆盖条件变化使一项旧源码字面断言失败；行为测试通过，
  改为同时检查递归选择和不覆盖用户选择的条件，不移除用例。
- admin-ui-final3.log：316 文件 / 2,279 项全通过，09:09:06 开始、耗时 73.83 秒。
- h5-test1.log：191 项 Node 用例通过；同目录一份 Vitest 用例用错运行器，报模块不存在。
  h5-test2.log 191 项 Node 用例全通过；h5-vitest1.log 使用保存的独立配置，2 项全通过。
- tooling1.log：现有脚手架/源码插件/构建与交付工具 520 项全通过，0 跳过；合成制品与 Docker 桩，不部署。
- admin-ui-build-final.log：最终代码生产构建通过（1m55s）；h5-build1.log DONE；report-ui-build1.log 33.68s。
  Report 初始无 node_modules，按现有锁文件安装，frozen-lockfile + ignore-scripts；锁文件未改。
- 独立 verification 工程 clean test：28 项全通过，实际完成时间为 08:58:01。
  复用真实采购 BPMN/H2/JSON 字段权限及真实供应商适配器 loopback；不叠加重复的供应商测试数量。

## 质量与范围

- Java 五项 testExcludes 全部移除；当前生产测试未发现 @Disabled / exclude 标签。
- 新 Java 生产类 20–112 行；ListPageGridDesigner SFC 981 行。旧 part1 JS 只改监听条件，行数不增长。
- 修改的测试文件执行定向 ESLint；格式修正后继续验证，没有加 eslint-disable。
- 两处历史源码 ESLint 70 + 1,156 条诊断，HEAD 同规则也是 70 + 1,156：按 rule/message 比较新增 0。
  不将它称作“ESLint 全绿”，不借修两行全量格式化巨型编排文件。
- 版本检查为 1.2.0；diff --check 通过。四服务 JAR 在原 target 生成，未启动业务服务。
- main / ForgePro / 历史 SQL / 无关图标与部署改动保留；没有 commit、push、真实配置写入或部署。
- 用户明确选择“保留现有客户端鉴权”；MCP/能力现有独立授权、委托、确认和审计仍做全模块回归，
  不伪造 clientId、exchange 或给普通 Agent 用户加授权。

## 可复跑命令

仓库根目录，使用 Java 17、当前依赖缓存的 Byte Buddy Agent 和 Maven 3.9.11：

```bash
JAVA_TOOL_OPTIONS='-javaagent:<本机 byte-buddy-agent-1.18.11.jar 绝对路径>' \
mvn -B -ntp -f forge-server/pom.xml -Pdev,enable-tests clean package -Dstyle.color=never

mvn -B -ntp -f code-copilot/changes/spring-boot4-t1/verification/pom.xml \
  -Penable-tests clean test -Dstyle.color=never

node scripts/forge-shared/version-cli.mjs --check
node --test $(rg --files scripts | rg '\.test\.mjs$')
git diff --check
```

Node 固定 20.19.0；下列命令分别在对应目录执行，不能粘贴多个 cd 混在同一个 shell：

```bash
# forge-admin-ui
node node_modules/vitest/vitest.mjs run --maxWorkers=4 --minWorkers=1
node --max_old_space_size=8192 node_modules/vite/bin/vite.js build

# forge-h5-ui：Node 与 Vitest 是两套既有用例
node --test $(rg --files src | rg '(/__tests__/.*|/tests/.*)\.(spec|test)\.[jm]s$' | rg -v 'version.spec.js$')
node node_modules/@dcloudio/vite-plugin-uni/bin/uni.js build

# 仓库根目录：仅运行上述版本用例，未排除业务用例
node forge-admin-ui/node_modules/vitest/vitest.mjs run \
  --config code-copilot/changes/spring-boot4-t3/verification/h5-vitest.config.mjs

# forge-report-ui（无既有单测定义）
pnpm --ignore-workspace install --frozen-lockfile --ignore-scripts
node --max_old_space_size=4096 node_modules/vite/bin/vite.js build
```

真实环境门禁及既有图像/多节点/原子 Token 遗留见 compatibility-report，未以本地通过代替验收。

## 最终收尾

- clean-final.log 的逐类 Surefire 汇总再次核对为 3,397 项、0 失败/错误/跳过；包含嵌套类，
  不把每模块总行重复累加。四服务 JAR 生成时间为 09:07–09:08。
- 最后仅格式化测试与改未使用正则捕获，不改业务实现；format-regression.log 的相关 54 项通过。
  修改的 11 个测试文件定向 ESLint 全通过（eslint-tests-complete.log 空输出）。
- 源码 lint 仍明确列为 1,226 条存量诊断，新增 0；不宣称全项目 lint 或真实 E2E 通过。
- 最后 diff --check 和版本检查通过，仍为 1.2.0、同一兼容分支与原 HEAD。
- 未 Git 提交、推送、切换 main、部署、创建业务服务 PID 或连接真实环境。
