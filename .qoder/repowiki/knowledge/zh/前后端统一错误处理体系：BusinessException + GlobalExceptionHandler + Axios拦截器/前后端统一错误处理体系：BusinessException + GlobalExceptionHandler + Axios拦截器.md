---
kind: error_handling
name: 前后端统一错误处理体系：BusinessException + GlobalExceptionHandler + Axios拦截器
category: error_handling
scope:
    - '**'
source_files:
    - forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/exception/BusinessException.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/exception/ExceptionUtil.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/exception/GlobalExceptionHandler.java
    - forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/exception/SaTokenExceptionHandler.java
    - forge-admin-ui/src/utils/http/index.js
    - forge-admin-ui/src/utils/http/interceptors.js
    - forge-admin-ui/src/utils/http/helpers.js
    - forge-admin-ui/src/utils/http/error-dialog.js
---

## 4. 约束与规则

- 业务层应优先使用 `ExceptionUtil.throwIf*` / `throwBiz` 抛出 `BusinessException`，而不是随意抛 `RuntimeException`，以便全局处理器正确归类。
- 控制器不应 catch 后吞掉异常再返回 RespInfo；需要特殊处理的场景才局部 try-catch，其余交由全局处理器。
- 前端调用统一通过 `utils/http/index.js` 导出的 `request` 实例，不要绕过拦截器直接发起 axios 请求，否则会丢失鉴权头、traceId、加解密和错误提示。
- 前端对 401 的处理是强制性的：`isAuthErrorCode` 匹配 `-8/401/11007/11008` 时一律调用 `authStore.logout()` 并跳转登录页，除非处于 `/login` 或 `isLoggingOut` 状态（静默处理）。
- 任何可能暴露 SQL 细节的异常（包括嵌套 cause）都会被 `containsSensitiveDatabaseDetail` 识别并替换为“数据访问异常，请联系管理员”，这是不可绕过的安全规则。
- 文件下载、图片等非 JSON 响应不走 `RespInfo.code` 判断，直接以 Blob/ArrayBuffer 形式返回，避免误判为业务错误。
- 插件模块（如 `forge-plugin-job`）可通过自己的 `@RestControllerAdvice` 覆盖通用处理，但需保持返回 `RespInfo` 结构，保证前端一致消费。