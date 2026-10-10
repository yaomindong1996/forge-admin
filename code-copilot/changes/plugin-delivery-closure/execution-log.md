# 执行记录

## 2026-10-09：范围核对

- 分支 codex/plugin-foundation；website 保持 codex/plugin-source-delivery，不混分支。
- 已检查现有客户 SSO、源码下载、安装器、生成器、封存制品和审批核验实现。
- 用户确认：COS 制品库、Docker Compose 部署。本轮不实际操作远端环境。
- 现有未提交的制品登记文件不属于本轮变更，保留原样。
- 当前仅完成读代码 / Spec，尚未执行本轮自动化测试。

## 2026-10-09：D1 客户源码获取

- CLI 新增 market owned / versions / check / add，复用 website 客户 SSO 和真实源码接口协议。
- 配置不放会话；PKCE verifier / state 与客户会话仅存在内存，退出擦除并关闭回调监听。
- 下载验真后调用已有安装预检和事务；--reviewed / --force 不绕过客户定制保护。
- `node --test scripts/forge-plugin/market/market.test.mjs scripts/forge-plugin/market/sso.test.mjs`：23/23 通过。
- `node --test scripts/forge-plugin/market/generated.test.mjs scripts/forge-plugin/*.test.mjs scripts/forge-create/*.test.mjs`：246/246 通过。
- 原模板 DB stub 31/31 通过；full 生成工程相同测试通过。单独复跑 generated 用例 1/1 通过。
- 全部新增生产模块 `node --check` 通过，`git diff --check` 通过。
- 工作区开源边界检查通过（索引 9741 / 工作区 9787）；提交前另检查暂存原始内容。
- 初次本地网络测试被沙箱拒绝监听，授权仅 loopback 测试后通过，不连接远端。
- 初次子测试继承 NODE_TEST_CONTEXT 导致 TAP 输出为空；清除子进程测试 IPC 上下文后严格断言通过。
- 接口状态与 website 实现核对为大写 PUBLISHED，Long ID 按精确字符串处理，无数字截断。
- 本轮没有 Java / Vue 改动，不执行无关服务聚合构建；网站真实 SSO / MySQL / COS / Docker 验收未运行。
- 测试服务全部由测试关闭，临时生成工程已由测试回收；用户服务和工作区存量文件未清理。

## 2026-10-09：D2.2 离线 Compose 准备

- D1 本地提交 14419e6a，只包含该阶段 17 个文件，原有候选制品登记改动没有暂存。
- 新增 Compose check / prepare --reviewed / verify；复用封存清单、流式复制与实际字节核验。
- 镜像锁定 digest，敏感配置不复制，仅挂载私有普通文件；准备不启动容器，deployed 保持 false。
- `node --test scripts/forge-plugin-release/compose/compose.test.mjs`：15/15 通过。
- 测试使用实际本地字节及合成 JAR / UI 夹具，不代表真实可启动应用或 Docker 运行验收。
- full 生成工程源码 / Compose 模块原字节复制和 DB stub 基线复跑 1/1 通过；实际子测试 fail 0。
- Compose 生产模块 `node --check`、本轮 `git diff --check` 通过；没有 Java / UI 改动。
- COS SDK 依赖选择待用户答复；未引入依赖、未运行任何真实 COS 上传或目标环境操作。
- D2.1 / D2.3 / D2.4 仍待完成，不能宣称第 2 项已经闭环。

## 2026-10-09：D2.1 / D2.3 开发收尾

- 用户明确要求完成全部剩余开发，排除真实环境验证；不连接共享数据库、不上传COS、不部署服务器。
- 原工作区候选登记作为闭环依赖纳入增量审查和验证；保留无关 `.DS_Store`，website工程未修改。
- 独立工具锁定官方COS SDK 3.0.0及lockfile，真实流式上传/取回与读回核验已实现，SDK安装验证通过。
- 新增授权目标、发布/部署/恢复任务、当前审批绑定、幂等请求、目标行锁、独立机器认证、租约与审计。
- Vue插件中心使用真实API、字典及Pinia；冻结重试请求、备份/兼容确认、人工核查和恢复完整接入。
- rootless Compose执行器使用固定argv，不运行shell、不拉镜像、不down/rm；检查实际资源限制及只读挂载。
- 独立探针核对当前JAR/插件目录，UI实际HTTP逐文件验SHA；核验成功之后才更新当前版本。
- unknown保留目标和本地锁；人工关闭保留原说明和核查审计，认证归档后仅恢复已确认版本。
- Admin JAR随包携带迁移，保持本地源码启动默认配置；机器JSON协议仅由认证服务端属性选择，
  保留防重放，不接受客户端Header冒充。部署身份配置禁止数据库覆盖。

### 修复与增量自审

- 迁移测试发现资源唯一约束不允许三个相同GET权限，改为一个模块通配资源；没有自动授权角色。
- 修复未知回执被误作允许解锁；真正发起up前的配置失败保持failed，不冒充发生过切换。
- 并发同请求在目标锁后做当前读，返回同一任务；旧列表/异常回执不覆盖UI或丢弃冻结请求。
- 新Java类/SFC均小于1000行；请求编排拆出领域模块，新生产方法按80行/120字符限制检查。
  DecryptRequestBodyAdvice原有121字符行保持不变，没有扩大存量问题。
- Spec契约与代码质量两阶段增量自审PASS；源码、上传、运行和授权证据分离，不宣称热安装或真实验收。

### 最终本地验证

- Node完整回归550/550通过，0失败/0跳过：COS/Compose/执行器负例、源码安装/生成、rootless边界、
  模板DB脚本桩、fresh full生成工程原字节复制与生成工程DB子测试。
- Maven使用`-Penable-tests`，9组选定测试实际72/72通过，0失败/0跳过：真实XML/H2/Spring事务、
  并发、审批、租约、恢复、权限/认证、迁移唯一约束、配置保护及机器JSON协议。
  初次没有测试profile的Maven调用未运行用例，不计入通过数量。
- Vue插件域Vitest：12个文件51/51通过；全部改动UI文件ESLint通过，没有整文件禁用规则。
- Admin聚合`package -DskipTests`成功；包内核对V1.0.214/215/216迁移及system/core/crypto模块。
- UI生产构建成功；既有chunk/插件耗时警告保留，未扩展到全站性能优化。
- 改动MJS语法检查和`git diff --check`通过，开源边界另核对工作区与暂存原始内容。
- 临时页面使用实际Naive UI组件和隔离合成数据，浏览器验证目标/动作选择、恢复确认、弹窗及按钮，
  无控制台错误。生产源码无模拟API；不以此检查替代真实登录/RBAC/部署E2E。
- `pnpm exec`因依赖目录非TTY保护中止，改用已安装Node20和项目内工具入口；未删除/重装业务前端依赖。

主要命令（原始环境日志只保留本轮私有临时目录，不提交）：

```bash
node --test scripts/forge-create/*.test.mjs scripts/forge-plugin/*.test.mjs \
  scripts/forge-plugin/market/*.test.mjs scripts/forge-plugin-builder/*.test.mjs \
  scripts/forge-plugin-release/*.test.mjs scripts/forge-plugin-release/compose/*.test.mjs \
  scripts/forge-plugin-release/cos/*.test.mjs scripts/forge-plugin-release/delivery/*.test.mjs \
  scripts/forge-shared/*.test.mjs scripts/guards/*.test.mjs forge-server/scripts/db/*.test.mjs
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test \
  -Dtest=SysPluginDeliveryTest,SysPluginDeliverySecurityTest,PluginRuntimeProbeTest,MachineJsonProtocolTest,SysPluginArtifactTest,SysPluginArtifactSecurityTest,SysPluginMigrationTest,SysPluginReleaseCheckTest,SysConfigServiceCryptoGuardTest \
  -Dsurefire.failIfNoSpecifiedTests=false
mvn -pl forge-admin-server -am package -DskipTests
node node_modules/vitest/vitest.mjs run src/views/system/plugin/__tests__
node node_modules/vite/bin/vite.js build
node scripts/guards/check-edition.mjs
```

### 保留项

仅保留部署后的真实SSO、COS策略/读回、MySQL迁移、LinuxrootlessCompose、许可证及业务冒烟验收。
自动支付、付费源码构建开放、独立Pro仓库和动态JAR热安装不在本轮已确认开发范围。

### 提交前收尾

- 暂存124个闭环相关文件；新文件EOF空行已机械整理，暂存`diff --check`通过。
- 开源边界检查通过：索引9846 / 工作区9846，包含暂存原始字节；未暂存用户 `.DS_Store`。
- 本轮隔离浏览器页已关闭，核对命令后仅停止临时61480服务，复核无监听；其他用户服务未操作。
- 保持codex/plugin-foundation，不推送、不合并main；真实环境验收仍待部署方执行。
