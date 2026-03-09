# 字典翻译模块

自动翻译实体类中的字典字段，支持多种翻译策略。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-trans</artifactId>
</dependency>
```

## 注解使用

### @DictTrans 类注解

启用类的字典翻译能力：

```java
@DictTrans
public class UserVO {
    // ...
}
```

### @TransField 字段注解

标记需要翻译的字段：

```java
@DictTrans
public class UserVO {
    private Integer status;
    
    @TransField(dictType = "sys_status")
    private String statusName;  // 自动翻译 status 字段
    
    @TransField(expression = "userCache.getById(userId).name")
    private String userName;  // 表达式翻译
}
```

| 属性 | 说明 |
|------|------|
| dictType | 字典类型 |
| expression | SpEL 表达式 |
| sourceField | 源字段（默认同名字段去掉 Name 后缀） |

## 使用示例

### 字典翻译

```java
@DictTrans
public class OrderVO {
    private Integer status;
    
    @TransField(dictType = "order_status")
    private String statusName;
    
    private Integer payType;
    
    @TransField(dictType = "pay_type")
    private String payTypeName;
}

// 调用翻译
@Autowired
private TransManager transManager;

public List<OrderVO> listOrders() {
    List<OrderVO> orders = orderMapper.selectList();
    transManager.translate(orders);  // 自动翻译
    return orders;
}
```

### 表达式翻译

```java
@DictTrans
public class TaskVO {
    private Long userId;
    
    @TransField(expression = "userCache.getById(userId).nickname")
    private String userName;
    
    private Long deptId;
    
    @TransField(expression = "deptCache.getById(deptId).name")
    private String deptName;
}
```

### 枚举翻译

```java
public enum Status {
    ENABLE(1, "启用"),
    DISABLE(0, "禁用");
    
    private final Integer code;
    private final String desc;
}

@DictTrans
public class UserVO {
    private Status status;
    
    @TransField
    private String statusName;  // 自动翻译枚举
}
```

## SPI 扩展

### DictValueProvider

自定义字典值提供者：

```java
@Component
public class MyDictValueProvider implements DictValueProvider {
    @Override
    public String getDictLabel(String dictType, String dictValue) {
        // 返回字典标签
        DictData dict = dictMapper.selectByTypeAndValue(dictType, dictValue);
        return dict != null ? dict.getDictLabel() : null;
    }
    
    @Override
    public int getOrder() {
        return 0;
    }
}
```

## TransManager API

```java
@Autowired
private TransManager transManager;

// 翻译单个对象
transManager.translate(userVO);

// 翻译列表
transManager.translate(userList);

// 翻译并返回新对象
UserVO translated = transManager.translateAndReturn(userVO);
```