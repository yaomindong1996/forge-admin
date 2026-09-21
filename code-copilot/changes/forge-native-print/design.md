# 技术规划

> 状态：implementing；M1 已开始，后续模块名称仍为规划，进度以 tasks/execution-log 为准。
>
> 范围依据：[spec.md](spec.md)；执行拆解：[tasks.md](tasks.md)

## 1. 分层与依赖

### 前端

| 拟新增目录 | 职责 |
|---|---|
| `forge-admin-ui/src/components/print/protocol/` | 模板版本、结构校验、字段绑定、单位换算 |
| `forge-admin-ui/src/components/print/engine/` | 资源准备、测量、分页、页码回填、输出树 |
| `forge-admin-ui/src/components/print/renderers/` | 文本/图片/表格/二维码等无编辑行为的渲染器 |
| `forge-admin-ui/src/components/print/designer/` | 画布、选中框、拖拽、属性面板、工具栏 |
| `forge-admin-ui/src/components/print/runtime/` | 模板选择、分页预览、浏览器打印及错误定位 |
| `forge-admin-ui/src/stores/print/` | `printDesignerStore.js`、`printRuntimeStore.js` |
| `forge-admin-ui/src/api/print.js` | 类型明确的 API 封装，沿用 request/加密策略 |
| `forge-admin-ui/src/views/print/` | 模板列表、独立全屏设计路由、运行预览路由 |

渲染器由设计预览和真实打印共用。打印画布使用 mm 物理布局；设计缩放通过外层 transform，不能改变模板坐标。跨面板通信通过 Pinia，页面仅提供入口上下文。

协议与绑定函数不依赖 Vue；测量器依赖浏览器 DOM；分页器只消费标准测量结果，便于用可预测测量结果验证算法。最终分页效果还必须用真实浏览器验证，jsdom 不承担字体排版验证。

### 后端

新增 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-print/`，包名 `com.mdframe.forge.plugin.print`。

- print 插件依赖技术 starter；拥有模板/版本/绑定/执行审计，以及 `PrintDataProvider` SPI。
- generator 插件依赖 print 插件，提供低代码和流程业务适配器。print 不反向依赖 generator。
- 代码业务模块按需实现 SPI；首个采购示例使用独立适配器，复用既有 Service/代码表单 Provider 的受控读取能力。
- Admin 聚合装配；Flow 引擎继续管理审批，不承担纸张渲染。流程信息通过现有 FlowClient/业务流程服务读取。
- 应用发布服务可以调用 print 的版本校验/引用服务；print 数据服务调用 SPI 时避免形成 Bean 环。适配器依赖只读业务能力，不依赖应用发布编排。

## 2. 平台模板协议 v1

### 身份与结构

顶层固定属性采用明确模型：`protocol`、`schemaVersion`、`paper`、`header`、`body`、`footer`、`resources`。模板身份/归属、绑定、权限与发布状态保存于平台元数据，不混入可编辑画布 JSON 充当授权依据。

| 模型 | 核心字段 |
|---|---|
| paper | widthMm、heightMm、orientation、四边 marginMm |
| header/footer | heightMm、repeat、elements |
| fixed section | id、kind=FIXED、heightMm、elements；内部绝对坐标 |
| text section | id、kind=TEXT、绑定/固定文本、字体/行距、段间距、keepWithNext |
| table section | id、kind=TABLE、collectionPath、columns、headerRows、repeatHeader、footer |
| element | id、type、xMm、yMm、widthMm、heightMm、binding、format、style |
| binding | source=FIELD/CONSTANT/SYSTEM/EXPRESSION、path/value/expression；表达式为白名单运算，不接受任意脚本 |

后端使用显式文档/节点 DTO，必要的受控样式属性白名单化；前端以同一协议约束校验。数据库可保存 JSON，但 Controller 固定字段不使用任意 Map 接收。

`body` 是有序区块。复杂单据头可以放入 fixed section；后续明细表和长文本按实际高度顺排，避免自由绝对定位元素在动态表格增长后互相覆盖。

字段示例：`main.orderNo`、`main.departmentName`、`children.items`、`flow.history`、`system.generatedAt`、`system.pageNumber`。字段目录由提供方返回，模板只可绑定目录中允许的路径。拒绝原型链路径/越界路径。

模板导入只支持本协议 v1；格式错误、未知版本、未知元素、未授权字段、资源超限应定位到具体区块/元素。模板导出包含结构与逻辑资源引用，不携带业务正文和长期文件访问凭证。

## 3. 测量与分页算法

1. 准备经过授权和格式化的打印上下文，解析模板允许的字段。
2. 固定纸张和字体，等待 `document.fonts.ready` 及图片解码；失败则返回可定位错误。
3. 在隔离测量容器中，以输出相同 CSS/宽度测量文本行、表格单元格和区块高度；mm 与 CSS px 使用标准换算，保持小数精度。
4. 扣除页边距和重复页眉/页脚，得到正文可用矩形。
5. 顺序放置区块。固定区块放不下时整体换页；文本按测得行切片续页；表格每页先预留表头再放整行，末页放合计。
6. 遇到空表、最后一行恰好装满、合计需单独换页时使用显式规则，避免多出空白页。超高行/固定区块返回 `ELEMENT_TOO_TALL`，不无限循环。
7. 得到总页数后填入固定宽度页码槽；如果影响布局需要有限次重排，超出次数返回失败。
8. 产生不可变 `PrintLayoutResult`，包含 pages、elementFragments、warnings、templateVersion、generatedAt。预览/打印都消费它。

打印 DOM 采用单独 iframe/隔离容器，复制必要的可信样式和已准备资源；每页明确 page-break，浏览器不再对业务块做第二次任意分页。打印对话框的纸张、缩放 100%、关闭浏览器额外页眉页脚列入人工验收指引。

M1 落地规则：显式字体使用本地 FontFace 加载校验，系统通用族保留浏览器字体回退；罕见字符缺字按人工验收检查。页眉/页脚不重复时，各页仍保留一致的正文矩形。`keepWithNext` 保持整个当前区块与后继首片，连续设置形成约束链，超出单页返回错误。系统页码绑定仅用于固定文本槽或 PAGE_NUMBER 元素，流式文本与编码不接受页码绑定，避免总页数引起无界重排。

撤销重做只记录用户命令，不记录鼠标每个移动事件；拖拽期间展示临时位置，结束时提交一次。删除区块/撤销恢复的元素 ID 必须稳定。

## 4. 统一打印上下文

### 请求身份

`PrintPrepareDTO` 固定字段：templateId、applicationId、pageId/formKey、objectCode、recordId、scene、taskId、processInstanceId、processRunId。只有对应场景所需字段必填，服务端重建其它关联；租户/用户取登录上下文。

客户端不得传入 SQL、任意数据源 URL、任意 provider Bean 名，不能用前端 row 替代服务端读取。模板版本由应用发布快照/节点策略解析；设计预览可以显式指定草稿，但需要设计权，真实样本另需记录读取权。

### 提供方 SPI

拟提供：

- `catalog(AuthorizedPrintSource)`：返回带类型、路径、格式化提示的允许字段目录。
- `authorize(PrintContextRequest)`：检查应用/对象/记录/流程身份，生成不可由客户端伪造的服务端上下文。
- `load(AuthorizedPrintContext, PrintBindingSelection)`：仅读取并组装需要的授权数据。

实现可以组合读取/授权服务；禁止把“页面上看得到按钮”作为安全前提。代码业务提供方只接收已解析场景并再校验业务权限。

输出 `PrintContextVO`：执行 ID、应用/模板发布版本、schemaJson、字段目录、context（main/children/flow）、dataMode=CURRENT、generatedAt。图片值为受控 fileId；前端仅将服务端 generatedAt 放入渲染上下文 system。M3b 校验失败直接拒绝，不返回部分数据或成功警告。动态业务字段可以 Map 表达；固定身份和流程字段使用明确 DTO/VO。

### 低代码适配

- 读取已发布模型及页面/表单元数据，生成同一份字段目录。
- 复用 DynamicCrudService 的数据范围、解密/公式/翻译/脱敏链路；补齐子表列权限与展示转换的验证。
- 主子表不同返回形态在适配层归一化；模板不直接依赖数据库列名。
- 字典/关联标签优先复用服务端权威值。金额格式化采用定点规则，ID 保持字符串。
- 模板绑定字段变更时，在应用发布检查里报告失效路径；运行态使用固定发布模型，不用最新草稿猜字段。

### 流程适配

- 从 taskId/instanceId/runId 解析并核验 objectCode/recordId，禁止请求随意拼接其它业务记录。
- 待办、已办、我发起分别进行访问验证；历史只读接口不作为天然授权证据。
- 节点表单权限与业务字段权限取交集，子表列显式过滤。
- 时间轴按实例取得真实任务记录，顺序稳定；保留 taskId，允许同一节点多次出现和多人会签。
- 签名继续使用文件存储权限；打印资源完成准备后再允许输出。

## 5. 存储设计

所有表包含 id、tenant_id、create_by/create_time/create_dept、update_by/update_time，并显式声明逻辑删除字段。

| 表 | 专属字段与约束 |
|---|---|
| sys_print_template | application_id、template_code/name、source_type/source_key、draft_schema、draft_revision、design_status、published_version_id、status、del_flag BIGINT；唯一 `(tenant_id, application_id, template_code, del_flag)` |
| sys_print_template_version | template_id、version_no、schema_version、schema_json、schema_hash、resource_manifest、publish_time、del_flag；版本号跨历史永久唯一 `(tenant_id, template_id, version_no)`，不因删除复用 |
| sys_print_binding | application_id、page_id/form_key、object_code、template_id、scene、is_default、sort_order、status、binding_revision、del_flag BIGINT；规范化 source_key，唯一 `(tenant_id, application_id, source_key, template_id, scene, del_flag)` |
| sys_print_execution | template_version_id、application_version_id、object_code、record_id、process_instance_id/run_id、actor、data_mode、generated_at、result、error_code、page_count、del_flag；不存单据正文 |

来源身份使用结构化字段及服务端规范化 source_key，不能让客户端自行构造唯一范围。默认模板唯一性在同一绑定范围事务锁内维护。

模板 JSON 使用 TEXT/LONGTEXT 按体积上限校验；发布时稳定序列化求 hash。资源只保存文件引用/字体定义，不把 token 或未经限制的 data URI 塞进 schema。

逻辑删除及引用保护按根规则实施。日志留存清理不在首期，不新增物理删除任务。

### 状态规则

- 设计状态 `DRAFT -> PUBLISHED -> CHANGED -> PUBLISHED`；编辑已发布模板只改变草稿。
- 启停字段独立使用 EnableStatus；停用阻止 prepare，不删除已发布版本。
- 发布成功新增版本并更新 published_version_id，CAS 校验 draft_revision；校验失败不改变已发布引用。
- 执行结果使用专用枚举：`PREPARED`、`DIALOG_OPENED`、`PDF_DOWNLOADED`、`FAILED`。客户端 DIALOG_OPENED/PDF_DOWNLOADED 仅为报告事件，不证明物理打印成功。
- 对外“删除”仅在引用校验通过后执行逻辑删除。

## 6. 拟定接口

基础路径 `/print`，最终落位前核实前端代理/统一响应约定；所有写接口为明确 DTO，返回 RespInfo。

| 方法/路径 | DTO 或用途 | 访问条件 |
|---|---|---|
| GET `/templates/page` | pageNum/pageSize、应用筛选 | view + 应用设计可见范围 |
| GET `/templates/:id` | 元数据及草稿 | view + 应用范围 |
| POST `/templates` | PrintTemplateCreateDTO | manage |
| PUT `/templates/:id` | PrintTemplateUpdateDTO，含 revision | manage + CAS |
| DELETE `/templates/:id` | 引用保护/逻辑删除 | manage |
| POST `/templates/:id/copy` | PrintTemplateCopyDTO | 来源可见 + 同一应用 manage（不允许复制时改来源） |
| POST `/templates/:id/publish` | PrintTemplatePublishDTO，含 expectedRevision；hash 服务端生成 | publish + 发布校验 |
| PUT `/templates/:id/status` | PrintTemplateStatusDTO | manage |
| GET `/templates/:id/versions` | 不可变版本列表 | view |
| GET/PUT `/bindings` | PrintBindingQueryDTO/PrintBindingSaveDTO | 所属应用设计权限 + GET view / PUT manage |
| POST `/catalog` | PrintCatalogQueryDTO | 场景对应的设计/运行权限 |
| POST `/available` | PrintAvailableTemplatesDTO | execute + 场景/记录访问权；不返回草稿 |
| POST `/prepare` | PrintPrepareDTO | execute + 模板/记录/流程/字段权限 |
| POST `/executions/:id/events` | PrintExecutionEventDTO | 执行人/租户一致 + 可接受事件 |

prepare 对每次请求重新授权；即使知道旧版本 ID 也不能绕过停用/发布绑定。执行事件不接受正文、不更改业务数据。失败返回具体错误码和元素/字段定位，日志不打印敏感内容。

## 7. 工作台、流程与发布接入

- 应用内新增“打印模板”资源入口，表单上下文进入时自动带 source identity；管理层仍允许按应用查看模板。当前正式管理入口挂在统一应用页的“应用设置”二级导航，应用中心卡片“更多”只负责跳转到 `view=settings&settingsSection=printing`。旧工作台 `section=printing` 不再作为入口。
- 设计器独立全屏路由；普通操作以“保存草稿/预览/发布”呈现，低频操作收纳。
- 列表行/详情优先通过既有 action 配置投影为受控打印路由，不向 AiCrudPage 巨型文件追加实现。
- 流程入口使用共享详情壳/表单面板的正式上下文，不能从 DOM、组件内部代理或全局变量猜当前任务。
- `FlowTaskDetailShell.vue` 已超过 800 行，接入前拆分时间轴/样式；如果仍需触达 `todo.vue` 等超 2000 行 SFC，先完成独立拆分并回归审批动作。
- 节点打印策略通过现有节点面板保存进节点扩展配置，随 BPMN 版本保留。增加的是打印选择策略，不变更审批动作。
- 应用发布将 printBindings 与 templateVersions 纳入候选快照/校验/hash，失败不产生半发布引用；回滚恢复对应清单。
- 代码下载携带打印协议/模板定义/绑定版本和运行时依赖；原生成器继续保持共用运行协议，不增加另一套解释器。
- `ApplicationSettingsPanel` 只管理二级导航和查询参数；独立 `ApplicationPrintSettings` 按 applicationCode 加载工作区并复用 `ApplicationPrintPanel`。不向 4900 行统一运行页传递打印对象，也不向 1500 行应用中心入口页追加处理函数。
- 打印快捷入口位置由纯函数生成 Vue Router location，组件测试固定验证应用编码、`view=settings` 与 `settingsSection=printing`，防止路由再次落到废弃工作台。

## 8. 验证与上线边界

首期桌面目标为实际验收机器上的 Chromium 系浏览器，另列待验证的浏览器/打印机。手机可查看预览，但不承诺移动端模板编辑和静默打印。

单位换算/分页/状态/权限用针对性测试；真实浏览器验证字体、分页位置与资源加载；应用/流程真实联调及打印机验收由用户回填。完整矩阵见 test-spec.md。

迁移和权限资源只在编码阶段生成。上线时模板为空，不自动生成真实业务样本或给全员开权限。回退停用入口和模板、回滚应用版本，保留所有用户设计与历史记录。

## M2 实施落位（2026-09-19）

- `stores/print/printDesignerStore.js` 是模板、选中项、缩放、预览开关、错误和历史的共享来源。props 只在工作台入口加载协议/字段目录，内部面板直接使用 store。
- `commands.js` 负责毫米几何命令，`history.js` 保存最多 50 次文档历史。pointermove 不入历史，pointerup 原子提交，取消/卸载恢复手势前状态；字段目录与业务预览上下文不进入撤销记录。
- 编辑器内部固定带使用 header/footer，正文使用 `section:<协议 ID>`，避免合法导入 ID 与固定带重名；序列化协议不增加内部属性。
- Canvas/SelectionOverlay/CanvasElement 与 drag/resize/keyboard 拆分；Palette/FieldTree/SectionList 提供物料、受控字段、顺序编辑；六个小属性面板含表头横向合并拆分与合计绑定。
- `designerSample.js` 只依据字段目录生成非业务示例上下文；真实样本存在时直接使用授权样本。Canvas 与 PrintPreview 接收同一 context，固定文本/页码/线框/椭圆调用 `renderers/registry.js` 的正式渲染组件，条码/二维码调用正式编码器；表格设计态按相同表头、列格式和表尾绑定生成预览行。内部字段路径只在字段树和绑定属性中展示，不作为纸面正文。
- `elementCatalog.js` 用预设区分标题/固定文本、横线/竖线等设计意图，持久化仍使用受控协议类型；椭圆是新增的原生协议类型，前后端白名单同步。画布选择命令集中在 `PrintCanvasActionBar.vue`，顺序变化通过数组层级完成并进入同一撤销历史。
- 元素协议以可选 `rotationDeg/flipX/flipY/locked` 表达中心点变换和设计锁定，样式以白名单 `objectFit/borderStyle/borderRadiusMm` 扩展图片与边框表现；Canvas 和 `PrintPage` 读取同一字段组合 transform。右键菜单、属性面板和工具栏只调用 Store 命令，锁定检查集中在手势、几何、对齐、层级和删除入口；复制出的锁定元素强制解锁。
- `STATIC_TABLE` 是固定区块内的原生元素，`table.columns/rows/cells` 分别保存毫米列宽、行高和带 row/column/rowSpan/colSpan 的稀疏起始单元格；校验器用覆盖矩阵保证每个坐标恰好覆盖一次。生产渲染使用 CSS Grid，设计态用独立编辑组件在 Pinia 中保存选中单元格，不把选择态写进模板。
- `PAGE_BREAK` 是正文有序区块，只表达“后续内容从新页开始”；分页游标处理显式换页，首尾或连续分页符由协议拒绝。`designerPagination.js` 使用示例内容高度生成多纸张编辑估算，页眉页脚重复展示但仍映射同一协议对象，最终页数以预览分页树为准。
- `printComponentRegistry.js` 只接受应用代码注册的唯一 key、标签、图标和同步工厂。工厂输出标准 element/section 片段，经当前文档 `assertPrintDocument` 校验后原子写入；下载/导入协议不包含注册器或函数。平台内置审批状态、签章位置和合同条款三个示例注册项，业务插件可在启动时追加。
- `printCalibration.js` 生成不含业务数据的 A3/A4/A5/B4/B5/自定义纸张校准结果和浏览器能力报告，复用隔离 iframe 打印会话；校准页包含 10mm 边距框、100mm 标尺、方向和缩放检查说明。物理打印结果只存用户本地验收勾选，不写服务端执行成功。
- `draftStorage.js` 校验导入体积和协议、只存模板。`PrintDesigner` 支持注入 `saveDraft(document)`，M3 接口就绪后替换默认本地适配器。`views/print/designer.vue` 提供页面组件与路由离开保护；菜单种子留给 M3。
- 当前一个页面只挂载一个 PrintDesigner；验证入口专用独立 Pinia。每次加载模板清空旧选择/历史/剪贴板，保存通过 generation 避免异步回调污染后来加载的文档。

## Flyway 并行版本冲突修复（2026-09-19）

- 以目标数据库已经执行的流程迁移为历史事实：V1.0.168 `repair_flow_task_process_def_key`、V1.0.169 `seed_business_flow_need_modify_status`、V1.0.170 `add_business_flow_instance_round_no` 必须按原文件名与原内容进入打印分支，checksum 分别保持 `-1783583920`、`-211989272`、`-698851386`。
- 原打印迁移按依赖顺序整体平移：建表改为 V1.0.171，字典/权限改为 V1.0.172，隐藏路由改为 V1.0.173，页面身份列扩展改为 V1.0.174。SQL 内容保持不变，H2 夹具和资源合同测试同步读取新名称。
- 新库顺序执行流程 168–170 与打印 171–174；已执行流程 168–170 的数据库从 171 开始正常迁移。任何环境都不执行 `repair`、不删除 `forge_schema_history`、不手工覆盖 checksum。

## M3a 落位细化（2026-09-19，编码前）

- Flyway 冲突修复后使用 V1.0.171（四张表）与 V1.0.172（字典/权限）；V1.0.168–V1.0.170 保留已执行的流程迁移，不覆盖任何历史脚本。
- 模板来源保留 page_id、form_key、object_code 及 source_type/source_key；source_key 长度 191，由后续授权 Provider 规范化，创建 DTO 不接受客户端 source_key 或 tenant/actor/status。
- 来源类型 LOWCODE/CODE；场景 LIST/DETAIL/FLOW_TODO/FLOW_DONE/FLOW_STARTED；数据模式 CURRENT。均有模块枚举和 sys_print_* 字典。
- template_code 长度 80，模板名 100，revision 使用 Long；启停也递增 draft_revision，避免停用与发布并发覆盖。版本仅可插入/读取。
- 执行表补 application_id、template_id、source_key、scene、task_id，方便归属校验和索引；不保存正文、请求快照、文件 URL 或异常堆栈。result 仅 PREPARED/DIALOG_OPENED/FAILED，page_count 最大 50。
- 协议服务直接解析 schemaJson，限制 UTF-8 1MiB、JSON 深度 64、重复键/尾随内容，按白名单拒绝未知字段并输出 path/code/message。仅返回规范化 JSON、SHA-256 与类型化文档，不执行模板内容。哈希对对象键排序、数字规范化，数组顺序保持；不用于跨语言签名。
- 明确模型使用 Java 17 record，绑定常量使用 JsonNode 保留 null/false/0，不做弱类型强转。服务端严格检查所有已提供属性（包括当前 kind/type 未使用的属性），避免隐藏无效内容；前端正常生成的模板不受影响。
- 迁移只增加表、字典和四项权限，无授权放开。回滚先移除应用依赖与停用权限；已有模板/版本/审计数据保留，禁止自动 drop 或覆盖。实际数据库恢复由备份和人工脚本执行。

## M3b 接口与授权细化（2026-09-19，编码前）

- 所有入口由服务端 PrintIdentity 取得 actor/tenant/dept，租户上下文与登录租户不一致时拒绝。Controller 权限注解与 Service 身份检查同时保留；操作日志不保存模板请求正文或运行响应。
- PrintApplicationAccess 独立负责应用设计可见/管理/发布范围、应用行锁和已发布快照引用保护。PrintDataProvider 负责来源、字段/文件、记录和流程授权；Registry 无默认放行，来源类型和结构化来源唯一匹配。M3 生产环境无适配器时返回明确不可用，Spring 仍能启动。
- 来源请求嵌套固定 DTO（applicationId/sourceType/pageId/formKey/objectCode）；规范化 source_key 对类型及稳定标识组合做 SHA-256，客户端不能指定。模板来源创建后不可变。
- 写事务统一先锁应用再锁模板，绑定写入同样遵循此顺序，避免默认绑定的空集合竞争和模板删除/绑定新增竞争。Provider 的 lockApplication 必须取得同一数据库事务内的持久化行锁。
- 发布前同时验证协议、来源字段目录和文件授权；同内容重复发布返回当前发布版本，编辑后恢复相同内容也重用该版本。版本写入和模板指针 CAS 同事务，失败回滚。版本详情 GET /templates/:id/versions/:versionId 仅设计查看权限。
- Runtime 请求使用 {source,recordId,scene,taskId,processInstanceId,processRunId}；prepare 另加 templateId，不接受客户端 versionId/actor/tenant/rawData。catalog 使用 source（设计）或 record（运行）二选一，分别需要 view/execute。
- authorize 返回不可由请求构造的 AuthorizedPrintContext，包括应用发布版本、当前允许的不可变模板版本引用和字段目录。运行选项仅来自该已发布清单；不回退到最新草稿/最新发布版本，不使用设计态绑定替代应用快照。模板停用仍立即阻止新 prepare。
- load 后再次按模板所需且目录允许的字段投影，仅保留引用到的 main/children/flow 字段；每个集合最多 500 行、输出 JSON 最多 4 MiB、标量文本最多 100000 字符。图片文件引用由 Provider 核验，不返回带长期 token 的 URL。
- 执行事件仅接受 DIALOG_OPENED/FAILED，限制 actor/tenant、页数和错误码，重复相同事件幂等返回，终态不能互相改写，不报告物理打印成功。
- 前端管理共享状态进入 printTemplateStore，运行上下文进入 printRuntimeStore；异步请求有代次防串数据。保存更新修订号，不回灌文档覆盖用户保存期间的新编辑。运行关闭清空正文；服务端编辑不会回退到本地保存。
- 设计预览未提供真实授权记录时明确为模板预览；不能把空数据预览称为业务打印。M3b 菜单仅注册隐藏列表/设计/预览路由，权限由已有四项控制，不自动授予角色。

M3b 绑定解除补充：`DELETE /print/bindings/{id}?expectedRevision=...` 按应用→模板锁顺序执行修订号 CAS 逻辑删除；停用绑定保留配置，解除绑定才允许模板引用检查通过。绑定列表与写入返回固定 Binding VO，不返回审计内部字段。

M3b 输出协议细化：prepare 返回规范化 `schemaJson`，浏览器按同一 v1 校验器解析，避免 Java record 的可选 null 属性改变协议。context 按 main/children/flow 投影，前端 system.generatedAt 取服务端时刻。文件下载复用鉴权 HTTP 客户端和 getFileUrl，限定 fileId、二进制响应、10 秒超时及取消；M5 继续补流程签名/附件特有授权。服务端模式设计器预览不可直接发起打印，实际单据打印必须经 prepare。

M3b 审查补充：图片元素的 FIELD 绑定只能使用目录类型 IMAGE；静态资源别名先归一到 fileId，再做设计/运行资源授权。流程请求可省略运行 ID 交由 Provider 解析，但授权结果的流程 context 必须含 processRunId。动态数据 context JSON 用限长输出流检查 4MiB，避免先分配超大序列化数组；schemaJson 仍独立受 v1 的 1MiB 上限约束。

## M4a 接入前置约束（2026-09-19）

- 低代码 pageId 使用 1–128 位受控字符串 `[A-Za-z0-9][A-Za-z0-9_.:-]*`，支持工作台 `page_*` 标识；已有数字 ID 转为等值十进制字符串，source_key 摘要算法不变。两张打印表通过 V1.0.174 扩展列，不修改 V1.0.171 建表迁移。回退保留 VARCHAR 列，不能强制降回 BIGINT 丢失新页面标识。
- 应用侧实现 PrintApplicationAccess：分别叠加应用 list/edit/publish 与 print view/manage/publish 权限，按应用当前可见范围检查；租户/用户来自 PrintIdentity，与 SPI actor 必须一致。此处是设计权限，不用于业务运行授权。
- 应用版本提交与打印修改/删除统一先锁应用行，随后读模板/版本。历史引用查询使用锁定读，避免 MySQL REPEATABLE READ 的先前快照漏掉刚发布的引用；查询显式 tenant_id/del_flag，锁查询不使用 LIMIT。
- 应用快照扩展 `printing = {schemaVersion: 1, bindings: [...]}`。每项固定 source、scene、templateId、templateVersionId、schemaHash、isDefault、sortOrder；无正文，无最新版本回退。同来源/场景/模板不能重复，每范围默认至多一个。旧快照缺少 printing 视为空；显式 null、未知版本、坏结构拒绝。
- 最终提交/回滚提交均校验固定版本仍存在、模板启用、应用来源一致及规范协议 SHA-256 一致。模板删除扫描全部未删除应用版本，任何历史引用均阻止删除，错误不回显历史正文。候选快照生成、来源字段变更检查由 M4b 完成；本阶段不开放真实数据 Provider。
- 本阶段无权限资源新增、无角色自动授权、无业务状态/数据修复 SQL。应用版本新增与发布指针仍由原事务处理，打印校验失败整体回滚该事务；既有协调发布前置步骤的副作用仍沿用原恢复机制，不承诺全系统原子回滚。

M4a 落位：`PrintApplicationAccessAdapter`、`PrintApplicationLock`、`PrintApplicationSnapshotCodec`、`PrintApplicationVersionGuard` 均在 generator 的 `service/printing/`，不使 print 反向依赖 generator。Codec 每个应用最多 1000 条引用、每来源/场景最多 100 个模板；IDs 支持 Long 范围字符串，拒绝小数/溢出与未知字段。`BusinessApplicationVersionService.commitImmutable` 在查询/写入版本之前调用守卫，因此幂等重试、正常提交和回滚提交均走共同锁。引用守卫不是候选字段合法性校验的替代，后者继续由 M4b 实现。

### M4b 编码拆分与边界（2026-09-19）

- M4b-1：PrintBindingMapper/XML、PrintApplicationSnapshotContributor、BusinessApplicationSnapshotService；候选生成时固定启用绑定的已发布模板版本/hash，后续发布重试和回滚保留固定引用，不重新读取最新模板指针。
- M4b-2：PrintMetadataResolver、LowcodePrintCatalogBuilder、LowcodePrintSourceResolver、PrintBindingValidationService；从应用版本指定的对象设计版本→CRUD 版本读取完整元数据；来源必须是该页面实际使用的对象；字段与明细可见性取发布模型/页面交集。
- M4b-3：严格读取固定配置的 LowcodePrintRecordReader、LowcodePrintValueAdapter、DynamicCrudService 小范围增量；主子表都走记录范围、解密/公式/翻译/脱敏，子表禁止猜测外键，超过 500 行拒绝。金额输出统一回到打印协议的分。
- M4b-4：LowcodePrintDataProvider、LowcodePrintResourceAccess、应用提交守卫接字段验证；仅 LIST/DETAIL，当前用户应用/页面/对象/记录权限全部通过才返回固定版本；流程场景仍拒绝，留 M5。
- M4b-5：以上服务单测、真实 Mapper/事务增量验证、Admin 聚合构建；回填本轮证据后分阶段本地提交。

每个子任务主要源码不超过 5 个文件；测试、构造器兼容调整和本 SDD 文档单列。运行读取绝不回退到草稿；缺少完整历史发布配置、子对象固定版本或关系元数据时给出可定位错误，不能以猜测配置继续输出。应用发布自身的多步骤恢复机制保持现状，打印验证失败不提交新的应用版本/指针。

### M4b 实际落位与兼容边界

- generator 的 `service/printing/` 新增 LowcodePrintSourceResolver、PrintMetadataResolver、LowcodePrintCatalogBuilder、LowcodePrintValueAdapter、LowcodePrintRecordReader、LowcodePrintResourceAccess、LowcodePrintDataProvider、PrintBindingValidationService、PrintApplicationSnapshotContributor。没有新 Controller 或请求体协议。
- 候选快照捕获已启用绑定的 publishedVersionId/hash；最终提交仍按快照固定引用校验，不跟随最新模板指针。回滚重用历史 printing 清单。应用已有多步骤发布恢复机制保持不变，打印失败不写新的应用版本/指针，不宣称回滚先前其它资产发布步骤。
- 运行态从门户同一过滤后页面树验证来源，要求对象 list/query 权限，并对主子表执行现有数据范围。缺失的历史安全配置、子对象固定版本、关系、已删除字段和格式类型不兼容均明确拒绝；历史缺少 printing 的应用仍为空绑定，需要重新发布后才获得打印入口资源。
- 当前数据模式：应用/对象/模板版本固定，业务记录取已保存的当前值。金额按打印协议输出分字符串，大整数不经浮点；字典和关联优先展示值，敏感字段不得用未脱敏 Name 副本替换。
- 字段范围使用模型的可见性与页面/子表显示列。字段改为不兼容的 MONEY/NUMBER/DATE/BOOLEAN/IMAGE 格式会拒绝；通用 TEXT 允许兼容标量类型。流程节点列权限与审批签名仍属于 M5。
- 普通 CRUD 的宽容行为保持现状；打印新增严格翻译/脱敏、无猜测外键、501 行探测/500 行上限及无业务正文日志的虚拟公式执行入口。公式执行失败不返回部分计算结果；复用现有公式引擎，不复制表达式解释器。
- 运行时的启停/删除检查使用 AiCrudConfigMapper/XML 的数量查询，兼容旧 CRUD 的 0=正常/1=停用与业务对象的 1=启用，不读取草稿字段作为运行配置。


## M4c 实际接入与导出协议

- 工作台 printing 分区从页面树实际 objectRef 与应用 objects 的交集生成来源；Pinia 记录应用身份、选中来源，失效或切应用清理。不让用户输入 objectCode/页面 ID。
- PortalPageRenderer 将 pageId 加入 render 请求，Controller 在既有流程叠加之后调用独立打印动作投影器。只使用 runtimeById 返回的已授权页面树与该版本固定 printing 绑定，检查 print:execute/对象读权限/模板启用。输出 route=/print/preview，静态来源参数 + rowField 主键参数，详情与列表场景独立，不读取/转发整行正文。
- LowcodeProtocolSnapshotBuilder 统一调用 PrintCodegenContributor，frontendRuntimeConfig.printing 与 protocol.runtimeConfig.printing 保持同一对象。Velocity 附加 config/<key>-printing.json 与 PRINTING.md；应用包额外生成 application-printing.json（包括主子表聚合的子对象），application-manifest.printingPath 指向它。
- 单对象导出结构：protocol=forge-print-export，schemaVersion=1，applications[] 含字符串 applicationId/applicationVersionId、versionNo、bindings[]、templates[]；模板含 templateId/templateVersionId/templateCode/templateName/versionNo/schemaHash/schema。runtime 指定共享前端组件/预览路由、forge-plugin-print、PrintDataProvider 与受控 fileId 资源策略。
- 应用级导出结构：protocol=forge-application-print-export、schemaVersion=1、applicationId、objects[{sourceConfigKey,printing}]。配置键下载包含相关当前发布应用；应用/访问入口下载通过 codegen.printApplicationId 缩小范围。所有来源均重新授权；缺失/停用模板或 hash 不一致整体失败。
- 查询走 Mapper XML：当前 application.last_publish_version → version.snapshot_json.objects.configKey，不用正在编辑的对象关系作为发布依据。应用包下载/预览在 REPEATABLE_READ 事务内，独立贡献器调用为只读事务；普通 CRUD 后端继续静态 Service/Mapper XML。MySQL JSON 函数与真实隔离级别需另行实跑。
- 导出不包含业务记录、文件内容或 token；独立环境导入需一致映射应用/模板/版本/fileId，并接入数据提供方。覆盖报告标记 REQUIRES_EXTENSION，避免把协议携带等同于独立部署已可打印。
