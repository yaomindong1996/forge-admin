# SpEL 表达式使用
## 核心知识
- 使用Spring Expression Language动态解析幂等键
- SpEL解析时需要处理异常，避免解析失败影响主业务
- 结合`ParameterNameDiscoverer`获取方法参数名，支持`#paramName`格式的表达式
