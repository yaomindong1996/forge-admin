# 认证授权模块

基于 Spring Security 的认证授权模块。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-auth</artifactId>
</dependency>
```

## 配置

```yaml
forge:
  auth:
    token-header: Authorization
    token-prefix: Bearer
    secret: your-secret-key
    expiration: 86400
    refresh-expiration: 604800
```

## 使用示例

### 登录认证

```java
@PostMapping("/login")
public Result login(@RequestBody LoginRequest request) {
    // 验证用户名密码
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
    );
    
    // 生成 Token
    String token = tokenService.generateToken(authentication);
    
    return Result.success(token);
}
```

### 权限控制

```java
// 方法级权限控制
@PreAuthorize("hasRole('admin')")
@GetMapping("/admin")
public Result adminOnly() {
    return Result.success();
}

@PreAuthorize("hasPermission('user', 'add')")
@PostMapping("/user")
public Result addUser(@RequestBody User user) {
    return Result.success();
}
```

### 获取当前用户

```java
@GetMapping("/current")
public Result getCurrentUser() {
    LoginUser user = SecurityUtils.getLoginUser();
    return Result.success(user);
}
```

## 注解说明

| 注解 | 说明 |
|------|------|
| @PreAuthorize | 方法执行前验证权限 |
| @PostAuthorize | 方法执行后验证权限 |
| @PreFilter | 过滤方法参数 |
| @PostFilter | 过滤返回值 |

## 权限表达式

| 表达式 | 说明 |
|------|------|
| hasRole('admin') | 拥有 admin 角色 |
| hasAnyRole('admin', 'user') | 拥有任一角色 |
| hasAuthority('user:add') | 拥有 user:add 权限 |
| hasAnyAuthority('user:add', 'user:edit') | 拥有任一权限 |
| hasPermission('user', 'add') | 自定义权限验证 |
| isAuthenticated() | 已认证 |
| isAnonymous() | 匿名用户 |

## Token 刷新

```java
@PostMapping("/refresh")
public Result refreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
    String newToken = tokenService.refreshToken(refreshToken);
    return Result.success(newToken);
}
```

## 安全配置

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                .requestMatchers("/login", "/captcha").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```