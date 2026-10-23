<template>
              <n-collapse-item v-if="isField" name="field">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:widgets-outline" />
                    字段组件
                  </span>
                </template>
                <section class="panel-item">
                  <n-form-item v-if="isOptionField && selectedComponent.componentKey !== 'transfer'" label="选项来源">
                    <div class="page-widget-config-stack">
                      <n-select
                        :value="selectedOptionSourceType"
                        :options="optionSourceTypeOptions"
                        :consistent-menu-width="false"
                        @update:value="updateOptionSourceType"
                      />
                      <!-- ────── 本页子表明细：当前表单运行时的子表行集合 ────── -->
                      <template v-if="selectedOptionSourceType === 'CURRENT_CHILDREN'">
                        <div class="option-source-card">
                          <div class="option-source-hint">
                            使用当前主记录下已加载的子表明细作为选项。若要引用其它页面/业务对象的已保存数据，请改用「其它表 / 业务对象」。
                            选中后会把显示名称冗余保存到「{{ resolveSelectionLabelValueField() || '字段Name' }}」，编辑回显无需再查源表。
                          </div>
                          <div class="option-source-field">
                            <label>关联子表</label>
                            <n-select
                              :value="selectedComponent.props?.optionSource?.relationKey || ''"
                              :options="childTableRelationOptions"
                              filterable
                              :disabled="!childTableRelationOptions.length"
                              :placeholder="childTableRelationOptions.length ? '选择子表或对象关系' : '暂无子表：可在主子表配置添加，或改用「其它表 / 业务对象」'"
                              @update:value="updateCurrentChildrenRelationKey"
                            />
                          </div>
                          <div class="option-source-field two-col">
                            <div class="option-source-field">
                              <label>值字段</label>
                              <n-select
                                :value="selectedComponent.props?.optionSource?.valueField || 'id'"
                                :options="childTableFieldOptions"
                                :loading="childTableTargetFieldsLoading"
                                filterable
                                tag
                                size="small"
                                placeholder="选择字段"
                                @update:value="updatePageWidgetOptionSource({ valueField: $event || 'id' })"
                              />
                            </div>
                            <div class="option-source-field">
                              <label>显示字段</label>
                              <n-select
                                :value="selectedComponent.props?.optionSource?.labelField || 'label'"
                                :options="childTableFieldOptions"
                                :loading="childTableTargetFieldsLoading"
                                filterable
                                tag
                                size="small"
                                placeholder="选择字段"
                                @update:value="updatePageWidgetOptionSource({ labelField: $event || 'label' })"
                              />
                            </div>
                          </div>
                          <div class="option-source-toggle">
                            <n-switch
                              size="small"
                              :value="selectedComponent.props?.optionSource?.persistedOnly !== false"
                              @update:value="updatePageWidgetOptionSource({ persistedOnly: $event })"
                            />
                            <span>仅已保存明细</span>
                          </div>
                        </div>
                      </template>
                      <!-- ────── 其它表 / 业务对象：查询已发布业务对象的存量数据 ────── -->
                      <template v-else-if="selectedOptionSourceType === 'BUSINESS_OBJECT'">
                        <div class="option-source-card">
                          <div class="option-source-hint">
                            引用应用内其它页面/业务对象的已保存数据作为下拉选项，不限于当前页子表。
                            选中后会把显示名称冗余保存到「{{ resolveSelectionLabelValueField() || '字段Name' }}」，编辑回显无需再查源表。
                          </div>
                          <div class="option-source-field">
                            <label>业务对象</label>
                            <n-select
                              :value="selectedComponent.props?.optionSource?.sourceKey || ''"
                              :options="businessObjectOptions"
                              :loading="businessObjectLoading"
                              :render-label="renderReferenceObjectLabel"
                              filterable
                              clearable
                              placeholder="选择要引用的业务对象 / 页面表"
                              @update:value="updateBusinessObjectOptionSource"
                            />
                          </div>
                          <div v-if="selectedComponent.props?.optionSource?.sourceKey" class="option-source-field two-col">
                            <div class="option-source-field">
                              <label>值字段</label>
                              <n-select
                                :value="selectedComponent.props?.optionSource?.valueField || 'id'"
                                :options="querySourceMetaFields"
                                :loading="querySourceMetaLoading"
                                filterable
                                size="small"
                                placeholder="选择字段"
                                @update:value="updatePageWidgetOptionSource({ valueField: $event || 'id' })"
                              />
                            </div>
                            <div class="option-source-field">
                              <label>显示字段</label>
                              <n-select
                                :value="selectedComponent.props?.optionSource?.labelField || 'name'"
                                :options="querySourceMetaFields"
                                :loading="querySourceMetaLoading"
                                filterable
                                size="small"
                                placeholder="选择字段"
                                @update:value="updatePageWidgetOptionSource({ labelField: $event || 'name' })"
                              />
                            </div>
                          </div>
                          <div
                            v-if="selectedComponent.props?.optionSource?.sourceKey"
                            class="option-source-field object-reference-mappings"
                          >
                            <label>选中后回填</label>
                            <div
                              v-for="(row, index) in referenceFieldMappings"
                              :key="`bo-map-${index}`"
                              class="object-reference-mappings__row"
                            >
                              <n-select
                                :value="row.sourceField || null"
                                :options="querySourceMetaFields"
                                :loading="querySourceMetaLoading"
                                size="small"
                                filterable
                                placeholder="源字段（业务对象）"
                                @update:value="updateReferenceFieldMapping(index, { sourceField: $event })"
                              />
                              <span class="object-reference-mappings__arrow">→</span>
                              <n-select
                                :value="row.targetField || null"
                                :options="formFieldOptions"
                                size="small"
                                filterable
                                placeholder="目标字段（当前表单）"
                                @update:value="updateReferenceFieldMapping(index, { targetField: $event })"
                              />
                              <n-button size="tiny" quaternary type="error" @click="removeReferenceFieldMapping(index)">
                                删
                              </n-button>
                            </div>
                            <n-button size="tiny" secondary @click="addReferenceFieldMapping">
                              + 添加回填
                            </n-button>
                            <small class="object-reference-mappings__hint">
                              选中下拉项后，把业务对象记录上的字段直接写入当前表单（不必再配「联动动作」或「字段自动查询」）。
                            </small>
                          </div>
                          <div v-if="querySourceMetaError && !querySourceMetaLoading" class="query-source-meta-error">
                            {{ querySourceMetaError }}
                          </div>
                        </div>
                      </template>
                      <!-- ────── 手写系统接口：直接配置当前系统的后台接口 ────── -->
                      <template v-else-if="selectedOptionSourceType === 'REMOTE'">
                        <div class="option-source-card">
                          <div class="option-source-hint">
                            调用当前系统后台接口，格式为 方法@路径，如 get@/api/system/user/list
                          </div>
                          <!-- 级联接口模式下接口地址由「级联选项」统一接管 -->
                          <div v-if="optionLinkageApiManaged" class="option-linkage-hint">
                            选项接口由下方「级联选项」统一配置：{{ optionLinkageApi || '尚未填写' }}
                          </div>
                          <div v-else class="option-source-field">
                            <label>接口地址</label>
                            <n-input
                              :value="selectedComponent.props?.optionSource?.api || ''"
                              size="small"
                              placeholder="方法@路径，如 get@/api/system/user/options"
                              @update:value="updatePageWidgetOptionSource({ api: $event || '' })"
                            />
                          </div>
                          <div class="option-source-field two-col">
                            <div class="option-source-field">
                              <label>请求方式</label>
                              <n-select
                                :value="selectedComponent.props?.optionSource?.method || 'get'"
                                :options="requestMethodOptions"
                                size="small"
                                @update:value="updatePageWidgetOptionSource({ method: $event || 'get' })"
                              />
                            </div>
                            <div class="option-source-field">
                              <label>列表路径</label>
                              <n-input
                                :value="selectedComponent.props?.optionSource?.recordsField || 'records'"
                                size="small"
                                placeholder="如 records"
                                @update:value="updatePageWidgetOptionSource({ recordsField: $event || 'records' })"
                              />
                            </div>
                          </div>
                          <div class="option-source-field two-col">
                            <div class="option-source-field">
                              <label>显示字段</label>
                              <n-input
                                :value="selectedComponent.props?.optionSource?.labelField || 'label'"
                                size="small"
                                placeholder="如 label"
                                @update:value="updatePageWidgetOptionSource({ labelField: $event || 'label' })"
                              />
                            </div>
                            <div class="option-source-field">
                              <label>值字段</label>
                              <n-input
                                :value="selectedComponent.props?.optionSource?.valueField || 'value'"
                                size="small"
                                placeholder="如 value"
                                @update:value="updatePageWidgetOptionSource({ valueField: $event || 'value' })"
                              />
                            </div>
                          </div>
                          <!-- 参数可视化编辑器 -->
                          <div class="option-source-field">
                            <label>
                              请求参数
                              <n-tooltip trigger="hover">
                                <template #trigger>
                                  <span class="option-source-help">?</span>
                                </template>
                                值支持 ${字段名} 引用表单当前值，如 ${deptId}
                              </n-tooltip>
                            </label>
                            <div class="option-source-params">
                              <div
                                v-for="param in optionSourceParamRows"
                                :key="param.key"
                                class="option-source-param-row"
                              >
                                <n-input
                                  :value="param.key"
                                  size="small"
                                  placeholder="参数名"
                                  :disabled="true"
                                  class="param-key-input"
                                />
                                <n-input
                                  :value="param.value"
                                  size="small"
                                  placeholder="参数值"
                                  class="param-value-input"
                                  @update:value="updateOptionSourceParam(param.key, $event)"
                                />
                                <button
                                  type="button"
                                  class="option-source-param-remove"
                                  title="删除参数"
                                  @click="removeOptionSourceParam(param.key)"
                                >
                                  ×
                                </button>
                              </div>
                              <n-button size="tiny" dashed block @click="addOptionSourceParam">
                                + 添加参数
                              </n-button>
                            </div>
                          </div>
                        </div>
                      </template>
                      <!-- ────── 受管查询源：选查询源后自动加载元数据 ────── -->
                      <template v-else-if="selectedOptionSourceType === 'QUERY_SOURCE'">
                        <div class="option-source-card">
                          <div class="option-source-field">
                            <label>查询源</label>
                            <n-select
                              :value="querySourceSelection"
                              :options="querySourceCatalogOptions"
                              :loading="querySourceCatalogLoading"
                              filterable
                              clearable
                              placeholder="选择数据集、业务对象或接口"
                              @update:value="updateQuerySourceSelection"
                            />
                          </div>
                          <div v-if="querySourceSelection" class="option-source-field two-col">
                            <div class="option-source-field">
                              <label>值字段</label>
                              <n-select
                                :value="selectedComponent.props?.optionSource?.valueField || 'id'"
                                :options="querySourceMetaFields"
                                :loading="querySourceMetaLoading"
                                filterable
                                size="small"
                                placeholder="选择字段"
                                @update:value="updatePageWidgetOptionSource({ valueField: $event || 'id' })"
                              />
                            </div>
                            <div class="option-source-field">
                              <label>显示字段</label>
                              <n-select
                                :value="selectedComponent.props?.optionSource?.labelField || 'name'"
                                :options="querySourceMetaFields"
                                :loading="querySourceMetaLoading"
                                filterable
                                size="small"
                                placeholder="选择字段"
                                @update:value="updatePageWidgetOptionSource({ labelField: $event || 'name' })"
                              />
                            </div>
                          </div>
                          <div v-if="querySourceMetaError && !querySourceMetaLoading" class="query-source-meta-error">
                            {{ querySourceMetaError }}
                          </div>
                          <!-- 查询参数：摘要 + 弹窗配置 -->
                          <div v-if="querySourceSelection && !querySourceMetaLoading" class="option-source-field">
                            <label>
                              查询参数
                              <n-tooltip trigger="hover">
                                <template #trigger>
                                  <span class="option-source-help">?</span>
                                </template>
                                参数列表由查询源输入契约自动生成，点击配置按钮选择表单字段引用或输入固定值
                              </n-tooltip>
                            </label>
                            <div class="query-source-param-summary">
                              <span class="query-source-param-summary-text">
                                <template v-if="querySourceParamRows.length">
                                  {{ querySourceParamFilledCount }} / {{ querySourceParamRows.length }} 已配置
                                  <span v-if="querySourceParamMissingRequired" class="query-source-param-warn">
                                    （{{ querySourceParamMissingRequired }} 项必填未填）
                                  </span>
                                </template>
                                <template v-else>
                                  该查询源无需输入参数
                                </template>
                              </span>
                              <n-button
                                v-if="querySourceParamRows.length"
                                size="tiny"
                                secondary
                                type="primary"
                                @click="openQueryParamModal"
                              >
                                配置参数
                              </n-button>
                            </div>
                          </div>
                          <div v-if="querySourceMetaLoading" class="option-source-meta-loading">
                            正在加载查询源元数据…
                          </div>
                        </div>
                      </template>
                      <!-- 树形选择：用业务语言说明「扁平表怎么拼成树」与「一次拉全量还是展开再加载」 -->
                      <div
                        v-if="isTreeOptionField && selectedOptionSourceType !== 'STATIC'"
                        class="option-source-card option-source-tree-card"
                      >
                        <div class="option-source-section-title">
                          树结构怎么拼
                        </div>
                        <div class="option-source-hint">
                          多数业务表是「一行一条 + 父级字段」的扁平数据。告诉系统「挂在谁下面」后，运行时会自动拼成树。
                          若接口已经返回嵌套的 children，可只改「子节点字段名」（高级）。
                        </div>
                        <div class="option-source-field">
                          <label>
                            这条数据挂在谁下面
                            <n-tooltip trigger="hover">
                              <template #trigger>
                                <span class="option-source-help">?</span>
                              </template>
                              对应数据源里的父级列，常见如 parentId、pid、parentCode。根节点该列一般为空或 0。
                            </n-tooltip>
                          </label>
                          <n-select
                            v-if="treeOptionFieldSelectOptions.length"
                            :value="selectedComponent.props?.optionSource?.parentField || 'parentId'"
                            :options="treeOptionFieldSelectOptions"
                            :loading="querySourceMetaLoading || childTableTargetFieldsLoading"
                            filterable
                            tag
                            size="small"
                            placeholder="选择父级字段，如 parentId"
                            @update:value="updateTreeOptionSourceMapping({ parentField: $event || 'parentId' })"
                          />
                          <n-input
                            v-else
                            :value="selectedComponent.props?.optionSource?.parentField || 'parentId'"
                            size="small"
                            placeholder="如 parentId"
                            @update:value="updateTreeOptionSourceMapping({ parentField: $event || 'parentId' })"
                          />
                        </div>
                        <div class="option-source-field two-col">
                          <div class="option-source-field">
                            <label>子节点字段名</label>
                            <n-input
                              :value="selectedComponent.props?.optionSource?.childrenField || 'children'"
                              size="small"
                              placeholder="默认 children"
                              @update:value="updateTreeOptionSourceMapping({ childrenField: $event || 'children' })"
                            />
                          </div>
                          <div class="option-source-field">
                            <label>顶级父级取值</label>
                            <n-input
                              :value="String(selectedComponent.props?.optionSource?.rootParentValue ?? '')"
                              size="small"
                              placeholder="空 / 0，表示根节点"
                              @update:value="updateTreeOptionSourceMapping({ rootParentValue: $event })"
                            />
                          </div>
                        </div>
                        <div class="option-source-section-title">
                          数据怎么加载
                        </div>
                        <n-radio-group
                          class="option-source-load-mode"
                          :value="selectedComponent.props?.optionSource?.loadMode === 'lazy' ? 'lazy' : 'full'"
                          size="small"
                          @update:value="updateTreeOptionSourceMapping({ loadMode: $event || 'full' })"
                        >
                          <n-radio-button value="full">
                            一次全部加载
                          </n-radio-button>
                          <n-radio-button value="lazy">
                            展开时再加载下级
                          </n-radio-button>
                        </n-radio-group>
                        <div class="option-source-hint">
                          <template v-if="selectedComponent.props?.optionSource?.loadMode === 'lazy'">
                            适合组织树、大型分类。首次只拉根节点，展开时再请求下级；数据源需支持按「父级字段」过滤（或提供 /tree 懒加载接口）。
                          </template>
                          <template v-else>
                            适合节点不多的分类树。一次拉齐后本地拼树，打开下拉即可搜索全树。
                          </template>
                        </div>
                      </div>
                      <!-- 动态源数据量：下拉/树共用，防止一次拉太多 -->
                      <div
                        v-if="showOptionLoadLimits"
                        class="option-source-card"
                      >
                        <div class="option-source-section-title">
                          每次最多取多少条
                        </div>
                        <div class="option-source-hint">
                          限制单次请求条数，避免数据源过大拖慢页面。树形全量建议不超过 500；需要更多时改用「展开时再加载下级」。
                        </div>
                        <div class="option-source-field">
                          <n-input-number
                            :value="Number(selectedComponent.props?.optionSource?.pageSize || defaultOptionPageSize)"
                            size="small"
                            :min="1"
                            :max="1000"
                            :show-button="false"
                            placeholder="条数"
                            @update:value="updatePageWidgetOptionSource({ pageSize: $event || defaultOptionPageSize })"
                          />
                        </div>
                      </div>
                    </div>
                  </n-form-item>
                  <!-- 级联选项：下拉级联一站式步骤式配置（运行时消费 props.cascade + optionSource）。
                       场景：选了部门后，人员下拉按部门参数重新加载；按①②③顺序配置即可生效。
                       人员组件（userSelect）也支持：选了组织后，人员只能从该组织范围内选择。 -->
                  <n-form-item v-if="(isOptionField && selectedComponent.componentKey !== 'transfer') || isUserSelectCascadeField">
                    <template #label>
                      <span class="option-list-label option-linkage-label">
                        <span>{{ isUserSelectCascadeField ? '组织级联' : '级联选项' }}</span>
                        <n-tooltip trigger="hover">
                          <template #trigger>
                            <span class="help-icon">?</span>
                          </template>
                          {{ isUserSelectCascadeField ? '选了组织后，人员只能从该组织范围内选择，组织重选时人员同步刷新。' : '本字段选项跟随另一个字段的值变化。例如：先选省份，市列表只显示该省的城市。' }}
                        </n-tooltip>
                        <n-switch
                          size="small"
                          :value="optionLinkageConfig.enabled"
                          @update:value="toggleOptionLinkage"
                        />
                      </span>
                    </template>
                    <div v-if="optionLinkageConfig.enabled" class="option-linkage-steps">
                      <!-- userSelect：组织级联简化配置 -->
                      <template v-if="isUserSelectCascadeField">
                        <div class="option-linkage-step">
                          <span class="option-linkage-step-label">① 组织字段</span>
                          <n-select
                            :value="optionLinkageConfig.sourceField"
                            :options="userSelectCascadeSourceFieldOptions"
                            :consistent-menu-width="false"
                            filterable
                            clearable
                            :placeholder="hasOrgFieldInSchema ? '选哪个字段代表组织' : '未找到组织组件字段，可手动选择其他字段'"
                            @update:value="updateOptionLinkageSourceField"
                          />
                          <span class="option-linkage-step-hint">人员选择范围会按所选组织过滤（{{ optionLinkageConfig.includeChildren === false ? '仅本组织直属人员' : '含子组织人员' }}）</span>
                        </div>
                        <div class="option-linkage-step">
                          <span class="option-linkage-step-label">② 组织范围</span>
                          <div class="option-linkage-modes">
                            <button
                              type="button"
                              class="option-linkage-mode"
                              :class="{ active: optionLinkageConfig.includeChildren !== false }"
                              @click="updateOptionLinkage({ includeChildren: true })"
                            >
                              <strong>含子组织</strong>
                              <span>该组织及其全部下级的人员都可选</span>
                            </button>
                            <button
                              type="button"
                              class="option-linkage-mode"
                              :class="{ active: optionLinkageConfig.includeChildren === false }"
                              @click="updateOptionLinkage({ includeChildren: false })"
                            >
                              <strong>仅本组织</strong>
                              <span>只能选择该组织直属人员</span>
                            </button>
                          </div>
                        </div>
                      </template>
                      <!-- 通用下拉级联：原有配置流程 -->
                      <template v-else>
                        <div class="option-linkage-step">
                          <span class="option-linkage-step-label">① 上级字段</span>
                          <n-select
                            :value="optionLinkageConfig.sourceField"
                            :options="optionLinkageSourceFieldOptions"
                            :consistent-menu-width="false"
                            filterable
                            clearable
                            placeholder="选谁变化时刷新本字段，如：部门"
                            @update:value="updateOptionLinkageSourceField"
                          />
                          <span class="option-linkage-step-hint">不限下拉：输入框、日期、数字等任意组件的值变化都会触发联动</span>
                        </div>
                        <div class="option-linkage-step">
                          <span class="option-linkage-step-label">② 联动方式</span>
                          <div class="option-linkage-modes">
                            <button
                              type="button"
                              class="option-linkage-mode"
                              :class="{ active: optionLinkageConfig.mode === 'remoteParam' }"
                              @click="updateOptionLinkageMode('remoteParam')"
                            >
                              <strong>接口加载</strong>
                              <span>选了上级后按参数请求接口刷新选项（省市区、按部门选人等常用）</span>
                            </button>
                            <button
                              type="button"
                              class="option-linkage-mode"
                              :class="{ active: optionLinkageConfig.mode === 'parentDictCode' }"
                              @click="updateOptionLinkageMode('parentDictCode')"
                            >
                              <strong>本地过滤</strong>
                              <span>从已配置的选项里按上级值筛选（选项数据需含父级编码）</span>
                            </button>
                          </div>
                        </div>
                        <template v-if="optionLinkageConfig.mode === 'remoteParam'">
                          <div class="option-linkage-step">
                            <span class="option-linkage-step-label">③ 选项接口</span>
                            <n-input
                              :value="optionLinkageApi"
                              placeholder="方法@地址，如 get@/api/system/user/list"
                              @update:value="updateOptionLinkageApi"
                            />
                          </div>
                          <div class="option-linkage-step">
                            <span class="option-linkage-step-label">④ 参数名</span>
                            <n-input
                              :value="optionLinkageConfig.paramName"
                              placeholder="接口接收上级值的参数，如 deptId"
                              @update:value="updateOptionLinkage({ paramName: $event || '' })"
                            />
                          </div>
                          <div class="option-linkage-step">
                            <span class="option-linkage-step-label">⑤ 上级为空时</span>
                            <n-select
                              :value="optionLinkageConfig.emptyStrategy"
                              :options="optionLinkageEmptyStrategyOptions"
                              :consistent-menu-width="false"
                              @update:value="updateOptionLinkage({ emptyStrategy: $event || 'empty' })"
                            />
                          </div>
                        </template>
                        <div v-else class="option-linkage-hint">
                          按上级字段值过滤本字段已有选项（静态选项或字典数据需包含父级编码）。
                        </div>
                      </template>
                      <div
                        v-if="optionLinkageSummary"
                        class="option-linkage-summary"
                        :class="{ 'is-warning': !optionLinkageConfig.sourceField || (!isUserSelectCascadeField && optionLinkageConfig.mode === 'remoteParam' && !optionLinkageApi) }"
                      >
                        {{ optionLinkageSummary }}
                      </div>
                      <div class="switch-line compact">
                        <span>上级变化时清空本字段已选值</span>
                        <n-switch
                          size="small"
                          :value="optionLinkageConfig.clearOnParentChange"
                          @update:value="updateOptionLinkage({ clearOnParentChange: $event })"
                        />
                      </div>
                    </div>
                    <div v-else class="option-linkage-hint">
                      {{ isUserSelectCascadeField ? '未开启：人员组件显示全部用户。开启后可按组织范围筛选人员。' : '未开启：选项固定不变。需要“先选 A、B 的选项跟着变”时开启。' }}
                    </div>
                  </n-form-item>
                  <!-- 选项列表：紧跟选项来源（仅静态来源时手动维护，不再放独立折叠项 — 用户反馈"太分散"）
                       宜搭式单行编辑：名称输入框占满行宽（值默认跟随名称），行尾仅保留禁用/删除图标，
                       避免 4 控件挤一行导致输入框被压到几十像素看不见 -->
                  <n-form-item
                    v-if="isManualOptionField && selectedOptionSourceType === 'STATIC'"
                  >
                    <template #label>
                      <span class="option-list-label">
                        <span>选项列表</span>
                        <n-checkbox
                          size="small"
                          :checked="showOptionValues"
                          @update:checked="showOptionValues = $event"
                        >
                          自定义值
                        </n-checkbox>
                      </span>
                    </template>
                    <div class="option-list">
                      <div
                        v-for="(option, optionIndex) in selectedOptions"
                        :key="`option-${optionIndex}`"
                        class="option-row"
                        :class="{ 'is-disabled': !!option.disabled }"
                      >
                        <span class="option-row-index">{{ optionIndex + 1 }}</span>
                        <n-input
                          class="option-row-label-input"
                          :value="option.label"
                          size="small"
                          placeholder="选项名称"
                          @update:value="updateOptionLabel(optionIndex, $event)"
                        />
                        <n-input
                          v-if="showOptionValues"
                          class="option-row-value-input"
                          :value="String(option.value ?? '')"
                          size="small"
                          placeholder="值"
                          @update:value="updateOption(optionIndex, { value: $event })"
                        />
                        <div class="option-row-actions">
                          <button
                            type="button"
                            class="option-row-action option-row-action--ban"
                            :class="{ active: !!option.disabled }"
                            :title="option.disabled ? '取消禁用' : '禁用选项'"
                            @click="toggleOptionDisabled(optionIndex)"
                          >
                            <n-icon :size="13">
                              <BanOutline />
                            </n-icon>
                          </button>
                          <button
                            type="button"
                            class="option-row-action option-row-action--remove"
                            title="删除选项"
                            @click="removeOption(optionIndex)"
                          >
                            <n-icon :size="14">
                              <CloseOutline />
                            </n-icon>
                          </button>
                        </div>
                        <template v-if="selectedComponent.componentKey === 'checkbox'">
                          <div class="option-row-extra">
                            <n-switch
                              size="small"
                              :value="!!option.indeterminate"
                              @update:value="updateOption(optionIndex, { indeterminate: $event })"
                            />
                            <span>半选</span>
                            <n-switch
                              size="small"
                              :value="option.focusable !== false"
                              @update:value="updateOption(optionIndex, { focusable: $event })"
                            />
                            <span>可聚焦</span>
                          </div>
                          <n-input
                            class="option-row-props-input"
                            :value="stringifyJsonProp(option.props)"
                            type="textarea"
                            :autosize="{ minRows: 1, maxRows: 3 }"
                            placeholder="Checkbox props JSON，例如 {&quot;checkedValue&quot;:true}"
                            @update:value="updateOptionJsonProps(optionIndex, $event)"
                          />
                        </template>
                      </div>
                      <!-- 新增按钮必须在 .option-list 内部：.n-form-item-blank 是 display:flex(row)，
                           按钮与列表并列会被横向挤到选项区右侧 -->
                      <n-button class="option-add-button" size="small" secondary block @click="addOption">
                        <template #icon>
                          <span class="option-add-icon">+</span>
                        </template>
                        新增选项
                      </n-button>
                    </div>
                  </n-form-item>
                  <template v-if="selectedComponent.componentKey === 'transfer'">
                    <n-form-item label="数据来源">
                      <n-select
                        :value="selectedComponent.props?.dataSourceType || 'static'"
                        :options="transferDataSourceOptions"
                        @update:value="updateComponent({ props: { dataSourceType: $event || 'static' } })"
                      />
                    </n-form-item>
                    <n-form-item label="分栏标题">
                      <div class="option-editor-row two-columns">
                        <n-input
                          :value="selectedComponent.props?.sourceTitle || '可选项'"
                          size="small"
                          placeholder="左侧标题"
                          @update:value="updateComponent({ props: { sourceTitle: $event || '可选项' } })"
                        />
                        <n-input
                          :value="selectedComponent.props?.targetTitle || '已选项'"
                          size="small"
                          placeholder="右侧标题"
                          @update:value="updateComponent({ props: { targetTitle: $event || '已选项' } })"
                        />
                      </div>
                    </n-form-item>
                    <n-form-item v-if="selectedComponent.props?.dataSourceType === 'remote'" label="远程接口">
                      <div class="page-widget-config-stack">
                        <n-input
                          :value="selectedComponent.props?.optionSource?.api || ''"
                          placeholder="例如 get@/api/system/user/options"
                          @update:value="updatePageWidgetOptionSource({ api: $event })"
                        />
                        <div class="option-editor-row two-columns">
                          <n-select
                            :value="selectedComponent.props?.optionSource?.method || 'get'"
                            :options="requestMethodOptions"
                            @update:value="updatePageWidgetOptionSource({ method: $event || 'get' })"
                          />
                          <n-input
                            :value="selectedComponent.props?.optionSource?.recordsField || 'records'"
                            placeholder="列表路径"
                            @update:value="updatePageWidgetOptionSource({ recordsField: $event || 'records' })"
                          />
                        </div>
                        <div class="option-editor-row two-columns">
                          <n-input
                            :value="selectedComponent.props?.optionSource?.labelField || 'label'"
                            placeholder="显示字段"
                            @update:value="updatePageWidgetOptionSource({ labelField: $event || 'label' })"
                          />
                          <n-input
                            :value="selectedComponent.props?.optionSource?.valueField || 'value'"
                            placeholder="值字段"
                            @update:value="updatePageWidgetOptionSource({ valueField: $event || 'value' })"
                          />
                        </div>
                        <n-input
                          :value="selectedComponent.props?.optionSource?.paramsText || '{}'"
                          placeholder="请求参数 JSON"
                          @update:value="updatePageWidgetOptionSource({ paramsText: $event || '{}' })"
                        />
                      </div>
                    </n-form-item>
                  </template>
                  <n-form-item v-if="isDictLikeField" label="字典类型">
                    <DictTypeSelect
                      :value="selectedComponent.props?.dictType || ''"
                      :fields="dictTypeFields"
                      compact
                      @update:value="updateDictType"
                    />
                  </n-form-item>
                  <template v-if="isRelationField">
                    <n-form-item :label="isObjectReferenceField ? '引用对象' : '目标对象'">
                      <n-select
                        :value="isObjectReferenceField ? referenceObjectCode : recordSelectorObjectCode"
                        :options="businessObjectOptions"
                        :loading="businessObjectLoading"
                        :render-label="renderReferenceObjectLabel"
                        filterable
                        clearable
                        placeholder="选择目标业务对象"
                        @update:value="isObjectReferenceField ? updateReferenceObjectCode($event) : updateRecordSelectorObjectCode($event)"
                      />
                    </n-form-item>
                    <!-- 下拉模式（objectReference）：显示字段 + 值字段 + 选中后回填 -->
                    <template v-if="isObjectReferenceField">
                      <n-form-item v-if="referenceObjectCode" label="显示字段">
                        <n-select
                          :value="referenceDisplayField"
                          :options="referenceTargetFieldOptions"
                          :loading="referenceTargetFieldLoading"
                          filterable
                          clearable
                          placeholder="选择下拉选项中显示的字段"
                          @update:value="updateReferenceDisplayField"
                        />
                      </n-form-item>
                      <n-form-item v-if="referenceObjectCode" label="值字段">
                        <n-select
                          :value="referenceValueField"
                          :options="referenceTargetFieldOptions"
                          :loading="referenceTargetFieldLoading"
                          filterable
                          clearable
                          placeholder="选择保存到当前字段的值字段，默认 id"
                          @update:value="updateReferenceValueField"
                        />
                      </n-form-item>
                      <n-form-item v-if="referenceObjectCode" label="选中后回填">
                        <div class="object-reference-mappings">
                          <div
                            v-for="(row, index) in referenceFieldMappings"
                            :key="`ref-map-${index}`"
                            class="object-reference-mappings__row"
                          >
                            <n-select
                              :value="row.sourceField || null"
                              :options="referenceTargetFieldOptions"
                              :loading="referenceTargetFieldLoading"
                              size="small"
                              filterable
                              placeholder="源字段（被引用对象）"
                              @update:value="updateReferenceFieldMapping(index, { sourceField: $event })"
                            />
                            <span class="object-reference-mappings__arrow">→</span>
                            <n-select
                              :value="row.targetField || null"
                              :options="formFieldOptions"
                              size="small"
                              filterable
                              placeholder="目标字段（当前表单）"
                              @update:value="updateReferenceFieldMapping(index, { targetField: $event })"
                            />
                            <n-button size="tiny" quaternary type="error" @click="removeReferenceFieldMapping(index)">
                              删
                            </n-button>
                          </div>
                          <n-button size="tiny" secondary @click="addReferenceFieldMapping">
                            + 添加回填
                          </n-button>
                          <small class="object-reference-mappings__hint">
                            选中下拉项后，把被引用对象上的字段直接写入当前表单（不必再配「字段自动查询」）。
                          </small>
                        </div>
                      </n-form-item>
                    </template>
                    <!-- 弹窗模式（recordSelector）：值字段 + 选择器设置（搜索字段/展示列/过滤/映射） -->
                    <template v-if="isRecordSelectorField">
                      <n-form-item v-if="recordSelectorObjectCode" label="值字段">
                        <n-select
                          :value="recordSelectorValueField"
                          :options="recordSelectorTargetFieldOptions"
                          :loading="recordSelectorTargetFieldLoading"
                          filterable
                          clearable
                          placeholder="保存到当前字段的值字段，默认 id"
                          @update:value="updateRecordSelectorValueField"
                        />
                      </n-form-item>
                      <n-form-item v-if="recordSelectorObjectCode" label="选择器设置">
                        <div class="record-selector-advanced">
                          <n-button size="small" @click="rsConfigDialogShow = true">
                            配置
                          </n-button>
                          <span class="record-selector-advanced-summary">
                            {{ rsConfigSummary }}
                          </span>
                        </div>
                      </n-form-item>
                    </template>
                  </template>
                  <!-- 通用属性后置：选项类核心配置（选项来源/字典/引用对象/transfer 数据源）置顶后，
                       占位提示/默认值等通用项紧随其后（主次分明，参考钉钉宜搭属性面板分区） -->
                  <n-form-item label="占位提示">
                    <n-input
                      :value="selectedComponent.props?.placeholder"
                      clearable
                      placeholder="请输入"
                      @update:value="updateComponent({ props: { placeholder: $event } })"
                    />
                  </n-form-item>
                  <n-form-item label="默认值">
                    <FieldDefaultValueEditor
                      :component-key="selectedComponent.componentKey"
                      :component="selectedComponent"
                      :field-asset="selectedFieldAsset"
                      :model-value="selectedComponent.props?.defaultValue"
                      :option-select-options="defaultValueSelectOptions"
                      :option-multiple="defaultValueSelectMultiple"
                      :option-loading="defaultValueOptionsLoading"
                      @update:model-value="updateDefaultValue"
                      @update:props="updateComponent({ props: $event })"
                    />
                  </n-form-item>
                  <n-form-item v-if="supportsFieldMaxLength" label="最大长度">
                    <div class="option-editor-row two-columns">
                      <n-input-number
                        :value="selectedFieldMaxLength"
                        :min="1"
                        :max="2048"
                        :show-button="false"
                        clearable
                        placeholder="最大长度"
                        @update:value="updateFieldMaxLength"
                      />
                      <n-switch
                        :value="selectedComponent.props?.showCount === true"
                        size="small"
                        @update:value="updateComponent({ props: { showCount: $event } })"
                      >
                        <template #checked>
                          计数
                        </template>
                        <template #unchecked>
                          计数
                        </template>
                      </n-switch>
                    </div>
                  </n-form-item>
                  <FieldNumberConstraintPanel
                    v-if="supportsFieldNumberRange"
                    :component="selectedComponent"
                    :field-asset="selectedFieldAsset"
                    @update="updateComponent({ props: $event })"
                  />
                  <!-- Naive Input / InputNumber 插槽：前缀后缀文案与图标、清除/密码图标、成对分隔符 -->
                  <FieldInputSlotsPanel
                    v-if="supportsFieldAffix"
                    :component="selectedComponent"
                    @update="updateComponent({ props: $event })"
                  />
                  <n-form-item v-if="supportsFieldShowButton" label="加减按钮">
                    <n-switch
                      size="small"
                      :value="selectedComponent.props?.showButton !== false && selectedComponent.props?.controls !== false"
                      @update:value="updateComponent({ props: { showButton: $event } })"
                    >
                      <template #checked>
                        显示
                      </template>
                      <template #unchecked>
                        隐藏
                      </template>
                    </n-switch>
                  </n-form-item>
                  <!-- Naive Select 插槽：header / action / empty / arrow -->
                  <FieldSelectSlotsPanel
                    v-if="supportsFieldSelectSlots"
                    :component="selectedComponent"
                    @update="updateComponent({ props: $event })"
                  />
                  <!-- Naive DatePicker 插槽：date-icon / footer / 导航图标 / separator -->
                  <FieldDatePickerSlotsPanel
                    v-if="supportsFieldDatePickerSlots"
                    :component="selectedComponent"
                    @update="updateComponent({ props: $event })"
                  />
                  <n-form-item v-if="selectedComponent.componentKey === 'barcodeScanner'" label="扫码输入设置">
                    <div class="field-constraint-config">
                      <div class="switch-line compact">
                        <span>允许手工输入</span>
                        <n-switch
                          size="small"
                          :value="selectedComponent.props?.allowManualInput !== false"
                          @update:value="updateComponent({ props: { allowManualInput: $event } })"
                        />
                      </div>
                      <n-input-number
                        :value="selectedComponent.props?.timeoutMs || 30000"
                        :min="1000"
                        :max="60000"
                        :step="1000"
                        :show-button="false"
                        placeholder="扫码超时（毫秒）"
                        @update:value="updateComponent({ props: { timeoutMs: $event || 30000 } })"
                      />
                      <n-select
                        :value="selectedComponent.props?.formats || []"
                        :options="barcodeFormatOptions"
                        multiple
                        clearable
                        filterable
                        placeholder="限定码制（不选表示全部）"
                        @update:value="updateComponent({ props: { formats: $event || [] } })"
                      />
                    </div>
                  </n-form-item>
                  <n-form-item label="输入校验">
                    <div class="field-constraint-config">
                      <n-select
                        :value="selectedComponent.validation?.preset || ''"
                        :options="commonValidationOptions"
                        clearable
                        placeholder="选择常用校验：手机号、邮箱、身份证等"
                        @update:value="updateValidationPreset"
                      />
                      <n-input
                        :value="selectedComponent.validation?.pattern || ''"
                        clearable
                        placeholder="自定义正则表达式（选填），例如 ^1[3-9]\d{9}$"
                        @update:value="updateComponent({ validation: { pattern: $event || undefined } })"
                      />
                    </div>
                  </n-form-item>
                  <n-form-item v-if="supportsAutoGeneration" label="自动编号">
                    <div class="auto-code-config">
                      <div class="switch-line compact">
                        <span>新增时自动生成</span>
                        <n-switch
                          size="small"
                          :value="selectedGenerationEnabled"
                          @update:value="handleGenerationEnabled"
                        />
                      </div>
                      <template v-if="selectedGenerationEnabled">
                        <n-select
                          :value="selectedGenerationRuleCode"
                          :options="codeRuleOptions"
                          :loading="codeRuleLoading"
                          clearable
                          filterable
                          placeholder="选择编码规则"
                          @update:value="updateGenerationRule"
                        />
                        <div class="option-editor-row two-columns">
                          <n-select
                            :value="selectedGenerationConfig.fillPolicy || 'EMPTY_ONLY'"
                            :options="generationFillPolicyOptions"
                            @update:value="updateGenerationConfig({ fillPolicy: $event || 'EMPTY_ONLY' })"
                          />
                          <n-select
                            :value="selectedGenerationConfig.trigger || 'ON_CREATE'"
                            :options="generationTriggerOptions"
                            @update:value="updateGenerationConfig({ trigger: $event || 'ON_CREATE' })"
                          />
                        </div>
                        <div class="switch-line compact">
                          <span>表单填写隐藏</span>
                          <n-switch
                            size="small"
                            :value="!!selectedComponent.visibility?.hidden"
                            @update:value="updateComponentHidden"
                          />
                        </div>
                        <div class="switch-line compact">
                          <span>运行态只读</span>
                          <n-switch
                            size="small"
                            :value="selectedGenerationConfig.readonly !== false"
                            @update:value="updateGenerationReadonly"
                          />
                        </div>
                        <div v-if="selectedGenerationRule" class="auto-code-rule-summary">
                          <span>{{ selectedGenerationRule.ruleName }}</span>
                          <code>{{ selectedGenerationRule.template }}</code>
                          <small>{{ selectedGenerationRule.category || 'COMMON' }} · 结构化规则</small>
                        </div>
                        <div class="auto-code-preview-row">
                          <n-button
                            size="small"
                            secondary
                            :disabled="!selectedGenerationRuleCode"
                            :loading="codeRulePreviewing"
                            @click="() => previewSelectedGenerationRule()"
                          >
                            预览编号
                          </n-button>
                          <strong :class="{ invalid: codeRulePreview?.valid === false }">
                            {{ codeRulePreview?.previewCode || '选择规则后可预览' }}
                          </strong>
                        </div>
                        <div v-if="codeRulePreview?.errors?.length" class="auto-code-issue error">
                          {{ codeRulePreview.errors[0].message }}
                        </div>
                        <div v-else-if="codeRulePreview?.warnings?.length" class="auto-code-issue warning">
                          {{ codeRulePreview.warnings[0].message }}
                        </div>
                      </template>
                    </div>
                  </n-form-item>
                  <n-form-item label="自动计算">
                    <div class="formula-config-entry">
                      <div>
                        <strong>{{ selectedFormulaConfig?.type ? `${selectedFormulaConfig.type} 公式` : '未启用公式' }}</strong>
                        <span>{{ selectedFormulaSummary || '按公式自动计算本字段值，例如：合计 = 单价 × 数量' }}</span>
                      </div>
                      <n-button size="small" secondary @click="openFieldFormulaPanel">
                        配置公式
                      </n-button>
                    </div>
                  </n-form-item>
                  <n-form-item label="组件尺寸">
                    <n-select
                      :value="selectedComponent.props?.size || ''"
                      :options="componentSizeOptions"
                      @update:value="updateComponent({ props: { size: $event || undefined } })"
                    />
                  </n-form-item>
                  <n-form-item v-if="hasSpecPanelProps" label="组件属性">
                    <n-button size="small" dashed block @click="componentPropsVisible = true">
                      <template #icon>
                        <n-icon :size="14">
                          <SettingsOutline />
                        </n-icon>
                      </template>
                      更多属性
                    </n-button>
                  </n-form-item>
                  <div class="switch-list">
                    <label v-if="supportsMultipleSelect">
                      <span>多选</span>
                      <n-switch
                        size="small"
                        :value="selectedMultipleEnabled"
                        @update:value="updateMultipleSelect"
                      />
                    </label>
                    <label>
                      <span>可清空</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.clearable !== false"
                        @update:value="updateComponent({ props: { clearable: $event } })"
                      />
                    </label>
                    <label>
                      <span>显示反馈</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.showFeedback !== false"
                        @update:value="updateComponent({ props: { showFeedback: $event } })"
                      />
                    </label>
                  </div>
                  <p v-if="supportsMultipleSelect && selectedMultipleEnabled" class="field-multiple-hint">
                    多选值按逗号分隔存储，并同步保存对应名称。保存并发布后生效。
                  </p>
                </section>
              </n-collapse-item>


</template>

<script>
import { useForgePropertyPanelApi } from '../forgePropertyPanelContext'
import { forgePropertyPanelLocalComponents } from '../forgePropertyPanelLocalComponents'

export default {
  name: 'SelectedFieldCollapse',
  components: { ...forgePropertyPanelLocalComponents },
  setup() {
    return useForgePropertyPanelApi()
  },
}
</script>

<style scoped src="../forge-property-panel-shell.css"></style>
<style scoped src="../forge-property-panel-fields.css"></style>
