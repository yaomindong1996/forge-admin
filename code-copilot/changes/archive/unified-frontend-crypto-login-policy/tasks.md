# 任务拆分 — 三端传输加密统一与登录密码策略解耦
> status: complete
> created: 2026-08-04

## Task 1: 统一后端登录密码策略

- [x] 在 `LoginConfig` 与 `LoginConfigResult` 增加 `enablePasswordEncryption`。
- [x] 新增服务端登录密码策略解析器，缺省值按启用处理。
- [x] 新增统一 RSA 密码解码器，开启时失败关闭、关闭时返回原始密码。
- [x] 两个密码认证策略复用统一解码器。
- [x] `/auth/loginConfig` 使用同一策略下发开关。

## Task 2: 调整 Admin 登录配置与登录请求

- [x] 配置中心登录页签增加独立开关和使用说明。
- [x] Admin 登录页按匿名登录配置决定是否 RSA 加密。
- [x] RSA 密码工具移除对通用传输加密总开关的依赖。

## Task 3: H5 读取统一运行配置

- [x] 补齐 H5 运行配置归一化和匿名加载函数。
- [x] App 启动阶段加载 `/crypto/config`。
- [x] 登录提交前读取 `userClient=h5` 的登录配置并按独立开关处理密码。
- [x] 移除公共 H5 客户端环境变量和登录报文中的固定 AppSecret。

## Task 4: 报表端读取统一运行配置

- [x] 补齐报表端运行配置归一化和匿名加载函数。
- [x] 应用挂载前加载 `/forge-report-api/crypto/config`。
- [x] 报表端通用传输加密支持运行配置中的 SM4/AES。
- [x] 登录前读取 `userClient=forge_report` 的登录配置，移除明文静默降级。
- [x] 移除报表公共客户端登录报文中的硬编码 AppSecret。

## Task 5: 静态交付检查

- [x] 检查目标文件差异与冲突标记。
- [x] 执行 `git diff --check`。
- [x] 回填未执行的构建、服务启动和真实登录验证项。
