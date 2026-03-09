# 多租户模块

基于 MyBatis Plus 的多租户隔离模块。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-tenant</artifactId>
</dependency>
```

## 配置

```yaml
forge:
  tenant:
    enabled: true
    column: tenant_id
    ignore-tables:
      - sys_tenant
      - sys_dict_type
```

## 使用示例

### 租户实体基类

```java
public class TenantEntity {
    private Long tenantId;  // 租户 ID
    // ...
}

// 业务实体继承
public class User extends TenantEntity {
    private Long userId;
    private String username;
    // ...
}
```

### 租户上下文

```java
// 设置当前租户
TenantContextHolder.setTenantId(1L);

// 获取当前租户
Long tenantId = TenantContextHolder.getTenantId();

// 清除租户上下文
TenantContextHolder.clear();
```

### 忽略租户隔离

```java
@IgnoreTenant
public List<User> listAllUsers() {
    // 此查询不添加租户条件
    return userMapper.selectList(null);
}

// 或使用 TenantUtil
public List<User> listAllUsers() {
    return TenantUtil.ignore(() -> {
        return userMapper.selectList(null);
    });
}
```

## API 参考

### TenantContextHolder

```java
// 设置租户 ID
TenantContextHolder.setTenantId(tenantId);

// 获取租户 ID
Long tenantId = TenantContextHolder.getTenantId();

// 判断是否已设置租户
boolean hasTenant = TenantContextHolder.hasTenant();

// 清除上下文
TenantContextHolder.clear();
```

### TenantUtil

```java
// 在忽略租户隔离的上下文中执行
TenantUtil.ignore(() -> {
    // 不添加租户条件的操作
});

// 在指定租户上下文中执行
TenantUtil.execute(tenantId, () -> {
    // 使用指定租户 ID 的操作
});
```

## 注意事项

1. 租户字段默认为 `tenant_id`
2. 需要在请求拦截器中设置租户上下文
3. 忽略租户隔离的操作需谨慎使用
4. 建议所有业务表都添加租户字段