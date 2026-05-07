# Forge项目用户偏好

## 1. 前端 Node 版本

**记录日期**: 2026-05-07

运行前端相关命令（如 `pnpm install`、`pnpm exec eslint`、`pnpm build`）前，先执行：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
```

避免使用系统默认 Node 12 导致 pnpm 版本不兼容。
