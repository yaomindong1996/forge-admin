# 数据权限模块

基于 MyBatis Plus 的数据权限控制模块，支持多种权限范围。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-datascope</artifactId>
</dependency>
```

## 权限范围

| 类型 | Code | 说明 |
|------|------|------|
| ALL | 1 | 全部数据权限 |
| SELF | 2 | 本人数据权限 |
| ORG | 3 | 本组织数据权限 |
| ORG_AND_CHILD | 4 | 本组织及子组织数据权限 |
| CUSTOM | 5 | 自定义数据权限 |
| TENANT_ALL | 6 | 租户全部数据权限 |

## 使用示例

### 配置数据权限

```java
// 在角色中配置数据权限范围
public class SysRole {
    private Long roleId;
    private String roleName;
    private Integer dataScope;  // 数据权限范围
    // ...
}
```

### 自定义权限配置

```java
// 配置自定义权限范围
public class SysDataScopeConfig {
    private Long configId;
    private Long roleId;
    private String tableName;
    private String scopeSql;  // 自定义 SQL 条件
}
```

### 在 Mapper 中使用

数据权限会自动拦截 SQL 并追加权限条件：

```java
// 原始 SQL
SELECT * FROM sys_user

// 自动追加权限条件后
SELECT * FROM sys_user WHERE dept_id IN (1, 2, 3)
```

## API 参考

### IDataScopeService

```java
public interface IDataScopeService {
    /**
     * 获取用户的数据权限 SQL
     */
    String getDataScopeSql(Long userId, String tableName, String alias);
    
    /**
     * 获取用户的数据权限部门列表
     */
    List<Long> getDataScopeDeptIds(Long userId);
}
```

### DataScopeContextHolder

```java
// 设置当前用户 ID
DataScopeContextHolder.setUserId(userId);

// 获取当前用户 ID
Long userId = DataScopeContextHolder.getUserId();

// 清除上下文
DataScopeContextHolder.clear();
```

## 配置

```yaml
forge:
  datascope:
    enabled: true
    # 排除的表
    exclude-tables:
      - sys_config
      - sys_dict_type
```

## 自定义权限处理器

```java
@Component
public class MyDataScopeHandler implements DataScopeHandler {
    
    @Override
    public String buildDataScopeSql(Long userId, String tableName, String alias) {
        // 自定义权限 SQL 构建逻辑
        return "org_id = 1";
    }
}
```

## 注意事项

1. 数据权限基于 MyBatis Plus 拦截器实现
2. 需要在用户登录时设置 DataScopeContextHolder
3. 自定义 SQL 需要使用 ${params.dataScope} 占位符
4. 复杂查询建议使用自定义权限处理器