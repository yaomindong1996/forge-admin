---
alwaysApply: true
---
# 工程上下文
> 本文件由 /init 命令自动生成，描述项目整体结构与约定
## 1. 应用概况
- 应用名: Forge Admin
- 简介: 基于Vue3 + Spring Boot的企业级中后台管理框架，微内核插件化架构
- 技术栈: Java 17 / Spring Boot 3 / Vue 3 / Naive UI / Vite
- 构建工具: Maven (后端) + pnpm (前端)
## 2. 目录结构与模块职责
### 后端（forge/）
```
forge/
├── forge-admin/                # 主应用启动入口，包含配置与SQL脚本
├── forge-flow/                 # 独立流程引擎服务
├── forge-framework/            # 核心框架层
│   ├── forge-dependencies/     # 统一依赖管理
│   ├── forge-plugin-parent/    # 插件集合
│   │   ├── forge-plugin-system/     # 系统管理插件（用户/角色/权限/菜单等）
│   │   ├── forge-plugin-generator/  # 代码生成插件
│   │   ├── forge-plugin-job/        # 定时任务插件
│   │   ├── forge-plugin-message/    # 消息中心插件
│   │   └── forge-plugin-flow/       # 流程引擎插件
│   └── forge-starter-parent/   # 启动器集合
│       ├── forge-starter-auth/      # 认证授权
│       ├── forge-starter-cache/     # 缓存管理
│       ├── forge-starter-orm/       # ORM封装（MyBatis-Plus）
│       ├── forge-starter-web/       # Web层封装
│       ├── forge-starter-crypto/    # 加解密能力
│       ├── forge-starter-tenant/    # 多租户支持
│       └── ...其他功能启动器
```

### 前端（forge-admin-ui/）
```
forge-admin-ui/
├── src/
│   ├── api/            # API接口定义
│   ├── components/     # 公共组件（AiCrudPage等）
│   ├── composables/    # 组合式API
│   ├── layouts/        # 布局组件
│   ├── router/         # 路由配置
│   ├── store/          # 状态管理（Pinia）
│   ├── utils/          # 工具函数
│   └── views/          # 页面视图
```
## 3. 分层架构（后端标准分层）
Controller (controller/) ← 入口层，参数校验 + 协议转换
↓
Service (service/)      ← 业务编排，事务边界
↓
Manager (manager/)      ← 领域能力，单一职责，可复用（可选）
↓
Mapper (mapper/)        ← 纯数据访问（MyBatis-Plus）
## 4. 关键依赖
| 中间件/框架 | 用途 | 备注 |
|--------|------|------|
| MySQL 8.0+ | 关系型数据库 | 业务数据存储 |
| Redis 6.0+ | 分布式缓存 | 缓存、会话存储、分布式锁 |
| Sa-Token | 认证授权 | 登录、权限校验 |
| MyBatis-Plus | ORM框架 | 数据访问层封装 |
| Redisson | 分布式锁 | 分布式场景下的并发控制 |
| Quartz | 定时任务 | 异步任务调度 |
| Flowable | 工作流引擎 | 流程编排（可选模块） |
