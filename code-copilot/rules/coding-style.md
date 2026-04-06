
---
alwaysApply: true
---
# 编码规范
## 1. 命名
- 类名：大驼峰，见名知意
- 方法名：小驼峰，动词开头
- 常量：全大写下划线分隔
- 抽象类以 Abstract 或 Base 开头
- 测试类以被测类名开头，Test 结尾
- 禁止拼音、中英混拼命名
## 2. 异常处理
- 业务异常使用自定义 BusinessException，携带错误码
- 系统异常向上抛出，由统一异常处理器兜底
- 禁止吞掉异常（空 catch）
- catch 中必须记录日志
## 3. 日志
- Controller 入口打 INFO，含请求关键参数
- 异常打 ERROR，含完整堆栈
- 禁止在日志中打印用户敏感信息
## 4. 其他
- 写接口必须考虑幂等
- 涉及并发场景必须说明同步策略
- 魔法值必须定义为常量
## 5. git提交规范
- 禁止 master 分支变更：编码前检查当前分支，master 上立即停止
- 自动 Commit：每个 task/fix 完成后自动 commit，保持一个 task 一个 commit
- Commit 必须可编译：commit 前执行编译检查
- 禁止自动 Push：push 由用户主动触发，保留审查机会
- Message 格式：[<变更名>] <中文简述>
