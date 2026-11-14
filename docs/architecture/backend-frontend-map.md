# 后端 / 前端结构速查

> 从根 `AGENTS.md` §3–§4 拆出；系统级 codemap 见根 `ARCHITECTURE.md`。
> 更完整的历史架构说明见 `docs/architecture.md`。

## 3. 后端架构

### 3.1 完整模块树

```
forge-server/
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

