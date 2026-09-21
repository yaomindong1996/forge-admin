# 执行记录

> 前序提案及 M1 实现误落在 `/Users/mini32g/Desktop/project/lawhub`。下方原始记录保留真实路径和结果，不作为 forge-admin 仓库复验结果。2026-09-18 用户纠正目标为 `/Users/mini32g/Desktop/project/forge-admin`；后续以文末纠正记录为准。

## 2026-09-18：SDD 提案与规划

### 输入与范围

- 用户明确要求参考 hiprint 自主实现打印，与现有 ForgeAdmin 低代码/流程结合，并按 SDD 先提案、规划任务、再编码。
- 创建 `forge-native-print` 独立变更，保留其它进行中变更。
- 本轮只产生六份文档：proposal.md、spec.md、design.md、tasks.md、test-spec.md、execution-log.md。
- 未修改业务前后端、package.json、POM、SQL、现有设计器或流程配置。

### 已读取规则与调查依据

- 根 AGENTS.md、code-copilot/AGENTS.md、用户偏好/踩坑索引（本会话已读）。
- `code-copilot/agents/copilot-prompt.md`：No Spec No Code、提案 HARD-GATE、/apply 前置条件。
- `code-copilot/rules/automated-testing-standard.md`、coding-style.md、前端 DESIGN.md。
- 流程开发 Skill 及低代码/采购示例参考（本会话前序分析已读）。
- 当前低代码字段/主子表详情、业务动作、流程表单上下文、历史签名、应用发布与协议快照实现。
- 现有 `lowcode-designer-unification` Spec/Tasks，避免与已有物料统一方向冲突。

### 命令与观察

1. `git status --short`：返回 `fatal: not a git repository`；根目录及前后端目录未发现 .git。未尝试初始化或自动提交。
2. `cat forge-docs/guide/sdd-workflow.md`：文件不存在。随后使用当前存在的 `code-copilot/agents/copilot-prompt.md` 与根规则作为 SDD 依据；未伪造或补写旧文档。
3. `rg --files` 搜索命令/模板：当前未找到项目内 `/propose`、`/apply` 命令文件，采用仓库已有 Spec/Tasks/Test/Log 结构并补 proposal/design。
4. `wc -l`：AiCrudPage.vue 6785 行、todo.vue 2465 行、FlowReadonlyFormPanel.vue 423 行、应用工作台入口 534 行；随后确认 FlowTaskDetailShell.vue 824 行。任务中登记公共入口复用/必要拆分条件。
5. package.json：当前 Vue 3/Naive UI/Pinia/Vite 8，已有 jsbarcode/qrcode-vue3；本轮未安装依赖。
6. 根 POM/插件 POM：打印插件尚不存在，design 中新增目录/类均标为拟实施；避免把未来接口描述为现状。

### 方案落点

- 首期自主协议/排版/设计器、模板版本、主子表与流程单据浏览器打印。
- 使用当前已保存授权数据；历史业务快照/后台归档/静默打印另列后续。
- 模板版本随应用发布固定；业务权限、节点字段与子表列权限在服务端落实。
- 新增权限与模板状态明确标记人工审查，暂未进入 /apply。

### 验证结果

首轮执行临时 Python 文档检查：本地链接、任务 ID/依赖和 15 处现状引用路径均通过；`git diff --no-index --check -- /dev/null <文档>` 发现 Markdown 双空格换行被计为 trailing whitespace。已改为显式引用空行；同时修正检查器对 no-index 退出码 1（文件存在差异）的判断，只有空白诊断输出/异常退出码视为失败。

最终复查通过（临时 Python 检查，退出码 0）：

```text
Documents: 6
Tasks: 51 total; 46 implementation/verification; 2 completed documentation tasks
Requirements covered: 16/16
Dependency graph: 40 task rows, no cycles
PASS: local links, whitespace, IDs, dependencies, task status and requirement coverage
```

- 六份 Markdown 的本地链接存在，逐文件 no-index whitespace 检查无诊断。
- 任务 ID 唯一、依赖目标存在、已列依赖无环；运行 DTO 前置顺序固定为 T25 → T27a → T26 → T27b。
- 只勾选 D00/D01 文档任务；D02 审查和所有编码/验收任务均未勾选。
- F01–F16 均有测试矩阵对应项；现状证据 15 个文件路径在首轮检查中均存在。
- 为代码 Review 和最终联调保留独立任务；不把本轮文档检查当成实现或业务验收。

未执行构建/业务测试，原因：本轮只有提案文档，首期实现尚未开始。

### 环境与清理

- 未启动任何服务、浏览器或打印会话；无本轮服务需要清理。
- 未连接数据库、Redis，未执行 Flyway；未修改桌面 hiprint 参考仓库。
- 未提交、推送或变更其它项目文件。

### 下一步

完成文档一致性校验，交付具体首期提案与任务供审查。收到首期范围与权限/发布方案确认后，将 Spec 状态更新为 implementing，再从 D02/T01 开始执行；不得提前勾选代码任务。

## 2026-09-18：开始 /apply，M1 增量实施

- 用户确认开始编码并要求分阶段 commit、不 push，D02 已完成，Spec 状态 implementing。
- 核对根、UI、Server 目录均无 `.git`。已异步询问在当前目录初始化本地 Git 或提供已有仓库路径，尚未收到回复；未初始化、未 commit、未 push。
- 开始前六份文档备份到 `/private/tmp/forge-native-print-20260918-pre-apply/`。
- Node 偏好 v20.19.0 当前未安装，已核实实际可用 v24.21.0，未安装依赖或替换 node_modules。
- T01/T02：协议白名单、毫米单位、受控路径、金额定点转换与目录校验；T03/T04：共用纯文本/图片/形状/表格/编码渲染器。
- T05：字体/鉴权图片/编码准备、10 秒时限、取消和 URL 释放；session 缓存、真实 DOM 测量。
- T06/T07：有序区块、文本按行续页、表格整行分页/重复表头/末尾合计/页码及保护上限。
- T08：不可变分页树、隔离 iframe、打印对话框事件、预览取消和错误展示；浏览器验证进行中。

### RED / GREEN 实际记录

- T01、T02、T05、T06/T07、T08 均先新增目标测试，再执行得到缺少实现模块的 RED，随后实现并复跑。
- T01 首轮 9/10 通过，毫米换算浮点精度不适合严格等号，改为 10 位容差后 10/10。
- T02 累计 16/16；渲染行为累计 18/18；资源失败清理回归修正后分页累计 32/32。
- 22:58 本机执行 `node node_modules/vitest/vitest.mjs run src/components/print`：7 文件、35 项全部通过，耗时 801ms。
- ESLint 初轮发现单行多语句，按规范分行修正；最终复查待追加。
- 未执行真实业务 API、数据库、Flyway、Flow 联调；未将这些测试标记为通过。

### M1 阶段出口结果

- 最终单测命令：工作目录 `lawhub-admin-ui`，`source ~/.nvm/nvm.sh && nvm use v24.21.0 && node node_modules/vitest/vitest.mjs run src/components/print`。
- 23:10 结果：8 文件、39 项通过。新增用例覆盖预览卸载过程中打印会话完成、准备完成后取消、显式字体不可用和连续 keepWithNext。
- 最终 ESLint：`node node_modules/eslint/bin/eslint.js src/components/print`，退出码 0，无错误/警告。此前最后一个测试回调单行多语句已分行修正。
- 主项目构建：`node --max-old-space-size=8192 node_modules/vite/bin/vite.js build`，退出码 0，9365 模块，38.09s。现有 Vite native config 导入提示和两条存量 CSS // 注释警告保留，未扩大修改范围。日志 `/private/tmp/forge-print-m1-build.log`。
- 独立打印验证入口构建：根目录 `node code-copilot/changes/forge-native-print/verification/serve.mjs --build`，退出码 0，最终 2867 模块，339ms。此入口明确覆盖尚未接入主应用路由的 M1 源码，不能用主应用构建代替。日志 `/private/tmp/forge-print-m1-verification-build.log`。安装依赖实际运行版本为 Vite 8.2.1，package.json 范围为 ^8.0.10。
- 合成验证服务只监听 127.0.0.1:4318；sandbox 初次监听被 EPERM 拦截，经工具授权成功启动，没有连接真实后端。
- Chromium 153：0/10/100/500 行得到 1/1/4/17 页，无明细重复、遗漏、顺序错误、单元格溢出或正文越界。100/500 行独立打印文档为 4/17 页，500 行检查 1566 单元格、3 图片，释放后 iframe 数量为 0。
- 缺少字体、损坏图片、超高明细分别得到 FONT_UNAVAILABLE、RESOURCE_FAILED、ELEMENT_TOO_TALL，打印被阻止；恢复正常数据可重新准备。
- 已查看 10 行预览截图；实际点击打印按钮收到 `{result:DIALOG_OPENED,pageCount:1}`，只作为客户端调用事件，不等同系统对话框已显示、保存 PDF 或物理出纸。
- 证据：`verification/browser-results.json`；复跑步骤：`verification/README.md`。
- T01–T08 本阶段完成；M2–M6 未开始。没有修改 package.json、lockfile、后端、SQL 或低代码/流程业务入口。
- 同工作区存在近期其它法律业务文件变动，非本次修改；没有加入任何提交范围。由于无 Git 元数据，不能据此重建或声称拥有其变更历史。
- 阶段 commit 仍待用户确认初始化当前 Git 或提供正确仓库位置。未初始化仓库，未 commit，未设置远程，未 push。
- 已关闭本次浏览器页并停止本次 Vite 进程（Ctrl+C，退出码 130）；未停止其它服务。验证入口 cacheDir 改为 `/private/tmp/forge-print-verification-cache`，清理本轮生成的 verification/.vite 缓存，避免提交第三方预构建产物。
- 空白检查首轮误包含 Vite 缓存中的第三方 CSS 字符串，清理后按实际源码/文档复查；最大新增 SFC 为 PrintPreview.vue 182 行。

## 2026-09-18：纠正目标仓库至 forge-admin

- 用户明确指出目标为 `/Users/mini32g/Desktop/project/forge-admin/`。前轮按会话 cwd 写入 LawHub 是助手目录判断错误。
- Forge 已有 Git，初始分支 main，初始工作区仅 `.DS_Store` 有变动；不初始化 Git、不将该既有改动纳入提交。
- 只复制本次新增的 `src/components/print/` 与 `code-copilot/changes/forge-native-print/`，目标两个目录此前均不存在。其余 LawHub 文件不迁入。
- 当前设计/任务/验收命令调整为 forge-admin-ui、forge-server 和 com.mdframe.forge；前序原始执行记录保留，避免把来源仓库结果当成目标仓库结果。
- 复验、阶段提交与来源目录清理结果待追加；本轮继续遵守不 push。

### Forge 目标仓库复验与提交

- 分支：`codex/forge-native-print`，从原 main 建立；SDD 文档提交 `bc72aafc`（6 文件）。
- 本轮读取目标 AGENTS.md、code-copilot/AGENTS.md、DESIGN.md、用户偏好、测试标准和 SDD 规则，核验 Spec 中目标目录与主子表/设计预览/动作接口实际存在。
- 在 `forge-admin-ui` 运行 `source ~/.nvm/nvm.sh && nvm use v24.21.0 && node node_modules/vitest/vitest.mjs run src/components/print`：8 文件 39 项通过，23:23 执行，1.32s。
- 同目录 `node node_modules/eslint/bin/eslint.js src/components/print`：退出码 0，无警告。
- 同目录 `node --max-old-space-size=8192 node_modules/vite/bin/vite.js build`：退出码 0；原有 Vite 导入兼容提示与 CSS 注释警告未扩大修改。日志 `/private/tmp/forge-native-print-target-build.log`。
- 根目录 `node code-copilot/changes/forge-native-print/verification/serve.mjs --build`：退出码 0，产物位于临时目录；日志 `/private/tmp/forge-native-print-target-verification-build.log`。
- 本轮验证服务仅监听 127.0.0.1:4318；没有启动 Admin/Flow/数据库。Chromium 153 的目标复验证据写入 `verification/browser-results-forge-admin.json`。
- 源码 36 文件逐字节迁移；验证脚本路径与设计中的目录/包名按目标工程修正。目标依赖文件、lockfile、现有页面、后端及迁移均未修改。
- M1 阶段提交覆盖打印模块、合成验证入口和更新后的 SDD 进度，`.DS_Store` 不进入提交；M2–M6 仍未开始。

- 迁移后逐字节核对 36 个源文件完全一致；误写的 LawHub 打印模块和 SDD 目录已移至 `/private/tmp/forge-native-print-misplaced-backup-20260918/{print,sdd}`，不再留在 LawHub 活跃源码/变更目录中，未修改其它 LawHub 文件。
- 已关闭本次 Forge 浏览器标签并 Ctrl+C 停止本次 Vite 进程（退出码 130）。目标主项目构建为 9275 模块、37.52s；独立打印构建为 2867 模块、1.70s。
- 最终提交前执行 `git diff --cached --check`，只允许 `forge-admin-ui/src/components/print/` 和当前 SDD 目录进入 M1 提交。

## 2026-09-18 23:35 — 2026-09-19 00:05：M2 原生打印设计器

### 范围与实现

- 目标始终为 `/Users/mini32g/Desktop/project/forge-admin`，分支 `codex/forge-native-print`；延续用户“只 commit、不 push”。未修改 LawHub，未将既有 `.DS_Store` 加入提交。
- 编码前读取目标仓库规范、DESIGN、用户偏好和测试标准，复用 M1 验证基线，在 tasks 中细分 T10/T12/T13，明确 M2 本地草稿边界。
- T09–T13：Pinia 状态、受限历史、缩放拖动、组合边界、框选/多选、复制/粘贴、删除/对齐、物料/字段拖入、区块排序、纸张/元素/表格属性、多行合并表头和合计、工作台、草稿/协议往返、未保存离开保护。
- 新增 SFC 最大 277 行；共享设计状态均在 Pinia，面板无多层 props/emit 透传。没有引入 hiprint 代码/依赖，没有修改 package.json 或 lockfile。
- M2 保存明确为本地模板草稿；真实服务端版本和低代码/流程 Provider 均未实现，不把合成演示视为业务接入。

### RED / GREEN 和复查修复

- 23:36 先写 history.spec，因未实现 store 得到 RED；实现后 8 项通过。首轮测试 import 多退一级，同时纠正为实际 src/stores 路径。
- 23:46 工作台行为测试先得到缺少 PrintDesigner/draftStorage 的 RED。实现时修复 Vue 插值中嵌套双花括号引发的模板解析错误；13 项增量测试通过。
- 后续补框选/键盘、非法纸张调整、字段失效、存储配额失败、表头合并拆分与合计撤销。
- 00:00 复查新增“正文区块 ID 等于 header”行为测试，实际失败；正文编辑键改为 section:<id> 后通过，协议内容不变。
- 独立验证入口首次误用 Pinia 物理路径与别名混合加载，出现两个 Pinia 实例；统一通过别名导入后重新加载验证通过。此问题只在新验证入口，不涉及现有主应用。
- 修复分数坐标在边界四舍五入后的潜在越界；缩放手势遵循鼠标开始时比例。预览弹窗限制内容区高度并独立滚动。

### 最终验证命令与结果

工作目录 `/Users/mini32g/Desktop/project/forge-admin/forge-admin-ui`，均先执行 `source ~/.nvm/nvm.sh && nvm use v24.21.0`：

1. `node node_modules/vitest/vitest.mjs run src/components/print`：00:01，10 文件 58 项通过，1.95s；包含 M1 39 项和 M2 19 项。
2. `node node_modules/eslint/bin/eslint.js src/components/print/designer src/stores/print src/views/print --fix`：退出码 0，最终无诊断；提交前再只读检查整个打印范围。
3. `node --max-old-space-size=8192 node_modules/vite/bin/vite.js build`：最终退出码 0，9339 模块，39.53s；日志 `/private/tmp/forge-print-m2-final-build.log`。现有 Vite native config 提示和存量 CSS 注释警告仍在，未修改无关文件。
4. 根目录 `node code-copilot/changes/forge-native-print/verification/serve.mjs --build`：退出码 0，2909 模块，353ms，临时产物。合成入口把整个组件栈打在一起，出现 chunk >500KB 提示；主应用使用页面分包，未为该验证提示调整业务构建。日志 `/private/tmp/forge-print-m2-verification-build.log`。

### 浏览器阶段证据

- `http://127.0.0.1:4318/?designer`，Chromium，合成 100 行数据。
- 80% 缩放拖动 60/24 px → x/y 19.844/7.938 mm；一条历史，撤销恢复原位。
- Shift 选中两项，方向键让 x 从 0/80 变为 1/81；框选实际选中 barcode/qrcode/synthetic-image 三项。
- 50% 缩放调整二维码 19 px：宽 25→35.054 mm，高度被区块限制到 30 mm；撤销恢复 25×25。
- 主字段拖入固定区块自动 FIELD 绑定，超出落点被夹到 x=145/y=20；复制生成新 ID，偏移 3 mm；恢复本地草稿保持先前位置。
- 新建时出现未保存提示，“继续编辑”保留模板；同一模板 JSON 校验导入后 dirty=false、无新增历史。切换失效字段禁用预览并定位对应表格，恢复目录后可预览。
- 100 行真实预览 4 页、3 图片 complete=true 且自然尺寸非零；与 M1 使用同一 PrintPreview。纸张保持白底，明暗主题、1280/960 px 宽窗口已目视截图检查。
- 添加表头行成功、区块上/下移动及两步撤销恢复原顺序；最后修改后的内部区块键选择与导入往返再次验证通过。
- 浏览器工具虚拟剪贴板阻止直接模拟粘贴快捷键，未将这次工具调用记为通过；浏览器的复制/粘贴按钮与单测复制 ID/撤销路径通过。没有操作用户系统剪贴板。
- 结构化证据 `verification/browser-results-m2.json`；截图在本次工具记录中已查看，没有伪造截图文件。

### 清理与未执行项

- 本次仅启动验证 Vite，PID 45377，监听 127.0.0.1:4318。最终 Ctrl+C 退出码 130；浏览器标签已关闭，临时 viewport 已 reset。
- 未启动 Admin/Flow/MySQL/Redis，未执行 Flyway、真实接口/权限/业务流程 E2E、保存 PDF 或物理打印。
- M2 完成阶段出口，M3–M6 保持未开始；新打印功能还不能宣称已接入真实业务。
- 提交只包含本阶段打印源码/测试与对应 SDD 文档、合成验证入口；commit 标题为 `[forge-native-print] 完成 M2 原生打印设计器与草稿编辑`，不 push。

## 2026-09-19：M3a 后端持久化与协议验证

### 范围与阶段拆分

- 正确目录 `/Users/mini32g/Desktop/project/forge-admin`，分支 `codex/forge-native-print`；原 `.DS_Store` 不改动、不提交。只做本地 commit，未 push。
- 执行前复用根 AGENTS、code-copilot/AGENTS、preferences、pitfalls/backend、automated-testing-standard、当前 SDD；应用 `.agents/skills/forge-codegen-crud/SKILL.md` 的实体、SQL、字典权限约定。先补 T14 的 BOM 文件与 T21 拆分，再编码。
- M3a 单独交付 T14–T21；M3b T22–T28 仍待实现。没有 Controller、Provider 运行接口或真实前端保存，不将本阶段写成全部 M3 完成。
- POM 单向 generator → print → 技术 starter，Admin 显式聚合；打印插件只有技术依赖，H2 为 test scope。没有 hiprint 源码/依赖或新的生产端打印库。
- 原实现使用 V1.0.168 建四表、V1.0.169 建 5 个 sys_print_* 字典和四项权限；T54 发现并行流程分支占用相同版本后，打印迁移整体顺延为 V1.0.171/V1.0.172。不向角色自动授权、不创建未完成的列表菜单。
- Mapper 使用明确租户/逻辑删除条件、CAS 修订号、模板行锁、版本归属验证与审计 actor 限定；版本仅 insert/select，历史最大版本号包含删除记录以保持永久唯一。默认绑定应用行锁和应用快照引用保护留 T24/T22 的服务/SPI 完成。
- 文档模型与验证器拆成 8 个小类；统一技术限制，UTF-8 1MiB/深度/重复 JSON 键/尾随 JSON/字段与样式白名单/图片来源/几何/表格跨度；输出规范化 JSON 与 SHA-256，不执行表达式。失败诊断不记录原始模板值或 parser 原文。

### RED / GREEN 与修复

- 先写 PrintProtocolValidatorTest/合成模板；在实现缺失时用 Java 17 编译 PrintProtocolSmokeTest，确实报 PrintProtocolValidator 不存在（3 个错误，退出码 1），记录 `/private/tmp/forge-print-protocol-red.log`；再实现验证器。
- 首次协议编译发现 Java 正则字符串转义错误，修正后独立 javac 通过；未将此轮失败当成功。
- 冷缓存 Maven 构建启动后才加入 H2 测试依赖，首次 reactor 使用旧 POM 快照，testCompile 缺 org.h2.jdbcx；重新执行完整命令加载当前 POM 后通过。独立 JUnit 先后 34/43 项通过，最终以正式 Maven 的 72 项报告为准。
- 审查补回发布事务 rollback、错模板版本、绑定修订失效、全局元素/UTF-8/内联图片限制和 29 组共享兼容样例；按 coding-style 用 AST 给所有新增 Java 控制语句补齐大括号，再复跑测试。
- 首次 Admin `-T 2` 聚合因 Maven resolver `Could not acquire lock(s)` 失败；串行重试成功，不归因为打印源码。没有为工具故障更改产品代码、POM 版本或降低测试断言。

### 实际验证

工具：本机无可用 Java/Maven，临时下载 [Adoptium Java 17](https://adoptium.net/installation/archives) 与 [Apache Maven 3.9.9](https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/)，校验 SHA-256/SHA-512 后使用。JDK 17.0.20.1+1，Maven 3.9.9；均位于 `/private/tmp/forge-print-toolchain`，未安装系统软件或改 shell 配置。初期下载超时后续传完成，备用 Corretto 下载已停止。

后端工作目录 `forge-server`，先 `source /private/tmp/forge-print-toolchain/env.sh`；临时 settings 仅使用 Maven Central 和 `/private/tmp/forge-print-maven-repository` 隔离缓存：

1. `mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest='Print*Test' -Dsurefire.failIfNoSpecifiedTests=false`：最终 BUILD SUCCESS，72 tests，0 failures/errors/skipped。日志 `/private/tmp/forge-print-m3-tests-final.log`。
2. `mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -e -pl forge-admin-server -am package -DskipTests`：串行 46 模块 BUILD SUCCESS，含 Print、Generator、Admin，耗时见结构化证据。日志 `/private/tmp/forge-print-m3-admin-package-serial.log`。Package 按用户偏好跳过测试，不能用它替代第 1 项。
3. 根目录 Node v24.21.0 执行 `node code-copilot/changes/forge-native-print/verification/protocol-compatibility.mjs`：前端实际协议 bundle 验证 29/29，临时 bundle 已自动清理；未启动端口/浏览器。共享样例来自插件 test/resources/print。
4. XML/POM 静态解析、迁移版本唯一、Flyway placeholder、Git whitespace 检查通过；提交前再次核对 staged allowlist。

已有 auth 两处 Lombok @Builder 默认值警告保留，未扩大改动。H2 用 MySQL 模式执行同一 DDL，去掉 ENGINE/CHARSET/COLLATE；证明 SQL 边界行为，不代表 MySQL、Flyway 或真实租户拦截器已验收。没有生成或提交包含环境属性的原始测试报告，只提交计数与检查结果。

### 两阶段自审与未执行项

- Spec 合规：T14–T21 与已补拆分匹配；低代码/流程权限、不可变应用引用、prepare/运行权限和前端 API 持久化仍保持未完成，不能通过本阶段绕开这些前置条件。
- 代码质量：复核 4 表索引、删除墓碑、版本永不复用、DTO 固定字段、SQL 无动态拼接、协议未知字段拒绝、异常隐私和资源边界；审查改动后的 72 项全部通过。
- 未启动 Admin/Flow/MySQL/Redis、未执行真实数据库迁移或接口 E2E，未重做 M1/M2 浏览器验证、PDF/打印机。没有修改 LawHub，也没有停止其它用户进程。
- 阶段提交名 `[forge-native-print] 完成 M3a 打印持久化与协议校验`；后续从 T22–T28 开始，不跳到真实业务接入。


## 2026-09-19：M3b 模板事务、授权编排与前端持久化

### 范围与规范

- 持续在 `/Users/mini32g/Desktop/project/forge-admin`、`codex/forge-native-print` 实施；未修改 LawHub，既有 `.DS_Store` 不纳入提交。用户仍授权分阶段 commit、禁止 push。
- 复用既有 Spec/tasks/test-spec/log、根及 code-copilot AGENTS、测试标准、UI DESIGN 与 Forge CRUD Skill；编码前补 T25/T22/T24/T26/T28 子任务和 SPI 契约。当前原生模块不使用生成器 POST-safe CRUD，遵循已审查 REST Spec。
- 模板草稿 CAS、复制、发布不可变版本、同内容重复发布复用版本、停用、逻辑删除及引用保护均落实。写入统一应用锁→模板锁；绑定默认项在空集合并发下也由应用行锁串行化。
- 设计依赖打印设计权限与应用/来源授权；运行仅依赖 print:execute 与 Provider 的应用/记录/场景授权。Provider 返回可信发布版本清单，不回退最新草稿或实时设计绑定。默认没有真实 Provider，503 拒绝。
- prepare 只返回模板使用且目录允许的字段。图片元素必须绑定 IMAGE 目录字段；资源别名归一 fileId 后再授权。数据单集合 500 行、单文本 100000 字符、JSON 流式限长 4MiB。流程授权结果必须带已解析 processRunId。打印审计仅记录 PREPARED/DIALOG_OPENED/FAILED 元数据；所有端点关闭请求/响应正文日志。
- 前端两个 Pinia store 管理异步版本和请求代次，服务端保存不落 localStorage；保存时的新编辑保留，409 不丢数据，关闭预览清理单据。模板/场景/状态展示使用字典，新增页面与面板均低于 800 行。
- 原 V1.0.170（T54 顺延为 V1.0.173）仅注册 3 个隐藏页面，NOT EXISTS 防重复、tenant_id=1，不自动给角色赋权。正式业务入口和应用发布集成仍在 M4。

### 测试与修复记录

- RED：PrintProviderRegistryTest 在 Registry 未实现时 testCompile 明确失败；实现后通过。后续逐项补事务、HTTP、前端竞态及本轮审查边界用例。
- 前端首次回归发现旧设计器测试仍点击服务端模式已移除的“新建”；按新行为断言按钮不存在并直接验证 canLeave，另外补只改名称的离开保护。首轮格式检查发现同一行多语句，按 AST 拆分后定向 lint 通过。
- 浏览器验证 mock 起初缺少 getDictData 导出，补齐字典缓存契约后刷新通过。首次合成样例因未安装宋体而正确禁用打印，正向 HTTP 样例改用 Arial 后渲染 1 页正常。
- 自审补图片字段类型检查，避免普通文本字段绕过运行资源授权；补资源别名、总输出大小流式限制、并发首个默认绑定与流程 run 身份测试。按 SDD 将分页入口统一为 `/print/templates/page`。
- 一次单独 Registry 命令误在仓库根执行，reactor 项目定位失败；更正工作目录 `forge-server` 后，包含真实 Spring 空 Provider 列表构造注入的完整 104 项测试通过。这次命令失败不计为通过。

### 最终验证命令与结果

后端先加载 `/private/tmp/forge-print-toolchain/env.sh`，Java 17 / Maven 3.9.9 及隔离 Maven 仓库沿用 M3a：

1. `mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest='Print*Test' -Dsurefire.failIfNoSpecifiedTests=false`：104 tests、0 failures/errors/skipped，BUILD SUCCESS。日志 `/private/tmp/forge-print-m3b-java.log`。
2. `mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-admin-server -am package -DskipTests`：46 模块成功；日志 `/private/tmp/forge-print-m3b-admin-build.log`。此命令不替代实际单测。
3. UI Node v24.21.0：`node node_modules/vitest/vitest.mjs run src/components/print src/stores/print/__tests__ src/api/__tests__/print.spec.js`：12 files、71 tests 全通过；分页路径最终修正后，`src/api/__tests__/print.spec.js` 2/2 再次通过。
4. 对全部本轮前端改动运行项目 ESLint，0 errors/warnings；日志 `/private/tmp/forge-print-m3b-eslint.log` 和最终 API 定向日志 `/private/tmp/forge-print-m3b-api-eslint.log`。
5. `node --max-old-space-size=8192 node_modules/vite/bin/vite.js build`：最终生产构建成功，保留既有 Rollup/Rolldown 性能/包体积提示，未关闭规则。日志 `/private/tmp/forge-print-m3b-ui-build.log`。
6. 浏览器实际执行合成 HTTP 场景，见 browser-results-m3b.json；真实 Service/Mapper/事务验证由 H2/MockMvc 完成，不混称真实业务 E2E。
7. Mapper XML、迁移版本唯一/V1.0.173 防重复与无自动角色授权、SFC 行数和 git diff --check 通过；结构化计数见 verification/m3b-results.json，不提交包含环境变量的原始 Surefire 报告。

### 两阶段自审、清理和交接

- Spec 合规：T22–T28 已完成源码及阶段出口；字段/版本/权限/审计和 UI 保存边界与设计一致。M4–M6 不勾选，整体 Spec 保持 implementing，不提前归档。
- 代码质量：无业务 Service 查询构造器、无 Controller Map 请求体、无服务循环依赖；租户/actor 来自服务端，版本和资源取自授权来源，写入 CAS 与事务回滚测试通过。新菜单不公开、不自动授予角色。
- 合成浏览器标签已关闭；本轮唯一验证服务器 127.0.0.1:4318 已 Ctrl+C（130）退出并确认无监听，没有修改 viewport。未停止其他用户进程。
- 未启动真实 Admin/Flow/MySQL/Redis，未执行 Flyway/真实鉴权加密/低代码或流程 E2E，未输出 PDF 或操作物理打印机。H2 并发结果不能替代 MySQL 锁与实际租户拦截器验收。
- 本阶段只做本地 commit，不 push；下一阶段从 M4 的真实应用授权、低代码 Provider 与发布快照集成开始。

## 2026-09-19 · M4a-1 页面身份兼容

用户要求新分支继续打印，随后指定去掉 codex 和 M4；当前分支 `forge-native-print`，从 882ff8e1 延续，未改动主分支、未 push。已有 .DS_Store 改动不纳入提交。

实际修改 PrintSourceRequest / PrintTemplateCreateDTO / PrintBindingQueryDTO、PrintTemplate / PrintBinding；原 V1.0.171（T54 顺延为 V1.0.174）扩展 page_id 为 VARCHAR(128)，数字 ID 的旧 source_key 保持一致。路由解析接受工作台 page_* 标识，拒绝路径和超限值。H2 夹具先建打印 V1.0.171 再运行 V1.0.174 对应 ALTER；MySQL information_schema/PREPARE 防重分支未在真实库执行。

验证：Java 17/Maven 3.9.9，既有 /private/tmp/forge-print-toolchain/env.sh 和 Maven settings，`-pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest='Print*Test' -Dsurefire.failIfNoSpecifiedTests=false`：106 项全部通过（包括 DTO→MockMvc→事务 Service→Mapper 的字符串页面身份）。前端 Node 24.21，`vitest run src/components/print src/stores/print`：12 文件 77 项通过；目标 ESLint 首轮提示正则风格，--fix 后通过；`node --max-old-space-size=8192 node_modules/vite/bin/vite.js build` 成功，46.44 秒，保留既有构建警告。未启动任何服务/未执行真实迁移。

M4a-2/3 正在实现，不将数据 Provider、应用发布快照生成或工作台入口标为完成。

## 2026-09-19 · M4a-2/3 应用权限与发布引用保护

落位：generator 新增 PrintApplicationAccessAdapter、PrintApplicationLock、PrintApplicationSnapshotCodec、PrintApplicationVersionGuard；扩展两组 Application Mapper/XML；BusinessApplicationVersionService 最终提交挂接引用守卫；PrintIdentity 提取 current() 供当前登录身份校验。新增 4 个测试类和 1 个合成数据工具，generator 增加 test-scope H2（无生产依赖新增）；既有 2 个应用版本/运行测试适配构造器。

结果与证据：
- `mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Penable-tests -Dtest='PrintApplication*Test,BusinessApplicationVersionServiceTest,BusinessApplicationRuntimeServiceTest,BusinessApplicationPhaseFiveSecurityTest' -Dsurefire.failIfNoSpecifiedTests=false`：30 项通过，28.726 秒。其中应用锁/版本事务 5 项、固定版本守卫 4 项、应用授权/历史引用 3 项、快照协议 4 项、既有应用版本/运行/快照安全 14 项。
- 前一轮 `-Dtest='Print*Test,...'` 中打印插件 106 项通过。新 current() 断言再按 `-Dtest=PrintIdentityTest` 增量复验 1 项通过。
- `mvn ... -pl forge-admin-server -am package -DskipTests`：46 模块 BUILD SUCCESS，29.232 秒。未启动 Admin。
- V1.0.174 静态检查：版本号唯一、两处 information_schema 防重、无 Flyway 业务占位符；git diff --check 通过。

验证中修正：新增守卫依赖后，旧 RuntimeService 测试子类 super 构造器遗漏参数导致 testCompile 失败，已适配；H2 合成模板最初漏写非空 create_by，导致 5 项夹具初始化失败，补齐 create_by/update_by/create_dept 后全部通过，未放宽表约束。

Spec 审查：本轮仅完成 M4a；应用候选快照生成、字段版本校验与真实 Provider 未提前勾选。旧应用快照缺少 printing 兼容为空；显式坏结构和未知版本拒绝。没有新增 Controller、角色授权、业务数据修改或流程动作。

代码审查：所有查询在 XML，tenant_id/del_flag 显式过滤；应用锁在前，模板锁在后；历史引用用锁定读，避免之前一致性读快照漏项；固定模板版本不读最新指针；错误不携带快照正文；没有 Service 循环依赖。H2 证明同一应用并发阻塞及事务回滚，MySQL 方言/隔离级别仍待真实环境验收。

本轮无新启动的服务。未运行真实迁移、MySQL、Redis、Admin、Flow；没有 push。既有 .DS_Store 保留。后续从 M4b（已发布模型目录、主子表权限/展示转换与候选快照贡献器）继续。

## 2026-09-19 · M4b 低代码真实数据适配与应用发布快照

用户“继续下阶段”授权下，仍在 `/Users/mini32g/Desktop/project/forge-admin` 的 `forge-native-print` 分支实施。先细分 M4b-1…5，再编码；工作目录 LawHub 没有被修改。原有 .DS_Store 保留，不纳入提交。复用既有 SDD/测试基线、Forge CRUD Skill 和自动化测试规范。

实现：9 个打印适配生产类；PrintBindingMapper/XML 增加应用锁内当前读；BusinessApplicationSnapshotService 生成 printing 候选清单，PrintApplicationVersionGuard 校验发布字段/资源；AiCrudConfigMapper/XML 增加当前启停/删除检查。DynamicCrudService 增加固定配置主子表读取与严格后处理；VirtualFormulaRuntime 增加禁用正文日志/执行 trace 且错误终止的打印入口，普通 CRUD 行为不变。新增 9 个行为测试类 + 1 个合成夹具，扩展既有真实 MyBatis/Spring/H2 事务测试及必要构造器适配。

验证命令（均在 forge-server，先 source /private/tmp/forge-print-toolchain/env.sh）：

```bash
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Penable-tests -Dtest='Print*Test,LowcodePrint*Test,DynamicCrudPrintReadTest,DynamicCrudMoneyValueTest,DynamicCrudCryptoLifecycleTest,DynamicCrudStructuredValueTest,VirtualFormula*Test,BusinessApplicationRuntimeServiceTest,BusinessApplicationPhaseFiveSecurityTest,BusinessApplicationVersionServiceTest' -Dsurefire.failIfNoSpecifiedTests=false
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Penable-tests -Dtest='PrintMetadataResolverTest,LowcodePrintDataProviderTest' -Dsurefire.failIfNoSpecifiedTests=false
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-admin-server -am package -DskipTests
```

结果：首条最终回归 print 106 / generator 79，失败 0；增加设计版本必须 PUBLISHED 的最后断言后，第二条 15/15 通过，含新增 1 项，累计不同用例 186 项。第三条 46 模块 BUILD SUCCESS，23.833 秒。git diff --check 通过；无前端更改，不重复 M4a 前端验证。聚合构建保留现有 SmsCaptchaResult/SliderCaptchaResult Lombok @Builder 初始化值警告，不在本轮扩大范围修复。

过程中修正的验证问题：子表合成模型追加外键时字符串换行导致夹具字段缺失，改为 JSON 节点追加；FileMetadata 必须用 builder；Mockito 默认返回空 Map，缺记录分支显式返回 null；公式夹具补齐 objectCode/tenantId。所有对应失败已复跑通过，没有放宽生产校验。复核又补上发布后的启停撤权、门户过滤空树不能重建隐藏页面、固定 manifest hash 对比、子表显示列过滤和严格公式执行。

Spec 审查：T29/T30/T33 后端阶段出口完成；T31/T32/T34、M5/M6 保留未完成。固定快照不含业务正文；运行数据只包含当前用户可读且模板使用的字段。字段类型变化按显式格式兼容性验证，通用文本仍支持兼容标量。真实流程打印仍拒绝。

代码审查：新增查询在 XML，显式 tenant/del_flag/status；绑定当前读及模板捕获遵循应用锁→模板锁。版本链固定到对象设计版本→CRUD 版本，不能选择最新草稿。文件复用下载权限并额外核验租户。服务依赖检查未发现反向环；聚合构建成功不替代真实 Spring 启动。没有启动 Admin/Flow/MySQL/Redis、运行迁移或改业务记录，没有本轮常驻进程需要清理，也没有 push。完整发布、MySQL 并发、加密 HTTP 与浏览器打印留真实环境验收。


## 2026-09-19 · M4c 工作台、业务打印动作与下载协议

继续在 `/Users/mini32g/Desktop/project/forge-admin`、`forge-native-print` 分支执行。先补 M4c-1…5 和增量验证计划，再编码；R01 核实无需修改 AiCrudPage，使用其已有 route/params/runtimeActions。沿用 Forge CRUD Skill、DESIGN.md 和自动化测试标准，LawHub 未改动，既有 .DS_Store 未提交。

落位：PrintWorkspaceSources/PrintWorkspaceStore/PrintSourceSelector、ApplicationPrintPanel，共享 PrintTemplateList；工作台导航/入口、PortalPageRenderer 与 render API 传递字符串 pageId；PrintRuntimeActionProjectionService 只投影发布绑定。PrintCodegenContributor + ApplicationVersion Mapper/XML 在统一 LowcodeProtocolSnapshotBuilder/Velocity 生成链携带固定定义、绑定和依赖；应用级清单覆盖聚合子对象，入口级导出限定应用，旧配置键入口亦走相同贡献器。新增 2 个后端测试类、3 个前端测试文件及可复跑合成验证入口。

最终后端命令（forge-server，先 source /private/tmp/forge-print-toolchain/env.sh）：

```bash
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Penable-tests -Dtest='Print*Test,LowcodePrint*Test,LowcodeProtocolSnapshotBuilderTest,GeneratedLowcodeRuntimeConfigBuilderTest,BusinessApplicationCodegenContractTest,BusinessApplicationRuntimeConfigOverlayServiceTest,BusinessApplicationRuntimeServiceTest,BusinessApplicationPhaseFiveSecurityTest,BusinessApplicationVersionServiceTest' -Dsurefire.failIfNoSpecifiedTests=false
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Penable-tests -Dtest='PrintCodegenContributorTest,LowcodeProtocolSnapshotBuilderTest,BusinessApplicationCodegenContractTest' -Dsurefire.failIfNoSpecifiedTests=false
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-admin-server -am package -DskipTests
```

结果：第一条 print 106 + generator 94，第二条 25 项（包括新增子对象完整性 1 项），累计 37 类 201 项，失败/跳过 0。最终 Admin 46 模块 BUILD SUCCESS，22.243 秒。真实 Velocity 生成包中 runtime-config/protocol/printing 一致，主前端使用 LowcodeRuntimePage；导出缺版本/hash/权限时失败。Mapper SQL 绑定参数及 JSqlParser 解析通过，MySQL 方言实跑未执行。

前端（forge-admin-ui，source ~/.nvm/nvm.sh 后 nvm use v24.21.0）：

```bash
node node_modules/vitest/vitest.mjs run src/components/print src/stores/print src/api/__tests__/print.spec.js src/api/__tests__/printRuntimeContext.spec.js src/components/ai-form/crud/__tests__/AiCrudPage-print.spec.js
node --max-old-space-size=8192 node_modules/vite/bin/vite.js build
```

16 文件 89 项通过，Vite 主构建 36.45 秒。全部修改的 JS/Vue 运行定向 ESLint，无错误/警告。首轮 lint 发现多语句行，已拆分；AiCrudPage 新测试最初缺少若干无关 Naive stub 的警告已补齐；未放宽生产断言。主项目保留现有 CSS 注释、Vite native config 导入和构建性能提示；合成入口单 chunk 体积警告不影响生产拆分。没有修复与本轮无关的构建提示。

浏览器：仓库根运行 `node code-copilot/changes/forge-native-print/verification/serve.mjs --workspace`，访问 `http://127.0.0.1:4318/workspace.html`。实际使用生产工作台/选择器/列表/设计器/预览组件，API 和用户权限为合成替身。验证两页面同对象选择、自动来源创建、全屏设计、亮暗主题、390px 和更多操作、列表/详情预览、无模板查看权空态。查看器日志无 error/warn。服务收到 page_archive 的创建请求，以及带同一特殊字符记录 ID 的 LIST/DETAIL available/prepare；不含记录正文。AiCrudPage 自身列表/详情点击由真实组件单测额外覆盖，未把合成页面当成真实门户 E2E。

独立验证入口 `serve.mjs --workspace --build` 通过。窗口视口已恢复，临时标签页关闭，验证服务 PID 92114 已停止；未启动 Admin/Flow/MySQL/Redis或执行迁移。未进行真实加密 HTTP、MySQL 并发读一致性、PDF 或物理打印。

Spec 审查：T31/T32/T34 完成阶段验收，M4 为 implemented-pending-e2e；M5 流程/代码业务、M6 完整验收未完成。下载依赖明确为 REQUIRES_TARGET_ADAPTER/REQUIRES_EXTENSION，不把保存打印 JSON 宣称为目标业务数据 Provider 已实现。代码审查：新增查询在 XML，tenant/del_flag/发布指针明确；源页面与对象逐层核验；row 只取记录主键、URLSearchParams 转义；只读导出权限不包含业务数据正文；没有新增全员授权、迁移或业务状态写入。修改的 SFC 最大 680 行，AiCrudPage 本体无变动。全程未 push。

## 2026-09-19 · M5a 流程身份、审批轨迹与采购 CODE Provider

仍在 `/Users/mini32g/Desktop/project/forge-admin` 的 `forge-native-print` 分支执行，只 commit、不 push；既有 `.DS_Store` 保留且不暂存。按 R02 先核对页面规模：`todo.vue` 2526 行、`started.vue` 869 行、`FlowTaskDetailShell.vue` 824 行，M5b 接入前必须拆分，M5a 未修改这些超限文件。

实现：FlowClient 增加实例历史分页读取；generator 增加 FlowPrintContextResolver/AccessPolicy/HistoryAdapter，并在 XML 通过 tenant_id + processInstanceId 解析应用业务 run。LOWCODE FLOW 场景要求 task/instance/run/record 完全一致，按待办/已办/我发起分别授权，节点显式隐藏字段与模板子集在服务端收敛。审批轨迹每条保留真实 taskId，最大 1000 条。流程 TaskFormInfo 返回 `printTemplatePolicy/printTemplateIds`，只读取 BPMN 节点属性，不修改任何审批动作。

采购 CODE Provider 只认 `sample_purchase_order` + `sample_purchase_order_approval_form`，复用 SamplePurchaseOrderService 读取当前保存数据，绑定只来自应用已发布快照；运行不依赖打印设计权限。字段目录排除内部标识和上传 fileId，并加入 `flow.history`。静态模板图片仍走现有私有文件授权。

验证（Java 17 / Maven 3.9.9，隔离 settings）：

```bash
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-business/forge-business-core -am compile -DskipTests
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-business/forge-business-core -am test -Penable-tests -Dtest=FlowPrintAccessPolicyTest,LowcodePrintDataProviderTest,SamplePurchaseOrderPrintDataProviderTest -Dsurefire.failIfNoSpecifiedTests=false
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile -DskipTests
```

结果：业务核心聚合 35 模块编译成功；定向测试 generator 11 项、business-core 2 项，共 13 项，0 failure/error/skipped；流程插件聚合 28 模块编译成功。`git diff --check` 通过。

过程中如实记录：首次把 `-Penable-tests` 与 `-DskipTests` 同时用于探测，profile 覆盖跳过设置并触发无关 system 旧测试路径失败；改用 compile 生命周期后通过。采购 Provider 首次编译出现 null 重载歧义，内部方法改名；新增测试首次有泛型断言编译错误和子表隐藏字段解析错误，分别收紧类型与按 childField 解析后复跑全绿，未放宽生产规则。

未启动 Admin/Flow/MySQL/Redis，未执行迁移或真实流程；Flow 受保护接口、数据库 run 映射与实际历史数据的端到端结果留用户环境验收。M5b 前端入口与 BPMN 编辑器策略、M5c 资源加载/审计尚未完成。

## 2026-09-19 · M5b 流程入口与 BPMN 节点模板策略

继续在 `/Users/mini32g/Desktop/project/forge-admin` 的 `forge-native-print` 分支执行，只 commit、不 push；既有 `.DS_Store` 保留且不暂存。先按 R02 迁出详情页作用域样式：`todo.vue` 从 2526 降至 1944 行、`started.vue` 从 869 降至 568 行、`FlowTaskDetailShell.vue` 从 824 降至 225 行；`done.vue` 为 606 行。样式迁移不改变模板和脚本行为。

实现：新增 `flowPrintContextStore` 与统一 `FlowPrintAction`，待办/已办/我发起详情工具栏复用同一模板选择器。Store 只组装稳定字符串身份，不保留 row/表单正文；低代码要求 processRunId，切换任务用 generation 丢弃旧回包。待办对业务表单和动态表单保存快照做 dirty 检查，用户明确选择后才打印已保存数据。业务键兜底只提取匹配 objectCode 的记录段，并保留超出 JS 安全整数范围的字符串。

后端 BusinessTaskFormContext 增加 processRunId；查询上下文优先采用 `ai_business_process_run` 固定的 applicationId/runId。历史 CODE 流程没有 run 时，通过 Mapper XML 查业务对象唯一归属的启用且已发布应用；多应用不猜测，保持空身份并返回告警。该补充只暴露身份，不把业务正文放入打印请求，也不改变审批保存和办理动作。

流程设计器审批节点新增打印策略分区。INHERIT 沿用应用场景发布绑定且不写冗余属性；RESTRICT 只保留当前来源模板子集，空子集代表节点无模板。解析/写回使用 `flowable:printTemplatePolicy` 与 `flowable:printTemplateIds`，未知策略按 INHERIT，模板 ID 去重；现有审批属性与动作不变。

验证：

```bash
# forge-admin-ui，直接使用现有 node_modules，避免 pnpm 包装器再次安装依赖
./node_modules/.bin/vitest run src/stores/print/__tests__/flowPrintContextStore.spec.js src/components/flow/__tests__/FlowPrintAction.spec.js src/components/flow-designer/converter/__tests__/user-task-parser-print.spec.js src/components/flow-designer/converter/__tests__/roundtrip.spec.js src/components/flow-designer/converter/__tests__/json-to-bpmn.spec.js src/components/flow-designer/panel/__tests__/PrintTemplatePolicyConfig.spec.js src/components/flow-designer/panel/__tests__/ApproverConfig.spec.js
./node_modules/.bin/eslint <本阶段变更的 16 个 JS/Vue 文件>
node --max_old_space_size=4096 ./node_modules/vite/bin/vite.js build

# forge-server，JAVA_HOME/PATH 指向 /private/tmp/forge-print-toolchain
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Penable-tests -Dtest=BusinessFlowServiceBusinessKeyTest,BusinessFlowServiceFormAssetMergeTest,BusinessFlowServicePrintIdentityTest,FlowPrintAccessPolicyTest -Dsurefire.failIfNoSpecifiedTests=false
xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessApplicationObjectMapper.xml
```

结果：前端 7 文件 40 项全部通过，定向 ESLint 无输出，Vite 9363 modules 生产构建成功；保留项目既有 Vite native config、CSS `//` 注释、dynamic import 与插件耗时提示。后端 33 模块 BUILD SUCCESS，4 类 22 项测试全部通过；其中新增规范身份 3 项覆盖 run 固定、唯一已发布应用回退和多应用拒绝。Mapper XML 解析及 `git diff --check` 通过。

过程中修正：前端组件测试最初依赖 Naive Teleport/组件内部 DOM，改为断言组件公开状态与 emit；移除无 Dialog Provider 时的原生 confirm，保持项目消息交互。pnpm 包装器尝试安装依赖时因 ignored-builds 退出并写入 workspace 占位行，已完整恢复该非业务改动，随后全部命令直接复用现有 node_modules。后端首次探测因 shell 未暴露 Maven/JDK，改用已缓存且前阶段验证过的 Java 17/Maven 3.9.9 工具链。

未启动 Admin/Flow/MySQL/Redis，未执行迁移或真实流程，没有新常驻服务。真实待办/已办/我发起、应用发布后的采购 CODE 模板范围、PDF/物理打印仍留 T44；M5c 的鉴权资源加载和执行事件收口尚未完成。

## 2026-09-19 · M5c 鉴权资源、流程签名与审计收口

继续在 `/Users/mini32g/Desktop/project/forge-admin` 的 `forge-native-print` 分支执行，只 commit、不 push；既有 `.DS_Store` 保留且不暂存。代码开发完成后由用户执行真实环境验证，本阶段未启动 Admin/Flow/MySQL/Redis、未执行迁移、PDF 或物理打印。

实现：新增 `runtime/printResourceLoader.js` 作为运行时资源错误边界，复用既有鉴权 HTTP Blob 下载、10 秒总超时、AbortSignal、图片类型/10MiB 限制和 Blob URL 清理。资源取消映射为 `PRINT_CANCELLED`，非法、缺少解析器或未就绪资源映射为 `RESOURCE_FAILED`，不把底层响应或签名内容写入审计。`PrintPreview` 在资源全部完成后才测量和分页。

补齐流程签名的实际纸面路径：资源引擎按字段目录识别 TABLE 的 IMAGE 列，从 `flow.history` 逐行读取 fileId，经同一鉴权加载器下载并解码；准备层生成图片单元格并按图片高度参与分页，渲染器输出 Blob 图片，不再把签名 fileId 当文本打印。服务端既有投影会把 IMAGE 列加入文件授权集合，新增测试固定该约束。

审计收口：客户端仍只提交 `DIALOG_OPENED` 或 `FAILED`；FAILED 必须只有白名单 errorCode 且 pageCount 为空。响应新增固定为 false 的 `physicalOutputConfirmed`，说明浏览器调用打印对话框无法证明物理出纸。模板选择和预览准备不会记录 DIALOG_OPENED。

验证：

```bash
# forge-admin-ui
./node_modules/.bin/vitest run src/components/print src/stores/print src/api/__tests__/print.spec.js
./node_modules/.bin/eslint <本阶段 11 个前端源码/测试文件>
node --max_old_space_size=4096 ./node_modules/vite/bin/vite.js build

# forge-server，Java 17 / Maven 3.9.9 / 隔离 settings
mvn -s /private/tmp/forge-print-maven-settings.xml -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-print -am -Dtest='Print*' -Dsurefire.failIfNoSpecifiedTests=false test
mvn -s /private/tmp/forge-print-maven-settings.xml -pl forge-admin-server -am package -DskipTests
```

结果：前端打印域 16 文件 98 项、后端打印插件 17 类 107 项全部通过；最终合并回归进一步覆盖流程入口/BPMN 策略，前端 22 文件 133 项；后端打印插件 107 + generator 113 + 采购 CODE Provider 2，共 222 项，全部 0 failure/error/skipped。定向 ESLint 无输出；Vite 9364 modules 构建成功；Admin 46 模块 BUILD SUCCESS。第一次 Maven 探测未带 `-Penable-tests`，只编译未运行测试，不计入通过证据；启用 profile 后新增测试夹具最初仍保留模板静态资源，修正夹具清空 resources 后 7 项定向及完整回归均通过。项目既有 Vite native config、CSS `//` 注释、dynamic import、组件重复注册 stderr 和 Lombok builder 警告未扩大处理。

Spec 审查：T39 完成，M1–M5 代码闭环完成；M6 的 T40/T42 仍需真实环境浏览器/流程/PDF/打印机结果。代码审查：签名仍是受控 fileId，无 URL/token 写入模板或 iframe；资源失败先于打印会话；图片行高进入分页；审计无正文/异常堆栈且终态幂等。人工验收与回滚见 `verification/user-acceptance.md`。

## 2026-09-19 · T46 设计器视觉基线补强

用户查看合成工作台后明确反馈 `workspace.html` 容易被理解为正式页面，且设计器与参考源码差距明显，缺少标尺和辅助线。按 SDD 先更新 F03、4.4、T46 和增量测试计划，再修改生产组件；`vue-plugin-hiprint` 只用于观察交互结构和视觉基线，没有引入依赖、复制 bundle 或兼容其 JSON 协议。

实现：新增独立 `PrintRuler.vue`，按 5mm 刻度、10mm 主刻度渲染横纵毫米标尺；纸张画布增加 1mm/5mm 网格、页边距框、页眉/页脚红色辅助线，元素选中后显示横纵坐标线和毫米坐标。工具栏增加 A4/A5、横竖旋转、网格开关及缩放加减；左侧物料改为紧凑图标宫格。三栏桌面工作台保持固定结构，窄视口使用工作区横向滚动，不再把属性面板堆到画布下方。合成服务支持 `/app-center/application/purchase-demo?section=printing` 和 `/print/designer` 路由，`workspace.html` 仅保留为 Vite 内部入口。

验证：

```bash
./node_modules/.bin/vitest run src/components/print src/stores/print src/api/__tests__/print.spec.js
./node_modules/.bin/eslint src/components/print/designer/PrintRuler.vue src/components/print/designer/PrintCanvas.vue src/components/print/designer/PrintDesigner.vue src/components/print/designer/PrintDesignerToolbar.vue src/components/print/designer/PrintElementPalette.vue src/components/print/designer/__tests__/PrintRuler.spec.js src/stores/print/printDesignerStore.js
node --max_old_space_size=4096 ./node_modules/vite/bin/vite.js build
git diff --check
```

结果：打印域 17 文件 101 项全部通过；目标 ESLint 和 `git diff --check` 无输出；Vite 9366 modules 构建成功，只有项目既有 native config、CSS `//` 注释、dynamic import 与插件耗时警告。浏览器以 1440×900 桌面视口从应用工作台路由进入模板设计页，实际观察双向标尺、网格、纸张辅助线、三栏、物料区和选中元素坐标线；浏览器控制台 error/warn 为空，随后恢复默认视口并保留页面给用户查看。

第一次按 memory 中 Node 20.19.0 命令执行时发现本机当前未安装该版本，测试尚未启动；改用此前本变更已验证的 Node 24.21.0 后通过。未启动 Admin/Flow/MySQL/Redis，未执行 Flyway、PDF 或物理打印；用户要求查看的本地合成预览服务继续监听 `127.0.0.1:4318`，其余真实环境验收仍归 T42/T44。

## 2026-09-19 · T47 工作台收敛、动态定位线与文档式预览

用户第二轮查看后指出操作按钮过多、窄窗口布局混乱、定位线不是真实吸附以及最终预览效果不足。按 SDD 先新增 Spec 4.5、T47 和增量测试计划，再修改生产代码。继续对照本地 `vue-plugin-hiprint` 的紧凑命令组织、拖动辅助线和纸张视图，但使用 Forge 自有协议、Pinia 状态和 Vue 3 组件实现。

实现：顶部编辑操作收敛为单行图标命令栏，纸张和缩放使用选择器，新建/复制/恢复/协议进入“更多”；页面级场景绑定、版本和发布操作同步压缩。区块行只保留排序图标和行级菜单，属性区复制/粘贴/删除改为图标工具组。左右面板由 Pinia 管理并可独立收起，900px 以下自动收起并以覆盖面板打开，不再使用 1080px 固定工作区横向滚动。

拖动和缩放以毫米坐标计算吸附，候选包含内容边界、其它元素的左中右/上中下和 1mm 网格；只有手势中实际命中阈值才显示定位线和坐标，结束、取消、切换区块或加载模板立即清除。多选补齐左/中/右、顶/中/底六向对齐及水平/垂直等距分布。预览改为独立文档查看工作台，含页码导航、适合宽度、缩放加减、纸张页标与阴影层级；预览和正式打印继续复用同一布局结果。

验证：

```bash
./node_modules/.bin/vitest run src/components/print src/stores/print src/api/__tests__/print.spec.js src/api/__tests__/printRuntimeContext.spec.js
node node_modules/eslint/bin/eslint.js <本阶段 13 个前端源码/测试文件>
node --max_old_space_size=4096 ./node_modules/vite/bin/vite.js build
git diff --check
```

结果：18 个测试文件 108 项全部通过，定向 ESLint 和 `git diff --check` 无输出；Vite 9367 modules 构建成功。构建只有项目既有 native config、CSS `//` 注释、dynamic import 和插件耗时提示。所有触达 SFC 均低于 800 行。

浏览器在 `/print/designer?templateId=1` 实际核对：桌面工具栏保持单行且三栏清晰；默认窄窗口自动收起左右面板，画布完整显示，组件面板可作为覆盖层打开后再次收起；预览显示纸张、页数、页码导航、适宽和缩放操作。拖动吸附的瞬态状态由 Store/组件测试覆盖，松手后浏览器页面无残留定位线。合成预览服务继续监听 `127.0.0.1:4318` 并保留新版页面给用户查看。

本阶段未启动 Admin/Flow/MySQL/Redis，未执行 Flyway、真实业务数据、PDF 或物理打印；这些继续由用户按 T44 清单验收。全程只在 `forge-native-print` 分支 commit，不 push；既有 `.DS_Store` 未暂存。

## 2026-09-19 · T48 设计内容同源与基础能力补齐

用户第三轮检查指出画布暴露字段路径、中央命令少、物料少，并且设计画布与预览的正文和表格结构不同。重新逐项检查本地 `vue-plugin-hiprint` 的默认物料提供器、设计 Demo、属性配置和元素命令；按 SDD 先补 Spec 4.6 与 T48，再编码。HTML、任意脚本、静默打印、服务端 PDF 和 hiprint JSON 仍在排除范围。

实现：新增 `designerSample.js`，从授权字段目录生成稳定的非业务示例上下文，真实样本存在时不覆盖；Canvas 与 PrintPreview 使用同一 context。文本、页码、线框、矩形和新增椭圆复用正式 renderer，条码/二维码调用正式编码器；流式文本、表格合并表头、格式化数据行和表尾合计均使用与预览相同的值和样式规则。纸面正文从 `{{ main.number }}`、`main.remark`、`name/amount` 改为“单号示例、备注示例、名称1、1288.00”等输出内容，内部路径保留在字段树和绑定配置中。

物料由 7 项扩充为标题文本、固定文本、图片、横线、竖线、矩形、椭圆、条码、二维码、页码 10 项，另有固定区块、流式文本、明细表格 3 类内容区块。纸张补 A3/B4/B5，属性补斜体、下划线、背景色、边框色/宽和内边距。画布顶部新增全选、复制、粘贴、复制一份、置顶、置底和删除；命令进入 Pinia 与撤销历史。椭圆同步前后端 v1 协议白名单。

验证：

```bash
./node_modules/.bin/vitest run src/components/print src/stores/print src/api/__tests__/print.spec.js src/api/__tests__/printRuntimeContext.spec.js
node node_modules/eslint/bin/eslint.js <打印设计器、协议、renderer 和 Store 触达文件>
node code-copilot/changes/forge-native-print/verification/protocol-compatibility.mjs
source /private/tmp/forge-print-toolchain/env.sh
mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest=PrintProtocolValidatorTest -Dsurefire.failIfNoSpecifiedTests=false
node --max_old_space_size=4096 ./node_modules/vite/bin/vite.js build
git diff --check
```

结果：前端 19 文件 114 项、共享协议 29 项、Java 协议 34 项全部通过；ESLint 与空白检查无输出，Vite 9370 modules 构建成功。浏览器在 `/print/designer?templateId=1&ui=t47` 验证组件抽屉、字段示例、画布命令、合并表头/明细/合计和文档预览；设计与预览首屏数据和表格结构一致。验证中“复制一份”产生的临时修改已撤销，页面保持未修改状态并留给用户检查。

未启动真实 Admin/Flow/MySQL/Redis，未执行真实图片/签名、PDF 或物理打印。合成服务继续监听 `127.0.0.1:4318`。本阶段只 commit，不 push，`.DS_Store` 不暂存。

## 2026-09-19 · T49 元素变换、锁定与右键命令

用户继续指出元素级操作仍少、物料属性不足，设计与预览需要保持一致。继续对照本地 `vue-plugin-hiprint` 的默认元素、上下文菜单和属性选项，先更新 Spec 4.7、任务和增量测试计划，再实现 Forge 自有协议能力；未引入参考项目 bundle、HTML/脚本、静默客户端或 PDF 服务。

实现：v1 协议新增 `rotationDeg`、`flipX`、`flipY`、`locked`，样式新增 `objectFit`、`borderStyle`、`borderRadiusMm`，前端与 Java 使用相同白名单和范围。设计 Canvas 与正式 `PrintPage` 使用同一旋转/镜像 transform，图片 renderer 读取同一适配方式。Pinia 增加旋转、镜像、锁定、层级命令；锁定后阻止拖动、缩放、方向键、对齐、分布、层级和删除，复制/粘贴副本自动解锁。Canvas 增加元素和空白区右键菜单，右侧新增元素行为属性区，并补条码制式、页码格式、图片适配及边框样式/圆角编辑。

验证：

```bash
./node_modules/.bin/vitest run src/components/print src/stores/print src/views/print
./node_modules/.bin/vitest run src/api/__tests__/print.spec.js src/api/__tests__/printRuntimeContext.spec.js
./node_modules/.bin/eslint <本阶段 20 个前端源码/测试文件>
node code-copilot/changes/forge-native-print/verification/protocol-compatibility.mjs
JAVA_HOME=/private/tmp/forge-print-toolchain/jdk-17.0.20.1+1/Contents/Home /private/tmp/forge-print-toolchain/apache-maven-3.9.9/bin/mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest='PrintProtocolValidatorTest,PrintProtocolCompatibilityTest' -Dsurefire.failIfNoSpecifiedTests=false
./node_modules/.bin/vite build
git diff --check
```

结果：前端 19 文件 119 项、共享协议 34 项、Java 协议 69 项全部通过；定向 ESLint 与空白检查无输出，Vite 9372 modules 构建成功。所有触达 SFC 低于 800 行。浏览器在 `/print/designer?templateId=1&ui=t47` 确认右键菜单包含复制/旋转/镜像/层级/锁定/删除；旋转后设计元素和预览元素均为 `rotate(90deg) scaleX(1) scaleY(1)`；锁定后缩放柄消失且删除/层级禁用，元素行为属性区显示旋转和镜像配置。临时验证状态已还原，页面继续留给用户检查。

第一次通过 pnpm 包装器并行执行时触发其依赖状态检查，并临时写入 `pnpm-workspace.yaml` 的占位项；该非业务变更已完整恢复，后续直接复用现有 `node_modules/.bin`。构建仅保留项目既有 native config、CSS `//` 注释、dynamic import 和插件耗时提示。未启动真实 Admin/Flow/MySQL/Redis，未执行 Flyway、PDF 或物理打印；本阶段只 commit、不 push，`.DS_Store` 不暂存。

## 2026-09-19 · T50 原生空白表格与单元格编辑

按用户选定的高级能力优先级，先完成空白表格。编码前已补 Spec 4.8、设计结构、T50–T53 任务与增量测试口径。实现没有接入 HTML、脚本或 hiprint JSON；`STATIC_TABLE` 是 forge-print v1 的原生声明式元素，列宽、行高、覆盖矩阵、绑定、格式和逐格样式均由前后端白名单校验。

实现：基础物料增加 3×3 空白表格；Pinia 管理单元格选择和历史，支持增删行列、矩形合并、拆分、逐格文字/字段绑定、字号、对齐、颜色与边框；双击单元格可直接输入，Shift/⌘/Ctrl 可多选。缩放会同步行列物理尺寸，复制、粘贴和复制模板会重建全部嵌套 ID。设计态和正式打印共用同一表格矩阵，预览只输出纯文本。

验证：前端打印域 20 文件 125 项、共享协议 36 项、Java 打印插件 116 项全部通过；定向 ESLint 与 `git diff --check` 无输出；Vite 9379 modules 构建成功，仅有项目既有构建提示。Chromium 合成工作台在 `/print/designer?templateId=1&ui=t50` 实际添加表格、选择/多选单元格、双击输入“合同编号”，正式预览显示同一 3×3 表格。原 4318 验证进程启动时的沙箱文件快照无法解析本轮新增文件，因此另启 4319 独立 localhost 验证服务；不影响生产代码。

未启动真实 Admin/Flow/MySQL/Redis，未执行 Flyway、Firefox/Edge/Safari、PDF 或物理打印。本阶段仅本地 commit，不 push；既有 `.DS_Store` 不暂存。

## 2026-09-19 · T51 多纸张设计与手动分页

按 T51 先扩展自有 `forge-print` v1 协议，再修改设计器。新增 `PAGE_BREAK` 正文区块，只允许位于两个内容区块之间；前后端同时拒绝首位、末位、连续分页符以及分页符上的内容属性。分页游标把分页符解释为强制新页，仍沿用 50 页保护上限，分页符本身不产生纸面内容。

设计器新增 `designerPagination.js`，根据纸张方向、正文容量、固定区块高度和当前示例文本/表格行数生成可编辑的多纸张估算。每页保留毫米标尺、边距/页眉/页脚定位线、页码和自动/手动分页提示；页眉页脚按 repeat 语义重复展示，并仍映射同一协议对象。设计页码元素按估算页数显示，正式预览继续复用既有测量与分页树，不读取设计估算结果。物料面板增加手动分页，页面结构区可选择、排序和删除分页符，右侧说明分页符不占纸张高度。

验证：前端打印域 21 文件 131 项、共享前后端协议 38 项、Java 打印插件 120 项全部通过；定向 ESLint 和 `git diff --check` 无输出；Vite 9380 modules 构建成功，仅保留项目既有 native config、CSS 注释、dynamic import 和插件耗时提示。Chromium 在独立合成入口 `http://127.0.0.1:4320/print/designer?templateId=1&ui=t51` 实际插入分页符，设计区从一张纸变为两张纸并重复页眉页脚；正式预览显示两页，页码为 `1 / 2`、`2 / 2`，第二页从分页符后的流式文本和明细表开始。

未启动真实 Admin/Flow/MySQL/Redis，未执行 Flyway、Firefox/Edge/Safari、PDF 或物理打印。本阶段只在 `forge-native-print` 分支 commit，不 push；既有 `.DS_Store` 不暂存。

## 2026-09-19 · T52 可信业务打印组件注册表

新增纯代码侧 `PrintComponentRegistry`。注册定义只接受唯一受限 key、标签、图标标识和同步工厂；扩展方通过 `registerBusinessPrintComponent` 在应用启动代码中追加。工厂只接收只读字段目录和正文宽度，必须同步返回 `ELEMENT` 或 `SECTION` 的纯 JSON 片段。插入前检查 Promise、函数、symbol、bigint、循环引用、非普通对象和片段外属性，随后重建区块、元素、明细列与空白表格嵌套 ID，并依靠现有完整文档协议校验原子提交；任何失败都不修改 Store。

左侧新增“业务组件”分组，内置审批状态、签章位置和合同条款三个示例。审批状态与合同条款只在授权字段目录存在约定字段时写 FIELD 绑定，否则使用明确示例常量；签章只接受非明细 IMAGE 字段，否则保留空的安全图片占位。模板保存的是展开后的固定区块、流式文本、图片和绑定协议，不保存注册 key、Vue 组件、动态 import、工厂或函数。

验证：前端打印域 22 文件 135 项、共享协议 38 项全部通过；注册测试覆盖非法/重复 key、async、Promise、函数字段、HTML 类型、ID 重建、字段绑定与纯协议导出；定向 ESLint 和 `git diff --check` 无输出；Vite 9382 modules 构建成功，仅保留项目既有构建提示。Chromium 在 `http://127.0.0.1:4321/print/designer?templateId=1&ui=t52` 显示三个业务组件，实际插入“审批状态”后页面结构增加普通固定区块，纸面和正式预览均显示“审批状态 / 待审批（示例）”。

未启动真实 Admin/Flow/MySQL/Redis，未加载第三方业务插件，未执行 Firefox/Edge/Safari、PDF 或物理打印。本阶段只在 `forge-native-print` 分支 commit，不 push；既有 `.DS_Store` 不暂存。

## 2026-09-19 · T53 打印校准与本机验收

实现 `printCalibration.js`，统一解析 A3/A4/A5/B4/B5、横纵方向和自定义纸张，以正式 `PrintPage` 结果生成不含业务数据的校准页。校准页使用真实毫米坐标绘制距四边 10mm 的矩形框、横纵各 100mm 标尺及 10mm 刻度，并写明纸张、方向、100% 缩放、无边距和关闭浏览器页眉页脚的操作要求。自定义纸张最短边限制为 148mm，确保校准标尺可完整呈现。

浏览器能力报告分别检查打印 API、隔离文档和 CSS 毫米单位，只把会话状态报告为“可打开对话框”；`physicalOutputConfirmed` 固定为 false。实际打印复用既有隔离 iframe、资源等待、`@page` 和 afterprint 清理链路，打开对话框后只记录 `DIALOG_OPENED`。本机验收包含缩放、浏览器页眉页脚、两条标尺、10mm 框和纸张方向六项，由使用者测量勾选；记录按纸张尺寸和方向隔离写入当前浏览器 localStorage，不写模板或服务端。

设计器“更多”菜单新增低频入口“打印校准与验收”。弹窗采用纸张设置、正式校准页预览、本机实测验收三栏，默认继承当前模板纸张；支持 A3/A4/A5/B4/B5、纵横向和自定义短边/长边。Chromium 在独立合成入口 `http://127.0.0.1:4322/print/designer?templateId=1&ui=t53` 实际打开入口，核对 A4 纵向/横向、自定义 210×297mm、浏览器能力报告、10mm 框、100mm 标尺和未勾选状态的本机记录保存。未自动打开系统打印对话框，以免把对话框或合成浏览器行为冒充物理打印结果。

验证：前端打印域 23 文件 147 项、共享协议 38/38、Java `PrintProtocolCompatibilityTest` 38 项与 `PrintProtocolValidatorTest` 37 项全部通过；定向 ESLint 与 `git diff --check` 无输出；Vite 9385 modules 构建成功，仅保留项目既有 native config、CSS 注释、dynamic import 和插件耗时提示。Firefox/Edge/Safari、真实 Admin/Flow/MySQL/Redis、PDF 与物理打印机仍由用户回填验收。本阶段只在 `forge-native-print` 分支 commit，不 push，既有 `.DS_Store` 不暂存。

## 2026-09-19 · T54 Flyway 并行版本冲突修复

用户提供的 Admin 启动日志显示 Flyway 在初始化阶段失败，V1.0.168–V1.0.170 的 applied checksum 分别为 `-1783583920`、`-211989272`、`-698851386`，而打印分支把相同版本号用于打印建表、权限和隐藏路由脚本。根因是并行分支复用了已落库的版本号，不是 Job 注册或业务 Bean 初始化问题。

修复遵循已执行迁移不可变原则：从流程分支恢复 V1.0.168 `repair_flow_task_process_def_key`、V1.0.169 `seed_business_flow_need_modify_status`、V1.0.170 `add_business_flow_instance_round_no` 的原始文件；打印建表、字典权限、隐藏路由、页面身份扩展顺延到 V1.0.171–V1.0.174。四份打印 SQL 的 Git blob hash 与改名前逐一相同，只调整版本文件名；测试夹具和历史验证引用同步到新名称。`PrintResourceContractTest` 新增 Flyway 逐行 CRC32 校验、全目录版本唯一性和新旧文件名断言，防止再次覆盖这组三个流程版本。

验证命令与结果：

```bash
JAVA_HOME=/private/tmp/forge-print-toolchain/jdk-17.0.20.1+1/Contents/Home /private/tmp/forge-print-toolchain/apache-maven-3.9.9/bin/mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest=PrintResourceContractTest,PrintPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false
JAVA_HOME=/private/tmp/forge-print-toolchain/jdk-17.0.20.1+1/Contents/Home /private/tmp/forge-print-toolchain/apache-maven-3.9.9/bin/mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am test -Penable-tests -Dtest=PrintApplicationPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false
JAVA_HOME=/private/tmp/forge-print-toolchain/jdk-17.0.20.1+1/Contents/Home /private/tmp/forge-print-toolchain/apache-maven-3.9.9/bin/mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest='Print*Test' -Dsurefire.failIfNoSpecifiedTests=false
JAVA_HOME=/private/tmp/forge-print-toolchain/jdk-17.0.20.1+1/Contents/Home /private/tmp/forge-print-toolchain/apache-maven-3.9.9/bin/mvn -s /private/tmp/forge-print-maven-settings.xml -B -ntp -pl forge-admin-server -am package -DskipTests
rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.168__repair_flow_task_process_def_key.sql forge-server/db/migration/V1.0.169__seed_business_flow_need_modify_status.sql forge-server/db/migration/V1.0.170__add_business_flow_instance_round_no.sql forge-server/db/migration/V1.0.171__add_native_print_tables.sql forge-server/db/migration/V1.0.172__add_native_print_resources.sql forge-server/db/migration/V1.0.173__add_native_print_hidden_routes.sql forge-server/db/migration/V1.0.174__support_print_workspace_page_identity.sql
git diff --check
```

结果：迁移合同/持久化目标测试 8 项、generator 持久化测试 8 项、打印插件完整回归 121 项全部通过；Admin 46 模块 BUILD SUCCESS；本轮 7 份迁移的占位符扫描和空白检查无输出。仓库全量占位符扫描仍命中既有 V1.0.72 消息模板保存的 `${taskTitle}` 等运行时文本，本轮不改写该已执行历史脚本。完整回归第一次在文件沙箱内运行时，Mockito/Byte Buddy 因外部 JVM attach 被阻止产生 32 个 MockMaker 初始化错误；在允许 JVM attach 的相同工作区用同一 Maven 命令复跑后 121 项全部通过，未修改生产或测试配置来规避问题。

本轮未启动 Admin/Flow/MySQL/Redis，未连接目标远端数据库，未执行 Flyway migrate/repair 或修改 `forge_schema_history`。用户拉取更新后的 `forge-native-print` 分支并重启 Admin 后，应先正常校验流程 V1.0.168–V1.0.170，再执行打印 V1.0.171–V1.0.174；该真实库结果仍由用户验收。


## 2026-09-19 · T55 新版应用页面打印入口修复

用户反馈当前前端没有此前说明的入口。代码核对确认 `/app-center/application/:applicationCode` 已重定向到 `BusinessApplicationRuntime`，旧 `application.[applicationCode].vue` 中的 `section=printing` 分区不可达；此前把旧工作台当正式入口的说明不准确。

实现：在当前 `ApplicationSettingsPanel` 二级导航增加“打印模板”，支持 `view=settings&settingsSection=printing` 直达和刷新保持；新增 `ApplicationPrintSettings` 按 applicationCode 加载工作区并复用现有 `ApplicationPrintPanel`、PrintWorkspaceStore 和模板列表。应用卡片“更多”增加“打印模板”，用纯路由 location 打开同一正式入口。未修改 4902 行的统一运行页和 1507 行的应用中心入口页；本轮触达 SFC 分别为 302、516、92 行，均低于 800 行。

验证：入口 location、设置分区、成功/失败工作区加载和切换应用旧响应丢弃共 4 个文件 7 项通过；与打印域合并回归 27 个文件 154 项通过。定向 ESLint、`git diff --check`、SFC 行数检查和 Vite 9388 modules 生产构建通过。浏览器访问 `http://127.0.0.1:3001/app-center/application/cgou_app_1ko3psh/runtime?view=settings&settingsSection=printing` 后进入登录页，登录重定向完整保留两个查询参数；本机 8580 后端未运行并返回 502，未执行登录后的真实模板列表、数据库、PDF 或物理打印验收。


## 2026-09-19 · T56 平台超级权限兼容修复

用户登录信息显示 `admin=true`、`permissions=['*:*:*']`，但打印工作台仍提示无查看权限。根因是打印工作台、模板列表和设计器三处只读取旧 `dataPermission`，且内联判断只识别 `**` 与精确权限；当前登录结构的 `permissions`、`*:*:*` 和 `isAdmin` 均未进入判断。

实现：新增 `hasPrintPermission` 作为打印域唯一权限判断，优先接受平台 `isAdmin`，随后同时检查 `permissions` 与兼容字段 `getDataPermission`，支持 `*:*:*`、`**` 和精确权限。工作台查看、模板管理、设计器管理/发布全部改用该函数；普通无权限用户仍显示受限状态，后端 Controller/Service 权限校验不变。

验证：权限目标测试 4 项；打印域、正式入口和工作区合并回归 29 文件 165 项；触达文件 ESLint、`git diff --check` 与 Vite 9389 modules 构建全部通过。浏览器自动验证会话未持有用户登录状态，且本机 8580 返回 502，因此未执行真实登录后的页面点击；本次用户返回的权限形态已由目标测试逐项覆盖。


## 2026-09-20 · T57f 画布优先布局收紧

未完成项收口：左右侧栏变窄（约 168/232px）、左侧组件/字段/结构合并为分段页签、中间栏对齐/分布/层级/文字对齐收入下拉、多页缩略导航、锁定角标、结构/表头/物料密度压缩。

验证：触达设计器文件 ESLint 通过；`vitest run src/components/print/designer/__tests__ src/stores/print` 9 文件 68 项通过。未启动 Admin/浏览器实机验收。

## 2026-09-20 · 设计器 UX / Bug 批量修复

实现：ActionBar 左右滚动箭头；右键菜单删除/常用前置并加图标；明细表格移入基础组件并压缩空 FIXED 高度减少误分页；属性 Tab 仅在选中身份变化时重置（修边框→基础）；去掉无效 geometry 复制粘贴删除条；图片 fileId 鉴权预览；空白表格选中后可拖；椭圆改 SVG；表头 headerStyle；翻页不再清掉上一页背景（去掉 active surface 背景色覆盖）；颜色值规范化避免样式写入失败。

验证：vitest 相关 8 文件 71 项通过；触达文件 eslint --fix 通过。未做浏览器实机验收。

## 2026-09-20 · 工具栏/页眉页脚/明细表属性二次打磨

ActionBar 放大、色块不显示 hex、中间区可撑开滚动；页眉/页脚线加粗可拖；恢复画布区块白底与选中底色；明细表选中后可拖宽高手柄；表头背景可改并在预览生效；TablePanel 改为紧凑列清单 + 点选编辑 + 表头快捷色（对齐 hiprint 分层思路）。

验证：相关 vitest 39 项通过；触达文件 eslint 通过。


## 2026-09-20 · LIST/DETAIL 放开 flow.history

根因：设计目录始终含 `flow.history`，但列表/详情运行态只有 FLOW_* 场景才注入目录，前端 `validateFieldCatalog` 报「字段不可用」。

实现：LIST/DETAIL 运行目录始终合并审批字段；按 `applicationId+objectCode+recordId` 软解析最近流程实例并加载历史，无实例返回空 `history`。

验证：`LowcodePrintDataProviderTest` 8 项通过（含 detail 空历史加载）。需重启 Admin 后重试详情打印。

## 2026-09-21 · T57 表达式 / 汇总 / 连续纸拼版 / 套打水印 / 溢出 / PDF 下载

用户要求补齐金额大写与运算表达式、表格小计/汇总、连续纸与标签拼版、套打底图/水印、文字溢出三种策略，并把预览「PDF」从调起打印改为下载文件。

实现要点：绑定 `EXPRESSION` 只走白名单解析器，禁止 `eval`；明细表 `subtotal` 按页聚合、`footer` 按全部行；连续纸禁止手动分页并按内容收缩高度；标签拼版按物理宽高校验，嵌套标签不再带 `data-print-page`；套打底图默认仅设计/预览可见；固定文本 `CLIP`/`SHRINK`/`AUTO_HEIGHT`；PDF 使用隔离 iframe 栅格化后 `jspdf.save`，不调用 `window.print`。选择「标签 · 50 × 30 mm」时自动横向，避免被纸张短边优先规则转成 30×50。

验证：

1. Node v20.19.5 `node ./node_modules/vitest/vitest.mjs run src/components/print`：25 文件 168 项通过，含表达式、连续纸/拼版、CLIP、PDF 下载 mock、拼版只产生 1 个打印页。
2. 触达打印文件 ESLint 无输出；`git diff --check` 无空白错误。
3. `node code-copilot/changes/forge-native-print/verification/protocol-compatibility.mjs`：前端 41/41。
4. Java 17 `mvn -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest='PrintProtocolValidatorTest,PrintProtocolCompatibilityTest' -Dsurefire.failIfNoSpecifiedTests=false`：81 项通过（兼容 41 + 校验 40）。
5. `node --max_old_space_size=4096 ./node_modules/vite/bin/vite.js build`：42.46s 成功，产物含 `html2canvas` 与 `jspdf` 懒加载 chunk。仅保留项目既有 native/dynamic import/plugin timings 提示。

未启动 Admin/Flow/MySQL/Redis，未执行真实登录后的 PDF 下载或物理打印。客户端 PDF 为栅格化下载，不等于服务端归档或不可篡改。本阶段未 commit。

## 2026-09-21 · PDF_DOWNLOADED 500 与预览同源导出

用户导出 PDF 后事件接口 500：`PDF_DOWNLOADED` 不在 `PrintExecutionResult`。同时 html2canvas 重排导致表格行高/垂直居中与预览不一致。

实现：枚举/DTO/Mapper 接受 `PDF_DOWNLOADED`（同 `DIALOG_OPENED`：要 pageCount、不写出纸）；新增 `V1.0.180__add_print_pdf_downloaded_result.sql`，不改已执行的 V1.0.172。导出改为截取预览 `[data-print-page]`（`html-to-image` toPng + jspdf），单元格文字包 `span` 以保留 flex 居中。打印仍走 `window.print()`。

验证：

1. Node v20.19.5 `node ./node_modules/vitest/vitest.mjs run src/components/print src/stores/print`：27 文件 183 项通过。
2. `CI=true` 触达文件 ESLint 无输出；`git diff --check` 无空白错误。
3. Java 17 `mvn -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-print -am test -Penable-tests -Dtest='PrintExecutionServiceTest,PrintResourceContractTest,PrintProtocolValidatorTest' -Dsurefire.failIfNoSpecifiedTests=false`：46 项通过。

未重启真实 Admin，因此本机仍会在加载旧 class 时拒绝该枚举。用户需重启 Admin 让 Flyway 跑 V1.0.180 并加载新枚举。未做登录后实点下载或物理打印。本轮未 commit。

## 2026-09-21 · 表头缺边框 / PDF 文件名 / 预览骨架屏

用户反馈：设计器和打印里表格表头上、左边没有框；想改 PDF 文件名且默认跟业务数据、时间戳有关；`/print/preview` 内容没出来前不要转圈 loading。

实现：

1. 表头底色会盖住容器 inset 阴影。首行/首列上、左边改为单元格内侧 `linear-gradient`，叠在底色上；右/下边仍用单元格 border。设计器表体 `overflow` 改为 visible。
2. 模板字段 `exportFileName`，纸张面板可编辑。占位符 `{{main.字段}}`、`{template}`、`{timestamp}`。留空时默认「模板名-单据名-yyyyMMddHHmmss.pdf」，未写时间戳也会自动追加。
3. `PrintTemplatePicker` / `PrintPreview` 加载中用 `PrintPreviewSkeleton`（纸张形 `NSkeleton`），去掉 `NSpin`。

验证：Node v20.19.5 Vitest 7 文件 52 项通过（exportFileName、protocol、renderers、PrintPreview 含骨架屏、exportPrintPdf、PrintDesigner、staticTable）。触达文件 ESLint 通过。本机 3000 端口当前无服务，未做登录后实点。本轮未 commit。

## 2026-09-21 · 表格上/左边框与其它边粗细不一致

上/左曾用 `background-image` 渐变，右/下用 CSS `border`，打印时两套线会对不齐。改为四面都用同一条 `0.15mm` CSS border，底色 `background-clip: padding-box`。

验证：`renderers.spec.js` 7 + `staticTable.spec.js` 7 通过；触达 ESLint 通过。未做实机打印。本轮未 commit。

## 2026-09-21 · 表格表头独立配色 / 空白表不带默认表头

根因：表头色曾写进 `style` 或 `column.style`，格子只认 `headerStyle`；选中态 `!important` 又盖住底色。空白表格插入还自带「表头」灰行。属性里基础/外观/单元格/中间栏各有一套色板，有的改了没效果。

实现：

1. 「样式」页拆成表头 / 表体（明细表另有斑马纹），分别写 `headerStyle` 和 `style`。
2. 空白表格插入 `headerRow: false`，第一行空格子；「设为表头」改 `headerStyle`，不再给格子写死灰底和「表头」字。
3. 基础面板去掉整表重复色板；列色只覆盖表体；多级表头色板只覆盖当前格。
4. 选中反馈改用选择框，不再用 `!important` 覆盖单元格底色。

验证：Node v20.19.5 Vitest 6 文件 68 项通过；触达 ESLint 与 `git diff --check` 通过。`localhost:3000` 可达，但 `/print/designer` 跳登录验证码，未做登录后实点。本轮未 commit。

## 2026-09-21 · 打印签名 flow.history[0].signature 加载失败

根因：设计器示例 IMAGE 用 `data:image/svg+xml`，预览资源准备只认 PNG/JPEG/WEBP，第一条审批签名就失败。真实下载若 Content-Type 是 `octet-stream` 也会被拒。

实现：示例图改为协议内 PNG data URL；加载时按文件头识别图片类型；空签名跳过。

验证：Node v20.19.5 `designerSample` 7 + `resources` 8 + `printResourceLoader` 6 + `tablePagination` 7 通过；触达 ESLint 与 `git diff --check` 通过。未做登录后实点预览。本轮未 commit。

## 2026-09-21 · 空白表格表头背景 / 字号下拉 / 元素透明度

根因：第一行格子常带默认 `#ffffff`/`#f1f5f9`，`cell.style` 后合并盖住 `headerStyle.backgroundColor`，表头文字色仍可见。属性面板字号是数字输入框。元素没有 `style.opacity`，图片无法调透明度。

实现：

1. `staticTableCellLook` 让第一行默认白/灰底给 `headerStyle` 让位；「样式」改表头/表体走 `patchStaticTableBand`，同时删掉对应行格子上的同名覆盖。
2. 表头/表体/正文/列/最小字号改为预设 pt 下拉。
3. 协议前后端白名单增加 `style.opacity`（0–1）；画布元素框和预览 `PrintPage` 外框应用，图片同样生效。

验证：Node v20.20.0 Vitest 6 文件 60 项通过；触达 ESLint 与 `git diff --check` 通过。Java 17 `PrintProtocolValidatorTest` 40 + `PrintProtocolCompatibilityTest` 42 通过。未做登录后实点。本轮未 commit。

## 2026-09-21 · 打印字体未安装 STHeiti 导致改字体后无法预览

根因：`requireLocalFont` 只校验栈里第一个名字。设计器「华文黑体」是 `STHeiti, sans-serif`，本机没有 STHeiti（新 macOS 常见），预览报「打印字体未安装：STHeiti」。微软雅黑在 Mac 上同样会失败。这会挡住预览，看起来像保存失败。

实现：整串字体栈任一具名字体能加载即通过；有 generic 回退时不拦截。选项改成跨系统回退（华文黑体以 `Heiti SC` 开头）。

验证：Node v20.20.0 `resources.spec` 11 + `protocol.spec` 15 通过；触达 ESLint 与 `git diff --check` 通过。未做登录后实点。本轮未 commit。

## 2026-09-21 · style 额外键入库 / 透明度滑块 / 单元格快捷面板 / 表格四边

根因：`style` 用封闭键列表，`opacity` 等前端已写入的展示属性被报「不支持此属性」。快捷面板按整张表算位置。表格 overlay 八向锚点叠在外沿上，且末行/末列没有轨道手柄。

实现：

1. 前后端 `style` 允许安全基础类型额外键并写入 canonical JSON；仍拒绝 `backgroundImage`/`url()`。Java `Style` `ignoreUnknown`。
2. 「样式」透明度改为 0–100% 滑块。
3. 选中空白表格单元格时快捷面板跟随格子包围盒。
4. 表格不再画 overlay 缩放锚点；四边补首末行列拖动手柄。

验证：Node v20.20.0 Vitest 4 文件 65 项；触达 ESLint 与 `git diff --check` 通过。Java 17 `PrintProtocolValidatorTest` 41 项通过。未做登录后实点。Java 需安装插件并重启 Admin。本轮未 commit。

## 2026-09-21 · 表格角锚点恢复 / 插件安装到本地仓库

根因：上一轮为避开四边拖拽冲突把表格 overlay 锚点全部去掉。保存 `opacity` 仍失败是因为 Admin 继续加载旧的 `forge-plugin-print` jar。

实现：空白表恢复四角锚点，明细表恢复右上/右下/左下；中点 n/s/e/w 仍不画，把外沿留给行列手柄。已 `mvn -pl forge-plugin-print -am install -DskipTests` 写入本地仓库。

验证：Node v20.20.0 `staticTable` 10 + `history` 25 通过。未重启用户的 Admin。本轮未 commit。

## 2026-09-21 · 表格锚点稳定可点

根因：选中单元格后 overlay 手柄跟着格子走并被藏掉；8px 手柄叠在纸张 `overflow:hidden` 和行列拖条上，经常点不中。

实现：缩放手柄始终锚定整张表/元素外框，Teleport 到页面层并加大热区，向外偏出避免挡住四边改行列。选中格子时快捷面板仍跟格子走。

验证：Node v20.20.0 `staticTable` 10 + `history` 25 + `PrintDesigner` 16，共 51 项通过。本轮未 commit。

## 2026-09-21 · 单元格背景色保存 / 缩小锚点

根因：快捷面板「无填充」写 `transparent`，Naive 取色器还会写出 `#rrggbbaa` / `rgba()`。格子 `style.backgroundColor` 只认 `#rgb`/`#rrggbb`，保存报「颜色须使用十六进制格式」。锚点热区 22px、外偏 16px 过大。

实现：

1. `toPrintColor` / `sanitizePrintColors` 在写入和 serialize 时收成 `#rrggbb` 或 `transparent`。前后端校验同时接受这两种以及取色器 8 位 hex / rgb。
2. Teleport 锚点改为 14px 热区、约 8px 色块、外偏 8px。

验证：Node v20.20.0 Vitest 5 文件 70 项（含 `printColor` 2、`protocol` 16、`PrintDesigner` 17）。触达 ESLint 与 `git diff --check` 通过。Java 17 `PrintProtocolValidatorTest` 42 项通过。已 `mvn -pl forge-plugin-print -am install -DskipTests`。需重启 Admin 后保存才会走新校验。未做登录后实点。本轮未 commit。

## 2026-09-21 · 表格去掉左上角缩放锚点

根因：空白表/明细表左上角已有「拖动移动表格」手柄，再叠一个 nw 缩放锚点会抢命中。

实现：`resizeHandlesForElement` 对 `STATIC_TABLE` / `DATA_TABLE` 不再返回 `nw`，其余七向保留。文字等组件仍是八向。

验证：`staticTable` 10 + `PrintDesigner` 17 通过。本轮未 commit。

## 2026-09-21 · 页眉折叠条挪位 / 格子图片可缩放

根因：折叠提示画在内容区顶边正中，挡住标题。格子 `z-index:1` 低于行列拖条 `z-index:6`，图片右下角缩放点永远点不中。

实现：折叠/收起芯片改到纸张左上/左下页边空白。选中图片格抬到拖条之上，手柄挂在图片框右下角并加大热区。

验证：`staticTable` 10 + `PrintDesigner` 19 通过。本轮未 commit。

## 2026-09-21 · 横竖线颜色/线型 / 毫米下拉

根因：横竖线画在 SVG `viewBox="0 0 100 100"` 里，0.5mm 盒子里描边几乎看不见，改背景色、边框色、实线/虚线/点线都像没生效。`NInputNumber` 仍能输入汉字。

实现：

1. 横线用 `border-top`、竖线用 `border-left` 画，默认黑色、0.5mm（约 2px），样式走 CSS `solid`/`dashed`/`dotted`。改颜色时同步 `borderColor`/`backgroundColor`，改粗细时同步细轴宽高。
2. 带 mm 的字段（边框、圆角、内边距、坐标、纸张、行列、校准自定义纸张）改为不可筛选的 `NSelect` 预设，不再用数字输入框。

验证：Node v20.20.0 Vitest `printMeasures` 1 + `renderers` 9 + `PrintDesigner` 19，共 29 项通过；同会话 `history` 25 项此前已通过。触达 ESLint 与 `git diff --check` 无输出。设计器需登录验证码，未做登录后实点；未启动 Admin。本轮未 commit。

## 2026-09-21 · 打印预览返回 / 应用卡片 hover 不再撑高

根因：打印预览由列表 `window.open` 新开标签，`router.back()` 没有历史。应用中心卡片 footer 默认 `display:none`，hover 再显示操作并 `translateY(-1px)`，网格跟着晃。

实现：预览返回有历史才后退，否则关标签或回到来源应用/流程页。卡片操作栏绝对定位叠在底部，hover 才显示，不再用 `display:none` 撑高。

验证：Node v20.20.0 Vitest `printRouteContext` 10 + `ApplicationTable-print-entry` 1 通过。触达 ESLint 无输出。Agent 浏览器停在登录页，未做登录后实点。本轮未 commit。

