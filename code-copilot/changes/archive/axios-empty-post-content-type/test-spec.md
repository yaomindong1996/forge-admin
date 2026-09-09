# 空 POST 不带 form-urlencoded - 测试计划

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-admin-ui
./node_modules/.bin/vitest run src/utils/http/__tests__/empty-body.spec.js
```

跳过：不启动 Admin 服务。
