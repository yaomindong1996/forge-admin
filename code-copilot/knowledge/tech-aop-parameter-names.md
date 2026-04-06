# AOP 方法参数名获取
## 核心知识
- 使用`LocalVariableTableParameterNameDiscoverer`获取方法参数名，但需要编译时添加`-parameters`参数
- Spring的`DefaultParameterNameDiscoverer`会尝试多种方式获取参数名，更加健壮
- 如果无法获取参数名，SpEL表达式解析会失效
