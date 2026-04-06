# 全局异常处理
## 核心知识
- 自定义业务异常继承项目的`BusinessException`，可以直接被全局异常处理器捕获
- 使用`@RestControllerAdvice` + `@ExceptionHandler`实现统一异常处理
- 异常处理中记录日志，返回统一的`RespInfo`格式
