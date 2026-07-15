<template>
  <div class="business-relation-designer">
    <div class="relation-designer-head">
      <div>
        <h3>关系与级联</h3>
        <p>优先在关系图拖线建立对象关系，再按需配置字段联动和高级行为。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary @click="loadRelations">
          刷新
        </n-button>
        <n-button size="small" secondary @click="addRelation">
          新增关系
        </n-button>
        <n-button size="small" type="primary" :loading="saving" @click="saveRelations">
          保存配置
        </n-button>
      </n-space>
    </div>

    <div class="relation-workbench">
      <aside class="relation-step-rail">
        <div class="rail-block">
          <button
            type="button"
            class="relation-choice er-choice"
            :class="{ active: activePanel === 'er' }"
            @click="selectErDiagram"
          >
            <strong>可视化关系图</strong>
            <span>{{ erDiagramSummary }}，拖动字段连接点创建关系</span>
          </button>
        </div>

        <div class="rail-divider" />

        <div class="rail-block">
          <div class="rail-title">
            <span>对象关系</span>
            <n-button size="tiny" secondary @click="addRelation">
              新增
            </n-button>
          </div>
          <div v-if="localRelations.length" class="relation-choice-list">
            <button
              v-for="relation in localRelations"
              :key="relation.clientKey"
              type="button"
              class="relation-choice"
              :class="{ active: activePanel === 'relation' && activeRelation?.clientKey === relation.clientKey }"
              @click="selectRelation(relation.clientKey)"
            >
              <strong>{{ relation.relationName || relationLabel(relation) }}</strong>
              <span>{{ relationSentence(relation) }}</span>
            </button>
          </div>
          <n-empty v-else size="small" description="暂无关系" />
        </div>

        <div class="rail-divider" />

        <div class="rail-block">
          <div class="rail-title">
            <span>字段联动</span>
            <n-button size="tiny" secondary @click="addLinkageRule">
              新增
            </n-button>
          </div>
          <div v-if="localLinkage.rules.length" class="relation-choice-list">
            <button
              v-for="rule in localLinkage.rules"
              :key="rule.ruleId"
              type="button"
              class="relation-choice linkage-choice"
              :class="{ active: activePanel === 'linkage' && activeLinkageRuleId === rule.ruleId }"
              @click="selectLinkageRule(rule.ruleId)"
            >
              <strong>{{ linkageRuleLabel(rule) }}</strong>
              <span>{{ linkageRuleSentence(rule) }}</span>
            </button>
          </div>
          <n-empty v-else size="small" description="暂无联动" />
        </div>
      </aside>

      <main class="relation-main-pane">
        <n-spin :show="loading">
          <template v-if="activePanel === 'relation'">
            <template v-if="activeRelation">
              <section id="relation-section-basic" class="relation-config-card">
                <header class="section-title-row">
                  <div>
                    <strong>关系属性</strong>
                    <span>{{ relationSentence(activeRelation) }}</span>
                  </div>
                  <n-space size="small">
                    <n-tag size="small" :type="relationMatchTagType(activeRelation)" :bordered="false">
                      {{ relationMatchSummary(activeRelation) }}
                    </n-tag>
                    <n-tag size="small" :type="activeRelation.status === 0 ? 'default' : 'success'" :bordered="false">
                      {{ activeRelation.status === 0 ? '停用' : '启用' }}
                    </n-tag>
                    <n-popconfirm @positive-click="removeActiveRelation">
                      <template #trigger>
                        <n-button quaternary circle size="small">
                          <template #icon>
                            <n-icon><TrashOutline /></n-icon>
                          </template>
                        </n-button>
                      </template>
                      确认移除该关系？
                    </n-popconfirm>
                  </n-space>
                </header>
                <div class="relation-identity-grid">
                  <n-form label-placement="top" :show-feedback="false" size="small">
                    <n-form-item label="关系名称">
                      <n-input v-model:value="activeRelation.relationName" placeholder="例如：采购单包含采购明细" @update:value="markDirty" />
                    </n-form-item>
                  </n-form>
                  <n-form label-placement="top" :show-feedback="false" size="small">
                    <n-form-item label="目标对象">
                      <n-select
                        v-model:value="activeRelation.targetObjectCode"
                        :options="targetObjectOptions"
                        filterable
                        placeholder="选择目标对象"
                        @update:value="value => updateTargetObject(activeRelation, value)"
                      />
                    </n-form-item>
                  </n-form>
                  <div class="relation-identity-state">
                    <span>关系类型</span>
                    <strong>一对多明细</strong>
                  </div>
                  <div class="relation-identity-state">
                    <span>启用关系</span>
                    <n-switch
                      :value="activeRelation.status !== 0"
                      @update:value="value => updateStatus(activeRelation, value)"
                    />
                  </div>
                </div>

                <div class="relation-match-box">
                  <div class="relation-match-box-head">
                    <div>
                      <strong>字段端点</strong>
                      <span>这两个字段决定主记录与明细记录如何匹配。</span>
                    </div>
                    <n-tag size="small" :type="relationMatchTagType(activeRelation)" :bordered="false">
                      {{ relationMatchSummary(activeRelation) }}
                    </n-tag>
                  </div>
                  <div class="relation-endpoint-flow">
                    <section class="relation-endpoint-card is-source">
                      <header>
                        <span>当前对象</span>
                        <strong>{{ objectName || objectCode }}</strong>
                      </header>
                      <n-form label-placement="top" :show-feedback="false" size="small">
                        <n-form-item :label="sourceFieldLabel(activeRelation)">
                          <n-select
                            v-model:value="activeRelation.sourceFieldCode"
                            :options="sourceFieldOptions"
                            clearable
                            filterable
                            :placeholder="sourceFieldPlaceholder(activeRelation)"
                            @update:value="markDirty"
                          />
                        </n-form-item>
                      </n-form>
                    </section>
                    <div class="endpoint-connector" aria-hidden="true">
                      <span>1</span>
                      <i />
                      <span>N</span>
                    </div>
                    <section class="relation-endpoint-card is-target">
                      <header>
                        <span>目标对象</span>
                        <strong>{{ objectNameMap.get(activeRelation.targetObjectCode) || activeRelation.targetObjectCode || '未选择' }}</strong>
                      </header>
                      <n-form label-placement="top" :show-feedback="false" size="small">
                        <n-form-item :label="targetFieldLabel(activeRelation)">
                          <n-select
                            v-model:value="activeRelation.targetFieldCode"
                            :options="targetFieldOptions(activeRelation)"
                            :loading="targetFieldLoadingMap[activeRelation.targetObjectCode]"
                            clearable
                            filterable
                            :placeholder="targetFieldPlaceholder(activeRelation)"
                            @update:value="markDirty"
                          />
                        </n-form-item>
                      </n-form>
                    </section>
                  </div>
                  <n-form label-placement="top" :show-feedback="false" size="small" class="relation-display-field">
                    <n-form-item :label="displayFieldLabel(activeRelation)">
                      <n-select
                        v-model:value="activeRelation.displayField"
                        :options="targetDisplayFieldOptions(activeRelation)"
                        :loading="targetFieldLoadingMap[activeRelation.targetObjectCode]"
                        clearable
                        filterable
                        placeholder="选择运行态回显字段"
                        @update:value="markDirty"
                      />
                    </n-form-item>
                  </n-form>
                </div>
              </section>

              <n-collapse class="relation-advanced-collapse" arrow-placement="right">
                <n-collapse-item name="inline">
                  <template #header>
                    <span class="advanced-relation-title">
                      显示与内嵌
                      <n-tag size="small" :type="inlineConfigTagType(activeRelation)" :bordered="false">
                        {{ inlineConfigSummary(activeRelation) }}
                      </n-tag>
                    </span>
                  </template>
                  <p class="advanced-relation-hint">
                    控制这条关系是否出现在新增、编辑和详情区域。
                  </p>
                  <n-form label-placement="top" :show-feedback="false" size="small">
                    <n-grid :cols="4" :x-gap="16" :y-gap="6" responsive="screen">
                      <n-form-item-gi label="新增表单维护">
                        <n-switch
                          :value="activeRelation.inlineCreateEnabled === true"
                          :disabled="!canInlineEdit(activeRelation)"
                          @update:value="value => updateInlineCreate(activeRelation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="编辑表单维护">
                        <n-switch
                          :value="activeRelation.inlineEditEnabled === true"
                          :disabled="!canInlineEdit(activeRelation)"
                          @update:value="value => updateInlineEdit(activeRelation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="详情页展示">
                        <n-switch
                          :value="activeRelation.showInDetail !== false"
                          @update:value="value => updateShowInDetail(activeRelation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="子表保存模式">
                        <n-select
                          v-model:value="activeRelation.saveMode"
                          :options="childSaveModeOptions"
                          :disabled="!canInlineEdit(activeRelation)"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="详情页签">
                        <n-input v-model:value="activeRelation.detailTabTitle" placeholder="例如：采购明细" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="排序">
                        <n-input-number v-model:value="activeRelation.sortOrder" :min="0" style="width: 100%" @update:value="markDirty" />
                      </n-form-item-gi>
                    </n-grid>
                  </n-form>
                </n-collapse-item>

                <n-collapse-item name="selector">
                  <template #header>
                    <span class="advanced-relation-title">
                      子表选择器
                      <n-tag size="small" :type="selectorConfigTagType(activeRelation)" :bordered="false">
                        {{ selectorConfigSummary(activeRelation) }}
                      </n-tag>
                    </span>
                  </template>
                  <div class="selector-toggle-row">
                    <span>{{ activeRelation.selectorEnabled ? '已启用选择器按钮' : '开启后可从候选对象批量选择记录' }}</span>
                    <n-space size="small">
                      <n-button v-if="activeRelation.selectorEnabled" size="tiny" secondary @click="applySelectorDefaults(activeRelation)">
                        智能补齐
                      </n-button>
                      <n-switch
                        :value="activeRelation.selectorEnabled === true"
                        :disabled="!canInlineEdit(activeRelation)"
                        @update:value="value => updateRelationSelectorEnabled(activeRelation, value)"
                      />
                    </n-space>
                  </div>
                  <n-form v-if="activeRelation.selectorEnabled" label-placement="top" :show-feedback="false" size="small">
                    <n-grid :cols="3" :x-gap="16" :y-gap="6" responsive="screen">
                      <n-form-item-gi label="候选对象">
                        <n-select
                          v-model:value="activeRelation.selectorObjectCode"
                          :options="targetObjectOptions"
                          clearable
                          filterable
                          placeholder="选择弹窗里查询的对象"
                          @update:value="value => updateSelectorObject(activeRelation, value)"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="按钮文案">
                        <n-input v-model:value="activeRelation.selectorButtonText" placeholder="选择记录" @update:value="markDirty" />
                      </n-form-item-gi>
                      <n-form-item-gi label="选择器标题">
                        <n-input v-model:value="activeRelation.selectorTitle" placeholder="可为空" @update:value="markDirty" />
                      </n-form-item-gi>
                    </n-grid>
                  </n-form>
                  <n-empty v-else size="small" description="开启后可配置候选对象、弹窗字段和回填映射" />
                </n-collapse-item>

                <n-collapse-item name="mapping">
                  <template #header>
                    <span class="advanced-relation-title">
                      字段映射
                      <n-tag size="small" :type="mappingConfigTagType(activeRelation)" :bordered="false">
                        {{ mappingConfigSummary(activeRelation) }}
                      </n-tag>
                    </span>
                  </template>
                  <div class="selector-toggle-row">
                    <span>选择器弹窗展示、搜索和选中后的写入规则。</span>
                    <n-button v-if="activeRelation.selectorEnabled" size="tiny" secondary @click="addSelectorMapping(activeRelation)">
                      <template #icon>
                        <n-icon><AddOutline /></n-icon>
                      </template>
                      添加映射
                    </n-button>
                  </div>
                  <template v-if="activeRelation.selectorEnabled">
                    <n-form label-placement="top" :show-feedback="false" size="small">
                      <n-grid :cols="2" :x-gap="16" :y-gap="6" responsive="screen">
                        <n-form-item-gi label="弹窗展示字段">
                          <n-select
                            v-model:value="activeRelation.selectorDisplayFields"
                            :options="selectorCandidateFieldOptions(activeRelation)"
                            :loading="targetFieldLoadingMap[selectorCandidateObjectCode(activeRelation)]"
                            multiple
                            clearable
                            filterable
                            placeholder="选择用户在弹窗里看到的字段"
                            @update:value="markDirty"
                          />
                        </n-form-item-gi>
                        <n-form-item-gi label="关键词搜索字段">
                          <n-select
                            v-model:value="activeRelation.selectorKeywordFields"
                            :options="selectorCandidateFieldOptions(activeRelation)"
                            :loading="targetFieldLoadingMap[selectorCandidateObjectCode(activeRelation)]"
                            multiple
                            clearable
                            filterable
                            placeholder="选择编号、名称等可搜索字段"
                            @update:value="markDirty"
                          />
                        </n-form-item-gi>
                      </n-grid>
                    </n-form>
                    <div v-if="activeRelation.selectorMappings.length" class="selector-mapping-list">
                      <div class="selector-row-head">
                        <span>候选对象字段</span>
                        <span />
                        <span>子表字段</span>
                        <span />
                      </div>
                      <div v-for="(mapping, mappingIndex) in activeRelation.selectorMappings" :key="mapping.clientKey" class="selector-mapping-row">
                        <n-select
                          v-model:value="mapping.sourceField"
                          :options="selectorCandidateFieldOptions(activeRelation, mapping.sourceField)"
                          :loading="targetFieldLoadingMap[selectorCandidateObjectCode(activeRelation)]"
                          clearable
                          filterable
                          placeholder="候选字段"
                          @update:value="markDirty"
                        />
                        <span class="selector-mapping-arrow">写入</span>
                        <n-select
                          v-model:value="mapping.targetField"
                          :options="selectorTargetFieldOptions(activeRelation, mapping.targetField)"
                          :loading="targetFieldLoadingMap[activeRelation.targetObjectCode]"
                          clearable
                          filterable
                          placeholder="子表字段"
                          @update:value="markDirty"
                        />
                        <n-button quaternary circle size="small" @click="removeSelectorMapping(activeRelation, mappingIndex)">
                          <template #icon>
                            <n-icon><TrashOutline /></n-icon>
                          </template>
                        </n-button>
                      </div>
                    </div>
                    <n-empty v-else size="small" description="还没有字段映射，点击添加映射" />

                    <div class="selector-filter-block">
                      <div class="selector-filter-title">
                        <strong>筛选候选记录</strong>
                        <n-button size="tiny" secondary @click="addSelectorSearchParam(activeRelation)">
                          <template #icon>
                            <n-icon><AddOutline /></n-icon>
                          </template>
                          添加筛选
                        </n-button>
                      </div>
                      <div v-if="activeRelation.selectorSearchParams.length" class="selector-filter-list">
                        <div v-for="(param, paramIndex) in activeRelation.selectorSearchParams" :key="param.clientKey" class="selector-filter-row">
                          <n-select
                            v-model:value="param.paramKey"
                            :options="selectorCandidateFieldOptions(activeRelation, param.paramKey)"
                            :loading="targetFieldLoadingMap[selectorCandidateObjectCode(activeRelation)]"
                            clearable
                            filterable
                            placeholder="候选对象筛选字段"
                            @update:value="markDirty"
                          />
                          <n-select
                            v-model:value="param.sourceType"
                            :options="selectorFilterSourceOptions"
                            @update:value="value => updateSelectorSearchParamSource(param, value)"
                          />
                          <n-select
                            v-if="param.sourceType !== 'static'"
                            v-model:value="param.sourceField"
                            :options="selectorContextFieldOptions(param)"
                            clearable
                            filterable
                            placeholder="选择来源字段"
                            @update:value="markDirty"
                          />
                          <n-input
                            v-else
                            v-model:value="param.staticValue"
                            clearable
                            placeholder="固定值"
                            @update:value="markDirty"
                          />
                          <n-button quaternary circle size="small" @click="removeSelectorSearchParam(activeRelation, paramIndex)">
                            <template #icon>
                              <n-icon><TrashOutline /></n-icon>
                            </template>
                          </n-button>
                        </div>
                      </div>
                      <n-empty v-else size="small" description="未设置筛选条件，弹窗展示全部候选记录" />
                    </div>
                  </template>
                  <n-empty v-else size="small" description="开启子表选择器后可维护字段映射" />
                </n-collapse-item>

                <n-collapse-item v-if="canInlineEdit(activeRelation)" name="approval">
                  <template #header>
                    <span class="advanced-relation-title">
                      审批后处理
                      <n-tag size="small" :type="approvalConfigTagType(activeRelation)" :bordered="false">
                        {{ approvalConfigSummary(activeRelation) }}
                      </n-tag>
                    </span>
                  </template>
                  <div class="selector-toggle-row">
                    <span>{{ activeRelation.approvalQuantityEnabled ? '审批通过后按当前关系逐行同步数量' : '未启用审批后数量同步' }}</span>
                    <n-switch
                      :value="activeRelation.approvalQuantityEnabled === true"
                      @update:value="value => updateRelationApprovalQuantityEnabled(activeRelation, value)"
                    />
                  </div>
                  <n-form v-if="activeRelation.approvalQuantityEnabled" label-placement="top" :show-feedback="false" size="small">
                    <n-grid :cols="2" :x-gap="16" :y-gap="6" responsive="screen">
                      <n-form-item-gi label="处理方式">
                        <n-select
                          v-model:value="activeRelation.approvalQuantityOperation"
                          :options="approvalQuantityOperationOptions"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="归属字段（主表）">
                        <n-select
                          v-model:value="activeRelation.approvalQuantityAccountField"
                          :options="sourceFieldOptions"
                          clearable
                          filterable
                          placeholder="选择主表字段"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="对象字段（明细）">
                        <n-select
                          v-model:value="activeRelation.approvalQuantityItemField"
                          :options="targetFieldOptions(activeRelation)"
                          :loading="targetFieldLoadingMap[activeRelation.targetObjectCode]"
                          clearable
                          filterable
                          placeholder="选择明细字段"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="备用对象字段（明细）">
                        <n-select
                          v-model:value="activeRelation.approvalQuantityItemFallbackFields"
                          :options="targetFieldOptions(activeRelation)"
                          :loading="targetFieldLoadingMap[activeRelation.targetObjectCode]"
                          multiple
                          clearable
                          filterable
                          placeholder="主对象字段为空时按顺序尝试"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="数量字段（明细）">
                        <n-select
                          v-model:value="activeRelation.approvalQuantityField"
                          :options="targetFieldOptions(activeRelation)"
                          :loading="targetFieldLoadingMap[activeRelation.targetObjectCode]"
                          clearable
                          filterable
                          placeholder="选择明细里的数量字段"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                      <n-form-item-gi label="备注">
                        <n-input
                          v-model:value="activeRelation.approvalQuantityRemark"
                          placeholder="例如：审批通过后自动同步"
                          @update:value="markDirty"
                        />
                      </n-form-item-gi>
                    </n-grid>
                  </n-form>
                </n-collapse-item>
              </n-collapse>
            </template>
            <n-empty v-else-if="!loading" class="relation-empty-state" description="暂无关系配置">
              <template #extra>
                <n-button size="small" type="primary" @click="addRelation">
                  新增关系
                </n-button>
              </template>
            </n-empty>
          </template>

          <template v-else-if="activePanel === 'er'">
            <section id="relation-section-er" class="relation-er-panel">
              <div class="er-object-picker">
                <div>
                  <strong>画布对象</strong>
                  <span>选择要参与连线的目标对象，字段会按需加载。</span>
                </div>
                <n-select
                  :value="erCandidateObjectCodes"
                  :options="targetObjectOptions"
                  multiple
                  filterable
                  clearable
                  max-tag-count="responsive"
                  placeholder="选择目标对象加入画布"
                  @update:value="updateErCandidateObjects"
                />
              </div>
              <LowcodeErDiagram
                title="拖线配置对象关系"
                :subtitle="erDiagramSubtitle"
                :models="erDiagramModels"
                :primary-model-code="props.objectCode"
                :selected-relation-key="activeRelationKey"
                editable
                :download-file-name="`${props.objectCode || 'business-object'}-er.svg`"
                empty-text="暂无可绘制的业务对象关系"
                @connect="handleErConnect"
                @relation-select="selectRelation"
              />
            </section>
          </template>

          <template v-else>
            <section id="relation-section-linkage" class="relation-config-card">
              <header class="section-title-row">
                <div>
                  <strong>字段联动</strong>
                  <span>维护表单字段之间的过滤和清空规则。</span>
                </div>
                <n-button size="tiny" secondary @click="addLinkageRule">
                  <template #icon>
                    <n-icon><AddOutline /></n-icon>
                  </template>
                  新增联动
                </n-button>
              </header>
              <div v-if="localLinkage.rules.length" class="relation-card-list">
                <section v-for="(rule, index) in localLinkage.rules" :key="rule.ruleId" class="relation-card linkage-card">
                  <header class="relation-card-head">
                    <div>
                      <strong>{{ linkageRuleLabel(rule) }}</strong>
                      <p>{{ linkageRuleSentence(rule) }}</p>
                    </div>
                    <n-space size="small">
                      <n-tag size="small" :type="rule.enabled === false ? 'default' : 'success'" :bordered="false">
                        {{ rule.enabled === false ? '停用' : '启用' }}
                      </n-tag>
                      <n-popconfirm @positive-click="removeLinkageRule(index)">
                        <template #trigger>
                          <n-button quaternary circle size="small">
                            <template #icon>
                              <n-icon><TrashOutline /></n-icon>
                            </template>
                          </n-button>
                        </template>
                        确认移除该级联规则？
                      </n-popconfirm>
                    </n-space>
                  </header>

                  <n-form label-placement="top" :show-feedback="false" size="small" class="relation-form">
                    <div class="linkage-flow-editor">
                      <n-form-item label="控制字段">
                        <n-select
                          v-model:value="rule.sourceField"
                          :options="sourceFieldOptions"
                          clearable
                          filterable
                          placeholder="选择控制字段"
                          @update:value="value => updateLinkageSource(rule, value)"
                        />
                      </n-form-item>
                      <div class="linkage-flow-arrow" aria-hidden="true">
                        →
                      </div>
                      <n-form-item label="联动方式">
                        <n-select
                          v-model:value="rule.type"
                          :options="linkageTypeOptions"
                          @update:value="value => updateLinkageType(rule, value)"
                        />
                      </n-form-item>
                      <div class="linkage-flow-arrow" aria-hidden="true">
                        →
                      </div>
                      <n-form-item label="目标字段">
                        <n-select
                          v-model:value="rule.targetField"
                          :options="linkageTargetFieldOptions(rule)"
                          clearable
                          filterable
                          placeholder="选择被过滤字段"
                          @update:value="value => updateLinkageTarget(rule, value)"
                        />
                      </n-form-item>
                    </div>

                    <section class="linkage-detail-section">
                      <strong>数据来源</strong>
                      <n-grid :cols="3" :x-gap="12" :y-gap="4" responsive="screen">
                      <template v-if="rule.dataSourceType === 'dict'">
                        <n-form-item-gi label="上级字典类型">
                          <n-input v-model:value="rule.dictConfig.sourceDictType" placeholder="选择或填写字典类型" @update:value="markLinkageDirty" />
                        </n-form-item-gi>
                        <n-form-item-gi label="目标字典类型">
                          <n-input v-model:value="rule.dictConfig.targetDictType" placeholder="选择或填写字典类型" @update:value="markLinkageDirty" />
                        </n-form-item-gi>
                        <n-form-item-gi v-if="rule.type === 'linkedDict'" label="关联字典类型">
                          <n-input v-model:value="rule.dictConfig.linkedDictType" placeholder="选择或填写关联字典类型" @update:value="markLinkageDirty" />
                        </n-form-item-gi>
                      </template>

                      <template v-else>
                        <n-form-item-gi label="请求参数名">
                          <n-input v-model:value="rule.remoteConfig.paramName" placeholder="例如：上级字段参数名" @update:value="markLinkageDirty" />
                        </n-form-item-gi>
                        <n-form-item-gi label="远程接口">
                          <n-input v-model:value="rule.remoteConfig.url" placeholder="选择项接口地址" @update:value="markLinkageDirty" />
                        </n-form-item-gi>
                        <n-form-item-gi label="请求方式">
                          <n-select v-model:value="rule.remoteConfig.method" :options="methodOptions" @update:value="markLinkageDirty" />
                        </n-form-item-gi>
                        <n-form-item-gi v-if="rule.type === 'objectReference'" label="目标对象">
                          <n-select
                            v-model:value="rule.objectConfig.targetObjectCode"
                            :options="targetObjectOptions"
                            clearable
                            filterable
                            placeholder="选择目标对象"
                            @update:value="markLinkageDirty"
                          />
                        </n-form-item-gi>
                      </template>
                      </n-grid>
                    </section>

                    <section class="linkage-state-grid">
                      <n-form-item label="控制字段为空时">
                        <n-select
                          v-model:value="rule.emptyStrategy"
                          :options="emptyStrategyOptions"
                          @update:value="markLinkageDirty"
                        />
                      </n-form-item>
                      <n-form-item label="控制值变化后清空目标">
                        <n-switch
                          :value="rule.clearOnSourceChange !== false"
                          @update:value="value => updateRuleClearOnChange(rule, value)"
                        />
                      </n-form-item>
                      <n-form-item label="启用规则">
                        <n-switch
                          :value="rule.enabled !== false"
                          @update:value="value => updateRuleEnabled(rule, value)"
                        />
                      </n-form-item>
                    </section>
                  </n-form>
                </section>
              </div>
              <n-empty v-else description="暂无级联规则" />
            </section>
          </template>
        </n-spin>
      </main>
    </div>

    <n-modal
      v-model:show="relationWizardVisible"
      preset="card"
      title="新增关系"
      :bordered="false"
      :style="{ width: 'min(760px, calc(100vw - 32px))' }"
    >
      <n-spin :show="relationWizardLoading">
        <n-form label-placement="top" size="small" :show-feedback="false" class="relation-wizard-form">
          <section class="wizard-section">
            <header>
              <span>1</span>
              <div>
                <strong>选择目标对象</strong>
                <small>当前版本统一创建一对多明细关系。</small>
              </div>
            </header>
            <n-form-item label="目标对象">
              <n-select
                v-model:value="relationWizardForm.targetObjectCode"
                :options="targetObjectOptions"
                filterable
                placeholder="选择目标业务对象"
                @update:value="applyRelationWizardDefaults"
              />
            </n-form-item>
          </section>

          <section class="wizard-section">
            <header>
              <span>2</span>
              <div>
                <strong>确认字段端点</strong>
                <small>系统会智能推断，你可以在这里调整。</small>
              </div>
            </header>
            <div class="wizard-endpoint-flow">
              <n-form-item :label="sourceFieldLabel({ relationType: relationWizardForm.relationType })">
                <n-select
                  v-model:value="relationWizardForm.sourceFieldCode"
                  :options="sourceFieldOptions"
                  filterable
                  clearable
                  :placeholder="sourceFieldPlaceholder({ relationType: relationWizardForm.relationType })"
                />
              </n-form-item>
              <div class="wizard-flow-arrow">
                1 → N
              </div>
              <n-form-item :label="targetFieldLabel({ relationType: relationWizardForm.relationType })">
                <n-select
                  v-model:value="relationWizardForm.targetFieldCode"
                  :options="targetFieldOptions({ targetObjectCode: relationWizardForm.targetObjectCode, targetFieldCode: relationWizardForm.targetFieldCode })"
                  :loading="targetFieldLoadingMap[relationWizardForm.targetObjectCode]"
                  filterable
                  clearable
                  :placeholder="targetFieldPlaceholder({ relationType: relationWizardForm.relationType })"
                />
              </n-form-item>
            </div>
          </section>

          <section class="wizard-section">
            <header>
              <span>3</span>
              <div>
                <strong>页面展示</strong>
                <small>这些文案会出现在表单和详情区域。</small>
              </div>
            </header>
            <n-grid :cols="2" :x-gap="12">
              <n-form-item-gi label="关系名称">
                <n-input v-model:value="relationWizardForm.relationName" placeholder="例如：采购单包含采购明细" />
              </n-form-item-gi>
              <n-form-item-gi label="详情页签标题">
                <n-input v-model:value="relationWizardForm.detailTabTitle" placeholder="例如：采购明细" />
              </n-form-item-gi>
            </n-grid>
            <n-form-item v-if="canInlineEdit({ relationType: relationWizardForm.relationType })" label="子表保存模式">
              <n-select v-model:value="relationWizardForm.saveMode" :options="childSaveModeOptions" />
              <div class="wizard-field-hint">
                推荐“行级合并”：按目标字段更新已有明细，未匹配行新增；全量替换会先删除旧明细。
              </div>
            </n-form-item>
          </section>
        </n-form>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button size="small" @click="relationWizardVisible = false">
            取消
          </n-button>
          <n-button size="small" type="primary" :loading="relationWizardLoading" @click="confirmRelationWizard">
            确认
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import {
  businessObjectDesigner,
  businessObjectList,
  businessObjectRelations,
  saveBusinessObjectDesigner,
} from '@/api/business-app'
import LowcodeErDiagram from '@/components/lowcode-builder/model/LowcodeErDiagram.vue'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import {
  applyLinkageSchemaToFields,
  createLinkageSchemaFromFields,
  normalizeLinkageSchema,
  validateLinkageSchema,
} from './form-first/linkageSchema'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  suiteCode: {
    type: String,
    default: '',
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  linkageSchema: {
    type: Object,
    default: null,
  },
  designerActions: {
    type: Array,
    default: () => [],
  },
  designerOptions: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['updated', 'dirtyChange', 'fieldsUpdated', 'update:linkageSchema', 'update:designerActions'])

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const activePanel = ref('er')
const activeRelationKey = ref('')
const activeLinkageRuleId = ref('')
const erCandidateObjectCodes = ref([])
const businessObjects = ref([])
const localRelations = ref([])
const localLinkage = ref(normalizeLinkageSchema())
const targetFieldsMap = ref({})
const targetFieldLoadingMap = ref({})
const relationsLoaded = ref(false)
const relationDirty = ref(false)
let resettingLinkage = false
const relationWizardVisible = ref(false)
const relationWizardLoading = ref(false)
const relationWizardForm = ref({
  relationType: 'DETAIL',
  targetObjectCode: '',
  sourceFieldCode: '',
  targetFieldCode: '',
  saveMode: 'merge',
  relationName: '',
  detailTabTitle: '',
})

const childSaveModeOptions = [
  { label: '全量替换', value: 'replace' },
  { label: '行级合并', value: 'merge' },
]

const approvalQuantityOperationOptions = [
  { label: '增加数量', value: 'INBOUND' },
  { label: '扣减数量', value: 'OUTBOUND' },
]

const selectorFilterSourceOptions = [
  { label: '固定值', value: 'static' },
  { label: '当前表单字段', value: 'formData' },
  { label: '当前记录字段', value: 'record' },
  { label: '当前行字段', value: 'row' },
  { label: '路由查询参数', value: 'query' },
  { label: '路由路径参数', value: 'params' },
]

const linkageTypeOptions = [
  { label: '字典父子(parent_dict_code)', value: 'parentDictCode' },
  { label: '关联字典(linked_dict_type/value)', value: 'linkedDict' },
  { label: '远程参数过滤', value: 'remoteParam' },
  { label: '组织范围过滤', value: 'orgScope' },
  { label: '对象引用过滤', value: 'objectReference' },
]

const emptyStrategyOptions = [
  { label: '显示空选项', value: 'empty' },
  { label: '显示全部选项', value: 'all' },
  { label: '禁用目标字段', value: 'disabled' },
]

const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
]

const sourceFieldOptions = computed(() => {
  const fields = (props.fields || []).map(toPageField).filter(field => field.field && !isInactiveField(field))
  return fields.map(field => ({
    label: businessFieldOptionLabel(field),
    value: field.field,
  }))
})

const targetObjectOptions = computed(() => {
  const objects = (businessObjects.value || []).filter(item => item.objectCode !== props.objectCode)
  return objects.map(item => ({
    label: `${item.objectName || item.objectCode}（${item.objectCode}）`,
    value: item.objectCode,
  }))
})

const objectNameMap = computed(() => new Map((businessObjects.value || []).map(item => [item.objectCode, item.objectName || item.objectCode])))
const fieldMap = computed(() => new Map((props.fields || []).map(field => [field.fieldCode || field.field, toPageField(field)]).filter(([fieldCode]) => fieldCode)))
const activeRelation = computed(() => {
  if (!localRelations.value.length)
    return null
  return localRelations.value.find(relation => relation.clientKey === activeRelationKey.value) || localRelations.value[0]
})
const erDiagramSummary = computed(() => {
  const relationCount = localRelations.value.filter(relation => relation.targetObjectCode).length
  return relationCount ? `${relationCount} 条关系` : '查看当前对象结构'
})
const erDiagramSubtitle = computed(() => {
  const modelCount = erDiagramModels.value.length
  const relationCount = localRelations.value.filter(relation => relation.targetObjectCode).length
  return `${props.objectName || props.objectCode || '当前对象'}：${modelCount} 个对象，${relationCount} 条关系`
})
const erDiagramModels = computed(() => buildErDiagramModels())

watch(() => props.objectId, () => {
  loadRelations()
}, { immediate: true })

watch(() => props.suiteCode, () => {
  loadBusinessObjects()
}, { immediate: true })

watch(
  () => [props.linkageSchema, props.fields],
  () => resetLinkageSchema(),
  { immediate: true, deep: true },
)

watch(localRelations, (relations) => {
  if (!relations.length) {
    activeRelationKey.value = ''
    return
  }
  if (!relations.some(relation => relation.clientKey === activeRelationKey.value))
    activeRelationKey.value = relations[0].clientKey
}, { deep: false })

onMounted(() => {
  loadBusinessObjects()
})

async function loadRelations() {
  if (!props.objectId) {
    localRelations.value = []
    relationsLoaded.value = true
    relationDirty.value = false
    return
  }
  loading.value = true
  relationsLoaded.value = false
  relationDirty.value = false
  try {
    const res = await businessObjectRelations(props.objectId)
    localRelations.value = (res.data || [])
      .filter(relation => !props.objectCode || !relation.sourceObjectCode || relation.sourceObjectCode === props.objectCode)
      .map(normalizeRelation)
    erCandidateObjectCodes.value = Array.from(new Set([
      ...erCandidateObjectCodes.value,
      ...localRelations.value.map(relation => relation.targetObjectCode).filter(Boolean),
    ]))
    relationsLoaded.value = true
    relationDirty.value = false
    emit('updated', localRelations.value)
    localRelations.value.forEach((relation) => {
      loadTargetFields(relation.targetObjectCode)
      loadTargetFields(selectorCandidateObjectCode(relation))
    })
  }
  catch (error) {
    message.error(error?.message || '关系配置加载失败')
  }
  finally {
    loading.value = false
  }
}

async function loadBusinessObjects() {
  try {
    const res = await businessObjectList({
      suiteCode: props.suiteCode || undefined,
    })
    const list = Array.isArray(res.data) ? res.data : []
    const seen = new Set()
    businessObjects.value = list.filter((item) => {
      if (!item.objectCode || seen.has(item.objectCode))
        return false
      seen.add(item.objectCode)
      return true
    })
    if (!erCandidateObjectCodes.value.length && targetObjectOptions.value[0])
      erCandidateObjectCodes.value = [targetObjectOptions.value[0].value]
    localRelations.value.forEach((relation) => {
      loadTargetFields(relation.targetObjectCode)
      loadTargetFields(selectorCandidateObjectCode(relation))
    })
    erCandidateObjectCodes.value.forEach(loadTargetFields)
  }
  catch {
    businessObjects.value = []
  }
}

function resetLinkageSchema() {
  resettingLinkage = true
  try {
    const next = createLinkageSchemaFromFields(props.fields || [], props.linkageSchema || {})
    if (!isSameSchema(next, localLinkage.value))
      localLinkage.value = cloneSchema(next)
  }
  finally {
    resettingLinkage = false
  }
}

function openRelationWizard() {
  const target = targetObjectOptions.value[0]
  if (!target) {
    message.warning('当前套件没有可关联的目标对象')
    return
  }
  relationWizardForm.value = {
    relationType: 'DETAIL',
    targetObjectCode: target.value,
    sourceFieldCode: '',
    targetFieldCode: '',
    saveMode: 'merge',
    relationName: '',
    detailTabTitle: '',
  }
  applyRelationWizardDefaults()
  relationWizardVisible.value = true
}

async function applyRelationWizardDefaults() {
  const { relationType, targetObjectCode } = relationWizardForm.value
  if (!targetObjectCode)
    return
  relationWizardLoading.value = true
  try {
    await loadTargetFields(targetObjectCode)
    const targetName = objectNameMap.value.get(targetObjectCode) || targetObjectCode
    relationWizardForm.value.sourceFieldCode = firstSourceField(relationType, targetObjectCode)
    relationWizardForm.value.targetFieldCode = firstTargetField(targetObjectCode, relationType)
    relationWizardForm.value.relationName = relationLabel({ relationType, targetObjectCode })
    relationWizardForm.value.detailTabTitle = targetName
  }
  finally {
    relationWizardLoading.value = false
  }
}

function confirmRelationWizard() {
  const form = relationWizardForm.value
  if (!form.targetObjectCode) {
    message.warning('请选择目标对象')
    return
  }
  if (!form.sourceFieldCode) {
    message.warning('请选择当前对象字段')
    return
  }
  if (!form.targetFieldCode) {
    message.warning('请选择目标对象字段')
    return
  }
  const relation = buildRelationDraft(form)
  localRelations.value.push(relation)
  activeRelationKey.value = relation.clientKey
  markRelationDirty()
  relationWizardVisible.value = false
  nextTick(() => scrollToRelationSection('basic'))
}

function buildRelationDraft(form = {}) {
  const relationType = normalizeDesignerRelationType(form.relationType)
  const targetName = objectNameMap.value.get(form.targetObjectCode) || form.targetObjectCode
  return {
    clientKey: createClientKey(),
    relationType,
    targetObjectCode: form.targetObjectCode,
    relationName: form.relationName || relationLabel({ relationType, targetObjectCode: form.targetObjectCode }),
    sourceFieldCode: form.sourceFieldCode,
    targetFieldCode: form.targetFieldCode,
    displayField: firstDisplayField(form.targetObjectCode, form.targetFieldCode),
    detailTabTitle: form.detailTabTitle || targetName,
    showInDetail: true,
    inlineCreateEnabled: canInlineEdit({ relationType }),
    inlineEditEnabled: canInlineEdit({ relationType }),
    saveMode: canInlineEdit({ relationType }) ? (form.saveMode || 'merge') : undefined,
    defaultFilter: '',
    selectorEnabled: false,
    selectorSuiteCode: props.suiteCode || '',
    selectorObjectCode: form.targetObjectCode,
    selectorTitle: '',
    selectorButtonText: '选择记录',
    selectorDisplayFields: [],
    selectorKeywordFields: [],
    selectorMappings: [],
    selectorSearchParams: [],
    description: '',
    status: 1,
    sortOrder: localRelations.value.length * 10 + 10,
  }
}

async function handleErConnect(connection = {}) {
  const currentObjectCode = props.objectCode || ''
  const sourceIsCurrent = connection.sourceModelCode === currentObjectCode
  const targetIsCurrent = connection.targetModelCode === currentObjectCode
  if (sourceIsCurrent === targetIsCurrent) {
    message.warning(sourceIsCurrent ? '不能在同一个对象内部创建自关联连线' : '请从当前对象连接到目标对象')
    return
  }

  const targetObjectCode = sourceIsCurrent ? connection.targetModelCode : connection.sourceModelCode
  const sourceFieldCode = sourceIsCurrent ? connection.sourceField : connection.targetField
  const targetFieldCode = sourceIsCurrent ? connection.targetField : connection.sourceField
  if (!targetObjectOptions.value.some(item => item.value === targetObjectCode)) {
    message.warning('目标对象不属于当前业务域，不能创建关系')
    return
  }

  await loadTargetFields(targetObjectCode)
  const existing = localRelations.value.find(relation => relation.targetObjectCode === targetObjectCode)
  if (existing) {
    existing.sourceFieldCode = sourceFieldCode
    existing.targetFieldCode = targetFieldCode
    existing.displayField = existing.displayField || firstDisplayField(targetObjectCode, targetFieldCode)
    activeRelationKey.value = existing.clientKey
    markRelationDirty()
    message.success('已更新关系字段连接')
  }
  else {
    const relation = buildRelationDraft({
      relationType: 'DETAIL',
      targetObjectCode,
      sourceFieldCode,
      targetFieldCode,
      saveMode: 'merge',
    })
    localRelations.value.push(relation)
    activeRelationKey.value = relation.clientKey
    markRelationDirty()
    message.success('关系已创建，请确认右侧配置后保存')
  }
  activePanel.value = 'relation'
  nextTick(() => scrollToRelationSection('basic'))
}

function addRelation() {
  openRelationWizard()
}

function selectRelation(clientKey) {
  activePanel.value = 'relation'
  activeRelationKey.value = clientKey || ''
}

function selectLinkageRule(ruleId) {
  activePanel.value = 'linkage'
  activeLinkageRuleId.value = ruleId || localLinkage.value.rules?.[0]?.ruleId || ''
}

function selectErDiagram() {
  activePanel.value = 'er'
  erCandidateObjectCodes.value.forEach(loadTargetFields)
}

function updateErCandidateObjects(values) {
  const relationTargets = localRelations.value.map(relation => relation.targetObjectCode).filter(Boolean)
  erCandidateObjectCodes.value = Array.from(new Set([...(values || []), ...relationTargets]))
  erCandidateObjectCodes.value.forEach(loadTargetFields)
}

function removeActiveRelation() {
  const current = activeRelation.value
  if (!current)
    return
  const index = localRelations.value.findIndex(relation => relation.clientKey === current.clientKey)
  if (index >= 0)
    removeRelation(index)
}

function scrollToRelationSection(key) {
  activePanel.value = key === 'linkage' ? 'linkage' : 'relation'
  nextTick(() => {
    const element = document.getElementById(`relation-section-${key}`)
    if (element)
      element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function addLinkageRule() {
  const source = sourceFieldOptions.value[0]
  const target = sourceFieldOptions.value.find(item => item.value !== source?.value) || sourceFieldOptions.value[1]
  if (!source || !target) {
    message.warning('至少需要两个可用字段才能配置级联')
    return
  }
  const rule = normalizeLinkageRuleDraft({
    ruleId: createLinkageRuleId(source.value, target.value),
    type: inferLinkageType(target.value),
    sourceField: source.value,
    targetField: target.value,
  })
  localLinkage.value = normalizeLinkageSchema({
    ...localLinkage.value,
    rules: [
      ...(localLinkage.value.rules || []),
      rule,
    ],
  })
  activePanel.value = 'linkage'
  activeLinkageRuleId.value = rule.ruleId
  syncLinkageModel(true)
  nextTick(() => scrollToRelationSection('linkage'))
}

function removeRelation(index) {
  localRelations.value.splice(index, 1)
  const nextRelation = localRelations.value[Math.max(0, index - 1)] || localRelations.value[0]
  activeRelationKey.value = nextRelation?.clientKey || ''
  markRelationDirty()
}

function removeLinkageRule(index) {
  const rules = [...(localLinkage.value.rules || [])]
  rules.splice(index, 1)
  localLinkage.value = normalizeLinkageSchema({
    ...localLinkage.value,
    rules,
  })
  if (!rules.some(rule => rule.ruleId === activeLinkageRuleId.value))
    activeLinkageRuleId.value = rules[0]?.ruleId || ''
  syncLinkageModel(true)
}

async function updateTargetObject(relation, value) {
  relation.targetObjectCode = value
  await loadTargetFields(value)
  const targetName = objectNameMap.value.get(value) || value
  relation.detailTabTitle = relation.detailTabTitle || targetName
  relation.relationName = relationLabel(relation)
  relation.sourceFieldCode = firstSourceField(relation.relationType, value, relation.sourceFieldCode)
  relation.targetFieldCode = firstTargetField(value, relation.relationType)
  relation.displayField = firstDisplayField(value, relation.targetFieldCode)
  if (!relation.selectorObjectCode)
    relation.selectorObjectCode = value || ''
  if (relation.selectorEnabled)
    await applySelectorDefaults(relation, false)
  markDirty()
}

function updateStatus(relation, value) {
  relation.status = value ? 1 : 0
  markDirty()
}

function updateShowInDetail(relation, value) {
  relation.showInDetail = !!value
  markDirty()
}

function updateInlineEdit(relation, value) {
  relation.inlineEditEnabled = canInlineEdit(relation) && !!value
  markDirty()
}

function updateInlineCreate(relation, value) {
  relation.inlineCreateEnabled = canInlineEdit(relation) && !!value
  markDirty()
}

function updateRelationSelectorEnabled(relation, value) {
  relation.selectorEnabled = canInlineEdit(relation) && !!value
  if (relation.selectorEnabled && !relation.selectorObjectCode)
    relation.selectorObjectCode = relation.targetObjectCode || ''
  if (relation.selectorEnabled)
    applySelectorDefaults(relation, false)
  markDirty()
}

function updateRelationApprovalQuantityEnabled(relation, value) {
  relation.approvalQuantityEnabled = canInlineEdit(relation) && !!value
  if (relation.approvalQuantityEnabled) {
    relation.approvalQuantityOperation = relation.approvalQuantityOperation || 'INBOUND'
    relation.approvalQuantityAccountField = relation.approvalQuantityAccountField || inferSourceFieldByTokens(['warehouse', 'store', 'depot', '仓库'])
    relation.approvalQuantityItemField = relation.approvalQuantityItemField || inferTargetFieldByTokens(relation, ['materialId', 'itemId', 'productId', '物料', '商品'])
    relation.approvalQuantityField = relation.approvalQuantityField || inferTargetFieldByTokens(relation, ['quantity', 'qty', 'num', '数量'])
    relation.approvalQuantityItemFallbackFields = normalizeTextList(relation.approvalQuantityItemFallbackFields)
  }
  markDirty()
}

async function updateSelectorObject(relation, value) {
  relation.selectorObjectCode = value || ''
  await loadTargetFields(selectorCandidateObjectCode(relation))
  await applySelectorDefaults(relation, false)
  markDirty()
}

function updateSelectorSearchParamSource(param, value) {
  param.sourceType = value || 'static'
  if (param.sourceType === 'static')
    param.sourceField = ''
  else
    param.staticValue = ''
  markDirty()
}

function addSelectorMapping(relation) {
  relation.selectorMappings = [
    ...(relation.selectorMappings || []),
    createSelectorMappingRow(),
  ]
  markDirty()
}

function removeSelectorMapping(relation, index) {
  relation.selectorMappings.splice(index, 1)
  markDirty()
}

function addSelectorSearchParam(relation) {
  relation.selectorSearchParams = [
    ...(relation.selectorSearchParams || []),
    createSelectorSearchParamRow(),
  ]
  markDirty()
}

function removeSelectorSearchParam(relation, index) {
  relation.selectorSearchParams.splice(index, 1)
  markDirty()
}

function updateLinkageType(rule, value) {
  const next = normalizeLinkageRuleDraft({
    ...rule,
    type: value,
    matchMode: value,
    dataSourceType: resolveLinkageDataSourceType(value),
  })
  Object.assign(rule, next)
  fillRuleFieldDefaults(rule)
  syncLinkageModel(true)
}

function updateLinkageSource(rule, value) {
  rule.sourceField = value || ''
  fillRuleFieldDefaults(rule)
  syncLinkageModel(true)
}

function updateLinkageTarget(rule, value) {
  rule.targetField = value || ''
  fillRuleFieldDefaults(rule)
  syncLinkageModel(true)
}

function updateRuleClearOnChange(rule, value) {
  rule.clearOnSourceChange = !!value
  syncLinkageModel(true)
}

function updateRuleEnabled(rule, value) {
  rule.enabled = !!value
  syncLinkageModel(true)
}

async function saveRelations() {
  if (!props.objectId)
    return
  const shouldSaveRelations = relationsLoaded.value
  if (shouldSaveRelations) {
    if (!relationsLoaded.value) {
      message.warning('关系配置还没有加载完成，请刷新后再保存')
      return
    }
    const invalidRelation = localRelations.value.find(relation => !relation.targetObjectCode || !relation.sourceFieldCode || !relation.targetFieldCode)
    if (invalidRelation) {
      message.warning(`请先补全「${invalidRelation.relationName || relationLabel(invalidRelation)}」的关联对象和匹配字段`)
      return
    }
    const missingDisplayRelation = localRelations.value.find(relation => relation.relationType === 'REFERENCE' && !relation.displayField)
    if (missingDisplayRelation) {
      message.warning(`请先为「${missingDisplayRelation.relationName || relationLabel(missingDisplayRelation)}」选择目标对象回显字段`)
      return
    }
    const invalidSelectorRelation = localRelations.value.find(relation => relation.selectorEnabled && !relation.selectorObjectCode)
    if (invalidSelectorRelation) {
      message.warning(`请先为「${invalidSelectorRelation.relationName || relationLabel(invalidSelectorRelation)}」选择子表选择器候选对象`)
      return
    }
  }
  saving.value = true
  try {
    const linkageSchema = normalizeLinkageSchema(localLinkage.value)
    const validation = validateLinkageSchema(linkageSchema, sourceFieldOptions.value.map(item => item.value))
    if (!validation.valid) {
      scrollToRelationSection('linkage')
      message.warning(validation.errors[0]?.message || '请先修复级联规则')
      return
    }
    const fields = applyLinkageSchemaToFields(props.fields || [], linkageSchema)
    const nextActions = buildDesignerActionsFromRelations(localRelations.value)
    const designerPayload = {
      fields: fields.map(toFieldPayload),
      linkageSchema,
      designerOptions: {
        ...(props.designerOptions || {}),
        actions: nextActions,
      },
    }
    if (shouldSaveRelations)
      designerPayload.relations = localRelations.value.map(toRelationPayload)
    emit('update:designerActions', nextActions)
    await saveBusinessObjectDesigner(props.objectId, designerPayload)
    message.success('关系与级联配置已保存')
    relationDirty.value = false
    emit('fieldsUpdated', fields)
    emit('update:linkageSchema', linkageSchema)
    emit('dirtyChange', false)
    await loadRelations()
  }
  finally {
    saving.value = false
  }
}

function normalizeRelation(relation = {}) {
  const config = parseRelationConfig(relation.relationConfig)
  const selector = normalizeRelationSelector(config.recordSelector || config.selector)
  return {
    ...relation,
    clientKey: relation.id || createClientKey(),
    relationType: normalizeDesignerRelationType(relation.relationType),
    relationName: relation.relationName || relationLabel(relation),
    targetObjectCode: relation.targetObjectCode || '',
    sourceFieldCode: relation.sourceFieldCode || '',
    targetFieldCode: relation.targetFieldCode || '',
    detailTabTitle: config.detailTabTitle || relation.relationName || relation.targetObjectName || relation.targetObjectCode || '',
    showInDetail: config.showInDetail !== false,
    inlineCreateEnabled: canInlineEdit(relation) && config.inlineCreateEnabled !== false && config.inlineCreateEnabled !== 'false',
    inlineEditEnabled: canInlineEdit(relation) && config.inlineEditEnabled !== false && config.inlineEditEnabled !== 'false',
    saveMode: normalizeChildSaveMode(config.saveMode),
    defaultFilter: config.defaultFilter || '',
    displayField: config.displayField || '',
    selectorEnabled: Boolean(selector.objectCode),
    selectorSuiteCode: selector.suiteCode,
    selectorObjectCode: selector.objectCode,
    selectorTitle: selector.title,
    selectorButtonText: selector.buttonText,
    selectorDisplayFields: normalizeDisplayFieldCodes(selector.displayFields),
    selectorKeywordFields: normalizeTextList(selector.keywordFields),
    selectorMappings: createSelectorMappingRows(selector.fieldMappings),
    selectorSearchParams: createSelectorSearchParamRows(selector.searchParams),
    selectorDisplayFieldsText: listToLines(selector.displayFields),
    selectorKeywordFieldsText: listToLines(selector.keywordFields),
    selectorMappingsText: mappingsToLines(selector.fieldMappings),
    selectorSearchParamsText: searchParamsToLines(selector.searchParams),
    ...resolveApprovalQuantityConfig(relation),
    status: relation.status ?? 1,
    sortOrder: relation.sortOrder ?? 0,
  }
}

function toRelationPayload(relation = {}) {
  return {
    id: relation.id,
    suiteCode: props.suiteCode,
    sourceObjectCode: props.objectCode,
    targetObjectCode: relation.targetObjectCode,
    relationType: normalizeDesignerRelationType(relation.relationType),
    relationName: relation.relationName || relationLabel(relation),
    sourceFieldCode: relation.sourceFieldCode,
    targetFieldCode: relation.targetFieldCode,
    relationConfig: buildRelationConfig(relation),
    description: relation.description,
    status: relation.status ?? 1,
    sortOrder: relation.sortOrder ?? 0,
  }
}

function buildRelationConfig(relation) {
  const config = {}
  if (relation.detailTabTitle)
    config.detailTabTitle = relation.detailTabTitle
  config.showInDetail = relation.showInDetail !== false
  config.inlineCreateEnabled = canInlineEdit(relation) && relation.inlineCreateEnabled === true
  config.inlineEditEnabled = canInlineEdit(relation) && relation.inlineEditEnabled === true
  config.saveMode = normalizeChildSaveMode(relation.saveMode)
  if (relation.defaultFilter)
    config.defaultFilter = relation.defaultFilter
  if (relation.displayField)
    config.displayField = relation.displayField
  const selector = buildRelationSelectorConfig(relation)
  if (selector)
    config.recordSelector = selector
  return Object.keys(config).length ? JSON.stringify(config) : ''
}

function parseRelationConfig(value) {
  if (!value)
    return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' ? parsed : {}
  }
  catch {
    return {
      defaultFilter: value,
    }
  }
}

function normalizeRelationSelector(value) {
  const config = value && typeof value === 'object' ? value : {}
  return {
    suiteCode: String(config.suiteCode || '').trim(),
    objectCode: String(config.objectCode || '').trim(),
    title: String(config.title || config.selectorTitle || '').trim(),
    buttonText: String(config.buttonText || '').trim(),
    displayFields: normalizeTextList(config.displayFields),
    keywordFields: normalizeTextList(config.keywordFields),
    fieldMappings: normalizeMappingList(config.fieldMappings || config.mappings),
    searchParams: normalizeSearchParams(config.searchParams),
  }
}

function normalizeDisplayFieldCodes(value) {
  return normalizeTextList(value)
    .map(item => String(item || '').split(/\s*[:：]\s*/, 1)[0].trim())
    .filter(Boolean)
}

function createSelectorMappingRow(source = {}) {
  return {
    clientKey: createClientKey(),
    sourceField: String(source.sourceField || source.source || '').trim(),
    targetField: String(source.targetField || source.target || '').trim(),
  }
}

function createSelectorMappingRows(value) {
  return normalizeMappingList(value).map(createSelectorMappingRow)
}

function createSelectorSearchParamRow(source = {}) {
  const row = normalizeSelectorSearchParamRow(source)
  return {
    clientKey: createClientKey(),
    ...row,
  }
}

function createSelectorSearchParamRows(value) {
  return Object.entries(normalizeSearchParams(value)).map(([key, item]) => createSelectorSearchParamRow({
    paramKey: key,
    value: item,
  }))
}

function normalizeSelectorSearchParamRow(source = {}) {
  const paramKey = String(source.paramKey || source.key || '').trim()
  const sourceType = String(source.sourceType || '').trim()
  if (sourceType) {
    return {
      paramKey,
      sourceType,
      sourceField: String(source.sourceField || '').trim(),
      staticValue: source.staticValue ?? '',
    }
  }
  return {
    paramKey,
    ...parseSelectorParamValue(source.value ?? source.staticValue),
  }
}

function parseSelectorParamValue(value) {
  const textValue = String(value ?? '').trim()
  const matched = textValue.match(/^\$\{(formData|form|record|row|query|routeQuery|params)\.([^}]+)\}$/)
  if (!matched) {
    return {
      sourceType: 'static',
      sourceField: '',
      staticValue: textValue,
    }
  }
  const sourceTypeAlias = {
    form: 'formData',
    routeQuery: 'query',
  }
  return {
    sourceType: sourceTypeAlias[matched[1]] || matched[1],
    sourceField: matched[2],
    staticValue: '',
  }
}

function buildRelationSelectorConfig(relation = {}) {
  if (!relation.selectorEnabled)
    return null
  const selector = normalizeRelationSelector({
    suiteCode: relation.selectorSuiteCode || props.suiteCode,
    objectCode: relation.selectorObjectCode || relation.targetObjectCode,
    title: relation.selectorTitle,
    buttonText: relation.selectorButtonText,
    displayFields: buildSelectorDisplayFields(relation),
    keywordFields: normalizeTextList(relation.selectorKeywordFields?.length ? relation.selectorKeywordFields : relation.selectorKeywordFieldsText),
    fieldMappings: buildSelectorMappings(relation),
    searchParams: buildSelectorSearchParams(relation),
  })
  if (!selector.objectCode)
    return null
  const result = {
    objectCode: selector.objectCode,
  }
  if (selector.suiteCode)
    result.suiteCode = selector.suiteCode
  if (selector.title)
    result.title = selector.title
  if (selector.buttonText)
    result.buttonText = selector.buttonText
  if (selector.displayFields.length)
    result.displayFields = selector.displayFields
  if (selector.keywordFields.length)
    result.keywordFields = selector.keywordFields
  if (selector.fieldMappings.length)
    result.fieldMappings = selector.fieldMappings
  if (Object.keys(selector.searchParams).length)
    result.searchParams = selector.searchParams
  return result
}

function buildSelectorDisplayFields(relation = {}) {
  const codes = normalizeDisplayFieldCodes((relation.selectorDisplayFields || []).length
    ? relation.selectorDisplayFields
    : relation.selectorDisplayFieldsText)
  if (!codes.length)
    return []
  const fieldMap = new Map((targetFieldsMap.value[selectorCandidateObjectCode(relation)] || []).map(field => [field.field, field]))
  return codes.map((fieldCode) => {
    const field = fieldMap.get(fieldCode)
    const label = field ? businessFieldLabel(field) : ''
    return label && label !== fieldCode ? `${fieldCode}:${label}` : fieldCode
  })
}

function buildSelectorMappings(relation = {}) {
  const rows = Array.isArray(relation.selectorMappings) && relation.selectorMappings.length
    ? relation.selectorMappings
    : createSelectorMappingRows(relation.selectorMappingsText)
  return rows
    .map(row => ({
      sourceField: String(row.sourceField || '').trim(),
      targetField: String(row.targetField || '').trim(),
    }))
    .filter(row => row.sourceField && row.targetField)
}

function buildSelectorSearchParams(relation = {}) {
  const rows = Array.isArray(relation.selectorSearchParams) && relation.selectorSearchParams.length
    ? relation.selectorSearchParams
    : createSelectorSearchParamRows(relation.selectorSearchParamsText)
  return rows.reduce((result, row) => {
    const paramKey = String(row.paramKey || '').trim()
    if (!paramKey)
      return result
    const sourceType = row.sourceType || 'static'
    if (sourceType === 'static') {
      if (row.staticValue !== undefined && row.staticValue !== null && String(row.staticValue).trim() !== '')
        result[paramKey] = row.staticValue
      return result
    }
    const sourceField = String(row.sourceField || '').trim()
    if (sourceField)
      result[paramKey] = `\${${sourceType}.${sourceField}}`
    return result
  }, {})
}

function normalizeTextList(value) {
  if (Array.isArray(value))
    return value.map(item => String(item || '').trim()).filter(Boolean)
  if (typeof value === 'string')
    return parseTextList(value)
  return []
}

function parseTextList(value) {
  return String(value || '')
    .split(/[\n,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

function normalizeMappingList(value) {
  if (Array.isArray(value)) {
    return value
      .map(item => ({
        sourceField: String(item?.sourceField || item?.source || '').trim(),
        targetField: String(item?.targetField || item?.target || '').trim(),
      }))
      .filter(item => item.sourceField && item.targetField)
  }
  if (value && typeof value === 'object') {
    return Object.entries(value)
      .map(([sourceField, targetField]) => ({
        sourceField: String(sourceField || '').trim(),
        targetField: String(targetField || '').trim(),
      }))
      .filter(item => item.sourceField && item.targetField)
  }
  if (typeof value === 'string')
    return parseMappingLines(value)
  return []
}

function parseMappingLines(value) {
  return String(value || '')
    .split(/\n/)
    .map(item => item.trim())
    .filter(Boolean)
    .map((item) => {
      const parts = item.split(/\s*(?:=>|=|:)\s*/, 2)
      return {
        sourceField: String(parts[0] || '').trim(),
        targetField: String(parts[1] || '').trim(),
      }
    })
    .filter(item => item.sourceField && item.targetField)
}

function normalizeSearchParams(value) {
  if (!value)
    return {}
  if (typeof value === 'string')
    return parseSearchParamLines(value)
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return Object.entries(value).reduce((result, [key, item]) => {
      const paramKey = String(key || '').trim()
      if (paramKey && item !== undefined && item !== null && item !== '')
        result[paramKey] = item
      return result
    }, {})
  }
  return {}
}

function parseSearchParamLines(value) {
  const text = String(value || '').trim()
  if (!text)
    return {}
  if (text.startsWith('{')) {
    try {
      return normalizeSearchParams(JSON.parse(text))
    }
    catch {
      return {}
    }
  }
  return text
    .split(/\n/)
    .map(item => item.trim())
    .filter(Boolean)
    .reduce((result, item) => {
      const parts = item.split(/\s*(?:=>|=|:)\s*/, 2)
      const key = String(parts[0] || '').trim()
      const paramValue = String(parts[1] || '').trim()
      if (key && paramValue)
        result[key] = paramValue
      return result
    }, {})
}

function listToLines(value) {
  return normalizeTextList(value).join('\n')
}

function mappingsToLines(value) {
  return normalizeMappingList(value)
    .map(item => `${item.sourceField}=${item.targetField}`)
    .join('\n')
}

function searchParamsToLines(value) {
  return Object.entries(normalizeSearchParams(value))
    .map(([key, item]) => `${key}=${item}`)
    .join('\n')
}

function normalizeChildSaveMode(value) {
  return String(value || '').toLowerCase() === 'merge' ? 'merge' : 'replace'
}

function resolveApprovalQuantityConfig(relation = {}) {
  const action = findApprovalQuantityAction(relation)
  const loopStep = action ? findForeachQuantityStep(action) : null
  const quantityStep = loopStep?.quantityStep || null
  const params = quantityStep?.stepConfig?.params || {}
  return {
    approvalQuantityEnabled: Boolean(action),
    approvalQuantityOperation: quantityStep?.stepConfig?.operationType || quantityStep?.stepConfig?.operation || 'INBOUND',
    approvalQuantityAccountField: unwrapFieldPath(params.accountCode, 'record.main.'),
    approvalQuantityItemField: unwrapFieldPath(params.itemCode, `${loopStep?.itemAlias || 'item'}.`),
    approvalQuantityField: unwrapFieldPath(params.quantity, `${loopStep?.itemAlias || 'item'}.`),
    approvalQuantityItemFallbackFields: normalizeTextList(quantityStep?.stepConfig?.itemCodeFallbackFields)
      .map(value => unwrapFieldPath(value, `${loopStep?.itemAlias || 'item'}.`))
      .filter(Boolean),
    approvalQuantityRemark: params.remark || '',
  }
}

function buildDesignerActionsFromRelations(relations = []) {
  const unmanagedActions = (props.designerActions || [])
    .filter(action => !isManagedApprovalQuantityAction(action))
  const managedActions = relations
    .filter(relation => relation.approvalQuantityEnabled === true)
    .map((relation, index) => createApprovalQuantityAction(relation, index))
  return [
    ...unmanagedActions,
    ...managedActions,
  ]
}

function createApprovalQuantityAction(relation = {}, index = 0) {
  const relationName = relation.relationName || relation.detailTabTitle || relationLabel(relation)
  const itemAlias = 'item'
  const collectionKey = relationCollectionKey(relation)
  const fallbackFields = normalizeTextList(relation.approvalQuantityItemFallbackFields)
    .map(field => `${itemAlias}.${field}`)
  return {
    actionCode: relationQuantityActionCode(relation),
    actionName: `${relationName}审批后数量更新`,
    actionPosition: 'DETAIL',
    actionType: 'COMMAND',
    status: 1,
    sortOrder: 500 + index,
    actionConfig: {
      managedBy: 'RELATION_APPROVAL_QUANTITY',
      relationKey: collectionKey,
      targetObjectCode: relation.targetObjectCode,
      triggerScene: 'FLOW_APPROVED',
      successBehavior: 'refreshList',
      steps: [
        {
          stepCode: `loop_${collectionKey}`,
          stepName: `逐行处理${relationName}`,
          stepType: 'FOREACH',
          rollbackOnFailure: true,
          stepConfig: {
            collectionPath: `record.children.${collectionKey}`,
            itemAlias,
            indexAlias: 'index',
            relationKey: collectionKey,
            relationName,
            targetObjectCode: relation.targetObjectCode,
            steps: [
              {
                stepCode: `quantity_${collectionKey}`,
                stepName: '更新数量',
                stepType: 'DOMAIN_ACTION',
                rollbackOnFailure: true,
                stepConfig: {
                  actionType: 'QUANTITY',
                  operationType: relation.approvalQuantityOperation || 'INBOUND',
                  itemCodeFallbackFields: fallbackFields,
                  params: {
                    accountCode: wrapExpression(`record.main.${relation.approvalQuantityAccountField}`),
                    itemCode: wrapExpression(`${itemAlias}.${relation.approvalQuantityItemField}`),
                    quantity: wrapExpression(`${itemAlias}.${relation.approvalQuantityField}`),
                    sourceDetailId: wrapExpression(`${itemAlias}.id`),
                    remark: relation.approvalQuantityRemark || '',
                  },
                },
              },
            ],
          },
        },
      ],
    },
  }
}

function findApprovalQuantityAction(relation = {}) {
  const relationKey = relationCollectionKey(relation)
  return (props.designerActions || []).find((action) => {
    if (!isManagedApprovalQuantityAction(action))
      return false
    const config = action.actionConfig || {}
    if (config.relationKey === relationKey || config.targetObjectCode === relation.targetObjectCode)
      return true
    const loopStep = findForeachQuantityStep(action)
    const collectionPath = loopStep?.foreachStep?.stepConfig?.collectionPath || ''
    return collectionPath.endsWith(relationKey)
  }) || null
}

function isManagedApprovalQuantityAction(action = {}) {
  const config = action.actionConfig || {}
  if (config.managedBy === 'RELATION_APPROVAL_QUANTITY')
    return true
  if (action.actionType !== 'COMMAND')
    return false
  if (!['FLOW_APPROVED', 'APPROVED', ''].includes(String(config.triggerScene || '').toUpperCase()))
    return false
  return Boolean(findForeachQuantityStep(action))
}

function findForeachQuantityStep(action = {}) {
  const steps = Array.isArray(action.actionConfig?.steps) ? action.actionConfig.steps : []
  for (const foreachStep of steps) {
    if (String(foreachStep?.stepType || '').toUpperCase() !== 'FOREACH')
      continue
    const childSteps = Array.isArray(foreachStep.stepConfig?.steps) ? foreachStep.stepConfig.steps : []
    const quantityStep = childSteps.find((step) => {
      const config = step?.stepConfig || {}
      return String(step?.stepType || '').toUpperCase() === 'DOMAIN_ACTION'
        && String(config.actionType || '').toUpperCase() === 'QUANTITY'
    })
    if (quantityStep) {
      return {
        foreachStep,
        quantityStep,
        itemAlias: foreachStep.stepConfig?.itemAlias || 'item',
      }
    }
  }
  return null
}

function relationQuantityActionCode(relation = {}) {
  return `relation_quantity_${relationCollectionKey(relation)}`
}

function relationCollectionKey(relation = {}) {
  return lowerSnake(relation.targetObjectCode || relation.relationName || relation.clientKey || 'detail')
}

function wrapExpression(path) {
  return path && !path.endsWith('undefined') && !path.endsWith('null') ? `\${${path}}` : ''
}

function unwrapFieldPath(value, prefix = '') {
  const text = String(value || '').trim().replace(/^\$\{|\}$/g, '')
  return prefix && text.startsWith(prefix) ? text.slice(prefix.length) : ''
}

function inferSourceFieldByTokens(tokens = []) {
  return sourceFieldOptions.value.find(option => tokens.some(token => optionMatchesToken(option, token)))?.value || ''
}

function inferTargetFieldByTokens(relation = {}, tokens = []) {
  return targetFieldOptions(relation).find(option => tokens.some(token => optionMatchesToken(option, token)))?.value || ''
}

function optionMatchesToken(option = {}, token = '') {
  const textValue = `${option.label || ''} ${option.value || ''}`.toLowerCase()
  return textValue.includes(String(token || '').toLowerCase())
}

function normalizeLinkageRuleDraft(rule = {}) {
  const sourceField = rule.sourceField || ''
  const targetField = rule.targetField || ''
  const type = linkageTypeOptions.some(item => item.value === rule.type) ? rule.type : 'linkedDict'
  const sourceFieldConfig = fieldMap.value.get(sourceField) || {}
  const targetFieldConfig = fieldMap.value.get(targetField) || {}
  const dataSourceType = resolveLinkageDataSourceType(type)
  return {
    ruleId: rule.ruleId || createLinkageRuleId(sourceField, targetField),
    type,
    sourceField,
    targetField,
    dataSourceType,
    matchMode: type,
    dictConfig: {
      ...(rule.dictConfig || {}),
      sourceDictType: rule.dictConfig?.sourceDictType || sourceFieldConfig.dictType || '',
      targetDictType: rule.dictConfig?.targetDictType || targetFieldConfig.dictType || '',
      linkedDictType: rule.dictConfig?.linkedDictType || sourceFieldConfig.dictType || '',
      parentValueSource: rule.dictConfig?.parentValueSource || 'sourceValue',
    },
    remoteConfig: {
      ...(rule.remoteConfig || {}),
      url: rule.remoteConfig?.url || targetFieldConfig.basicProps?.optionSource?.api || '',
      method: rule.remoteConfig?.method || 'GET',
      paramName: rule.remoteConfig?.paramName || sourceField || '',
      valuePath: rule.remoteConfig?.valuePath || 'data',
      labelField: rule.remoteConfig?.labelField || 'label',
      valueField: rule.remoteConfig?.valueField || 'value',
    },
    objectConfig: {
      ...(rule.objectConfig || {}),
      targetObjectCode: rule.objectConfig?.targetObjectCode || targetFieldConfig.referenceObjectCode || targetFieldConfig.basicProps?.referenceObjectCode || '',
      displayField: rule.objectConfig?.displayField || targetFieldConfig.referenceDisplayField || targetFieldConfig.basicProps?.referenceDisplayField || '',
    },
    orgConfig: {
      ...(rule.orgConfig || {}),
      paramName: rule.orgConfig?.paramName || rule.remoteConfig?.paramName || sourceField || '',
    },
    condition: { ...(rule.condition || {}) },
    emptyStrategy: ['empty', 'all', 'disabled'].includes(rule.emptyStrategy) ? rule.emptyStrategy : 'empty',
    clearOnSourceChange: rule.clearOnSourceChange !== false,
    enabled: rule.enabled !== false,
  }
}

function fillRuleFieldDefaults(rule) {
  const normalized = normalizeLinkageRuleDraft(rule)
  Object.assign(rule, normalized)
}

function syncLinkageModel(dirty = false) {
  if (resettingLinkage)
    return
  const normalized = normalizeLinkageSchema(localLinkage.value)
  if (!isSameSchema(normalized, localLinkage.value))
    localLinkage.value = normalized
  if (!isSameSchema(normalized, props.linkageSchema))
    emit('update:linkageSchema', cloneSchema(normalized))
  if (dirty) {
    emit('dirtyChange', true)
  }
}

function resolveLinkageDataSourceType(type) {
  if (['parentDictCode', 'linkedDict'].includes(type))
    return 'dict'
  if (type === 'orgScope')
    return 'org'
  if (type === 'objectReference')
    return 'object'
  return 'remote'
}

function createLinkageRuleId(sourceField = '', targetField = '') {
  return `linkage_${sourceField || 'source'}_${targetField || Date.now()}`
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
}

function inferLinkageType(targetField) {
  const target = fieldMap.value.get(targetField) || {}
  if (target.componentType === 'objectReference' || target.fieldType === 'REFERENCE')
    return 'objectReference'
  if (target.componentType === 'orgTreeSelect' || ['DEPT', 'ORG'].includes(target.fieldType))
    return 'orgScope'
  if (target.dictType || ['DICT', 'RADIO', 'CHECKBOX'].includes(target.fieldType))
    return 'linkedDict'
  return 'remoteParam'
}

function linkageTargetFieldOptions(rule = {}) {
  return sourceFieldOptions.value.filter(item => item.value !== rule.sourceField)
}

function linkageRuleLabel(rule = {}) {
  const target = fieldLabel(rule.targetField)
  const typeLabel = linkageTypeOptions.find(item => item.value === rule.type)?.label || rule.type
  return `${target || '目标字段'} · ${typeLabel}`
}

function linkageRuleSentence(rule = {}) {
  const source = fieldLabel(rule.sourceField) || '上级字段'
  const target = fieldLabel(rule.targetField) || '目标字段'
  const verbs = {
    parentDictCode: '按父级字典过滤',
    linkedDict: '按关联字典过滤',
    remoteParam: '作为远程参数过滤',
    orgScope: '作为组织范围过滤',
    objectReference: '过滤引用对象',
  }
  return `${source} → ${target}，${verbs[rule.type] || '联动'}`
}

function relationMatchSummary(relation = {}) {
  if (!relation.targetObjectCode)
    return '先选择目标对象'
  if (relation.sourceFieldCode && relation.targetFieldCode)
    return `已自动推断：${relation.sourceFieldCode} → ${relation.targetFieldCode}`
  return '请补齐匹配字段'
}

function relationMatchTagType(relation = {}) {
  return relation.sourceFieldCode && relation.targetFieldCode ? 'success' : 'warning'
}

function inlineConfigSummary(relation = {}) {
  const enabledCount = [
    relation.inlineCreateEnabled === true,
    relation.inlineEditEnabled === true,
    relation.showInDetail !== false,
  ].filter(Boolean).length
  return enabledCount ? `${enabledCount} 项已开启` : '未配置'
}

function inlineConfigTagType(relation = {}) {
  return inlineConfigSummary(relation) === '未配置' ? 'default' : 'success'
}

function selectorConfigSummary(relation = {}) {
  return relation.selectorEnabled ? '已启用' : '未启用'
}

function selectorConfigTagType(relation = {}) {
  return relation.selectorEnabled ? 'success' : 'default'
}

function mappingConfigSummary(relation = {}) {
  if (!relation.selectorEnabled)
    return '无映射'
  const count = selectorMappingCount(relation)
  return count ? `${count} 条映射` : '无映射'
}

function mappingConfigTagType(relation = {}) {
  return selectorMappingCount(relation) ? 'success' : 'default'
}

function selectorMappingCount(relation = {}) {
  return (relation.selectorMappings || []).filter(item => item.sourceField && item.targetField).length
}

function approvalConfigSummary(relation = {}) {
  return relation.approvalQuantityEnabled ? '已启用' : '未启用'
}

function approvalConfigTagType(relation = {}) {
  return relation.approvalQuantityEnabled ? 'success' : 'default'
}

function fieldLabel(fieldCode) {
  if (!fieldCode)
    return ''
  const field = fieldMap.value.get(fieldCode)
  return field ? `${field.label || field.fieldName || fieldCode}（${fieldCode}）` : fieldCode
}

function relationSentence(relation) {
  const source = props.objectName || props.objectCode || '当前对象'
  const target = objectNameMap.value.get(relation.targetObjectCode) || relation.targetObjectName || relation.targetObjectCode || '目标对象'
  const verbs = {
    DETAIL: '包含多个',
  }
  return `${source}${verbs[relation.relationType] || '关联'}${target}`
}

function sourceFieldLabel(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '当前对象中的关联字段'
    : '当前对象匹配字段'
}

function sourceFieldPlaceholder() {
  return '通常选择：记录ID'
}

function targetFieldLabel(relation) {
  return relation?.relationType === 'REFERENCE'
    ? '目标对象主键字段'
    : '目标对象里指向本对象的字段'
}

function targetFieldPlaceholder() {
  return '选择目标对象中的所属字段'
}

function displayFieldLabel() {
  return '目标对象回显字段'
}

function targetFieldOptions(relation) {
  const fields = targetFieldsMap.value[relation.targetObjectCode] || []
  const options = fields
    .filter(field => field.field && !isInactiveField(field))
    .map(field => ({
      label: businessFieldOptionLabel(field),
      value: field.field,
    }))
  if (relation.targetFieldCode && !options.some(item => item.value === relation.targetFieldCode)) {
    options.unshift({
      label: `已配置字段：${relation.targetFieldCode}`,
      value: relation.targetFieldCode,
    })
  }
  return options
}

function targetDisplayFieldOptions(relation) {
  const fields = targetFieldsMap.value[relation.targetObjectCode] || []
  const options = fields
    .filter(field => field.field && !isInactiveField(field) && !field.systemField && field.field !== relation.targetFieldCode)
    .map(field => ({
      label: businessFieldOptionLabel(field),
      value: field.field,
    }))
  if (relation.displayField && !options.some(item => item.value === relation.displayField)) {
    options.unshift({
      label: `已配置字段：${relation.displayField}`,
      value: relation.displayField,
    })
  }
  return options
}

function selectorCandidateObjectCode(relation = {}) {
  return relation.selectorObjectCode || relation.targetObjectCode || ''
}

function selectorCandidateFieldOptions(relation, currentValue = '') {
  return fieldOptionsForObject(selectorCandidateObjectCode(relation), currentValue)
}

function selectorTargetFieldOptions(relation, currentValue = '') {
  return fieldOptionsForObject(relation.targetObjectCode, currentValue)
}

function selectorContextFieldOptions(param = {}) {
  if (['query', 'params'].includes(param.sourceType)) {
    return [
      { label: 'id', value: 'id' },
      { label: 'recordId', value: 'recordId' },
      { label: 'objectCode', value: 'objectCode' },
    ]
  }
  return sourceFieldOptions.value
}

function fieldOptionsForObject(objectCode, currentValue = '') {
  const fields = targetFieldsMap.value[objectCode] || []
  const options = fields
    .filter(field => field.field && !isInactiveField(field))
    .map(field => ({
      label: businessFieldOptionLabel(field),
      value: field.field,
    }))
  if (currentValue && !options.some(item => item.value === currentValue)) {
    options.unshift({
      label: `已配置字段：${currentValue}`,
      value: currentValue,
    })
  }
  return options
}

async function loadTargetFields(objectCode) {
  if (!objectCode || targetFieldsMap.value[objectCode] || targetFieldLoadingMap.value[objectCode])
    return
  const targetObject = businessObjects.value.find(item => item.objectCode === objectCode)
  if (!targetObject?.id)
    return
  targetFieldLoadingMap.value = {
    ...targetFieldLoadingMap.value,
    [objectCode]: true,
  }
  try {
    const res = await businessObjectDesigner(targetObject.id)
    const fields = res.data?.fields || res.data?.modelSchema?.fields || []
    targetFieldsMap.value = {
      ...targetFieldsMap.value,
      [objectCode]: fields.map(toPageField),
    }
  }
  catch {
    targetFieldsMap.value = {
      ...targetFieldsMap.value,
      [objectCode]: [],
    }
  }
  finally {
    targetFieldLoadingMap.value = {
      ...targetFieldLoadingMap.value,
      [objectCode]: false,
    }
  }
}

function buildErDiagramModels() {
  const currentCode = props.objectCode || 'current_object'
  const models = [
    {
      modelCode: currentCode,
      modelName: props.objectName || currentCode,
      tableName: currentCode,
      modelSchema: {
        object: {
          code: currentCode,
          name: props.objectName || currentCode,
        },
        tableName: currentCode,
        fields: normalizeErFields(props.fields || []),
        relations: localRelations.value
          .filter(relation => relation.targetObjectCode)
          .map(toErRelation),
      },
    },
  ]
  const targetCodes = Array.from(new Set([
    ...erCandidateObjectCodes.value,
    ...localRelations.value.map(relation => relation.targetObjectCode).filter(Boolean),
  ]))
  targetCodes.forEach((targetCode) => {
    const targetObject = businessObjects.value.find(item => item.objectCode === targetCode) || {}
    models.push({
      modelCode: targetCode,
      modelName: targetObject.objectName || targetCode,
      tableName: targetObject.tableName || targetCode,
      modelSchema: {
        object: {
          code: targetCode,
          name: targetObject.objectName || targetCode,
        },
        tableName: targetObject.tableName || targetCode,
        fields: normalizeErFields(targetFieldsMap.value[targetCode] || []),
        relations: [],
      },
    })
  })
  return models
}

function normalizeErFields(fields = []) {
  const rows = (fields || [])
    .map(toPageField)
    .filter(field => field.field && !isInactiveField(field))
    .map(field => ({
      field: field.field,
      columnName: field.columnName || field.field,
      label: field.label || field.fieldName || field.field,
      dataType: field.dataType || field.fieldType || field.businessFieldType || '',
      primaryKey: Boolean(field.primaryKey) || field.field === 'id' || field.columnName === 'id',
      systemField: Boolean(field.systemField),
    }))
  if (!rows.some(field => field.field === 'id' || field.columnName === 'id')) {
    rows.unshift({
      field: 'id',
      columnName: 'id',
      label: 'ID',
      dataType: 'bigint',
      primaryKey: true,
      systemField: true,
    })
  }
  return rows
}

function toErRelation(relation = {}) {
  return {
    relationKey: relation.clientKey || relation.id || '',
    relationType: normalizeErRelationType(relation.relationType),
    targetObjectCode: relation.targetObjectCode,
    sourceField: relation.sourceFieldCode || 'id',
    targetField: relation.targetFieldCode || 'id',
  }
}

function normalizeErRelationType(value) {
  const type = normalizeDesignerRelationType(value)
  if (type === 'DETAIL')
    return 'ONE_TO_MANY'
  return type
}

function firstTargetField(objectCode, relationType = 'DETAIL', currentValue = '') {
  const fields = targetFieldsMap.value[objectCode] || []
  if (relationType === 'REFERENCE') {
    if (currentValue && currentValue !== 'id' && fields.some(field => field.field === currentValue && !isInactiveField(field)))
      return currentValue
    return fields.find(field => field.field === 'id' && !isInactiveField(field))?.field
      || fields.find(field => field.primaryKey && !isInactiveField(field))?.field
      || fields.find(field => !isInactiveField(field))?.field
      || ''
  }
  const sourceObject = lowerFirst(props.objectCode || '')
  const candidates = [
    `${sourceObject}Id`,
    `${sourceObject}Code`,
    props.objectCode,
    'parentId',
  ].filter(Boolean)
  const activeFields = fields.filter(field => !isInactiveField(field))
  const matched = activeFields.find(field => candidates.includes(field.field))
    || activeFields.find((field) => {
      const label = field.label || ''
      const sourceName = props.objectName || props.objectCode || ''
      return sourceName && label.includes(sourceName)
    })
  if (matched)
    return matched.field
  if (currentValue && currentValue !== 'id' && fields.some(field => field.field === currentValue && !isInactiveField(field)))
    return currentValue
  const fallback = activeFields.find(field => field.field !== 'id' && !field.systemField)
  return fallback?.field || ''
}

function firstDisplayField(objectCode, relationField = '') {
  const targetObject = businessObjects.value.find(item => item.objectCode === objectCode)
  const fields = targetFieldsMap.value[objectCode] || []
  const activeFields = fields.filter(field => field.field && !isInactiveField(field) && !field.systemField && field.field !== relationField)
  const configured = targetObject?.displayField
  const matched = activeFields.find(field => configured && field.field === configured)
    || activeFields.find((field) => {
      const fieldName = String(field.field || '').toLowerCase()
      const label = String(field.label || field.fieldName || '')
      return fieldName.includes('name') || label.includes('名称') || label.includes('姓名')
    })
    || activeFields[0]
  return matched?.field || ''
}

async function applySelectorDefaults(relation, force = true) {
  const candidateObjectCode = selectorCandidateObjectCode(relation)
  await loadTargetFields(candidateObjectCode)
  await loadTargetFields(relation.targetObjectCode)
  const candidateFields = selectorActiveFields(candidateObjectCode)
  const targetFields = selectorActiveFields(relation.targetObjectCode)
  if (force || !(relation.selectorDisplayFields || []).length)
    relation.selectorDisplayFields = inferSelectorDisplayFields(candidateFields)
  if (force || !(relation.selectorKeywordFields || []).length)
    relation.selectorKeywordFields = inferSelectorKeywordFields(candidateFields, relation.selectorDisplayFields)
  if (force || !(relation.selectorMappings || []).some(item => item.sourceField && item.targetField))
    relation.selectorMappings = inferSelectorMappings(candidateFields, targetFields)
  if (!relation.selectorButtonText)
    relation.selectorButtonText = '选择记录'
  if (!relation.selectorTitle) {
    const objectName = objectNameMap.value.get(candidateObjectCode) || candidateObjectCode
    relation.selectorTitle = objectName ? `选择${objectName}` : ''
  }
  markDirty()
}

function selectorActiveFields(objectCode) {
  return (targetFieldsMap.value[objectCode] || [])
    .filter(field => field.field && !isInactiveField(field) && !field.systemField)
}

function inferSelectorDisplayFields(fields = []) {
  const preferred = ['code', 'no', 'number', 'name', 'title']
  const matched = fields.filter(field => preferred.some(key => normalizedFieldToken(field).includes(key)))
  return (matched.length ? matched : fields).slice(0, 4).map(field => field.field)
}

function inferSelectorKeywordFields(fields = [], displayFields = []) {
  const displaySet = new Set(displayFields || [])
  const preferred = fields.filter((field) => {
    const token = normalizedFieldToken(field)
    return token.includes('code') || token.includes('no') || token.includes('number')
      || token.includes('name') || token.includes('title') || displaySet.has(field.field)
  })
  return (preferred.length ? preferred : fields).slice(0, 3).map(field => field.field)
}

function inferSelectorMappings(candidateFields = [], targetFields = []) {
  const candidateByField = new Map(candidateFields.map(field => [field.field, field]))
  const candidateByToken = new Map(candidateFields.map(field => [normalizedFieldToken(field), field]))
  const rows = []
  const usedTargets = new Set()
  targetFields.forEach((target) => {
    const source = candidateByField.get(target.field)
      || candidateByToken.get(normalizedFieldToken(target))
      || findSemanticSourceField(candidateFields, target)
    if (source && !usedTargets.has(target.field)) {
      rows.push(createSelectorMappingRow({
        sourceField: source.field,
        targetField: target.field,
      }))
      usedTargets.add(target.field)
    }
  })
  return rows.slice(0, 8)
}

function findSemanticSourceField(candidateFields = [], target = {}) {
  const targetToken = normalizedFieldToken(target)
  const semanticGroups = [
    ['id', 'recordid'],
    ['code', 'no', 'number', 'sn'],
    ['name', 'title', 'label'],
    ['price', 'amount', 'cost', 'fee'],
    ['unit', 'uom'],
    ['spec', 'model', 'type'],
  ]
  const group = semanticGroups.find(items => items.some(item => targetToken.includes(item)))
  if (!group)
    return null
  return candidateFields.find(field => group.some(item => normalizedFieldToken(field).includes(item))) || null
}

function normalizedFieldToken(field = {}) {
  return `${field.field || ''}_${field.label || ''}_${field.fieldName || ''}`
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .toLowerCase()
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function businessFieldLabel(field = {}) {
  const fieldName = field.field || ''
  if (fieldName === 'id')
    return '记录ID'
  if (fieldName === 'createBy')
    return '创建人'
  if (fieldName === 'createTime')
    return '创建时间'
  if (fieldName === 'updateBy')
    return '修改人'
  if (fieldName === 'updateTime')
    return '修改时间'
  if (fieldName === 'createDept')
    return '创建部门'
  return field.label || field.fieldName || fieldName
}

function businessFieldOptionLabel(field = {}) {
  const code = field.field || field.fieldCode || ''
  const label = businessFieldLabel(field)
  return label && code && label !== code ? `${label}（${code}）` : label || code
}

function relationLabel(relation) {
  return relationSentence(relation)
}

function canInlineEdit(relation = {}) {
  return normalizeDesignerRelationType(relation.relationType) === 'DETAIL'
}

function normalizeDesignerRelationType() {
  return 'DETAIL'
}

function firstSourceField(relationType = 'DETAIL', targetObjectCode = '', currentValue = '') {
  if (relationType === 'REFERENCE') {
    const matched = findReferenceSourceField(targetObjectCode)
    if (matched)
      return matched.value
    if (currentValue && sourceFieldOptions.value.some(item => item.value === currentValue))
      return currentValue
  }
  else {
    const idField = sourceFieldOptions.value.find(item => item.value === 'id')?.value
    if (idField)
      return idField
    if (currentValue && sourceFieldOptions.value.some(item => item.value === currentValue))
      return currentValue
  }
  return sourceFieldOptions.value.find(item => item.value === 'id')?.value || sourceFieldOptions.value[0]?.value || ''
}

function findReferenceSourceField(targetObjectCode = '') {
  const targetObject = businessObjects.value.find(item => item.objectCode === targetObjectCode)
  const targetCode = lowerFirst(targetObjectCode || '')
  const targetName = targetObject?.objectName || ''
  const candidates = [
    `${targetCode}Id`,
    `${targetCode}Code`,
    targetObjectCode,
  ].filter(Boolean)
  return sourceFieldOptions.value.find(item => candidates.includes(item.value))
    || sourceFieldOptions.value.find((item) => {
      const label = item.label || ''
      return targetName && label.includes(targetName)
    })
}

function createClientKey() {
  return `relation_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function lowerFirst(value) {
  if (!value)
    return ''
  const normalized = String(value)
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    .toLowerCase()
  return normalized.replace(/_([a-z0-9])/g, (_, char) => char.toUpperCase())
}

function lowerSnake(value) {
  return String(value || '')
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/\W+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    .toLowerCase()
}

function toPageField(field) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    fieldStatus: field.fieldStatus,
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

function toFieldPayload(field = {}) {
  return {
    fieldName: field.fieldName || field.label,
    fieldCode: field.fieldCode || field.field,
    columnName: field.columnName,
    fieldType: field.fieldType || field.businessFieldType,
    dataType: field.dataType,
    length: field.length,
    precision: field.precision,
    required: field.required,
    defaultValue: field.defaultValue,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
    importable: field.importable,
    exportable: field.exportable,
    componentType: field.componentType,
    queryType: field.queryType,
    dictType: field.dictType,
    sensitiveType: field.sensitiveType,
    encryptAlgorithm: field.encryptAlgorithm,
    sortable: field.sortable,
    systemField: field.systemField,
    readonly: field.readonly,
    fieldStatus: field.fieldStatus,
    referenceObjectCode: field.referenceObjectCode,
    referenceDisplayField: field.referenceDisplayField,
    placeholder: field.basicProps?.placeholder || field.placeholder || '',
    remark: field.remark,
    sortOrder: field.sortOrder,
    fieldBinding: { ...(field.fieldBinding || {}) },
    basicProps: { ...(field.basicProps || {}) },
    advancedProps: { ...(field.advancedProps || {}) },
  }
}

function markDirty() {
  markRelationDirty()
}

function markLinkageDirty() {
  syncLinkageModel(true)
}

function markRelationDirty() {
  relationDirty.value = true
  emit('dirtyChange', true)
}

defineExpose({
  saveRelations,
  loadRelations,
})
</script>

<style scoped>
.business-relation-designer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--bg-secondary, #f7f8fa);
}

.relation-designer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-primary, #fff);
  padding: 10px 14px;
}

.relation-designer-head h3 {
  margin: 0;
  color: var(--text-primary, #1d2129);
  font-size: 16px;
  font-weight: 650;
}

.relation-designer-head p {
  margin: 4px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.relation-workbench {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
}

.relation-step-rail {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  border-right: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-secondary, #f7f8fa);
  padding: 12px 10px;
}

.rail-block {
  display: grid;
  gap: 10px;
}

.rail-divider {
  height: 1px;
  background: #e5e9f2;
}

.rail-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
}

.relation-choice-list {
  display: grid;
  gap: 8px;
}

.relation-choice {
  display: grid;
  gap: 4px;
  width: 100%;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  padding: 9px 10px;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.relation-choice:hover,
.relation-choice.active {
  border-color: #2b6bed;
  background: #f3f7ff;
  box-shadow: 0 6px 16px rgb(43 107 237 / 8%);
}

.er-choice {
  border-style: dashed;
}

.relation-er-panel {
  display: grid;
  gap: 10px;
  min-height: 100%;
  padding: 0;
}

.er-object-picker {
  display: grid;
  grid-template-columns: minmax(180px, 0.45fr) minmax(320px, 1fr);
  align-items: center;
  gap: 12px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
  background: var(--bg-primary, #fff);
  padding: 9px 11px;
}

.er-object-picker > div {
  display: grid;
  gap: 2px;
}

.er-object-picker strong {
  color: var(--text-primary, #1d2129);
  font-size: 12px;
}

.er-object-picker span {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.relation-choice strong {
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.relation-choice span {
  color: #667085;
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.relation-main-pane {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 14px 18px;
  scroll-behavior: smooth;
}

.relation-main-pane :deep(.n-spin-container),
.relation-main-pane :deep(.n-spin-content) {
  display: grid;
  gap: 16px;
  min-height: 100%;
}

.relation-config-card {
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
  background: var(--bg-primary, #fff);
  padding: 14px;
  scroll-margin-top: 16px;
}

.relation-match-box {
  display: grid;
  gap: 10px;
  margin-top: 10px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 7px;
  background: var(--bg-secondary, #f7f8fa);
  padding: 11px;
}

.relation-match-box-head,
.selector-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.relation-match-box-head > div {
  display: grid;
  gap: 2px;
}

.relation-match-box-head strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  font-weight: 650;
}

.relation-match-box-head span {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.relation-identity-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(220px, 1fr) 112px 92px;
  align-items: end;
  gap: 10px;
}

.relation-identity-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.relation-identity-state {
  display: flex;
  min-height: 56px;
  flex-direction: column;
  justify-content: space-between;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: var(--bg-secondary, #f7f8fa);
  padding: 7px 9px;
}

.relation-identity-state span {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.relation-identity-state strong {
  color: var(--text-primary, #1d2129);
  font-size: 12px;
}

.relation-endpoint-flow {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 72px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
}

.relation-endpoint-card {
  min-width: 0;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  padding: 9px 10px 0;
}

.relation-endpoint-card header {
  display: grid;
  gap: 2px;
  margin-bottom: 7px;
}

.relation-endpoint-card header span {
  color: var(--text-tertiary, #86909c);
  font-size: 10px;
}

.relation-endpoint-card header strong {
  overflow: hidden;
  color: var(--text-primary, #1d2129);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-endpoint-card :deep(.n-form-item) {
  margin-bottom: 9px;
}

.endpoint-connector {
  display: grid;
  grid-template-columns: 18px minmax(20px, 1fr) 18px;
  align-items: center;
  color: var(--primary-color, #165dff);
  font-size: 11px;
  font-weight: 700;
  text-align: center;
}

.endpoint-connector i {
  height: 1px;
  background: var(--primary-color, #165dff);
}

.endpoint-connector span {
  border: 1px solid var(--primary-color, #165dff);
  border-radius: 50%;
  line-height: 16px;
}

.relation-display-field {
  max-width: calc(50% - 44px);
}

.relation-display-field :deep(.n-form-item) {
  margin-bottom: 0;
}

.relation-advanced-collapse {
  border: 1px solid #e3e8f2;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 26px rgb(16 24 40 / 4%);
  padding: 4px 16px 10px;
}

.relation-advanced-collapse :deep(.n-collapse-item:not(:last-child)) {
  border-bottom: 1px solid #edf1f7;
}

.advanced-relation-title {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: #111827;
  font-size: 14px;
  font-weight: 650;
}

.advanced-relation-hint,
.selector-toggle-row span {
  margin: 0 0 12px;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.selector-toggle-row {
  margin-bottom: 12px;
}

.section-title-row,
.relation-card-head,
.selector-filter-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.section-title-row > div,
.relation-card-head > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.section-title-row strong,
.relation-card-head strong,
.selector-filter-title strong {
  color: #111827;
  font-size: 14px;
  font-weight: 650;
  overflow-wrap: anywhere;
}

.section-title-row span,
.relation-card-head p {
  margin: 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.relation-card-list {
  display: grid;
  gap: 12px;
}

.relation-card {
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 7px;
  background: var(--bg-secondary, #f7f8fa);
  padding: 12px;
}

.linkage-flow-editor {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) 26px minmax(180px, 1fr) 26px minmax(160px, 1fr);
  align-items: center;
  gap: 6px;
}

.linkage-flow-editor :deep(.n-form-item),
.linkage-state-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.linkage-flow-arrow {
  margin-top: 18px;
  color: var(--primary-color, #165dff);
  font-size: 18px;
  text-align: center;
}

.linkage-detail-section {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  border-top: 1px solid var(--border-light, #e5e6eb);
  padding-top: 10px;
}

.linkage-detail-section > strong {
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}

.linkage-state-grid {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(180px, 1fr) minmax(120px, 0.7fr);
  align-items: end;
  gap: 12px;
  margin-top: 4px;
  border-top: 1px solid var(--border-light, #e5e6eb);
  padding-top: 10px;
}

.selector-row-head,
.selector-mapping-row,
.selector-filter-row {
  display: grid;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.selector-row-head,
.selector-mapping-row {
  grid-template-columns: minmax(220px, 1fr) 56px minmax(220px, 1fr) 34px;
}

.selector-row-head {
  margin-top: 8px;
  color: #667085;
  font-size: 12px;
}

.selector-mapping-list,
.selector-filter-list,
.selector-filter-block {
  display: grid;
  gap: 10px;
}

.selector-filter-block {
  margin-top: 14px;
  border-top: 1px solid #edf1f7;
  padding-top: 14px;
}

.selector-filter-row {
  grid-template-columns: minmax(180px, 1fr) 140px minmax(180px, 1fr) 34px;
}

.selector-mapping-arrow {
  border-radius: 999px;
  background: #eff4ff;
  color: #3157a8;
  font-size: 12px;
  line-height: 24px;
  text-align: center;
  white-space: nowrap;
  padding: 0 8px;
}

.relation-empty-state {
  align-self: center;
  min-height: 260px;
  border: 1px dashed #cfd8e6;
  border-radius: 8px;
  background: #fff;
  padding-top: 72px;
}

.wizard-field-hint {
  margin-top: 6px;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.relation-wizard-form {
  display: grid;
  gap: 10px;
}

.wizard-section {
  display: grid;
  gap: 9px;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 7px;
  background: var(--bg-secondary, #f7f8fa);
  padding: 11px 12px 2px;
}

.wizard-section > header {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.wizard-section > header > span {
  flex: 0 0 22px;
  border-radius: 50%;
  color: #fff;
  background: var(--primary-color, #165dff);
  font-size: 11px;
  font-weight: 700;
  line-height: 22px;
  text-align: center;
}

.wizard-section > header > div {
  display: grid;
  gap: 2px;
}

.wizard-section > header strong {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
}

.wizard-section > header small {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.wizard-section :deep(.n-form-item) {
  margin-bottom: 9px;
}

.wizard-endpoint-flow {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 52px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
}

.wizard-flow-arrow {
  margin-top: 18px;
  border-radius: 999px;
  color: var(--primary-color, #165dff);
  background: color-mix(in srgb, var(--primary-color, #165dff) 9%, transparent);
  font-size: 11px;
  font-weight: 700;
  line-height: 24px;
  text-align: center;
}

@media (max-width: 1180px) {
  .relation-workbench {
    grid-template-columns: 1fr;
  }

  .relation-step-rail {
    position: static;
    grid-template-columns: minmax(0, 1fr);
    border-right: 0;
    border-bottom: 1px solid #e5e9f2;
  }

  .relation-identity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .relation-designer-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .relation-main-pane {
    padding: 14px;
  }

  .relation-match-box-head,
  .selector-toggle-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .relation-identity-grid,
  .relation-endpoint-flow,
  .er-object-picker,
  .linkage-flow-editor,
  .linkage-state-grid,
  .wizard-endpoint-flow {
    grid-template-columns: minmax(0, 1fr);
  }

  .endpoint-connector {
    width: 120px;
    justify-self: center;
  }

  .linkage-flow-arrow {
    margin: -2px 0;
    transform: rotate(90deg);
  }

  .relation-display-field {
    max-width: none;
  }

  .wizard-flow-arrow {
    width: 64px;
    margin: 0;
    justify-self: center;
    transform: rotate(90deg);
  }

  .selector-row-head {
    display: none;
  }

  .selector-mapping-row,
  .selector-filter-row {
    grid-template-columns: 1fr;
  }

  .selector-mapping-arrow {
    width: max-content;
  }
}
</style>
