# 任务拆分 — 分布式幂等防重组件（美团GTIS方案实现）
> 拆分顺序：基础结构 → 核心注解 → 生成器 → 存储服务 → 切面/拦截器 → 配置 → 测试
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名
## 前置条件
- [x] 需求Spec已确认
- [x] 所有待澄清问题已解决
## Task 1: 创建幂等组件模块结构与基础依赖
- **目标**: 创建forge-starter-idempotent启动器模块，定义基础依赖和自动配置结构
- **涉及文件**:
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/pom.xml` — 新增，依赖Spring Boot、Redis、AOP等
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — 新增，自动配置注册
    - `forge/forge-framework/forge-starter-parent/pom.xml` — 修改，添加forge-starter-idempotent模块
## Task 2: 定义@Idempotent注解与配置类
- **目标**: 定义幂等注解和自动配置属性类
- **涉及文件**:
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/annotation/Idempotent.java` — 新增，注解定义
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/properties/IdempotentProperties.java` — 新增，配置属性类
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/constant/IdempotentConstant.java` — 新增，常量定义
- **关键签名**:
  ```java
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Idempotent {
      // 幂等键前缀
      String prefix() default "";
      // 过期时间（秒），默认10分钟
      int expire() default 600;
      // SpEL表达式，自定义幂等键
      String key() default "";
      // 校验失败提示信息
      String message() default "请勿重复提交";
      // 是否删除幂等键（执行成功后删除，允许后续重复执行）
      boolean deleteKeyAfterSuccess() default false;
  }
  ```
## Task 3: 实现幂等键生成器
- **目标**: 实现默认幂等键生成策略和SpEL表达式支持
- **涉及文件**:
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/generator/IdempotentKeyGenerator.java` — 新增，生成器接口
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/generator/DefaultIdempotentKeyGenerator.java` — 新增，默认生成实现
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/util/SpelUtil.java` — 新增，SpEL解析工具
## Task 4: 实现Redis幂等存储服务
- **目标**: 实现基于Redis的幂等存储服务，支持原子操作、降级逻辑
- **涉及文件**:
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/service/IdempotentStorageService.java` — 新增，存储服务接口
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/service/RedisIdempotentStorageService.java` — 新增，Redis实现
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/exception/IdempotentException.java` — 新增，自定义异常
## Task 5: 实现AOP切面与Web拦截器
- **目标**: 实现幂等校验的AOP切面和Web请求拦截器
- **涉及文件**:
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/aop/IdempotentAspect.java` — 新增，AOP切面实现
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/interceptor/IdempotentInterceptor.java` — 新增，Web拦截器（可选）
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/config/IdempotentAutoConfiguration.java` — 新增，自动配置类
## Task 6: 实现全局开关与异常处理
- **目标**: 实现全局配置开关和统一异常处理
- **涉及文件**:
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/handler/IdempotentExceptionHandler.java` — 新增，全局异常处理
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/config/IdempotentWebConfig.java` — 新增，Web配置类（注册拦截器）
## Task 7: 编写单元测试与集成测试
- **目标**: 完成核心逻辑测试，覆盖所有场景
- **涉及文件**:
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/test/java/com/mdframe/forge/starter/idempotent/IdempotentTest.java` — 新增，单元测试
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/src/test/resources/application-test.yml` — 新增，测试配置
## Task 8: 编写使用文档与示例
- **目标**: 提供使用说明和示例代码
- **涉及文件**:
    - `forge/forge-framework/forge-starter-parent/forge-starter-idempotent/README.md` — 新增，使用文档
    - `forge-docs/backend/modules/idempotent.md` — 新增，官方文档
