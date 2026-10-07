# 插件中心

平台超级管理员从「平台管理 → 插件中心」查看**当前后端服务实例**包含的插件。
首次升级需要部署本轮后端/前端，并执行主迁移 `V1.0.210__add_plugin_center.sql`。
菜单不会覆盖应用总览、打印中心或客户已有同路径菜单；同路径冲突需管理员人工核对。
租户管理员和普通用户即便被授予插件编码也不能读取这个全实例目录。

## 可以查看什么

- 内置业务模块：由实际 JAR 的 `META-INF/forge-module.json` 登记，不是源码模块目录清单。
- 外部插件：来自已校验的 `META-INF/forge-plugin.json` 和 `PluginRegistry`。
- 插件独立版本、当前核心版本、来源、发行版、核心兼容范围、后端 Maven 模块。
- 外部插件声明的功能编码及当前 `FeatureGate` 使用权结果。

「后端已加载」只表示当前服务带有后端模块声明，**不是健康检查通过**。
没有 server 的描述只能显示「仅元数据」。前端声明也不代表 UI 已构建部署。
纯前端插件、尚未部署的源码、独立 Flow/Report 服务不在本阶段的清单范围。
刷新只重新查询目录和功能授权，不会重新加载 Java 代码或清除权限缓存。

## 安装与部署的边界

当前页面是运行清单，不提供上传/升级/卸载。已有 `scripts/forge-plugin/index.mjs` 源码 CLI 仍可使用，
操作前阅读工程内 `.agents/skills/forge-project-init/references/plugins.md` 的安装维护参考。
源码安装成功后要重新构建、部署，再来插件中心核验实际插件版本。
源码配置文件里的登记不等于运行成功，卸载源码也不删除数据库或迁移历史。

后续安装工作台将展示 ZIP 预检、冲突差异、确认和任务状态；独立低权限执行器负责构建，
生产 Web 服务不会直接执行上传代码。商业许可证、维保权益和 Pro 工程另行建设，
当前功能结果不能被当作商业许可证有效性证明。

## 内置声明的维护

仅真实业务插件 JAR 携带 `META-INF/forge-module.json`；聚合父 POM 和技术 starter 不登记。
稳定 ID 使用模块目录清单的逻辑 ID，例如 `plugin-system`，不能随宿主品牌/坐标改名。
声明里的 `version` 和 `module` 分别用 Maven 构建版本、artifactId 占位符，由插件父 POM
仅过滤此文件；业务 JSON/SQL 保持不过滤。声明重复、损坏或尚未过滤会在启动期报错。

API：`GET /system/plugin/page` 使用 `pageNum` / `pageSize` / `keyword` / `origin`，
`GET /system/plugin/{id}` 查询详情。清单是当前服务不可变 classpath 快照，功能结果每次查询计算。
