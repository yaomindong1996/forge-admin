<template>
  <div v-show="propertyPanelTab === 'props'" class="list-tree-model-property">
    <div class="property-search-anchor" data-property-search="树形模型 树表 父级 parentId 添加下级 层级" />
    <n-collapse
      class="list-tree-model-collapse"
      :default-expanded-names="embeddedTreeEnabled ? ['tree-model'] : []"
    >
      <n-collapse-item
        name="tree-model"
        :title="layoutType === 'tree-crud' ? '本表树形字段' : '树形模型'"
      >
        <n-alert
          v-if="layoutType === 'tree-crud'"
          type="info"
          :bordered="false"
          class="list-tree-model-alert"
        >
          左侧筛选树请在画布选中「筛选树」配置。这里只配本表是否按父子层级展示。
        </n-alert>
        <n-form
          class="list-tree-model-form"
          size="small"
          label-placement="top"
          :show-feedback="false"
        >
          <div class="tree-switch-row">
            <div class="tree-switch-copy">
              <strong>{{ layoutType === 'tree-crud' ? '本表树形列表' : '启用树形列表' }}</strong>
              <span>{{ embeddedTreeEnabled ? '列表按父子层级展示' : '当前为普通平铺列表' }}</span>
            </div>
            <n-switch
              size="small"
              :value="embeddedTreeEnabled"
              @update:value="updateEmbeddedTreeEnabled"
            />
          </div>

          <div class="list-tree-model-fields" :class="{ 'is-disabled': !embeddedTreeEnabled }">
            <n-form-item label="主键字段">
              <n-select
                :value="embeddedTreeConfig.keyField || 'id'"
                :options="primaryFieldOptions"
                filterable
                :disabled="!embeddedTreeEnabled"
                @update:value="value => patchEmbeddedTreeConfig({ keyField: value, targetField: value })"
              />
            </n-form-item>
            <n-form-item label="父级字段">
              <n-select
                :value="embeddedTreeConfig.parentField || 'parentId'"
                :options="primaryFieldOptions"
                filterable
                :disabled="!embeddedTreeEnabled"
                @update:value="value => patchEmbeddedTreeConfig({ parentField: value, filterField: value })"
              />
            </n-form-item>
            <n-form-item label="显示字段">
              <n-select
                :value="embeddedTreeConfig.labelField"
                :options="primaryFieldOptions"
                filterable
                clearable
                :disabled="!embeddedTreeEnabled"
                @update:value="value => patchEmbeddedTreeConfig({ labelField: value })"
              />
            </n-form-item>
            <n-form-item label="加载方式">
              <n-select
                :value="embeddedTreeConfig.loadMode || 'full'"
                :options="treeLoadModeOptions"
                :disabled="!embeddedTreeEnabled"
                @update:value="value => patchEmbeddedTreeConfig({ loadMode: value })"
              />
            </n-form-item>
            <n-form-item label="子级字段名">
              <n-input
                :value="embeddedTreeConfig.childrenField || 'children'"
                :disabled="!embeddedTreeEnabled"
                @update:value="value => patchEmbeddedTreeConfig({ childrenField: value || 'children' })"
              />
            </n-form-item>
          </div>

          <div class="tree-switch-row">
            <div class="tree-switch-copy">
              <strong>添加下级</strong>
              <span>{{ layoutType === 'tree-crud' ? '在右侧行新增子节点' : '在树表行新增子节点' }}</span>
            </div>
            <n-switch
              size="small"
              :disabled="!embeddedTreeEnabled && layoutType !== 'tree-crud'"
              :value="treeAddChildEnabled"
              @update:value="updateTreeAddChildEnabled"
            />
          </div>
        </n-form>
      </n-collapse-item>
    </n-collapse>
  </div>
</template>

<script>
import { useListPageDesignerApi } from './listPageDesignerContext'
import { listPageDesignerLocalComponents } from './listPageDesignerLocalComponents'

export default {
  components: { ...listPageDesignerLocalComponents },
  name: 'ListPagePropertyTreeModel',
  setup() {
    return useListPageDesignerApi()
  },
}
</script>

<style scoped src="./list-page-grid-designer-shell.css"></style>
<style scoped src="./list-page-grid-designer-panels.css"></style>
