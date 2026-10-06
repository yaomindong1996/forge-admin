# 插件化底座 Test Spec

## 1. 改名基线（T0 / T5 / T6）

执行位置：仓库根目录。

```bash
pnpm forge:create -- /tmp/forge-baseline-full --base-package com.acme.demo --preset full --force
pnpm forge:create -- /tmp/forge-baseline-min --base-package com.acme.demo --preset minimal-admin --force
```

- T0：记录两套工程全部文件的相对路径和 SHA-256。
- T5：重构后以相同参数重新生成，与基线逐项比对，差异必须为 0。
- T6：差异只允许出现在以下几处：
  - 新增的 `package.json`、`scripts/`；
  - `forge.config.json` 的 `forgeVersion`、`plugins` 字段；
  - `.gitignore` 去掉的模板区块。

## 2. Node 单测（无需数据库和 Maven）

```bash
node --test scripts/forge-shared scripts/forge-plugin scripts/guards
node --test forge-server/scripts/db/clean-db.test.mjs
```

| 用例 | 预期 |
|------|------|
| rename：pom 先改 groupId，再做文本替换 | groupId 与 basePackage 不同时，pom 与 Java 结果都正确 |
| rename：Java 目录移动 | `com/mdframe/forge/...` 移到新包路径，空目录被清理 |
| plugin add：模板仓库 | 不改名；文件落到 `forge-server/plugins/`、`src/views/plugins/hello/`；标记区块写入 |
| plugin add：生成工程 | 包名、groupId、模块名与工程一致；`forge.config.json.plugins` 写入 |
| plugin add：版本不兼容 | 失败，不写任何文件 |
| plugin add：重复安装 | 未加 `--force` 失败；加了但插件目录有未提交修改仍失败 |
| plugin add：非法描述 | `id` 格式、`edition` 与 `features` 前缀不一致时失败 |
| plugin add `--dev` | 模板仓库创建软链接；生成工程拒绝 |
| plugin list / remove | 列表正确；删除目录、标记区块和配置记录，不触碰数据库 |
| check-edition | `ee` 包名、`ee` 描述、非空标记区块、被跟踪的插件目录各报一次错；干净仓库通过 |
| clean-db | `forge_plugin_hello_history` 归入保留，不在清空列表 |
| forge:create | `.gitignore` 无模板区块；`forge.config.json` 含 `forgeVersion`、`plugins` |

## 3. Java 单测（需 Maven，用户执行）

```bash
cd forge-server
mvn -pl forge-framework/forge-starter-parent/forge-starter-plugin,forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests
```

| 用例 | 预期 |
|------|------|
| `ForgeVersion.satisfies` | `>=1.1.0 <2.0.0` 对 1.1.0 / 1.9.9 为真，对 1.0.9 / 2.0.0 为假；非法表达式抛错 |
| `CommunityFeatureGate` | 空编码启用、`ee.*` 未启用、其它编码启用 |
| `FeatureGateInterceptor` | 方法注解优先于类注解；未授权返回 403 与约定文案 |
| `PluginRegistry` | 重复 ID、非法字段、版本不兼容时启动失败，错误信息含插件 ID 与版本范围 |
| `PluginFlywayMigrationStrategy` | 主迁移先执行；插件表名推导正确；插件 `baselineVersion` 为 0 |
| `SysResourceServiceImpl.getUserResources` | 超级管理员和普通用户都隐藏 `ee.*` 资源；空编码资源保留 |
| `UserLoadServiceImpl` | 普通用户的按钮权限、接口权限不含 `ee.*` 资源 |

## 4. 人工验收（真实 MySQL 8 + Maven）

1. 生成工程：`pnpm forge:create -- ../plugin-check --base-package com.acme.check --preset minimal-admin`。
2. 在生成工程中执行：`pnpm forge:plugin add <模板仓库>/plugins-samples/forge-plugin-hello`。
3. 用 `init-db.sh --recreate --clean` 初始化库，然后编译启动：`mvn -pl <admin模块> -am package -DskipTests`。
4. 检查：
   - `SHOW TABLES LIKE '%_plugin_hello_history'` 存在，且记录 `V1.0.0` 成功；
   - admin 登录后菜单出现“示例插件”，页面可打开，`GET /plugin/hello/info` 返回插件版本和框架版本。
5. 授权过滤：
   - 执行 `UPDATE sys_resource SET feature_code = 'ee.test' WHERE ...`，重新登录后菜单消失；
   - 给示例接口加 `@RequiresFeature("ee.test")` 后重启，超级管理员调用返回 403。
6. 再次启动，Flyway 主库和插件都报告 up to date。
7. 执行 `clean-db.sh --execute --yes` 后再启动，插件脚本不重复执行。
