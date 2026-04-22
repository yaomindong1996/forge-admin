# 任务拆分 — 配置驱动 CRUD 页面
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件
- [x] ai-codegen-enhance 变更已完成（AiClient/AiClientAdapter/AiSchemaService/AiRecommendService 可用）
- [ ] MyBatis-Plus dynamicTableName 与 TenantLineInnerInterceptor 兼容性验证（Q6）

## Task 1: 数据模型与基础 CRUD（ai_crud_config 表 + 实体 + Mapper + Service + Controller）
- **目标**: 建立 ai_crud_config 表和完整的配置管理 CRUD 链路
- **涉及文件**:
    - `forge-admin/src/main/resources/sql/ai_crud_config.sql` — 新增，建表 DDL + Agent 初始数据
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiCrudConfig.java` — 新增，实体类继承 TenantEntity
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/mapper/AiCrudMapper.java` — 新增，Mapper 接口
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/AiCrudConfigDTO.java` — 新增，新增/修改 DTO
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigService.java` — 新增，配置管理 Service
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/AiCrudConfigController.java` — 新增，配置管理 Controller
- **关键签名**:
  ```java
  public AiCrudConfig getByConfigKey(String configKey);
  public void createConfig(AiCrudConfigDTO dto);
  public void updateConfig(AiCrudConfigDTO dto);
  public void deleteConfig(Long id);
  public IPage<AiCrudConfig> listPage(PageQuery pageQuery, String configKey, String tableName);
  public AiCrudConfigRenderVO getRenderConfig(String configKey);
  ```
- **依赖**: 无

## Task 2: SchemaGenerator 服务（GenTableColumn → 前端三套 Schema JSON）
- **目标**: 实现从 GenTableColumn 列表自动生成前端 searchSchema/columns/editSchema + apiConfig JSON 的转换器
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/SchemaGenerator.java` — 新增，Schema 生成服务
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/SchemaGenerateResult.java` — 新增，生成结果 DTO
- **关键签名**:
  ```java
  public SchemaGenerateResult generate(String configKey, String tableName, List<GenTableColumn> columns);
  // 内部方法
  private List<Object> buildSearchSchema(List<GenTableColumn> columns);
  private List<Object> buildColumns(List<GenTableColumn> columns);
  private List<Object> buildEditSchema(List<GenTableColumn> columns);
  private Map<String, String> buildApiConfig(String configKey);
  ```
- **依赖**: Task 1（需要 configKey 和 tableName 的关联）

## Task 3: DynamicCrudController + DynamicCrudService（运行时通用 CRUD）
- **目标**: 实现通用运行时 CRUD Controller，根据 configKey 动态执行增删改查
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/DynamicCrudController.java` — 新增，通用 CRUD Controller
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java` — 新增，通用 CRUD Service
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/DynamicCrudQuery.java` — 新增，动态查询参数 DTO
- **关键签名**:
  ```java
  // DynamicCrudController
  public RespInfo<IPage<Map<String, Object>>> page(@PathVariable String configKey, PageQuery pageQuery, DynamicCrudQuery query);
  public RespInfo<Map<String, Object>> getById(@PathVariable String configKey, @PathVariable Long id);
  public RespInfo<Void> create(@PathVariable String configKey, @RequestBody Map<String, Object> data);
  public RespInfo<Void> update(@PathVariable String configKey, @RequestBody Map<String, Object> data);
  public RespInfo<Void> delete(@PathVariable String configKey, @PathVariable Long id);

  // DynamicCrudService
  public IPage<Map<String, Object>> selectPage(String tableName, PageQuery pageQuery, DynamicCrudQuery query, Object searchSchema);
  public Map<String, Object> selectById(String tableName, Long id);
  public void insert(String tableName, Map<String, Object> data, Object editSchema);
  public void updateById(String tableName, Map<String, Object> data, Object editSchema);
  public void deleteById(String tableName, Long id);
  public boolean hasDelFlag(String tableName);
  ```
- **依赖**: Task 1（需要读取 ai_crud_config 获取 tableName/searchSchema/editSchema）

## Task 4: 菜单自动注册（配置 CRUD 与 sys_resource 联动）
- **目标**: 配置创建/删除/更新时自动注册/删除/更新 sys_resource 菜单记录
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigService.java` — 修改，新增菜单联动逻辑
    - `forge-admin/src/main/resources/sql/ai_crud_config.sql` — 修改，补充 AI 管理目录菜单的初始化 SQL
- **关键签名**:
  ```java
  // AiCrudConfigService 新增内部方法
  private Long registerMenu(AiCrudConfigDTO dto, String configKey);
  private void updateMenu(AiCrudConfig config, AiCrudConfigDTO dto);
  private void deleteMenu(Long menuResourceId);
  ```
- **依赖**: Task 1（AiCrudConfigService 已创建）

## Task 5: AI 配置生成（crud_config_builder Agent + AI 生成接口）
- **目标**: 新增 crud_config_builder Agent，实现 AI 全量配置生成和已有表 AI 增强生成
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/AiCrudConfigGenerateService.java` — 新增，AI 配置生成服务
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/AiCrudGenerateRequest.java` — 新增，AI 生成请求 DTO
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/AiCrudGenerateResult.java` — 新增，AI 生成结果 DTO
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/AiCrudConfigController.java` — 修改，新增 AI 生成接口
    - `forge-admin/src/main/resources/sql/ai_crud_config.sql` — 修改，补充 Agent 初始数据 INSERT
- **关键签名**:
  ```java
  // AiCrudConfigGenerateService
  public AiCrudGenerateResult generateFromDescription(String description);
  public AiCrudGenerateResult generateFromTable(String tableName);
  private AiCrudGenerateResult callAiAndBuildResult(String prompt, String configKey);
  private AiCrudGenerateResult fallbackFromTable(String tableName, String configKey);
  ```
- **依赖**: Task 2（SchemaGenerator 用于已有表的规则生成降级 + AI 结果校验层）

## Task 6: 前端通用渲染页 + 配置加载 API
- **目标**: 新建通用 CRUD 渲染页 crud-page.vue，从后端 render 接口加载配置，传递给 AiCrudPage 渲染
- **涉及文件**:
    - `forge-admin-ui/src/views/ai/crud-page.vue` — 新增，通用 CRUD 渲染页
    - `forge-admin-ui/src/api/ai.js` — 修改，新增 crud-config 相关 API 接口
- **关键签名**:
  ```javascript
  // api/ai.js 新增
  export function crudConfigPage(params) { return request.get('/ai/crud-config/page', { params }) }
  export function crudConfigGetById(id) { return request.get(`/ai/crud-config/${id}`) }
  export function crudConfigRender(configKey) { return request.get(`/ai/crud-config/render/${configKey}`) }
  export function crudConfigAdd(data) { return request.post('/ai/crud-config', data) }
  export function crudConfigUpdate(data) { return request.put('/ai/crud-config', data) }
  export function crudConfigDelete(id) { return request.delete(`/ai/crud-config/${id}`) }
  export function crudConfigAiGenerate(data) { return request.post('/ai/crud-config/ai/generate', data) }
  export function crudConfigAiGenerateFromTable(data) { return request.post('/ai/crud-config/ai/generateFromTable', data) }
  ```
- **依赖**: Task 1（render 接口可用）、Task 3（DynamicCrudController 可用）

## Task 7: 前端配置管理页面 + AI 生成弹窗
- **目标**: 新增 CRUD 配置管理页面（列表+新增/编辑/删除）和 AI 生成配置弹窗（自然语言/已有表 → 预览 → 确认入库）
- **涉及文件**:
    - `forge-admin-ui/src/views/ai/crud-config.vue` — 新增，配置管理页面
    - `forge-admin-ui/src/views/ai/components/AiGenerateConfigModal.vue` — 新增，AI 生成配置弹窗
- **关键签名**:
  ```vue
  <!-- crud-config.vue: AiCrudPage 配置列表 -->
  <!-- AiGenerateConfigModal.vue: AI 生成配置弹窗 -->
  <!-- Props: v-model:show, @success -->
  <!-- 功能: description 输入 / tableName 选择 → AI 生成 → 预览 Schema → 确认入库 -->
  ```
- **依赖**: Task 5（AI 生成接口可用）、Task 6（API 接口定义可用）

## Task 8: 端到端集成验证 + 回归测试
- **目标**: 完整链路验证：已有表导入 → AI 生成配置 → 确认入库 → 菜单出现 → 点击菜单 → CRUD 页面渲染 → 增删改查正常
- **涉及文件**:
    - 无新增文件，修复集成过程中发现的问题
- **验证清单**:
  - [ ] 已有表导入 → AI 生成配置 → 预览 → 确认入库
  - [ ] 自然语言输入 → AI 生成配置 → 预览 → 确认入库
  - [ ] 配置入库后菜单自动出现
  - [ ] 点击菜单 → crud-page 渲染 → 搜索/分页/新增/编辑/删除正常
  - [ ] DictTag/AuthImage 渲染正确
  - [ ] 租户隔离：A 租户看不到 B 租户的配置和数据
  - [ ] 字段白名单：非法字段不写入
  - [ ] 逻辑删除：有 del_flag 的表走 UPDATE，无 del_flag 走 DELETE
  - [ ] AI 降级：不配置 Provider → 已有表走规则生成 / 自然语言提示不可用
  - [ ] 回归：现有代码生成器功能不变
- **依赖**: Task 1-7 全部完成
