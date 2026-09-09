# 界面验证

这里挂载真实 `ExcelExportConfig`、`ExcelColumnConfig`、`AiCrudPage` 和 Naive UI，仅由 Playwright 拦截业务接口并提供合成方案。截图不代表真实数据库或导出引擎验收结果。

首次复跑，从仓库根目录创建不入库的依赖链接：

```bash
ln -s "$PWD/forge-admin-ui/node_modules" code-copilot/changes/excel-export-usability/verification/node_modules
```

在 `forge-admin-ui` 下执行：

```bash
source /Users/yaomindong/.nvm/nvm.sh && nvm use v20.19.0
python3 /Users/yaomindong/.agents/skills/webapp-testing/scripts/with_server.py \
  --server 'node node_modules/vite/bin/vite.js --config ../code-copilot/changes/excel-export-usability/verification/vite.config.js' \
  --port 5188 --timeout 60 -- \
  python3 ../code-copilot/changes/excel-export-usability/verification/check_ui.py
```

检查业务化列表、状态确认、编辑抽屉高级设置折叠、文件列排序与未保存提醒、复制说明、亮暗色和 390px 窄屏布局。
