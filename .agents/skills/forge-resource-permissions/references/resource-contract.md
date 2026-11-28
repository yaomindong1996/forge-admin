# 资源与授权合同

| 资源类型 | 要核对的字段与用途 |
| --- | --- |
| `1` 目录 | 导航父级、`parent_id`、客户端和有效状态 |
| `2` 菜单 | `parent_id`、`path`、组件、客户端、访问角色 |
| `3` 按钮 | 所属页面、动作权限码 `perms`。前端与服务端动作守卫使用同一码 |
| `4` API | `api_method`、`api_url`、资源可见状态。角色按方法及路径获得 API 匹配表达式 |

- 从 [SysResourceServiceImpl](../../../../forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysResourceServiceImpl.java) 核对当前用户的菜单树、客户端和套餐可见性。仅 1/2 会进导航。查看 [SysResource Mapper](../../../../forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysResourceMapper.xml) 的 `selectConfiguredApiUrls` 和 `selectApiPermissionPatternsByResourceIds`，两处均要求受控 API `visible=1`。
- 资源自然键以当前表结构为准。社区版以 `tenant_id + resource_type + perms + client_code + del_flag` 查同一资源（[对应迁移](../../../../forge-server/db/mysql/migration/V1.0.133__align_resource_unique_key_with_client_code.sql)）。查父菜单时再核对 `path`、启用状态和 `del_flag`，保留已存在资源的 ID。管理端与 H5 各用实际客户端码。
- 匹配 Controller 的 HTTP 方法与服务内部路径，去掉网关前缀、Servlet context path 和查询串。优先用固定方法及路径，如 `POST /business/sample-purchase-order/export`。按钮和 API 资源使用不同 `perms`，因为角色各类型资源的权限码都可能进入服务端动作会话。
- `sys_role_resource` 使用目标角色所属的租户，与资源定义的内置租户分别核对。资源有父子关系，授权仍以角色的实际关联为准。确认用户在当前租户、当前组织的角色已启用。只授给 Spec 明确的角色。`POST /system/role/{roleId}/resources` 是替换授权集合，经接口维护时需要读取并合并原授权。
- [API 鉴权拦截器](../../../../forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/interceptor/ApiPermissionInterceptor.java) 对未配置路径有兼容放行，对明确匿名配置或注解豁免也会跳过 API 权限。核对 [Sa-Token MVC 配置](../../../../forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/config/SaTokenConfig.java) 和有效 API 鉴权开关，敏感动作保留 Controller 守卫。
- UI 从会话权限控制动作是否显示。自定义按钮对照所在页面已有的授权实现。`v-hasPermi` 在权限列表为空时会跳过检查，对受控动作结合权限加载状态和真实权限码使用 `v-if`。使用 AiCrudPage 的 `show-export` 时传计算后的布尔值；该 prop 自身只控制 UI。
- [用户权限加载](../../../../forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java) 将权限写入会话。SQL 升级、授予或撤销后重新加载资源配置缓存。用新会话验证 `GET /auth/current/menu`、`GET /auth/current/permissions` 和业务接口。检查同组普通账号是否已有 `/**`、按模块通配或管理身份，会扩大哪些操作。
