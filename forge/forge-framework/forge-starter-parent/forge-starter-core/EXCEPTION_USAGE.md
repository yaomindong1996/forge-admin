# 异常处理使用示例

## 概述

`forge-starter-core` 提供了完整的异常处理体系，包括：
- `BusinessException` - 业务异常类
- `ExceptionUtil` - 异常工具类
- `GlobalExceptionHandler` - 全局异常处理器

## 1. 业务异常类（BusinessException）

业务异常类用于封装业务逻辑中的错误信息。

### 基本使用

```java
// 抛出默认异常
throw new BusinessException();

// 带消息的异常
throw new BusinessException("用户不存在");

// 带状态码和消息的异常
throw new BusinessException(404, "用户不存在");

// 带状态码、消息和详细数据的异常
throw new BusinessException(400, "参数验证失败", validationErrors);

// 链式调用设置属性
throw new BusinessException()
    .setCode(400)
    .setMessage("参数错误")
    .setData(errorDetails);
```

## 2. 异常工具类（ExceptionUtil）

提供了多种便捷的静态方法来抛出异常。

### 基本抛出方法

```java
// 直接抛出异常
ExceptionUtil.throwBiz("操作失败");

// 带状态码的异常
ExceptionUtil.throwBiz(400, "参数错误");

// 带详细数据的异常
ExceptionUtil.throwBiz(400, "参数验证失败", validationErrors);
```

### 条件判断方法

```java
// 如果条件为true则抛出异常
ExceptionUtil.throwIf(user == null, "用户不存在");
ExceptionUtil.throwIf(age < 18, 400, "年龄必须大于18岁");

// 如果条件为false则抛出异常
ExceptionUtil.throwIfNot(user.isActive(), "用户未激活");
ExceptionUtil.throwIfNot(hasPermission, 403, "没有权限");
```

### 空值检查方法

```java
// 检查对象是否为null
ExceptionUtil.throwIfNull(user, "用户不能为空");
ExceptionUtil.throwIfNull(order, 404, "订单不存在");

// 检查字符串是否为空
ExceptionUtil.throwIfBlank(username, "用户名不能为空");
ExceptionUtil.throwIfBlank(password, 400, "密码不能为空");
```

### 异常包装方法

```java
try {
    // 可能抛出检查型异常的代码
    someMethodThrowsCheckedException();
} catch (Exception e) {
    // 包装为运行时异常
    throw ExceptionUtil.wrap(e);
    
    // 包装并自定义消息
    throw ExceptionUtil.wrap(e, "处理失败");
    
    // 包装并指定状态码和消息
    throw ExceptionUtil.wrap(e, 500, "系统错误");
}
```

### 条件返回方法

```java
// 如果条件为true则抛出异常，否则返回对象
User user = ExceptionUtil.throwIfOrElse(
    userId == null, 
    "用户ID不能为空", 
    userService.getById(userId)
);
```

## 3. 全局异常处理器（GlobalExceptionHandler）

自动拦截并处理各种异常，返回统一的响应格式。

### 支持的异常类型

1. **业务异常** - `BusinessException`
2. **参数校验异常** - `@Valid`、`@Validated` 注解触发
3. **参数绑定异常** - URL参数绑定错误
4. **约束违反异常** - Bean Validation 约束违反
5. **缺少参数异常** - 必需的请求参数缺失
6. **参数类型不匹配** - 参数类型转换失败
7. **请求方法不支持** - HTTP方法不匹配
8. **404异常** - 资源不存在
9. **访问拒绝异常** - 权限不足
10. **文件上传超限** - 文件大小超过限制
11. **空指针异常** - NullPointerException
12. **非法参数异常** - IllegalArgumentException
13. **运行时异常** - 其他运行时异常
14. **未知异常** - 兜底处理

### 响应格式

所有异常都会被转换为统一的响应格式：

```json
{
  "code": 500,
  "message": "错误消息",
  "data": null,
  "timestamp": 1702012345678
}
```

## 4. 实际使用示例

### 示例1：用户服务

```java
@Service
public class UserService {
    
    public User getUserById(Long userId) {
        // 参数校验
        ExceptionUtil.throwIfNull(userId, 400, "用户ID不能为空");
        
        // 查询用户
        User user = userMapper.selectById(userId);
        
        // 检查结果
        ExceptionUtil.throwIfNull(user, 404, "用户不存在");
        
        return user;
    }
    
    public void updateUser(User user) {
        // 检查用户是否存在
        User existUser = userMapper.selectById(user.getId());
        ExceptionUtil.throwIfNull(existUser, 404, "用户不存在");
        
        // 检查用户状态
        ExceptionUtil.throwIfNot(existUser.isActive(), 400, "用户未激活，无法修改");
        
        // 更新用户
        int rows = userMapper.updateById(user);
        ExceptionUtil.throwIf(rows == 0, "更新用户失败");
    }
    
    public void deleteUser(Long userId) {
        // 参数校验
        ExceptionUtil.throwIfNull(userId, 400, "用户ID不能为空");
        
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        ExceptionUtil.throwIfNull(user, 404, "用户不存在");
        
        // 检查是否可删除
        if (user.isSystemUser()) {
            throw new BusinessException(403, "系统用户不允许删除");
        }
        
        // 删除用户
        userMapper.deleteById(userId);
    }
}
```

### 示例2：订单服务

```java
@Service
public class OrderService {
    
    public Order createOrder(OrderDTO orderDTO) {
        // 参数校验
        ExceptionUtil.throwIfNull(orderDTO, "订单信息不能为空");
        ExceptionUtil.throwIfBlank(orderDTO.getProductId(), "商品ID不能为空");
        
        // 检查商品库存
        Product product = productService.getById(orderDTO.getProductId());
        ExceptionUtil.throwIfNull(product, 404, "商品不存在");
        ExceptionUtil.throwIf(product.getStock() < orderDTO.getQuantity(), 
            400, "商品库存不足");
        
        // 创建订单
        Order order = new Order();
        // ... 设置订单信息
        
        try {
            orderMapper.insert(order);
        } catch (Exception e) {
            throw ExceptionUtil.wrap(e, 500, "创建订单失败");
        }
        
        return order;
    }
    
    public void cancelOrder(Long orderId) {
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        ExceptionUtil.throwIfNull(order, 404, "订单不存在");
        
        // 检查订单状态
        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException(400, "订单已支付，无法取消");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(400, "订单已完成，无法取消");
        }
        
        // 取消订单
        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);
    }
}
```

### 示例3：Controller层参数校验

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping
    public RespInfo<User> createUser(@Valid @RequestBody UserDTO userDTO) {
        // @Valid注解会自动触发参数校验
        // 如果校验失败，GlobalExceptionHandler会自动处理
        User user = userService.createUser(userDTO);
        return RespInfo.success(user);
    }
    
    @GetMapping("/{id}")
    public RespInfo<User> getUser(@PathVariable Long id) {
        // 业务异常会被GlobalExceptionHandler自动捕获并处理
        User user = userService.getUserById(id);
        return RespInfo.success(user);
    }
}
```

### 示例4：复杂业务场景

```java
@Service
public class PaymentService {
    
    @Transactional(rollbackFor = Exception.class)
    public void processPayment(Long orderId, PaymentRequest request) {
        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);
        ExceptionUtil.throwIfNull(order, 404, "订单不存在");
        
        // 2. 验证订单状态
        ExceptionUtil.throwIfNot(
            order.getStatus() == OrderStatus.PENDING,
            400,
            "订单状态不正确，当前状态：" + order.getStatus()
        );
        
        // 3. 验证支付金额
        ExceptionUtil.throwIfNot(
            request.getAmount().equals(order.getTotalAmount()),
            400,
            "支付金额不匹配"
        );
        
        // 4. 调用支付网关
        PaymentResult result;
        try {
            result = paymentGateway.pay(request);
        } catch (Exception e) {
            throw ExceptionUtil.wrap(e, 500, "支付网关调用失败");
        }
        
        // 5. 检查支付结果
        if (!result.isSuccess()) {
            throw new BusinessException(400, "支付失败：" + result.getMessage());
        }
        
        // 6. 更新订单状态
        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(result.getPaymentId());
        orderMapper.updateById(order);
    }
}
```

## 5. 最佳实践

1. **优先使用 ExceptionUtil 工具类**
   - 代码更简洁
   - 语义更清晰
   
2. **合理设置错误码**
   - 400: 客户端参数错误
   - 401: 未认证
   - 403: 无权限
   - 404: 资源不存在
   - 500: 服务器内部错误

3. **提供清晰的错误消息**
   - 消息应该告诉用户发生了什么
   - 消息应该告诉用户如何解决

4. **使用参数校验注解**
   - 在DTO中使用 `@NotNull`、`@NotBlank` 等注解
   - 在Controller方法参数上使用 `@Valid` 或 `@Validated`

5. **日志记录**
   - GlobalExceptionHandler 会自动记录异常日志
   - 业务异常记录为 WARN 级别
   - 系统异常记录为 ERROR 级别

## 6. 自动配置

异常处理器会通过 Spring Boot 自动配置机制自动启用，无需额外配置。

如果需要自定义配置，可以创建自己的配置类：

```java
@Configuration
public class CustomExceptionConfig {
    
    @Bean
    @Primary
    public GlobalExceptionHandler customGlobalExceptionHandler() {
        // 返回自定义的异常处理器
        return new CustomGlobalExceptionHandler();
    }
}
```
