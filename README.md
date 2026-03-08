# Forge Admin

Forge Admin 是一套基于 Spring Boot + Vue 3 构建的企业级中后台管理系统，采用前后端分离架构，提供完善的用户权限体系、租户管理、系统监控等企业级功能。

## 项目概述

Forge Admin 是一个现代化的企业级 admin 系统，旨在为企业提供快速开发、业务扩展的中后台基础框架。系统采用微内核 + 插件化架构，核心功能以插件形式存在，便于按需引入和扩展。

### 核心特性

- **微内核架构**：核心框架轻量级，功能通过插件扩展
- **多租户支持**：完善的多租户体系，支持数据隔离
- **权限管理**：基于 RBAC 的细粒度权限控制
- **代码生成**：可视化代码生成，快速构建业务模块
- **动态 API**：运行时 API 配置管理，支持动态调整接口行为
- **任务调度**：分布式任务调度，支持 Cron 表达式
- **消息中心**：统一消息管理，支持多种通知渠道
- **系统监控**：实时系统监控，掌握服务器状态

## 系统截图

### 登录页面

![登录页面](images/login.png)

系统提供安全的登录认证，支持验证码校验，保障系统安全。

### 首页仪表盘

![首页仪表盘](https://s1.img-e.com/20260308/69ad700fc2738.png)

直观的数据展示面板，实时掌握系统运行状态和关键业务指标。

### 菜单管理

![菜单管理](https://s1.img-e.com/20260308/69ad70200ed25.png)

灵活的菜单配置，支持动态路由、权限绑定，轻松构建系统导航结构。

### 配置管理

![配置管理](https://s1.img-e.com/20260308/69ad700e23968.png)

可视化配置管理，支持系统参数、字典数据的动态维护。

### 消息管理

![消息管理](https://s1.img-e.com/20260308/69ad7021716a9.png)

统一消息中心，管理系统通知、站内消息，支持消息模板配置。

### 服务监控

![服务监控](https://s1.img-e.com/20260308/69ad7007ec819.png)

实时监控服务器状态，包括 CPU、内存、磁盘等关键指标，保障系统稳定运行。

## 技术栈

### 后端技术

| 技术 | 说明 |
|------|------|
| Spring Boot | 应用开发框架 |
| Spring Cloud | 微服务框架（可选） |
| MyBatis-Plus | ORM 框架 |
| Sa-Token | 认证授权框架 |
| Redisson | 分布式缓存 |
| Quartz | 任务调度 |
| Spring Cloud Gateway | 网关（可选） |

### 前端技术

| 技术 | 说明 |
|------|------|
| Vue 3 | 渐进式前端框架 |
| Naive UI | Vue 3 组件库 |
| Pinia | 状态管理 |
| Vue Router | 路由管理 |
| Vite | 构建工具 |
| UnoCSS | 原子化 CSS |

## 模块说明

### 后端模块

```
forge/
├── forge-admin/                 # 主应用模块
├── forge-framework/            # 框架核心
│   ├── forge-plugin-parent/    # 插件父模块
│   │   ├── forge-plugin-system/     # 系统管理插件
│   │   ├── forge-plugin-generator/  # 代码生成插件
│   │   ├── forge-plugin-job/        # 任务调度插件
│   │   └── forge-plugin-message/    # 消息插件
│   └── forge-starter-parent/   # 启动器父模块
│       ├── forge-starter-auth/      # 认证授权
│       ├── forge-starter-cache/     # 缓存管理
│       ├── forge-starter-config/    # 配置中心
│       └── forge-starter-api-config/# API配置
```

### 前端项目

```
forge-admin-ui/
├── src/
│   ├── api/            # API 接口
│   ├── assets/         # 静态资源
│   ├── components/     # 公共组件
│   ├── composables/    # 组合式 API
│   ├── layouts/       # 布局组件
│   ├── router/        # 路由配置
│   ├── store/         # 状态管理
│   ├── styles/        # 全局样式
│   ├── utils/         # 工具函数
│   └── views/         # 页面视图
└── ...
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- pnpm 8+
- MySQL 8.0+
- Redis 6.0+

### 后端部署

1. 克隆项目

```bash
git clone https://gitee.com/ForgeLab/forge-admin.git
cd forge-admin
```

2. 导入数据库

执行 `forge/forge-admin/sql/sys.sql` 创建基础数据库表

3. 修改配置

编辑 `forge/forge-admin/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/forge_admin?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
```

4. 启动服务

```bash
cd forge/forge-admin
mvn spring-boot:run
```

服务默认启动在 `http://localhost:8080`

### 前端部署

1. 安装依赖

```bash
cd forge-admin-ui
pnpm install
```

2. 启动开发服务器

```bash
pnpm dev
```

3. 构建生产版本

```bash
pnpm build
```

## 功能模块

### 系统管理

| 模块 | 说明 |
|------|------|
| 用户管理 | 用户的增删改查、角色绑定、组织关联 |
| 角色管理 | 角色权限配置、资源绑定 |
| 菜单管理 | 动态菜单配置、页面路由管理 |
| 部门管理 | 组织架构管理、树形结构 |
| 岗位管理 | 岗位配置、用户岗位关联 |
| 租户管理 | 多租户配置、租户隔离 |

### 系统监控

| 模块 | 说明 |
|------|------|
| 在线用户 | 查看当前在线用户、强制下线 |
| 定时任务 | 任务配置、动态调度 |
| 系统日志 | 操作日志、登录日志查询 |
| 系统监控 | CPU、内存、磁盘监控 |

### 运维工具

| 模块 | 说明 |
|------|------|
| 缓存管理 | Redis 缓存可视化操作 |
| 文件管理 | 文件上传、存储配置 |
| 字典管理 | 静态字典维护 |
| 通知公告 | 通知发布、阅读状态跟踪 |

### 开发者工具

| 模块 | 说明 |
|------|------|
| 代码生成 | 可视化配置、代码生成 |
| API 配置 | 接口行为动态配置 |
| 数据源管理 | 多数据源配置 |

## 插件说明

### 系统管理插件 (forge-plugin-system)

提供完整的系统管理功能，包括用户、角色、菜单、部门、岗位、租户等管理。

### 代码生成插件 (forge-plugin-generator)

可视化代码生成工具，支持：
- 数据库表导入
- 字段配置
- 模板管理
- 代码预览与下载

### 任务调度插件 (forge-plugin-job)

基于 Quartz 的分布式任务调度，支持：
- Cron 表达式配置
- 手动触发执行
- 任务执行日志

### 消息插件 (forge-plugin-message)

统一消息中心，支持：
- 系统通知
- 站内消息
- 消息模板

## 贡献指南

欢迎提交 Issue 和 Pull Request。

## 许可证

本项目基于 [MIT](LICENSE) 许可证开源。