<h1 align="center">Forge Admin</h1>

<p align="center">
  🚀 基于 Vue3 + Spring Boot 3 的企业级中后台管理框架<br>
  ✨ 集成 AI 数据可视化大屏，自然语言一键生成数据报表
</p>

<p align="center">
  <a href="https://gitee.com/ForgeLab/forge-admin/stargazers"><img src="https://gitee.com/ForgeLab/forge-admin/badge/star.svg?theme=gvp" alt="Gitee stars"></a>
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-green.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.x-brightgreen.svg" alt="Vue3">
  <img src="https://img.shields.io/badge/GoView-AI%20Enhanced-orange.svg" alt="AI Dashboard">
</p>

<p align="center">
  <a href="#演示地址">在线演示</a> ·
  <a href="#快速开始">快速开始</a> ·
  <a href="#-ai-数据可视化大屏">AI 大屏</a> ·
  <a href="#功能模块">功能模块</a> ·
  <a href="CHANGELOG.md">更新日志</a>
</p>

---

## ✨ 项目简介

**Forge Admin** 是一个现代化的企业级中后台管理框架，采用 **微内核 + 插件化** 架构设计，核心功能以插件形式存在，便于按需引入和扩展。

除了完善的后台管理能力外，Forge Admin 还集成了基于 [GoView](https://gitee.com/dromara/go-view) 二次开发的 **AI 数据可视化大屏平台**，支持通过自然语言描述一键生成数据大屏，对接真实后台接口，让数据可视化开发效率提升 10 倍。

### 🌟 核心亮点

| 能力                | 说明                              |
|-------------------|---------------------------------|
| 🏗️ **微内核架构**     | 核心框架轻量级，功能通过插件扩展，按需引入           |
| 🤖 **AI 大屏生成**    | 自然语言描述需求，AI 自动生成完整数据大屏          |
| 🎨 **低代码大屏设计**    | 拖拽式可视化编辑器，丰富的图表组件和主题            |
| 🔐 **多租户 & RBAC** | 完善的多租户数据隔离 + 细粒度权限控制            |
| ⚡ **AI驱动代码生成器**   | AI驱动0代码生成，简单CRUD实现0代码驱动         |
| 🔄 **流程管理**       | 轻量集成 Flowable 工作流引擎             |
| 🔌 **多 AI 供应商**   | 支持阿里百炼、OpenAI、DeepSeek、Ollama 等 |
| 📊 **真实数据对接**     | 大屏报表直接对接后端 API，告别静态 Mock        |

## 📺 演示地址

> **后台管理**：http://81.70.22.48:8084/forge/login
>
> 账号：`admin` / `123456`
>
> **项目文档**：http://81.70.22.48:8084/forge-docs/
>
> **项目源码**：gitee: https://gitee.com/ForgeLab/forge-admin
> **项目源码**：github: https://github.com/yaomindong1996/forge-admin

---

## 🖼️ 系统截图

### 后台管理系统

#### 登录页面

![登录页面](images/login.png)

系统提供安全的登录认证，支持验证码校验，保障系统安全。

#### 首页仪表盘

![首页](images/%E9%A6%96%E9%A1%B5.png)

直观的数据展示面板，实时掌握系统运行状态和关键业务指标。

#### 菜单管理

![菜单管理](images/%E8%8F%9C%E5%8D%95%E7%AE%A1%E7%90%86.png)

灵活的菜单配置，支持动态路由、权限绑定，轻松构建系统导航结构。

#### 配置管理

![配置管理](images/%E9%85%8D%E7%BD%AE%E7%AE%A1%E7%90%86.png)

可视化配置管理，支持系统参数、字典数据的动态维护。

#### 消息管理

![消息管理](images/%E6%B6%88%E6%81%AF%E7%AE%A1%E7%90%86.png)

统一消息中心，管理系统通知、站内消息，支持消息模板配置。

#### 流程管理

![流程模型](images/%E6%B5%81%E7%A8%8B%E6%A8%A1%E5%9E%8B.png)
![流程设计](images/%E6%B5%81%E7%A8%8B%E8%AE%BE%E8%AE%A1.png)
![流程时间轴](images/%E6%B5%81%E7%A8%8B%E6%97%B6%E9%97%B4%E8%BD%B4.png)

轻量集成 Flowable 工作流引擎，业务一键触发，统一管控。

#### 我的待办

![我的待办](images/%E6%88%91%E7%9A%84%E5%BE%85%E5%8A%9E.png)

#### 文件管理

![文件管理](images/%E6%96%87%E4%BB%B6%E7%AE%A1%E7%90%86.png)

统一文件管理，支持 RustFS、本地存储等多种存储方式。

#### 数据权限配置

![数据权限配置](images/%E6%95%B0%E6%8D%AE%E6%9D%83%E9%99%90%E9%85%8D%E7%BD%AE.png)

灵活的数据权限配置，精确到字段级别的数据隔离。

#### Excel 导出配置

![excel导出配置](images/excel%E5%AF%BC%E5%87%BA%E9%85%8D%E7%BD%AE.png)

导入导出可配置，省去多余的注解配置，动态调整模版。

#### 服务监控

![服务监控](images/%E6%9C%8D%E5%8A%A1%E7%9B%91%E6%8E%A7.png)

实时监控服务器状态，包括 CPU、内存、磁盘等关键指标。

---

## 🤖 AI 数据可视化大屏

基于开源项目 [GoView](https://gitee.com/dromara/go-view)（Vue3 + TypeScript + ECharts + VChart）深度二次开发，在保留原有低代码拖拽设计能力的基础上，新增了 **AI 智能生成**和**真实后台接口对接**两大核心能力。

### 大屏登录页

![大屏登录页](images/report/%E7%99%BB%E5%BD%95%E9%A1%B5.png)

与 ForgeAdmin 共享认证体系，登录即可使用大屏报表功能。

### 项目管理

![项目列表](images/report/%E9%A1%B9%E7%9B%AE%E5%88%97%E8%A1%A8.png)

大屏项目管理页面，支持项目创建、发布、模板市场等功能。

### 可视化编辑器

![编辑器画布](images/report/%E7%94%BB%E5%B8%83.png)

核心编辑器界面，三栏布局设计：
- **左侧**：组件库（图表、装饰、信息、列表等）+ AI 助手入口
- **中间**：可视化画布，支持拖拽布局、缩放、对齐
- **右侧**：属性配置面板，支持数据源、样式、动画、事件配置

### AI 供应商配置

![AI供应商配置](images/report/AI%E4%BE%9B%E5%BA%94%E5%95%86%E9%85%8D%E7%BD%AE.png)

灵活的多供应商架构，内置预设模板一键配置：

| 供应商 | 说明 |
|--------|------|
| 阿里百炼 | 通义千问系列模型 |
| OpenAI | GPT 系列模型 |
| 智谱 AI | GLM 系列模型 |
| Moonshot | Kimi 系列模型 |
| DeepSeek | DeepSeek 系列模型 |
| Ollama | 本地部署开源模型 |
| 自定义 | 兼容 OpenAI API 格式的任意服务 |

### AI 大屏核心能力

- **🧠 自然语言生成**：描述需求即可生成完整大屏，内置电商、智慧城市、工厂、财务等快捷模板
- **🎨 深色/浅色风格**：一键切换大屏主题风格
- **📊 智能布局引擎**：AI 生成组件自动网格布局，支持 ECharts / VChart 双图表框架
- **🔄 SSE 流式输出**：实时展示 AI 生成过程，体验流畅
- **🔗 真实数据对接**：图表数据源直接配置后端 API，告别静态 Mock
- **💾 项目持久化**：大屏项目存储到后端数据库，支持多端同步

---

---
## AI驱动的代码生成

🧠 基于AI驱动的代码生成，区别与其它传统的代码生成器，简单的CRUD页面，可以通过组件市场进行个性化配置，配置之后既可完整0代码生成，也可以选择将代码包下载到工程，进行后续的改造

### AI表单生成
![AI表单生成.png](images/AI%E4%BB%A3%E7%A0%81%E7%94%9F%E6%88%90/AI%E8%A1%A8%E5%8D%95%E7%94%9F%E6%88%90.png)

### AI表单生成列表
![AI表单生成列表.png](images/AI%E4%BB%A3%E7%A0%81%E7%94%9F%E6%88%90/AI%E8%A1%A8%E5%8D%95%E7%94%9F%E6%88%90%E5%88%97%E8%A1%A8.png)

### 模版配置
![模版配置.png](images/AI%E4%BB%A3%E7%A0%81%E7%94%9F%E6%88%90/%E6%A8%A1%E7%89%88%E9%85%8D%E7%BD%AE.png)

### 表单编辑
![表单编辑.png](images/AI%E4%BB%A3%E7%A0%81%E7%94%9F%E6%88%90/%E8%A1%A8%E5%8D%95%E7%BC%96%E8%BE%91.png)

---

## 💻 技术栈

### 后端技术

| 技术 | 说明 |
|------|------|
| Spring Boot 3 | 应用开发框架 |
| Spring Cloud | 微服务框架（可选） |
| MyBatis-Plus | ORM 框架 |
| Sa-Token | 认证授权框架 |
| Redisson | 分布式缓存 |
| Quartz | 任务调度 |
| Flowable | 工作流引擎 |
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
| ECharts 5 | 数据可视化图表库 |
| VChart | 字节跳动图表框架 |

---

## 📁 模块说明

### 后端模块

```
forge/
├── forge-admin/                 # 主应用模块
├── forge-flow/                  # 流程管理模块
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
forge-admin-ui/                  # 后台管理系统前端
├── src/
│   ├── api/            # API 接口
│   ├── components/     # 公共组件（含 AI 表单组件）
│   ├── composables/    # 组合式 API
│   ├── layouts/       # 布局组件
│   ├── router/        # 路由配置
│   ├── store/         # 状态管理
│   └── views/         # 页面视图

forge-report-ui/                # AI 数据可视化大屏前端（基于 GoView）
├── src/
│   ├── components/
│   │   └── GoAI/       # AI 功能核心模块
│   ├── api/ai/         # AI 接口层
│   ├── packages/       # 图表组件包（ECharts/VChart/装饰/3D）
│   └── views/          # 大屏编辑器/预览/项目管理
```

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- pnpm 8+
- MySQL 8.0+
- Redis 6.0+

### 后端部署

#### 1. 克隆项目

```bash
git clone https://gitee.com/ForgeLab/forge-admin.git
cd forge-admin
```

#### 2. 导入数据库

执行 `forge/forge-admin/sql/初始化脚本.sql` 创建基础数据库表
如果需要部署大屏项目
执行 `forge/forge-report/sql/report-init.sql` 大屏相关数据表

#### 3. 本地环境配置

##### 复制配置模板

首次克隆项目后，需要复制配置模板文件到本地配置：

```bash
# 复制admin模块配置
cp forge/forge-admin/src/main/resources/application-dev.example.yml forge/forge-admin/src/main/resources/application-dev.yml

# 复制flow模块配置
cp forge/forge-flow/src/main/resources/application-dev.example.yml forge/forge-flow/src/main/resources/application-dev.yml
```

##### 修改配置信息

编辑 `application-dev.yml` 文件，修改以下配置：

**数据库配置**

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/your_database?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true
          username: your_username
          password: 'your_password'
```

**Redis配置**

```yaml
spring.data:
  redis:
    host: localhost
    port: 6379
    database: 0
    password: 'your_redis_password'
    redisson:
      config: |
        singleServerConfig:
          address: "redis://localhost:6379"
          database: 0
          password: 'your_redis_password'
```

##### 配置说明

- `application-dev.yml` 属于本地配置文件，已经加入 `.gitignore`，不会提交到Git仓库
- `application-dev.example.yml` 是配置模板，提交到Git仓库，供其他开发者参考
- 其他配置项根据需要自行修改，不需要提交到仓库

##### 注意事项

> ⚠️ **禁止将包含敏感信息的配置文件提交到Git仓库**
> - 数据库密码、Redis密码、密钥等敏感信息不得提交
> - 新增配置项需要添加到 `application-dev.example.yml` 模板中，并替换敏感信息
> - 生产环境配置使用单独的 `application-prod.yml`，同样不提交到仓库

##### 其他配置

- 前端配置请参考 `forge-admin-ui/.env.example` 文件，复制为 `.env.local` 进行修改

#### 4. 启动服务

```bash
cd forge/forge-admin
mvn spring-boot:run
```

服务默认启动在 `http://localhost:8080`

### 前端部署

#### 后台管理系统

```bash
cd forge-admin-ui
pnpm install
pnpm dev
```

#### AI 大屏平台

```bash
后台项目启动 forge-report服务
```

```bash
cd forge-report-ui
npm install
npm run dev
```

系统用户名: `admin` / `123456`

#### 构建生产版本

```bash
# 后台管理
cd forge-admin-ui && pnpm build

# AI 大屏
cd forge-report-ui && npm run build
```

### 测试/生产环境部署

详细的 Nginx 配置请参考 [NGINX_CONFIG.md](NGINX_CONFIG.md)

---

## 📋 功能模块

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
| Excel 配置 | 导入导出模版动态配置 |

### AI 大屏报表

| 模块 | 说明 |
|------|------|
| 大屏编辑器 | 拖拽式可视化设计，支持 30+ 图表组件 |
| AI 生成 | 自然语言描述，一键生成数据大屏 |
| AI 供应商 | 多供应商管理，支持 7+ 主流 AI 服务 |
| 数据源配置 | 对接真实后端 API，支持动态数据刷新 |
| 项目管理 | 大屏项目持久化存储、发布管理 |
| 模板市场 | 预置行业模板，快速复用 |

---

## 🔌 插件说明

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

---

## ❓ 常见问题

### Q: 为什么我拉取代码后启动报错？
A: 首次拉取代码需要复制配置模板并修改为本地环境的数据库和Redis配置。

### Q: 新增配置项需要注意什么？
A: 如果新增的配置项是通用的，请同步更新到 `application-dev.example.yml` 模板中，敏感信息用占位符代替。

### Q: 不小心提交了敏感配置怎么办？
A:
1. 立即修改密码
2. 执行 `git rm --cached <file>` 从仓库中移除文件
3. 将文件加入 `.gitignore`
4. 提交并推送到仓库

---

## 📝 更新日志

查看 [CHANGELOG.md](CHANGELOG.md) 了解项目的版本更新历史。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request。

## 📮 联系作者
<img src="images/wechat.png" width="200">
<img src="images/wechat1.png" width="200">

## 📄 许可证
本项目基于 [MIT](LICENSE) 许可证开源。
