# 空 POST 不带 form-urlencoded - 执行记录

## 2026-09-02

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-admin-ui
./node_modules/.bin/vitest run src/utils/http/__tests__/empty-body.spec.js
```

3 passed。未启动 Admin 服务。
