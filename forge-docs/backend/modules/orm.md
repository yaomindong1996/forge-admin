# MyBatis-Plus 集成（forge-starter-orm）

MyBatis-Plus 集成模块，提供自动填充、分页、乐观锁等开箱即用功能。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-orm</artifactId>
</dependency>
```

### 2. 配置 Mapper 扫描包路径

```yaml
mybatis-plus:
  mapperPackage: com.mdframe.forge.**.mapper
```

## 核心功能

### 自动填充

`InjectionMetaObjectHandler` 会自动填充以下审计字段：

#### 插入时（`insertFill`）

| 字段 | 值 |
|------|-----|
| `createTime` | `LocalDateTime.now()` |
| `updateTime` | `LocalDateTime.now()` |
| `createBy` | 当前登录用户 ID（`SessionHelper.getLoginUser().getUserId()`） |
| `updateBy` | 当前登录用户 ID |
| `createDept` | 当前登录用户所属部门 ID（`loginUser.getOrgIds().get(0)`） |

#### 更新时（`updateFill`）

| 字段 | 值 |
|------|-----|
| `updateTime` | `LocalDateTime.now()` |
| `updateBy` | 当前登录用户 ID（`SessionHelper.getUserId()`） |

### 实体类使用示例

```java
@Data
@TableName("sys_user")
public class User extends BaseEntity {

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createDept;

    // 业务字段...
    private String username;
    private String phone;
}
```

> **提示**：继承 `BaseEntity` 即可享受自动填充功能。

### 分页插件

自动注册 `PaginationInnerInterceptor`，支持 MyBatis-Plus 分页查询：

```java
Page<User> page = new Page<>(1, 10);
userMapper.selectPage(page, Wrappers.<User>lambdaQuery()
    .eq(User::getStatus, 1));
```

### 乐观锁

自动注册 `OptimisticLockerInnerInterceptor`，支持乐观锁：

```java
@Version
private Integer version;
```

### 分布式 ID 生成器

自动注册集群安全的 Snowflake ID 生成器：

```java
@Bean
public IdentifierGenerator identifierGenerator() {
    return new DefaultIdentifierGenerator(NetUtil.getLocalhost());
}
```

## 配置项

```yaml
mybatis-plus:
  mapperPackage: com.mdframe.forge.**.mapper
  mapperLocations: classpath*:mapper/**/*.xml
  typeAliasesPackage: com.mdframe.forge.**.entity
  global-config:
    db-config:
      id-type: ASSIGN_ID        # Snowflake ID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## 扩展点

模块支持自动发现其他模块注册的 `InnerInterceptor`，可通过实现 `InnerInterceptor` 接口并注册为 Spring Bean 来扩展拦截器。
