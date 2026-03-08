# 数据权限配置管理使用指南

## 功能概述

数据权限配置管理模块允许通过可视化界面配置和管理系统的数据权限规则，无需手动修改代码即可实现细粒度的数据访问控制。

## 数据库表结构

### sys_data_scope_config（数据权限配置表）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键ID |
| tenant_id | bigint | 租户编号 |
| resource_code | varchar(100) | 资源编码（如：system:user:list） |
| resource_name | varchar(100) | 资源名称 |
| mapper_method | varchar(255) | Mapper方法全限定名 |
| table_alias | varchar(50) | 主表别名（如：t、lc、o） |
| user_id_column | varchar(255) | 用户ID字段（支持简单模式和SQL模式） |
| org_id_column | varchar(255) | 组织ID字段（支持简单模式和SQL模式） |
| tenant_id_column | varchar(255) | 租户ID字段（支持简单模式和SQL模式） |
| enabled | tinyint | 是否启用（0-禁用，1-启用） |
| remark | varchar(500) | 备注 |
| create_by | bigint | 创建人ID |
| create_time | datetime | 创建时间 |
| update_by | bigint | 更新人ID |
| update_time | datetime | 更新时间 |

## 安装步骤

### 1. 执行数据库脚本

```bash
# 执行菜单配置SQL（位于 forge/forge-admin/sql/data_scope_menu.sql）
# 注意：执行前需要修改SQL中的parent_id为"系统管理"菜单的实际ID
```

### 2. 分配权限

在系统管理 → 角色管理中，为相应角色分配"数据权限配置"菜单及其子权限。

### 3. 重启后端服务

```bash
cd forge/forge-admin
mvn clean package
java -jar target/forge-admin.jar
```

### 4. 重启前端服务

```bash
cd forge-admin-ui
pnpm install
pnpm dev
```

## 配置说明

### 基础信息配置

#### 资源编码
- 唯一标识一个资源，建议格式：`模块:功能:操作`
- 示例：`system:user:list`（系统管理-用户-列表查询）

#### 资源名称
- 资源的描述性名称，便于识别
- 示例：`用户列表查询`

#### Mapper方法
- 需要进行数据权限控制的Mapper方法全限定名
- 格式：`com.mdframe.forge.plugin.system.mapper.XxxMapper.methodName`
- 示例：`com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectList`

### 字段配置

#### 表别名
- 主表的别名，用于构建SQL条件
- 常用值：`t`、`lc`、`o`、`u`等
- 示例：如果SQL是 `SELECT * FROM sys_user t WHERE ...`，则表别名为 `t`

#### 用户ID字段
配置用户ID相关的数据权限规则，支持两种模式：

**简单模式（直接字段名）**
```
user_id
```
生成的SQL条件：`AND t.user_id = #{userId}`

```
create_by
```
生成的SQL条件：`AND t.create_by = #{userId}`

**复杂模式（SQL语句）**
以 `<sql>` 开头，可以使用占位符：
- `#{userId}` - 当前用户ID
- `#{tenantId}` - 当前租户ID
- `#{orgIds}` - 当前用户所属组织及子组织ID列表
- `#{customOrgIds}` - 自定义组织ID列表（需要在前端传入）

示例：
```sql
<sql>
(t.user_id = #{userId} OR t.create_by = #{userId})
AND t.org_id IN (#{orgIds})
```

#### 组织ID字段
配置组织ID相关的数据权限规则，同样支持两种模式：

**简单模式**
```
org_id
```
生成的SQL条件：`AND t.org_id IN (#{orgIds})`

**复杂模式**
```sql
<sql>
t.org_id IN (
  SELECT org_id FROM sys_org 
  WHERE FIND_IN_SET(#{orgId}, ancestors)
)
```

#### 租户ID字段
配置租户隔离的数据权限规则：

**简单模式**
```
tenant_id
```
生成的SQL条件：`AND t.tenant_id = #{tenantId}`

**复杂模式**
```sql
<sql>
(t.tenant_id = #{tenantId} OR t.is_public = 1)
```

## 使用示例

### 示例1：用户列表 - 仅查看自己的数据

```
资源编码: system:user:list
资源名称: 用户列表
Mapper方法: com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectList
表别名: u
用户ID字段: user_id
组织ID字段: org_id
租户ID字段: tenant_id
```

生成的SQL条件：
```sql
AND u.user_id = #{userId}
AND u.org_id IN (#{orgIds})
AND u.tenant_id = #{tenantId}
```

### 示例2：订单列表 - 查看自己创建的订单

```
资源编码: business:order:list
资源名称: 订单列表
Mapper方法: com.mdframe.forge.plugin.business.mapper.OrderMapper.selectList
表别名: o
用户ID字段: create_by
组织ID字段: dept_id
租户ID字段: tenant_id
```

生成的SQL条件：
```sql
AND o.create_by = #{userId}
AND o.dept_id IN (#{orgIds})
AND o.tenant_id = #{tenantId}
```

### 示例3：复杂权限 - 查看自己及下级的数据

```
资源编码: system:report:list
资源名称: 报表列表
Mapper方法: com.mdframe.forge.plugin.system.mapper.SysReportMapper.selectList
表别名: r
用户ID字段: <sql>r.user_id = #{userId} OR r.create_by = #{userId}
组织ID字段: <sql>r.org_id IN (SELECT org_id FROM sys_org WHERE FIND_IN_SET(#{orgId}, ancestors))
租户ID字段: tenant_id
```

生成的SQL条件：
```sql
AND (r.user_id = #{userId} OR r.create_by = #{userId})
AND r.org_id IN (SELECT org_id FROM sys_org WHERE FIND_IN_SET(#{orgId}, ancestors))
AND r.tenant_id = #{tenantId}
```

## 技术原理

### 工作流程

1. **配置读取**：系统启动时从数据库读取所有启用的数据权限配置
2. **缓存机制**：配置信息缓存到内存中，提高性能
3. **拦截器**：MyBatis拦截器拦截Mapper方法执行
4. **SQL拼接**：根据配置的规则动态拼接WHERE条件
5. **权限控制**：最终SQL只返回用户有权限访问的数据

### 核心类

- `SysDataScopeConfig` - 数据权限配置实体
- `SysDataScopeConfigMapper` - 数据权限配置Mapper
- `SysDataScopeConfigService` - 数据权限配置Service
- `SysDataScopeConfigController` - 数据权限配置Controller
- `DataScopeInterceptor` - MyBatis拦截器（数据权限核心）

### 缓存刷新

配置修改后，系统会自动刷新数据权限缓存：
- 新增配置：立即生效
- 修改配置：立即生效
- 删除配置：立即生效
- 启用/禁用：立即生效

## 注意事项

1. **表别名必须正确**：表别名必须与Mapper XML中定义的别名一致
2. **字段名必须存在**：字段名必须在表中真实存在
3. **SQL模式谨慎使用**：SQL模式灵活但复杂，需要确保SQL语法正确
4. **性能考虑**：复杂SQL可能影响查询性能，建议使用EXPLAIN分析
5. **权限测试**：配置完成后务必进行充分的权限测试
6. **备份数据**：修改配置前建议先备份数据

## 常见问题

### Q1: 配置后没有生效？
A: 请检查：
- 配置是否已启用（enabled = 1）
- Mapper方法路径是否正确
- 表别名是否与XML中一致
- 是否刷新了缓存（修改配置会自动刷新，但需要重新查询）

### Q2: SQL语法错误？
A: 请检查：
- 复杂模式的SQL是否以`<sql>`开头
- 占位符格式是否正确（#{userId}等）
- SQL语法是否合法

### Q3: 查询结果为空？
A: 请检查：
- 字段名是否正确
- 表别名是否正确
- 当前用户是否有符合条件的数据

### Q4: 如何临时禁用某个配置？
A: 在数据权限配置页面，将"是否启用"改为"禁用"即可

## 扩展开发

### 自定义数据权限规则

如果需要更复杂的数据权限控制，可以：

1. 继承 `DataScopeInterceptor` 类
2. 重写 `buildSqlCondition` 方法
3. 在 `application.yml` 中配置自定义拦截器

### 新增占位符

在 `DataScopeInterceptor` 中添加新的占位符参数：
```java
params.put("customParam", getCustomParam());
```

然后在配置中使用：`#{customParam}`

## 版本历史

- v1.0.0 (2025-01-20)
  - 初始版本
  - 支持简单模式和复杂SQL模式
  - 支持用户、组织、租户三种维度的数据权限
  - 自动缓存刷新机制

## 技术支持

如有问题或建议，请联系开发团队。
