# Spring Boot 自动配置
## 核心知识
- 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件注册自动配置类
- 使用 `@Configuration` + `@EnableConfigurationProperties` 定义配置类
- 使用 `@ConditionalOnProperty` 实现条件化自动装配
- 使用 `@ConditionalOnMissingBean` 支持用户自定义实现覆盖默认实现
