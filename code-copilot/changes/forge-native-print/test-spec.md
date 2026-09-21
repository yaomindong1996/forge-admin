# 测试与验收基线

> 状态：M1–M5 与 T46–T53 自动化及 Chromium 合成验证完成；真实应用/低代码/流程 E2E、其它浏览器和实机打印仍待用户验收。
>
> 依据：[spec.md](spec.md)、[tasks.md](tasks.md)、`code-copilot/rules/automated-testing-standard.md`

## 1. 原则与分工

- 后续每轮先读本文件与 execution-log，只增加本轮差异，不重建计划。
- 单测针对分页边界、业务规则、权限与并发；不写仅断言文件名或实现字符串的低价值测试。
- 纯函数可使用固定测量结果验证分页；真实字体/图片/页面高度必须在浏览器验证。
- 数据全部合成，不使用真实姓名、业务单据、签名或附件。
- 本轮不启动真实 Admin/Flow、不运行数据库迁移；后续实际联调继续按用户既有分工执行并回填。

## 2. P0 必过矩阵

| 用例 | 关联需求 | 预期 |
|---|---|---|
| 协议往返及未知版本/元素 | F01 | 不丢配置；不支持的版本/元素拒绝并定位 |
| 字段 0/false/null/超长 ID | F04/F08 | 0 和 false 正常输出；ID 无精度损失 |
| 金额分→元/负数/大额 | F08 | 定点格式一致，无浮点误差 |
| 毫米坐标与缩放 | F03/F05 | 50%/100%/150% 拖拽后存储尺寸一致 |
| 0/1/多页/500 行明细 | F06 | 行无漏失/重复，页数稳定，表头重复，合计仅末尾 |
| 恰好页满/剩余不足一行/合计另页 | F06 | 不多空页、不压页脚、不重叠 |
| 超高行/超高固定区块/超限页数 | F06/F12 | 明确阻止输出并定位；无死循环/截断 |
| 中英文长文本/中文标点/字体替换 | F06/F12 | 真实浏览器测量与预览一致；缺字字体失败可见 |
| 图片延迟/403/解码失败/超时 | F12/F16 | 不输出缺签名的“成功单据”，可重试且资源清理 |
| 并发编辑/重复发布/停用 | F02/F13 | CAS 拒绝覆盖；旧版本不可变；停用旧版本不可绕过 |
| 应用发布/回滚/模板删除 | F13 | 引用固定版本；回滚一致；被引用模板不能直接删除 |
| 字段删除/改型/表单重命名 | F07 | 发布检查定位失效绑定；稳定身份不随展示名称漂移 |
| 列表行/详情的统一打印动作 | F09 | 同一记录得到一致授权上下文；不信任前端 row 正文；未保存记录拒绝 |
| 跨租户/无记录权/未发布模板 | F15 | prepare 拒绝；不返回正文与文件引用 |
| 隐藏主字段/子表列/脱敏字段 | F08/F15 | 响应中不含不可见值；模板绑定无法绕过 |
| 伪造 task/instance/run/record 组合 | F10/F15 | 服务端验证关联并拒绝不一致请求 |
| 待办/已办/我发起身份 | F10 | 分别验证授权；不能只凭 readonly 读取 |
| 会签/退回重提/同节点多次办理 | F11 | 不同 taskId 保留，单据不混不同实例轨迹 |
| 实时数据与归档语义 | F10/F12 | 明确 CURRENT/生成时间；不声称历史原样 |
| 模板脚本/非法 HTML/原型链绑定/任意 URL | F01/F15 | 服务端和渲染层白名单拒绝，不执行动态代码 |
| 模板切换/预览关闭/路由卸载 | F16 | 无残留事件、iframe、临时 DOM/Blob URL，无任务串数据 |
| 下载代码打印运行 | F14 | 模板/绑定/依赖完整，与在线使用同一协议解释器 |
| 打印审计事件 | F15 | 只记身份/结果，不记正文；DIALOG_OPENED 不表示出纸 |

## 3. P1 与人工验收

- 键盘操作、框选/多选、复制粘贴、撤销/重做；保存失败保留草稿。
- 明暗主题、桌面窄窗口、中文文案；纸张保持白底，不出现横向溢出遮挡按钮。
- 模板选择：一个直接预览、多个可选、无模板明确提示；无设计权用户仍可按运行权限打印。
- 多行表头/表头横向合并；表格前后文本顺延；页码/总页数稳定。
- 浏览器取消打印不提示“打印成功”；用户另存 PDF 的文件能够打开，页数/内容与预览一致。
- 性能采样：10/100/500 行的测量时间、分页时间、页数、峰值资源；记录机器/浏览器与字体，不预先宣称性能达标。
- 实机检查 A4 100% 缩放、关闭浏览器额外页眉页脚，尺量纸面尺寸与边距；不同打印机分别记录偏差。

## 4. 计划命令

以下为后续执行模板，不是本轮成功记录；文件/模块尚未新增前不得执行或伪造通过。

前端（工作目录 forge-admin-ui）：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --ignore-workspace exec vitest run src/components/print src/stores/print
pnpm --ignore-workspace exec eslint src/components/print src/stores/print src/views/print src/api/print.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm --ignore-workspace build
```

后端（工作目录 forge-server，先确认 Java 17 实际路径，不照抄旧机器路径）：

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-print -am compile -DskipTests
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
mvn -pl forge-admin-server -am package -DskipTests
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest='Print*Test' -Dsurefire.failIfNoSpecifiedTests=false
```

generator/代码业务的针对性测试按本轮实际类名指定模块与 `-Dtest`；检查报告确有测试运行，不能把跳过视为通过。发布 package 保持跳过测试的用户偏好。

SQL：扫描新增迁移防重复/租户/逻辑删除/敏感数据，并检查 `rg -n '\$\{[^}]+\}' <新增迁移路径>` 无业务占位符。XML 解析、重复 Flyway 版本检查随迁移变更执行。

浏览器：使用受控临时前端验证环境和合成 API 数据，具体脚本在 T08/M1 建立后加入本文件；打印结果使用真实浏览器输出或 PDF 检查，不以 jsdom 元素存在断言代替。记录启动/停止的服务。

## 5. 真实联调交付清单（用户执行）

1. 备份后运行新增迁移，确认 forge_schema_history 成功且表/索引/权限资源正确。
2. 分配设计、发布、普通打印使用者角色；准备同租户和跨租户合成记录。
3. 发布模板和应用，实际列表/详情打印；校验草稿更新不影响已发布版本。
4. 完成主子表审批、会签、退回重提，检查待办/已办/我发起打印范围、签名和实例归属。
5. 验证未授权字段/子表列/文件及伪造请求被拒绝。
6. 实际浏览器另存 PDF 与打印机出纸；核对行数、页数、金额、页边距和签名。
7. 停用/应用回滚后复验；填入版本号、环境、结果与证据路径。

## 6. 本轮增量：2026-09-18 提案文档

- 范围：proposal/spec/design/tasks/test-spec/execution-log。
- 应执行：Markdown 本地链接、现状引用路径、任务 ID/依赖、状态一致性、空白问题检查。
- Git diff：工作区缺少 Git 元数据，不创建 .git；对六份新增文档逐文件执行 `git diff --no-index --check -- /dev/null <文档>`。退出码 1 仅表示新增差异，空白诊断输出为空且无其它错误时通过。
- 不执行：依赖安装、构建、单测、浏览器、服务、迁移、打印机测试，原因是本轮无业务实现。

## 7. 本轮增量：M1 打印协议与运行时

- 单测范围：`src/components/print/`，目前 35 项；协议、绑定、资源、分页、渲染、iframe 生命周期。
- 资源清理测试发现 Set.forEach 透传额外参数，已修正 URL 回收适配；尺寸测试改用浮点容差。
- T03 加 style.js 共享 CSS；T04 加 codes.js 复用已安装编码库；T06 加 prepare.js 分离测量前绑定；渲染行为测试作为 T03/T04 的共同验证文件。
- T08 增加 verification 独立合成入口，不修改业务路由，不启动 Admin/Flow。
- 本机 v20.19.0 不存在，实际可用 Node v24.21.0。用现有依赖直接运行本地 Vitest/ESLint/Vite 入口，避免 pnpm 自动替换依赖。

```bash
# 工作目录 forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v24.21.0
node node_modules/vitest/vitest.mjs run src/components/print
node node_modules/eslint/bin/eslint.js src/components/print
node --max-old-space-size=8192 node_modules/vite/bin/vite.js build
```

最终增量：8 文件 39 项通过，增加字体失败、预览卸载后的异步打印取消、打印会话取消、连续 keepWithNext 约束。ESLint 无错误、无警告。主项目构建与独立打印验证入口构建通过，具体输出见 execution-log。

实际浏览器：0/10/100/500 行分别为 1/1/4/17 页，行顺序和数量一致；100/500 行隔离打印 DOM 无溢出且 iframe 释放后为 0。字体不存在、图片损坏和超高行返回预期错误；按钮调用输出 DIALOG_OPENED 客户端事件。证据见 [verification/browser-results.json](verification/browser-results.json)。

仍未执行：保存 PDF、物理打印机、罕见字字形逐项检查、真实数据/权限/数据库/流程 E2E。目标已纠正为现有 forge-admin Git 仓库，不再需要初始化确认；复验和提交结果见后续追加，M2–M6 未开始。

## 8. 本轮增量：目标纠正与 Forge 仓库复验

- 执行目录：`/Users/mini32g/Desktop/project/forge-admin/forge-admin-ui`。
- 相同 M1 代码在目标依赖环境重新运行：8 文件 39 项测试通过；ESLint 无错误/警告。
- 主项目生产构建通过；独立验证入口构建通过，避免未接入路由的打印源码漏检。
- 目标浏览器验证：100 行 4 页、500 行 17 页；隔离输出 500 行，无溢出，3 图片就绪，释放后 iframe 为 0。三类故障按预期阻断。
- 前序不自动初始化 Git 的疑问已解除：正确项目有现成 Git，本地分支 `codex/forge-native-print`。SDD 文档提交为 `bc72aafc`，M1 代码随后单独提交，禁止 push。
- 未执行数据库、真实流程、物理打印或保存 PDF；后续阶段保持未开始。

## 9. M2 增量验证计划

- 先编写设计器状态行为测试并记录 RED，再实现命令/历史和组件。
- 覆盖缩放坐标、组合边界、拖动单次撤销、取消、复制唯一 ID、无效导入原子性、异步保存期间继续编辑、失败保存不清 dirty、卸载事件清理。
- 浏览器验证拖动/框选/属性/区块顺序/保存恢复/预览/明暗主题，继续使用独立合成入口，不连接真实服务。
- 本轮只扩大打印模块测试范围；执行目标 ESLint、主项目构建和验证入口构建。

M2 最终结果：10 文件 58 项通过（M1 39 + M2 19）；状态历史、手势缩放/取消/卸载、框选键盘、复制/无效导入/异步保存/存储失败/字段失效/表头合并与合计撤销已覆盖。区块 ID 重名回归先 RED 后 GREEN。ESLint、主项目和合成入口构建通过。浏览器增量及未执行范围见 execution-log 与 verification/browser-results-m2.json。

## 10. M3a 增量验证计划

- 先新增协议行为用例再实现：合成完整模板、零/假/null、大数、顺序无关哈希、未知/重复键、脚本属性、原型路径、外部图片、越界元素、页码上下文、表格合并和尺寸、1MiB/数量限制。
- DTO 使用 Jakarta Validator 验证必填、ID/revision/枚举/长度；MyBatis 解析真实 XML 检查绑定参数与租户/删除/CAS/行锁条件；迁移静态核对四表审计字段、活动唯一键、版本永久唯一、字典与权限防重复、无自动角色授予。
- 后端目标单测必须启用 -Penable-tests；编译打印模块并尝试 Admin 聚合 package -DskipTests。不启动 Admin/Flow/MySQL/Redis，不实跑迁移，真实事务竞争/租户拦截/DDL 留用户后续验收。
- 本机没有 Java/Maven，临时下载官方发行版并校验摘要，仅 process-local JAVA_HOME/PATH 与临时 Maven 缓存，不修改系统或提交工具二进制。

M3a 增强：H2 MySQL 模式加载冲突修复后的 V1.0.171 打印 DDL，仅去除 ENGINE/CHARSET/COLLATE 后运行 Mapper 行为测试；不会据此宣称 MySQL Flyway 已执行或已验证租户拦截器。

### M3a 最终结果

- Java：7 个测试类、72 项、0 失败/错误/跳过；包含 29 项跨语言共享协议样例、5 项 H2 Mapper 行为测试。使用根 enable-tests profile，确认 Surefire 实际运行。
- 前端兼容：`source ~/.nvm/nvm.sh && nvm use v24.21.0 && node code-copilot/changes/forge-native-print/verification/protocol-compatibility.mjs`，29/29。服务端额外拒绝未使用属性中的非法格式，与设计中严格边界一致。
- 构建：Java 17，打印模块及依赖测试成功；`mvn -pl forge-admin-server -am package -DskipTests` 串行聚合 46 模块通过（本机实际命令另有临时 Maven settings，见 execution-log）。
- 静态：4 个 Mapper XML 与 5 个 POM 解析、Flyway 版本唯一性、V1.0.171/V1.0.172 无业务占位符、空白检查通过。
- 不扩大前端页面回归：本轮未改任何 UI 源码，独立构建实际前端协议模块验证兼容；M1/M2 页面和 58 项既有测试没有重跑，沿用上一阶段证据。
- 未执行真实 MySQL/Flyway、Admin/Flow 启动、API/低代码/流程 E2E、浏览器打印、PDF 或物理打印。详见 verification/m3a-results.json。

## 11. M3b 增量验证计划

- 后端先写 Provider 缺失/重复的 RED 测试，再覆盖身份与来源核验、模板/版本/CAS/回滚/引用保护、绑定默认切换、runtime 不依赖设计权、旧版本与来源伪造、字段投影、500 行/4MiB、事件归属与幂等。
- Controller 使用 MockMvc 与反射核对 DTO/加密/权限/不记录正文；Spring 事务代理配 H2 Mapper 验证服务端原子性，合成 Provider 只在 test 源码。
- 前端 API/Pinia/编辑保存与切换并发测试、目标 ESLint、主构建和独立合成入口构建；浏览器实际保存/冲突/发布/选择模板/切换清理验证，合成 HTTP 不计真实业务 E2E。
- 复用 M3a 临时 Java 17/Maven 和隔离缓存，串行 Admin package；不启动真实 Admin/Flow/MySQL/Redis、不执行 Flyway，继续保留人工 E2E 未验收状态。

### M3b 验证结果（2026-09-19）

- 104 项 Java 测试和 71 项前端测试通过；其中 M3b 新增/扩展登录权限、事务回滚、CAS、应用锁并发默认项、固定发布版本、字段/资源授权、执行事件及异步状态隔离。
- `PrintTemplateControllerTest` 走 MockMvc→真实事务 Service→H2 Mapper；只检查加密/Sa-Token 注解契约，没有模拟成已通过真实认证或加密握手。生成的 target/print-prepare-wire.json 为合成数据，冻结样例见 verification/m3b-wire.json。
- 浏览器测试使用独立 `--persistence` 验证服务器；生产页面和 store 保持原实现，仅 API/字典/用户来源在验证 Vite 配置中替换。已观察服务器失败时没有本地数据兜底。
- 前端 12 个测试文件、定向 ESLint、Vite 生产构建、46 模块 Maven Admin package 通过；API 路径统一为 `/print/templates/page` 后增量跑 2 项契约测试并重建前端。
- 真实 MySQL 锁语义/迁移、租户拦截器、应用/记录/流程授权适配器、PDF 与物理打印仍未执行，不能由 H2 或浏览器 mock 代替。

## M4a 增量验证

复用 M3b 基线：新增字符串页面身份的 DTO/HTTP/真实 Mapper/路由行为测试；兼容旧数字身份 hash；应用可见范围/权限/actor 租户检查；历史版本引用保护、锁与事务边界；固定版本归属/hash/停用失败不提交发布指针、旧快照兼容。执行相关 Print 和 generator 目标单测、Admin 聚合 package、前端 lint/打印单测/build。仅路由解析逻辑调整，不改可视组件；真实 MySQL 迁移与锁并发验收仍待用户环境，不启动 Admin/Flow。

M4a 实际结果：Print 106 + generator 30 项通过，PrintIdentityTest 扩充 current 方法断言后单独复验 1 项通过（不重复计入总数）；前端 77 项通过，ESLint 正则修复后路由 8 项复验通过；Admin 46 模块 package 与 Vite build 通过。无视觉组件修改，本次仅验证路由解析，不重复上一阶段浏览器截图。H2 覆盖 SQL/事务/并发阻塞，不能代替 MySQL REPEATABLE READ 与冲突修复后的 V1.0.174 information_schema/PREPARE 实跑。

真实环境下一轮新增验收：先运行 V1.0.174，检查两表旧数字 page_id 仍等值且 source_key 未改变；应用发布、停用/删除模板并发时不能生成悬空引用；保留历史应用版本时解除当前绑定仍不能删除模板。候选打印快照生成和 DataProvider 在 M4b 安装后才执行完整真实业务链路。

## M4b 增量验证

复用 M4a 的固定引用、事务和 H2 基线。新增验证：候选快照固定版本且重试不漂移；页面/对象身份和字段删除、隐藏、类型变化拒绝；历史 CRUD 配置不回退草稿；主子表记录范围及脱敏失败关闭；金额/关联/字典/公式值归一；文件鉴权与流程场景拒绝。只运行目标测试及 Admin 聚合构建，不启动真实服务或数据库。本轮后端为主，未修改前端则不重复前端构建。

M4b 执行结果：106 项打印插件 + 79 项 generator 回归通过；新增设计版本状态断言后元数据/Provider 的 15 项增量复验通过（新增 1 项，总计 186 个不同用例）。新增/扩展 H2 用例覆盖候选绑定当前读、tenant/status/del_flag、字段校验失败时应用版本/指针不前进；纯服务验证固定元数据、来源/权限、字段/列范围、当前记录读取与严格处理、文件授权。VirtualFormulaPrintTest 使用真实引擎验证计算，同时验证错误不含业务输入且不写公式日志。Admin 46 模块构建通过。没有连接真实 MySQL、Admin/Flow 或浏览器，完整入口 E2E 留 M4c 与 T44。

## T46 设计器视觉基线增量验证

- 组件测试：标尺按 5mm 生成刻度并突出 10mm 主刻度；纸张网格、边距、页眉/页脚辅助线与开关状态可观察；A4/A5、旋转和缩放保持协议尺寸合法且支持撤销。
- 静态检查：只检查本轮打印设计器、Pinia 和验证入口文件；执行 `git diff --check`。
- 构建：执行前端 Vite 生产构建，确认新增图标/CSS 和组件拆分可打包。
- 浏览器：使用合成工作台的桌面视口进入模板设计页，核对三栏、横纵标尺、网格、纸张辅助线、工具栏和物料区；合成地址不得冒充正式业务入口。

执行结果：打印域 17 个测试文件、101 项通过；本轮目标 ESLint 与 `git diff --check` 通过；Vite 9366 modules 生产构建成功。合成浏览器通过正式形态路由 `/app-center/application/purchase-demo?section=printing` 进入 `/print/designer?templateId=1`，已观察三栏、双向标尺、网格、纸张/选中辅助线，控制台无 error/warn。真实 Admin/Flow/数据库仍未启动，不改变 T42/T44 的用户验收边界。

## T47 工作台、动态定位线与预览增量验证

- 状态/命令测试：验证纸张/元素边界及中心吸附、阈值外不吸附、手势结束/取消/切换区块清除定位线；左右面板收起状态由 Pinia 管理且不改变模板协议。
- 组件测试：高频命令以图标按钮暴露 `aria-label`/提示，低频命令和区块操作进入菜单；预览具备适合宽度、缩放、页数/页码信息，设计预览继续隐藏正式打印动作。
- 静态和构建：运行打印域 Vitest、触达文件 ESLint、`git diff --check` 与 Vite 生产构建；Node v20.19.0 若仍不可用，记录实际使用的 v24.21.0，不修改项目依赖。
- 浏览器：在合成正式形态路由检查默认窄窗口和桌面窗口；确认工具栏不再堆叠文本按钮、左右面板可收起、拖动时定位线出现且松手消失、预览纸张层级与缩放/适宽可用。合成数据不替代真实 Admin/Flow/PDF/打印机验收。

执行结果：打印组件、打印 API 与打印 Pinia Store 共 18 个测试文件、108 项通过；吸附阈值、移动/缩放定位线清理、六向对齐、水平/垂直分布、面板状态、紧凑命令和预览所有权均有行为断言。触达文件 ESLint 与 `git diff --check` 无输出；Vite 9367 modules 生产构建成功，保留项目既有 native config、CSS 注释、dynamic import 和插件耗时提示。浏览器实际核对桌面三栏、默认窄窗口自动收起两侧面板、组件面板抽屉、紧凑单行命令栏及文档式预览；合成服务继续保留供用户查看，未启动真实 Admin/Flow/MySQL/Redis，也未执行 PDF 或物理打印。

## M4c 增量验证计划

复用 M4b 与 M3b 浏览器基线：新增实际页面来源、重复对象跨页面隔离、来源失效/切换清理、权限隐藏、列表/详情动作参数、只有发布绑定投影、字符串主键 URL 编码、下载固定版本/hash/完整性及共享运行依赖。验证 AiCrudPage 无修改，新增/修改 SFC 均不超过 800 行。运行定向 Java/Vitest、ESLint、Admin/Vite 构建，合成浏览器检查明暗主题与窄屏。真实授权 HTTP/MySQL/发布/物理打印保留人工验收。


M4c 结果：首轮 34 项后端、86 项前端通过；新增真实 Velocity 生成与 AiCrudPage 点击验证后，最终回归 print 106 + generator 94 = 200 项，聚合子对象导出补充后 25 项定向复验通过（新增 1 项，累计 201 个不同用例）；前端 16 文件 89 项通过。SQL 真实 MyBatis 参数绑定与 JSqlParser 解析通过，未冒充 MySQL 执行。ESLint、Vite 生产/合成入口构建、最终 Admin 46 模块构建成功。

浏览器增量：合成 HTTP 创建请求 page_archive 自动带入；列表/详情各自传 LIST/DETAIL，特殊字符主键完整保持，无 row 正文。亮暗布局可读；390px 页面宽度仍 390，表格视区 342、内容 620，可滚动到更多操作。没有模板查看权限显示明确空态。既有分页/设计器验证基线继续沿用，不重复实机/PDF。

## T48 设计内容同源与基础能力增量验证

- 示例数据：字段路径不出现在纸面正文；主表、子表、日期、金额、布尔和图片生成稳定的非业务示例，真实 context 存在时不被示例覆盖。画布和模板预览接收同一 context。
- 渲染一致：文本、页码、横竖线、矩形、椭圆直接复用正式渲染组件；条码/二维码复用正式编码器；表格示例行使用列格式和字段目录。
- 命令和物料：测试物料预设尺寸/样式、椭圆协议往返、全选、复制一份、置顶、置底、删除与撤销；检查 A3/A4/A5/B4/B5 和新增样式控件。
- 自动化：运行打印前端 Vitest、共享协议兼容脚本、打印插件协议目标测试、触达文件 ESLint、`git diff --check` 和 Vite 生产构建。浏览器核对字段中文示例、更多物料、画布命令以及设计/预览首屏内容一致；不启动真实 Admin/Flow/MySQL/Redis，不执行 PDF 或物理打印。

执行结果：前端打印组件、API 与 Store 共 19 个测试文件 114 项通过；共享跨语言协议 29/29，后端 `PrintProtocolValidatorTest` 34 项通过。定向 ESLint 与 `git diff --check` 无输出，Vite 9370 modules 构建成功，仅保留项目既有 native config、CSS 注释、dynamic import 和插件耗时提示。浏览器确认纸面不再显示字段路径，设计和预览均显示同一主表/流式文本/三行明细/合并表头/合计行；组件面板显示 10 个基础物料和 3 个内容区块，选中元素后 7 个画布命令可用。真实 Admin/Flow/MySQL/Redis、PDF 和物理打印未执行。

## T49 元素级编辑增量验证

- 协议：旋转限制为 -180–180 度，镜像/锁定必须为布尔值；图片适配、边框样式和圆角仅接受白名单值，未知属性继续拒绝。前后端对新增字段做相同接受/拒绝断言。
- 命令：旋转、镜像、锁定和解锁均可撤销；锁定后拖动、缩放、方向键、对齐、层级和删除无效；复制/粘贴出的元素解锁且 ID 唯一。
- 组件：右键菜单根据空白区/选择/锁定状态启用命令；属性面板可编辑旋转、镜像、锁定、图片适配、条码制式、页码格式、边框样式和圆角。设计 Canvas 与 PrintPage 的 transform 和图片适配一致。
- 执行打印前端回归、共享协议检查、Java `PrintProtocolValidatorTest`、定向 ESLint、空白检查和 Vite build；浏览器实际核对右键菜单、锁定/解锁、旋转及属性面板。真实业务和物理输出边界不变。

执行结果：前端打印组件、API 与 Store 共 19 个测试文件 119 项通过；共享跨语言协议 34/34，Java `PrintProtocolCompatibilityTest` 34 项与 `PrintProtocolValidatorTest` 35 项通过。定向 ESLint 与 `git diff --check` 无输出，Vite 9372 modules 构建成功，仅保留项目既有 native config、CSS 注释、dynamic import 和插件耗时提示。浏览器确认元素右键菜单、90° 旋转、锁定后的无缩放柄/禁用删除、元素行为属性区均可用；设计元素与预览元素的 transform 完全一致。验证产生的临时旋转与锁定已还原。真实 Admin/Flow/MySQL/Redis、PDF 和物理打印未执行。

## T50–T53 高级设计能力增量验证计划

- T50 空白表格：前后端同时接受完整 2×2/合并矩阵，拒绝空行列、越界、重叠、缺口、尺寸不一致和危险属性；Store 覆盖增删行列、矩形合并、拆分、逐格样式、撤销重做及复制 ID 重建；Canvas 覆盖双击直接编辑和选中反馈，PrintPage 与设计态结构一致。
- T51 多页/分页符：分页引擎覆盖显式换页、自动换页叠加、首尾/连续分页符拒绝和 50 页上限；设计分页纯函数覆盖纸张方向、正文容量、固定/文本/表格估算及分页标识；浏览器确认多张纸张、页码和页眉页脚重复展示。
- T52 注册机制：拒绝重复/非法 key、异步工厂、函数或非标准片段；注册项展开后必须通过现有协议，导出 JSON 不含 registry/component/function 字段；示例业务组件在无匹配字段时使用安全固定占位，有授权字段时使用字段绑定。
- T53 校准验收：校准结果使用选择纸张真实毫米尺寸，包含 10mm 边距框、100mm 标尺与横纵方向信息；能力检查不把“支持 window.print”报告成物理成功；隔离 iframe、afterprint/取消清理、纸张 CSS 和本地验收状态均有测试。
- 每阶段先跑触达单测和共享协议/Java 协议，再跑打印域回归、定向 ESLint、`git diff --check` 与 Vite build；UI 阶段在本地合成入口实际点击。真实 Admin/Flow/MySQL/Redis、Firefox/Edge/Safari 和物理打印机不由自动化伪造结果。

T50 执行结果：前端打印域 20 个测试文件 125 项、共享协议 36/36、Java 打印插件 116 项全部通过；定向 ESLint、`git diff --check` 与 Vite 9379 modules 生产构建通过。Chromium 合成工作台实际完成空白表格添加、单元格选择/Shift 多选、双击直接输入“合同编号”，并确认正式预览输出同一 3×3 表格和文字。真实业务、其它浏览器与物理打印未执行。

T51 执行结果：前端打印域 21 个测试文件 131 项、共享协议 38/38、Java 打印插件 120 项全部通过；定向 ESLint、`git diff --check` 与 Vite 9380 modules 生产构建通过。Chromium 合成工作台实际在固定区块后插入分页符，设计区显示两张独立 A4 纸、手动分页标识、重复页眉页脚和 `1 / 2`、`2 / 2` 页码；正式预览同样输出两页，第二页从流式文本和明细表格开始。设计页数明确标为示例数据估算，正式预览仍由共享分页引擎测量。真实业务、其它浏览器与物理打印未执行。

T52 执行结果：前端打印域 22 个测试文件 135 项、共享协议 38/38 全部通过；定向 ESLint、`git diff --check` 与 Vite 9382 modules 生产构建通过。注册器自动化覆盖非法/重复 key、声明式异步工厂、Promise 返回、函数字段、未知片段和非法协议类型的原子拒绝，并确认导出 JSON 不含注册 key、factory、registry 或函数。Chromium 合成工作台显示审批状态、签章位置、合同条款三个业务物料；实际插入审批状态后只生成 Forge 固定区块和文本元素，设计区与正式预览均显示安全示例值。真实业务插件、Admin/Flow/MySQL/Redis、其它浏览器与物理打印未执行。

T53 执行结果：前端打印域 23 个测试文件 147 项、共享协议 38/38、Java 协议 75 项全部通过；定向 ESLint、`git diff --check` 与 Vite 9385 modules 生产构建通过。校准自动化覆盖 A3/A4/A5/B4/B5、横纵方向、自定义尺寸、10mm 边距框、横纵 100mm 标尺、隔离 iframe、精确 `@page`、afterprint 清理、能力报告和按纸张配置隔离的本机验收记录，并明确 `window.print` 或 `DIALOG_OPENED` 都不是物理成功。Chromium 在 `http://127.0.0.1:4322/print/designer?templateId=1&ui=t53` 从“更多”打开三栏校准工作台，核对 A4 纵向/横向、自定义纸张、浏览器能力、正式渲染页和待人工确认记录保存。没有自动打开系统打印对话框或伪造实体测量；Firefox/Edge/Safari、真实 Admin/Flow/MySQL/Redis、PDF 与物理打印仍由用户验收。

## T54 Flyway 并行版本冲突修复验证计划

- 以启动日志中的 applied checksum 为基线，按 Flyway 逐行 CRC32 算法确认流程 V1.0.168–V1.0.170 分别为 `-1783583920`、`-211989272`、`-698851386`；本地不得再把这些版本解析为打印 SQL。
- 确认迁移目录版本唯一，打印迁移按 V1.0.171 建表、V1.0.172 字典权限、V1.0.173 隐藏路由、V1.0.174 页面身份扩展顺序存在；旧打印文件名不存在，测试与文档不残留错误版本引用。
- 运行 `PrintResourceContractTest`、`PrintPersistenceTest`、`PrintApplicationPersistenceTest` 及打印插件完整回归；执行 Admin 聚合 package，验证 Flyway 资源能被打包。真实远端数据库不执行 repair、不由自动化连接或修改，用户重新启动后观察正常 migrate。

T54 执行结果：流程 V1.0.168–V1.0.170 的 Flyway checksum 分别固定为 `-1783583920`、`-211989272`、`-698851386`；迁移目录无重复版本。四份打印 SQL 与顺延前的 Git blob 完全一致，只变更文件版本名为 V1.0.171–V1.0.174。`PrintResourceContractTest + PrintPersistenceTest` 8 项、`PrintApplicationPersistenceTest` 8 项和打印插件完整回归 121 项全部通过，Admin 46 模块聚合 package 成功，`git diff --check` 与本轮 7 份迁移的业务占位符扫描无输出。仓库全量扫描仍会命中既有 V1.0.72 消息模板中的运行时 `${...}` 文本，本轮不改写已执行历史。首次完整回归在沙箱内因 Mockito/Byte Buddy 无法自附加产生 32 个环境性错误；相同命令在允许 JVM attach 的本机执行环境复跑为 121/121 通过。没有连接或修改真实数据库，没有执行 repair；目标库正常 migrate 仍由用户重启验收。

真实联调追加：应用发布后从实际门户的列表/详情各打印一条；同对象跨页面检查模板范围；同一次应用代码下载中并发发布新版本，检查对象协议与 application-printing.json 固定引用一致；MySQL 实跑 selectPublishedPrintSources 的 JSON_CONTAINS 与租户条件；独立部署须先完成 PRINTING.md 列出的资产导入和 Provider 适配。

## M5a 增量验证

- 流程身份：taskId、processInstanceId、processRunId、businessKey、objectCode、recordId 和代码表单 formKey 均由 Flow 受保护接口与业务运行表反查后逐项比对；伪造任一标识拒绝。
- 场景权限：待办只允许当前签收人或经 Flow 候选可见性校验的未签收任务；已办只允许实际办理人/owner；我发起只允许实例发起人。三类场景分别测试，不用单一只读标志代替授权。
- 数据范围：节点显式 `readable=false` 的主表/子表字段从目录移除；RESTRICT 只保留节点模板 ID 子集。审批轨迹保持每个 taskId 独立并限制 1000 条，不按节点名合并会签或重提轮次。
- Provider：低代码 FLOW 场景要求真实应用 processRun；采购 CODE Provider 读取业务 Service 和应用不可变发布快照，目录排除内部 ID、流程身份和上传 fileId，并增加审批轨迹目录。
- 自动化：`FlowPrintAccessPolicyTest` 4 项、`LowcodePrintDataProviderTest` 7 项、`SamplePurchaseOrderPrintDataProviderTest` 2 项，共 13 项通过；业务核心 35 模块 compile 与流程插件 28 模块 compile 通过。真实 Flow/Admin/MySQL/Redis 未启动，真实任务/实例 E2E 留 T44。

## M5b 增量验证

- 结构约束：先迁出 `todo.vue`、`started.vue`、`FlowTaskDetailShell.vue` 的作用域样式；以行数检查确认触达 SFC 均回到 2000/800 行阈值内。共享流程打印上下文只进入 Pinia Store，详情入口不转发业务正文。
- 流程身份：前端请求只包含 application/source/record/scene/task/instance/run 的稳定身份；低代码场景必须有不可变 processRunId。后端表单上下文优先返回业务流程 run 固定的应用身份，旧 CODE 流程只有在业务对象唯一归属一个已发布应用时才补齐；多个候选保持未解析。
- 交互：待办有未保存修改时先明确提示打印的是服务端已保存数据；切换任务关闭模板选择并丢弃旧异步回包。待办、已办、我发起复用同一 `FlowPrintAction` 与现有 `PrintTemplatePicker`。
- 节点策略：`INHERIT` 不写冗余 BPMN 属性；`RESTRICT` 写允许模板 ID 子集，空子集代表当前节点无模板。模板列表按当前应用及 CODE formKey / LOWCODE pageId 收窄，策略只减权、不授予额外权限。
- 自动化：前端 7 文件 40 项通过（Store/动作/策略组件/BPMN 解析写回及既有 roundtrip/json-to-bpmn）；定向 ESLint 与 Vite 生产构建通过。后端 33 模块聚合测试编译通过，`BusinessFlowService*Test + FlowPrintAccessPolicyTest` 共 22 项通过；新增 Mapper XML 通过 xmllint。
- 仍未执行：真实 Admin/Flow/MySQL/Redis、Flyway、待办/已办/我发起浏览器 E2E、PDF 和物理打印。M5c 的签名/图片鉴权资源失败阻断与执行事件收口尚未实施。

## M5c 增量验证

- 前端打印域 16 个测试文件 98 项通过；新增覆盖流程历史签名的鉴权下载、表格图片渲染、fileId 不落纸面、资源错误/取消的受限审计码。定向 ESLint 无输出，Vite 生产构建 9364 modules 成功。
- 后端打印插件 `Print*` 17 个测试类 107 项通过；新增覆盖审批签名文件进入运行资源授权集合、FAILED 不接受 pageCount、DIALOG_OPENED 返回 `physicalOutputConfirmed=false`。Admin 46 模块聚合 package 成功。
- 最终合并回归：前端打印/流程入口/BPMN 策略 22 文件 133 项；后端打印插件 107 项、generator 的打印/应用版本/流程上下文 113 项、采购 CODE Provider 2 项，共 222 项，全部 0 failure/error/skipped。
- 代码审查确认预览或模板选择不发送 `DIALOG_OPENED`；只有调用隔离 iframe 的 `print()` 后才报告。403、非图片 Blob、解码失败和超时均在打印会话创建前阻断，切换/卸载继续取消请求并释放 Blob URL。
- 未启动 Admin/Flow/MySQL/Redis，未执行 Flyway、真实流程、PDF 或物理打印。用户验收步骤见 [verification/user-acceptance.md](verification/user-acceptance.md)，完成前状态保持 `implemented-pending-e2e`。


## T55 正式前端入口修复验证计划

- 路由合同：应用卡片快捷入口必须解析到 `BusinessApplicationRuntime`，携带原 applicationCode、`view=settings` 和 `settingsSection=printing`；未知设置分区回退基础属性。
- 设置分区：查询参数为 printing 时显示“打印模板”导航和独立打印设置组件，且不显示通用“保存设置”按钮；普通设置分区行为保持不变。
- 工作区加载：按 applicationCode 调用 `businessApplicationWorkspaceByCode`，成功时把 application/objects 交给既有 `ApplicationPrintPanel`；失败展示可重试错误态，切换编码忽略旧异步响应。
- 结构与构建：新增/修改 SFC 均低于 800 行，不触达 4900 行统一运行页和 1500 行应用中心入口页；运行定向 Vitest、ESLint、`git diff --check` 和 Vite 生产构建。
- 浏览器：后端可用时从应用中心卡片“更多 → 打印模板”进入并核对模板列表；后端不可用时只验证 Vite 路由可达和登录/请求边界，不把 502 或合成数据记为业务验收通过。


T55 执行结果：新增入口/设置/工作区组件测试 4 个文件 7 项通过；与打印域合并回归共 27 个文件 154 项通过。触达文件定向 ESLint、`git diff --check` 和 SFC 行数检查通过；Vite 9388 modules 生产构建成功，仅保留项目既有 native config、CSS 注释、dynamic import 和插件耗时提示。浏览器访问正式 URL 后正确进入登录路由，并完整保留 `view=settings&settingsSection=printing`；当前 8580 后端未运行，请求返回 502，因此未把登录后的模板列表记为真实业务验收通过。


## T56 打印权限通配修复验证计划

- 纯函数覆盖：`isAdmin=true`、`permissions=['*:*:*']`、`dataPermission=['**']`、精确 `print:template:*` 均允许；空数组、null 和无关权限拒绝。
- 接入检查：`ApplicationPrintPanel` 的查看、`PrintTemplateList` 的管理、打印设计器的管理/发布全部调用同一权限函数，不残留只识别 `**` 的内联判断。
- 回归：运行权限目标测试、打印域 Vitest、触达文件 ESLint、`git diff --check` 和 Vite 生产构建。后端权限注解与接口重新授权保持不变。


T56 执行结果：权限纯函数 4 项通过，覆盖本次用户返回的 `isAdmin=true` 和 `permissions=['*:*:*']`，同时覆盖 `**`、精确权限与拒绝路径。打印域、正式入口及工作区合并回归 29 个文件 165 项通过；触达文件 ESLint 与 `git diff --check` 通过，源码扫描确认查看、管理、发布三处均使用统一函数。Vite 9389 modules 生产构建成功，仅保留项目既有构建提示。浏览器仍因当前 Codex 会话未登录且本机 8580 返回 502，未冒充登录后页面验收。

## T57 表达式 / 汇总 / 连续纸拼版 / 套打水印 / 溢出 / PDF 下载

- 表达式：白名单四则运算与 `SUM/AVG/COUNT/MIN/MAX/ABS/ROUND/IF/CONCAT/TEXT/MONEY/UPPER/RMB`；拒绝 `eval` 与控制字符。金额大写走 `MONEY_UPPER`（分）或 `UPPER`/`RMB`。
- 表格：每页 `subtotal`、末页 `footer`；小计只聚合当前页数据行。
- 纸张：`CONTINUOUS` 按内容撑高；`tiling` 2×2 标签拼到目标纸，拼版内标签不再计为独立打印页。`textFit` 覆盖 CLIP/SHRINK/AUTO_HEIGHT。
- PDF：预览「PDF」截取已排版 `PrintPage`（`html-to-image` + `jspdf`）下载文件，事件为 `PDF_DOWNLOADED`；「打印」仍只打开对话框。套打底图默认不进打印/PDF。
- 自动化：前端打印域 Vitest、共享协议 `protocol-compatibility.mjs`、Java `PrintProtocolValidatorTest`/`PrintProtocolCompatibilityTest`、触达文件 ESLint、`git diff --check`、Vite 生产构建。
- 跳过：真实 Admin/Flow/MySQL/Redis、登录后预览实点下载、物理打印机；客户端 PDF 不是服务端归档件。

T57 执行结果：前端打印域 25 文件 168 项通过；共享协议 41/41；Java 协议测试 81 项通过。触达文件 ESLint 与 `git diff --check` 无输出；Vite 生产构建 42.46s 成功，产物含 html2canvas/jspdf 懒加载 chunk。未启动真实 Admin/Flow/MySQL/Redis，未做登录后实点下载或物理打印。

## T57 增量：PDF_DOWNLOADED 事件 + 预览同源导出

- 后端接受 `PDF_DOWNLOADED`（与 `DIALOG_OPENED` 同形，不代表出纸）；已执行 V1.0.172 不改，新增 V1.0.180 字典。
- 导出截取预览已排版 `[data-print-page]`（`html-to-image` + `jspdf` PNG），不再 html2canvas 重排。
- 自动化：打印域 Vitest、触达 ESLint、Java `PrintExecutionServiceTest`/`PrintResourceContractTest`/`PrintProtocolValidatorTest`、`git diff --check`。
- 跳过：真实 Admin 重启后的登录下载、物理打印、全量 Vite 生产构建。

T57 增量执行结果：前端 `src/components/print` + `src/stores/print` 27 文件 183 项通过；触达 ESLint（CI=true）无输出；`git diff --check` 无空白错误。Java 17 `PrintExecutionServiceTest` 3 + `PrintProtocolValidatorTest` 40 + `PrintResourceContractTest` 3，共 46 项通过。未启动真实 Admin/Flow/MySQL，V1.0.180 需用户重启 Admin 后才会写入字典并加载新枚举。

## T57 增量：表头边框 / PDF 文件名 / 预览骨架屏

- 表头上/左边改画在单元格 `background-image` 内侧线上，避免不透明表头盖住外框、祖先 `overflow:hidden` 裁掉贴边细线。
- `exportFileName` 在纸张面板可配；默认「模板名-业务名称-时间戳」，未写 `{timestamp}` 仍追加。
- `/print/preview` 准备模板/资源时用纸张骨架屏，不再用 `NSpin`。
- 自动化：`exportFileName`/`protocol`/`renderers`/`PrintPreview`/`exportPrintPdf`/`PrintDesigner`/`staticTable` 共 7 文件 52 项；触达 ESLint（不含 PrintCanvas 既有 curly）；`git diff --check`。
- 跳过：本机 `localhost:3000` 当前不可达，未做登录后实点；未跑 Java 本轮（协议校验此前已覆盖 exportFileName）。

T57 增量执行结果：Node v20.19.5 上述 7 个 Vitest 文件 52 项通过。Cursor 内置浏览器无法打开 localhost。未启动 Admin/Flow/MySQL。本轮未 commit。

## T57 增量：表格表头/表体独立配色

- 空白表格插入不再生成「表头」灰行；`headerStyle` 只作用第一行，`style` 作用表体。
- 右侧「样式」页拆成表头/表体（明细表另有斑马纹）；基础面板去掉重复整表色板，列色只覆盖表体。
- 点选格子不再用 `!important` 盖住真实底色；「设为表头」写 `headerStyle` 而不是给格子写死灰底。
- 自动化：`staticTable`/`PrintDesigner`/`designerSample`/`history`/`stores/print` 共 6 文件 68 项；触达 ESLint；`git diff --check`。
- 跳过：本机 `/print/designer` 跳登录验证码，未做登录后实点。

T57 增量执行结果：Node v20.19.5 上述 6 个 Vitest 文件 68 项通过。触达文件 ESLint 无输出。`localhost:3000` 可达但设计器需验证码登录，未实点。本轮未 commit。

## T57 增量：flow.history.signature 资源加载

- 设计器示例 IMAGE 改为协议允许的 PNG data URL，预览不再因 SVG 在 `flow.history[0].signature` 失败。
- 下载签名按文件头识别 PNG/JPEG/WEBP；空签名跳过。
- 自动化：`designerSample`/`resources`/`printResourceLoader`/`tablePagination`；触达 ESLint；`git diff --check`。
- 跳过：登录验证码，未做实点预览。

T57 增量执行结果：Node v20.19.5 上述相关 Vitest 通过（resources 8 项含示例签名与 octet-stream）。本轮未 commit。

## T57 增量：空白表格表头背景 / 字号下拉 / 元素透明度

- 空白表格「样式 → 表头背景」写 `headerStyle` 并清掉第一行默认白/灰底；`staticTableCellLook` 对 `#fff`/`#f1f5f9` 让位给表头底色。
- 属性面板字号（表头/表体/正文/列/最小字号）改为预设 pt 下拉，不再用 `NInputNumber`。
- 横竖线默认黑色 0.5mm，用 CSS border 画实线/虚线/点线；带 mm 的字段改为 `NSelect`。
- 协议 `style.opacity` 0–1，设计器「样式」百分比下拉；画布与预览/打印外框生效（含图片）。
- 自动化：`staticTable`/`PrintDesigner`/`protocol`/`renderers`/`stores/print` 共 6 文件 60 项；触达 ESLint；`git diff --check`；Java `PrintProtocolValidatorTest` 40 + `PrintProtocolCompatibilityTest` 42。
- 跳过：设计器需登录验证码，未做登录后实点；未启动 Admin/Flow/MySQL。

T57 增量执行结果：Node v20.20.0 上述 6 个 Vitest 文件 60 项通过。触达 ESLint 与 `git diff --check` 无输出。Java 17 协议测试 82 项通过。本轮未 commit。

## T57 增量：字体栈回退 / STHeiti 未安装不拦截预览

- `requireLocalFont` 按整串字体栈检查；任一具名字体可用，或存在 `sans-serif`/`serif` 回退，即通过。
- 设计器中文字体写成 Windows/macOS 回退栈（华文黑体不再把 `STHeiti` 放第一位）。
- 自动化：`resources.spec` 11 项 + `protocol.spec` 15 项；触达 ESLint；`git diff --check`。
- 跳过：登录验证码，未做实点预览。

T57 增量执行结果：Node v20.20.0 上述 2 个 Vitest 文件 26 项通过。本轮未 commit。

## T57 增量：style 原样入库 / 透明度滑块 / 单元格快捷面板 / 表格四边改行列

- 元素 `style` 允许安全的基础类型额外键（含 `opacity`）入库；仍拒绝 `backgroundImage`/`url()`。Java `Style` 忽略未知字段以免模型转换失败。
- 设计器透明度改为 0–100% 滑块。
- 选中空白表格单元格时快捷面板跟随单元格包围盒；表格去掉 overlay 八向锚点，四边补首末行列手柄。
- 自动化：`protocol` / `staticTable` / `history` / `PrintDesigner`；触达 ESLint；`git diff --check`；Java `PrintProtocolValidatorTest`。
- 跳过：登录验证码未实点；Java 改动需用户安装插件并重启 Admin 后才生效。

T57 增量执行结果：Node v20.20.0 `protocol` 15 + `staticTable` 10 + `history` 25 + `PrintDesigner` 15，共 4 文件 65 项通过。触达文件 ESLint 与 `git diff --check` 无输出。Java 17 `PrintProtocolValidatorTest` 41 项通过。未启动 Admin；插件改动需安装后重启 Admin 才生效。本轮未 commit。
