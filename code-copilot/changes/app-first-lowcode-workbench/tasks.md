# 任务拆分 — 应用优先的低代码开发工作台
> status: apply
> created: 2026-07-13
> change: `app-first-lowcode-workbench`
> 执行原则：按 Phase 顺序推进；每一阶段通过独立门禁后才能进入下一阶段。

## 0. 路径约定

以下任务中的后端相对路径均位于：

`forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/`

前端相对路径均位于：

`forge-admin-ui/src/`

数据库迁移位于：

`forge-server/db/migration/`

## 1. 前置条件

- [x] 用户确认 `spec.md` 的 HARD-GATE。
- [x] 实施前重新读取 `AGENTS.md`、三份 memory 和 `automated-testing-standard.md`。
- [x] 实施前检查最新 Flyway 版本，确认正式目录最新为 `V1.0.26`，本阶段使用 `V1.0.27`。
- [x] 记录当前后端测试和 Flyway 静态检查基线；Phase 1 无前端改动，不重复执行前端构建。
- [x] 未覆盖 `.DS_Store`、`preferences.md` 等用户已有无关改动。
- [x] 未启动真实服务、未执行真实数据库迁移。
- [x] 后端使用 JDK 17；Phase 1 未执行前端命令。

## 2. 阶段总览

| Phase | 目标 | Tasks | 可交付结果 |
|-------|------|-------|------------|
| Phase 0 | 术语与兼容冻结 | 0～2 | 决策、测试基线、协议冻结 |
| Phase 1 | 应用聚合基础 | 3～11 | 新应用 CRUD、对象关联、入口归属、存量回填 |
| Phase 2 | 应用优先总览 | 12～16 | 总览以应用为主体，聚合分页无 N+1 |
| Phase 3 | 应用工作台与表优先设计 | 17～24 | 集中工作台、表映射首屏、保存/同步分离 |
| Phase 4 | 受治理扩展中心 | 25～34 | JS/CSS/服务绑定的安全生命周期 |
| Phase 5 | 应用级协调发布 | 35～41 | 就绪度、不可变快照、发布、恢复和回滚 |
| Phase 6 | 验证与归档准备 | 42～45 | 构建、浏览器验证、执行证据、评审结论 |

## 3. 任务状态总览

| Task | 阶段 | 名称 | 状态 | 优先级 |
|------|------|------|------|--------|
| 0 | Phase 0 | Proposal 文档形成 | completed | P0 |
| 1 | Phase 0 | HARD-GATE 与术语确认 | completed | P0 |
| 2 | Phase 0 | 实施基线和兼容契约测试 | completed | P0 |
| 3 | Phase 1 | 应用聚合 Flyway 迁移 | completed | P0 |
| 4 | Phase 1 | 应用实体与 DTO/VO 协议 | completed | P0 |
| 5 | Phase 1 | 应用聚合 Mapper 查询 | completed | P0 |
| 6 | Phase 1 | 应用 CRUD 服务 | completed | P0 |
| 7 | Phase 1 | 应用-对象关联服务 | completed | P0 |
| 8 | Phase 1 | 应用 Controller 与权限资源 | completed | P0 |
| 9 | Phase 1 | 访问入口 applicationId 兼容 | completed | P0 |
| 10 | Phase 1 | Binding APPLICATION 目标兼容 | completed | P1 |
| 11 | Phase 1 | 存量回填与 Phase 1 验证 | completed | P0 |
| 12 | Phase 2 | 应用聚合分页计数 | completed | P0 |
| 13 | Phase 2 | 应用前端 API 与路由骨架 | completed | P0 |
| 14 | Phase 2 | 应用总览主区重构 | completed | P0 |
| 15 | Phase 2 | 新建/编辑应用交互 | completed | P0 |
| 16 | Phase 2 | Phase 2 构建与浏览器验收 | completed-static | P0 |
| 17 | Phase 3 | 应用工作台后端聚合 | completed | P0 |
| 18 | Phase 3 | 应用工作台页面骨架 | completed | P0 |
| 19 | Phase 3 | 数据对象分区 | completed | P0 |
| 20 | Phase 3 | 表映射聚合协议 | completed | P0 |
| 21 | Phase 3 | 表结构首屏与字段网格 | completed | P0 |
| 22 | Phase 3 | DDL 预览与显式同步编排 | completed | P0 |
| 23 | Phase 3 | 集中设计入口与旧对象兼容 | completed | P0 |
| 24 | Phase 3 | Phase 3 回归验收 | completed-static | P0 |
| 25 | Phase 4 | 扩展中心安全设计复核 | completed | P0 |
| 26 | Phase 4 | 扩展数据表与字典迁移 | completed | P0 |
| 27 | Phase 4 | 扩展实体、Mapper 和 CRUD | completed | P0 |
| 28 | Phase 4 | 扩展版本、差异和编辑锁 | completed | P0 |
| 29 | Phase 4 | JS 沙箱执行器 | completed | P0 |
| 30 | Phase 4 | CSS 解析与作用域隔离 | completed | P0 |
| 31 | Phase 4 | 服务端白名单绑定 | completed | P0 |
| 32 | Phase 4 | 扩展验证、测试与状态机 | completed | P0 |
| 33 | Phase 4 | 扩展中心前端 | completed | P0 |
| 34 | Phase 4 | Phase 4 安全负例验收 | completed-static | P0 |
| 35 | Phase 5 | 应用版本表迁移 | completed | P0 |
| 36 | Phase 5 | 应用就绪度聚合 | completed | P0 |
| 37 | Phase 5 | 不可变快照服务 | completed | P0 |
| 38 | Phase 5 | 协调发布编排 | completed | P0 |
| 39 | Phase 5 | 发布恢复与回滚 | completed | P0 |
| 40 | Phase 5 | 发布历史前端 | completed | P1 |
| 41 | Phase 5 | Phase 5 故障恢复验收 | completed-static | P0 |
| 42 | Phase 6 | 后端聚合构建和目标测试 | pending | P0 |
| 43 | Phase 6 | 前端构建、Lint 和浏览器验收 | pending | P0 |
| 44 | Phase 6 | 真实迁移/API/E2E 用户回填 | pending | P0 |
| 45 | Phase 6 | 文档回填和归档评审 | pending | P1 |

## Phase 0：语义与兼容冻结

### Task 0：Proposal 文档形成

- **目标**：形成完整 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。
- **涉及文件**：
  - `code-copilot/changes/app-first-lowcode-workbench/spec.md`
  - `code-copilot/changes/app-first-lowcode-workbench/tasks.md`
  - `code-copilot/changes/app-first-lowcode-workbench/test-spec.md`
  - `code-copilot/changes/app-first-lowcode-workbench/execution-log.md`
- **验收**：文档状态一致，无未解释占位项；仅文档变更。

### Task 1：HARD-GATE 与术语确认

- **目标**：用户确认新应用聚合、对象复用、表优先双入口和扩展安全边界。
- **涉及文件**：
  - `code-copilot/changes/app-first-lowcode-workbench/spec.md` — 回填确认状态、确认人和时间。
  - `code-copilot/changes/app-first-lowcode-workbench/execution-log.md` — 记录确认内容。
- **退出条件**：六项待确认全部明确；未确认时禁止执行 Task 2 之后的代码任务。

### Task 2：实施基线和兼容契约测试

- **目标**：在新增代码前固化现有访问入口和对象设计行为。
- **涉及文件**：
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessAppServiceCompatibilityTest.java` — 新增访问入口兼容测试。
  - `src/test/java/com/mdframe/forge/plugin/generator/controller/BusinessAppControllerCompatibilityTest.java` — 新增旧 API 协议测试。
  - `code-copilot/changes/app-first-lowcode-workbench/execution-log.md` — 记录 Red/Green 前基线。
- **关键场景**：旧 `/ai/business/app` CRUD、open-info、代码配置、代码预览/下载入口不改变；逻辑删除过滤仍有效。
- **验收**：先记录现有通过/失败基线，后续 Phase 1 反复复跑。

## Phase 1：应用聚合基础

### Task 3：应用聚合 Flyway 迁移

- **目标**：新增真实应用表、应用对象关联表，并为访问入口补可空归属字段。
- **涉及文件**：
  - `forge-server/db/migration/V<next>__add_business_application_aggregate.sql` — 新增，实施时替换 `<next>` 为最新可用版本。
- **迁移内容**：
  - `ai_business_application`
  - `ai_business_application_object`
  - `ai_business_app.application_id` 与索引
  - 应用相关字典和 `sys_resource` 权限
- **规则**：完整审计字段、`tenant_id=1` 内置数据、`del_flag`、生成列未删除唯一键、`information_schema` 防重复、无 Flyway `${...}` 占位符。
- **验证**：SQL 静态扫描；实体代码未完成前不执行真实迁移。

### Task 4：应用实体与 DTO/VO 协议

- **目标**：建立新聚合的 Java 数据协议，避免复用旧 `BusinessApp`。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessApplication.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessApplicationObject.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessApplicationDTO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessApplicationQueryDTO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessApplicationVO.java`
- **关键字段**：`applicationCode/applicationName/suiteCode/status/designStatus/lastPublishVersion/lastPublishTime`；关联角色和计数字段。
- **约束**：两个实体显式 `@TableLogic`；状态展示依赖字典，Java 常量只负责校验和状态机。

### Task 5：应用聚合 Mapper 查询

- **目标**：所有应用查询落在 Mapper XML，并为后续聚合分页预留一次查询能力。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessApplicationMapper.java`
  - `src/main/resources/mapper/BusinessApplicationMapper.xml`
  - `src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessApplicationObjectMapper.java`
  - `src/main/resources/mapper/BusinessApplicationObjectMapper.xml`
- **关键签名**：
  ```java
  Page<BusinessApplicationVO> selectApplicationPage(Page<BusinessApplicationVO> page,
      @Param("tenantId") Long tenantId,
      @Param("query") BusinessApplicationQueryDTO query);

  BusinessApplicationVO selectApplicationDetail(@Param("tenantId") Long tenantId,
      @Param("id") Long id);

  List<AiBusinessApplicationObject> selectByApplicationId(@Param("tenantId") Long tenantId,
      @Param("applicationId") Long applicationId);
  ```
- **验收**：XML 中所有主表和 join 的设计元数据显式过滤 `del_flag='0'`。

### Task 6：应用 CRUD 服务

- **目标**：实现应用校验、CRUD、状态管理和删除保护。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationService.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationServiceTest.java`
- **关键签名**：
  ```java
  Page<BusinessApplicationVO> page(Integer pageNum, Integer pageSize, BusinessApplicationQueryDTO query);
  BusinessApplicationVO detail(Long id);
  BusinessApplicationVO detailByCode(String applicationCode);
  Long create(BusinessApplicationDTO dto);
  void update(BusinessApplicationDTO dto);
  void updateStatus(Long id, Integer status);
  void delete(Long id);
  ```
- **核心测试**：编码唯一、业务域有效、租户隔离、启用入口删除保护、逻辑删除后可重建同编码。

### Task 7：应用-对象关联服务

- **目标**：实现多对多对象编排和唯一主对象规则。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessApplicationObjectDTO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessApplicationObjectVO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationObjectService.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationObjectServiceTest.java`
- **关键签名**：
  ```java
  List<BusinessApplicationObjectVO> list(Long applicationId);
  void replace(Long applicationId, List<BusinessApplicationObjectDTO> objects);
  Set<Long> listAffectedApplicationIds(Long objectId);
  ```
- **核心测试**：最多一个 PRIMARY、共享对象不被删除、重复关联拒绝、跨租户对象拒绝、草稿允许无主对象。

### Task 8：应用 Controller 与权限资源

- **目标**：暴露新应用 API，不影响旧访问入口 Controller。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessApplicationController.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/controller/BusinessApplicationControllerTest.java`
  - Task 3 的 Flyway 文件 — 补齐 `ai:businessApplication:*` 资源，仍未执行时可在同脚本维护。
- **接口**：`/page`、`/list`、`/{id}`、`/by-code/{applicationCode}`、POST、PUT、status、DELETE、objects GET/PUT。
- **验收**：`pageNum/pageSize`、`@ApiEncrypt/@ApiDecrypt`、`RespInfo`、`@OperationLog`、细分权限完整。

### Task 9：访问入口 applicationId 兼容

- **目标**：让新入口归属于应用，同时保证存量和旧 API 可空兼容。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessApp.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessAppDTO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessAppQueryDTO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessAppVO.java`
  - `src/main/resources/mapper/BusinessAppMapper.xml`
- **配套修改**：`BusinessAppService.java` 校验指定应用与业务域/对象一致；兼容同步调用允许为空。
- **测试**：复跑 Task 2；新增合法归属、跨域归属拒绝、空归属兼容。

### Task 10：Binding APPLICATION 目标兼容

- **目标**：让能力可挂到真实应用，同时保留旧 `APP=访问入口`。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessBindingService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessBindingDTO.java`
  - `src/main/resources/mapper/BusinessBindingMapper.xml`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessBindingServiceTest.java`
- **验收**：`APPLICATION` 校验新应用 ID；`APP` 仍校验旧入口 ID；不存在、跨租户和已删除目标失败关闭。

### Task 11：存量回填与 Phase 1 验证

- **目标**：实现确定性、幂等的默认应用和入口归属回填。
- **涉及文件**：
  - Task 3 Flyway 脚本 — 增加回填 SQL 或调用可重复存储过程片段。
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationBackfillContractTest.java` — 用固定数据集验证规则。
  - `code-copilot/changes/app-first-lowcode-workbench/execution-log.md`
- **回填断言**：主对象、明细、引用、歧义入口、无对象入口、重复执行、逻辑删除数据、租户隔离。
- **阶段门**：Phase 1 测试全绿；真实 Flyway 仍由用户执行并回填证据。

## Phase 2：应用优先总览

### Task 12：应用聚合分页计数

- **目标**：一次查询返回对象、入口、流程、扩展和问题数量，不产生 N+1。
- **涉及文件**：
  - `src/main/resources/mapper/BusinessApplicationMapper.xml`
  - `src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessApplicationVO.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/mapper/BusinessApplicationMapperTest.java`
- **实现要求**：通过预聚合子查询或分组 join 计算计数；业务域筛选使用已有子树解析结果；分页排序稳定。
- **验收**：默认 20 条应用的 SQL 数量固定，不随记录数增加。
- **执行结果**：已使用预聚合子查询一次返回对象、入口、流程、扩展和问题数；父业务域由 MySQL 8 递归子树解析；`BusinessApplicationMapperTest` 3 个场景通过。真实 SQL 次数和 800ms 响应基线待用户运行环境回填。

### Task 13：应用前端 API 与路由骨架

- **目标**：新应用 API 与旧访问入口 API 分文件，建立工作台路由。
- **涉及文件**：
  - `api/business-application.js` — 新增。
  - `router/index.js` — 新增 `/app-center/application/:applicationCode`。
  - `views/app-center/application.[applicationCode].vue` — 新增最小占位骨架。
- **关键函数**：
  ```javascript
  businessApplicationPage(params)
  businessApplicationDetail(id)
  businessApplicationDetailByCode(applicationCode)
  createBusinessApplication(data)
  updateBusinessApplication(data)
  saveBusinessApplicationObjects(id, data)
  ```
- **验收**：`api/business-app.js` 继续只表示访问入口，不搬迁旧函数。
- **执行结果**：已新增独立 `business-application.js`、稳定工作台路由和 Phase 2 摘要占位页；旧 `/app-center/app/:appId`、对象设计路由和 `business-app.js` 保持。

### Task 14：应用总览主区重构

- **目标**：总览从对象分组改为应用列表。
- **涉及文件**：
  - `views/app-center/index.vue`
  - `views/app-center/components/ApplicationTable.vue` — 新增。
  - `views/app-center/components/AppFilterBar.vue` — 改为应用筛选语义，必要时更名为 `ApplicationFilterBar.vue`。
- **交互**：保留业务域树；应用为唯一主记录；显示状态和计数；进入工作台新开页签。
- **删除依赖**：总览不再加载 `businessObjectRelations`，不再渲染 `BusinessObjectTable`。
- **执行结果**：总览已改为左侧业务域父子树、右侧应用聚合列表；父域数量与子树筛选口径一致；总览源码不再引用对象分组、对象列表、入口列表或对象关系查询。增量收口将业务域配色统一到系统主题变量，左侧树和右侧列表各自独立纵向滚动，应用表格统一承载横向/纵向滚动，并删除右侧重复的“全部业务域/应用总览”标题说明区。小屏通过 `ResizeObserver` 读取列表真实可用宽度，隐藏业务域、资产、更新时间等次要列，最窄场景始终保留应用和操作列；分页导航与每页条数拆分后可换行展示。

### Task 15：新建/编辑应用交互

- **目标**：提供两步新建和轻量编辑，不在总览中堆对象设计细节。
- **涉及文件**：
  - `views/app-center/components/ApplicationEditorDrawer.vue`
  - `views/app-center/components/ApplicationInitializeStep.vue`
  - `views/app-center/index.vue`
- **初始化方式**：空白、已有对象、数据库表、模板、AI 草稿；首期未实现的来源必须清晰标为后续动作，不伪装成功。
- **验收**：创建应用成功即能进入工作台；初始化失败保留草稿和重试提示。
- **执行结果**：已完成空白、已有对象和数据库表三种可用初始化；选择“从数据库表开始”后直接在新建应用向导内选择运行数据源和数据表，应用草稿创建后自动导入对象并建立 PRIMARY 关联，不再进入应用后二次打开对象导入向导。应用或对象已创建但后续绑定失败时保留对应 ID，重试不会重复创建；模板/AI 草稿仍明确禁用为后续阶段。

### Task 16：Phase 2 构建与浏览器验收

- **目标**：验证总览信息架构和关键交互。
- **涉及文件**：
  - `code-copilot/changes/app-first-lowcode-workbench/test-spec.md`
  - `code-copilot/changes/app-first-lowcode-workbench/execution-log.md`
- **浏览器场景**：父域筛选含子域、关键词/状态筛选、分页、空状态、新建、编辑、启停、进入应用、旧入口路由。
- **阶段门**：前端 build、浏览器交互、聚合 API 单测通过；无“业务对象分组”主内容。
- **执行结果**：定向 ESLint、前端生产构建、41 个后端目标测试、Mapper XML、Flyway 静态门禁、Admin 42 模块聚合包和 `git diff --check` 均通过。未启动 Admin/Vite、未执行 Flyway，因此浏览器交互、真实 SQL 次数、接口耗时和旧入口运行态回归仍保持 pending。

## Phase 3：应用工作台与表优先对象设计

### Task 17：应用工作台后端聚合

- **目标**：返回工作台首屏摘要和分区状态，不一次加载全部详情。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessApplicationWorkspaceVO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationWorkspaceService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessApplicationController.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationWorkspaceServiceTest.java`
- **关键签名**：
  ```java
  BusinessApplicationWorkspaceVO workspace(Long applicationId);
  BusinessApplicationReadinessVO readiness(Long applicationId);
  ```
- **验收**：摘要含分区数量、阻断/警告数和最近变更；详细列表按分区 API 懒加载。
- **执行结果**：已新增七分区摘要、就绪度问题和独立 workspace/readiness API；无主对象、无启用入口为阻断，对象停用/未发布为提醒，流程缺失不阻断。

### Task 18：应用工作台页面骨架

- **目标**：建立概览、数据对象、页面入口、流程自动化、动作增强、权限、发布历史七个分区。
- **涉及文件**：
  - `views/app-center/application.[applicationCode].vue`
  - `views/app-center/application-workspace/ApplicationWorkspaceHeader.vue`
  - `views/app-center/application-workspace/ApplicationWorkspaceNav.vue`
  - `views/app-center/application-workspace/ApplicationOverviewPanel.vue`
- **验收**：紧凑企业工作台；分区按需加载；刷新和深链可恢复当前分区。
- **执行结果**：已落地命令栏、应用头、左侧七分区导航和 query 深链；对象、入口、自动化分区异步加载，页面未使用统计卡、渐变或装饰性动效。页面入口分区继续复用现有入口能力，并把当前应用 ID、业务域和应用内对象快照传入简化向导，避免用户重复选择应用上下文或误关联域外对象。

### Task 19：数据对象分区

- **目标**：在应用内编排对象，显示数据库映射摘要和共享影响。
- **涉及文件**：
  - `views/app-center/application-workspace/ApplicationObjectsPanel.vue`
  - `views/app-center/application-workspace/ApplicationObjectBinder.vue`
  - `api/business-application.js`
- **操作**：关联已有对象、从表导入、新建对象、设主对象、调整角色、移除关联。
- **验收**：共享对象显示被多少应用使用；移除关联不删除对象。
- **执行结果**：已支持关联已有对象、空白新建、数据库表导入、角色调整、设主对象和移除；列表一次返回数据源、物理表、设计版本和共享应用数，移除仍只改编排关系。新建应用时选择数据库表已前移到应用向导并自动完成对象导入和主对象关联，进入工作台后直接查看结果；对象分区中的数据库表导入继续保留，用于已有应用追加业务对象。

### Task 20：表映射聚合协议

- **目标**：复用现有对象设计器、低代码模型和 DDL 服务，统一返回表映射信息。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessObjectTableMappingVO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessObjectDesignerController.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectTableMappingServiceTest.java`
- **关键签名**：
  ```java
  BusinessObjectTableMappingVO getTableMapping(Long objectId);
  LowcodeDdlPreviewVO previewDatabaseDiff(Long objectId, Integer designVersion);
  ```
- **字段**：数据源、表名、字段映射、系统字段、同步状态、最近同步时间、未同步变更和共享应用数。
- **执行结果**：已通过 `BusinessObjectDesignContextProvider` 复用统一设计上下文，返回业务字段/字段编码/数据库列三向映射、实际列元数据、差异、设计版本和最近同步结果。

### Task 21：表结构首屏与字段网格

- **目标**：新建对象先显示表和字段，设计页持续显示映射摘要。
- **涉及文件**：
  - `views/app-center/object-designer.[objectCode].vue`
  - `views/app-center/components/designer/BusinessObjectDesignerShell.vue`
  - `views/app-center/components/designer/BusinessFieldManager.vue`
  - `views/app-center/components/designer/BusinessTableMappingSummary.vue` — 新增。
- **字段网格**：业务名、字段编码、数据库列、类型、长度、可空、默认值、索引、控件、同步状态。
- **验收**：新对象和数据库导入对象默认数据结构；表单深链仍可直接进入画布；表映射摘要不藏在高级配置。
- **执行结果**：对象设计器默认打开“数据结构”，该导航常驻；表映射摘要在所有设计分区可见，数据结构首屏展示可滚动字段网格，`?panel=form/list/detail` 深链保持。数据库导入增量修正为按物理列保留类型、长度、decimal 精度和必填状态，表单数字控件不再把已有 `decimal/bigint` 反向覆盖为 `int`；数据结构首屏新增显式索引配置入口，并为修复前已经保存错误类型的导入草稿提供“按数据库校准字段”显式修复动作。

### Task 22：DDL 预览与显式同步编排

- **目标**：把现有 `LowcodeDdlService` 和发布检查整理为明确的预览/确认动作，不另写 DDL 引擎。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDesignerService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessObjectDesignerController.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessObjectDatabaseSyncServiceTest.java`
- **关键签名**：
  ```java
  LowcodeDdlPreviewVO previewDatabaseDiff(Long objectId, Integer designVersion);
  void syncDatabase(Long objectId, Integer designVersion, boolean confirmOnlineDdl);
  ```
- **规则**：保存草稿不执行 DDL；权限 `ai:lowcode:deploy-ddl`；版本冲突失败；高风险差异默认只导出脚本；数据源 `allowDdl=0` 拒绝。
- **执行结果**：对象保存接口不再处理旧 `syncDdl` 兼容字段；预览与同步使用独立 API，版本、确认、权限、数据源能力和只读状态逐层校验，仅 CREATE/ADD COLUMN 及其受控索引/注释可在线执行，高风险脚本只预览/导出。增量修复对象发布检查与最终执行白名单不一致：两者统一复用同一安全判定，执行前先校验全部语句，避免先执行部分追加 DDL 后才被 `MODIFY/ALTER COLUMN` 拦截。二级索引进一步改为完全显式配置：不再从查询字段、关系字段、租户字段或审计字段自动生成，旧 `auto=true` 配置也不进入 DDL；用户保存索引配置后才允许在预览中生成受控 ADD/CREATE INDEX。

### Task 23：集中设计入口与旧对象兼容

- **目标**：把表单、列表、详情、关系、动作、流程、增强、发布和历史集中到对象上下文。
- **涉及文件**：
  - `views/app-center/components/designer/BusinessObjectDesignerShell.vue`
  - `views/app-center/object-designer.[objectCode].vue`
  - `views/app-center/application-workspace/ApplicationEntriesPanel.vue`
  - `views/app-center/application-workspace/ApplicationAutomationPanel.vue`
- **约束**：只做导航和现有能力编排，不复制 FormDesignerSchema、ViewSchema 或对象版本表。
- **验收**：存量对象、旧 object designer 路由、表单深链、发布检查和回滚仍可用。
- **执行结果**：七分区只编排现有对象设计器、入口、流程、触发器、权限、发布检查和版本能力；未复制表单/列表/详情 Schema，旧对象与访问入口路由保持。

### Task 24：Phase 3 回归验收

- **目标**：验证表优先与表单优先兼容，以及 DDL 安全边界。
- **场景**：新建空白对象、导入已有表、保存草稿、DDL 预览、无权限同步、数据源禁 DDL、版本冲突、旧对象表单编辑、共享影响提示。
- **涉及文件**：`test-spec.md`、`execution-log.md`。
- **阶段门**：P0 场景通过后才能启用扩展中心开发。
- **执行结果**：56 个 Phase 1～3/兼容目标测试、定向 ESLint、前端生产构建、Mapper XML、Admin 42 模块聚合包和差异检查通过；真实数据库、API 和浏览器场景仍待用户环境回填，因此标记为 `completed-static`。

## Phase 4：受治理的扩展中心

### Task 25：扩展中心安全设计复核

- **目标**：在编码前冻结脚本宿主、CSS 解析器、服务注册协议、超时和日志脱敏方案。
- **涉及文件**：
  - `spec.md` — 只在用户确认变更时修订技术决策。
  - `test-spec.md` — 固化沙箱逃逸和越界负例。
  - `execution-log.md` — 记录复核结论。
- **硬门**：无法证明主页面无 `eval/new Function`、服务绑定无法任意反射时，不进入 Task 26。
- **执行结果**：已冻结独立 Worker、CSS AST、服务端 `handlerCode` 注册表、SHA-256 锁令牌和脱敏审计边界；宿主页动态执行与任意反射硬门已解除。

### Task 26：扩展数据表与字典迁移

- **目标**：新增扩展和扩展版本表、字典与权限资源。
- **涉及文件**：
  - `forge-server/db/migration/V<next>__add_business_extension_governance.sql`
- **内容**：`ai_business_extension`、`ai_business_extension_version`、扩展类型/状态/钩子/失败策略字典和权限。
- **规则**：完整审计、逻辑删除、未删除唯一键、敏感值不入库、防重复和 Flyway 占位符扫描。
- **执行结果**：已新增 `V1.0.28__add_business_extension_governance.sql`，包含扩展、不可变内容版本、执行审计、字典、权限和角色继承；真实迁移待用户执行。

### Task 27：扩展实体、Mapper 和 CRUD

- **目标**：建立扩展基本 CRUD 和租户/归属校验。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessExtension.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessExtensionVersion.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessExtensionMapper.java`
  - `src/main/resources/mapper/BusinessExtensionMapper.xml`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessExtensionService.java`
- **测试文件**：`src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessExtensionServiceTest.java`。
- **验收**：应用/对象/入口归属一致；显式 `@TableLogic`；XML 显式过滤。
- **执行结果**：已完成扩展 CRUD、应用/对象/入口租户归属校验、逻辑删除、XML 显式过滤和敏感配置拒绝。

### Task 28：扩展版本、差异和编辑锁

- **目标**：每次内容保存生成版本，支持 diff、回滚为新草稿和超时锁。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessExtensionVersionService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessExtensionLockService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessExtensionController.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessExtensionVersionServiceTest.java`
- **验收**：历史版本不覆盖；锁不能跨租户释放；超时释放；回滚产生新版本。
- **执行结果**：每次内容保存追加版本，diff/回滚生成新草稿；编辑锁绑定租户、用户和 token 摘要，支持续期、超时接管和条件释放。

### Task 29：JS 沙箱执行器

- **目标**：实现受限客户端脚本宿主，不在主页面上下文执行存储脚本。
- **涉及文件**：
  - `components/lowcode-extension/js/ExtensionSandboxHost.vue`
  - `components/lowcode-extension/js/extension-sandbox.worker.js`
  - `components/lowcode-extension/js/extension-context-api.js`
  - `components/lowcode-extension/js/extension-sandbox.spec.js`
- **验收负例**：访问 window/document/cookie/storage/token、任意 fetch、无限循环、超大输出、原型污染均失败关闭。
- **执行结果**：已实现独立 module Worker 宿主、结构化上下文/effects、敏感 API 裁剪、超时 terminate、输出上限和禁止动态执行策略。增量增加 Worker READY 握手、初始化/执行双超时、校验/固化/执行/返回阶段诊断、ErrorEvent 文件行列信息和消息反序列化错误；用户脚本前后端均禁止引用 postMessage，Worker 协议通道保持可用。Vite 全局对象兼容层已从仅主线程可用的 `window` 改为 `globalThis`，避免 module Worker 加载 `env.mjs` 时在用户脚本执行前异常终止。

### Task 30：CSS 解析与作用域隔离

- **目标**：解析、校验并重写 CSS 选择器，限制到应用/页面根节点。
- **涉及文件**：
  - `components/lowcode-extension/css/scoped-css.js`
  - `components/lowcode-extension/css/scoped-css.spec.js`
  - `components/lowcode-extension/css/ScopedCssPreview.vue`
- **验收负例**：`@import`、外部 URL、`html/body/:root`、作用域逃逸和 Forge 布局选择器被拒绝。
- **执行结果**：已使用 `css-tree` AST 解析/重写并增加服务端二次作用域复核；全局根、URL、导入、布局选择器和越界样式失败关闭。

### Task 31：服务端白名单绑定

- **目标**：只调用显式注册的低代码扩展处理器。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/extension/LowcodeExtensionHandler.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/extension/LowcodeExtensionRegistry.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/extension/ServerBindingExecutor.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/extension/ServerBindingExecutorTest.java`
- **关键签名**：
  ```java
  ExtensionExecutionResult execute(ExtensionExecutionContext context);
  Optional<LowcodeExtensionHandler> find(String handlerCode);
  ```
- **验收**：未知 handler、任意 class/bean 名、超时、越权、非法输入全部失败关闭。
- **执行结果**：已完成显式处理器注册表、输入 Schema、允许钩子、权限、租户、风险和超时校验；协议不接受 Bean/Class/URL 入口。增量补充 `lowcode-java-extension.md` 开发模板，处理器结构化失败会写失败审计，并按 BLOCK/WARN/IGNORE 执行。

### Task 32：扩展验证、测试与状态机

- **目标**：统一校验、受限测试、DRAFT→TESTED→ENABLED 状态和失败策略。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessExtensionValidationService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessExtensionExecutionService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessExtensionController.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessExtensionExecutionServiceTest.java`
- **验收**：内容变更退回 DRAFT；未测试不能启用；BLOCK/WARN/IGNORE 符合钩子风险规则；审计脱敏。
- **执行结果**：已实现 `DRAFT→TESTED→ENABLED→DISABLED`、内容变更退回草稿、风险/失败策略约束和不保存输入/原始异常的执行审计；扩展类型与钩子兼容性在保存和草稿校验两层失败关闭，Java 处理器返回 `success=false` 不再误判为测试通过。

### Task 33：扩展中心前端

- **目标**：在应用工作台集中管理增强，不把编辑器堆在总览。
- **涉及文件**：
  - `views/app-center/application-workspace/ApplicationExtensionsPanel.vue`
  - `views/app-center/application-workspace/ExtensionEditorDrawer.vue`
  - `views/app-center/application-workspace/ExtensionVersionDrawer.vue`
  - `api/business-extension.js`
- **交互**：按类型/钩子/状态筛选；编辑、验证、测试、启停、diff、回滚、锁提示。
- **验收**：高风险选项有业务说明；普通用户默认先看到可视化规则，脚本位于开发者分区。
- **执行结果**：应用工作台已接入真实扩展中心，支持筛选、可视化规则、JS/CSS/Java 服务增强编辑、验证、测试、启停、锁、diff 和回滚；新增按数据写入、读取、交换、页面交互分组的钩子矩阵，并根据扩展类型和 Java 处理器契约禁用不兼容触发点。JS/CSS 已从普通文本域升级为 CodeMirror 引导式工作台，按触发点提供可直接使用的示例、可用 API/增强区域、能力边界和全屏编辑。CLIENT_JS 测试上下文已改为引导式业务数据表单：从 `readField/setField/triggerAction` 自动识别字段与动作，加载所选业务对象的真实字段/动作名称，提供类型化测试值、示例值/空值场景和只读高级 JSON 预览，不再要求用户手工同步逗号字段串和整段 JSON。

### Task 34：Phase 4 安全负例验收

- **目标**：安全失败场景全部形成自动化证据。
- **涉及文件**：`test-spec.md`、`execution-log.md`。
- **门禁**：JS 沙箱逃逸、CSS 越界、任意服务反射、超时、敏感日志、跨租户、未测试启用任一失败即阻断 Phase 5。
- **执行结果**：已提供 75 个 Phase 1～4 后端回归用例和 26 个前端安全用例的历史通过证据；最终前端复验因本地 `vitest` 依赖缺失未执行，用户明确改为自行验证，因此 Phase 4 标记 `completed-static`。

## Phase 5：应用级协调发布

### Task 35：应用版本表迁移

- **目标**：新增不可变应用版本快照表和发布字典/权限。
- **涉及文件**：
  - `forge-server/db/migration/V<next>__add_business_application_version.sql`
- **内容**：`ai_business_application_version`、应用发布状态字典、发布/回滚权限。
- **验收**：应用内版本唯一、完整审计、逻辑删除、快照内容无密钥。
- **执行结果**：已新增 `V1.0.29__add_business_application_release_coordination.sql`，建立不可变版本表和可恢复发布运行单，补齐状态/步骤字典、发布/恢复/回滚权限与角色继承。

### Task 36：应用就绪度聚合

- **目标**：聚合对象、数据库、入口、流程、扩展、权限和发布差异。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessApplicationReadinessVO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationReadinessService.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationReadinessServiceTest.java`
- **关键签名**：
  ```java
  BusinessApplicationReadinessVO check(Long applicationId);
  ```
- **验收**：阻断/警告/提示分级；每项带定位信息；共享对象变更显示复用影响但不阻断当前应用发布。
- **执行结果**：已新增统一就绪度服务，聚合应用/业务域、主对象、对象发布检查、数据库同步、入口、流程、扩展、权限和共享对象影响；问题携带分区与资产定位。增量修正扩展选择语义：默认只纳入已测试、已启用或已停用扩展，未测试草稿保留并提示跳过，不再阻断其它应用资产发布；只有显式选择未测试扩展时才阻断。共享对象存在未发布变更时改为 `WARN` 影响提醒，只显示复用应用数量，不再要求同步评估并阻断当前应用。发布路径一次解析对象、入口、扩展和应用绑定，权限资源按对象集合批量查询，并把已解析上下文传给快照阶段复用。

### Task 37：不可变快照服务

- **目标**：收集并持久化应用发布快照，生成稳定摘要。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessApplicationVersion.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/mapper/BusinessApplicationVersionMapper.java`
  - `src/main/resources/mapper/BusinessApplicationVersionMapper.xml`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationVersionService.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationVersionServiceTest.java`
- **验收**：版本单调递增；历史行无 update；相同快照 hash 可识别；无 Secret。
- **执行结果**：已实现白名单快照、递归敏感键清理、SHA-256 摘要、应用内版本预留和只插入版本服务；版本 Mapper 不暴露 update。历史列表不读取大型 `snapshot_json`；快照复用发布检查已装载的应用、对象、入口、扩展、绑定和权限摘要，扩展版本按 `(extension_id, version_no)` 批量读取。

### Task 38：协调发布编排

- **目标**：按可恢复步骤发布对象、入口和扩展，最终提交应用状态。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessApplicationPublishDTO.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPublishService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessApplicationController.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPublishServiceTest.java`
- **关键签名**：
  ```java
  BusinessApplicationPublishResultVO publish(Long applicationId,
      BusinessApplicationPublishDTO dto, String idempotencyKey);
  ```
- **验收**：预检查阻断、幂等、依赖补齐、步骤结果清晰、成功后 `PUBLISHED`、后续变更转 `CHANGED`。
- **执行结果**：已实现 `PRECHECK→SNAPSHOT→OBJECTS→ENTRIES→EXTENSIONS→COMMIT` 六步编排、16～128 位幂等键、数据库认领、主对象/入口/扩展依赖补齐、步骤结果和最终 `PUBLISHED` 提交。首次发布复用创建运行单前的权威检查结果，对象最新发布版本改为一次批量查询，运行单步骤更新后复用内存实体，成功认领后不再额外回查；恢复发布仍强制重新执行检查。

### Task 39：发布恢复与回滚

- **目标**：对部分失败发布提供重试/补偿，对历史快照提供兼容回滚。
- **涉及文件**：
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPublishRecoveryService.java`
  - `src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationRollbackService.java`
  - `src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationRollbackServiceTest.java`
- **验收**：不自动回滚业务数据/破坏性 DDL；缺字段历史版本阻断；重复恢复不重复副作用。
- **执行结果**：部分失败持久化为 `PARTIAL/FAILED` 并可按原运行单恢复；历史回滚检查对象版本、物理表/列、入口、扩展版本和挂接，缺失即阻断，不执行反向 DDL 或业务数据回滚。

### Task 40：发布历史前端

- **目标**：显示就绪度、发布差异、步骤结果、版本和回滚边界。
- **涉及文件**：
  - `views/app-center/application-workspace/ApplicationPublishPanel.vue`
  - `views/app-center/application-workspace/ApplicationVersionDrawer.vue`
  - `api/business-application.js`
- **验收**：阻断问题可跳转；部分失败不显示为成功；回滚前明确提示数据库不自动回滚。
- **执行结果**：已用紧凑企业工作台替换发布占位，提供发布检查、问题跳转、不可变版本详情、运行步骤、失败恢复和带数据库边界提示的回滚确认。应用工作台头部“发布应用”是工作台内唯一发布入口，点击后切换到发布历史并立即打开发布确认，不在发布面板重复放置第二个按钮；应用总览的草稿行额外提供可见“发布”操作，通过 `section=releases&publish=1` 深链复用同一确认和发布逻辑，不复制发布 API；进入发布历史只并发加载轻量版本/运行记录，完整检查改为显式按需执行，直接发布只由后端执行一次最终权威检查；发布检查超时保护为 60 秒，发布/恢复/回滚为 120 秒。

### Task 41：Phase 5 故障恢复验收

- **目标**：模拟对象发布失败、入口切换失败、扩展启用失败和重复请求。
- **涉及文件**：`test-spec.md`、`execution-log.md`。
- **阶段门**：发布结果可解释、可重试、快照不可变、旧运行入口不受损。
- **执行结果**：已补 Controller、Mapper、快照脱敏、固定步骤和回滚无反向 DDL 的自动化契约用例；按用户要求本轮不运行，真实故障注入、构建和 E2E 保持 `pending`。

## Phase 6：验证与归档准备

### Task 42：后端聚合构建和目标测试

- **目标**：执行 generator 目标测试和 admin 聚合构建。
- **命令**：按 `test-spec.md`，JDK 17；先目标测试，后 `mvn -pl forge-admin-server -am package -DskipTests`。
- **涉及文件**：`execution-log.md` 记录命令、输出、警告和跳过项。

### Task 43：前端构建、Lint 和浏览器验收

- **目标**：Node 20.19.0 下完成构建和应用总览/工作台浏览器交互。
- **命令**：`pnpm build`，必要时按项目已有脚本执行目标 lint；不得用全量自动修复覆盖无关文件。
- **涉及文件**：`execution-log.md` 记录构建和截图/交互证据。
- **用户验收反馈**：首次验收发现业务域树缺少颜色层次、应用图标同色、窄视口操作不可见和工作台 403；已补前端视觉/滚动修复及 `V1.0.30` 隐藏路由资源，等待用户复验，任务状态保持 `pending`。
- **第三轮修复结果**：按用户澄清取消操作列固定，应用总览下整张表统一横向移动；列表使用明确视口高度并由单一容器承载横向/纵向滚动。目标 ESLint 和生产构建通过，浏览器复验受托管环境端口/Chromium 权限阻塞，Task 43 仍保持 `pending`。
- **工作台整合增量**：轻量工作台快照直接携带对象、入口和扩展；分区使用 KeepAlive 保留数据；入口类型中文化；对象设计器以内嵌模式复用到应用工作台，消除应用看板与对象看板之间的新页签跳转。前端 ESLint/build、后端生产源码编译、Mapper XML 和 Flyway 静态检查通过；目标测试受仓库既有测试构造器不一致阻断，状态为 `completed-static`。
- **2026-07-14 缺陷增量**：数据源测试连接改为数据源级消息键，修复成功结果被 loading 消息立即销毁；AiCrudPage 有数据列表取消 144px 强制最小高度，空状态高度保持；草稿应用列表增加发布操作。进入应用、发布改为带悬停说明和无障碍标签的图标按钮，操作列从 176/160px 收窄为 128px。按用户分工未运行构建、Lint 或浏览器验证，Task 43 仍保持 `pending`。
- **2026-07-14 易用性增量**：新建应用的数据库表初始化合并到同一向导；新建访问入口由三步收敛为“选择场景 → 配置入口”两步，应用、业务域、主业务单元、入口名称、入口编码、页面配置和单一默认表单自动带出。普通路径只保留一个“添加到管理端菜单”开关，父菜单和排序继续由高级编辑承载。按用户分工仅做静态检查，不运行构建、Lint 或浏览器验证，Task 43 仍保持 `pending`。

### Task 44：真实迁移/API/E2E 用户回填

- **目标**：由用户在真实本地环境执行 Flyway、服务启动、API 和数据库核对。
- **用户需回填**：`forge_schema_history`、应用/关联/入口计数、关键 API 返回、发布和回滚场景。
- **状态规则**：用户未提供证据时保持 `pending`，不得写成通过。

### Task 45：文档回填和归档评审

- **目标**：同步 Spec、Tasks、Test Spec 和执行日志，确认无未解决 P0 风险。
- **涉及文件**：本变更目录四份文档；如产生通用决策/踩坑，增量更新对应 memory。
- **退出条件**：所有必需 Task 完成、跳过项有依据、用户确认真实联调结果后，才可进入 `/archive`。

## Phase 7：模板化快速搭建、无入口预览与工作台收口

### Task 46：统一模板初始化后端

- **目标**：基于现有业务对象、页面 Schema 和对象关系协议初始化单表、左树右表和主子表，不建设第二套生成器。
- **涉及文件**：模板初始化 DTO/VO、`BusinessApplicationTemplateService`、`BusinessApplicationController`、目标服务测试源码。
- **验收**：应用草稿外的模板资产在同一事务中提交；失败不遗留半套对象；结果返回主对象和全部对象摘要。
- **执行结果**：已新增应用级模板初始化 DTO/VO、统一事务服务和 Controller 接口；三类模板复用业务对象字段、关系和页面 Schema，补充静态契约测试源码但按用户分工未执行。

### Task 47：应用创建模板选择与定向引导

- **目标**：新建应用第二步优先显示三套模板线框预览，并只展示当前模板需要的配置。
- **涉及文件**：`ApplicationInitializeStep.vue`、`ApplicationEditorDrawer.vue`、`business-application.js`。
- **验收**：单表零额外关系配置；左树只显示树字段配置；主子表支持一至多个子表配置；其他初始化方式继续可用。
- **执行结果**：模板线框提升为初始化主入口；左树只展示树对象/显示字段/父级字段/主表关联字段，主子表支持动态子表清单；空白、数据库表和已有对象保留在“其他起点”。

### Task 48：轻表设计入口收口

- **目标**：简易数据库表导入统一走应用/对象向导，独立模型设计从主导航降级隐藏但保留旧路由/API。
- **涉及文件**：`V1.0.33__hide_legacy_lowcode_model_menu.sql`、相关说明文案。
- **验收**：新用户主导航不再看到重复入口；旧收藏和存量模型不被删除。
- **执行结果**：新增 `V1.0.33`，只隐藏 `/ai/lowcode-models` 菜单显示，不删除资源、路由、API 或模型数据；简易表导入继续由应用/对象向导承接。

### Task 49：无页面入口发布与默认入口选择

- **目标**：缺少入口只提醒不阻断；默认发布不全选停用或缺配置入口。
- **涉及文件**：`BusinessApplicationReadinessService`、`BusinessApplicationAssetSelectionService`、发布检查测试源码。
- **验收**：零入口应用可通过应用级发布预检查；对象、数据库和权限硬门保持不变。
- **执行结果**：完整发布检查和工作台轻量检查均把缺少入口降为提醒；默认发布入口集合只包含启用且运行配置完整的入口，并补充对应测试源码。

### Task 50：应用草稿预览

- **目标**：应用头部提供显式预览，直接渲染主对象或选中对象草稿，不依赖菜单和访问入口。
- **涉及文件**：应用预览路由/页面、`ApplicationWorkspaceHeader.vue`、`application.[applicationCode].vue`。
- **验收**：无入口、未发布对象也可打开配置预览；多对象可切换；无对象显示下一步。
- **执行结果**：原独立 `LowcodePreviewPane` 草稿页已被后续 Task 52 的真实页面预览替代；`V1.0.34` 和旧路由仅保留已执行迁移兼容，不再作为工作台预览目标。

### Task 51：工作台顶部留白压缩与静态收口

- **目标**：删除重复命令栏、合并返回动作、压缩头部和内容间距，并完成静态检查和文档回填。
- **验收**：`objects` 深链首屏直接显示应用头部和对象内容；无额外顶部空白；按用户分工不执行构建和浏览器验证。
- **执行结果**：删除页面内重复命令栏，把返回、预览和发布集中到应用头部；头部、主体高度和内容间距已压缩。完成目标引用、Flyway placeholder、迁移版本和差异空白静态检查；未执行构建或真实验证。

### Task 52：模板来源选择与真实 CRUD 预览修正

- **目标**：模板内的主对象、树对象和明细对象改为数据库表/已有对象选择；关系字段改为真实字段下拉；工作台预览直接打开真实 CRUD 页面。
- **涉及文件**：模板来源 DTO/服务、数据源字段查询接口、`ApplicationInitializeStep.vue`、来源选择组件、`ApplicationEditorDrawer.vue`、`application.[applicationCode].vue`、CRUD 配置与动态数据预览权限链路。
- **验收**：不再手填对象/表身份；左树和主子表只允许选择来源中存在的字段；未发布预览要求设计权限；正常运行链路不放宽；模板标题与徽标显示完整。
- **执行结果**：已新增统一对象来源协议和数据库表字段查询接口；主对象、树对象、每个明细对象均可选择数据库表或当前业务域已有对象，树字段和主外键从真实字段下拉选择。工作台预览改为直接打开 `/ai/crud-page/:configKey?designPreview=1`，草稿配置和动态数据访问均要求对象设计权限，正式运行发布门禁不变。模板线框高度缩小并锁定边框盒模型，标题区和推荐徽标设置稳定布局；来源切换改为自有两列分段按钮，避免 Naive 单选按钮在抽屉中压缩重叠。按用户分工仅完成静态检查，真实页面由用户验收。

### Task 53：业务字段显式类型优先级修复

- **目标**：修复数据库导入字段或设计器已明确字段类型后，仍被中文字段名称推断覆盖并误报“字典字段必须配置字典类型”的保存阻断。
- **涉及文件**：`BusinessFieldSchemaService.java`、`BusinessFieldSchemaServiceTest.java`。
- **验收**：显式 `TEXT/SWITCH/NUMBER` 等类型不受“状态/类型/金额”等名称规则影响；字段类型缺失时仍允许名称推断；真正字典组件未配置字典或静态选项时继续阻断，并返回具体字段名称。
- **执行结果**：字段类型归一化调整为“显式类型优先，名称推断仅用于类型为空”；未知显式类型安全降级为 `TEXT`，不再触发名称推断。字典校验文案补充字段名称和可选修复方式；新增显式 TEXT、SWITCH、未知类型和真实字典字段回归测试源码。按用户分工不运行 JUnit，由用户完成真实保存验证。

## Phase 8：应用草稿图预览与字段配置分层

### Task 54：主子表应用草稿图编译

- **目标**：模板初始化后无需对象发布即可预览最新主子关系，复用已有已发布对象时也不读取旧运行 Schema。
- **涉及文件**：`AiCrudConfigService.java`、`BusinessObjectDesignerService.java`、草稿渲染相关测试源码。
- **实现步骤**：设计预览强制从当前 `modelSchema/pageSchema` 构建运行配置；加载主对象草稿时根据当前关系和子对象草稿重新水合页面模型引用；正式运行仍读取发布快照。
- **验收**：主子表初始化、复用已发布对象、子字段变更三类场景均能通过同一 `designPreview=1` 链路看到最新草稿。
- **执行结果**：`designPreview=true` 现在先按 `configKey` 定位对象并调用 `prepareRuntimeDraft`，根据最新关系和子对象字段刷新主对象草稿图，再强制从当前 `modelSchema/pageSchema` 编译运行配置；普通运行仍走不可变发布版本。应用协调发布在权威检查前只刷新 PRIMARY 对象聚合图，不要求用户逐个进入对象发布。已补静态契约测试源码，真实预览保持 `pending-user`。

### Task 55：字段资产与页面用法写入边界

- **目标**：停止表单/列表页面属性无条件反写全局字段，同时保留字段语义、字段重命名和数据库映射的全局传播。
- **涉及文件**：`BusinessObjectDesignerService.java`、`BusinessFormDesigner.vue`、`ForgePropertyPanel.vue`、字段/表单协议测试源码。
- **实现步骤**：表单编译只生成页面 `fieldSettings`；仅在新字段自动创建或用户显式编辑字段资产时更新字段定义；页面控件、标题、提示、只读、隐藏和表单校验保持在表单 Schema；列表继续使用页面 `fieldSettings`。
- **验收**：表单页面属性不改变列表或字段数据库映射；字段资产语义变更仍能被页面默认继承；既有 Schema 可继续读取。
- **执行结果**：对象保存不再用表单 Schema 归一化整批字段，表单编译不再调用 `applyFormDesignerSchemaToModel`；既有字段从表单移除时继续保留在字段资产，新拖入且不存在的字段才根据组件生成初始定义。控件切换、页面校验和页面最大长度只修改当前表单组件；显式“保存字段”仍可更新字段资产。字段编码重命名、数据库映射和公式等全局能力保持原协议。

### Task 56：字段与数据库映射工作台重构

- **目标**：用与表单/列表设计器一致的字段列表和固定属性栏替换中央属性弹窗，并清楚区分字段定义与页面使用情况。
- **涉及文件**：`BusinessFieldManager.vue`、`BusinessFieldPropertyPanel.vue`，必要时新增共享字段摘要组件。
- **实现步骤**：桌面端选中字段后直接在右侧编辑；窄屏使用抽屉；属性分为业务定义、数据库、规则与安全；页面显示开关降级为默认建议并补说明；展示表单/列表使用摘要和同步状态。
- **验收**：普通字段编辑不打开中央大弹窗；小屏可滚动；主题适配；保存、放弃、切换字段和系统字段只读行为保持完整。
- **执行结果**：删除普通字段属性中央大弹窗，桌面端改为 `字段列表 + 420～460px 固定属性栏`，980px 以下只挂载同一属性面板的右侧抽屉；保留未保存切换/关闭保护。头部统计收敛为业务字段、系统字段、结构一致三项紧凑摘要，颜色改用 Forge/Naive 主题变量。属性页签收敛为“业务定义 / 规则与安全 / 数据库与开发”，公式与调试只保留在表单设计器的字段公式入口；移除全局“显示在表单、显示在列表、作为查询条件”开关；默认控件和默认查询方式明确只供新页面继承。字段列表只展示是否已进入当前表单，新建字段弹窗明确页面配置需到对应设计器完成。

### Task 57：Phase 8 静态收口

- **目标**：补充测试源码、变更日志和长期决策，完成目标引用与差异空白检查。
- **涉及文件**：`test-spec.md`、`execution-log.md`、`code-copilot/memory/decisions.md`、相关测试文件。
- **验收**：静态检查无空白错误；未执行项明确记录为 `pending-user`，不宣称真实运行通过。
- **执行结果**：新增 `BusinessApplicationDraftPreviewContractTest` 测试源码，覆盖草稿强制编译、预览前关系图刷新、发布前 PRIMARY 刷新和页面配置不反写字段资产契约；同步测试矩阵、执行日志、架构决策和踩坑记录。按用户分工仅执行目标 `rg` 与差异空白检查，不运行 Maven、JUnit、Lint、build、API、数据库、Vite 或浏览器。

## Phase 9：字段属性、关系画布与应用概览密度优化

### Task 58：字段业务定义重排与必填默认值

- **目标**：降低固定属性栏的信息拥挤，并在用户开启必填时补充可解释的类型默认值。
- **涉及文件**：`BusinessFieldPropertyPanel.vue`，必要时同步字段新增表单。
- **实现步骤**：业务定义拆为字段身份、默认值与提示、数据约束、关联配置和备注；开发属性移回数据库页；增加 `updateRequired` 与类型默认值解析函数，只对可安全推断类型自动赋值。
- **验收**：右栏无横向重叠；已有默认值不覆盖；系统字段只读；引用/字典字段不写伪造值。
- **执行结果**：业务定义已拆为字段身份、默认值与提示、数据约束、关联对象和业务说明五个中性分组，字段编码与小数位回到数据库开发语义。必填开关移动到默认值分组；数字/金额/开关自动填 `0`，日期和日期时间填当前本地值，复选/文件/图片填 `[]`，文本填 `-`。自动值通过本地标记追踪，切换为不可安全推断类型时会撤销自动值；既有或用户手填默认值不覆盖，字典、人员、部门、地区和引用字段显示人工选择提示。

### Task 59：可编辑 ER 关系画布

- **目标**：在既有 ER 图上提供字段拖线创建/更新关系和连线选择能力。
- **涉及文件**：`LowcodeErDiagram.vue`、`BusinessRelationDesigner.vue`。
- **实现步骤**：ER 图增加可选编辑模式、字段连接点、连接预览线、关系选中事件；父组件把连接端点归一化为当前对象到目标对象的 `DETAIL` 关系，复用现有保存协议和自动默认值。
- **验收**：对象卡拖动与字段拖线不冲突；非法连线拒绝；点击已配置连线打开对应关系；无新依赖。
- **执行结果**：既有 `LowcodeErDiagram` 增加可选编辑模式、字段连接点、贝塞尔预览线、关系命中区和选中态，不新增依赖。关系页默认打开可视化关系图，用户先选择画布对象并按需加载字段，再从任意方向拖线；父层统一归一化为当前对象到目标对象的 `DETAIL` 关系。同对象自连、目标对象互连和跨业务域目标被拒绝；点击已有配置线或标签会打开对应关系属性。

### Task 60：关系表单与字段联动排版

- **目标**：把右侧基础信息、新增关系和级联表单改为端点清晰、主次分明的紧凑配置。
- **涉及文件**：`BusinessRelationDesigner.vue`。
- **实现步骤**：关系身份与字段端点拆区；新增关系向导按目标、端点、展示配置分段；级联采用控制字段到目标字段的流式布局，远程/字典细节单独成组；高级能力继续折叠。
- **验收**：桌面和窄屏不重叠；主要关系字段首屏可见；高级设置不抢占首屏。
- **执行结果**：关系属性改为关系身份、状态和 `当前对象字段 1 → N 目标对象字段` 端点卡，回显字段单独放置；选择器、映射、审批数量仍在折叠区。新增关系向导分为选择目标、确认字段端点、页面展示三段，移除只有一个选项的关系类型下拉。字段联动改为“控制字段 → 联动方式 → 目标字段”，字典/远程数据源和空值/清空/启用策略分别成组；窄屏降级为单列。

### Task 61：应用概览密度收口

- **目标**：减少应用工作台和概览面板的嵌套 padding、gap 与空状态高度。
- **涉及文件**：`application.[applicationCode].vue`、`ApplicationWorkspaceHeader.vue`、`ApplicationOverviewPanel.vue`。
- **实现步骤**：压缩外层边距、概览内容 padding、标题间距、行高和空状态；同步头部高度与工作区高度计算；小屏保持可滚动。
- **验收**：首屏信息密度提高；无内容遮挡；主题变量保持。
- **执行结果**：工作台外边距由 16px 降为 8px，内容区由 16px 降为 12px、概览顶部进一步收敛；头部由 72px 降为 60px，侧栏由 218px 降为 204px并同步高度计算。概览分区 gap 由 20px 降为 12px，配置行由 44px 降为 38px，问题行和空状态同步压缩；小屏仍保留单列内容和横向导航。

### Task 62：Phase 9 静态收口

- **目标**：记录实施结果、目标引用和差异空白检查，真实交互交由用户验收。
- **涉及文件**：`execution-log.md`、相关目标文件。
- **验收**：静态扫描无残留旧布局和 whitespace error；未执行项标记 `pending-user`。
- **执行结果**：完成必填默认值、ER 编辑事件、关系草稿构建、画布对象按需加载和概览密度目标扫描；删除不再使用的关系类型选项、类型更新函数和旧拖动处理函数。目标已跟踪文件差异空白检查通过；按用户分工不运行 Lint/build/Vite/浏览器，真实交互保持 `pending-user`。

### Task 63：应用总览紧凑卡片网格

- **目标**：用应用主体卡片消除六列表格在中小屏上的横向滚动和操作列不可见问题。
- **涉及文件**：`ApplicationTable.vue`、`app-center/index.vue`。
- **实现步骤**：保留组件事件协议和分页数据源，把表格头/行改为 268～320px 自适应卡片；固定呈现身份、状态、资产计数和底部操作；移除 ResizeObserver 宽度分支与 1180px 最小宽度；滚动容器改为仅纵向滚动。
- **验收**：卡片不会随单条数据拉满整行；4/3/2/1 列稳定；操作始终可见；分页和筛选行为不变。
- **执行结果**：`ApplicationTable` 在不改变组件名和事件协议的前提下改为紧凑应用卡片网格，轨道以 268px 为最低宽度并等分当前行剩余空间；修正原 `minmax(268px, 320px)` 按固定上限计算列数、可放三列却只显示两列的问题。`auto-fill` 继续保留宽屏空轨道，少量数据不会拉满整行。卡片高度约 166px，包含应用身份、设计状态、单行说明、业务域、更新时间、四类资产计数、启停状态/版本和固定底部操作。“草稿”状态标签使用跟随主题的低饱和暖色背景区分，问题状态原有警示仍保留。移除表头、六列宽度、ResizeObserver 和 1180px 最小宽度分支；容器改为仅纵向滚动，筛选与分页保持原逻辑。

## Phase 10：应用级完整代码包

### Task 64：草稿生成配置强制重编译

- **目标**：让现有 Velocity 生成器严格读取当前单表、左树右表和主子表配置，不被旧运行字段覆盖。
- **涉及文件**：`LowcodeCodegenService.java`、既有代码生成契约测试源码。
- **实现步骤**：存在统一模型/页面 Schema 时重新生成查询、列表、表单、接口和 options；草稿应用生成前刷新主对象关系图；保留代码包设置但让最新运行配置覆盖旧派生字段。
- **验收**：三类布局分别进入现有模板分支；主子表和树配置使用最新对象关系及字段；关联元数据被完整渲染，Java 产物无未解析模板变量。
- **执行结果**：`LowcodeCodegenService` 在存在统一模型/页面协议时不再复用旧 `layoutType/searchSchema/columnsSchema/editSchema/apiConfig`，最新页面 Schema 编译出的布局强制覆盖旧 `simple-crud`，运行派生 options 也覆盖旧派生字段但保留代码包设置；发布来源复用 `AiCrudConfigService.resolvePublishedRuntimeConfig` 读取已发布快照。Velocity 继续使用同一策略，根据最新 `treeConfig/masterDetailConfig` 进入单表、左树右表或主子表分支。增量修复主子表派生配置为空时静默退化为单表的问题：优先读取 `masterDetailConfig.children`，并从 `pageSchema.modelRefs` 的 `CHILD_LIST/DETAIL/ONE_TO_MANY` 关系补齐主外键；仍无法解析时直接阻断并提示检查子表关系。再次修正关联元数据私有内部类导致 Velocity 无法访问 getter 的问题，将模板元数据改为公开静态类型；Java 产物增加未解析变量扫描，渲染失败改为整体失败，不再跳过文件。

### Task 65：应用级批量代码组装服务

- **目标**：按应用和批量对象选择组装单个可部署 ZIP，并复用现有对象生成器。
- **涉及文件**：`BusinessApplicationCodegenService.java`、`BusinessApplicationController.java`、代码生成请求/预览 VO、共享业务接口配置装配类。
- **实现步骤**：读取应用对象角色；主对象聚合消费 DETAIL/REFERENCE 依赖；其余对象独立生成；合并文件时检测冲突；增加应用 README/manifest；提供 options、preview、download API。
- **验收**：默认全选、支持对象 ID 批量选择、无入口也可生成；主子表明细对象有独立 Service；同路径不同内容不会被覆盖；发布来源不降级草稿。
- **执行结果**：新增应用级组装服务和 `/ai/business/application/{id}/code/*` 设置、预览、下载接口。默认读取应用全部对象，也接受 `objectIds` 批量范围；选中 PRIMARY 时刷新关系草稿并消费已进入主子/左树页面的 DETAIL/REFERENCE 依赖，其余对象复用现有对象生成器独立生成。聚合依赖除 Entity/Mapper 外继续生成 DTO、Query、Service 和 ServiceImpl；主 Service 仍通过 Mapper 在同一事务内执行明细查询、插入、替换和清理，遵循 Service 间不互相注入约束。文件按可部署目录合并，同路径不同内容直接阻断；ZIP 增加应用 README 和 manifest。新增 `V1.0.35` 代码设置/预览/下载权限，只继承应用编辑角色。

### Task 66：复用代码预览工作台接入应用管理

- **目标**：应用总览和工作台都能先预览、再批量下载完整代码。
- **涉及文件**：`AppCodePanel.vue`、`ApplicationTable.vue`、`app-center/index.vue`、`ApplicationWorkspaceHeader.vue`、`application.[applicationCode].vue`、`business-application.js`。
- **实现步骤**：现有预览面板增加应用 scope 和对象多选；复用文件树、CodeMirror 和下载方法；设置变化使预览失效；未预览时禁用下载；两处应用入口打开同一面板。
- **验收**：不复制预览组件；应用对象可多选；下载参数与最后一次预览一致；小屏可滚动。
- **执行结果**：现有 `AppCodePanel` 增加 `APPLICATION` scope，继续复用原文件树、CodeMirror、复制和下载逻辑，只增加应用摘要与对象复选区。应用总览更多操作和应用工作台头部都打开同一面板；来源、包设置或对象选择变化后下载立即禁用，只有当前参数成功预览后才允许下载当前预览。增量将应用模式的代码包设置改为默认收起，展开后的设置内容独立滚动，对象区同步压缩，并为文件树/源码区保留 300px 最小高度。

### Task 67：Phase 10 静态收口

- **目标**：补充生成契约测试源码、执行记录和静态检查，不代替用户真实验收。
- **涉及文件**：`test-spec.md`、`execution-log.md`、代码生成契约测试。
- **验收**：单表/树形/主子/批量组装关键引用存在；目标文件无 whitespace error；未执行项标记 `pending-user`。
- **执行结果**：新增 `BusinessApplicationCodegenContractTest` 测试源码，覆盖统一生成器复用、最新 Schema 覆盖、三类布局模板、Mapper XML 和应用 API 契约；增量增加主子表页面关系兜底、失败关闭、关联类型实际渲染、子表独立 Service 文件和主 Service 聚合方法契约。完成模板指令平衡、Flyway placeholder、Mapper XML、目标引用和已跟踪/未跟踪文件差异空白检查；按用户分工未运行 Maven/JUnit、前端 Lint/build、API、Flyway、Vite 或浏览器，真实生成与解压运行保持 `pending-user`。

## Phase 11：低代码协议与下载代码自动适配

### Task 68：完整协议快照与覆盖门禁

- **目标**：为每个生成对象输出可重放的完整协议、前后端运行配置和覆盖报告，禁止 JSON 丢失或解析失败后继续下载。
- **涉及文件**：新增 `LowcodeProtocolSnapshotBuilder.java`，修改 `VelocityCodegenStrategy.java`、`BusinessCodegenConfigAssembler.java`、应用代码包 manifest/README。
- **实现步骤**：以 `AiCrudConfig` 为载体序列化全部顶层字段；将 model/page/search/columns/edit/api/options/dict/desensitize/encrypt/trans 解析为结构化 JSON；生成独立 `generated_*` 键；输出 frontend/backend/protocol/coverage 四类资产并校验存在性。
- **验收**：未来嵌套字段无需维护模板白名单即可原样保留；低代码 model/page 缺失或 JSON 非法时生成失败；manifest 记录运行契约和覆盖报告路径。
- **执行结果**：已新增完整协议快照和覆盖报告构建器，前端配置将 model/page/search/columns/edit/api/options/dict/desensitize/encrypt/trans 转为结构化 JSON，后端保留可反序列化配置；ZIP 固定输出 frontend runtime、backend classpath、protocol、coverage 四类资产。公共 Velocity 入口增加 `GeneratedLowcodeRuntimeConfigBuilder`，普通旧配置键也会派生 `generated_*` 键，非法 JSON、缺失权威 Schema 和未改写平台通用接口均失败关闭。

### Task 69：共享前端低代码运行解释器

- **目标**：删除生成 Vue 模板中的第二套配置解释逻辑，让在线页和下载页使用相同运行实现。
- **涉及文件**：`forge-admin-ui/src/views/ai/crud-page.vue`、新增 `forge-admin-ui/src/components/lowcode-runtime/LowcodeRuntimePage.vue`、`templates/vm/ai-crud/index.vue.vm`。
- **实现步骤**：运行页支持内嵌 `runtimeConfig`；共享组件提供稳定出口；生成页面只导入本地 JSON 并传入共享组件；路由和设计预览原链路保持兼容。
- **验收**：生成模板不再出现 `transformColumns/transformFields/preloadDicts`；多表单、字段渲染、动作、Hook、详情、树和主子表继续走 `crud-page.vue` 唯一路径。
- **执行结果**：已为在线 `crud-page.vue` 增加内嵌 `runtimeConfig` 输入并新增稳定的 `LowcodeRuntimePage` 出口；生成 Vue 模板缩减为共享组件和本地 `runtime-config.json`。左树右表继续加载 `TreeCrudTemplate`，主子表继续加载 `MasterDetailCrudTemplate`，异步导出任务在存在业务 API 配置时走生成 Controller 的任务端点。

### Task 70：后端内嵌协议注册与业务 Controller 委托

- **目标**：让生成业务接口自动继承动态 CRUD 内核的当前及未来能力，同时保持业务专属 URL。
- **涉及文件**：新增 `GeneratedLowcodeConfigRegistry.java`，修改 `DynamicCrudService.java`、`templates/vm/controller.java.vm`、相关测试构造。
- **实现步骤**：启动期加载 classpath 协议配置；动态服务识别独立生成键；Controller 使用动态 CRUD/Excel 服务执行查询、树、详情、增改删、批量删和导入导出，并发布记录事件。
- **验收**：生成 Controller 无 `/ai/crud/` 路径和静态 Service 规则复制；运行时公式、自动编号、唯一约束、数据权限、加密和主子事务均由共享内核承载；配置冲突失败关闭。
- **执行结果**：已新增 classpath 运行配置注册器，`DynamicCrudService` 对 `generated_*` 键优先读取内嵌配置且不回退数据库同名配置；生成 Controller 改为委托动态 CRUD/Excel 内核并保留 POST-safe 业务 URL、业务事件发布、树查询和导入导出。业务 API 投影补齐 `/tree`、异步导出任务列表/详情，协议中的页面跳转改写为生成前端路由。

### Task 71：协议自动适配契约测试与静态收口

- **目标**：用自动化证据证明完整协议携带、共享解释器复用和运行内核委托，而不只扫描三类模板名称。
- **涉及文件**：`BusinessApplicationCodegenContractTest.java`、新增协议快照/注册器测试、`test-spec.md`、`execution-log.md`、memory。
- **实现步骤**：实际构造带未来未知嵌套字段、表单、视图、联动、树和主子关系的配置；断言快照保留、覆盖报告无 unsupported、生成模板为薄壳、Controller 调用动态内核、内嵌键与原键隔离；执行允许范围内的静态检查并记录跳过项。
- **验收**：契约测试源码覆盖未来字段透传和失败关闭；目标文件无 whitespace error；真实 ZIP/服务/浏览器状态明确标记 `pending-user`。
- **执行结果**：已新增运行配置规范化、协议快照和 classpath 注册器测试源码，并扩展应用代码生成契约，覆盖普通键自动派生、未来嵌套字段透传、页面/API 路由改写、树与导出任务业务端点、非法协议和键冲突失败关闭。目标引用、薄模板、Velocity 变量、已跟踪与未跟踪文件差异空白检查无错误；按既定分工未运行 Maven/JUnit、前端构建、服务、数据库或浏览器，真实 ZIP 解压、生成模块编译和三布局运行保持 `pending-user`。

## Phase 12：下载后端静态协议编译与可持续二次开发

### Task 72：静态 Service Controller 恢复

- **目标**：下载 Controller 改为调用生成的 MyBatis-Plus Service，在线动态 CRUD 保持不变。
- **涉及文件**：`templates/vm/controller.java.vm`、`service.java.vm`、`serviceImpl.java.vm`、`mapper.java.vm`、`mapper.xml.vm`。
- **实现步骤**：恢复类型化 DTO/Query/Entity；保留 POST-safe 接口；分页/列表/树/主子查询走 Mapper XML；写操作和主子事务进入生成 Service。
- **验收**：生成后端无 `DynamicCrudService`/`DynamicCrudExcelService`；三类布局静态代码契约完整；Service 无 `LambdaQueryWrapper`。
- **状态**：completed。
- **执行结果**：生成 Controller 已恢复类型化 DTO/Query/Entity 和 `I${className}Service` 调用；ServiceImpl 继承 MyBatis-Plus `ServiceImpl`，基础增改删使用 Mapper/MP 内置方法，分页、列表、树、主子明细查询继续位于 Mapper XML。查询编译补齐 `eq/ne/like/left_like/right_like/gt/ge/gte/lt/le/lte/in/between`，IN/BETWEEN 在 Query DTO 使用类型化 List。主子写入、删除和导入保持 Service 事务，Controller 不再引用动态 CRUD 服务。

### Task 73：不覆盖的业务扩展链

- **目标**：允许用户完整替换查询或增强复杂新增，且重新下载不覆盖用户实现。
- **涉及文件**：新增 ServiceExtension 与示例模板，修改 `VelocityCodegenStrategy.java`、ServiceImpl、README 和 ownership 资产。
- **实现步骤**：为主生成对象渲染 around 扩展接口；ServiceImpl 组装有序扩展链；正式用户实现目录不进入生成集合；输出 `.java.example` 和文件所有权清单。
- **验收**：无扩展执行默认逻辑；扩展可调用或跳过 `proceed`；用户实现路径不出现在 `GENERATED` 文件列表。
- **状态**：completed。
- **执行结果**：新增主对象 `ServiceExtension` 默认方法契约和有序 around 调用链，使用 `ObjectProvider.orderedStream()` 保证零扩展 Bean 也能执行默认逻辑。扩展示例只输出到 `examples/...java.example`，正式 custom 目录不进入生成文件集合；每个对象新增 ownership 清单，明确 `GENERATED`、`CREATE_ONCE_SAMPLE` 与 `NEVER_GENERATED_NEVER_OVERWRITTEN`。新增 `LowcodeStaticCodegenContributor`，后续后端协议能力可由统一生成策略自动装配到所有下载入口。

### Task 74：静态导入导出与协议资产修正

- **目标**：导入导出改用生成 Service 和 Forge Excel starter，移除下载后端 classpath 动态配置依赖。
- **涉及文件**：`controller.java.vm`、`sql/excel.sql.vm`、运行配置/协议构建器、`DynamicCrudService.java`、classpath 注册器及测试。
- **实现步骤**：导出元数据指向生成 Service；导入解析后在 Service 事务中新增；删除异步动态 CRUD 任务 URL；移除 registry 和 `META-INF/forge-lowcode` 产物；coverage 改为静态编译语义。
- **验收**：在线动态 CRUD 不受影响；下载 Controller/资源无 classpath 运行配置依赖；Excel SQL 可重复执行。
- **状态**：completed。
- **执行结果**：生成 Controller 的导出改用 `forge-starter-excel` 元数据引擎，数据源 Bean 显式指向生成 Service；导入由标准 Excel 服务解析 DTO 后调用生成 Service 的事务导入方法。`excel.sql.vm` 已使用 `WHERE NOT EXISTS` 并由公共策略输出。删除 classpath 注册器、DynamicCrudService 内嵌配置分支、后端 `META-INF/forge-lowcode` 资产和异步动态导出任务 API；在线动态 CRUD 继续从数据库读取配置。coverage 改为静态编译语义，公式、自动编号、唯一校验和任意组合查询等未内建语义明确列入 `REQUIRES_EXTENSION`，不再误报完整覆盖。

### Task 75：Phase 12 契约测试与静态收口

- **目标**：固化静态 Service、扩展所有权和未来能力统一编译入口。
- **涉及文件**：`BusinessApplicationCodegenContractTest.java`、协议构建测试、`test-spec.md`、`execution-log.md`、memory。
- **验收**：目标引用、模板渲染和差异空白检查通过；真实 ZIP/编译/运行按边界标记 `pending-user`。
- **状态**：completed。
- **执行结果**：契约测试源码已改为断言静态 Controller、MyBatis-Plus Service、Mapper XML、扩展所有权、静态 Excel、无 classpath 运行资源和统一编译贡献器。使用 Apache Velocity 2.3 对 simple/master/tree 三类 Controller、Service、Extension、Excel、README 模板做真实静态渲染，另对全部查询操作符渲染 Query/Mapper XML；均无未解析变量。`xmllint`、目标引用、tracked/untracked 差异空白检查无错误；按既定边界未运行 Maven/JUnit、前端 build、服务、数据库和浏览器，真实 ZIP 与运行保持 `pending-user`。

## Phase 13：下载包命名与输出策略

### Task 76：脱敏注解条件收敛

- **目标**：无真实脱敏策略时不生成字段注解和实体 import。
- **涉及文件**：`VelocityCodegenStrategy.java`、`templates/vm/entity.java.vm`、生成契约测试。
- **实现步骤**：统一规范化策略值；排除空值和 `NONE`；仅在匹配到真实字段时打开总开关；模板增加最终保护。
- **验收**：NONE/空配置无 `Desensitize` 残留，PHONE 等真实策略仍正常生成。
- **状态**：completed。
- **执行结果**：主表和关联对象字段的脱敏类型统一去空白、转大写并把 `NONE` 归一为空；实体级 import 开关改为只统计真实生成字段，配置指向不存在字段时不再产生无用 import。实体模板保留非空且非 `NONE` 的最终保护，新增契约测试源码覆盖无策略实体。

### Task 77：统一命名与输出设置协议

- **目标**：支持实体前缀、表前缀剥离、输出目录和生成范围，并让全部下载入口自动消费。
- **涉及文件**：`LowcodeCodegenRequest.java`、`LowcodeDomainSchema.java`、三类代码生成 Service、`BusinessCodegenConfigAssembler.java`、`VelocityCodegenStrategy.java`。
- **实现步骤**：扩展 `options.codegen`；实现类名和安全相对路径规范化；统一主表/关联表类名；按后端、前端和 SQL 子项控制输出。
- **验收**：设置可保存/回显；所有入口使用同一配置；路径非法时失败关闭；关闭范围不产生对应文件。
- **状态**：completed。
- **执行结果**：新增统一命名/路径规范化工具和 `options.codegen` 字段，支持 `entityPrefix`、有序 `stripTablePrefixes`、后端 Java/Mapper XML/前端页面/前端 API 四类根目录，以及后端、前端、Excel SQL 独立范围。主表、树对象和明细对象统一通过最终 `className` 派生全部类型与文件名；非法 Java 前缀、绝对路径、空目录和 `..` 失败关闭，同路径不同内容不再静默覆盖。应用级、访问入口级、低代码应用级和旧 configKey 公共策略均消费同一协议。

### Task 78：下载设置界面

- **目标**：以紧凑分组方式暴露高价值设置，不把面板扩张为模板 IDE。
- **涉及文件**：`AppCodePanel.vue`。
- **实现步骤**：增加命名策略、输出目录和下载内容分组；表前缀使用可增删标签；设置进入 payload 和预览签名。
- **验收**：应用级/入口级共用界面；新设置变更后旧预览失效；设置区域滚动且预览区保持可用。
- **状态**：completed。
- **执行结果**：代码包设置按基础信息、命名策略、输出目录和下载内容分组；表前缀使用可增删标签，后端/前端/SQL 子项使用独立开关。全部新增字段进入保存、预览、下载和预览签名；GET 数组参数统一转为逗号协议并保留空前缀列表语义，设置区保持内部滚动。

### Task 79：Phase 13 契约测试与静态收口

- **目标**：固化零冗余注解、统一类名、路径安全和输出范围契约。
- **涉及文件**：`BusinessApplicationCodegenContractTest.java`、`test-spec.md`、`execution-log.md`、memory。
- **验收**：目标引用、模板条件和差异空白检查通过；真实 ZIP/编译/运行按边界标记 `pending-user`。
- **状态**：completed。
- **执行结果**：新增命名/路径工具单测源码并扩展应用生成契约，覆盖 NONE 脱敏、统一设置字段、输出范围和类名规则。JS/Vue script 语法检查、Velocity 指令平衡（README 6/6、Entity 17/17）、全入口引用扫描以及 tracked/untracked 差异空白检查均无错误；按既定边界未运行 Maven/JUnit、前端 build、服务、数据库或浏览器，真实 ZIP 与设置回显保持 `pending-user`。

## 4. 推荐交付节奏

每个 Phase 单独作为一个可部署里程碑：

1. 先完成 Phase 1，后台具备真实应用模型但不切 UI。
2. 再完成 Phase 2，应用总览切换为应用优先；这是用户最先能感知的改进。
3. Phase 3 建应用工作台和表优先设计，解决开发体验核心问题。
4. Phase 4 独立做安全评审后上线扩展中心，不能与普通 UI 改造混在一个大提交中。
5. Phase 5 最后补应用级发布；在此之前继续使用对象级发布能力。

任一阶段超出 Spec 时停止扩张，新增需求另立变更，不把“完整低代码平台”无限塞入当前变更。
