#!/usr/bin/env python3
# r4b: ListPageGridDesigner 主组件改造 —— 字段抽屉拆分至 FieldConfigDrawer.vue
# 所有行号基于改造前文件（13636 行），从后往前执行，每处带边界断言。
import sys

path = 'src/components/lowcode-builder/page/ListPageGridDesigner.vue'
with open(path) as f:
    lines = f.readlines()

def check(ln, expect):
    actual = lines[ln - 1]
    assert expect in actual, f'L{ln} 边界不符: expect {expect!r} in {actual!r}'

def delete(a, b):
    del lines[a - 1:b]

def insert_after(ln, text):
    lines[ln:ln] = [t + '\n' for t in text.split('\n') if t != '']

# ---- 1. 删样式段: .field-setting-control .. .available-item:hover (13164-13277) ----
check(13164, '')
check(13165, '.field-setting-control {')
check(13277, '}')
check(13279, '.list-source-modal')
delete(13164, 13277)

# ---- 2. 删样式段: /* Field drawer */ .. .field-help (12693-12908) ----
check(12693, '')
check(12694, '/* Field drawer */')
check(12907, '}')
check(12908, '')
check(12909, '.preview-config-panel')
delete(12693, 12908)

# ---- 3. 删 resolveCrudFieldQuickValue/updateCrudFieldQuickSetting/buildCrudFieldListPatch (10006-10051) ----
check(10006, '')
check(10007, 'function resolveCrudFieldQuickValue')
check(10051, '}')
check(10052, '</script>')
delete(10006, 10051)

# ---- 4. 删 normalizeParamName (9862-9867) ----
check(9862, '')
check(9863, 'function normalizeParamName')
check(9867, '}')
delete(9862, 9867)

# ---- 5. 删 resolveFieldRoleEnabled..updateFieldSetting (8925-9033) ----
check(8925, '')
check(8926, 'function resolveFieldRoleEnabled')
check(9033, '}')
check(9035, 'function applySelectedTableGlobalAlign')
delete(8925, 9033)

# ---- 6. 删 updateSelectedFieldRefs (8915-8924) ----
check(8915, '')
check(8916, 'function updateSelectedFieldRefs')
check(8924, '}')
delete(8915, 8924)

# ---- 7. 删 selectDrawerField..handleSelectedReorder (8782-8824)，插入新 resolveBlockFieldCount ----
check(8782, '')
check(8783, 'function selectDrawerField')
check(8801, 'function resolveBlockFieldCount')
check(8823, '}')
check(8824, '')
check(8825, 'function handleCrudTableFieldReorder')
delete(8782, 8824)
NEW_COUNT = '''function resolveBlockFieldCount(block = selectedBlock.value, zoneKey = 'table') {
  if (block?.blockType === 'AiCrudPage' && zoneKey === 'table') {
    const refs = new Set(resolveSelectedFieldRefs(block, 'table', props.fields))
    return props.fields.filter(field => refs.has(field.field) && isCrudTableFieldVisible(field.field, block)).length
  }
  return resolveSelectedFieldRefs(block, zoneKey, props.fields).length
}
'''
insert_after(8781, NEW_COUNT)  # 原 8781 行后（即 openInlineFieldDrawer 的 } 之后）

# ---- 8. 替换 openFieldDrawer/openInlineFieldDrawer (8768-8781)，追加 patch 事件处理 ----
check(8768, '// Field config drawer')
check(8769, 'function openFieldDrawer')
check(8781, '}')
lines[8768 - 1:8781] = [
    '// Field config drawer（抽屉本体已拆分至 FieldConfigDrawer.vue：选中态走 listDesigner store，写入走 patch 事件回传）\n',
    "function openFieldDrawer(mode = 'table') {\n",
    "  fieldDrawerMode.value = mode === 'search' ? 'search' : 'table'\n",
    "  fieldDrawerInitialField.value = ''\n",
    '  fieldDrawerOpen.value = true\n',
    '}\n',
    '\n',
    "function openInlineFieldDrawer(fieldName = '', mode = 'table') {\n",
    "  fieldDrawerMode.value = mode === 'search' ? 'search' : 'table'\n",
    '  fieldDrawerInitialField.value = fieldName\n',
    '  fieldDrawerOpen.value = true\n',
    '}\n',
    '\n',
    'function handleFieldDrawerPatchProps({ blockId, patch } = {}) {\n',
    '  if (!blockId)\n',
    '    return\n',
    '  patchBlockProps(blockId, patch)\n',
    '}\n',
    '\n',
    'function handleFieldDrawerPatchBlock({ blockId, patch } = {}) {\n',
    '  if (!blockId)\n',
    '    return\n',
    '  patchBlock(blockId, patch)\n',
    '}\n',
]

# ---- 9. 删树函数族 (7500-7641) ----
check(7500, 'function findBlockInTree')
check(7640, '}')
check(7641, '')
check(7642, 'function patchBlock')
delete(7500, 7641)

# ---- 10. 删 activeDrawerField/activeDrawerFieldSetting/watch(selectedFieldRefs) (5772-5786) ----
check(5772, 'const activeDrawerField = computed')
check(5786, '})')
check(5787, '')
check(5788, 'watch(')
delete(5772, 5786)

# ---- 11. 删 fieldMap..availableFields (5733-5758) ----
check(5733, 'const fieldMap = computed')
check(5758, '})')
check(5759, 'const crudTablePanelFields')
delete(5733, 5758)

# ---- 12. 状态定义：activeDrawerFieldName -> fieldDrawerInitialField，删 fieldAdvancedOpen ----
check(5072, "const activeDrawerFieldName = ref('')")
check(5074, "const fieldAdvancedOpen = ref(false)")
lines[5072 - 1] = "const fieldDrawerInitialField = ref('')\n"
delete(5074, 5074)

# ---- 13. 删选项常量 6 个 ----
check(5014, 'const alignOptions = [')
check(5027, ']')
check(5028, 'const shadowOptions = [')
delete(5014, 5027)
check(4974, 'const queryTypeOptions = [')
check(5005, ']')
check(5006, 'const treeLoadModeOptions = [')
delete(4974, 5005)

# ---- 14. import 插入（L4564 page-schema import 结束后；L4548 CrudHookRulesEditor 后）----
check(4564, "} from './page-schema'")
BLOCKTREE_IMPORT = '''import {
  collectBlocksInTree,
  findBlockInTree,
  mapBlocksInTree,
  mapBlockSiblingsInTree,
  removeBlockFromTree,
} from './blockTree'
import { alignOptions, normalizeParamName, resolveSelectedFieldRefs } from './fieldDrawerConfig\''''
insert_after(4564, BLOCKTREE_IMPORT)
check(4548, "import CrudHookRulesEditor from './CrudHookRulesEditor.vue'")
insert_after(4548, "import FieldConfigDrawer from './FieldConfigDrawer.vue'")

# ---- 15. 模板替换：字段抽屉 drawer (3647-4016) -> FieldConfigDrawer 组件 ----
check(3647, '<n-drawer v-model:show="fieldDrawerOpen"')
check(4016, '</n-drawer>')
check(4017, '')
check(4018, '<!-- 组件属性抽屉')
lines[3647 - 1:4016] = [
    '    <FieldConfigDrawer\n',
    '      v-model:show="fieldDrawerOpen"\n',
    '      :mode="fieldDrawerMode"\n',
    '      :fields="props.fields"\n',
    '      :initial-field="fieldDrawerInitialField"\n',
    '      :block-meta-title="selectedBlockMeta?.title || \'\'"\n',
    '      :page-target-options="pageTargetOptions"\n',
    '      :form-target-options="formTargetOptions"\n',
    '      @patch-props="handleFieldDrawerPatchProps"\n',
    '      @patch-block="handleFieldDrawerPatchBlock"\n',
    '    />\n',
]

# ---- 16. 保留函数的 resolveSelectedFieldRefs 调用补 fields 参数 ----
content = ''.join(lines)
content = content.replace(
    "resolveSelectedFieldRefs(selectedBlock.value, 'table')",
    "resolveSelectedFieldRefs(selectedBlock.value, 'table', props.fields)")
content = content.replace(
    "resolveSelectedFieldRefs(block, 'table')",
    "resolveSelectedFieldRefs(block, 'table', props.fields)")
lines = content.splitlines(keepends=True)

with open(path, 'w') as f:
    f.write(''.join(lines))

print(f'OK, 总行数: {len(lines)}')
