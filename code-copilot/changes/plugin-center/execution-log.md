# 插件中心执行记录

## 2026-10-08：P3.3b1 本地候选制品封存交付

### 范围与两阶段增量自审

- Stage 1：按先补充的Spec实现独立forge:plugin-release check/publish/verify，
  不在Web读工作区/执行代码，不改后台任务/权限/状态/数据库，不选未授权远程仓库或部署目标。
  publish必须本地--reviewed；快照明确liveTaskApprovalVerified=false、deployed=false。
- 固定原始result.json SHA、jobId、源码/包/镜像/插件/核心版本及Admin范围；
  复用现有严格JSON/ID/版本/路径/实际产物核验，有界读取与流式复制，声明逐项比对。
  SHA算法与worker产物清单一致，但原报告文件SHA与服务端报告SHA明确区分。
- 私有canonical非root目录、独占锁、随机暂存、逐文件再散列及规范清单内容寻址。
  只创建新快照；已有同ID必须实际复验，损坏不覆盖，失败暂存/失联锁保留供人工核查。
  文件0400/目录0500为工具约定，不是签名/防管理员写入；回执时间无签名认证。
- Stage 2：核对配置白名单/JSON重复键/非字符串摘要、路径别名/重叠/软硬链接、
  中断/锁冲突/复制中改变/已有快照破坏、POSIX身份/权限与输出脱敏；没有新依赖、密钥或真实数据。
  只复制JAR/UI和固定元数据，不复制源码/配置/原ZIP/日志；部署准备仅artifact_integrity通过，
  实时授权/目标/备份/迁移/部署健康全部pending。新生产模块26–95行，方法≤80行/参数≤5，按域拆分。
- project-init技能影响：运行工具在改名之后原字节交付，保留原Forge协议及共享规则；
  验证最终完整full生成工程，不拿局部目录复制冒充生成工程。

### 环境、命令及证据

- QA_DIR=`/private/tmp/forge-plugin-release-p3.TXIDli`，Node20.19.0；codex/plugin-foundation起点20c5e2b5。
  没有安装依赖或连接158/生产环境，不启动任何服务；用户.DS_Store保留未提交。
- 最终`node-sealed.tap`451/451、失败/跳过0、22.43秒；新增release54项。
  source init/clean DB31已包含在完整矩阵，mysql/Maven为桩，未创建真实数据库。

```bash
node --test scripts/forge-shared scripts/forge-plugin scripts/forge-plugin-builder scripts/forge-plugin-release \
  scripts/guards scripts/forge-create \
  code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  code-copilot/changes/plugin-center/contracts.test.mjs \
  code-copilot/changes/plugin-center/workbench-contract.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
node scripts/forge-create/create-project.mjs <QA_DIR>/sealed-generated --preset full \
  --project-name plugin-release-check --java-name PluginReleaseCheck \
  --base-package com.acme.release --group-id com.acme.maven \
  --artifact-prefix release-host --module-artifact-prefix kernel \
  --display-name 插件制品验证 --database-name plugin_release_check
node --test <QA_DIR>/sealed-generated/release-host-server/scripts/db/init-db.test.mjs \
  <QA_DIR>/sealed-generated/release-host-server/scripts/db/clean-db.test.mjs
node <QA_DIR>/generated-smoke.mjs <QA_DIR>/sealed-generated
node scripts/guards/check-edition.mjs
git diff --check
```

- Node位于`/Users/mini32g/.nvm/versions/node/v20.19.0/bin/node`，没有依赖环境默认版本。
  原完整矩阵有本机socket桩，需要沙箱外运行；仅隔离临时目录/桩，不访问业务服务。
- `sealed-generate.log`退出0，最新fresh full含Admin/Report/H5/Docker、独立包名/坐标/宿主/模块前缀。
  `sealed-db.tap`31/31、失败/跳过0、19.18秒；未执行真实MySQL/Flyway。
- `sealed-cli.log`退出0，41份runtime mjs/README字节一致、无测试/fixtures，
  从其它cwd实际CLI check不写vault、publish/重试already_sealed/verify成功，固定清单摘要一致。
  明确标注synthetic UI产物；只核验工具，不假装编译/部署完整应用。
- `edition-working.log`及`edition-staged.log`通过，暂存diff-check通过。未改Java/SQL/Vue/API，
  复用P3.3a的编译/测试/UI成功记录，本轮不无差别重跑Maven/UI/浏览器。

### 失败修正与未完成边界

- 首轮release/project-tools74项中的13项因macOS只读顶层rename EACCES失败；
  沙箱外诊断同样复现，不是审批限制。发布锁内短暂解锁顶层后rename，finally收敛并复验；
  文件/子目录仍只读。修复后74/74通过，后续完整矩阵全部通过。
- 完整矩阵首轮446/447，原edition测试精确脚本列表未包含新增命令；扩展为精确三项，
  仍检查不交付guard，未删除/放宽安全断言。
- 自审补强有界读取/产物散列/复制的O_NONBLOCK再fstat，避免FIFO/路径替换等待写者；
  新真实子进程FIFO负例和跨64KiB分块复制通过；不改变原读取限额/构建协议。
- 最后补强真实UID/有效UID均非root，补真实UID非root但有效UID为root的拒绝断言；
  完整451及最终fresh full/41份原字节运行工具/实际CLI/DB桩31再次通过，不沿用修正前生成工程。
- 未连接真实HTTPS/当前任务审批、rootless镜像、MySQL锁/Flyway/远程仓库或目标服务。
  本地审查声明不等于服务端审批，sealed/verified不等于安装、认证发布、健康或商业许可有效。
  P3.3b2与P3整体仍未完成；目标环境及权限需单独确认。无服务清理事项，QA证据/生成工程保留。

## 2026-10-08：P3.3b1 开始

- 按“继续”承接部署准备，先实现不依赖未选定远程仓库/主机的本地封存工具。
  `--reviewed` 仅本地声明；当前任务审批桥接和真实部署仍属于后续阶段，不扩大权限。
- 使用 forge-project-init 技能；工具在改名后原字节交付，保留固定 Forge 协议。
- QA 独占目录 `/private/tmp/forge-plugin-release-p3.TXIDli`；分支 codex/plugin-foundation，
  起点20c5e2b5，用户 .DS_Store 保留。不写158或任何共享库、不启动/停止用户服务。

## 2026-10-08：P3.3a 构建验收与失联关闭交付

### 范围与两阶段增量自审

- Stage 1：按先补充的Spec，只做部署前人工审查/关闭，不执行制品发布或部署。
  approve_build仅built且绑定成功报告→release_ready，保留占用；close_task允许终态或过期building，
  明确停止执行器/未部署/说明后→closed并释放占用，活跃构建拒绝关闭。
- 独立system:plugin:review权限、平台管理员双重检查、显式DTO及正常加解密链路；
  不接受调用者身份/路径/命令/部署URL，不提供审计删除，操作日志不保存说明和响应。
- build→task锁顺序与finish一致，按唯一tenant/id锁，无ORDER/LIMIT；封存只设置结束时间和可信操作人，
  不生成worker结果。旧缓存续期/finish的SQL CAS拒绝穿过finished_time，审计/封存/任务CAS同事务。
- requestId绑定tenant/task/actor及规范化请求摘要，同内容幂等、变更拒绝；保留ZIP及已有报告。
  V1.0.213只新增审计表、两个平台资源及字典，不修改210–212或授予普通角色/覆盖客户资源。
- Stage 2：核对报告SHA/成功元数据/固定源码镜像包绑定、人工声明而非平台验证、租户/逻辑删除过滤，
  UI无HTML执行/本地存储说明/机器凭据；冻结请求用Pinia而不是多层props状态中转。
  延迟响应不清理新页面草稿、活跃请求不重复发送，错误集中详情避免背景列表重复显示。
  新生产类/SFC及方法符合规模要求；原CLI/worker调用协议不变，未新增运行时依赖。
- project-init Skill影响：沿用源码交付格式和原工具；对最终完整改名工程验证，
  工具改名后原样交付。实际停容器/核验产物是管理员义务，页面勾选不伪装为平台远程核验。

### 环境、最终回归与生成工程

- 独占QA_DIR：`/private/tmp/forge-plugin-review-p3.1assTa`；Node20.19.0/JDK17/Maven3.9.11，
  离线Maven，复用已有Byte Buddy agent，不安装依赖或访问共享158库。
- 模板`java-final.log`退出0，选定25份surefire报告：starter-plugin188/system68，共256/256，
  失败/错误/跳过0。新增6项实际核查事务/并发/XML测试和1项Controller DTO/权限契约测试。
  `admin-package.log`聚合包退出0；命令：

```sh
mvn -o -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests \
  '-Dtest=SourcePluginPackageReaderTest,Plugin*Test,RuntimePluginCatalogTest,CommunityFeatureGateTest,ForgeVersionTest,SysPlugin*Test,*FeatureGateTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.test.redirectTestOutputToFile=true
mvn -o -q -pl forge-admin-server -am package -DskipTests
```

- 沿用上一轮完整Node矩阵，`node-full.tap`397/397、失败/跳过0，20.40秒；新增review交付门禁。
  UI `ui-final.log`范围ESLint及Vitest25/25，`ui-final-build.log`退出0，28.23秒。
  构建仍有既有chunk/UnoCSS性能提示，不将其当作本轮新增故障。
- Vite43130明确标注模拟接口，系统Chrome独立无登录会话实际点击：审查四项声明、失败同请求重试、
  关闭两项声明、活跃无关闭入口、失联关闭/无报告封存、历史与明暗。`browser-final.log`pageerror=[]，
  document宽320，提交按钮x68/y513.08/w154/h34；查看review-narrow-dark/review-sealed-light。
- 最后生产修复后重新从完整源码生成`QA_DIR/release-generated`，full、com.acme.review包名、
  com.acme.maven坐标、review-host宿主/kernel模块前缀，`release-generate.log`退出0。
  UI实际目录是plugin-review-check-admin-ui，而不是按artifact前缀猜测的review-host-ui。
- `release-generated-db.tap`31/31、失败/跳过0，49.52秒；mysql/Maven为桩，无真实库写入。
  改名后system -am选择SourcePluginPackageReaderTest,PluginAutoConfigurationTest,SysPlugin*Test，
  13份报告76/76、失败/错误/跳过0；`release-generated-java.log`及Admin聚合package退出0。
- 15份执行mjs/README逐字节一致，213迁移及核查Store/组件原样交付；Java已改包，
  /internal/plugin-build、forge.plugin-build.worker和system:plugin:review保持稳定协议。
  edition、git diff --check通过；用户.DS_Store保持未提交，不push、不合并main。

### 失败修正与验收边界

- UI初次lint发现多语句/列表换行，整理后通过。Naive Checkbox不是原生input，
  单测改为真实组件的update:checked事件/checked断言，并由浏览器真实点击补证，未削弱校验。
- 新Node静态断言初次把权限资源的通配符路径误当Controller的{id}路径；按实际SQL修正后完整397通过。
  浏览器初次发现背景/详情重复错误提示，集中到详情；最终点击、截图及布局断言通过。
- 收尾修正封存build.update_by为实际审查人，新增不同确认人/核查人断言，
  重跑最终模板矩阵、Admin包并全新生成工程验证，不沿用修复前包。
- 仅停止本轮模拟Vite会话9403（退出130），lsof复核43130无监听；不停止用户服务，保留QA证据。
- 未连接真实MySQL/Redis、执行Flyway/正常登录/加密业务请求、rootless容器或制品发布/部署。
  H2/MockMvc/模拟页面不是目标环境验收。权限/状态流转迁移上线前须人工审查，
  P3.3b目标/制品仓库/健康核验仍需单独确认；不能宣称“插件已安装”或P3整体完成。

## 2026-10-08：P3.3a 开始

- 用户继续；基线9a4cadfe，codex/plugin-foundation，仅用户.DS_Store未提交。
- 先补部署前人工验收/失联关闭契约和测试计划；复用 project-init 原工具及生成工程规则。
- 本轮不执行真实部署、共享数据库迁移、服务启动或商业/Pro实现；目标环境需另确认。

## 2026-10-08：P3.2 认证桥接交付

### 范围与两阶段增量自审

- Stage 1：按本轮 Spec 接通选定任务，不自动消费队列、不部署、不创建 Pro 工程。
  沿用 project-init Skill 的原源码 CLI、稳定插件协议及改名后原样复制运行工具规则。
- 独立机器过滤器始终注册、默认关闭；必须可信 HTTPS、固定部署身份/租户、未过期凭证。
  SaIgnore 只跳过用户会话；普通 Token、X-Inner-Call、客户端租户均不能代替机器认证。
  原始机器凭证只读环境，私有恢复收据只保存随机租约，不进入容器或公开 API。
- queued→building→built/build_failed；任务一对一构建审计，90秒/30秒/25分钟租约边界。
  并发领取、阶段及终态 CAS、结果绑定和同结果幂等；失联/终态保留占用与审计，不自动重试。
- Web 仅有界 BLOB/DTO/事务；没有文件系统/子进程或部署执行。worker 调用原隔离构建器，
  实际核验产物后仅回写摘要，服务端不把机器报告冒充独立制品复验。
- Stage 2：核对租户上下文恢复、私有字段不出 VO、拒绝日志脱敏、HTTP 不重定向/3秒期限，
  原确认/取消 CAS 和幂等不受续期影响。补 finish 的阶段 CAS，避免并发旧结果覆盖新阶段。
  新类/SFC远低于规模上限；新增生产方法≤80行、单行≤120，职责按认证/状态机/视图/传输拆分。
- V1.0.212 新增表与系统字典，未修改210/211、未授予普通角色、无物理删除接口。
  clean-db 纳入审计表；本轮没有新增运行时依赖、凭证/真实个人数据或构建镜像。

### 环境及最终回归

- 独占 QA_DIR：`/private/tmp/forge-plugin-worker-p3.0PkCQH`；Node20.19.0、JDK17、Maven3.9.11。
  Maven离线 `-o`，复用已有依赖；测试通过 JAVA_TOOL_OPTIONS 加载既有 Byte Buddy agent。
- 在 forge-server 执行以下相关矩阵，`java-final.log` 退出0：starter-plugin188、system61，
  共249/249，失败/错误/跳过均0（不统计未选择的旧 surefire 报告）。

```sh
mvn -o -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests \
  '-Dtest=SourcePluginPackageReaderTest,Plugin*Test,RuntimePluginCatalogTest,CommunityFeatureGateTest,ForgeVersionTest,SysPlugin*Test,*FeatureGateTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.test.redirectTestOutputToFile=true
mvn -o -q -pl forge-admin-server -am package -DskipTests
```

- `admin-package.log` 退出0。新增13项Java测试覆盖实际Mapper/XML、Spring事务回滚、
  两线程并发领取、私有包摘要/租户、阶段/终态、始终注册的过滤器及MockMvc参数校验。
  H2实际210/211/212连续执行两次，7字典类型/22字典数据、15资源不额外扩权。
- 仓库根目录最终 Node 矩阵，`node-final.tap` 396/396、失败/跳过0，34.24秒：

```sh
node --test scripts/forge-shared scripts/forge-plugin scripts/forge-plugin-builder \
  scripts/guards scripts/forge-create \
  code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  code-copilot/changes/plugin-center/contracts.test.mjs \
  code-copilot/changes/plugin-center/workbench-contract.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
```

- 新增10项覆盖固定TLS请求、真实3秒超时/中止、响应边界、选定任务、串行续期、摘要不匹配、
  失租不能成功与结果不确定；其中真实Git/ZIP/产物文件核验、Docker/socket调用为受控桩。
  ESLint `--no-config-lookup --rule 'no-unused-vars:error' scripts/forge-plugin-builder` 退出0。
- UI最终 ESLint范围 `src/api/system/plugin.js src/views/system/plugin.vue src/views/system/plugin`，
  Vitest该域17/17；`ui-final-verified.log`通过，`ui-final-build.log`生产构建退出0，38.71秒。
  沿用已安装Vite/Rolldown，既有chunk/UnoCSS性能提示非阻断；未重新安装依赖。
- 明确标注模拟接口的Vite43129 + 系统Chrome隔离会话，实际组件点击上传/确认/刷新/详情，
  核对building、租约到期及成功摘要、无取消/部署假按钮、明暗主题和320px。
  `browser-compact.log`无pageerror，窄屏document宽320，关闭按钮x240/y670/w56/h34。
  人工查看execution-light、execution-narrow-dark；摘要标签改为左侧，避免大屏信息过度纵向占用。

### 最新 full 生成工程

- 从最终生产代码全新生成QA_DIR/final-generated，不是局部拷贝；full、com.acme.worker包名、
  com.acme.maven坐标、worker-host宿主前缀、kernel框架前缀，`final-generate.log`退出0。
- `final-generated-db.tap`31/31，失败/跳过0，25.06秒；mysql/Maven调用为桩，没有创建真实数据库。
- 改名后端 `kernel-plugin-system -am -Penable-tests` 选择
  SourcePluginPackageReaderTest,PluginAutoConfigurationTest,SysPlugin*Test，69/69，
  `final-generated-java.log`退出0；`kernel-admin-server -am package -DskipTests`，
  `final-generated-package.log`退出0。使用本机已有改名BOM缓存，不发布远程制品。
- 15份运行mjs/README逐字节一致；真实生成Java包名改变但/internal/plugin-build和
  forge.plugin-build.worker保持原协议，212迁移字节一致。生成工具仍排除测试/夹具。
- edition门禁、git diff --check通过；本轮未提交用户 .DS_Store。

### 遇到的环境/修复与验收边界

- 初次聚焦Node测试因沙箱Unix socket EPERM失败，授权仅临时socket/Git夹具后重跑完整矩阵通过。
  UI测试初次3项列表换行lint失败，格式化后重跑lint/test/build通过；没有弱化断言。
  收尾门禁第一次误在UI目录找根scripts而未运行，改回仓库根目录后edition/diff通过。
  暂存后新增文件的diff检查发现7个DTO/VO尾部空行，移除后重新检查；只改空白，不改已验证逻辑。
- 浏览器首次截图在抽屉动画中，增加完成等待再检查；独立会话每次finally关闭，不读用户登录。
- 未连接共享158 MySQL/Redis；未执行Flyway、真实Admin/Flow登录、加密、业务写入或部署。
  H2/MockMvc/TLS请求桩/模拟UI不等价于目标HTTPS/重放/MySQL或rootless容器验收。
  本机仍无Docker；真实离线镜像/cache、资源控制及失联容器清理仍须目标环境验收。
- 制品仅在私有worker工作区；制品仓库、人工部署/健康核验与受控恢复留在P3.3，
  不直接改表释放占用，不宣称插件已安装。只验收Admin构建范围，不冒充独立Flow/Report部署。
- 仅停止本轮模拟Vite会话11758（退出130）；lsof复核43129无监听，QA日志/截图/工程保留复查。
  不停止用户服务、不清理其他任务数据；按已有阶段惯例本地中文提交，不push、不合并main。

## 2026-10-08：P3.2 开始

- 用户继续；基线 f9b47529，保持 codex/plugin-foundation 和未提交 .DS_Store。
- 复用 P3.1、project-init Skill/plugins 参考、DESIGN、测试规范和既有验证证据。
- 先补机器认证/租约/有界包下载/结构化回写契约；不自动执行队列或部署。
- 本地 Docker 缺失仍按实情记录；不改共享 MySQL/Redis，不创建 Pro 工程。

## 2026-10-07：P3.1 开始

- 用户继续开发，基线 5c8a378b，分支 codex/plugin-foundation，保留未提交 .DS_Store。
- 复用 project-init Skill、插件参考、现有 CLI/快照/生成工具及测试基线；追加离线执行器契约。
- 本轮不改任务 Web 状态/权限/数据库，不连接共享服务；构建不能冒充部署。
- 本机 command -v docker 无输出；实现执行器逻辑并验证拒绝/桩边界，真实 rootless 构建待验收。

## 2026-10-07：P3.1 离线执行器交付

### 范围与两阶段自审

- 原 CLI 新增只读 check，与 add 共用完整宿主/POM/目录/所有权校验，未改原事务与恢复逻辑。
- 新 `scripts/forge-plugin-builder/` 按配置、快照、子进程、容器、产物和编排拆分；
  check 固定 Git HEAD/每文件 blob/ZIP SHA，run 必须 --reviewed，整包替换须 --force。
  已提交定制及原安装目录忽略的本地配置仍拒绝，不在原工程写入或执行上传代码。
- rootless/私有本地 socket 父目录/不可变本地镜像检查；验证 cgroup v2/systemd 及
  memory/swap/CPU quota/PID 能力，防止资源参数被静默忽略。控制器字段核对 Docker/Moby 主文档，
  运维说明链接 Docker 官方 rootless 资源限制要求，不自动更改 daemon 或系统配置。
- 固定无网络/只读根/cap-drop/no-new-privileges/资源限额；只读 source/package/control、唯一输出。
  不继承生产环境/认证，Docker 配置目录本次独占；有界输出/超时/SIGINT/SIGTERM，
  核对随机名称+标签才清理本次容器，清理失败报错，私有 container.json 保留身份回执。
- 固定离线 Maven/BOM/pnpm/Vite，不启动服务/Flyway/部署，不以宿主执行作为容器失败降级。
  从实际产物重新计算大小/SHA，拒绝缺失/超量/零文件/越界/链接/硬链接/普通文本假 JAR。
  result.json 明确 checked/built/failed、阶段/错误码及 deployed=false，不把构建当安装/健康证明。
- Stage 1：本轮仅 P3.1，未改 Java、SQL、Web 任务/API/UI、权限或租户；
  P3.2 认证领取/租约/回写、P3.3 部署/运行核验仍未完成，P2 队列不会自动执行。
- Stage 2：新增模块/SFC 规模与函数 ≤80/行 ≤120/参数及嵌套自检，产物哈希流式读取，
  原安装器保护/源码固定视图/命令无 shell/无 Secret 继承与失败清理复核。
  清理仅涉及本次容器，job 保留；不删客户文件/数据，不自动增加限额或打开网络。
- project-init Skill 影响：复用原 CLI/交付格式，改名完成后原样复制工具/README，
  修正把 Vite build 源码目录忽略的旧规则，保持 dist/target 等实际产物保护。

### 增量验证命令与证据

独占 QA_DIR：`/private/tmp/forge-plugin-builder-p3.B7t3Rs`。
node 指 `/Users/mini32g/.nvm/versions/node/v20.19.0/bin/node`，沿用现有 JDK17/Maven3.9.11，
未安装 Docker/依赖。根目录运行：

```sh
node --test scripts/forge-shared scripts/forge-plugin scripts/forge-plugin-builder \
  scripts/guards scripts/forge-create \
  code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  code-copilot/changes/plugin-center/contracts.test.mjs \
  code-copilot/changes/plugin-center/workbench-contract.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
node forge-admin-ui/node_modules/eslint/bin/eslint.js --no-config-lookup \
  --rule 'no-unused-vars:error' scripts/forge-plugin-builder scripts/forge-plugin/preflight.test.mjs
node scripts/guards/check-edition.mjs
git diff --check
```

- `node-final-acceptance.tap`：386/386、失败/跳过0，28.60秒。含模板 DB 桩及原基础测试；
  相对 P2 的353基线新增33项，涵盖源码/包/定制、商业/降级、进程与容器拒绝/清理和产物边界。
- `eslint-final-acceptance.log`：退出0，执行脚本级 unused/syntax 检查，非全 UI lint；
  新增 .mjs 的120字符检查无输出，edition 与 diff --check 通过。
- 模板本轮无 Java/UI/API 改动，复用 P2 236项 Java/14项 UI/聚合构建证据，不重复模拟浏览器验收。
- 按现有 create-project 从当前模板新生成 full/final/release 三套完整工程，不是局部目录拷贝。
  参数：plugin-builder-check/PluginBuilderCheck/com.acme.builder/com.acme.maven/
  builder-host/kernel/plugin_builder_check；release 为验收源。
- `release-db.tap`：新 full 的 init/clean DB 桩31/31，38.87秒；未创建数据库或执行真实 Flyway。
- `generated-check.mjs` 使用生成后的实际运行工具，Git初始提交、原始 hello ZIP、固定摘要：
  源码6568文件、61628074字节、排除1913文件，admin-build 范围；
  目标为 builder-host-server/plugins/kernel-plugin-hello 和
  plugin-builder-check-admin-ui/src/views/plugins/hello，版本hello1.0.0/core1.2.0。
  安装仅发生在独占副本，原生成仓库 Git 干净，真实 build/plugin-ui-manifest.js 被跟踪。
  工具以 copyGeneratedPluginTools 机械刷新收尾的 unused import/运维说明/容器检查后，再固定 QA Git提交，
  `generated-final-acceptance.log` 退出0，字节与模板原工具一致，不带 tests/fixtures。
- `generated-cli.log`：生成工程实际 CLI 入口执行 check 成功，非仅导入函数。
- 对 QA 已审查公开 hello 样例的副本 job-YJlJ9Q/source 手工验证安装器构建契约：
  在 builder-host-server 先 `mvn -o -q -f kernel-framework/kernel-dependencies/pom.xml install -DskipTests`，
  再 `mvn -o -q -pl kernel-admin-server -am package -DskipTests`；使用沿用 JDK17，
  staged-bom.log/staged-admin-package.log 均退出0。仅测试坐标 BOM 写本机缓存，未发布远程制品。
  这是已审查样例的生成/POM编译回归，**不是执行器宿主降级或真实容器验收**。
- `artifact-check.log`：实际 Admin JAR 224361668字节，ZIP文件头和流式 SHA 校验通过；
  SHA `03b019e20a30e107180a57d8df4387daa35bb7f461637826243ce96132a7c703`。
  不是数字签名/运行健康/数据库迁移证明；生成 UI全量构建仍未执行。

### 发现、修复及验证边界

- 初次 Unix socket 测试被沙箱 EPERM 阻断，经授权仅创建私有临时测试 socket 后重跑通过。
  Docker始终为明确桩，不运行真实 daemon；socket服务在测试结束关闭，临时测试目录自动清理。
- macOS 默认大小写不敏感，初次碰撞夹具实际覆盖同一文件；改为直接验证真正不同路径的
  Set/NFC/前缀碰撞协议，不把未制造成功的磁盘夹具声称为安全通过。
- 全量首次381/382：旧生成工程契约仅允许一个命令；按新增实际接口精确断言两个命令并重跑，
  不删除测试、不降低门槛。脚本 lint 发现两个未使用 path import，删除后最终检查通过。
- full 真快照首次触发 SOURCE_SIZE_LIMIT：定位 Report UI约509MiB字体库；
  不扩大512MiB阈值，固定只选 Admin构建输入，排除其它前端/部署/文档/旧发布ZIP，
  实际新 full 验证通过；定制范围外的工作区仍不自动支持。故障记入 backend 踩坑索引。
- 没有本地 Docker：真实 rootless daemon、审查镜像、离线缓存/构建、资源与网络隔离尚未验收；
  运维目录/镜像/权限需人工准备，不把 Docker桩、手工 Maven或文件哈希当成该验收。
- 未连接158 MySQL/Redis、启动真实Admin/Flow、执行Flyway/业务写入/生产部署；
  P1/P2 真实登录/RBAC/加密/multipart验收、P3.2/3.3和独立Pro工程均仍待后续阶段。
- 未启动业务/浏览器预览服务；所有本轮测试/构建进程完成，无 Docker 容器或真实服务遗留。
  QA日志/生成工程/私有job与已校验产物保留供复查；本轮未删除材料，用户 .DS_Store 保留。
- 收尾按项目规则在 codex/plugin-foundation 本地中文提交，不 push、不合并 main。

## 2026-10-07：P2 开始

- 用户请求继续下一阶段，基线 203068d7，保持 codex/plugin-foundation 和用户 .DS_Store。
- 重读 project-init Skill/plugins 参考、DESIGN、测试规范及已有 P1 证据，追加 P2 执行契约。
- P2 仅受控上传/预检/确认/审计及 UI 构建声明；确认排队不执行安装，不读生产源码路径。
- ZIP 入私有有界 BLOB，平台/RBAC/租户上下文隔离；新增迁移待人工上线，不连接共享数据库。

## 2026-10-07：P2 交付与增量自审

### 范围与实现

- 有界 ZIP central/local、路径、链接、大小、压缩流、CRC、NFC 重名校验；严格 JSON、
  根/runtime 描述一致、POM 单模块/外部实体/64 层限制，私有包不解压落盘、不执行。
- 任务审计/私有 BLOB、显式 DTO/VO、Mapper XML 租户过滤、上传 requestId 幂等，
  确认/取消 SHA-256+revision+CAS，运行快照变化拒绝确认；独立同插件活动任务占用键。
- 平台超级管理员和独立 RBAC；上传/确认/取消 OperationLog 不保存请求/响应内容。
  只到待构建，无 shell/Node/部署接口；前端不显示假安装成功，不实现 Pro/许可证。
- V1.0.211 新表/10 权限资源/2 字典类型/6 字典项；不改 V1.0.210、不赋普通角色权限。
  不覆盖客户菜单/权限/接口，当前唯一键要求 snapshot 独立权限编码。
- 上传工作台、确认风险/取消/查询及有界预览；真实 UI 构建清单核验所有权/开发链接，
  与完整后端快照比较。源码登记、定制、路径/依赖及迁移实际影响明确留到 P3。
- Stage 1 Spec 合规：P2 覆盖，P3 仍未接入；Stage 2 质量：新类/SFC <1000、方法 ≤80、
  新增行 ≤120，字典/枚举/构造器注入/事务/幂等/权限/租户/逻辑删除与日志隐私自检。
  vite.config.js 原有 137 字符环境变量解构行未增长，不顺手修改历史代码。
- forge-project-init Skill 影响：复用源码 CLI/格式，保持协议坐标和稳定 ID，
  模板清理任务包并对新生成 full 工程执行 DB 桩、原始坐标单测和聚合构建。

### 验证环境与命令

- 独占目录 `/private/tmp/forge-plugin-center-p2.03Fo6H`，以下简称 QA_DIR。
  沿用 Node20/JDK17/Maven3.9.11 离线工具和已有依赖；H2 仅 test-scope（BOM 已管理）。
- 模板 forge-server：`mvn -o -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test
  -Penable-tests '-Dtest=SourcePluginPackageReaderTest,Plugin*Test,RuntimePluginCatalogTest,
  CommunityFeatureGateTest,ForgeVersionTest,SysPlugin*Test,*FeatureGateTest'
  -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.test.redirectTestOutputToFile=true`。
  实际 -Dtest 参数为无换行/空格的上述列表，预加载同 P1 的 Byte Buddy agent。
  `java-final.log` 退出 0，starter-plugin 188/system 48，共 236，失败/错误/跳过均 0。
- 模板 `mvn -o -q -pl forge-admin-server -am package -DskipTests`：
  `admin-final-package.log` 退出 0；不是全仓所有业务测试均已通过。
- UI 现有依赖直接运行 ESLint、`vitest run src/views/system/plugin/__tests__`、`vite build`：
  `eslint-final.log` 退出 0、14/14 通过、`ui-final-build.log` 退出 0（38.60 秒），
  现有大包/构建分析提示非阻断，不安装/替换 node_modules。
- Node 完整矩阵沿用 P1 命令并追加 workbench-contract.test.mjs：
  `node-final.tap` 353/353，无失败/跳过（34.35 秒）；文档/中心契约独立 13/13。
- `SysPluginTaskMapperTest` 执行真实 MyBatis/XML/表结构，验证私有包内部读取、
  公共查询不取 ZIP、租户隔离、活动唯一键、CAS、取消释放和逻辑删除。
  `SysPluginMigrationTest` 在 H2 MySQL 模式执行实际 210/211、211 两次，
  15 总资源/14 平台限制资源、6 字典类型/14 条目，客户菜单/权限冲突不覆盖。
  QA_DIR/migration-fixture.sql 的独立 RunScript 也退出 0，不等价于 MySQL/Flyway。

### 生成工程与浏览器

- 新 full 工程生成四次（full/delivery/final/release-generated），最后以 release-generated 验收；
  每次从当前模板生成，不是只拷贝变更目录。参数沿用 P1：plugin-center-check、
  PluginCenterCheck、com.acme.center、com.acme.maven、center-host/kernel、plugin_center_check。
- release-db.tap：新 full 工程 init/clean DB 桩 31/31（48.25 秒），未创建真实数据库。
- release-java.log：`-pl kernel-framework/kernel-plugin-parent/kernel-plugin-system -am test`
  `-Penable-tests -Dtest=SourcePluginPackageReaderTest,PluginAutoConfigurationTest,SysPlugin*Test`，
  56/56，退出 0；源包测试仍使用原始 Forge groupId，不让夹具随宿主改名自洽。
- 在独占生成工程用现有 CLI 安装标准 plugins-samples/forge-plugin-hello，
  release-install.log 退出 0；实际生成 UI helper 的 release-manifest.log 输出 hello 1.0.0、
  core 1.2.0，无源码路径；随后 release-package.log Admin 聚合 package 退出 0。
  未做生成 UI 全量构建，模板 UI 已完整构建；不是执行器接入或目标部署。
- 明确标注模拟接口的 Vite 127.0.0.1:43128，隔离系统 Chrome，无用户会话读取。
  browser-final.log：上传/风险勾选前禁止确认/确认待构建/取消/阻断无确认按钮/
  查询/失败重试/构建版本不同/320px 深色；无 pageerror，页面 scrollWidth=320，
  抽屉 x=0,width=320,height=720，关闭按钮 x=240,y=670,width=56,height=34 完整可见。
  人工检查 preview-light、preview-narrow-dark、workbench-narrow-dark 截图；
  使用模拟响应，不替代真实用户登录、加密/multipart 或数据库验收。

### 发现与修复

- 真 Mapper 测试发现 byte[] 返回被 MyBatis 当作多行数组，改为私有实体包装列，
  不降低公开 API 隐私；隔离 Factory 的删除测试显式传入审计字段，避免未注册填充器产生 NULL。
- strict POM 原始 groupId 被生成器改名，改分段协议常量，生成后的测试仍用原始包。
- sys_resource 真实唯一键不允许 page/snapshot 复用编码，snapshot 改独立权限；
  冲突保护同时校验 PC 权限及 API，新增真实唯一键迁移回归。
- 初次测试夹具 Mockito 未完成 stubbing、唯一键错误文案大小写、UI mock 缺字典导出，
  分别修正夹具；测试断言/门禁未降级，浏览器窄屏等待动画稳定再检查位置。
- 补日志时误用不存在的 OperationType.INSERT，编译发现后查枚举改为 ADD，完整回归重跑通过。
- 初次生成工程对 Admin 全链开测试，触发存量 AiProviderAdapterRegistryTest 缺
  createEmbeddingModel 实现的编译问题（generated-java.log）。本轮未改无关 AI 测试；
  改用本轮 system/starter 的定向 Reactor 测试通过，Admin 生产聚合 package 通过。
- 构建 helper 位于既有被忽略但包含源码的 build 目录，提交显式纳入此新文件，避免遗漏。
- 两个真实故障已沉淀 backend 踩坑及索引；未改用户 .DS_Store。

### 待执行和清理

- 未连接或修改 158 MySQL/Redis、启动真实 Admin/Flow、执行 Flyway/生产部署；
  V1.0.211 和真实登录/RBAC/加密/multipart 验收仍待目标环境执行。
- P3 builder/源工作区冲突和定制检查/构建/受控部署/运行核验未开发；Pro 工程未创建。
- 隔离 Chrome 每次 finally 关闭；只停止本轮 43128 模拟 Vite，日志/截图/生成工程保留。
- Vite 会话 91764 Ctrl-C 结束（退出 130），lsof 复核 43128 无监听；未停止用户已有前端。
- 收尾文档/中心契约重跑 13/13、edition 门禁及 diff --check 通过，索引与工作区仅本轮变更。
- 本地中文提交 codex/plugin-foundation，不 push、不合并 main，.DS_Store 不提交。

## 2026-10-07：P1 开始

- 用户批准可视化插件架构后开始；基线 ae6dc7ab，分支 codex/plugin-foundation。
- 保留用户 .DS_Store；不切换 main、不 push、不创建 Pro 工程。
- 读取 AGENTS/Skill/plugins 参考、DESIGN、测试规范和 foundation 已有证据。
- 发现 Registry 仅有外部描述，内置插件无登记；P1 补 classpath 声明，不读取部署机源码。
- 本轮先交付当前后端实例清单；纯 UI 构建清单和安装任务分 P2/P3，明确不伪造状态。

## 2026-10-07：P1 实现与增量验证

### 交付与自审

- 内置业务 JAR 15 份稳定 ID 声明；Maven 只过滤该描述，不过滤其它业务 JSON/SQL。
- RuntimePluginCatalog 合并内置与 Registry，严格解析、大小上限、重复 ID 拒绝、不可变有序快照。
- system 只读分页/详情 DTO/VO API：RBAC 注解与真实 SessionHelper.assertAdmin 双重限制。
- 平台管理独立入口、4 类系统字典、列表/详情/竞态保护、重试及主题/窄屏适配。
- Stage 1（Spec）：P1 覆盖；独立实例/纯 UI/源码与部署边界均说明；P2/P3 未冒充已交付。
- Stage 2（质量）：无写接口/外部进程/许可证泄露，无服务循环注入；新增类/SFC/方法规模达标。
  新增 Java/JS/Vue 单行不超过 120；菜单不覆盖应用总览/打印中心，不授权普通角色。
- 发现现有 v-hasPermi 更新钩子 this 未绑定造成页面异常；新页面用 userStore 计算权限，
  不扩大范围修改全局指令，后端鉴权保持权威。故障沉淀到 frontend 踩坑。
- 生成项目遵循 forge-project-init Skill：改包/坐标后重新生成 full，保持插件 ID 不变。

### 工具与证据位置

- Node v20.19.0；JDK17 `/private/tmp/lawhub-october-jdk/Contents/Home`；
  Maven `/private/tmp/apache-maven-3.9.11/bin/mvn`，离线 `-o`，不新增第三方依赖。
- 本轮独占夹具/日志目录：`/private/tmp/forge-plugin-center.HXbPfO`，以下简称 QA_DIR。
- Maven 测试预加载本机 Byte Buddy agent（JAVA_TOOL_OPTIONS=-javaagent:.../1.17.8/...jar），
  避免 JDK 沙箱自附加失败；这是测试环境设置，不写入项目运行配置。

### 后端与真实构建声明

在 forge-server 执行：

```sh
mvn -o -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test \
  -Penable-tests \
  '-Dtest=RuntimePluginCatalogTest,Plugin*Test,CommunityFeatureGateTest,ForgeVersionTest,SysPlugin*Test,*FeatureGateTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.test.redirectTestOutputToFile=true
mvn -o -q -pl forge-admin-server -am package -DskipTests
mvn -o -q -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am process-resources -DskipTests
```

- `java-regression-final.log`：退出 0。starter-plugin 169、system 本轮选择的 38，共 207，通过无跳过。
  system 既有其它 surefire 文件不计入本轮选择范围；新增测试共 24。
- `admin-package.log`：退出 0，Admin 聚合构建通过；`flow-resources.log`：资源处理退出 0。
- QA_DIR/JarCheck.java 用 Java ZipFile 读取实际宿主及改名宿主的嵌套业务插件 JAR。
  `jar-check.log`：各 14 个、版本 1.2.0、模块坐标正确、无未过滤占位符/重复 ID；
  plugin-flow 不在 Admin 依赖中，没有将源码的 15 份声明假装成此实例已加载 15 个。

### 前端与浏览器

在 forge-admin-ui 使用已有依赖执行（node 指 Node20）：

```sh
node node_modules/eslint/bin/eslint.js src/api/system/plugin.js \
  src/views/system/plugin.vue src/views/system/plugin
node node_modules/vitest/vitest.mjs run src/views/system/plugin/__tests__
node node_modules/vite/bin/vite.js build
```

- ESLint 无错误；新增 Vitest 8/8：API 编码、筛选/分页、列表/详情竞态、关闭/销毁及失败重试。
- `ui-final-build.log`：退出 0，40.03 秒；现有大包/分析器提示为非阻断警告。
- 自建明确标注模拟接口的预览，127.0.0.1:43127；使用系统 Chrome 的隔离 Playwright 会话。
  `browser-check.mjs` 覆盖列表/搜索/详情/功能授权/重置/分页/刷新错误/重试/明暗/320px，
  `browser-final.log`：无 pageerror；320px 抽屉 x=0,width=320,height=720，可见关闭按钮。
- 人工检查 list-light、detail-narrow 截图；窄屏列表二次独立加载/截图复核，DOM 仅 1 个
  main、1 个页面头、1 个插件中心、15 数据行，无重复页面，截图 list-narrow-inspected.png。
- 浏览器使用模拟响应，不声称已覆盖真实租户、正常登录、加密/菜单缓存或部署数据。

### Node 基线、模板/生成工程、迁移

仓库根目录执行：

```sh
node --test scripts/forge-shared scripts/forge-plugin scripts/guards scripts/forge-create \
  code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  code-copilot/changes/plugin-center/contracts.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
node scripts/forge-create/create-project.mjs <QA_DIR>/delivery-generated \
  --preset full --project-name plugin-center-check --java-name PluginCenterCheck \
  --base-package com.acme.center --group-id com.acme.maven \
  --artifact-prefix center-host --module-artifact-prefix kernel \
  --display-name 插件中心验证 --database-name plugin_center_check
node --test <生成工程>/center-host-server/scripts/db/init-db.test.mjs \
  <生成工程>/center-host-server/scripts/db/clean-db.test.mjs
```

- `node-full.tap`：348/348，失败/跳过均 0，27.25 秒，包含模板 DB 桩和文档/插件中心契约。
- 先后生成独占 generated/final-generated/delivery-generated；不是原工程的局部目录拷贝。
  最后一次 `delivery-db.tap`：30/30，20.28 秒。mysql/Maven 为桩，未创建实际数据库。
- final-generated/center-host-server 中先安装改名 BOM：
  `mvn -o -q -f kernel-framework/kernel-dependencies/pom.xml install -DskipTests`，
  再 `mvn -o -q -pl kernel-admin-server -am package -DskipTests`，退出 0。
  BOM 写本机 Maven 缓存经批准，不更新任何远程制品仓库。
- 改名工程 `-Penable-tests` 执行 RuntimePluginCatalogTest,PluginAutoConfigurationTest,SysPlugin*Test，
  33/33，退出 0；生成的生产 Java 与当前源码经包名替换对比 6/6 相同。
  独立 Flow 的 process-resources 退出 0、版本/坐标正确；未做独立 Flow 全量 package。
- Java H2 RunScript 在内存库 `MODE=MySQL;NON_KEYWORDS=VALUE` 执行 migration-fixture.sql：
  实际 V1.0.210 连续运行两次，5 资源/4 字典类型/8 数据条目，无重复；客户菜单冲突不覆盖。
  `migration-h2.log` 退出 0。H2 合成表验证不等价于真实 MySQL/Flyway。
- `node scripts/guards/check-edition.mjs` 和 `git diff --check` 均通过；提交前再次检查工作区与索引。

### 遇到的环境问题及解决

- pnpm v11 exec 试图在无 TTY 环境重新安装，未继续安装；改用现有 node_modules 工具。
- POM 的显式 argLine 覆盖 CLI -DargLine，初次 Mockito 自附加失败；改 JAVA_TOOL_OPTIONS 后重跑通过。
- 初次生成工程聚合路径误用外部 artifact-prefix；核对实际 kernel-admin-server 后成功。
- 生成 BOM 的本机缓存写入、临时 Vite 端口绑定受沙箱限制，均通过授权后完成。
- Playwright 下载缓存不存在，复用本机 Chrome；不下载浏览器，不读用户标签页。
- UI 合成字典缓存与实际不同；页面统一显式 useDict→DictTag options，不硬编码状态文案。
- 收尾误指定不存在的 docs-contract.test.mjs，命令未运行测试；核对文件后重跑实际
  scripts/forge-plugin/documentation.test.mjs（6/6）与本轮 contracts.test.mjs（3/3）通过。

### 待执行及清理边界

- 未连接/修改 158 MySQL/Redis；未启动真实 Admin/Flow；未执行 Flyway、业务写入或生产部署。
- 真实目标环境正常登录/RBAC/加密链路及菜单加载待用户部署验收，禁止以 MockMvc 替代。
- 未做生成工程 UI 全量构建；本模板 UI 已构建，生成前后本轮前端源码无品牌坐标替换差异。
- P2 ZIP 上传/预检/任务记录/UI manifest、P3 builder/部署/运行核验尚未开发；Pro 工程未创建。
- 只停止本轮端口 43127 的模拟 Vite，隔离浏览器每次 finally 关闭；不停止用户已有服务。
  QA_DIR 日志/截图/生成工程保留供复查，无真实 DB 需删除，无后台真实服务遗留。
- 临时 Vite 会话 92021 Ctrl-C 结束（退出 130）；lsof 复核 43127 无监听。
- 本地中文提交在 codex/plugin-foundation；不 push、不合并 main，用户 .DS_Store 保留未提交。
