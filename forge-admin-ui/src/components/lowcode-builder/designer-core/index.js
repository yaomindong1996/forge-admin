/**
 * @fileoverview designer-core 统一导出入口
 * @description 聚合注册表、类型、数据源规范、拖拽协议、容器规范、树操作和桥接层。
 *   首次 import 时自动注册全部 105 个统一组件 spec（transfer 双重身份合并）。
 */

import { businessComponentSpecs } from './spec/business-components'
// ─── 注册表 API ───────────────────────────────────────────
// ─── 组件 spec 定义（按分类导入并注册）─────────────────────
import { fieldComponentSpecs } from './spec/field-components'
import { layoutComponentSpecs } from './spec/layout-components'
import { mediaComponentSpecs } from './spec/media-components'
import { pageComponentSpecs } from './spec/page-components'
import { registerComponents } from './spec/registry'
import { widgetComponentSpecs } from './spec/widget-components'
import { zoneActionComponentSpecs } from './spec/zone-action-components'

// ─── 画布共用组件 ────────────────────────────────────────
export { default as DesignerGridRenderer } from './canvas/DesignerGridRenderer.vue'
export { default as DesignerNodeOverlay } from './canvas/DesignerNodeOverlay.vue'
export { useNodeOverlay } from './canvas/useNodeOverlay'

// ─── 拖拽协议 ────────────────────────────────────────────
export {
  createDragPayload,
  DESIGNER_MIME,
  parseDragPayload,
} from './dnd/protocols'

// ─── 属性面板共用组件 ────────────────────────────────────
export { default as PropertyGroupCollapse } from './panel/PropertyGroupCollapse.vue'

// ─── 桥接层（按需导入）─────────────────────────────────────
export {
  FORM_COMPONENT_KEY_OVERRIDES,
  isPaletteUnionSpec,
  LIST_BLOCK_TYPE_OVERRIDES,
  LIST_CANVAS_PENDING_TYPES,
  PALETTE_ZONE_ONLY_TYPES,
  resolveBridgeSpec,
  toCanvasComponentCatalog,
  toFieldComponentDefaults,
  toFieldComponentDefaultsMap,
  toFieldPaletteGroups,
  toListPageBlockCatalog,
  toPageWidgetCatalog,
  toPageWidgetComponentKeys,
  toPageZoneCatalog,
} from './spec/bridge'

// ─── 容器插槽规范 ────────────────────────────────────────
export {
  canAcceptChild,
  detectTabsMode,
  getContainerSlot,
  getMaxDepth,
  isContainer,
  listContainerTypes,
  resolveContainerConfig,
  resolveNodeChildSlots,
} from './spec/container-slot-spec'

/** 全部 105 个统一组件 spec（transfer 同时是 field + widget，后者覆盖前者） */
export const allComponentSpecs = [
  ...fieldComponentSpecs,
  ...layoutComponentSpecs,
  ...businessComponentSpecs,
  ...pageComponentSpecs,
  ...mediaComponentSpecs,
  ...widgetComponentSpecs,
  ...zoneActionComponentSpecs,
]

// 首次 import 时自动注册
registerComponents(allComponentSpecs)

// ─── 数据源规范 ───────────────────────────────────────────
export {
  DATA_SOURCE_KINDS,
  isDataSourceKind,
  resolveDataSourceKinds,
} from './spec/data-source-spec'

// ─── 节点工厂 ───────────────────────────────────────────
export {
  createCol,
  createCollapseItem,
  createContainerNode,
  createGridCells,
  createInitialChildren,
  createListpageTab,
  createNode,
  createTableCell,
  createTabPane,
  generateNodeId,
  resetNodeCounter,
} from './spec/node-factory'

export {
  clearRegistry,
  getAliasMap,
  getComponentSpec,
  getRegistryStats,
  groupComponents,
  hasComponent,
  listAllComponents,
  listAllTypes,
  listComponents,
  listComponentsByCategory,
  registerComponent,
  registerComponents,
  resolveComponentType,
} from './spec/registry'

// ─── Schema 树操作 ───────────────────────────────────────
export {
  canInsertAtDepth,
  collectAllNodeIds,
  collectAllNodes,
  findNodeById,
  findNodePath,
  findParentNode,
  getMaxTreeDepth,
  getNodeDepth,
  insertNode,
  isAncestorOf,
  mapSiblingsInTree,
  mapTree,
  moveNode,
  removeNode,
  replaceNode,
  walkTree,
} from './spec/tree-ops'
