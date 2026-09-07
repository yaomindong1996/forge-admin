# 界面验证

这里挂载真实 DataScopeConfig、AiCrudPage、Naive UI、字典加载与请求拦截器，仅由 Playwright 拦截业务接口并提供合成配置。截图不代表真实数据库验收结果。

首次复跑，从仓库根目录创建不入库的依赖链接：

```bash
ln -s "$PWD/forge-admin-ui/node_modules" code-copilot/changes/data-scope-usability/verification/node_modules
```

已有链接时跳过上一步。在 forge-admin-ui 下执行：

```bash
source /Users/yaomindong/.nvm/nvm.sh && nvm use v20.19.0
python3 /Users/yaomindong/.agents/skills/webapp-testing/scripts/with_server.py \
  --server 'node node_modules/vite/bin/vite.js --config ../code-copilot/changes/data-scope-usability/verification/vite.config.js' \
  --port 5187 --timeout 60 -- \
  python3 ../code-copilot/changes/data-scope-usability/verification/check_ui.py
```

检查列表启停、取消、刷新列表/权限分离、刷新失败提示、编辑保留技术字段、保存后清除失败状态、亮暗色及 390px 布局。辅助程序退出时自动停止本轮启动的 Vite。
