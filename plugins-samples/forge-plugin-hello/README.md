# 社区示例插件

此目录是可交付的源码包，不默认安装。需要 Forge 核心 `>=1.2.0 <2.0.0`，Node `>=20.19.0`。

在目标工程根目录执行（模板与 `forge:create` 生成工程均可）：

```bash
pnpm forge:plugin add /absolute/path/to/forge-plugin-hello
pnpm forge:plugin list
```

模板开发时可使用 `add /absolute/path/to/forge-plugin-hello --dev`；改名工程只支持复制安装。
ZIP 的根目录必须直接包含 `forge-plugin.json`、`server/`、`ui/`，不要再套目录。

安装仅写入源码、POM 和工程登记，不启动服务或操作数据库。安装后从后端根目录运行：

```bash
mvn -pl <项目前缀>-admin-server -am package -DskipTests
# 显式执行插件测试，发布构建默认跳过测试：
mvn -pl plugins/<模块前缀>-plugin-hello -am test -Penable-tests \
  -Dtest='HelloPlugin*Test' -Dsurefire.failIfNoSpecifiedTests=false
```

在 `<项目名>-admin-ui` 中按宿主说明安装依赖并执行生产构建；不要单独构建 ui/。
本样例在模板的模块前缀是 `forge`。插件发布版本来自两份描述文件，Maven 构件版本继承宿主 revision。

部署 Admin 后，插件迁移跟随主库迁移执行：创建一个 `/plugins/hello` 菜单和一个
`GET /plugin/hello/info` API 权限资源；独立历史为 `<宿主前缀>_plugin_hello_history`。
迁移不授予现有角色权限，接口也不公开。管理员需为普通用户同时授权菜单与 API 资源，再重新登录。
功能编码 `community.hello` 由社区 Gate 默认启用，不能代替 RBAC。

升级需同时修改根目录和 META-INF 中的描述，已执行 SQL 禁止修改，新增递增版本 SQL。
`add ... --force` 是整包替换：先提交本地定制，恢复备份保存在 `.forge-plugin/backups/hello/`。

卸载前先在菜单管理中停用示例菜单/API，并撤销角色关联；再执行：

```bash
pnpm forge:plugin remove hello
```

重新构建部署。卸载不删除数据库资源或迁移历史，重新安装不会自动恢复已停用资源。
如需清理资源，请按菜单管理的逻辑删除流程人工处理；不要删除独立迁移历史或自动执行 Flyway clean。

样例 Java 测试使用随机内存 H2，UI 测试模拟接口。它们不代替真实 MySQL 迁移、登录、普通用户授权和菜单点击验收。
