# 多租户模块使用文档

## 概述

`forge-starter-tenant` 提供了完整的多租户解决方案，基于 MyBatis-Plus 的多租户插件实现，支持：
- 自动在 SQL 中添加租户条件
- 从用户 Session 中动态获取租户ID
- 灵活的租户隔离配置
- 支持忽略特定表或方法的租户隔离

## 核心组件

### 1. TenantContextHolder - 租户上下文持有者

线程安全的租户上下文管理器，支持异步线程传递。

```java
// 设置租户ID
TenantContextHolder.setTenantId(1001L);

// 获取租户ID
Long tenantId = TenantContextHolder.getTenantId();

// 清除租户上下文
TenantContextHolder.clear();

// 忽略租户执行操作
TenantContextHolder.executeIgnore(() -> {
    // 这里的查询不会添加租户条件
    userMapper.selectList(null);
});

// 使用指定租户执行操作
TenantContextHolder.executeWithTenant(2001L, () -> {
    // 使用租户2001查询
    userMapper.selectList(null);
});
```

### 2. @IgnoreTenant - 忽略租户注解

用于标记不需要租户隔离的类或方法。

```java
// 在实体类上使用
@IgnoreTenant
@TableName("sys_config")
public class SysConfig extends BaseEntity {
    // 这个表不会自动添加租户条件
}

// 在Mapper方法上使用
public interface UserMapper extends BaseMapper<User> {
    
    @IgnoreTenant
    List<User> selectAllUsers();  // 查询所有租户的用户
}
```

### 3. TenantEntity - 租户基类

提供租户字段的基类。

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends TenantEntity {
    // 自动继承 tenantId 字段
    private Long id;
    private String username;
    // ...
}
```

## 配置说明

### application.yml 配置

```yaml
forge:
  tenant:
    # 是否启用多租户（默认：true）
    enabled: true
    
    # 租户字段名（默认：tenant_id）
    column: tenant_id
    
    # 严格模式（默认：false）
    # true: 没有租户ID时抛出异常
    # false: 没有租户ID时记录警告
    strict-mode: false
    
    # 忽略租户的表（这些表不会自动添加租户条件）
    ignore-tables:
      - sys_tenant          # 租户表本身
      - sys_config          # 系统配置表
      - sys_dict_type       # 字典类型表
      - sys_dict_data       # 字典数据表
      - sys_file_storage_config  # 文件存储配置
      - sys_login_log       # 登录日志（可选）
      - sys_operation_log   # 操作日志（可选）
```

### 数据库表设计

需要租户隔离的表都应该添加 `tenant_id` 字段：

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    username VARCHAR(50) NOT NULL,
    -- 其他字段...
    INDEX idx_tenant_id (tenant_id)
) COMMENT '用户表';

-- 不需要租户隔离的表可以不添加 tenant_id
CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500),
    -- 其他字段...
) COMMENT '系统配置表';
```

## 使用示例

### 1. 基本使用（自动模式）

系统会自动从用户 Session 中获取租户ID，并在所有 SQL 查询中添加租户条件。

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public List<User> listUsers() {
        // SQL: SELECT * FROM sys_user WHERE tenant_id = 当前租户ID
        return userMapper.selectList(null);
    }
    
    public User getById(Long id) {
        // SQL: SELECT * FROM sys_user WHERE id = ? AND tenant_id = 当前租户ID
        return userMapper.selectById(id);
    }
    
    public int save(User user) {
        // 保存时会自动设置租户ID
        return userMapper.insert(user);
    }
}
```

### 2. 手动设置租户ID

在某些特殊场景下（如定时任务、消息队列），需要手动设置租户ID：

```java
@Service
public class ScheduledTaskService {
    
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyTask() {
        // 获取所有租户
        List<SysTenant> tenants = tenantService.listAll();
        
        for (SysTenant tenant : tenants) {
            // 为每个租户执行任务
            TenantContextHolder.executeWithTenant(tenant.getId(), () -> {
                // 这里的操作会使用指定的租户ID
                processData();
            });
        }
    }
}
```

### 3. 忽略租户查询

某些情况下需要查询所有租户的数据：

```java
@Service
public class AdminService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 方式1：使用上下文忽略
    public List<User> getAllUsers() {
        return TenantContextHolder.executeIgnore(() -> {
            return userMapper.selectList(null);
        });
    }
    
    // 方式2：在Mapper上使用注解
    public List<User> getAllUsersWithAnnotation() {
        return userMapper.selectAllUsers(); // Mapper方法上有@IgnoreTenant
    }
}
```

### 4. 跨租户操作

超级管理员查看多个租户的数据：

```java
@Service
public class SuperAdminService {
    
    @Autowired
    private UserMapper userMapper;
    
    public Map<Long, List<User>> getUsersByTenants(List<Long> tenantIds) {
        Map<Long, List<User>> result = new HashMap<>();
        
        for (Long tenantId : tenantIds) {
            List<User> users = TenantContextHolder.executeWithTenant(tenantId, () -> {
                return userMapper.selectList(null);
            });
            result.put(tenantId, users);
        }
        
        return result;
    }
}
```

### 5. 数据权限结合

租户隔离与数据权限（部门、角色）结合使用：

```java
@Service
public class DataScopeService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    public List<Order> listOrders() {
        // 获取当前用户信息
        LoginUser user = SessionHelper.getLoginUser();
        
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        
        // 1. 租户隔离（自动添加）
        // SQL已自动添加: tenant_id = 当前租户ID
        
        // 2. 数据权限（手动添加）
        if (!user.isAdmin()) {
            // 只能查看自己部门的数据
            wrapper.eq("dept_id", user.getDeptId());
        }
        
        return orderMapper.selectList(wrapper);
    }
}
```

### 6. 实体类配置

```java
// 需要租户隔离的实体
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends TenantEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    // tenantId 从 TenantEntity 继承
    private String username;
    // ...
}

// 不需要租户隔离的实体
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
@IgnoreTenant  // 使用注解忽略
public class SysConfig extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    // ...
}
```

## 工作原理

### 1. 租户ID获取流程

```
用户登录 
  ↓
SessionHelper.setLoginUser(loginUser)  // 包含tenantId
  ↓
TenantInterceptor拦截请求
  ↓
从SessionHelper获取tenantId
  ↓
TenantContextHolder.setTenantId(tenantId)
  ↓
执行业务逻辑
  ↓
MyBatis-Plus拦截SQL
  ↓
DefaultTenantLineHandler添加租户条件
  ↓
SQL: WHERE ... AND tenant_id = ?
  ↓
请求结束，清除上下文
```

### 2. SQL拦截示例

原始SQL：
```sql
SELECT * FROM sys_user WHERE username = 'admin'
```

拦截后：
```sql
SELECT * FROM sys_user WHERE username = 'admin' AND tenant_id = 1001
```

## 最佳实践

### 1. 租户表设计规范

```java
/**
 * 推荐的表设计：
 * 1. 所有需要租户隔离的表都继承TenantEntity
 * 2. tenant_id字段添加索引
 * 3. 联合索引时将tenant_id放在前面
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_order", autoResultMap = true)
public class SysOrder extends TenantEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String orderNo;
    private Long userId;
    // ...
    
    // 推荐的索引：
    // INDEX idx_tenant_user (tenant_id, user_id)
    // INDEX idx_tenant_orderno (tenant_id, order_no)
}
```

### 2. 系统级操作

```java
@Service
public class SystemService {
    
    /**
     * 系统初始化等操作，不需要租户隔离
     */
    @IgnoreTenant
    public void initSystemData() {
        // 初始化字典数据、系统配置等
        dictService.initDict();
        configService.initConfig();
    }
    
    /**
     * 或者使用上下文忽略
     */
    public void loadAllConfigs() {
        TenantContextHolder.executeIgnore(() -> {
            configMapper.selectList(null);
        });
    }
}
```

### 3. 异步任务中使用

```java
@Service
public class AsyncTaskService {
    
    @Async
    public void processAsync(Long orderId) {
        // 异步任务中需要手动传递租户ID
        Long tenantId = SessionHelper.getTenantId();
        
        CompletableFuture.runAsync(() -> {
            TenantContextHolder.executeWithTenant(tenantId, () -> {
                // 处理业务
                orderService.process(orderId);
            });
        });
    }
}
```

### 4. 事务中的租户管理

```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(OrderDTO dto) {
        // 事务开始前租户ID已经设置
        // 整个事务过程中租户ID保持不变
        
        Order order = new Order();
        // tenantId会自动填充
        orderMapper.insert(order);
        
        OrderDetail detail = new OrderDetail();
        detailMapper.insert(detail);
        
        // 提交事务后，租户ID在请求结束时统一清除
    }
}
```

### 5. 租户数据迁移

```java
@Service
public class DataMigrationService {
    
    /**
     * 从租户A迁移数据到租户B
     */
    public void migrateData(Long fromTenantId, Long toTenantId) {
        // 1. 从源租户读取数据
        List<User> users = TenantContextHolder.executeWithTenant(fromTenantId, () -> {
            return userMapper.selectList(null);
        });
        
        // 2. 写入目标租户
        TenantContextHolder.executeWithTenant(toTenantId, () -> {
            users.forEach(user -> {
                user.setId(null); // 清除ID，让其自动生成
                userMapper.insert(user);
            });
        });
    }
}
```

## 注意事项

1. **租户ID的来源**
   - 优先从 SessionHelper 获取（需要引入 forge-starter-auth）
   - 其次从请求头 `X-Tenant-Id` 获取
   - 可以手动设置 `TenantContextHolder.setTenantId()`

2. **忽略租户的场景**
   - 系统配置表（sys_config、sys_dict_*）
   - 租户表本身（sys_tenant）
   - 全局性的日志表（可选）
   - 第三方集成表（可选）

3. **性能优化**
   - 为 tenant_id 字段添加索引
   - 联合查询时将 tenant_id 放在索引前面
   - 合理使用缓存减少数据库查询

4. **安全性**
   - 严格验证租户ID的合法性
   - 防止租户ID伪造或篡改
   - 敏感操作需要二次验证租户权限

5. **调试技巧**
   - 开启 MyBatis-Plus SQL 日志查看实际执行的 SQL
   - 使用 `log.debug` 查看租户ID的设置和清除
   - 监控租户上下文的生命周期

## 常见问题

### Q1: 为什么查询结果为空？

**A**: 检查以下几点：
1. 当前用户的租户ID是否正确
2. 数据表中是否有对应租户的数据
3. 表是否被错误地添加到了忽略列表中

### Q2: 如何在定时任务中使用多租户？

**A**: 定时任务中需要手动设置租户ID：
```java
@Scheduled(cron = "...")
public void task() {
    List<Tenant> tenants = getTenants();
    for (Tenant tenant : tenants) {
        TenantContextHolder.executeWithTenant(tenant.getId(), () -> {
            // 执行任务
        });
    }
}
```

### Q3: 跨租户查询怎么处理？

**A**: 使用 `TenantContextHolder.executeIgnore()` 或 `@IgnoreTenant` 注解。

### Q4: 租户ID什么时候清除？

**A**: 在请求结束时，`TenantInterceptor.afterCompletion()` 会自动清除。

## 集成示例

### 完整的系统集成

```java
// 1. 引入依赖
// pom.xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-tenant</artifactId>
</dependency>

// 2. 配置
// application.yml
forge:
  tenant:
    enabled: true
    column: tenant_id

// 3. 实体类
@Data
@TableName("sys_user")
public class SysUser extends TenantEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
}

// 4. Service
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    public List<User> list() {
        // 自动添加租户条件
        return userMapper.selectList(null);
    }
}
```

完成！多租户模块已经可以正常使用了。
