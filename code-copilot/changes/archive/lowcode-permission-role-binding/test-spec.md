# 增量测试计划

## P0

- 应用范围保存不得删除角色的其它资源权限。
- 应用发布不得重建页面角色绑定。
- 正式运行配置必须按页面 RBAC 权限码过滤系统菜单页面。
- 动态低代码数据范围必须携带对象模块编码。
- 应用权限 PUT 请求不得接受当前应用目录以外的资源 ID 或模块编码。

## P1

- 应用权限目录正确区分已注册和待发布页面、按钮资源。
- 未启用 `FOLLOW_SYSTEM` 的对象不可配置角色数据范围。
- 前端角色切换时丢弃上一角色的过期请求结果。
- 前端保存后重新加载角色授权状态并更新摘要。
- 应用权限目录返回对象数据范围策略及可选字段，适配保存拒绝当前应用以外的对象。
- 数据范围适配保存只修改模型 `policies`，保留字段、页面、表单和关系配置。
- 适配弹窗在当前权限页面完成配置；角色授权草稿未保存时不得静默丢失。
- 业务对象设计器不再展示数据范围适配入口，树形模型配置仍可使用。

## 计划命令

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-system,forge-admin-server -am test
```

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run src/views/app-center/__tests__
pnpm exec eslint src/views/app-center/application-workspace/ApplicationPermissionsPanel.vue src/views/app-center/application-permission-utils.js src/views/app-center/__tests__/application-permission-utils.spec.js
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

## UI 验证

- 打开 `/app-center/application/PRESALE_REGISTRATION_APP?section=permissions`。
- 切换两个角色，确认授权草稿独立加载。
- 勾选页面和对象动作、调整对象数据范围并保存。
- 重新加载页面确认回显；发布应用后再次确认授权未被覆盖。

## 本轮增量验证（2026-08-16：复用角色授权工作台）

- `application-permission-utils.spec.js`：应用页面和绑定对象正确转换为共享权限模块；待发布资源不可选；继承数据范围不进入保存 payload；Long ID 保持字符串。
- `role-permission-settings.spec.js`：应用模式勾选功能权限时不隐式勾选页面入口；角色默认数据范围只读。
- `role-permission-settings.spec.js`：应用模式勾选页面入口时不隐式勾选功能权限。
- `SysRoleServiceImplScopedPermissionTest`：范围化保存拒绝越界资源，并保留当前应用范围外的资源和模块授权。
- ESLint 覆盖 `ApplicationPermissionsPanel.vue`、`RolePermissionSettings.vue`、权限转换工具和对应测试。
- 生产构建验证共享角色授权组件同时被系统角色弹窗和应用工作台消费时无编译回归。
- 浏览器验证仍使用 3000 端口；若后端代理不可用，记录登录阻断和控制台证据，不把源码检查替代为真实交互通过。

## 本轮增量验证（2026-08-16：迁移数据范围适配）

- `BusinessApplicationPermissionServiceTest`：目录返回字段适配信息；保存时校验应用对象归属并把字段名映射为数据库列名。
- `application-permission-utils.spec.js`：对象模块携带就地适配动作，不再生成对象设计器跳转语义。
- `application-data-scope-adapter-modal.spec.js`：`FOLLOW_SYSTEM` 模式校验本人和组织字段，提交窄 DTO。
- `object-designer-navigation.spec.js`：旧 `permission` 深链兼容映射到树形模型，对象设计器导航不再出现数据范围适配。

## 环境限制

真实数据库写入、Flyway 和登录态接口联调由用户环境执行；本轮至少完成单元测试、聚合构建和本地页面只读验证。
