<template>
  <!-- 访问与行级权限沿用真实表单协议 -->
  <section class="dataset-edit-panel dataset-edit-panel--access" data-step-section="4">
    <div class="panel-section-head">
      <h3>权限控制</h3>
    </div>

    <div class="access-control-block">
      <div class="access-mode-row">
        <span>访问范围</span>
        <n-radio-group
          :value="formData.accessMode"
          :disabled="isFormReadOnly"
          @update:value="value => handleAccessModeChange(value, formData, updateValue)"
        >
          <n-radio v-for="option in accessModeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </n-radio>
        </n-radio-group>
      </div>

      <div class="row-permission-strip">
        <div class="row-permission-title">
          <span>行级权限规则</span>
          <n-checkbox
            :checked="isRowScopeEnabled(formData)"
            :disabled="isFormReadOnly"
            @update:checked="checked => handleRowScopeEnabledChange(checked, formData, updateValue)"
          >
            根据用户属性设置权限
          </n-checkbox>
        </div>
        <div class="row-scope-expression">
          {{ getRowScopeConditionPreview(formData) }}
        </div>
        <div class="row-permission-rules">
          <span
            v-for="rule in getRowScopeRules(formData).filter(item => item.attribute && item.field)"
            :key="rule.__key"
            class="rule-chip"
          >
            {{ getRowScopeRuleLabel(rule) }}
          </span>
          <span v-if="getRowScopeConfiguredCount(formData) === 0" class="rule-chip rule-chip--empty">
            暂无规则
          </span>
          <button
            class="dataset-text-action"
            type="button"
            :disabled="isFormReadOnly"
            @click="addRowScopeRule(formData, updateValue)"
          >
            + 添加规则
          </button>
        </div>
        <div v-if="getRowScopeRules(formData).length" class="row-scope-rule-mini-list">
          <div
            v-for="(rule, index) in getRowScopeRules(formData)"
            :key="rule.__key || index"
            class="row-scope-rule-mini"
          >
            <NSelect
              :value="rule.attribute"
              :options="getRowScopeAttributeOptions(formData, rule)"
              :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
              clearable
              placeholder="用户属性"
              @update:value="value => handleRowScopeRuleAttributeChange(formData, rule, value, updateValue)"
            />
            <span>=</span>
            <NSelect
              :value="rule.field"
              :options="getRowScopeFieldOptions(formData)"
              :loading="rowScopeTableFieldLoading"
              :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
              clearable
              filterable
              placeholder="数据表字段"
              @update:value="value => handleRowScopeRuleFieldChange(formData, rule, value, updateValue)"
            />
            <n-button
              quaternary
              :disabled="isFormReadOnly || !isRowScopeEnabled(formData)"
              @click="removeRowScopeRule(formData, index, updateValue)"
            >
              <template #icon>
                <i class="i-material-symbols:delete-outline-rounded" />
              </template>
            </n-button>
          </div>
        </div>
      </div>

      <div v-if="formData.accessMode === 'PRIVATE'" class="acl-editor acl-editor--compact">
        <div class="acl-editor__toolbar">
          <div>
            <div class="acl-editor__title">
              授权对象
            </div>
            <div class="acl-editor__hint">
              已配置 {{ getAclCount(formData) }} 个授权主体。
            </div>
          </div>
          <n-button
            secondary
            type="primary"
            :disabled="isFormReadOnly || permissionOptionsLoading"
            :loading="permissionOptionsLoading"
            @click="addAclItem(formData, updateValue)"
          >
            <template #icon>
              <i class="i-material-symbols:add-rounded" />
            </template>
            选择用户/用户组
          </n-button>
        </div>

        <n-empty
          v-if="!formData.aclItems || formData.aclItems.length === 0"
          description="暂无授权主体"
          size="small"
        />
        <div v-else class="acl-tag-list">
          <span
            v-for="(item, index) in formData.aclItems"
            :key="item.__key || `${item.subjectType || 'ACL'}-${index}`"
            class="acl-tag"
          >
            {{ getAclItemLabel(item) }}
            <button
              type="button"
              :disabled="isFormReadOnly"
              @click="removeAclItem(formData, index, updateValue)"
            >
              ×
            </button>
          </span>
        </div>
        <div v-if="formData.aclItems && formData.aclItems.length > 0" class="acl-rows">
          <div
            v-for="(item, index) in formData.aclItems"
            :key="item.__key || `${item.subjectType || 'ACL'}-${index}`"
            class="acl-row"
          >
            <NSelect
              :value="item.subjectType"
              :options="aclSubjectTypeOptions"
              :disabled="isFormReadOnly"
              @update:value="value => handleAclSubjectTypeChange(item, value, updateValue)"
            />
            <n-tree-select
              v-if="item.subjectType === 'ORG'"
              :value="item.subjectId"
              :options="getAclOrgOptions(item)"
              :disabled="isFormReadOnly || permissionOptionsLoading"
              :loading="permissionOptionsLoading"
              :virtual-scroll="false"
              clearable
              filterable
              placeholder="选择组织"
              @update:value="value => handleAclSubjectIdChange(item, value, updateValue)"
            />
            <NSelect
              v-else
              :value="item.subjectId"
              :options="getAclSubjectOptions(item)"
              :disabled="isFormReadOnly || permissionOptionsLoading"
              :loading="permissionOptionsLoading"
              :virtual-scroll="false"
              clearable
              filterable
              placeholder="选择授权主体"
              @update:value="value => handleAclSubjectIdChange(item, value, updateValue)"
            />
            <NSelect
              :value="item.accessLevel"
              :options="accessLevelOptions"
              :disabled="isFormReadOnly"
              placeholder="权限级别"
              @update:value="value => handleAclAccessLevelChange(item, value, updateValue)"
            />
            <n-button
              quaternary
              type="error"
              :disabled="isFormReadOnly"
              @click="removeAclItem(formData, index, updateValue)"
            >
              <template #icon>
                <i class="i-material-symbols:delete-outline-rounded" />
              </template>
            </n-button>
          </div>
        </div>
      </div>
    </div>
  </section>

  <section class="dataset-edit-panel dataset-edit-panel--info">
    <div class="panel-section-head">
      <h3>数据集信息</h3>
    </div>
    <dl class="dataset-info-grid">
      <div>
        <dt>数据集ID</dt>
        <dd>{{ formData.id || '保存后生成' }}</dd>
      </div>
      <div>
        <dt>数据集类型</dt>
        <dd>{{ getDatasetTypeLabel(formData.datasetType) }}</dd>
      </div>
      <div>
        <dt>数据源</dt>
        <dd>{{ formData.connectionId ? getConnectionName(formData.connectionId) : '-' }}</dd>
      </div>
      <div>
        <dt>创建人</dt>
        <dd>{{ getDatasetCreatorLabel(formData) }}</dd>
      </div>
      <div>
        <dt>创建时间</dt>
        <dd>{{ formatDatasetDate(formData.createTime) }}</dd>
      </div>
      <div>
        <dt>更新时间</dt>
        <dd>{{ formatDatasetDate(formData.updateTime) }}</dd>
      </div>
      <div>
        <dt>更新人</dt>
        <dd>{{ getDatasetUpdaterLabel(formData) }}</dd>
      </div>
      <div>
        <dt>版本号</dt>
        <dd>
          {{ getDatasetVersionLabel(formData) }}
          <span class="dataset-current-version">当前版本</span>
        </dd>
      </div>
    </dl>
  </section>
</template>

<script>
import { useDatasetWorkspaceContext } from '@/stores/data/datasetWorkspaceStore'
import { datasetLocalComponents } from '../datasetLocalComponents'

export default {
  components: { ...datasetLocalComponents },
  props: { formData: { type: Object, required: true }, updateValue: { type: Function, required: true } },
  setup() { return useDatasetWorkspaceContext() },
}
</script>

<style scoped src="../dataset-editor.css"></style>
