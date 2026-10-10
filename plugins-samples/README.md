# 源码插件开发指南

本指南面向插件交付作者，使用原始 Forge 包名与 Maven 坐标；只保存在模板，不随客户工程改名。
客户安装、升级、卸载见[操作参考](../.agents/skills/forge-project-init/references/plugins.md)。
从[hello 社区源码包](forge-plugin-hello/README.md)开始核对真实结构，不修改模板内建插件来模拟独立交付。
当前底座是源码装配、一起编译和重新部署，不是热加载插件市场；企业 License 实现/签发及收费系统不在此交付中。

## 1. 交付结构与描述

```text
plugin-package/
├── forge-plugin.json
├── server/
│   └── forge-plugin-hello/
│       ├── pom.xml
│       └── src/main/resources/
│           ├── META-INF/forge-plugin.json
│           └── db/plugin/hello/V1.0.0__add_hello_resources.sql
└── ui/
    ├── index.vue
    └── api/
```

根描述与后端 `META-INF/forge-plugin.json` 必须一致；后者进入插件 JAR 供启动扫描使用。
UI-only 可不带 server，server-only 可不带 ui，但至少声明一种能力；ZIP 根直接放上述内容，不套多余目录。

```json
{
  "id": "hello",
  "name": "示例插件",
  "version": "1.0.0",
  "edition": "community",
  "requiresCore": ">=1.2.0 <2.0.0",
  "features": ["community.hello"],
  "server": { "module": "forge-plugin-hello" },
  "ui": { "dir": "ui" }
}
```

| 字段 | 实际约束 |
| --- | --- |
| id | 小写字母起始、字母数字/连字符，总长 2–32；全局唯一，发布后不要改 |
| name | 非空字符串，最长 128 |
| version | 插件发布 SemVer；与 Maven 宿主 revision 是两件事 |
| edition | community 或 ee；开源模板只提交 community 包 |
| requiresCore | 完整 SemVer 比较式用空格连接；支持 >=、>、<=、<、=，不支持 OR、^、~ 范围 |
| features | 去重数组，最多 128；每项非空、最长 64，符合现有功能编码语法 |
| server.module | 单个后端模块目录名，小写字母起始，最长 64，必须与 POM artifactId 相同 |
| ui.dir | 包内相对路径，不允许越界/绝对路径或链接；安装后固定落到宿主 views/plugins/id |

不接受重复 JSON 键或未知字段；当前没有插件依赖、安装钩子、在线执行脚本或自动更新字段。
不要用插件 ID 字母排序表达依赖关系，依赖宿主可选模块时先核对生成预设及依赖闭包。
启动 Registry 会拒绝重复 ID、非法描述和不兼容版本；复制一个 JAR 但漏描述不等同于完成安装。

## 2. 后端工程与扫描

参考[样例 POM](forge-plugin-hello/server/forge-plugin-hello/pom.xml)：

- 原始 parent 为 `com.mdframe.forge:forge-server`、版本 `${revision}`；插件继承 revision，不能覆盖它。
  不提前按某个客户 groupId/包名改交付源包；复制安装复用工程改名规则。
- source parent.relativePath 可指向作者的模板位置；安装器改为宿主插件位置的 `../../pom.xml`。
  不直接软链接整个模块；开发接入保留普通宿主 POM，以免 Maven 沿真实路径找错父工程。
- 控制器/服务在宿主扫描根下（原始为 `com.mdframe.forge`），Mapper 遵守宿主 `mapper` 扫描及 XML 规范。
  安装器只登记后端根 modules 与 Admin dependencies；不要假设会自动接入独立 Flow/App/Report 服务。
- 依赖 `forge-starter-plugin` 使用 Registry/Gate/注解，依赖 `forge-starter-core` 使用统一响应。
  尽量沿用宿主依赖管理；新增外部依赖另行评估，不把依赖打包成来源不明的可执行构件。
- 版本响应使用 Registry 中的插件发布版本及 `ForgeVersion.CURRENT`，不要把 `${revision}` 当插件 version。

## 3. 登录、角色权限与功能授权

所有企业版 HTTP Controller 入口必须同时满足宿主登录/RBAC 和功能使用权；示意声明为：

```java
@SaCheckPermission("plugin:document:view")
@RequiresFeature("ee.document")
```

注解分别来自 `cn.dev33.satoken.annotation.SaCheckPermission` 与
`com.mdframe.forge.starter.plugin.feature.RequiresFeature`。可以标注 Controller 类；方法注解优先，
覆盖时必须仍保护对应企业功能，不能误换成空编码或社区编码。

- 企业包 `edition=ee` 的功能编码必须以 `ee.` 开头；community 编码不能用此前缀。
  features 列表只是声明，不会自动给每个 Controller 加注解或授予功能。
- 默认 CommunityFeatureGate 禁用 `ee.` 功能，允许普通/社区编码；企业授权提供方另行实现 FeatureGate。
  安装企业源码不等于安装 License，也不等于获得使用权。源码交付不是防破解保证。
- `sys_resource.feature_code` 对齐 API 的功能码；菜单隐藏只是访问引导，不能替代服务端拦截。
  管理员保留 RBAC 通配权限，企业接口仍必须由 RequiresFeature 拦截；普通用户还需菜单与 API 两类授权。
- 当前注解由 MVC 拦截器执行，未授权返回 HTTP 403；不是 Service AOP。
  定时任务、消息消费者、内部 Java 调用等非 HTTP 入口需在自身执行边界显式检查 Gate 和业务权限。
- 请求 DTO、状态枚举、租户/数据权限、加解密和审计遵循[AGENTS 规范](../AGENTS.md)，不要因插件另开旁路。

## 4. 数据库迁移

SQL 放模块 `src/main/resources/db/plugin/<id>/`，版本从 `V1.0.0__描述.sql` 独立递增。
主库 migration 仍走主库历史；不要把插件 SQL 复制进主库目录或复用其它插件 ID。

- 默认策略先跑主 Flyway，再按插件 ID 执行有 SQL 的插件；不重跑主 Java migrations/provider。
  数据源及通用配置继承主 Flyway，插件独立 baselineVersion=0；禁止绕过既有状态或自动 clean。
  宿主自定义 FlywayMigrationStrategy 会覆盖默认策略，接入者必须自行保留插件执行语义。
- 主历史 `forge_schema_history` 对应 `forge_plugin_hello_history`；客户工程按其主历史前缀推导。
  `id` 连字符转下划线，最终表名不能超过 MySQL 64 字符；即使插件 ID 合法也要核对工程前缀长度。
- 已执行的 SQL 禁止改字节，升级追加更高版本脚本；checksum 失败不以自动 repair 掩盖。
  主迁移成功后插件失败会阻断启动，但不承诺 MySQL DDL 或此前插件迁移自动回滚。
- 表结构、逻辑删除、字典、审计字段按 AGENTS 5.12/5.13；使用 NOT EXISTS / information_schema 防重复、
  显式列名、默认租户 tenant_id=1。敏感值不进入 SQL，避免 Flyway 业务模板占位符。
- 菜单/API 插入自身独立资源，不覆盖客户同路径资源或给现有角色自动授权；权限变更先人工审查。
  可参考[hello 迁移](forge-plugin-hello/server/forge-plugin-hello/src/main/resources/db/plugin/hello/V1.0.0__add_hello_resources.sql)。
- 正式插件应在 MySQL 验证首次/重复/升级/失败恢复，以及真实用户权限。H2 只能作为快速回归。
  清理前预览计划；主库及插件历史必须保留，不通过删历史触发重新初始化。

## 5. 前端与菜单

`ui/` 是宿主页面源码，不是单独的前端应用。安装到 `src/views/plugins/<id>/`，复用宿主 Vue/Naive UI、
主题和 `@/utils/request`。顶层 `index.vue` 的菜单 component 为 `plugins/<id>/index`，
路由约定 `/plugins/<id>`；API 前缀与后端约定，不把客户端路由当 API。
页面动态路由不会替你插入后端资源或授权角色。

API/测试放 `api/`，辅助组件放 `components/`；两类目录不进入宿主页面选择列表，
只将真正的页面暴露给菜单。不要把辅助组件当作独立业务入口。
参考[样例页面](forge-plugin-hello/ui/index.vue)与[真实请求](forge-plugin-hello/ui/api/info.js)，
保留加载/失败/重试和响应校验，不在正式页面硬编码演示数据。
普通用户须授权菜单及 API 后重新登录；只有菜单没有 API 时出现 403 是权限缺失，不应关闭鉴权。

## 6. 版本、打包及交付检查

更新两份一致描述并追加迁移；列出宿主兼容范围、变更、定制影响和升级/数据修复策略。
交付干净源码目录或 ZIP，不带 .git、node_modules、target、本机配置、密钥或用户数据。
包的所有普通源码应可审查；不要使用 ZIP 链接/穿越路径来共享外部文件。

至少完成：描述严格校验 → 模板复制/开发模式 → 改名 full/minimal-admin 安装 → 相关测试 →
Admin 聚合 package 与管理端 build → 真实 MySQL/普通用户验收。只支持特定预设时明确声明，不伪称通用。
发布 Maven 构建按宿主默认跳过测试；验证测试必须显式 `-Penable-tests`，实际计数不能是全跳过。
构件要确认含运行描述、SQL 和控制器，测试资源不进生产包；安装成功不代表路由、授权或迁移已验收。

客户升级的 `add ... --force` 是整包替换，已提交定制也会被替换；卸载不删数据库或迁移历史。
恢复副本不等于数据库回滚。把这些影响写进包说明，不增加工具尚未支持的 merge/restore/update 参数。
完整操作流程统一见[安装与维护参考](../.agents/skills/forge-project-init/references/plugins.md)，不在每个样例复制一套指令。

## 7. 开源与企业交付边界

社区示例允许独立提交；本地安装目录只用于联调，卸载后按 AGENTS 5.18 运行 `pnpm check:edition`。
模板的两个 POM 插件区块须恢复为空，不通过强制提交忽略文件或删除标记绕过检查。
客户工程正常提交自己的合法授权源码，不复制模板门禁。
企业源码放独立私有仓库；企业包名前缀在规范中以 `com.mdframe.forge` + `.ee` 分段表达，
无需把私有实现或真实 License 测试数据放进社区仓库。授权签发、收费与永久授权/维保规则另行立项。
