# 知识索引
> 领域知识的轻量索引。每条用一句话说清核心逻辑。
> 格式：- **触发关键词**: 一句话核心逻辑 → `包名.类名.方法名`（可选）
## 业务知识
（随实践积累补充）
## 技术约定
- **Spring Boot自动配置**: 通过META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports文件注册自动配置类 → `tech-spring-boot-autoconfig.md`
- **Maven多模块**: 新增starter模块需要在父pom.xml的modules标签中添加模块声明 → `tech-maven-multimodule.md`
- **SpEL表达式**: 使用Spring Expression Language动态解析幂等键，需要异常处理 → `tech-spel-expression.md`
- **AOP参数名**: 使用DefaultParameterNameDiscoverer获取方法参数名，更加健壮 → `tech-aop-parameter-names.md`
- **全局异常处理**: 自定义业务异常继承BusinessException，直接被全局异常处理器捕获 → `tech-global-exception-handler.md`
## 踩坑记录
- **多余依赖**: 注入未使用的依赖会导致不必要的依赖要求
- **SpEL解析异常**: SpEL表达式解析失败会影响主业务，需要异常处理
- **参数名获取失效**: StandardReflectionParameterNameDiscoverer在无-parameters编译参数时会失效，改用DefaultParameterNameDiscoverer
- **切面全局开关**: 仅靠自动配置条件注解不够，建议在切面中再次检查开关状态
