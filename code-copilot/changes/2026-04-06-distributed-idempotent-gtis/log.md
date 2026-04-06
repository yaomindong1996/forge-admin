# 变更日志 — 分布式幂等防重组件（美团GTIS方案实现）

> 记录决策、踩坑和知识发现。知识飞轮的输入。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-04-06 | propose | 完成需求Spec与任务拆分 | 所有待澄清问题已确认 |
| 2026-04-06 | apply | 完成Task1：创建幂等组件模块结构与基础依赖 | - |
| 2026-04-06 | apply | 完成Task2：定义@Idempotent注解与配置类 | - |
| 2026-04-06 | apply | 完成Task3：实现幂等键生成器 | - |
| 2026-04-06 | apply | 完成Task4：实现Redis幂等存储服务 | - |
| 2026-04-06 | apply | 完成Task5：实现AOP切面与Web拦截器 | - |
| 2026-04-06 | apply | 完成Task6：实现全局开关与异常处理 | - |

## 知识发现
> 每个 task 后实时记录，/archive 时逐条确认沉淀到 knowledge/
- [x] **Spring Boot自动配置**: 通过META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports文件注册自动配置类
- [x] **Maven多模块结构**: 新增starter模块需要在父pom.xml的modules标签中添加模块声明
- [x] **SpEL表达式**: 使用Spring Expression Language动态解析幂等键
- [x] **AOP方法参数获取**: 使用LocalVariableTableParameterNameDiscoverer获取方法参数名
- [ ] **全局异常处理**: 自定义业务异常继承项目的BusinessException，直接被GlobalExceptionHandler捕获处理

## 知识发现
> 每个 task 后实时记录，/archive 时逐条确认沉淀到 knowledge/
- [x] **Spring Boot自动配置**: 通过META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports文件注册自动配置类
- [x] **Maven多模块结构**: 新增starter模块需要在父pom.xml的modules标签中添加模块声明
- [ ] **SpEL表达式**: 使用Spring Expression Language动态解析幂等键
- [ ] **AOP方法参数获取**: 使用LocalVariableTableParameterNameDiscoverer获取方法参数名

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|-----------|------|
| 存储介质 | 仅Redis | 数据库/本地缓存 | 性能最优，满足绝大多数场景 |
| 默认过期时间 | 10分钟 | 5分钟/30分钟/1小时 | 覆盖大多数业务场景 |
| 全局开关 | 支持 | 不支持 | 方便全局管控 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|----------|--------|

## 知识发现
> 每个 task 后实时记录，/archive 时逐条确认沉淀到 knowledge/
- [ ] **Spring Boot自动配置**: 通过META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports文件注册自动配置类
- [ ] **Maven多模块结构**: 新增starter模块需要在父pom.xml的modules标签中添加模块声明

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|----------|----------|--------|

## 代码质量备忘
