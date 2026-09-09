# 应用设计器信息架构重构 - Phase C/D 增量测试计划

> 变更：`lowcode-app-designer-ia`  
> 验证日期：2026-08-15  
> 范围：页面资源树、表单画布视图、流程绑定与节点分区策略

## 1. 验证目标

- 新资源树可直接定位表单页、列表页、自由编排页、数据、自动化、流程与设置。
- 旧 `designerSection` 深链稳定映射到新资源，非法 `designResource` 不产生空白页。
- 表单布局、页面分区、详情设置在同一画布内切换，共享 schema 和草稿状态。
- 流程工作台按绑定、节点、高级配置纵向排列，节点和办理人由 BPMN `userTasks` 解析。
- 分区策略通过中文下拉选择完成，不要求用户手填 `nodeKey` 或 `sectionId`。
- 未绑定流程时显示空态引导；旧节点或失效分区配置不静默丢失。

## 2. P0 必跑矩阵

| 层级 | 验证内容 | 通过标准 |
|---|---|---|
| 静态检查 | C/D 涉及的 Vue/JS/测试文件 ESLint | 0 error |
| 单元/组件 | 资源映射、流程节点合并、失效分区、分区编辑协议、表单 schema | 定向用例全通过 |
| 生产构建 | `forge-admin-ui` Vite build | 构建成功，警告无新增阻断项 |
| 可视化烟测 | 1024/1280/1440px 资源树、三画布视图、流程纵向布局 | 无页面级横向溢出、无重叠、无控制台阻断错误 |
| 交互烟测 | 分区草稿跨视图保持、事件红点、财务节点分区选择、未绑定流程空态 | 仅使用界面文案完成，无标识符手输 |

## 3. 可复跑命令

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint <Phase C/D 涉及的 Vue/JS/测试文件>
pnpm exec vitest run \
  src/views/app-center/__tests__/application-designer-navigation.spec.js \
  src/views/app-center/components/__tests__/application-flow-interaction.spec.js \
  src/views/app-center/components/designer/forge-form-designer/__tests__/PageSectionEditor.spec.js \
  src/views/app-center/components/designer/forge-form-designer/__tests__/pageSectionEditorUtils.spec.js \
  src/views/app-center/components/designer/form-first/__tests__/formDesignerSchema.spec.js
pnpm build
```

```bash
cd forge-admin-ui
pnpm dev --host 127.0.0.1 --port 3000

cd ..
env PYTHONDONTWRITEBYTECODE=1 /opt/anaconda3/bin/python \
  code-copilot/changes/lowcode-app-designer-ia/visual-smoke.py
```

## 4. P1 真实服务验收（本轮跳过）

- 保存流程交互草稿并从真实接口重新读取。
- 以财务身份打开 H5 待办，验证付款分区不可见。
- 对未配置 `flowInteraction` 和不带 `pageSections` 的历史应用执行真实回归。
- 执行 Spec 第六章的完整保存、预览、发布、H5 验收剧本。

跳过原因：本轮 `8580`/`8081` 真实后端与 Flow/H5 验收环境不可用；项目偏好要求真实服务联调由用户执行。烟测使用确定性 mock API，不发送保存或发布请求。

## 5. 顶部摘要中性化增量验证（2026-08-15）

- 范围：所有对象设计页复用的 `BusinessTableMappingSummary` 顶部数据库映射摘要。
- 静态检查：定向 ESLint 通过，0 error。
- 可视化检查：1024/1280/1440px 下 `.table-mapping` 计算背景色均为 `rgb(255, 255, 255)`，且原 Phase C/D 交互烟测继续通过。
- 生产构建：Node `v20.19.0` 下 `pnpm build` 通过。
- 跳过项：本次仅调整共享前端组件视觉，不涉及真实后端、Flow 服务或 H5 验收。

## 6. Phase E 回归修正增量验证（2026-08-15）

### 6.1 验证目标

- E5：设计器恢复应用级「动作增强」节点，旧增强引导直达真实扩展面板，内嵌态不重复显示页头。
- E1：对象表单页内嵌时隐藏单项对象导航和「单据闭环配置」，工作台恢复为单列。
- E2/E3：自由编排编辑态只保留资源树，并可从「页面」分组直接新建、选中新页面。
- E6：触发器、业务动作、审批与增强节点使用真实摘要计算配置状态，分组显示配置计数，零配置分组默认折叠。
- E4：MOBILE/H5 运行时入口可选择对象页面或应用自由编排页面，并生成可核对的预览路径。

### 6.2 P0/P1 自动验证矩阵

| 层级 | 验证内容 | 通过标准 |
|---|---|---|
| 静态检查 | Phase E 涉及 Vue/JS/测试文件 ESLint | 0 error |
| 单元/契约 | 导航映射、增强节点、真实配置计数、嵌入式布局、入口目标 | 9 个测试文件全部通过 |
| 生产构建 | `forge-admin-ui` Vite build | 构建成功，无新增阻断警告 |
| 视觉冒烟 | 1024/1280/1440px 表单页、增强页、自由页面、流程页 | 无横向溢出、无重复导航/标题、无重叠 |
| 交互冒烟 | 旧增强深链、新建页面、自由页面唯一侧栏 | 真实节点可见，新页面 URL 与选中态一致 |

### 6.3 可复跑命令

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint \
  src/views/app-center/application-designer-navigation.js \
  'src/views/app-center/application-runtime.[applicationCode].vue' \
  'src/views/app-center/application.[applicationCode].vue' \
  src/views/app-center/components/ApplicationDesignerResourceTree.vue \
  src/views/app-center/components/AppEntryWizard.vue \
  src/views/app-center/components/app-entry-targets.js \
  src/views/app-center/application-workspace/ApplicationEntriesPanel.vue \
  src/views/app-center/application-workspace/ApplicationExtensionsPanel.vue \
  src/views/app-center/components/designer/BusinessObjectDesignerShell.vue \
  src/views/app-center/__tests__/application-designer-navigation.spec.js \
  src/views/app-center/__tests__/application-designer-phase-e-contract.spec.js \
  src/views/app-center/components/__tests__/app-entry-targets.spec.js
```

```bash
pnpm exec vitest run \
  src/views/app-center/__tests__/application-designer-navigation.spec.js \
  src/views/app-center/__tests__/application-designer-phase-e-contract.spec.js \
  src/views/app-center/components/__tests__/app-entry-targets.spec.js \
  src/views/app-center/components/__tests__/application-flow-interaction.spec.js \
  src/views/app-center/components/designer/__tests__/object-designer-navigation.spec.js \
  src/views/app-center/components/designer/__tests__/business-action-designer-protocol.spec.js \
  src/views/app-center/components/designer/forge-form-designer/__tests__/PageSectionEditor.spec.js \
  src/views/app-center/components/designer/forge-form-designer/__tests__/pageSectionEditorUtils.spec.js \
  src/views/app-center/components/designer/form-first/__tests__/formDesignerSchema.spec.js
```

```bash
cd ..
env PYTHONDONTWRITEBYTECODE=1 \
  FORGE_VISUAL_SMOKE_BASE_URL=http://127.0.0.1:4179 \
  /opt/anaconda3/bin/python \
  code-copilot/changes/lowcode-app-designer-ia/visual-smoke.py
```

### 6.4 真实服务验收（本轮跳过）

- 在真实 `ai_business_extension` 数据上核对历史记录未丢失，并完成页面 JS 增强的创建、保存、校验、启用和版本查看。
- 新建 MOBILE 入口后通过真实 `forge-h5-ui` 验证 `targetPageKey` 落到所选列表页或自由编排页。
- 使用真实应用数据回归 5 个对象时的分组折叠和未配置节点展示。

跳过原因：本轮不启动或写入真实后端、数据库、Flow/H5 运行态；按项目偏好，这部分由用户在完整联调环境执行。自动验证使用确定性 mock GET 数据，不发送保存、发布或状态变更请求。
