# AGENTS.md
> AI 编程助手（OpenCode / DeepSeek TUI）工作指引 — 进入仓库后首先阅读本文件

---

## 1. 项目概述

**Forge Admin** — 基于 Vue3 + Spring Boot 3 的企业级中后台管理框架，微内核插件化架构。
- **后端**: Java 17 + Spring Boot 3.2 + MyBatis-Plus 3.5 + Sa-Token 1.38 + Flowable 7.0
- **前端**: Vue 3.5 + Naive UI 2.42 + Vite 7 + Pinia 3 + UnoCSS 66
- **数据库**: MySQL 8.0+ / Redis 6.0+
- **构建**: Maven (后端) + pnpm (前端)
- **核心能力**: RBAC 权限、多租户隔离、AI 代码生成、Flowable 工作流、消息中心、AI 数据大屏

```
forge/                          # 后端根目录
├── forge-admin-server/         # 主应用入口（Spring Boot）
├── forge-report-server/        # 大屏报表服务
├── forge-app-server/           # App 接口服务
├── forge-flow/                 # 独立流程引擎服务
├── forge-framework/            # 核心框架层（插件 + 启动器）
│   ├── forge-plugin-parent/    # 业务插件（system/generator/job/message/flow/ai）
│   └── forge-starter-parent/   # 技术启动器（auth/cache/orm/tenant/crypto … 共 20 个）
├── forge-business/             # 业务模块
forge-admin-ui/                 # 前端主项目
forge-docs/                     # VitePress 文档站
code-copilot/                   # AI 辅助编码规则 & 变更管理
.opencode/                      # OpenCode 配置 & 记忆文件
```

---

## 2. 快速命令

### 2.1 渐进式开发流程（推荐）

> 遵循 **No Spec No Code** 原则。所有变更产物存放在 `code-copilot/changes/[变更名]/`

| 命令 | 用途 |
|------|------|
| `/spec-init` | 初始化项目上下文 |
| `/propose <需求>` | 创建变更提案（生成 spec.md + tasks.md） |
| `/apply <变更名>` | 按 Spec 执行编码 |
| `/fix <变更名>` | Review 后增量修正 |
| `/review <变更名>` | 两阶段审查（Spec 合规 + 代码质量） |
| `/test <变更名>` | 按自动化测试标准增量生成/执行测试 |
| `/archive <变更名>` | 归档并沉淀知识 |

> 执行 `/test`、阶段收尾验证、Review 后修复验证或归档前验收时，必须先读取 `code-copilot/rules/automated-testing-standard.md`，复用当前变更已有 `test-spec.md`、`execution-log.md`、`spec.md`、`tasks.md`，按本轮差异做增量验证；禁止每次从零开始重新规划测试流程。

### 2.2 后端

```bash
# 构建全项目（跳过测试加速）
cd forge && mvn clean install -DskipTests

# 启动 admin 服务（默认 localhost:8580）
cd forge/forge-admin-server && mvn spring-boot:run

# 启动 flow 服务（默认 localhost:8581）
cd forge/forge-flow && mvn spring-boot:run

# 指定环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2.3 前端

```bash
cd forge-admin-ui

# 安装依赖
pnpm install

# 开发模式（默认 localhost:5173）
pnpm dev

# 生产构建
pnpm build

# Lint & 自动修复
pnpm lint:fix
```

> 默认登录凭证：`admin` / `123456`

### 2.4 环境变量

| 文件 | 用途 |
|------|------|
| `forge/forge-admin-server/src/main/resources/application-dev.yml` | 后端本地配置（数据库/Redis） |
| `forge/forge-admin-server/src/main/resources/application-dev.example.yml` | 后端配置模板（可提交） |
| `forge-admin-ui/.env.local` | 前端本地环境变量 |
| `forge-admin-ui/.env.example` | 前端环境变量模板（可提交） |

---

## 3. 后端架构

### 3.1 完整模块树

```
forge/
├── forge-admin-server/                         # 【主应用】Spring Boot 入口，聚合所有插件
│   └── src/main/java/com/mdframe/forge/admin/
│       ├── controller/                          # REST 控制器
│       ├── service/                             # 业务服务
│       └── config/                              # 应用配置
│
├── forge-framework/                             # 【框架层】不依赖具体业务
│   ├── forge-dependencies/                      # 统一依赖版本管理（BOM）
│   │
│   ├── forge-plugin-parent/                     # 【业务插件】可插拔功能模块
│   │   ├── forge-plugin-system/                 # 系统管理（用户/角色/菜单/部门/岗位/租户/字典）
│   │   ├── forge-plugin-generator/              # 代码生成器（AI 驱动）
│   │   ├── forge-plugin-job/                    # 定时任务（Quartz / SnailJob）
│   │   ├── forge-plugin-message/                # 消息中心（站内信/邮件/短信）
│   │   ├── forge-plugin-flow/                   # 流程引擎（Flowable）
│   │   └── forge-plugin-ai/                     # AI 供应商管理
│   │
│   └── forge-starter-parent/                    # 【技术启动器】底层能力封装
│       ├── forge-starter-core/                  # 核心工具类、异常、统一响应
│       ├── forge-starter-web/                   # Web 层封装（Undertow + 全局异常处理）
│       ├── forge-starter-auth/                  # 认证授权（Sa-Token + 权限注解）
│       ├── forge-starter-orm/                   # ORM（MyBatis-Plus + 动态数据源 + 分页）
│       ├── forge-starter-cache/                 # 缓存（Redis + Redisson 分布式锁）
│       ├── forge-starter-tenant/                # 多租户（ TenantLineInnerInterceptor ）
│       ├── forge-starter-datascope/             # 数据权限（ DataScopeInterceptor ）
│       ├── forge-starter-crypto/                # API 加解密（@ApiEncrypt / @ApiDecrypt）
│       ├── forge-starter-excel/                 # Excel 导入导出（EasyExcel）
│       ├── forge-starter-file/                  # 文件存储（OSS / RustFS / 本地）
│       ├── forge-starter-log/                   # 操作日志（@OperationLog）
│       ├── forge-starter-idempotent/            # 幂等性（注解 + Redisson 分布式锁）
│       ├── forge-starter-id/                    # 分布式 ID（雪花算法）
│       ├── forge-starter-config/                # 动态配置刷新（@RefreshScope）
│       ├── forge-starter-trans/                 # 分布式事务
│       ├── forge-starter-social/                # 社交登录
│       ├── forge-starter-websocket/             # WebSocket
│       ├── forge-starter-message/               # 消息服务
│       ├── forge-starter-job/                   # 任务调度基础设施
│       ├── forge-starter-api-config/            # API 行为动态配置
│       └── forge-flow-client/                   # 流程客户端（@FlowBind / @FlowStart / @FlowCallback）
│
├── forge-flow/                                  # 独立流程服务（可选部署）
├── forge-report-server/                         # 大屏报表服务
├── forge-app-server/                            # App 接口服务
└── forge-business/                              # 业务模块
```

### 3.2 标准分层架构

```
Controller 层  → 接收请求、参数校验、协议转换
    ↓
Service 层     → 业务编排、事务边界（禁止互相注入导致循环依赖）
    ↓
Manager 层     → [可选] 领域能力、单一职责、可复用
    ↓
Mapper 层      → 纯数据访问（MyBatis-Plus + XML）
```

**包结构约定**（每个 plugin 内部）：
```
plugin-xxx/
├── controller/    # REST 控制器
├── service/       # 服务接口
│   └── impl/      # 服务实现
├── mapper/        # MyBatis Mapper 接口 + XML
├── entity/        # 数据库实体
├── dto/           # 请求 DTO
├── vo/            # 响应 VO
├── constant/      # 常量
└── listener/      # 事件监听器
```

### 3.3 核心子系统速查

| 子系统 | 关键类/注解 | 文档 |
|--------|------------|------|
| 认证授权 | `SaTokenInterceptor`, `@SaCheckPermission` | `forge-starter-auth` |
| 多租户 | `TenantLineInnerInterceptor`（自动追加 `WHERE tenant_id = ?`） | `forge-starter-tenant` |
| 数据权限 | `DataScopeInterceptor`（按 mapperMethod 精确匹配 XML SQL 改写） | `forge-starter-datascope` |
| API 加解密 | `@ApiEncrypt`, `@ApiDecrypt` | `forge-starter-crypto` |
| 操作日志 | `@OperationLog` | `forge-starter-log` |
| 幂等控制 | `@Idempotent` | `forge-starter-idempotent` |
| 流程引擎 | `@FlowBind`, `@FlowStart`, `@FlowCallback` | `forge-plugin-flow` |
| 统一响应 | `RespInfo.success(data)` / `RespInfo.error(msg)` | `forge-starter-core` |
| 全局异常 | `GlobalExceptionHandler`（`@RestControllerAdvice`） | `forge-starter-web` |

### 3.4 前后端术语映射

| 前端（UI/组件） | 后端（API/Entity） |
|-----------------|-------------------|
| `AiCrudPage` 组件 | `GET /page`, `POST /`, `PUT /`, `DELETE /:id` |
| `DictSelect` / `DictTag` | `sys_dict_type` + `sys_dict_data` 表 |
| `RegionTreeSelect` | `sys_region_code` 表 |
| `useDict()` | `GET /system/dict/data/type/{type}` |
| `request` 工具 | `SaTokenInterceptor` 鉴权 |
| `postEncrypt` 工具 | `@ApiDecrypt` 注解 |

---

## 4. 前端架构

### 4.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5 | 组合式 API（`<script setup>`） |
| Naive UI | 2.42 | 组件库（NButton, NTable, NTree, NModal …） |
| Vite | 7 | 构建工具 + HMR |
| Pinia | 3 | 状态管理（`useUserStore`, `useAppStore` …） |
| Vue Router | 4.5 | 路由（动态路由 + 权限过滤） |
| UnoCSS | 66 | 原子化 CSS（`text-primary`, `p-4`, `flex` …） |
| Axios | 1.11 | HTTP 客户端（封装在 `@/utils/request`） |
| ECharts | 6 | 图表 |
| BPMN.js | 17 | 流程设计器 |
| CodeMirror | 6 | 代码编辑器 |

### 4.2 目录结构

```
forge-admin-ui/src/
├── api/              # API 接口定义（按模块拆分）
├── components/       # 公共组件
│   ├── ai-form/           # AI 表单组件
│   ├── ai-modal/          # AI 弹窗组件
│   ├── bpmn/              # BPMN 流程设计器
│   ├── common/            # 通用工具组件（AuthImage 等）
│   ├── file-upload/       # 文件上传
│   ├── form-designer/     # 表单设计器
│   ├── DictSelect.vue     # 字典选择器
│   ├── DictTag.vue        # 字典标签
│   ├── RegionTreeSelect.vue  # 行政区划树选择器
│   └── IconSelector.vue   # 图标选择器
├── composables/      # 组合式 API（useDict, usePermission …）
├── layouts/          # 布局组件
├── router/           # 路由配置（动态路由生成）
├── stores/           # Pinia Store
├── utils/            # 工具函数（request, encrypt-request, file …）
├── views/            # 页面视图（system/, flow/, generator/, message/ …）
├── styles/           # 全局样式
└── config/           # 应用配置
```

### 4.3 API 层约定

- **统一请求工具**：`import { request } from '@/utils/request'`
- **加密请求**：`import { postEncrypt } from '@/utils/encrypt-request'`（对应后端 `@ApiDecrypt`）
- **接口路径格式**：`METHOD@/api/module/action`（AiCrudPage `api-config` 使用）
- **占位符格式**：`:id`（冒号），**不是** `{id}`（花括号）
- **分页参数**：前端传 `pageNum` + `pageSize`，后端必须用相同命名接收

### 4.4 核心组件速查

| 组件 | 路径 | 用途 |
|------|------|------|
| `AiCrudPage` | 内建组件 | 零代码 CRUD 页面（配置 api-config + schema） |
| `DictSelect` | `@/components/DictSelect.vue` | 字典下拉选择 |
| `DictTag` | `@/components/DictTag.vue` | 字典标签渲染（自动映射颜色） |
| `RegionTreeSelect` | `@/components/RegionTreeSelect.vue` | 行政区划树选择 |
| `AuthImage` | `@/components/common/AuthImage.vue` | 鉴权图片（fetch + Bearer Token） |
| `IconSelector` | `@/components/IconSelector.vue` | 图标选择器 |

### 4.5 按钮样式约定

使用 UnoCSS 语义化颜色类区分操作类型：

| 类名 | 颜色 | 场景 |
|------|------|------|
| `text-primary` | 蓝 | 编辑、查看、授权 |
| `text-info` | 灰蓝 | 详情、统计、在线用户 |
| `text-warning` | 黄 | 刷新缓存、重置、封禁 |
| `text-error` | 红 | 删除、强制下线 |
| `text-success` | 绿 | 启用、发布、通过 |

```vue
<a class="text-primary cursor-pointer hover:text-primary-hover" @click="handleEdit(row)">编辑</a>
<a class="text-error cursor-pointer hover:text-error-hover" @click="handleDelete(row)">删除</a>
```

---

## 5. 关键约定

> 以下规则违反会直接导致编译失败、运行时异常或数据问题。

### 5.1 SQL 必须写在 Mapper XML 中

查询类 SQL **禁止**在 Service 层用 `LambdaQueryWrapper` 构建。必须写在 Mapper XML 中。
- **原因**：`DataScopeInterceptor` 按 `mapperMethod` 精确匹配改写 SQL；XML 更易审查和优化
- **例外**：仅单表 `selectById`、`insert`、`updateById`、`deleteById` 等 MyBatis-Plus 内置方法允许

### 5.2 租户 ID 规则

- 业务数据（字典、配置等）的 `tenant_id` **必须设为 `1`**（默认租户），**禁止设 `0`**
- `TenantLineInnerInterceptor` 自动追加 `WHERE tenant_id = 当前租户ID`，`0` 的数据对所有租户不可见
- `sys_resource`（菜单/权限）表不受租户拦截，`tenant_id` 设为 `1` 即可

### 5.3 分页参数命名

- 前端传 `pageNum` + `pageSize`
- 后端 Controller 必须用 `@RequestParam(defaultValue = "1") Integer pageNum`，不能用 `page`

### 5.4 AiCrudPage 占位符

- URL 占位符用 **冒号格式**：`:id`、`:dictId`
- **禁止**花括号格式：`{id}` — 组件只识别 `includes(':id')`

### 5.5 图片/文件渲染

- `imageUpload` 组件存储的是 **fileId**，不是 URL
- 表格列渲染图片用 **`AuthImage`** 组件（自动带 Token），禁止直接用 `NAvatar` src
- 获取下载链接用 `getFileUrl(fileId)` from `@/utils/file`

### 5.6 循环依赖禁止

- Service 之间**禁止互相注入**
- 跨 Service 协调逻辑上提到 Controller 层

### 5.7 字典禁止硬编码

- 下拉选项、状态标签 **必须使用字典组件**（`DictSelect` / `DictTag` / `useDict`）
- Schema 必须定义为 `computed`，确保字典异步加载后响应式更新
- 业务枚举和可配置枚举必须维护到 `sys_dict_type` + `sys_dict_data`，禁止在前端页面写死 `options` 或标签映射
- 新增内置字典必须通过 `forge/db/migration/` 的 Flyway 脚本写入，脚本需具备 `NOT EXISTS` 防重复保护，`tenant_id` 必须为 `1`
- 字典类型命名使用小写下划线，系统级字典建议使用 `sys_` 前缀；文件存储类型统一使用 `sys_file_storage_type`
- 前端读取字典时使用 `useDict('<dict_type>')`，表格回显优先使用 `DictTag`，下拉选项使用 `computed(() => dict.value.<dict_type> || [])`
- 字典数据的 `dict_value` 必须与后端枚举/存储策略/业务状态值保持一致，`dict_label` 只负责展示文案，`list_class` 负责标签样式

### 5.8 API 规范

- RESTful：`GET /page`(分页)、`GET /:id`(详情)、`POST /`(新增)、`PUT /`(修改)、`DELETE /:id`(删除)
- 统一返回：`RespInfo.success(data)` / `RespInfo.error(msg)`
- 敏感接口：`@ApiDecrypt` / `@ApiEncrypt`

### 5.9 安全红线

- 禁止硬编码密钥、AK/SK、数据库密码
- 禁止在日志中打印手机号、身份证、银行卡
- API Key/Secret 返回前端必须脱敏（保留前4后4，中间 `****`）
- 涉及资金/状态流转/权限变更，必须在 Spec 中标注并经人工审查

### 5.10 数据库规范

- 所有业务表必须包含：`id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`
- 字符集 `utf8mb4`，引擎 `InnoDB`
- 金额字段用 `long`，单位**分**
- 时间字段用 `LocalDateTime`

### 5.11 数据库脚本维护规范

所有数据库结构和内置数据变更必须走统一脚本，不允许只改实体、Mapper 或本地数据库。

#### 目录约定

| 目录 | 用途 |
|------|------|
| `forge/db/migration/` | Flyway 版本化迁移脚本，放表结构、索引、字段、系统资源等正式变更 |
| `forge/db/seed/required/` | 系统运行必需初始化数据 |
| `forge/db/seed/demo/` | 演示数据，默认不导入 |
| `forge/db/seed/optional/` | 可选模块数据 |

#### Flyway 命名规范

- 版本脚本统一命名：`V<版本号>__<lower_snake_case_description>.sql`
- 示例：`V1.0.2__add_dashboard_version_table.sql`
- `V1.0.0__baseline.sql` 是历史基线，新变更版本必须大于 `1.0.0`
- 版本号必须单调递增；同一版本号只能有一个脚本
- 已经执行到数据库并进入 `forge_schema_history` 的脚本禁止修改；需要修正时新增下一个版本脚本

#### SQL 编写规则

- 脚本必须可重复执行或具备防重复保护：`CREATE TABLE IF NOT EXISTS`、`INSERT ... SELECT ... WHERE NOT EXISTS`、新增列/索引前查 `information_schema`
- `INSERT` 必须显式写列名，禁止依赖表字段顺序
- 业务内置数据 `tenant_id` 必须为 `1`，禁止写 `0`
- `sys_resource`、`sys_role_resource` 等权限资源脚本必须做 `NOT EXISTS` 防重复
- 生产敏感数据、真实密码、Token、AK/SK、API Key 禁止提交到 SQL
- 涉及数据修复、状态流转、资金、权限放开的 SQL，必须在 Spec 中说明影响范围和回滚方式

#### 启动与验证

- `forge-admin-server` 启动时由 Flyway 执行 `forge/db/migration` 脚本；`forge-report-server` 单独启动不会执行这些迁移
- 默认配置兼容不同启动目录：`filesystem:./db/migration,filesystem:../db/migration,filesystem:forge/db/migration`
- 若设置了 `FORGE_FLYWAY_LOCATIONS` 或 `FORGE_FLYWAY_ENABLED`，会覆盖默认配置；迁移未执行时优先检查这两个环境变量
- 验证迁移结果：

```sql
SELECT installed_rank, version, description, success
FROM forge_schema_history
ORDER BY installed_rank DESC;
```

---

## 6. 本地开发及验证流程

### 6.1 首次环境搭建

```bash
# 1. 数据库
mysql -u root -p -e "CREATE DATABASE forge DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -u root -p forge < forge/forge-admin-server/src/main/resources/sql/forge.sql

# 2. 后端配置
cp forge/forge-admin-server/src/main/resources/application-dev.example.yml \
   forge/forge-admin-server/src/main/resources/application-dev.yml
# 编辑 application-dev.yml，填入数据库/Redis 连接信息

# 3. 前端配置
cd forge-admin-ui
cp .env.example .env.local
# 编辑 .env.local（可选，默认代理到 localhost:8580）

# 4. 启动
cd forge/forge-admin-server && mvn spring-boot:run       # 后端 :8580
cd forge-admin-ui && pnpm install && pnpm dev             # 前端 :5173
```

### 6.2 改 → 构建 → 验证闭环

```bash
# === 后端 ===
# 改代码
vim forge/forge-framework/forge-plugin-system/src/main/java/.../XxxController.java

# 构建（确认编译通过）
cd forge && mvn clean install -DskipTests

# 重启服务
cd forge/forge-admin-server && mvn spring-boot:run

# 验证
curl -s http://localhost:8580/xxx/page?pageNum=1&pageSize=10

# === 前端 ===
# 改代码
vim forge-admin-ui/src/views/system/xxx/index.vue

# Lint 检查
cd forge-admin-ui && pnpm lint:fix

# HMR 自动热更新，浏览器验证 http://localhost:5173
```

### 6.3 Token 获取与 API 验证模板

```bash
# 1. 登录获取 Token
curl -s -X POST http://localhost:8580/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | jq '.data.token'

# 2. 用 Token 调用业务接口
TOKEN="<上面获取的token>"

# 分页查询
curl -s http://localhost:8580/system/user/page?pageNum=1&pageSize=10 \
  -H "Authorization: Bearer $TOKEN" | jq .

# 详情查询
curl -s http://localhost:8580/system/user/1 \
  -H "Authorization: Bearer $TOKEN" | jq .

# 新增
curl -s -X POST http://localhost:8580/system/user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"test","nickname":"测试"}' | jq .

# 修改
curl -s -X PUT http://localhost:8580/system/user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":123,"nickname":"新昵称"}' | jq .

# 删除
curl -s -X DELETE http://localhost:8580/system/user/123 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 6.4 日志路径

| 日志 | 路径 |
|------|------|
| 应用日志 | `forge/forge-admin-server/logs/` |
| 前端开发日志 | 终端 `pnpm dev` 输出 |
| SQL 日志 | 配置 `spring.datasource.dynamic.datasource.master.log: true`（开发环境） |

---

## 7. 质量检查

自动化测试与验证统一遵循 `code-copilot/rules/automated-testing-standard.md`。执行前必须先复用已有验证基线，执行后必须把命令、结果、警告、跳过项和服务清理情况追加到当前变更的 `execution-log.md`。

| 检查项 | 后端命令 | 前端命令 |
|--------|---------|---------|
| 编译 | `cd forge && mvn clean compile` | `cd forge-admin-ui && pnpm build` |
| Lint | — | `pnpm lint:fix` |
| 测试 | `cd forge && mvn test` | — |
| 全量构建 | `cd forge && mvn clean install` | `cd forge-admin-ui && pnpm build` |
| 跳过测试构建 | `mvn clean install -DskipTests` | — |

---

## 8. 参考项目约定

本项目 AI 编程助手的工作优先级（从高到低）：

1. **本文件（AGENTS.md）** — 最高优先级，项目核心约定
2. **`code-copilot/changes/[变更名]/spec.md`** — 当前变更的 Spec（如在进行 `/apply` 时）
3. **`code-copilot/rules/project-context.md`** — 工程上下文详细版
4. **`code-copilot/rules/coding-style.md`** — 编码规范
5. **`code-copilot/rules/domain-rules.md`** — 业务领域约束
6. **`code-copilot/rules/security.md`** — 安全红线
7. **`code-copilot/rules/automated-testing-standard.md`** — 自动化测试与验证标准（执行 `/test` 和阶段验证时必读）
8. **`.opencode/memory/pitfalls.md`** — 踩坑记录（每次新对话必读）
9. **`.opencode/memory/decisions.md`** — 项目决策记录
10. **`forge-docs/guide/conventions.md`** — 完整编码规范

**优先级规则**：本文件与子文件冲突时，以本文件为准；Spec 文档与通用规则冲突时，以 Spec 为准。

---

## 9. 文档导航

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目 README | `README.md` | 项目介绍、截图、快速开始 |
| 编码规范 | `forge-docs/guide/conventions.md` | 完整命名、异常、日志、数据库规范 |
| SDD 工作流 | `forge-docs/guide/sdd-workflow.md` | Spec 驱动开发全流程 |
| 工程上下文 | `code-copilot/rules/project-context.md` | 技术栈、模块依赖、详细配置 |
| 编码规范（规则） | `code-copilot/rules/coding-style.md` | Java/前端编码规则速查 |
| 业务领域约束 | `code-copilot/rules/domain-rules.md` | 金额、时间、状态机规则 |
| 安全红线 | `code-copilot/rules/security.md` | 代码安全 + 业务安全 |
| 按钮样式规范 | `code-copilot/rules/button-style-guide.md` | UnoCSS 操作按钮颜色 |
| 踩坑记录 | `.opencode/memory/pitfalls.md` | 常见错误 & 解决方案 |
| 项目决策 | `.opencode/memory/decisions.md` | 架构决策记录 |
| 用户偏好 | `.opencode/memory/preferences.md` | 编码风格偏好 |
| 代码规则 | `.opencode/instructions/code-rules.md` | Java 基础规范 |
| 更新日志 | `CHANGELOG.md` | 版本变更记录 |
| Nginx 部署 | `NGINX_CONFIG.md` | 生产环境 Nginx 配置 |
| 字典管理 | `forge-admin-ui/DICT_MANAGEMENT_SETUP.md` | 字典功能配置说明 |
| 文件 URL 指南 | `forge-admin-ui/FILE_URL_GUIDE.md` | 文件访问 URL 规范 |

---

## 附录 A：行政区划查询规则（重要）

> 此规则适用于所有含 `region_code` 字段的业务表查询。

**核心概念**：虚拟组织节点 code 以 `ALL` 结尾（如 `150000ALL`），代表"本级 + 下级"聚合。

**MyBatis XML 写法**（推荐，写在 Mapper XML 中）：
```xml
<if test="regionCode != null and regionCode != '' and regionCode.contains('ALL')">
    AND (region_code = REPLACE(#{regionCode},'ALL','')
         OR region_code IN (SELECT code FROM sys_region_code WHERE parent_code = REPLACE(#{regionCode},'ALL','')))
</if>
<if test="regionCode != null and regionCode != '' and !regionCode.contains('ALL')">
    AND region_code = #{regionCode}
</if>
```

**规则说明**：
- 选择虚拟组织（如 `150100ALL`）：查询本级 + 所有下级区划
- 选择普通区划（如 `150102`）：精确匹配
- 前端 `RegionTreeSelect` 中虚拟组织节点 `disabled: true`，不可选中

---

## 附录 B：记忆知识图谱

每次新对话开始，先读取以下文件：
- `.opencode/memory/pitfalls.md` — 踩坑记录
- `.opencode/memory/decisions.md` — 项目决策
- `.opencode/memory/preferences.md` — 用户偏好

发现有价值信息时主动写入对应文件。实体类型：踩坑记录、项目决策、用户偏好、环境配置、业务知识。
