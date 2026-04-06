# Maven 多模块项目结构
## 核心知识
- 新增starter模块需要在父`pom.xml`的`modules`标签中添加模块声明
- 父模块定义`dependencyManagement`统一管理依赖版本
- 子模块继承父模块的依赖版本，无需重复声明
