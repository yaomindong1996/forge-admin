---
alwaysApply: true
---
# 工程上下文
> 本文件由 /spec:init 命令自动生成，描述项目整体结构与约定

## 1. 应用概况
- **应用名**: Forge Admin
- **简介**: 基于Vue3 + Spring Boot的企业级中后台管理框架，微内核插件化架构
- **技术栈**: Java 17 / Spring Boot 3.2.9 / Vue 3.5 / Naive UI 2.42 / Vite 7
- **构建工具**: Maven (后端) + pnpm (前端)
- **数据库**: MySQL 8.0+ / Redis 6.0+
- **版本**: 1.0.0

## 2. 技术栈详情

### 2.1 后端技术栈
| 技术/框架 | 版本 | 用途 |
|----------|------|------|
| Spring Boot | 3.2.9 | 基础框架 |
| MyBatis-Plus | 3.5.7 | ORM框架 |
| Sa-Token | 1.38.0 | 认证授权 |
| Redisson | 3.34.1 | 分布式锁 |
| Hutool | 5.8.31 | Java工具库 |
| Flowable | 7.0.1 | 工作流引擎 |
| MapStruct-Plus | 1.4.4 | 对象映射 |
| EasyExcel | 4.0.2 | Excel处理 |
| Velocity | 2.3 | 模板引擎 |
| SpringDoc | 2.6.0 | API文档 |
| SnailJob | 1.1.2 | 分布式任务调度 |
| AWS SDK | 2.25.15 | 对象存储 |
| SMS4J | 3.3.2 | 短信服务 |
| Undertow | 2.3.15 | Web服务器 |

### 2.2 前端技术栈
| 技术/框架 | 版本 | 用途 |
|----------|------|------|
| Vue | 3.5.20 | 前端框架 |
| Naive UI | 2.42.0 | UI组件库 |
| Vite | 7.1.3 | 构建工具 |
| Pinia | 3.0.3 | 状态管理 |
| Vue Router | 4.5.1 | 路由管理 |
| UnoCSS | 66.4.2 | 原子化CSS |
| Axios | 1.11.0 | HTTP客户端 |
| ECharts | 6.0.0 | 图表库 |
| BPMN.js | 17.11.1 | 流程设计器 |
| CodeMirror | 6.0.1 | 代码编辑器 |
| XLSX | 0.18.5 | Excel处理 |
| Lodash-ES | 4.17.21 | 工具函数库 |

## 3. 目录结构与模块职责

### 3.1 后端目录结构
```
forge/
├── forge-admin/                      # 主应用启动入口
│   ├── src/main/
│   │   ├── java/                     # 主程序入口
│   │   └── resources/
│   │       ├── application.yml       # 主配置文件
│   │       ├── application-dev.yml   # 开发环境配置
│   │       ├── sql/                  # SQL脚本
│   │       └── i18n/                 # 国际化资源
│   └── pom.xml                       # 依赖所有插件和starter
│
├── forge-flow/                       # 独立流程引擎服务
│
└── forge-framework/                  # 核心框架层
    ├── forge-dependencies/           # 统一依赖管理
    │
    ├── forge-plugin-parent/         # 插件集合（业务模块）
    │   ├── forge-plugin-system/      # 系统管理插件
    │   │   ├── controller/           # 控制器层
    │   │   ├── service/              # 服务层
    │   │   │   └── impl/             # 服务实现
    │   │   ├── mapper/               # 数据访问层
    │   │   ├── entity/               # 实体类
    │   │   ├── dto/                  # 数据传输对象
    │   │   ├── vo/                   # 视图对象
    │   │   ├── constant/             # 常量定义
    │   │   └── listener/             # 事件监听器
    │   ├── forge-plugin-generator/   # 代码生成插件
    │   ├── forge-plugin-job/         # 定时任务插件
    │   ├── forge-plugin-message/     # 消息中心插件
    │   └── forge-plugin-flow/        # 流程引擎插件
    │
    └── forge-starter-parent/        # 启动器集合（技术能力）
        ├── forge-starter-core/       # 核心功能
        ├── forge-starter-web/        # Web层封装
        ├── forge-starter-auth/       # 认证授权（Sa-Token）
        ├── forge-starter-orm/        # ORM封装（MyBatis-Plus）
        ├── forge-starter-cache/      # 缓存管理（Redis）
        ├── forge-starter-config/     # 配置中心
        ├── forge-starter-crypto/     # 加解密能力
        ├── forge-starter-tenant/     # 多租户支持
        ├── forge-starter-file/       # 文件存储（OSS）
        ├── forge-starter-excel/      # Excel处理
        ├── forge-starter-job/        # 任务调度
        ├── forge-starter-log/        # 日志管理
        ├── forge-starter-id/         # ID生成器
        ├── forge-starter-idempotent/ # 幂等性控制
        ├── forge-starter-datascope/  # 数据权限
        ├── forge-starter-websocket/  # WebSocket支持
        ├── forge-starter-trans/      # 分布式事务
        ├── forge-starter-social/     # 社交登录
        ├── forge-starter-api-config/ # API配置
        ├── forge-starter-message/    # 消息服务
        └── forge-flow-client/        # 流程客户端
```

### 3.2 前端目录结构
```
forge-admin-ui/
├── src/
│   ├── api/              # API接口定义
│   ├── components/       # 公共组件
│   │   ├── ai-form/           # AI表单组件
│   │   ├── ai-modal/          # AI弹窗组件
│   │   ├── bpmn/              # BPMN流程设计器
│   │   ├── common/            # 通用组件
│   │   ├── file-upload/       # 文件上传
│   │   ├── form-designer/     # 表单设计器
│   │   ├── image-upload/      # 图片上传
│   │   ├── DictSelect.vue     # 字典选择器
│   │   ├── DictTag.vue        # 字典标签
│   │   ├── IconSelector.vue   # 图标选择器
│   │   └── ...
│   ├── composables/      # 组合式API
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置
│   ├── store/            # Pinia状态管理
│   ├── stores/           # Pinia Store模块
│   ├── utils/            # 工具函数
│   ├── config/           # 配置文件
│   ├── styles/           # 样式文件
│   ├── views/            # 页面视图
│   │   ├── system/           # 系统管理页面
│   │   ├── flow/             # 流程管理页面
│   │   ├── generator/        # 代码生成页面
│   │   ├── message/          # 消息中心页面
│   │   ├── login/            # 登录页面
│   │   └── ...
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── vite.config.js        # Vite配置
└── package.json          # 项目依赖
```

## 4. 模块依赖关系

### 4.1 后端模块依赖图
```
forge-admin (主应用)
├── forge-plugin-system (系统管理)
├── forge-plugin-job (定时任务)
├── forge-plugin-message (消息中心)
├── forge-plugin-generator (代码生成)
├── forge-flow-client (流程客户端)
└── starter集合:
    ├── forge-starter-web (Web能力)
    ├── forge-starter-auth (认证授权)
    ├── forge-starter-excel (Excel)
    ├── forge-starter-config (配置中心)
    ├── forge-starter-id (ID生成)
    └── forge-starter-crypto (加解密)

forge-flow (流程引擎服务)
├── forge-plugin-flow (流程插件)
└── starter集合

每个plugin模块依赖:
└── forge-starter-orm (MyBatis-Plus)
└── forge-starter-core (核心能力)
```

### 4.2 分层架构

#### 后端标准分层
```
Controller层 (controller/)
    ↓ 接收请求、参数校验、协议转换
Service层 (service/)
    ↓ 业务编排、事务边界
Manager层 (manager/) [可选]
    ↓ 领域能力、单一职责、可复用
Mapper层 (mapper/)
    ↓ 纯数据访问（MyBatis-Plus）

Entity实体层:
- entity/    数据库表映射
- dto/       数据传输对象（请求参数）
- vo/        视图对象（响应数据）
```

#### 前端标准分层
```
Views (views/)           # 页面视图
    ↓
Components (components/) # 可复用组件
    ↓
Composables (composables/) # 组合式API
    ↓
API Layer (api/)         # 接口调用
    ↓
Utils (utils/)           # 工具函数
```

## 5. 核心功能模块

### 5.1 系统管理插件 (forge-plugin-system)
- 用户管理：用户CRUD、用户分配角色
- 角色管理：角色CRUD、角色分配菜单权限
- 菜单管理：菜单树、权限标识
- 部门管理：部门树、数据权限
- 岗位管理：岗位CRUD
- 字典管理：字典类型、字典数据
- 参数配置：系统参数配置
- 通知公告：系统公告
- 日志管理：操作日志、登录日志
- 在线用户：在线用户监控
- 缓存监控：Redis缓存监控
- 定时任务：Quartz任务调度

### 5.2 代码生成插件 (forge-plugin-generator)
- 表管理：数据库表导入
- 字段管理：字段配置
- 代码生成：前端Vue、后端Java代码生成
- 模板管理：代码模板配置

### 5.3 流程引擎插件 (forge-plugin-flow)
- 流程设计：BPMN流程设计
- 流程部署：流程部署管理
- 流程实例：流程实例管理
- 任务管理：待办任务、已办任务
- 流程监控：流程监控面板

### 5.4 消息中心插件 (forge-plugin-message)
- 消息发送：站内信、邮件、短信
- 消息模板：消息模板管理
- 消息记录：消息发送记录

### 5.5 定时任务插件 (forge-plugin-job)
- 任务管理：Quartz任务CRUD
- 任务日志：任务执行日志
- 任务监控：任务执行监控

## 6. 常用命令清单

### 6.1 后端命令
```bash
# 编译整个项目
cd forge && mvn clean install

# 运行admin服务
cd forge/forge-admin && mvn spring-boot:run

# 运行flow服务
cd forge/forge-flow && mvn spring-boot:run

# 跳过测试编译
mvn clean install -DskipTests

# 指定环境运行
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 6.2 前端命令
```bash
# 安装依赖
cd forge-admin-ui && pnpm install

# 开发模式
pnpm dev

# 生产构建
pnpm build

# 代码检查和修复
pnpm lint:fix

# 预览构建结果
pnpm preview
```

### 6.3 数据库命令
```sql
-- 创建数据库
CREATE DATABASE forge DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 导入初始数据
USE forge;
SOURCE /path/to/forge-admin/src/main/resources/sql/forge.sql;
```

## 7. 开发环境配置

### 7.1 后端配置 (application-dev.yml)
```yaml
# 数据源配置
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/forge?useUnicode=true&characterEncoding=utf8
    username: root
    password: root

# Redis配置
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0

# 服务端口
server:
  port: 8580
```

### 7.2 前端配置 (.env.local)
```bash
# API代理配置
VITE_REQUEST_PREFIX=/api
VITE_HTTP_PROXY_TARGET=http://localhost:8580
VITE_FLOW_PROXY_TARGET=http://localhost:8581
```

## 8. 关键设计约定

### 8.1 数据库设计规范
- 所有业务表必须包含基础字段：`id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`
- 使用 `utf8mb4` 字符集，`InnoDB` 存储引擎
- 主键使用雪花ID（分布式ID）
- 高频查询字段建立索引，复合索引遵循最左前缀原则

### 8.2 API设计规范
- RESTful风格：
  - `GET /page` - 分页查询
  - `GET /{id}` - 详情查询
  - `POST /` - 新增
  - `PUT /` - 修改
  - `DELETE /{id}` - 删除
- 统一响应：`RespInfo.success(data)` / `RespInfo.error(msg)`
- 敏感接口加密：使用 `@ApiDecrypt` / `@ApiEncrypt` 注解

### 8.3 代码生成规范
- 使用内置 `forge-plugin-generator` 生成CRUD模块
- 前端CRUD页面使用 `AiCrudPage` 组件
- 避免手写重复代码，使用代码生成器

### 8.4 前端开发规范
- API调用使用 `@/utils/request` 工具
- 加密API调用使用 `@/utils/encrypt-request`
- 样式使用UnoCSS，避免自定义CSS
- 组件开发遵循现有组件模式
- 优先使用Naive UI组件

## 9. 项目统计

- **后端模块数量**: 33个Maven模块
- **插件数量**: 5个业务插件
- **启动器数量**: 20个技术启动器
- **前端组件数量**: 20+公共组件
- **主要功能模块**: 6大功能模块

## 10. 快速开始

### 10.1 环境准备
```bash
# 检查Java版本（需要17+）
java -version

# 检查Node版本（需要18+）
node -v

# 检查pnpm版本（需要8+）
pnpm -v

# 检查MySQL版本（需要8.0+）
mysql --version

# 检查Redis版本（需要6.0+）
redis-server --version
```

### 10.2 后端启动
```bash
# 1. 创建数据库
mysql -u root -p
CREATE DATABASE forge DEFAULT CHARACTER SET utf8mb4;

# 2. 导入SQL脚本
USE forge;
SOURCE forge/forge-admin/src/main/resources/sql/forge.sql;

# 3. 配置数据库连接
cp forge/forge-admin/src/main/resources/application-dev.example.yml \
   forge/forge-admin/src/main/resources/application-dev.yml
# 编辑application-dev.yml，配置数据库和Redis

# 4. 启动服务
cd forge/forge-admin
mvn spring-boot:run

# 访问 http://localhost:8580
```

### 10.3 前端启动
```bash
# 1. 安装依赖
cd forge-admin-ui
pnpm install

# 2. 配置环境（可选）
cp .env.example .env.local
# 编辑.env.local配置API代理

# 3. 启动开发服务
pnpm dev

# 访问 http://localhost:5173
# 默认账号: admin / 123456
```

## 11. 注意事项

1. **首次配置**: 后端需要复制 `application-dev.example.yml` 为 `application-dev.yml` 并配置数据库和Redis
2. **前端配置**: 前端需要复制 `.env.example` 为 `.env.local` 配置本地环境
3. **代码规范**: 遵循 `code-copilot/rules/coding-style.md` 编码规范
4. **领域规则**: 遵循 `code-copilot/rules/domain-rules.md` 业务规则
5. **渐进式开发**: 使用 `/propose` 命令创建变更提案，遵循 No Spec No Code 原则
6. **分支管理**: 禁止直接提交到master分支，使用feature分支开发
7. **代码生成**: 优先使用代码生成器，避免手写CRUD代码