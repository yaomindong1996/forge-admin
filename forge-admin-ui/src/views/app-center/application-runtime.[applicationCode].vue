<template>
  <div class="application-runtime-page">
    <n-spin :show="loading">
      <template v-if="application">
        <header v-if="editing" class="runtime-header">
          <div class="runtime-brand">
            <n-button quaternary circle aria-label="返回应用工作台" @click="openWorkspace">
              <template #icon>
                <NIcon><ArrowBackOutline /></NIcon>
              </template>
            </n-button>
            <button type="button" class="runtime-breadcrumb" title="返回应用工作台" @click="openWorkspace">
              <span>应用工作台</span>
              <span aria-hidden="true">›</span>
            </button>
            <div class="runtime-brand-copy">
              <span class="runtime-brand-app-icon" aria-hidden="true"><NIcon><FolderOpenOutline /></NIcon></span>
              <div class="runtime-design-title">
                <span>应用页面设计</span>
                <strong>{{ application.applicationName || '未命名应用' }}</strong>
              </div>
              <span class="runtime-brand-status">{{ editing ? (dirty ? '未保存修改' : '已保存到草稿') : isDraftPreview ? '草稿预览' : currentNode?.title || '首页' }}</span>
            </div>
          </div>
          <n-space v-if="!formDesignerMode" size="small">
            <n-button v-if="editing" circle secondary :disabled="!canUndo" title="撤销" aria-label="撤销" @click="undoBuilder">
              <template #icon>
                <NIcon><ArrowUndoOutline /></NIcon>
              </template>
            </n-button>
            <n-button v-if="editing" circle secondary :disabled="!canRedo" title="重做" aria-label="重做" @click="redoBuilder">
              <template #icon>
                <NIcon><ArrowRedoOutline /></NIcon>
              </template>
            </n-button>
            <n-popover v-if="editing" trigger="click" placement="bottom-end" :show-arrow="false">
              <template #trigger>
                <n-button secondary>
                  页面资源
                  <template #icon>
                    <NIcon><FolderOpenOutline /></NIcon>
                  </template>
                </n-button>
              </template>
              <div class="application-form-assets-popover">
                <div class="application-form-assets-popover-head">
                  <div>
                    <strong>页面表单资产</strong>
                    <small>仅用于独立录入和内容页</small>
                  </div>
                  <n-button size="tiny" type="primary" @click="createStandaloneFormAsset">
                    新建表单
                  </n-button>
                </div>
                <button
                  v-for="asset in formAssets"
                  :key="asset.id"
                  type="button"
                  class="application-form-asset-row"
                  @click="openFormAssetDesigner(asset.id)"
                >
                  <span>
                    <strong>{{ asset.name }}</strong>
                    <small>{{ resolveFormAssetFields(asset).length }} 个字段</small>
                  </span>
                  <span>编辑</span>
                </button>
                <n-empty v-if="!formAssets.length" size="small" description="还没有页面表单，先创建一个" />
              </div>
            </n-popover>
            <n-button v-if="editing" :disabled="!dirty" :loading="saving" secondary @click="saveDraft">
              <template #icon>
                <NIcon><SaveOutline /></NIcon>
              </template>
              保存草稿
            </n-button>
            <n-button v-if="editing" secondary @click="openDraftPreview">
              <template #icon>
                <NIcon><EyeOutline /></NIcon>
              </template>
              预览草稿
            </n-button>
            <n-button v-if="editing" type="primary" @click="openPublishPanel">
              <template #icon>
                <NIcon><RocketOutline /></NIcon>
              </template>
              发布应用
            </n-button>
            <n-dropdown v-if="editing" trigger="click" placement="bottom-end" :options="runtimeHeaderMoreOptions" @select="handleRuntimeHeaderMoreSelect">
              <n-button secondary circle title="更多操作" aria-label="更多操作">
                <template #icon>
                  <NIcon><EllipsisHorizontalOutline /></NIcon>
                </template>
              </n-button>
            </n-dropdown>
          </n-space>
        </header>

        <section v-if="formDesignerMode" class="application-form-asset-workbench">
          <header class="application-form-asset-head">
            <div>
              <span class="application-form-asset-crumb">{{ application.applicationName }} / 页面资源 / 表单资产</span>
              <h1>{{ activeFormAsset?.name || '未命名表单' }}</h1>
              <p>用于独立录入和内容页；业务对象的字段、表单和列表请在“业务对象设计”中维护。</p>
            </div>
            <n-space size="small">
              <n-button secondary @click="returnToPageDesigner">
                返回页面
              </n-button>
              <n-button type="primary" :disabled="!dirty" :loading="saving" @click="saveDraft">
                保存草稿
              </n-button>
            </n-space>
          </header>
          <div v-if="activeFormAsset" class="application-form-asset-designer">
            <ForgeFormDesigner
              :model-value="activeFormDesignerSchema"
              :fields="activeFormFields"
              :object-code="application.applicationCode"
              :object-name="activeFormAsset.name"
              @update:model-value="updateActiveFormDesignerSchema"
            />
          </div>
          <n-empty v-else description="表单不存在或已被删除" />
        </section>

        <div v-else class="runtime-body" :class="{ 'configuring': editing && configPanelVisible, 'sidebar-collapsed': sidebarCollapsed, 'headerless': !editing }">
          <aside class="runtime-navigation base-app-sidebar__vertical no-page-group" :class="{ collapsed: sidebarCollapsed }">
            <div class="title_wrapper">
              <div class="application-sidebar-title base-app-sidebar__title_bar">
                <div class="base-app-title-wrapper">
                  <span class="application-icon-slot" aria-hidden="true">
                    <AuthImage :src="tenantStore.systemLogo" :fallback="defaultLogo" alt="" />
                  </span>
                  <strong class="base-app-title-content">{{ application.applicationName }}</strong>
                </div>
                <div class="sidebar-title-actions">
                  <button v-if="canEditApplication && !editing" class="sidebar-edit-trigger" type="button" aria-label="编辑应用" title="编辑应用" @click="editing = true">
                    <NIcon><CreateOutline /></NIcon>
                  </button>
                  <button class="sidebar-collapse-hint" type="button" :aria-label="sidebarCollapsed ? '展开页面菜单' : '收起页面菜单'" :title="sidebarCollapsed ? '展开页面菜单' : '收起页面菜单'" @click="sidebarCollapsed = !sidebarCollapsed">
                    {{ sidebarCollapsed ? '»' : '«' }}
                  </button>
                </div>
              </div>
            </div>
            <div class="navigation-list scroll_wrapper">
              <div class="list_wrapper base-app-sidebar__list_vertical">
                <template v-for="item in navigationNodes" :key="item.id">
                  <div class="navigation-row base-app-sidebar__node_vertical" :class="{ 'base-app-sidebar__node_selected': item.id === selectedNodeId }" :style="{ paddingLeft: `${12 + item.depth * 16}px` }">
                    <button
                      v-if="item.type === 'page'"
                      class="navigation-page"
                      :class="{ active: item.id === selectedNodeId }"
                      type="button"
                      @click="selectNode(item.id)"
                    >
                      <span v-if="item.icon" class="navigation-icon-slot" aria-hidden="true">
                        <IconRenderer v-if="item.icon" :icon="item.icon" :size="16" />
                      </span>
                      <span>{{ item.title }}</span>
                    </button>
                    <span v-else class="navigation-group">
                      {{ item.title }}
                    </span>
                    <n-dropdown
                      v-if="editing"
                      trigger="click"
                      placement="bottom-end"
                      :options="resolveNavigationMoreOptions(item)"
                      @select="key => handleNavigationMoreSelect(key, item)"
                    >
                      <button type="button" class="navigation-more" :aria-label="`${item.title}更多操作`" title="更多操作" @click.stop>
                        <span aria-hidden="true">•••</span>
                      </button>
                    </n-dropdown>
                  </div>
                </template>
              </div>
            </div>
            <div v-if="editing" class="new_node_wrapper">
              <n-popover v-model:show="newNodePopoverVisible" trigger="click" placement="right-end" :show-arrow="false">
                <template #trigger>
                  <button type="button" class="navigation-create base-app-sidebar__new_node_vertical">
                    <span>+</span>新建
                  </button>
                </template>
                <div class="new-node-popover">
                  <button type="button" class="new-node-choice" @click="createQuickNode('page')">
                    <span class="new-node-choice-icon"><NIcon><DocumentTextOutline /></NIcon></span>
                    <span><strong>新建页面</strong><small>创建空白页后按需添加组件</small></span>
                  </button>
                  <button type="button" class="new-node-choice" @click="createQuickNode('group')">
                    <span class="new-node-choice-icon group"><NIcon><FolderOpenOutline /></NIcon></span>
                    <span><strong>新建页面组</strong><small>用于归类多个页面</small></span>
                  </button>
                </div>
              </n-popover>
            </div>
            <div v-if="iconPickerVisible" class="navigation-icon-picker">
              <div class="navigation-icon-picker-head">
                <span>选择图标</span>
                <button type="button" aria-label="关闭图标选择" @click="iconPickerVisible = false">
                  ×
                </button>
              </div>
              <IconSelector v-model="navigationIconValue" />
            </div>
          </aside>

          <main class="runtime-main">
            <section v-if="!currentNode" class="application-empty-state">
              <div class="application-empty-intro">
                <div>
                  <span class="application-empty-eyebrow">应用页面设计</span>
                  <h1>{{ application.applicationName || '未命名应用' }}</h1>
                  <p>从一个页面模板开始搭建，也可以从下方常用组件直接起步。</p>
                </div>
                <button v-if="editing" type="button" class="application-create-group-card" @click="createQuickNode('group')">
                  <span class="application-create-group-icon" aria-hidden="true"><NIcon><FolderOpenOutline /></NIcon></span>
                  <span>
                    <strong>新建页面组</strong>
                    <small>用于组织多个页面</small>
                  </span>
                  <i aria-hidden="true">→</i>
                </button>
              </div>
              <section v-if="editing" class="application-template-section" aria-label="页面模板">
                <div class="application-empty-section-head">
                  <span class="application-section-kicker"><NIcon><AppsOutline /></NIcon>页面模板</span>
                  <span>选择后立即创建</span>
                </div>
                <div class="application-template-grid">
                  <button
                    v-for="template in pageTemplateOptions"
                    :key="template.key"
                    type="button"
                    class="application-template-card"
                    :class="{ selected: selectedPageTemplateKey === template.key }"
                    @click="selectIntroTemplate(template.key)"
                  >
                    <span class="application-template-icon" :class="`kind-${template.key}`" aria-hidden="true">
                      <NIcon><component :is="resolvePageTemplateIcon(template)" /></NIcon>
                    </span>
                    <span>
                      <strong>{{ template.label }}</strong>
                      <small>{{ template.description }}</small>
                      <em>立即创建 <i aria-hidden="true">→</i></em>
                    </span>
                  </button>
                </div>
                <div v-if="selectedPageTemplate.requiresObject" class="application-template-object-binding">
                  <template v-if="pageTemplateObjectOptions.length">
                    <span>为“{{ selectedPageTemplate.label }}”选择业务对象</span>
                    <n-select v-model:value="selectedPageTemplateObjectId" size="small" :options="pageTemplateObjectOptions" filterable placeholder="选择已有业务对象" />
                    <n-button type="primary" size="small" :disabled="!selectedPageTemplateObjectId" @click="createPageFromTemplate(selectedPageTemplate.key)">
                      创建页面
                    </n-button>
                  </template>
                  <template v-else>
                    <span class="application-template-object-empty-icon" aria-hidden="true"><NIcon><CubeOutline /></NIcon></span>
                    <span class="application-template-object-empty-copy">
                      <strong>还没有可用的业务对象</strong>
                      <small>先创建业务对象并配置字段，回来后即可生成“{{ selectedPageTemplate.label }}”页面。</small>
                    </span>
                    <n-button type="primary" size="small" @click="openBusinessObjectDesign">
                      创建业务对象
                    </n-button>
                  </template>
                </div>
              </section>
              <section v-if="editing" class="application-component-section" aria-label="常用组件">
                <div class="application-empty-section-head">
                  <span class="application-section-kicker"><NIcon><AddOutline /></NIcon>常用组件</span>
                  <span>创建空白页并直接放入组件</span>
                </div>
                <div class="application-component-grid">
                  <button v-for="item in recommendedComponents" :key="item.blockType" type="button" @click="createPageFromTemplate('blank', item.blockType)">
                    <span class="empty-component-icon" :class="`kind-${resolveComponentPickerGroup(item)}`" aria-hidden="true">
                      <NIcon><component :is="resolveEmptyGuideIcon(item)" /></NIcon>
                    </span>
                    <span>{{ item.title }}</span>
                  </button>
                </div>
              </section>
              <span v-else class="application-empty-readonly">页面尚未配置</span>
            </section>
            <section v-else class="page-surface">
              <section v-if="currentNode.pageType === 'object'" class="object-page-card">
                <strong>{{ currentNode.objectRef?.objectCode || '未绑定业务对象' }}</strong>
                <p>{{ currentNode.objectRef?.valid === false ? '绑定的业务对象已不可用，请重新选择。' : '该页面复用已有对象的列表、表单、详情和 CRUD 运行配置。' }}</p>
                <n-space v-if="editing && currentNode.objectRef?.objectCode" size="small">
                  <n-button secondary @click="openObjectDesigner('fields')">
                    配置字段
                  </n-button>
                  <n-button type="primary" secondary @click="openObjectDesigner('list')">
                    配置列表
                  </n-button>
                  <n-button secondary @click="openObjectDesigner('form')">
                    配置表单
                  </n-button>
                  <n-button secondary @click="openObjectDesigner('detail')">
                    配置详情
                  </n-button>
                  <n-button secondary @click="openObjectDesigner('flow')">
                    配置流程
                  </n-button>
                </n-space>
              </section>

              <div v-if="editing" class="canvas-component-anchor" :class="{ 'moving': componentButtonMoveCtx, 'is-default-position': !hasCustomComponentButtonPosition }" :style="componentButtonStyle" @pointerdown.capture="startComponentButtonMove">
                <n-popover v-model:show="componentPopoverVisible" trigger="click" placement="top-start" :show-arrow="false">
                  <template #trigger>
                    <button type="button" class="component-add-trigger" aria-label="添加组件" title="添加组件">
                      <span class="component-add-icon" aria-hidden="true">+</span>
                      <span class="component-add-label">添加组件</span>
                    </button>
                  </template>
                  <div class="component-popover">
                    <n-input v-model:value="componentKeyword" clearable size="small" placeholder="搜索组件" class="component-search-input" />
                    <div v-if="componentPickerGroups.length" class="component-picker-groups">
                      <section v-for="group in componentPickerGroups" :key="group.key" class="component-picker-group">
                        <h3>{{ group.label }}</h3>
                        <div class="component-picker-grid">
                          <button
                            v-for="item in group.items"
                            :key="item.blockType"
                            type="button"
                            :draggable="editing"
                            @dragstart="handleComponentCatalogDragStart($event, item)"
                            @click="insertComponent(item)"
                          >
                            <span class="component-icon-slot" :class="`kind-${group.key}`" aria-hidden="true">
                              <img v-if="resolveComponentIcon(item)" :src="resolveComponentIcon(item)" alt="">
                              <svg v-else-if="group.key === 'list'" viewBox="0 0 24 24"><path d="M7 6h11M7 12h11M7 18h11M3.5 6h.01M3.5 12h.01M3.5 18h.01" /></svg>
                              <svg v-else-if="group.key === 'chart'" viewBox="0 0 24 24"><path d="M4 19V5m0 14h16M8 16v-4m4 4V8m4 8V6" /></svg>
                              <svg v-else-if="group.key === 'view'" viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="14" rx="2" /><path d="M4 9h16M8 13h8" /></svg>
                              <svg v-else viewBox="0 0 24 24"><path d="M12 4v16M4 12h16" /><circle cx="12" cy="12" r="7" /></svg>
                            </span>
                            <span class="component-item-title">{{ item.title }}</span>
                          </button>
                        </div>
                      </section>
                    </div>
                    <n-empty v-else size="small" description="没有匹配的组件" />
                  </div>
                </n-popover>
              </div>

              <div class="application-grid-host">
                <draggable
                  :model-value="pageBlocks"
                  item-key="id"
                  handle=".page-block-drag-handle"
                  class="application-page-flow"
                  :style="{ minHeight: pageBlocks.length ? `${pageFlowHeight}px` : '100%' }"
                  :disabled="true"
                  :animation="180"
                  :force-fallback="true"
                  fallback-class="page-block-drag-shadow"
                  :fallback-on-body="true"
                  :fallback-tolerance="2"
                  ghost-class="page-block-ghost"
                  chosen-class="page-block-chosen"
                  @dragover.prevent
                  @drop="handlePageDrop"
                  @update:model-value="updatePageBlocks"
                >
                  <template #item="{ element: block }">
                    <section
                      class="application-page-block"
                      :class="{ selected: selectedPageBlockId === block.id, editing, dragging: draggingPageBlockId === block.id }"
                      :style="resolvePageBlockShellStyle(block)"
                      :data-page-block-id="block.id"
                      @click.stop="selectPageBlock(block.id); openPageBlockConfiguration(block)"
                    >
                      <div v-if="editing" class="page-block-node-overlay">
                        <span
                          class="page-block-drag-handle"
                          title="拖动区块"
                          @pointerdown.stop="startPageBlockMove(block, $event)"
                          @click.stop
                        >
                          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                            <path d="M8.25 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm0 7.25a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Zm1.75 5.5a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 0 3.5Z M14.753 6.5a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5ZM16.5 12a1.75 1.75 0 1 1-3.5 0 1.75 1.75 0 0 1 3.5 0Zm-1.747 9a1.75 1.75 0 1 0 0-3.5 1.75 1.75 0 0 0 0 3.5Z" fill="currentColor" />
                          </svg>
                        </span>
                        <n-dropdown
                          trigger="click"
                          placement="bottom-end"
                          :options="resolvePageBlockMoreOptions(block)"
                          @select="key => handlePageBlockMoreSelect(key, block)"
                        >
                          <button
                            type="button"
                            class="page-block-menu-trigger"
                            title="更多操作"
                            aria-label="更多操作"
                            @click.stop
                            @mousedown.stop
                          >
                            <svg width="1em" height="1em" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                              <circle cx="256" cy="256" r="32" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="32" />
                              <circle cx="416" cy="256" r="32" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="32" />
                              <circle cx="96" cy="256" r="32" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="32" />
                            </svg>
                          </button>
                        </n-dropdown>
                        <div v-if="backgroundPickerBlockId === block.id && blockBackgroundPickerVisible" class="page-block-color-picker page-block-color-picker-floating" @click.stop>
                          <div class="page-block-color-picker-head">
                            <button type="button" class="page-block-color-picker-reset" @click="updatePageBlockBackgroundColor(block, 'transparent')">
                              恢复默认
                            </button>
                            <button type="button" aria-label="关闭颜色选择器" @click="blockBackgroundPickerVisible = false">
                              ×
                            </button>
                          </div>
                          <div class="page-block-color-presets" aria-label="推荐颜色">
                            <button type="button" class="transparent" title="透明" @click="updatePageBlockBackgroundColor(block, 'transparent')" />
                            <button v-for="color in pageBlockRecommendedColors" :key="color" type="button" :style="{ background: color }" @click="updatePageBlockBackgroundColor(block, color)" />
                          </div>
                          <n-color-picker
                            :value="resolvePageBlockBackgroundColor(block)"
                            :show-alpha="true"
                            :modes="['hex']"
                            @update:value="updatePageBlockBackgroundColor(block, $event)"
                          />
                          <button type="button" class="page-block-color-picker-transparent" @click="updatePageBlockBackgroundColor(block, 'transparent')">
                            设为透明
                          </button>
                        </div>
                      </div>
                      <template v-if="editing && selectedPageBlockId === block.id">
                        <button
                          v-for="anchor in pageBlockResizeAnchors"
                          :key="anchor"
                          type="button"
                          class="page-block-resize-anchor"
                          :class="`anchor-${anchor}`"
                          title="调整组件大小"
                          @pointerdown.stop="startPageBlockResize(block, $event, anchor)"
                        />
                      </template>
                      <GridBlockRenderer
                        :block="resolvePagePreviewBlock(block)"
                        :fields="resolvePageBlockFields(block)"
                        :runtime-crud-props="resolvePageBlockRuntimeCrudProps(block)"
                        :selected="false"
                        :inline-text-editing="editing"
                        readonly
                        @block-activate="selectPageBlock"
                        @inline-text-update="handleInlineTextUpdate"
                      />
                    </section>
                  </template>
                </draggable>
                <span
                  v-if="dragPreview"
                  class="page-block-move-shadow page-block-drag-preview-shadow"
                  :style="{ left: `${dragPreview.x}px`, top: `${dragPreview.y}px`, width: `${dragPreview.width}px`, height: `${dragPreview.height}px` }"
                  aria-hidden="true"
                />
                <section
                  v-if="dragPreview && dragPreviewBlock"
                  class="application-page-block page-block-drag-preview"
                  :style="{ left: `${dragPreview.x}px`, top: `${dragPreview.y}px`, width: `${dragPreview.width}px`, height: `${dragPreview.height}px` }"
                  aria-hidden="true"
                >
                  <GridBlockRenderer
                    :block="resolvePagePreviewBlock(dragPreviewBlock)"
                    :fields="resolvePageBlockFields(dragPreviewBlock)"
                    :runtime-crud-props="resolvePageBlockRuntimeCrudProps(dragPreviewBlock)"
                    :selected="false"
                    readonly
                  />
                </section>
                <section v-if="editing && !pageBlocks.length" class="grid-empty-guide">
                  <div class="empty-guide-copy">
                    <span class="empty-guide-eyebrow">页面搭建</span>
                    <h2>从一个组件开始</h2>
                    <p>选择常用组件，页面会立刻呈现最终效果；后续仍可自由拖动、调整尺寸和配置数据。</p>
                  </div>
                  <div class="page-recommendations">
                    <button v-for="item in recommendedComponents" :key="item.blockType" type="button" @click="appendPageBlock(item.blockType)">
                      <span class="empty-component-icon" :class="`kind-${resolveComponentPickerGroup(item)}`" aria-hidden="true">
                        <NIcon><component :is="resolveEmptyGuideIcon(item)" /></NIcon>
                      </span>
                      <span>{{ item.title }}</span>
                    </button>
                  </div>
                  <div class="empty-guide-preview" aria-hidden="true">
                    <div class="empty-guide-page-sheet">
                      <div class="empty-guide-sheet-head">
                        <i />
                        <span />
                        <em />
                      </div>
                      <div class="empty-guide-sheet-title">
                        <b />
                        <span />
                      </div>
                      <div class="empty-guide-sheet-metrics">
                        <i /><i /><i />
                      </div>
                      <div class="empty-guide-sheet-content">
                        <div class="empty-guide-sheet-list">
                          <i /><i /><i /><i />
                        </div>
                        <div class="empty-guide-sheet-chart">
                          <i /><i /><i /><i /><i />
                        </div>
                      </div>
                    </div>
                    <span class="empty-guide-float-card float-list"><NIcon><ListOutline /></NIcon></span>
                    <span class="empty-guide-float-card float-chart"><NIcon><BarChartOutline /></NIcon></span>
                    <span class="empty-guide-float-card float-filter"><NIcon><FunnelOutline /></NIcon></span>
                  </div>
                </section>
              </div>
            </section>
          </main>
          <aside v-if="editing && configPanelVisible" class="runtime-inspector">
            <div class="runtime-inspector-head">
              <div class="runtime-inspector-tabs" role="tablist" aria-label="组件配置类型">
                <button type="button" :class="{ active: inspectorTab === 'properties' }" role="tab" :aria-selected="inspectorTab === 'properties'" @click="inspectorTab = 'properties'">
                  <NIcon><SettingsOutline /></NIcon>属性
                </button>
                <button type="button" :class="{ active: inspectorTab === 'data' }" role="tab" :aria-selected="inspectorTab === 'data'" @click="inspectorTab = 'data'">
                  <NIcon><FolderOpenOutline /></NIcon>数据
                </button>
              </div>
              <button type="button" class="runtime-inspector-close" aria-label="收起配置面板" title="收起配置面板" @click="configPanelVisible = false">
                ×
              </button>
            </div>
            <div v-if="selectedPageBlock && inspectorTab === 'data'" class="application-form-source-config">
              <div class="application-form-source-head">
                <strong>{{ supportsFormAsset(selectedPageBlock) ? '数据表单' : '组件数据' }}</strong>
                <span>{{ selectedPageBlockUsesObjectRuntime ? '列表与新增弹窗共用当前业务对象的表单' : supportsFormAsset(selectedPageBlock) ? '选择此页面组件要引用的独立页面表单' : selectedPageBlock.label }}</span>
              </div>
              <template v-if="selectedPageBlockUsesObjectRuntime">
                <div class="runtime-form-selector">
                  <n-select
                    size="small"
                    :value="selectedPageBlockRuntimeObjectId"
                    :options="runtimeObjectFormOptions"
                    @update:value="updateSelectedPageBlockRuntimeObject"
                  />
                  <n-button quaternary size="small" title="编辑当前表单" aria-label="编辑当前表单" @click="openObjectDesigner('form', selectedPageBlockRuntimeObjectRef)">
                    <template #icon>
                      <NIcon><CreateOutline /></NIcon>
                    </template>
                  </n-button>
                </div>
              </template>
              <template v-else-if="supportsFormAsset(selectedPageBlock)">
                <n-popover v-model:show="formAssetSelectorOpen" trigger="click" placement="bottom-start" :show-arrow="false" :to="false">
                  <template #trigger>
                    <button type="button" class="form-asset-selector-trigger" :class="{ active: formAssetSelectorOpen }">
                      <NIcon><FolderOpenOutline /></NIcon>
                      <span>{{ selectedPageBlockFormAsset?.name || '选择已经设计好的表单' }}</span>
                      <span v-if="selectedPageBlockFormAssetId" class="form-asset-selector-open" title="编辑表单" role="button" tabindex="0" @click.stop="editSelectedBlockFormAsset">↗</span>
                      <span class="form-asset-selector-arrow" aria-hidden="true">{{ formAssetSelectorOpen ? '⌃' : '⌄' }}</span>
                    </button>
                  </template>
                  <div class="form-asset-selector-menu">
                    <n-input v-model:value="formAssetSelectorKeyword" clearable size="small" placeholder="搜索">
                      <template #prefix>
                        ⌕
                      </template>
                    </n-input>
                    <button
                      v-for="asset in filteredFormAssets"
                      :key="asset.id"
                      type="button"
                      class="form-asset-selector-option"
                      :class="{ selected: asset.id === selectedPageBlockFormAssetId }"
                      @click="selectFormAssetFromPicker(asset.id)"
                    >
                      <NIcon><FolderOpenOutline /></NIcon>
                      <span>{{ asset.name }}</span>
                      <span v-if="asset.id === selectedPageBlockFormAssetId" class="form-asset-selector-check">✓</span>
                    </button>
                    <n-empty v-if="!filteredFormAssets.length" size="small" description="没有匹配的表单" />
                  </div>
                </n-popover>
                <p v-if="formAssets.length === 1" class="form-asset-default-hint">
                  当前应用只有一个表单，已自动关联。
                </p>
                <div class="form-asset-actions">
                  <n-button size="tiny" type="primary" secondary @click="createFormAssetForSelectedBlock">
                    新建表单
                  </n-button>
                  <n-button size="tiny" :disabled="!selectedPageBlockFormAssetId" @click="editSelectedBlockFormAsset">
                    编辑表单
                  </n-button>
                </div>
              </template>
              <n-empty v-else size="small" description="该组件没有可绑定的数据表单" />
            </div>
            <ListPageGridDesigner
              v-if="inspectorTab === 'properties'"
              panel-only
              :model-value="currentGridLayout"
              :model-schema="applicationGridModelSchema"
              :fields="selectedPageBlockFields"
              :active-block-id="selectedPageBlockId"
              @update:model-value="updateCurrentGridLayout"
            />
          </aside>
        </div>
      </template>
      <DesignerAsyncLoader
        v-else-if="loading"
        title="正在加载应用页面"
        description="正在准备页面、表单和组件配置"
      />
    </n-spin>

    <n-modal v-model:show="navigationActionVisible" preset="card" :title="navigationActionTitle" style="width: 420px">
      <n-form label-placement="top">
        <n-form-item v-if="navigationActionMode === 'rename'" label="名称">
          <n-input v-model:value="navigationActionForm.title" maxlength="40" show-count />
        </n-form-item>
        <n-form-item v-if="navigationActionMode === 'icon'" label="图标">
          <IconSelector v-model:value="navigationActionForm.icon" />
        </n-form-item>
        <n-form-item v-if="navigationActionMode === 'move'" label="移动到页面组">
          <n-select v-model:value="navigationActionForm.parentId" clearable :options="moveGroupOptions" placeholder="顶级菜单" />
        </n-form-item>
        <template v-if="navigationActionMode === 'delete'">
          <p class="navigation-action-tip">
            {{ navigationActionHasChildren ? '该页面组含有子项，请选择子项处理方式。' : '删除后无法恢复，请确认。' }}
          </p>
          <n-form-item v-if="navigationActionHasChildren" label="子项处理">
            <n-radio-group v-model:value="navigationActionForm.deleteStrategy">
              <n-radio value="delete-children">
                同时删除子项
              </n-radio>
              <n-radio value="move-children">
                移动到指定页面组
              </n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item v-if="navigationActionHasChildren && navigationActionForm.deleteStrategy === 'move-children'" label="目标页面组">
            <n-select v-model:value="navigationActionForm.targetParentId" clearable :options="moveGroupOptions" placeholder="顶级菜单" />
          </n-form-item>
        </template>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="navigationActionVisible = false">
            取消
          </n-button>
          <n-button :type="navigationActionMode === 'delete' ? 'error' : 'primary'" @click="confirmNavigationAction">
            确认
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="copyBlockVisible" preset="card" title="复制组件到其他页面" style="width: 420px">
      <n-form label-placement="top">
        <n-form-item label="目标页面">
          <n-select v-model:value="copyBlockTargetPageId" :options="copyBlockPageOptions" filterable placeholder="选择要复制到的页面" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="copyBlockVisible = false">
            取消
          </n-button>
          <n-button type="primary" :disabled="!copyBlockTargetPageId" @click="copySelectedBlockToOtherPage">
            复制
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="exitEditingVisible" preset="dialog" title="退出编辑">
      尚有未保存的页面、导航或组件调整。退出后将丢失这些修改。
      <template #action>
        <n-space justify="end">
          <n-button @click="exitEditingVisible = false">
            继续编辑
          </n-button>
          <n-button type="error" @click="discardAndExitEditing">
            放弃修改并退出
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { AddOutline, AppsOutline, ArrowBackOutline, ArrowDownOutline, ArrowRedoOutline, ArrowUndoOutline, ArrowUpOutline, BarChartOutline, ColorFillOutline, CopyOutline, CreateOutline, CubeOutline, DocumentTextOutline, DuplicateOutline, EllipsisHorizontalOutline, ExpandOutline, EyeOutline, FolderOpenOutline, FunnelOutline, GitBranchOutline, InformationCircleOutline, ListOutline, MoveOutline, ReaderOutline, RemoveOutline, ResizeOutline, RocketOutline, SaveOutline, SettingsOutline, SquareOutline, StatsChartOutline, SwapHorizontalOutline, TextOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon, useMessage } from 'naive-ui'
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import draggable from 'vuedraggable'
import { crudConfigRender } from '@/api/ai'
import { businessObjectRuntimeInfo } from '@/api/business-app'
import { businessApplicationWorkspaceByCode, updateBusinessApplication } from '@/api/business-application'
import defaultLogo from '@/assets/images/logo.png'
import AuthImage from '@/components/common/AuthImage.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import IconSelector from '@/components/IconSelector.vue'
import GridBlockRenderer from '@/components/lowcode-builder/page/GridBlockRenderer.vue'
import ListPageGridDesigner from '@/components/lowcode-builder/page/ListPageGridDesigner.vue'
import { createGridBlock, listPageBlockCatalog, resolveListPageBlockMeta } from '@/components/lowcode-builder/page/page-schema'
import { buildRuntimeCrudProps } from '@/components/lowcode-builder/shared/runtime-crud-props'
import { useTenantStore, useUserStore } from '@/store'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'
import ForgeFormDesigner from '@/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue'
import { buildAutoFieldAssets, createFieldFromComponent } from '@/views/app-center/components/designer/form-first/autoFieldRegistry'
import { createDefaultFormDesignerSchema, isFieldComponent, normalizeFormDesignerSchema } from '@/views/app-center/components/designer/form-first/formDesignerSchema'
import {
  createInAppFormAsset,
  createNavigationNode,
  mergeInAppBuilderOptions,
  moveNavigationNode,
  normalizeInAppBuilder,
  removeNavigationNode,
  updateInAppFormAsset,
} from './in-app-builder/in-app-builder-schema'
import { inAppPageTemplateCatalog, resolveInAppPageTemplate } from './in-app-builder/page-template-catalog'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const tenantStore = useTenantStore()
const formComponentIconModules = import.meta.glob('/src/assets/images/form/*.png', { eager: true, import: 'default' })
const formComponentIconFileByBlockType = {
  'search-form': 'chaxunbiaodan',
  'toolbar': 'caozuogongjulan',
  'back-button': 'fanhuishangyiye',
  'page-title': 'yemianbiaoti',
  'grid-layout': 'shangebuju',
  'detail-info': 'xiangqingxinxi',
  'AiCrudPage': 'crud',
  'AiTable': 'zhinengbiaoge',
  'AiForm': 'zhinengbiaodan',
  'data-table': 'shujuliebiao',
  'tree-panel': 'shaixuanshu',
  'stats-strip': 'zhibiaokapian',
  'info-panel': 'tishimianban',
  'custom-html': 'shuomingwenben',
  'action-button': 'button',
  'button-group': 'buttongroup',
  'tag-list': 'biaoqianliebiao',
  'steps': 'buzhoutiao',
  'timeline': 'shijianxian',
  'empty-state': 'kognzhuangtai',
  'card': 'kapianrongqi',
  'tabs': 'tabs',
  'divider': 'fengexian',
  'spacer': 'liubaizhanwei',
  'signature-pad': 'qianming',
  'step-form': 'fenbubiaodan',
  'text-title': 'biaoti',
  'paragraph': 'duanluo',
  'statistic': 'tongjishuzhi',
  'link': 'lianjie',
  'text-tip': 'wenzitishi',
  'audio-player': 'yinpinbofangqi',
  'video-player': 'shipinbofangqi',
  'avatar': 'touxiang',
  'iframe': 'neiqianyemian',
  'box-layout': 'gezibuju',
  'space': 'jianju',
  'sub-table-tabs': 'zibiaotab',
  'section-divider': 'fenzubiaoti',
  'transfer': 'chuansuokuang',
  'watermark': 'shuiyin',
  'vue-component': 'vue',
  'markdown': 'md',
  'barcode': 'tiaoxingma',
  'qrcode': 'erweima',
  'calendar': 'rili',
  'code': 'daima',
  'countdown': 'daojishi',
  'descriptions': 'miaoshu',
  'announcement': 'gognshi',
  'list': 'shujuliebiao',
  'log': 'log',
  'number-animation': 'shuzidonghua',
  'breadcrumb': 'mianbaoxie',
  'menu': 'caidan',
  'pagination': 'fenye',
}
const userStore = useUserStore()
const application = ref(null)
const objects = ref([])
const builder = ref(null)
const loading = ref(false)
const saving = ref(false)
const editing = ref(route.query.edit === '1')
const exitEditingVisible = ref(false)
const selectedNodeId = ref('')
const newNodePopoverVisible = ref(false)
const selectedPageTemplateKey = ref('blank')
const selectedPageTemplateObjectId = ref(null)
const iconPickerVisible = ref(false)
const iconPickerNodeId = ref('')
const navigationActionVisible = ref(false)
const navigationActionMode = ref('')
const navigationActionNodeId = ref('')
const navigationActionForm = ref({ title: '', icon: '', parentId: null, deleteStrategy: 'delete-children', targetParentId: null })
const componentPopoverVisible = ref(false)
const componentKeyword = ref('')
const savedSignature = ref('')
const undoStack = ref([])
const redoStack = ref([])
const historyReady = ref(false)
let latestBuilderSnapshot = null
const HISTORY_LIMIT = 50
const selectedPageBlockId = ref('')
const draggingPageBlockId = ref('')
const dragPreview = ref(null)
const configPanelVisible = ref(false)
const inspectorTab = ref('properties')
const componentButtonPosition = ref({ x: null, y: null })
const componentButtonMoveCtx = ref(null)
const runtimeCrudPropsByObjectId = ref({})
const runtimeCrudLoadingObjectIds = new Set()
const runtimeCrudUnavailableObjectIds = new Set()
const formDesignerMode = ref(false)
const activeFormAssetId = ref('')
const sidebarCollapsed = ref(false)
const copyBlockVisible = ref(false)
const copyBlockId = ref('')
const copyBlockTargetPageId = ref('')
const blockBackgroundPickerVisible = ref(false)
const backgroundPickerBlockId = ref('')
const formAssetSelectorOpen = ref(false)
const formAssetSelectorKeyword = ref('')

const componentPickerGroupOptions = [
  { key: 'list', label: '列表' },
  { key: 'chart', label: '图表' },
  { key: 'view', label: '视图' },
  { key: 'other', label: '其他' },
]
const runtimeHeaderMoreOptions = computed(() => [
  { label: '设计业务对象', key: 'object-design', icon: () => renderNavigationMenuIcon(ReaderOutline) },
  { label: '预览草稿', key: 'preview-draft', icon: () => renderNavigationMenuIcon(EyeOutline) },
  { type: 'divider', key: 'header-more-divider' },
  { label: '退出编辑', key: 'exit-editing', icon: () => renderNavigationMenuIcon(ArrowBackOutline) },
])
const legacyBlockTypeMap = { 'intro': 'page-title', 'metric-card': 'stats-strip', 'business-list': 'AiCrudPage', 'business-form': 'AiForm', 'todo': 'info-panel', 'chart': 'stats-strip', 'text': 'custom-html', 'image': 'info-panel', 'columns': 'grid-layout', 'divider': 'divider' }
const pageBlockResizeAnchors = ['top-left', 'top', 'top-right', 'right', 'bottom-right', 'bottom', 'bottom-left', 'left']
const pageBlockRecommendedColors = ['#3370ff', '#8b5cf6', '#14b8a6', '#f59e0b', '#f97316', '#ef4444', '#4e5969', '#edf4ff', '#f3efff', '#e6fffb', '#fff7e6', '#fff1f0', '#f2f3f5']

const groupOptions = computed(() => (builder.value?.nodes || []).filter(item => item.type === 'group').map(item => ({ label: item.title, value: item.id })))
const navigationActionNode = computed(() => builder.value?.nodes.find(item => item.id === navigationActionNodeId.value) || null)
const iconPickerNode = computed(() => builder.value?.nodes.find(item => item.id === iconPickerNodeId.value) || null)
const navigationIconValue = computed({
  get: () => iconPickerNode.value?.icon || '',
  set: (icon) => {
    if (!iconPickerNode.value)
      return
    builder.value = {
      ...builder.value,
      nodes: builder.value.nodes.map(item => item.id === iconPickerNode.value.id ? { ...item, icon } : item),
    }
    iconPickerVisible.value = false
  },
})
const navigationActionTitle = computed(() => ({ rename: '重命名', move: '移动到', delete: '删除页面或页面组' }[navigationActionMode.value] || '页面操作'))
const navigationActionHasChildren = computed(() => navigationActionNode.value?.type === 'group' && builder.value?.nodes.some(item => item.parentId === navigationActionNode.value.id))
const moveGroupOptions = computed(() => groupOptions.value.filter(item => item.value !== navigationActionNodeId.value && !isNavigationGroupDescendant(item.value, navigationActionNodeId.value)))
const navigationNodes = computed(() => flattenNodes(builder.value?.nodes || []))
const currentNode = computed(() => builder.value?.nodes.find(item => item.id === selectedNodeId.value) || builder.value?.nodes.find(item => item.id === builder.value?.homePageId) || null)
const currentPage = computed(() => currentNode.value ? builder.value?.pages[currentNode.value.id] : null)
const currentGridLayout = computed(() => {
  const layout = currentPage.value?.layout || {}
  if (layout.gridLayout && Array.isArray(layout.gridLayout.items))
    return layout.gridLayout
  return {
    cols: 12,
    rowHeight: 32,
    gap: 8,
    designWidth: 1366,
    layoutType: 'simple-crud',
    items: (layout.items || []).map((item, index) => createLegacyBlock(item, index)).filter(Boolean),
  }
})
const pageBlocks = computed(() => currentGridLayout.value.items || [])
const pageFlowHeight = computed(() => pageBlocks.value.reduce((bottom, block, index) => {
  const y = Number(block.props?.style?.pageFlowY)
  const height = Number(block.props?.style?.pageFlowHeight)
  const top = Number.isFinite(y) && y >= 0 ? y : resolveDefaultPageBlockY(block, index)
  return Math.max(bottom, top + (height > 0 ? height : resolveDefaultPageBlockHeight(block)) + 36)
}, 680))
const hasCustomComponentButtonPosition = computed(() => Number.isFinite(componentButtonPosition.value.x) && Number.isFinite(componentButtonPosition.value.y))
const componentButtonStyle = computed(() => ({
  left: `${componentButtonPosition.value.x ?? 20}px`,
  ...(hasCustomComponentButtonPosition.value
    ? { top: `${componentButtonPosition.value.y}px` }
    : { bottom: '20px' }),
}))
const selectedPageBlock = computed(() => pageBlocks.value.find(item => item.id === selectedPageBlockId.value) || null)
const formAssets = computed(() => builder.value?.formAssets || [])
const filteredFormAssets = computed(() => {
  const keyword = formAssetSelectorKeyword.value.trim().toLowerCase()
  return !keyword
    ? formAssets.value
    : formAssets.value.filter(asset => `${asset.name || ''}${asset.id || ''}`.toLowerCase().includes(keyword))
})
const activeFormAsset = computed(() => formAssets.value.find(asset => asset.id === activeFormAssetId.value) || null)
const activeFormDesignerSchema = computed(() => normalizeFormDesignerSchema(activeFormAsset.value?.formDesignerSchema || {}))
const activeFormFields = computed(() => activeFormAsset.value ? resolveFormAssetFields(activeFormAsset.value) : [])
const selectedPageBlockFormAssetId = computed(() => selectedPageBlock.value?.props?.formAssetId || (formAssets.value.length === 1 ? formAssets.value[0].id : ''))
const selectedPageBlockFormAsset = computed(() => formAssets.value.find(asset => asset.id === selectedPageBlockFormAssetId.value) || null)
const selectedPageBlockRuntimeObjectRef = computed(() => selectedPageBlock.value ? resolvePageBlockObjectRef(selectedPageBlock.value) : null)
const selectedPageBlockUsesObjectRuntime = computed(() => selectedPageBlock.value?.blockType === 'AiCrudPage' && Boolean(selectedPageBlockRuntimeObjectRef.value))
const selectedPageBlockRuntimeObjectId = computed(() => String(selectedPageBlockRuntimeObjectRef.value?.objectId ?? selectedPageBlockRuntimeObjectRef.value?.id ?? ''))
const runtimeObjectFormOptions = computed(() => objects.value
  .filter(item => item.objectId ?? item.id)
  .map(item => ({
    value: String(item.objectId ?? item.id),
    label: item.objectName || item.objectCode || '未命名表单',
  })))
const pageTemplateOptions = computed(() => inAppPageTemplateCatalog.filter(template => ['blank', 'intro', 'crud', 'tree-table', 'master-detail'].includes(template.key)))
const selectedPageTemplate = computed(() => resolveInAppPageTemplate(selectedPageTemplateKey.value))
const pageTemplateObjectOptions = computed(() => objects.value
  .filter(item => item.objectId ?? item.id)
  .map(item => ({
    value: String(item.objectId ?? item.id),
    label: item.objectName || item.objectCode || '未命名业务对象',
  })))
const selectedPageBlockFields = computed(() => selectedPageBlock.value ? resolvePageBlockFields(selectedPageBlock.value) : [])
const applicationGridModelSchema = computed(() => {
  // ListPageGridDesigner 会在 modelSchema 改变时同步并回写整个布局。
  // 这里必须保持页面级模型稳定；当前区块切换数据源仅更新 fields prop，避免循环回写卡死。
  const objectRef = resolvePageBlockObjectRef({})
  const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
  const pageRuntimeFields = runtimeCrudPropsByObjectId.value[cacheKey]?.fieldCatalog || []
  return {
    businessName: currentNode.value?.title || '应用页面',
    // 列表设计器用 configKey 生成 /ai/crud/{configKey} 的默认接口。
    // 应用页必须把业务对象的真实配置键带过去，不能触发“当前配置”占位兜底。
    configKey: objectRef?.configKey || '',
    objectCode: objectRef?.objectCode || '',
    object: objectRef
      ? {
          code: objectRef.objectCode || '',
          configKey: objectRef.configKey || '',
        }
      : undefined,
    fields: pageRuntimeFields,
  }
})
const dragPreviewBlock = computed(() => dragPreview.value ? pageBlocks.value.find(item => item.id === dragPreview.value.blockId) || null : null)
const copyBlockPageOptions = computed(() => flattenNodes(builder.value?.nodes || [])
  .filter(node => node.type === 'page' && node.id !== currentNode.value?.id)
  .map(node => ({ label: `${'　'.repeat(node.depth || 0)}${node.title}`, value: node.id })))
const dirty = computed(() => JSON.stringify(builder.value || {}) !== savedSignature.value)
const canUndo = computed(() => undoStack.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)
const isDraftPreview = computed(() => route.query.draft === '1')
const canEditApplication = computed(() => userStore.isAdmin || hasPermission(userStore.permissions, 'ai:businessApplication:edit') || hasPermission(userStore.apiPermissions, 'ai:businessApplication:edit') || hasPermission(userStore.getDataPermission, 'ai:businessApplication:edit'))
const filteredComponents = computed(() => {
  const keyword = componentKeyword.value.trim().toLowerCase()
  return listPageBlockCatalog.filter((item) => {
    if (item.onlyFor && !item.onlyFor.includes('simple-crud'))
      return false
    if (!keyword)
      return true
    return `${item.title || ''}${item.desc || ''}${item.blockType || ''}`.toLowerCase().includes(keyword)
  })
})
const componentPickerGroups = computed(() => componentPickerGroupOptions
  .map(group => ({
    ...group,
    items: filteredComponents.value.filter(item => resolveComponentPickerGroup(item) === group.key),
  }))
  .filter(group => group.items.length))

function resolveComponentPickerGroup(item = {}) {
  const descriptor = `${item.blockType || ''} ${item.title || ''} ${item.group || ''}`.toLowerCase()
  if (/chart|gauge|stat|progress|趋势|图表|指标/.test(descriptor))
    return 'chart'
  if (/crud|table|list|search|列表|查询|数据/.test(descriptor))
    return 'list'
  if (/title|text|html|layout|tabs|divider|view|标题|文本|布局|标签|分割/.test(descriptor))
    return 'view'
  return 'other'
}

function resolveComponentIcon(item = {}) {
  const fileName = formComponentIconFileByBlockType[item.blockType]
  return fileName ? formComponentIconModules[`/src/assets/images/form/${fileName}.png`] || '' : ''
}

function resolveEmptyGuideIcon(item = {}) {
  const icons = {
    'page-title': TextOutline,
    'stats-strip': StatsChartOutline,
    'AiCrudPage': ListOutline,
    'AiForm': ReaderOutline,
    'custom-html': DocumentTextOutline,
    'info-panel': InformationCircleOutline,
  }
  return icons[item.blockType] || SquareOutline
}

const recommendedComponents = computed(() => {
  const recommendedTypes = ['page-title', 'stats-strip', 'AiCrudPage', 'AiForm', 'custom-html', 'info-panel']
  return recommendedTypes
    .map(blockType => resolveListPageBlockMeta(blockType))
    .filter(Boolean)
    .slice(0, 6)
})

watch(() => route.params.applicationCode, load, { immediate: true })
watch(() => currentNode.value?.id, () => {
  preloadCurrentPageCrudRuntimeProps()
}, { flush: 'post' })
watch(editing, value => router.replace({ query: { ...route.query, edit: value ? '1' : undefined } }))
watch(builder, (nextBuilder) => {
  if (!nextBuilder) {
    latestBuilderSnapshot = null
    return
  }
  const nextSnapshot = cloneBuilderSchema(nextBuilder)
  if (!historyReady.value) {
    latestBuilderSnapshot = nextSnapshot
    return
  }
  if (JSON.stringify(nextSnapshot) === JSON.stringify(latestBuilderSnapshot))
    return
  if (latestBuilderSnapshot)
    undoStack.value = [...undoStack.value, latestBuilderSnapshot].slice(-HISTORY_LIMIT)
  redoStack.value = []
  latestBuilderSnapshot = nextSnapshot
}, { deep: true, flush: 'sync' })
watch(() => navigationActionForm.value.icon, (icon) => {
  if (navigationActionMode.value !== 'icon' || !navigationActionNode.value)
    return
  builder.value = {
    ...builder.value,
    nodes: builder.value.nodes.map(item => item.id === navigationActionNode.value.id ? { ...item, icon: String(icon || '') } : item),
  }
})
onMounted(load)
onMounted(() => window.addEventListener('keydown', handleBuilderShortcut))
onBeforeUnmount(() => {
  endPageBlockResize()
  endPageBlockMove()
  endComponentButtonMove()
  window.removeEventListener('keydown', handleBuilderShortcut)
})

async function load() {
  const code = String(route.params.applicationCode || '')
  if (!code)
    return
  loading.value = true
  historyReady.value = false
  runtimeCrudPropsByObjectId.value = {}
  runtimeCrudLoadingObjectIds.clear()
  runtimeCrudUnavailableObjectIds.clear()
  try {
    const response = await businessApplicationWorkspaceByCode(code)
    const workspace = response.data || {}
    application.value = workspace.application || null
    objects.value = workspace.objects || []
    builder.value = ensurePageTitleComponents(normalizeInAppBuilder(application.value?.options, application.value, objects.value))
    hydratePageCrudApiPlaceholders()
    bindSingleFormToCompatibleBlocks()
    savedSignature.value = JSON.stringify(builder.value)
    resetBuilderHistory(builder.value)
    selectedNodeId.value = String(route.query.pageId || builder.value.homePageId || '')
    await nextTick()
    preloadCurrentPageCrudRuntimeProps()
  }
  finally { loading.value = false }
}

function cloneBuilderSchema(schema) {
  return JSON.parse(JSON.stringify(schema || {}))
}

function resetBuilderHistory(schema = builder.value) {
  undoStack.value = []
  redoStack.value = []
  latestBuilderSnapshot = cloneBuilderSchema(schema)
  historyReady.value = true
}

function applyBuilderHistorySnapshot(snapshot) {
  historyReady.value = false
  builder.value = cloneBuilderSchema(snapshot)
  latestBuilderSnapshot = cloneBuilderSchema(builder.value)
  historyReady.value = true
}

function undoBuilder() {
  if (!canUndo.value)
    return
  const currentSnapshot = cloneBuilderSchema(builder.value)
  const previousSnapshot = undoStack.value[undoStack.value.length - 1]
  undoStack.value = undoStack.value.slice(0, -1)
  redoStack.value = [currentSnapshot, ...redoStack.value].slice(0, HISTORY_LIMIT)
  applyBuilderHistorySnapshot(previousSnapshot)
}

function redoBuilder() {
  if (!canRedo.value)
    return
  const currentSnapshot = cloneBuilderSchema(builder.value)
  const nextSnapshot = redoStack.value[0]
  redoStack.value = redoStack.value.slice(1)
  undoStack.value = [...undoStack.value, currentSnapshot].slice(-HISTORY_LIMIT)
  applyBuilderHistorySnapshot(nextSnapshot)
}

function handleBuilderShortcut(event) {
  if (!editing.value || formDesignerMode.value)
    return
  const key = event.key?.toLowerCase?.()
  const isUndoKey = (event.metaKey || event.ctrlKey) && !event.shiftKey && key === 'z'
  const isRedoKey = (event.metaKey || event.ctrlKey) && ((event.shiftKey && key === 'z') || key === 'y')
  if (!isUndoKey && !isRedoKey)
    return
  if (event.target?.closest?.('input, textarea, [contenteditable="true"]'))
    return
  event.preventDefault()
  if (isRedoKey)
    redoBuilder()
  else
    undoBuilder()
}

function selectNode(nodeId) {
  selectedNodeId.value = nodeId || ''
  selectedPageBlockId.value = ''
  router.replace({ query: { ...route.query, pageId: nodeId || undefined, edit: editing.value ? '1' : undefined } })
}

function ensurePageTitleComponents(schema) {
  if (!schema?.pages || !Array.isArray(schema.nodes))
    return schema
  const nodeMap = new Map(schema.nodes.map(node => [node.id, node]))
  let changed = false
  const pages = Object.fromEntries(Object.entries(schema.pages).map(([pageId, page]) => {
    const node = nodeMap.get(pageId)
    if (node?.type !== 'page' || page?.layout?.pageTitleComponentInitialized)
      return [pageId, page]
    const rawLayout = page?.layout || {}
    const rawGridLayout = rawLayout.gridLayout || {
      cols: 12,
      rowHeight: 32,
      gap: 8,
      designWidth: 1366,
      layoutType: 'simple-crud',
      items: (rawLayout.items || []).map((item, index) => createLegacyBlock(item, index)).filter(Boolean),
    }
    const items = Array.isArray(rawGridLayout.items) ? rawGridLayout.items : []
    const hasPageTitle = items.some(item => item?.blockType === 'page-title')
    const titleBlock = hasPageTitle ? null : createPageTitleBlock(node, page)
    const shiftedItems = titleBlock
      ? items.map((item) => {
          const flowY = Number(item?.props?.style?.pageFlowY)
          if (!Number.isFinite(flowY))
            return item
          return {
            ...item,
            props: {
              ...(item.props || {}),
              style: { ...(item.props?.style || {}), pageFlowY: flowY + 192 },
            },
          }
        })
      : items
    changed = true
    return [pageId, {
      ...page,
      layout: {
        ...rawLayout,
        items: [],
        pageTitleComponentInitialized: true,
        gridLayout: {
          ...rawGridLayout,
          items: titleBlock ? [titleBlock, ...shiftedItems] : shiftedItems,
        },
      },
    }]
  }))
  return changed ? { ...schema, pages } : schema
}

function createPageTitleBlock(node, page) {
  const block = createGridBlock('page-title', { businessName: node?.title || '应用页面', fields: [] }, { gridX: 0, gridY: 0 })
  if (!block)
    return null
  return {
    ...block,
    props: {
      ...(block.props || {}),
      title: node?.title || page?.title || '页面标题',
      subtitle: page?.description || '',
      content: createPageTitleRichContent(node?.title || page?.title || '页面标题', page?.description || ''),
      style: {
        ...(block.props?.style || {}),
        widthMode: 'full',
        pageFlowX: 24,
        pageFlowY: 20,
        pageFlowWidth: 'calc(100% - 48px)',
        pageFlowHeight: 176,
      },
    },
  }
}

function createPageTitleRichContent(title, subtitle) {
  const escape = value => String(value || '').replace(/[&<>"']/g, (char) => {
    if (char === '\"')
      return '&quot;'
    if (char === String.fromCharCode(39))
      return '&#39;'
    return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' })[char]
  })
  return `<h1>${escape(title)}</h1>${subtitle ? `<p>${escape(subtitle)}</p>` : ''}`
}

function createQuickNode(type = 'page', parentId = null) {
  const normalizedType = type === 'group' ? 'group' : 'page'
  if (normalizedType === 'page')
    createPageFromTemplate('blank', '', parentId)
  if (normalizedType === 'page')
    return
  const previousIds = new Set(builder.value.nodes.map(item => item.id))
  builder.value = createNavigationNode(builder.value, {
    type: normalizedType,
    title: resolveNextNavigationTitle(normalizedType),
    pageType: 'content',
    parentId,
  })
  const created = builder.value.nodes.find(item => !previousIds.has(item.id))
  newNodePopoverVisible.value = false
  if (created?.type === 'page')
    selectNode(created.id)
}

function selectIntroTemplate(templateKey) {
  selectedPageTemplateKey.value = templateKey
  const template = resolveInAppPageTemplate(templateKey)
  if (!template.requiresObject) {
    selectedPageTemplateObjectId.value = null
    createPageFromTemplate(templateKey)
  }
  else if (pageTemplateObjectOptions.value.length === 1) {
    selectedPageTemplateObjectId.value = pageTemplateObjectOptions.value[0].value
    createPageFromTemplate(templateKey)
  }
}

function resolvePageTemplateIcon(template = {}) {
  const icons = {
    'blank': DocumentTextOutline,
    'intro': InformationCircleOutline,
    'crud': ListOutline,
    'tree-table': GitBranchOutline,
    'master-detail': ReaderOutline,
  }
  return icons[template.key] || DocumentTextOutline
}

function createPageFromTemplate(templateKey = selectedPageTemplateKey.value, initialBlockType = '', parentId = null) {
  const template = resolveInAppPageTemplate(templateKey)
  const object = template.requiresObject
    ? objects.value.find(item => String(item.objectId ?? item.id ?? '') === String(selectedPageTemplateObjectId.value || ''))
    : null
  if (template.requiresObject && !object) {
    message.warning('请选择要绑定的业务对象')
    return
  }
  const previousIds = new Set(builder.value.nodes.map(item => item.id))
  builder.value = createNavigationNode(builder.value, {
    type: 'page',
    title: resolveNextNavigationTitle('page'),
    parentId,
    pageType: template.pageType || 'content',
    pageTemplate: template.key,
    objectRef: object
      ? {
          objectId: String(object.objectId ?? object.id),
          objectCode: object.objectCode || '',
          objectName: object.objectName || '',
          configKey: object.configKey || '',
          pageKey: 'list',
          pageMode: template.objectPageMode || 'crud',
        }
      : null,
  })
  const created = builder.value.nodes.find(item => !previousIds.has(item.id))
  if (created) {
    selectNode(created.id)
    applyPageTemplate(created.id, template.key, initialBlockType)
  }
}

function resolveNextNavigationTitle(type = 'page') {
  const prefix = type === 'group' ? '页面组' : '页面'
  const count = (builder.value?.nodes || []).filter(node => node.type === type).length + 1
  return `${prefix} ${count}`
}

function applyPageTemplate(pageId, templateKey, initialBlockType = '') {
  const template = resolveInAppPageTemplate(templateKey)
  const blockTypes = initialBlockType ? [initialBlockType] : template.blockTypes
  const items = blockTypes
    .map((blockType, index) => attachDefaultRuntimeObject(createGridBlock(blockType, { businessName: currentNode.value?.title || '应用页面', fields: [] }, { gridX: 0, gridY: index * 2 })))
    .filter(Boolean)
  const page = builder.value?.pages?.[pageId]
  if (!page)
    return
  builder.value = {
    ...builder.value,
    pages: {
      ...builder.value.pages,
      [pageId]: {
        ...page,
        layout: {
          ...page.layout,
          items: [],
          // 空白页不再被旧页面迁移逻辑补入标题组件。
          pageTitleComponentInitialized: true,
          gridLayout: { cols: 12, rowHeight: 32, gap: 8, designWidth: 1366, layoutType: 'simple-crud', items },
        },
      },
    },
  }
}

function resolveNavigationMoreOptions(node) {
  const siblings = (builder.value?.nodes || []).filter(item => item.parentId === node.parentId).sort((a, b) => a.sort - b.sort)
  const index = siblings.findIndex(item => item.id === node.id)
  return [
    ...(
      node.type === 'group'
        ? [
            { label: '在本组新建页面', key: 'create-page', icon: () => renderNavigationMenuIcon(AddOutline) },
            { label: '在本组新建页面组', key: 'create-group', icon: () => renderNavigationMenuIcon(FolderOpenOutline) },
            { type: 'divider', key: 'create-divider' },
          ]
        : []
    ),
    { label: '重命名', key: 'rename', icon: () => renderNavigationMenuIcon(CreateOutline) },
    { label: '更改图标', key: 'icon', icon: () => renderNavigationMenuIcon(ColorFillOutline) },
    { label: '复制', key: 'duplicate', icon: () => renderNavigationMenuIcon(CopyOutline) },
    { label: '移动至', key: 'move', icon: () => renderNavigationMenuIcon(MoveOutline) },
    { type: 'divider', key: 'move-divider' },
    { label: '上移', key: 'move-up', disabled: index <= 0, icon: () => renderNavigationMenuIcon(ArrowUpOutline) },
    { label: '下移', key: 'move-down', disabled: index < 0 || index >= siblings.length - 1, icon: () => renderNavigationMenuIcon(ArrowDownOutline) },
    { type: 'divider', key: 'danger-divider' },
    { label: '删除', key: 'delete', icon: () => renderNavigationMenuIcon(TrashOutline) },
  ]
}

function renderNavigationMenuIcon(icon) {
  return h(NIcon, { size: 16, class: 'navigation-menu-icon' }, { default: () => h(icon) })
}

function handleNavigationMoreSelect(key, node) {
  if (key === 'create-page' || key === 'create-group') {
    createQuickNode(key === 'create-group' ? 'group' : 'page', node.id)
    return
  }
  if (key === 'move-up' || key === 'move-down') {
    moveNavigationByOffset(node, key === 'move-up' ? -1 : 1)
    return
  }
  if (key === 'duplicate') {
    duplicateNavigationNode(node)
    return
  }
  if (key === 'icon') {
    iconPickerNodeId.value = node.id
    iconPickerVisible.value = true
    nextTick(() => document.querySelector('.navigation-icon-picker .icon-selector button')?.click())
    return
  }
  navigationActionNodeId.value = node.id
  navigationActionMode.value = key
  navigationActionForm.value = {
    title: node.title || '',
    icon: node.icon || '',
    parentId: node.parentId || null,
    deleteStrategy: 'delete-children',
    targetParentId: null,
  }
  navigationActionVisible.value = true
}

function duplicateNavigationNode(node) {
  const copyId = `${node.type}_${Date.now()}`
  const copy = {
    ...JSON.parse(JSON.stringify(node)),
    id: copyId,
    title: `${node.title} 副本`,
    sort: Number(node.sort || 0) + 1,
  }
  builder.value = {
    ...builder.value,
    nodes: [...builder.value.nodes, copy],
    pages: node.type === 'page' && builder.value.pages[node.id]
      ? { ...builder.value.pages, [copyId]: JSON.parse(JSON.stringify(builder.value.pages[node.id])) }
      : builder.value.pages,
  }
  if (copy.type === 'page')
    selectNode(copy.id)
}

function moveNavigationByOffset(node, offset) {
  const siblings = (builder.value?.nodes || []).filter(item => item.parentId === node.parentId).sort((a, b) => a.sort - b.sort)
  const index = siblings.findIndex(item => item.id === node.id)
  if (index < 0 || index + offset < 0 || index + offset >= siblings.length)
    return
  try {
    builder.value = moveNavigationNode(builder.value, node.id, node.parentId, offset < 0 ? index - 1 : index + 1)
  }
  catch (error) {
    message.error(error?.message || '页面排序失败')
  }
}

function confirmNavigationAction() {
  const node = navigationActionNode.value
  if (!node)
    return
  try {
    if (navigationActionMode.value === 'rename') {
      const title = navigationActionForm.value.title.trim()
      if (!title) {
        message.warning('请输入名称')
        return
      }
      builder.value = {
        ...builder.value,
        nodes: builder.value.nodes.map(item => item.id === node.id ? { ...item, title } : item),
        pages: node.type === 'page'
          ? { ...builder.value.pages, [node.id]: { ...builder.value.pages[node.id], title } }
          : builder.value.pages,
      }
    }
    if (navigationActionMode.value === 'icon') {
      builder.value = {
        ...builder.value,
        nodes: builder.value.nodes.map(item => item.id === node.id ? { ...item, icon: navigationActionForm.value.icon.trim() } : item),
      }
    }
    if (navigationActionMode.value === 'move')
      builder.value = moveNavigationNode(builder.value, node.id, navigationActionForm.value.parentId)
    if (navigationActionMode.value === 'delete') {
      const strategy = navigationActionHasChildren.value
        ? {
            type: navigationActionForm.value.deleteStrategy,
            targetParentId: navigationActionForm.value.targetParentId,
          }
        : undefined
      if (strategy?.type === 'move-children' && !strategy.targetParentId && strategy.targetParentId !== null) {
        message.warning('请选择子项移动目标')
        return
      }
      builder.value = removeNavigationNode(builder.value, node.id, strategy)
      if (!builder.value.nodes.some(item => item.id === selectedNodeId.value))
        selectNode(builder.value.homePageId)
    }
    navigationActionVisible.value = false
  }
  catch (error) {
    message.error(error?.message || '页面操作失败')
  }
}

function isNavigationGroupDescendant(candidateId, nodeId) {
  if (!candidateId || !nodeId)
    return false
  const nodes = builder.value?.nodes || []
  let current = nodes.find(item => item.id === candidateId)
  const visited = new Set()
  while (current?.parentId && !visited.has(current.id)) {
    if (current.parentId === nodeId)
      return true
    visited.add(current.id)
    current = nodes.find(item => item.id === current.parentId)
  }
  return false
}

function insertComponent(component) {
  appendPageBlock(component.blockType)
  componentPopoverVisible.value = false
}

function updateCurrentGridLayout(gridLayout) {
  if (!currentNode.value || !currentPage.value)
    return
  builder.value = {
    ...builder.value,
    pages: {
      ...builder.value.pages,
      [currentNode.value.id]: {
        ...currentPage.value,
        layout: { ...currentPage.value.layout, items: [], gridLayout },
      },
    },
  }
}

function appendPageBlock(blockType) {
  const meta = resolveListPageBlockMeta(blockType)
  if (!meta)
    return
  if (meta.unique && pageBlocks.value.some(block => block.blockType === blockType)) {
    message.info(`${meta.title} 每个页面只能添加一个`)
    return
  }
  let block = createGridBlock(blockType, applicationGridModelSchema.value, {
    gridX: 0,
    gridY: pageBlocks.value.length * 2,
  })
  if (!block)
    return
  block = attachDefaultRuntimeObject(block)
  block = attachSingleFormAsset(block)
  updatePageBlocks([...pageBlocks.value, block], { resolveCollisions: true, changedBlockId: block.id })
  selectedPageBlockId.value = block.id
  preloadPageBlockCrudRuntimeProps(block)
}

function supportsFormAsset(block = {}) {
  return ['AiForm', 'AiCrudPage', 'AiTable', 'data-table', 'search-form', 'detail-info'].includes(block.blockType)
}

function bindSingleFormToCompatibleBlocks() {
  if (formAssets.value.length !== 1 || !builder.value)
    return
  builder.value = {
    ...builder.value,
    pages: Object.fromEntries(Object.entries(builder.value.pages || {}).map(([pageId, page]) => {
      const items = page?.layout?.gridLayout?.items
      if (!Array.isArray(items))
        return [pageId, page]
      return [pageId, {
        ...page,
        layout: {
          ...page.layout,
          gridLayout: {
            ...page.layout.gridLayout,
            items: items.map(item => attachSingleFormAsset(item)),
          },
        },
      }]
    })),
  }
}

function attachSingleFormAsset(block = {}) {
  if (formAssets.value.length !== 1 || !supportsFormAsset(block))
    return block
  const asset = formAssets.value[0]
  if (block.props?.formAssetId && block.props.formAssetId !== asset.id)
    return block
  const fieldRefs = resolveFormAssetFields(asset).map(field => field.fieldCode)
  const currentRefs = Array.isArray(block.fieldRefs) ? block.fieldRefs : []
  const mergedRefs = Array.from(new Set([...currentRefs, ...fieldRefs]))
  const currentSettings = block.props?.fieldSettings || {}
  const allBoundFieldsHidden = mergedRefs.length > 0 && mergedRefs.every(field => currentSettings[field]?.visible === false)
  return {
    ...block,
    fieldRefs: mergedRefs,
    props: {
      ...(block.props || {}),
      formAssetId: asset.id,
      formAssetFieldsInitialized: mergedRefs.length > 0,
      fieldSettings: createFormFieldVisibilitySettings(currentSettings, mergedRefs, !block.props?.formAssetId || allBoundFieldsHidden),
      ...(['AiCrudPage', 'search-form'].includes(block.blockType)
        ? { searchFieldRefs: (Array.isArray(block.props?.searchFieldRefs) && block.props.searchFieldRefs.length ? block.props.searchFieldRefs : mergedRefs).slice(0, 8) }
        : {}),
    },
  }
}

function resolveFormAssetFields(asset = {}) {
  const schema = normalizeFormDesignerSchema(asset.formDesignerSchema || {})
  const fieldsByCode = new Map(buildAutoFieldAssets(schema).fields.map(field => [field.fieldCode || field.field, field]))
  const appendComponentFields = (components = []) => {
    ;(Array.isArray(components) ? components : []).forEach((component, index) => {
      const fieldCode = component?.fieldBinding?.fieldCode || component?.field || ''
      if (component?.fieldBinding?.mode !== 'virtual' && isFieldComponent(component) && fieldCode && !fieldsByCode.has(fieldCode))
        fieldsByCode.set(fieldCode, createFieldFromComponent(component, index))
      appendComponentFields(component?.children || [])
    })
  }
  appendComponentFields(schema.components)
  return [...fieldsByCode.values()].map((field, index) => ({
    ...field,
    field: field.field || field.fieldCode || field.fieldBinding?.fieldCode,
    fieldCode: field.fieldCode || field.field || field.fieldBinding?.fieldCode,
    sourceField: field.sourceField || field.field || field.fieldCode || field.fieldBinding?.fieldCode,
    fieldName: field.fieldName || field.label || field.fieldCode || `字段 ${index + 1}`,
    label: field.fieldName || field.label || field.fieldCode || `字段 ${index + 1}`,
    listVisible: field.listVisible !== false,
    formVisible: field.formVisible !== false,
    fieldStatus: field.fieldStatus || 'ENABLED',
    systemField: Boolean(field.systemField),
  })).filter(field => field.fieldCode && field.field)
}

function resolvePageBlockFields(block = {}) {
  const runtimeFields = resolvePageBlockRuntimeCrudProps(block)?.fieldCatalog || []
  if (runtimeFields.length)
    return runtimeFields
  const formAssetId = block?.props?.formAssetId || (formAssets.value.length === 1 ? formAssets.value[0]?.id : '')
  const asset = formAssets.value.find(item => item.id === formAssetId)
  return asset ? resolveFormAssetFields(asset) : []
}

/**
 * 应用页不是让业务人员再次填写接口地址的地方：
 * - 业务对象页天然绑定当前对象；
 * - 单对象应用无需选择，直接绑定唯一对象；
 * - 多对象的普通内容页不猜测数据源，仍保持安全的静态预览。
 */
function resolvePageBlockObjectRef(block = {}, pageNode = currentNode.value) {
  const blockRef = block?.props?.objectRef || block?.props?.businessObjectRef
  // 业务应用存在多个关联对象时，未显式选择数据源的页面组件默认复用主对象。
  // 这是列表设计器原本的默认行为，不能退回成“当前配置”这种不可运行占位值。
  const primaryObject = objects.value.find(item => String(item.objectRole || '').toUpperCase() === 'PRIMARY')
  const candidate = blockRef || pageNode?.objectRef || primaryObject || (objects.value.length === 1 ? objects.value[0] : null)
  if (!candidate)
    return null
  const objectId = candidate.objectId ?? candidate.id
  const objectCode = candidate.objectCode || ''
  if (objectId === undefined || objectId === null || objectId === '') {
    const matched = objects.value.find(item => objectCode && String(item.objectCode || '') === String(objectCode))
    if (!matched)
      return null
    return { ...matched, objectId: matched.objectId ?? matched.id }
  }
  const matched = objects.value.find(item => String(item.objectId ?? item.id ?? '') === String(objectId))
  return { ...(matched || {}), ...candidate, objectId: String(objectId), objectCode: objectCode || matched?.objectCode || '' }
}

/**
 * 早期应用页创建的区块会把列表设计器的无上下文占位值持久化为
 * `/ai/crud/当前配置`。加载时按每页实际绑定的业务对象完成一次迁移，
 * 让右侧属性面板与真实运行接口都展示同一个 configKey。
 */
function hydratePageCrudApiPlaceholders() {
  if (!builder.value?.pages || !builder.value?.nodes)
    return
  const nodeById = new Map(builder.value.nodes.map(node => [node.id, node]))
  let changed = false
  const pages = Object.fromEntries(Object.entries(builder.value.pages).map(([pageId, page]) => {
    const items = page?.layout?.gridLayout?.items
    if (!Array.isArray(items))
      return [pageId, page]
    const pageNode = nodeById.get(pageId)
    const nextItems = items.map((block) => {
      if (block?.blockType !== 'AiCrudPage')
        return block
      const configKey = resolvePageBlockObjectRef(block, pageNode)?.configKey || ''
      if (!configKey)
        return block
      const serializedProps = JSON.stringify(block.props || {})
      const nextSerializedProps = serializedProps.replaceAll('/ai/crud/当前配置', `/ai/crud/${configKey}`)
      if (nextSerializedProps === serializedProps)
        return block
      changed = true
      return { ...block, props: JSON.parse(nextSerializedProps) }
    })
    if (!nextItems.some((item, index) => item !== items[index]))
      return [pageId, page]
    return [pageId, {
      ...page,
      layout: {
        ...page.layout,
        gridLayout: { ...page.layout.gridLayout, items: nextItems },
      },
    }]
  }))
  if (changed)
    builder.value = { ...builder.value, pages }
}

function resolveRuntimeObjectCacheKey(objectRef) {
  if (!objectRef)
    return ''
  return String(objectRef.objectId ?? objectRef.id ?? objectRef.objectCode ?? '').trim()
}

function attachDefaultRuntimeObject(block = {}) {
  if (block.blockType !== 'AiCrudPage' || block.props?.objectRef || block.props?.businessObjectRef)
    return block
  const objectRef = resolvePageBlockObjectRef(block)
  if (!objectRef)
    return block
  return {
    ...block,
    props: {
      ...(block.props || {}),
      objectRef: {
        objectId: String(objectRef.objectId ?? objectRef.id),
        objectCode: objectRef.objectCode || '',
        objectName: objectRef.objectName || '',
      },
    },
  }
}

function resolvePageBlockRuntimeCrudProps(block = {}) {
  if (block.blockType !== 'AiCrudPage')
    return null
  const objectRef = resolvePageBlockObjectRef(block)
  const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
  if (!cacheKey)
    return null
  if (!runtimeCrudPropsByObjectId.value[cacheKey])
    preloadPageBlockCrudRuntimeProps(block)
  return runtimeCrudPropsByObjectId.value[cacheKey] || null
}

function preloadCurrentPageCrudRuntimeProps() {
  pageBlocks.value
    .filter(block => block.blockType === 'AiCrudPage')
    .forEach(block => preloadPageBlockCrudRuntimeProps(block))
}

function preloadPageBlockCrudRuntimeProps(block = {}) {
  const objectRef = resolvePageBlockObjectRef(block)
  const cacheKey = resolveRuntimeObjectCacheKey(objectRef)
  const objectId = objectRef?.objectId ?? objectRef?.id
  if (!cacheKey || objectId === undefined || objectId === null || objectId === '')
    return
  if (runtimeCrudPropsByObjectId.value[cacheKey] || runtimeCrudLoadingObjectIds.has(cacheKey) || runtimeCrudUnavailableObjectIds.has(cacheKey))
    return
  runtimeCrudLoadingObjectIds.add(cacheKey)
  void loadRuntimeCrudProps(objectRef, cacheKey)
}

async function loadRuntimeCrudProps(objectRef, cacheKey) {
  try {
    // 工作台对象快照本身已经带 configKey，优先使用它，和旧列表设计器的
    // 运行入口一致，也避免运行用户额外依赖“查看业务对象”权限。
    let runtimeInfo = {}
    let configKey = String(objectRef.configKey || '').trim()
    if (!configKey) {
      runtimeInfo = (await businessObjectRuntimeInfo(objectRef.objectId ?? objectRef.id)).data || {}
      configKey = String(runtimeInfo.configKey || '').trim()
    }
    if (!configKey)
      throw new Error('该业务对象还没有可用的列表运行配置')
    const config = (await crudConfigRender(configKey)).data
    if (!config || typeof config !== 'object')
      throw new Error('业务对象运行配置为空')
    runtimeCrudPropsByObjectId.value = {
      ...runtimeCrudPropsByObjectId.value,
      [cacheKey]: {
        ...buildRuntimeCrudProps(config),
        title: config.title || runtimeInfo.objectName || objectRef.objectName || '',
      },
    }
  }
  catch (error) {
    runtimeCrudUnavailableObjectIds.add(cacheKey)
    // 对象还在设计/未发布时维持静态预览，不中断整个应用页。
    console.warn('[application-runtime] 加载业务对象 CRUD 运行配置失败', error?.message || error)
  }
  finally {
    runtimeCrudLoadingObjectIds.delete(cacheKey)
  }
}

function createFormFieldVisibilitySettings(currentSettings = {}, fieldRefs = [], forceVisible = false) {
  const settings = { ...(currentSettings || {}) }
  fieldRefs.filter(Boolean).forEach((field) => {
    const current = settings[field] || {}
    if (forceVisible || !Object.prototype.hasOwnProperty.call(current, 'visible'))
      settings[field] = { ...current, visible: true }
  })
  return settings
}

function updateSelectedBlockFormAsset(formAssetId) {
  if (!selectedPageBlock.value)
    return
  const asset = formAssets.value.find(item => item.id === formAssetId)
  const fieldRefs = asset ? resolveFormAssetFields(asset).map(field => field.fieldCode) : []
  updatePageBlocks(pageBlocks.value.map(item => item.id === selectedPageBlock.value.id
    ? {
        ...item,
        fieldRefs: fieldRefs.length ? fieldRefs : item.fieldRefs,
        props: {
          ...(item.props || {}),
          formAssetId: formAssetId || '',
          formAssetFieldsInitialized: fieldRefs.length > 0,
          fieldSettings: createFormFieldVisibilitySettings(item.props?.fieldSettings, fieldRefs, true),
          ...(['AiCrudPage', 'search-form'].includes(item.blockType) && fieldRefs.length
            ? { searchFieldRefs: fieldRefs.slice(0, 8) }
            : {}),
        },
      }
    : item))
}

function selectFormAssetFromPicker(formAssetId) {
  updateSelectedBlockFormAsset(formAssetId)
  formAssetSelectorOpen.value = false
  formAssetSelectorKeyword.value = ''
}

function updateSelectedPageBlockRuntimeObject(objectId) {
  if (!selectedPageBlock.value)
    return
  const object = objects.value.find(item => String(item.objectId ?? item.id ?? '') === String(objectId || ''))
  if (!object)
    return
  const configKey = String(object.configKey || '').trim()
  const apiPrefix = configKey ? `/ai/crud/${configKey}` : ''
  const objectApiProps = apiPrefix
    ? {
        api: apiPrefix,
        listApi: `get@${apiPrefix}/page`,
        detailApi: `get@${apiPrefix}/:id`,
        createApi: `post@${apiPrefix}`,
        updateApi: `put@${apiPrefix}`,
        deleteApi: `delete@${apiPrefix}/:id`,
        importApi: `post@${apiPrefix}/import`,
        exportApi: `post@${apiPrefix}/export`,
      }
    : {}
  const nextBlock = {
    ...selectedPageBlock.value,
    props: {
      ...(selectedPageBlock.value.props || {}),
      ...objectApiProps,
      objectRef: {
        objectId: String(object.objectId ?? object.id),
        objectCode: object.objectCode || '',
        objectName: object.objectName || '',
        configKey: object.configKey || '',
      },
    },
  }
  // 数据源切换不影响坐标和尺寸，直接更新布局，避免触发根页面碰撞重算。
  updateCurrentGridLayout({
    ...currentGridLayout.value,
    items: pageBlocks.value.map(block => block.id === nextBlock.id ? nextBlock : block),
  })
  runtimeCrudUnavailableObjectIds.delete(resolveRuntimeObjectCacheKey(object))
  preloadPageBlockCrudRuntimeProps(nextBlock)
}

function createFormAssetForSelectedBlock() {
  if (!currentNode.value)
    return
  const blockTitle = selectedPageBlock.value?.label || resolveListPageBlockMeta(selectedPageBlock.value?.blockType)?.title || '页面'
  const name = `${currentNode.value.title}${blockTitle === 'AiForm' ? '录入表单' : '数据表单'}`
  const result = createInAppFormAsset(builder.value, {
    name,
    formDesignerSchema: createDefaultFormDesignerSchema({
      objectCode: application.value?.applicationCode || 'application',
      objectName: name,
      formName: name,
    }),
  })
  builder.value = result.schema
  updateSelectedBlockFormAsset(result.formAssetId)
  activeFormAssetId.value = result.formAssetId
  formDesignerMode.value = true
}

function createStandaloneFormAsset() {
  const name = `${application.value?.applicationName || '应用'}表单`
  const result = createInAppFormAsset(builder.value, {
    name,
    formDesignerSchema: createDefaultFormDesignerSchema({
      objectCode: application.value?.applicationCode || 'application',
      objectName: name,
      formName: name,
    }),
  })
  builder.value = result.schema
  bindSingleFormToCompatibleBlocks()
  openFormAssetDesigner(result.formAssetId)
}

function openFormAssetDesigner(formAssetId) {
  activeFormAssetId.value = formAssetId
  formDesignerMode.value = true
}

function openPageBlockConfiguration(block = {}) {
  if (!editing.value || !block?.id)
    return
  selectedPageBlockId.value = block.id
  inspectorTab.value = 'properties'
  configPanelVisible.value = true
}

function editSelectedBlockFormAsset() {
  const formAssetId = selectedPageBlockFormAssetId.value
  if (!formAssetId)
    return
  openFormAssetDesigner(formAssetId)
}

function updateActiveFormDesignerSchema(schema) {
  if (!activeFormAsset.value)
    return
  const normalizedSchema = normalizeFormDesignerSchema(schema)
  const formAssetId = activeFormAsset.value.id
  let nextBuilder = updateInAppFormAsset(builder.value, formAssetId, {
    name: normalizedSchema.formName || activeFormAsset.value.name,
    formDesignerSchema: normalizedSchema,
  })
  const nextAsset = nextBuilder.formAssets.find(asset => asset.id === formAssetId)
  const fieldRefs = resolveFormAssetFields(nextAsset).map(field => field.fieldCode)
  if (fieldRefs.length) {
    nextBuilder = {
      ...nextBuilder,
      pages: Object.fromEntries(Object.entries(nextBuilder.pages || {}).map(([pageId, page]) => {
        const items = page?.layout?.gridLayout?.items
        if (!Array.isArray(items))
          return [pageId, page]
        return [pageId, {
          ...page,
          layout: {
            ...page.layout,
            gridLayout: {
              ...page.layout.gridLayout,
              items: items.map((item) => {
                if (item?.props?.formAssetId !== formAssetId)
                  return item
                const currentRefs = Array.isArray(item.fieldRefs) ? item.fieldRefs : []
                const mergedRefs = Array.from(new Set([...currentRefs, ...fieldRefs]))
                const currentSearchRefs = Array.isArray(item.props?.searchFieldRefs) ? item.props.searchFieldRefs : []
                return {
                  ...item,
                  fieldRefs: mergedRefs,
                  props: {
                    ...(item.props || {}),
                    formAssetFieldsInitialized: true,
                    fieldSettings: createFormFieldVisibilitySettings(item.props?.fieldSettings, mergedRefs),
                    ...(['AiCrudPage', 'search-form'].includes(item.blockType)
                      ? { searchFieldRefs: Array.from(new Set([...currentSearchRefs, ...fieldRefs])).slice(0, 8) }
                      : {}),
                  },
                }
              }),
            },
          },
        }]
      })),
    }
  }
  builder.value = nextBuilder
}

function returnToPageDesigner() {
  formDesignerMode.value = false
  activeFormAssetId.value = ''
}

function updatePageBlocks(items, options = {}) {
  const nextItems = options.resolveCollisions
    ? resolveRootPageBlockCollisions(items, options.changedBlockId)
    : items
  updateCurrentGridLayout({ ...currentGridLayout.value, items: nextItems })
}

function resolvePageBlockShellStyle(block) {
  const meta = resolveListPageBlockMeta(block.blockType) || {}
  const style = block.props?.style || {}
  const customWidth = String(style.pageFlowWidth || '').trim()
  const customHeight = Number(style.pageFlowHeight)
  const customX = Number(style.pageFlowX)
  const customY = Number(style.pageFlowY)
  const widthMode = style.widthMode || 'full'
  const heightMode = style.heightMode || 'fixed'
  const frameWidth = readPageBlockLength(style.width)
  const frameHeight = readPageBlockLength(style.height)
  const index = pageBlocks.value.findIndex(item => item.id === block.id)
  const position = {
    position: 'absolute',
    left: `${Number.isFinite(customX) && customX >= 0 ? customX : 24}px`,
    top: `${Number.isFinite(customY) && customY >= 0 ? customY : resolveDefaultPageBlockY(block, index)}px`,
    height: `${heightMode === 'auto' ? Math.min(frameHeight || resolveDefaultPageBlockHeight(block), 180) : customHeight > 0 ? customHeight : frameHeight > 0 ? frameHeight : resolveDefaultPageBlockHeight(block)}px`,
    textAlign: style.textAlign || block.props?.textAlign || block.props?.align || 'left',
  }
  if (heightMode === 'full') {
    position.height = 'auto'
    position.bottom = '24px'
  }
  // “填充容器”必须覆盖此前拖拽/固定宽度留下的 pageFlowWidth 和 X 偏移。
  // 否则组件虽已切到填充模式，运行预览仍会沿用旧的固定尺寸。
  if (widthMode === 'full')
    return { ...position, left: '24px', width: 'calc(100% - 48px)' }
  if (widthMode === 'auto')
    return { ...position, width: customWidth || `min(${Math.max(280, Math.min(560, frameWidth || 520))}px, calc(100% - 48px))` }
  if (widthMode === 'fixed' && frameWidth > 0)
    return { ...position, width: customWidth || `min(${frameWidth}px, calc(100% - 48px))` }
  const columns = Math.min(12, Math.max(3, Number(meta.defaultW) || 6))
  return {
    ...position,
    width: customWidth || `${Math.round((columns / 12) * 10000) / 100}%`,
  }
}

function resolveDefaultPageBlockHeight(block = {}) {
  const blockType = block.blockType || ''
  if (blockType === 'page-title')
    return 176
  if (['divider', 'custom-html'].includes(blockType))
    return 88
  if (['stats-strip', 'info-panel', 'AiForm'].includes(blockType))
    return 128
  if (['AiCrudPage', 'AiTable', 'data-table', 'search-form', 'toolbar'].includes(blockType))
    return 220
  return 116
}

function resolveDefaultPageBlockY(block, index = pageBlocks.value.findIndex(item => item.id === block?.id)) {
  return pageBlocks.value
    .slice(0, Math.max(0, index))
    .reduce((top, item) => top + Number(item.props?.style?.pageFlowHeight || resolveDefaultPageBlockHeight(item)) + 16, 20)
}

function readPageBlockLength(value, fallback = 0) {
  const number = Number.parseFloat(String(value || fallback))
  return Number.isFinite(number) ? Math.round(number) : 0
}

/**
 * 根页面的组件是最终页面的布局，不是可重叠的自由画布。
 * 这里仅整理根级块；组合布局中的 children 继续由自己的容器布局管理。
 */
function resolveRootPageBlockCollisions(items = [], changedBlockId = '') {
  const gap = 16
  const placed = []
  const sorted = items
    .map((block, index) => ({ block, index, geometry: resolvePageBlockFlowGeometry(block, index, items) }))
    .sort((left, right) => left.geometry.y - right.geometry.y || left.geometry.x - right.geometry.x || left.index - right.index)

  const resolvedById = new Map()
  sorted.forEach(({ block, geometry }) => {
    let nextY = geometry.y
    placed.forEach((previous) => {
      const horizontallyOverlapped = geometry.x < previous.right && geometry.right > previous.x
      const needsPushDown = horizontallyOverlapped && nextY < previous.bottom + gap
      if (needsPushDown)
        nextY = previous.bottom + gap
    })

    const resolved = { ...geometry, y: nextY, bottom: nextY + geometry.height }
    placed.push(resolved)
    if (nextY !== geometry.y || block.id === changedBlockId) {
      resolvedById.set(block.id, {
        ...block,
        props: {
          ...(block.props || {}),
          style: {
            ...(block.props?.style || {}),
            pageFlowY: Math.round(nextY),
          },
        },
      })
    }
  })

  return items.map(block => resolvedById.get(block.id) || block)
}

function resolvePageBlockFlowGeometry(block = {}, index = 0, items = pageBlocks.value) {
  const style = block.props?.style || {}
  const meta = resolveListPageBlockMeta(block.blockType) || {}
  const x = Number.isFinite(Number(style.pageFlowX)) && Number(style.pageFlowX) >= 0
    ? Number(style.pageFlowX)
    : 24
  const y = Number.isFinite(Number(style.pageFlowY)) && Number(style.pageFlowY) >= 0
    ? Number(style.pageFlowY)
    : resolveDefaultPageBlockYFromItems(items, index)
  const widthMode = style.widthMode || 'full'
  const heightMode = style.heightMode || 'fixed'
  const explicitWidth = readPageBlockLength(style.pageFlowWidth) || readPageBlockLength(style.width)
  const explicitHeight = readPageBlockLength(style.pageFlowHeight) || readPageBlockLength(style.height)
  const width = widthMode === 'full'
    ? 100000
    : explicitWidth || Math.round((Math.min(12, Math.max(3, Number(meta.defaultW) || 6)) / 12) * 1200)
  const defaultHeight = resolveDefaultPageBlockHeight(block)
  const height = heightMode === 'auto'
    ? Math.min(explicitHeight || defaultHeight, 180)
    : heightMode === 'full'
      ? Math.max(explicitHeight || defaultHeight, 180)
      : explicitHeight || defaultHeight
  return { x, y, width, height, right: x + width, bottom: y + height }
}

function resolveDefaultPageBlockYFromItems(items = [], index = 0) {
  return items
    .slice(0, Math.max(0, index))
    .reduce((top, item) => top + Number(item.props?.style?.pageFlowHeight || resolveDefaultPageBlockHeight(item)) + 16, 20)
}

function selectPageBlock(blockId) {
  if (!editing.value)
    return
  selectedPageBlockId.value = blockId
  inspectorTab.value = 'properties'
}

function handleInlineTextUpdate({ blockId, patch }) {
  if (!blockId || !patch)
    return
  updatePageBlocks(pageBlocks.value.map(item => item.id === blockId
    ? { ...item, props: { ...(item.props || {}), ...patch } }
    : item))
}

function resolvePagePreviewBlock(block) {
  return {
    ...block,
    props: {
      ...(block.props || {}),
      style: {
        ...(block.props?.style || {}),
        width: '100%',
        height: '100%',
        minHeight: '',
        maxHeight: '',
        margin: 0,
      },
    },
  }
}

function handleComponentCatalogDragStart(event, component) {
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-forge-app-page-block', component.blockType)
}

function handlePageDrop(event) {
  const blockType = event.dataTransfer.getData('application/x-forge-app-page-block')
  if (blockType)
    appendPageBlock(blockType)
}

function resolvePageBlockMoreOptions(block = {}) {
  const borderWidth = Number(block.props?.style?.borderWidth || 0)
  const borderMode = itemBorderColorMode(block)
  const isThemeBorder = borderMode === 'theme' && borderWidth > 0
  const menuLabel = (label, active) => active ? `✓  ${label}` : label
  const moveIntoOptions = resolvePageBlockMoveIntoOptions(block)
  return [
    { label: '配置', key: 'configure', icon: () => renderNavigationMenuIcon(SettingsOutline) },
    { type: 'divider', key: 'configureDivider' },
    { label: '复制到当前页面', key: 'duplicate', icon: () => renderNavigationMenuIcon(DuplicateOutline) },
    { label: '复制到其他页面', key: 'copyToPage', disabled: !copyBlockPageOptions.value.length, icon: () => renderNavigationMenuIcon(CopyOutline) },
    {
      label: '移入布局',
      key: 'moveIntoLayout',
      icon: () => renderNavigationMenuIcon(GitBranchOutline),
      children: moveIntoOptions.length
        ? moveIntoOptions
        : [{ label: '暂无布局组合和标签页', key: 'moveInto:empty', disabled: true }],
    },
    {
      label: '尺寸',
      key: 'size',
      icon: () => renderNavigationMenuIcon(SettingsOutline),
      children: [
        { type: 'group', label: '宽度', key: 'widthGroup', children: ['auto', 'full', 'fixed'].map(mode => ({ label: menuLabel({ auto: '默认宽度', full: '填充容器', fixed: '固定宽度' }[mode], (block.props?.style?.widthMode || 'full') === mode), key: `size:width:${mode}`, icon: () => renderNavigationMenuIcon(mode === 'auto' ? RemoveOutline : mode === 'full' ? SwapHorizontalOutline : ResizeOutline) })) },
        { type: 'group', label: '高度', key: 'heightGroup', children: ['fixed', 'auto', 'full'].map(mode => ({ label: menuLabel({ fixed: '默认高度', auto: '适应内容', full: '填充容器' }[mode], (block.props?.style?.heightMode || 'fixed') === mode), key: `size:height:${mode}`, icon: () => renderNavigationMenuIcon(mode === 'auto' ? ResizeOutline : mode === 'full' ? ExpandOutline : RemoveOutline) })) },
      ],
    },
    {
      label: '更换背景色',
      key: 'backgroundColor',
      icon: () => renderNavigationMenuIcon(ColorFillOutline),
    },
    {
      label: '背景描边',
      key: 'backgroundBorder',
      icon: () => renderNavigationMenuIcon(SquareOutline),
      children: [
        { label: menuLabel('跟随主题', isThemeBorder), key: 'border:theme', icon: () => renderNavigationMenuIcon(SettingsOutline) },
        {
          label: '粗细',
          key: 'borderWidth',
          children: [
            { label: menuLabel('跟随主题', isThemeBorder), key: 'borderWidth:theme', icon: () => renderNavigationMenuIcon(SettingsOutline) },
            ...[0, 0.5, 1, 2, 3, 4].map(width => ({ label: menuLabel(`${width} px`, borderWidth === width && borderMode !== 'theme'), key: `borderWidth:${width}`, icon: () => renderNavigationMenuIcon(RemoveOutline) })),
          ],
        },
        {
          label: '颜色',
          key: 'borderColor',
          children: [
            { label: menuLabel('跟随主题', borderMode === 'theme'), key: 'border:theme', icon: () => renderNavigationMenuIcon(SettingsOutline) },
            { label: menuLabel('浅灰', borderMode === '#d0d3d8'), key: 'border:#d0d3d8', icon: () => renderNavigationMenuIcon(SquareOutline) },
            { label: menuLabel('深灰', borderMode === '#86909c'), key: 'border:#86909c', icon: () => renderNavigationMenuIcon(SquareOutline) },
            { label: menuLabel('蓝色', borderMode === '#3370ff'), key: 'border:#3370ff', icon: () => renderNavigationMenuIcon(SquareOutline) },
          ],
        },
      ],
    },
    { type: 'divider', key: 'dangerDivider' },
    { label: '删除', key: 'delete', icon: () => renderNavigationMenuIcon(TrashOutline) },
  ]
}

function handlePageBlockMoreSelect(key, block) {
  const index = pageBlocks.value.findIndex(item => item.id === block.id)
  if (index < 0)
    return
  selectPageBlock(block.id)
  if (key === 'configure') {
    configPanelVisible.value = true
    return
  }
  if (key === 'copyToPage') {
    copyBlockId.value = block.id
    copyBlockTargetPageId.value = ''
    copyBlockVisible.value = true
    return
  }
  if (key.startsWith('moveInto:')) {
    movePageBlockIntoContainer(block.id, key.slice('moveInto:'.length))
    return
  }
  if (key === 'backgroundColor') {
    backgroundPickerBlockId.value = block.id
    blockBackgroundPickerVisible.value = true
    return
  }
  if (key.startsWith('size:')) {
    updatePageBlockSize(block, key)
    return
  }
  if (key.startsWith('background:')) {
    updateSelectedBlockAppearance({ backgroundColor: key.slice('background:'.length) || 'transparent' })
    return
  }
  if (key.startsWith('borderWidth:')) {
    const value = key.slice('borderWidth:'.length)
    updateSelectedBlockAppearance(value === 'theme'
      ? { borderColor: 'theme', borderWidth: 1 }
      : { borderWidth: Number(value) || 0 })
    return
  }
  if (key.startsWith('border:')) {
    updateSelectedBlockAppearance({ borderColor: key.slice('border:'.length) || 'theme' })
    return
  }
  const items = [...pageBlocks.value]
  if (key === 'duplicate') {
    const copy = JSON.parse(JSON.stringify(block))
    copy.id = `${block.blockType}_${Date.now()}`
    copy.label = `${block.label || resolveListPageBlockMeta(block.blockType)?.title || '区块'} 副本`
    items.splice(index + 1, 0, copy)
    updatePageBlocks(items)
    selectedPageBlockId.value = copy.id
    return
  }
  if (key === 'delete') {
    items.splice(index, 1)
    updatePageBlocks(items)
    selectedPageBlockId.value = ''
    return
  }
  updatePageBlocks(items)
}

function resolvePageBlockMoveIntoOptions(block = {}) {
  return pageBlocks.value
    .filter((candidate) => {
      if (!candidate?.id || candidate.id === block.id)
        return false
      const meta = resolveListPageBlockMeta(candidate.blockType)
      return meta?.container === true && ['grid-layout', 'box-layout', 'card', 'tabs'].includes(candidate.blockType)
    })
    .map((candidate) => {
      const meta = resolveListPageBlockMeta(candidate.blockType)
      return {
        label: `${meta?.title || '布局组合'} · ${candidate.label || meta?.title || '未命名布局'}`,
        key: `moveInto:${candidate.id}`,
        icon: () => renderNavigationMenuIcon(candidate.blockType === 'tabs' ? DocumentTextOutline : MoveOutline),
      }
    })
}

function movePageBlockIntoContainer(blockId, containerId) {
  if (!blockId || !containerId || blockId === containerId)
    return
  const source = pageBlocks.value.find(item => item.id === blockId)
  const container = pageBlocks.value.find(item => item.id === containerId)
  if (!source || !container)
    return

  const nested = normalizePageBlockForContainer(source)
  const nextItems = pageBlocks.value
    .filter(item => item.id !== blockId)
    .map((item) => {
      if (item.id !== containerId)
        return item
      if (item.blockType === 'grid-layout') {
        const cells = Array.isArray(item.props?.cells) && item.props.cells.length
          ? item.props.cells.map(cell => ({ ...cell, children: [...(cell.children || [])] }))
          : [{ key: 'cell_1', title: '栅格 1', span: 24, children: [] }]
        cells[0] = { ...cells[0], children: [...cells[0].children, nested] }
        return { ...item, props: { ...(item.props || {}), cells } }
      }
      if (item.blockType === 'tabs') {
        const tabs = Array.isArray(item.props?.tabs) && item.props.tabs.length
          ? item.props.tabs.map(tab => ({ ...tab, children: [...(tab.children || [])] }))
          : [{ key: 'tab1', title: '标签一', children: [] }]
        tabs[0] = { ...tabs[0], children: [...tabs[0].children, nested] }
        return { ...item, props: { ...(item.props || {}), tabs } }
      }
      return { ...item, children: [...(item.children || []), nested] }
    })
  updatePageBlocks(nextItems, { resolveCollisions: true, changedBlockId: containerId })
  selectedPageBlockId.value = containerId
  message.success(`组件已移入${container.label || resolveListPageBlockMeta(container.blockType)?.title || '布局组合'}`)
}

function normalizePageBlockForContainer(block) {
  const { pageFlowX, pageFlowY, pageFlowWidth, pageFlowHeight, ...containerStyle } = block.props?.style || {}
  return {
    ...JSON.parse(JSON.stringify(block)),
    props: {
      ...(block.props || {}),
      style: {
        ...containerStyle,
        widthMode: 'full',
        width: '100%',
      },
    },
  }
}

function updatePageBlockSize(block, key) {
  const [, axis, mode] = key.split(':')
  const style = block.props?.style || {}
  const nextStyle = { ...style }
  if (axis === 'width') {
    nextStyle.widthMode = mode
    nextStyle.width = mode === 'full' ? '100%' : mode === 'auto' ? 'auto' : Math.max(280, Math.min(640, readPageBlockLength(style.width, 640)))
    nextStyle.pageFlowWidth = mode === 'full' ? 'calc(100% - 48px)' : `${readPageBlockLength(nextStyle.width, mode === 'auto' ? 520 : 640)}px`
  }
  if (axis === 'height')
    nextStyle.heightMode = mode
  updatePageBlocks(
    pageBlocks.value.map(item => item.id === block.id ? { ...item, props: { ...(item.props || {}), style: nextStyle } } : item),
    { resolveCollisions: true, changedBlockId: block.id },
  )
}

function resolvePageBlockBackgroundColor(block = {}) {
  const value = String(block.props?.style?.backgroundColor || '').trim()
  return value && value !== 'transparent' ? value : '#FFFFFF00'
}

function updatePageBlockBackgroundColor(block, value) {
  if (!block?.id)
    return
  selectPageBlock(block.id)
  updateSelectedBlockAppearance({ backgroundColor: value || 'transparent' })
}

function copySelectedBlockToOtherPage() {
  const block = pageBlocks.value.find(item => item.id === copyBlockId.value)
  const targetPage = builder.value?.pages?.[copyBlockTargetPageId.value]
  if (!block || !targetPage)
    return
  const targetLayout = targetPage.layout?.gridLayout || {
    cols: 12,
    rowHeight: 32,
    gap: 8,
    designWidth: 1366,
    layoutType: 'simple-crud',
    items: [],
  }
  const copy = JSON.parse(JSON.stringify(block))
  copy.id = `${block.blockType}_${Date.now()}`
  copy.label = `${block.label || resolveListPageBlockMeta(block.blockType)?.title || '组件'} 副本`
  const targetItems = [...(targetLayout.items || []), copy]
  builder.value = {
    ...builder.value,
    pages: {
      ...builder.value.pages,
      [copyBlockTargetPageId.value]: {
        ...targetPage,
        layout: { ...targetPage.layout, gridLayout: { ...targetLayout, items: targetItems } },
      },
    },
  }
  copyBlockVisible.value = false
  message.success('组件已复制到目标页面')
}

function updateSelectedBlockAppearance(patch = {}) {
  if (!selectedPageBlock.value)
    return
  const borderColorMode = patch.borderColor || itemBorderColorMode(selectedPageBlock.value)
  const normalizeBorderColor = borderColorMode === 'theme' ? 'var(--primary-color, #3370ff)' : borderColorMode
  updatePageBlocks(pageBlocks.value.map(item => item.id === selectedPageBlock.value.id
    ? {
        ...item,
        props: {
          ...(item.props || {}),
          style: {
            ...(item.props?.style || {}),
            ...patch,
            ...(Object.prototype.hasOwnProperty.call(patch, 'borderColor')
              ? { borderColor: normalizeBorderColor, borderColorMode }
              : {}),
            borderStyle: Number(patch.borderWidth ?? item.props?.style?.borderWidth ?? 0) > 0 ? 'solid' : 'none',
          },
        },
      }
    : item))
}

function itemBorderColorMode(block = {}) {
  return block.props?.style?.borderColorMode || (block.props?.style?.borderColor === 'var(--primary-color, #3370ff)' ? 'theme' : block.props?.style?.borderColor) || 'theme'
}

let pageBlockResizeCtx = null
function startPageBlockResize(block, event, anchor = 'bottom-right') {
  if (event.button !== 0)
    return
  const node = event.currentTarget.closest('[data-page-block-id]')
  const flow = node?.parentElement
  const rect = node?.getBoundingClientRect?.()
  const flowRect = flow?.getBoundingClientRect?.()
  if (!rect || !flowRect)
    return
  event.preventDefault()
  selectPageBlock(block.id)
  pageBlockResizeCtx = {
    blockId: block.id,
    anchor,
    startX: event.clientX,
    startY: event.clientY,
    originWidth: rect.width,
    originHeight: rect.height,
    originX: rect.left - flowRect.left + (flow.scrollLeft || 0),
    originY: rect.top - flowRect.top + (flow.scrollTop || 0),
    maxWidth: Math.max(240, flowRect.width - 24),
  }
  window.addEventListener('pointermove', onPageBlockResize)
  window.addEventListener('pointerup', endPageBlockResize)
}

function onPageBlockResize(event) {
  if (!pageBlockResizeCtx)
    return
  const ctx = pageBlockResizeCtx
  const widthDelta = event.clientX - ctx.startX
  const heightDelta = event.clientY - ctx.startY
  const anchor = ctx.anchor || 'bottom-right'
  let width = ctx.originWidth
  let height = ctx.originHeight
  let pageFlowX = ctx.originX
  let pageFlowY = ctx.originY
  if (anchor.includes('right'))
    width = ctx.originWidth + widthDelta
  if (anchor.includes('left')) {
    width = ctx.originWidth - widthDelta
    pageFlowX = ctx.originX + widthDelta
  }
  if (anchor.includes('bottom'))
    height = ctx.originHeight + heightDelta
  if (anchor.includes('top')) {
    height = ctx.originHeight - heightDelta
    pageFlowY = ctx.originY + heightDelta
  }
  const pageFlowWidth = `${Math.round(Math.min(ctx.maxWidth, Math.max(180, width)))}px`
  const pageFlowHeight = Math.round(Math.max(56, height))
  updatePageBlocks(pageBlocks.value.map((item) => {
    if (item.id !== ctx.blockId)
      return item
    return {
      ...item,
      props: {
        ...(item.props || {}),
        style: {
          ...(item.props?.style || {}),
          pageFlowWidth,
          pageFlowHeight,
          pageFlowX: Math.round(Math.max(0, pageFlowX)),
          pageFlowY: Math.round(Math.max(0, pageFlowY)),
        },
      },
    }
  }))
}

function endPageBlockResize() {
  const resizedBlockId = pageBlockResizeCtx?.blockId || ''
  pageBlockResizeCtx = null
  window.removeEventListener('pointermove', onPageBlockResize)
  window.removeEventListener('pointerup', endPageBlockResize)
  if (resizedBlockId)
    updatePageBlocks(pageBlocks.value, { resolveCollisions: true, changedBlockId: resizedBlockId })
}

let pageBlockMoveCtx = null
let pageBlockMoveFrame = 0
let pendingPageBlockMoveEvent = null
function startPageBlockMove(block, event) {
  if (event.button !== 0)
    return
  const node = event.currentTarget.closest('[data-page-block-id]')
  const flow = node?.parentElement
  const rect = node?.getBoundingClientRect?.()
  const flowRect = flow?.getBoundingClientRect?.()
  if (!rect || !flowRect)
    return
  event.preventDefault()
  selectPageBlock(block.id)
  const originX = Math.round(rect.left - flowRect.left + (flow.scrollLeft || 0))
  const originY = Math.round(rect.top - flowRect.top + (flow.scrollTop || 0))
  pageBlockMoveCtx = {
    blockId: block.id,
    startX: event.clientX,
    startY: event.clientY,
    originX,
    originY,
    originClientLeft: rect.left,
    originClientTop: rect.top,
    width: rect.width,
    height: rect.height,
    activeSwapTargetId: '',
    blockSlots: new Map(pageBlocks.value.map((item) => {
      const style = resolvePageBlockShellStyle(item)
      return [item.id, {
        x: readPageBlockLength(item.props?.style?.pageFlowX, style.left),
        y: readPageBlockLength(item.props?.style?.pageFlowY, style.top),
      }]
    })),
    maxX: Math.max(0, flowRect.width - rect.width),
    maxY: Math.max(0, flowRect.height - rect.height),
  }
  dragPreview.value = { blockId: block.id, x: originX, y: originY, width: rect.width, height: rect.height }
  draggingPageBlockId.value = block.id
  window.addEventListener('pointermove', onPageBlockMove)
  window.addEventListener('pointerup', endPageBlockMove)
}

function onPageBlockMove(event) {
  pendingPageBlockMoveEvent = event
  if (pageBlockMoveFrame)
    return
  pageBlockMoveFrame = window.requestAnimationFrame(() => {
    pageBlockMoveFrame = 0
    const nextEvent = pendingPageBlockMoveEvent
    pendingPageBlockMoveEvent = null
    if (nextEvent)
      applyPageBlockMove(nextEvent)
  })
}

function applyPageBlockMove(event) {
  if (!pageBlockMoveCtx)
    return
  const ctx = pageBlockMoveCtx
  const pageFlowX = Math.round(Math.max(0, Math.min(ctx.maxX, ctx.originX + event.clientX - ctx.startX)))
  const pageFlowY = Math.round(Math.max(0, ctx.originY + event.clientY - ctx.startY))
  dragPreview.value = { ...dragPreview.value, x: pageFlowX, y: pageFlowY }
  const targetId = resolvePageBlockSwapTarget(ctx.blockId, {
    left: ctx.originClientLeft + event.clientX - ctx.startX,
    top: ctx.originClientTop + event.clientY - ctx.startY,
    right: ctx.originClientLeft + event.clientX - ctx.startX + ctx.width,
    bottom: ctx.originClientTop + event.clientY - ctx.startY + ctx.height,
  })
  if (targetId && targetId !== ctx.activeSwapTargetId) {
    applyPageBlockSwapPreview(ctx, targetId)
  }
  else if (!targetId) {
    clearPageBlockSwapPreview(ctx)
  }
}

function applyPageBlockSwapPreview(ctx, targetId) {
  const previousTargetId = ctx.activeSwapTargetId
  const targetNode = document.querySelector(`[data-page-block-id="${targetId}"]`)
  const previousTargetNode = previousTargetId ? document.querySelector(`[data-page-block-id="${previousTargetId}"]`) : null
  const targetRect = targetNode?.getBoundingClientRect?.()
  const previousTargetRect = previousTargetNode?.getBoundingClientRect?.()
  const originSlot = ctx.blockSlots.get(ctx.blockId)
  const previousTargetSlot = previousTargetId ? ctx.blockSlots.get(previousTargetId) : null
  if (!originSlot)
    return
  updatePageBlocks(pageBlocks.value.map((item) => {
    if (item.id === previousTargetId && previousTargetSlot) {
      return {
        ...item,
        props: { ...(item.props || {}), style: { ...(item.props?.style || {}), pageFlowX: previousTargetSlot.x, pageFlowY: previousTargetSlot.y } },
      }
    }
    if (item.id === targetId) {
      return {
        ...item,
        props: { ...(item.props || {}), style: { ...(item.props?.style || {}), pageFlowX: originSlot.x, pageFlowY: originSlot.y } },
      }
    }
    return item
  }))
  if (previousTargetId && previousTargetRect)
    animatePageBlockSwap(previousTargetId, previousTargetRect)
  if (targetRect)
    animatePageBlockSwap(targetId, targetRect)
  ctx.activeSwapTargetId = targetId
}

function clearPageBlockSwapPreview(ctx) {
  if (!ctx.activeSwapTargetId)
    return
  const targetId = ctx.activeSwapTargetId
  const targetNode = document.querySelector(`[data-page-block-id="${targetId}"]`)
  const targetRect = targetNode?.getBoundingClientRect?.()
  const targetSlot = ctx.blockSlots.get(targetId)
  if (targetSlot) {
    updatePageBlocks(pageBlocks.value.map(item => item.id === targetId
      ? { ...item, props: { ...(item.props || {}), style: { ...(item.props?.style || {}), pageFlowX: targetSlot.x, pageFlowY: targetSlot.y } } }
      : item))
  }
  if (targetRect)
    animatePageBlockSwap(targetId, targetRect)
  ctx.activeSwapTargetId = ''
}

function animatePageBlockSwap(blockId, previousRect) {
  window.requestAnimationFrame(() => {
    const node = document.querySelector(`[data-page-block-id="${blockId}"]`)
    const nextRect = node?.getBoundingClientRect?.()
    if (!node || !nextRect)
      return
    const deltaX = previousRect.left - nextRect.left
    const deltaY = previousRect.top - nextRect.top
    if (Math.abs(deltaX) < 1 && Math.abs(deltaY) < 1)
      return
    node.animate([
      { transform: `translate(${deltaX}px, ${deltaY}px)` },
      { transform: 'translate(0, 0)' },
    ], {
      duration: 280,
      easing: 'cubic-bezier(0.22, 0.8, 0.24, 1)',
      fill: 'both',
    })
  })
}

function endPageBlockMove() {
  if (pageBlockMoveFrame) {
    window.cancelAnimationFrame(pageBlockMoveFrame)
    pageBlockMoveFrame = 0
    pendingPageBlockMoveEvent = null
  }
  const ctx = pageBlockMoveCtx
  if (ctx) {
    const targetSlot = ctx.activeSwapTargetId ? ctx.blockSlots.get(ctx.activeSwapTargetId) : null
    const finalX = targetSlot?.x ?? Math.round(dragPreview.value?.x ?? ctx.originX)
    const finalY = targetSlot?.y ?? Math.round(dragPreview.value?.y ?? ctx.originY)
    updatePageBlocks(
      pageBlocks.value.map(item => item.id === ctx.blockId
        ? { ...item, props: { ...(item.props || {}), style: { ...(item.props?.style || {}), pageFlowX: finalX, pageFlowY: finalY } } }
        : item),
      { resolveCollisions: true, changedBlockId: ctx.blockId },
    )
  }
  pageBlockMoveCtx = null
  draggingPageBlockId.value = ''
  dragPreview.value = null
  window.removeEventListener('pointermove', onPageBlockMove)
  window.removeEventListener('pointerup', endPageBlockMove)
}

function resolvePageBlockSwapTarget(blockId, movingRectOverride) {
  const movingNode = document.querySelector(`[data-page-block-id="${blockId}"]`)
  const movingRect = movingRectOverride || movingNode?.getBoundingClientRect?.()
  if (!movingRect)
    return ''
  let matchedId = ''
  let maxArea = 0
  document.querySelectorAll('[data-page-block-id]').forEach((node) => {
    const targetId = node.dataset.pageBlockId
    if (!targetId || targetId === blockId)
      return
    const rect = node.getBoundingClientRect()
    const overlapWidth = Math.max(0, Math.min(movingRect.right, rect.right) - Math.max(movingRect.left, rect.left))
    const overlapHeight = Math.max(0, Math.min(movingRect.bottom, rect.bottom) - Math.max(movingRect.top, rect.top))
    const area = overlapWidth * overlapHeight
    if (area > maxArea) {
      maxArea = area
      matchedId = targetId
    }
  })
  return maxArea >= 900 ? matchedId : ''
}

function startComponentButtonMove(event) {
  if (event.button !== 0)
    return
  const button = event.currentTarget
  const host = button.closest('.page-surface')
  const buttonRect = button.getBoundingClientRect()
  const hostRect = host?.getBoundingClientRect()
  if (!hostRect)
    return
  componentButtonMoveCtx.value = {
    host,
    button,
    startX: event.clientX,
    startY: event.clientY,
    originTop: buttonRect.top - hostRect.top,
    originLeft: buttonRect.left - hostRect.left,
  }
  window.addEventListener('pointermove', onComponentButtonMove)
  window.addEventListener('pointerup', endComponentButtonMove)
}

function onComponentButtonMove(event) {
  if (!componentButtonMoveCtx.value)
    return
  const nextTop = componentButtonMoveCtx.value.originTop + event.clientY - componentButtonMoveCtx.value.startY
  const nextLeft = componentButtonMoveCtx.value.originLeft + event.clientX - componentButtonMoveCtx.value.startX
  const hostRect = componentButtonMoveCtx.value.host?.getBoundingClientRect()
  const buttonRect = componentButtonMoveCtx.value.button?.getBoundingClientRect()
  const maxX = Math.max(12, (hostRect?.width || 0) - (buttonRect?.width || 44) - 12)
  const maxY = Math.max(12, (hostRect?.height || pageFlowHeight.value) - (buttonRect?.height || 44) - 12)
  componentButtonPosition.value = {
    x: Math.round(Math.max(12, Math.min(maxX, nextLeft))),
    y: Math.round(Math.max(12, Math.min(maxY, nextTop))),
  }
}

function endComponentButtonMove() {
  componentButtonMoveCtx.value = null
  window.removeEventListener('pointermove', onComponentButtonMove)
  window.removeEventListener('pointerup', endComponentButtonMove)
}

function createLegacyBlock(item, index) {
  const block = createGridBlock(legacyBlockTypeMap[item.componentKey] || 'info-panel', { fields: [] }, { gridX: index % 2 ? 6 : 0, gridY: Math.floor(index / 2) * 4 })
  if (!block)
    return null
  return { ...block, label: item.props?.title || item.label || block.label, props: { ...block.props, title: item.props?.title || item.label || block.label, subtitle: item.props?.description || item.props?.subtitle || item.props?.content || '' } }
}

async function saveDraft() {
  if (!application.value || saving.value)
    return false
  saving.value = true
  try {
    const options = mergeInAppBuilderOptions(application.value.options, builder.value)
    await updateBusinessApplication({ id: application.value.id, applicationCode: application.value.applicationCode, applicationName: application.value.applicationName, suiteCode: application.value.suiteCode, icon: application.value.icon, description: application.value.description, status: application.value.status, options: JSON.stringify(options) })
    application.value.options = JSON.stringify(options)
    savedSignature.value = JSON.stringify(builder.value)
    message.success('应用草稿已保存')
    return true
  }
  catch {
    message.error('草稿保存失败，请稍后重试')
    return false
  }
  finally { saving.value = false }
}

function requestExitEditing() {
  if (dirty.value) {
    exitEditingVisible.value = true
    return
  }
  editing.value = false
}

function handleRuntimeHeaderMoreSelect(key) {
  if (key === 'object-design') {
    openBusinessObjectDesign()
    return
  }
  if (key === 'preview-draft') {
    openDraftPreview()
    return
  }
  if (key === 'exit-editing')
    requestExitEditing()
}

function discardAndExitEditing() {
  historyReady.value = false
  builder.value = ensurePageTitleComponents(normalizeInAppBuilder(application.value?.options, application.value, objects.value))
  savedSignature.value = JSON.stringify(builder.value)
  resetBuilderHistory(builder.value)
  selectedPageBlockId.value = ''
  exitEditingVisible.value = false
  editing.value = false
}

function openWorkspace() {
  router.push({ name: 'BusinessApplicationWorkspace', params: { applicationCode: application.value.applicationCode } })
}

async function openDraftPreview() {
  if (dirty.value && !await saveDraft())
    return
  const target = router.resolve({
    name: 'BusinessApplicationRuntime',
    params: { applicationCode: application.value.applicationCode },
    query: { pageId: selectedNodeId.value, draft: '1' },
  })
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function openPublishPanel() {
  router.push({
    name: 'BusinessApplicationWorkspace',
    params: { applicationCode: application.value.applicationCode },
    query: { section: 'releases', publish: '1' },
  })
}

function openObjectDesigner(panel = 'list', targetObjectRef) {
  const objectRef = targetObjectRef || currentNode.value?.objectRef || selectedPageBlockRuntimeObjectRef.value
  if (!objectRef?.objectCode)
    return
  const detailTab = panel === 'detail' ? 'detail' : panel === 'form' ? 'form' : 'list'
  router.push({
    name: 'BusinessObjectDesigner',
    params: { objectCode: objectRef.objectCode },
    query: {
      objectId: objectRef.objectId,
      panel,
      detailTab,
      returnTo: route.fullPath,
    },
  })
}

function openBusinessObjectDesign() {
  const objectRef = currentNode.value?.objectRef || selectedPageBlockRuntimeObjectRef.value
  if (objectRef?.objectCode) {
    openObjectDesigner('fields', objectRef)
    return
  }
  router.push({
    name: 'BusinessApplicationWorkspace',
    params: { applicationCode: application.value.applicationCode },
    query: { section: 'objects' },
  })
}

function flattenNodes(nodes, parentId = null, depth = 0) {
  return nodes.filter(item => item.parentId === parentId).sort((a, b) => a.sort - b.sort).flatMap(item => [{ ...item, depth }, ...flattenNodes(nodes, item.id, depth + 1)])
}

function hasPermission(source, permission) {
  return Array.isArray(source) && (source.includes(permission) || source.includes('**') || source.includes('*:*:*'))
}
</script>

<style scoped>
.application-runtime-page {
  min-height: 100%;
  background: #f7f8fa;
  color: #1f2329;
}
.runtime-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 14px;
  border-bottom: 1px solid #e5e6eb;
  background: #fff;
}
.runtime-brand,
.runtime-brand-copy {
  display: flex;
  align-items: center;
}
.runtime-brand {
  gap: 6px;
  min-width: 0;
}
.runtime-brand-copy {
  min-width: 0;
  gap: 7px;
}
.runtime-design-title {
  display: grid;
  min-width: 0;
  gap: 1px;
}
.runtime-design-title > span {
  color: #646a73;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.04em;
  line-height: 12px;
}
.runtime-breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 180px;
  overflow: hidden;
  border: 0;
  background: transparent;
  color: #646a73;
  cursor: pointer;
  font-size: 13px;
  line-height: 24px;
  white-space: nowrap;
}
.runtime-breadcrumb:hover {
  color: #1f2329;
}
.runtime-brand-app-icon {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 6px;
  background: #e8f3ec;
  color: #1f8c54;
  font-size: 14px;
}
.runtime-brand-copy strong {
  overflow: hidden;
  max-width: 220px;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1f2329;
  font-size: 14px;
}
.runtime-brand-status {
  overflow: hidden;
  max-width: 150px;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #8f959e;
  font-size: 12px;
}
.runtime-body {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  height: calc(100vh - 56px);
  min-height: 0;
  overflow: hidden;
}
.runtime-body.configuring {
  grid-template-columns: 232px minmax(0, 1fr) 408px;
}
.runtime-body.sidebar-collapsed {
  grid-template-columns: 64px minmax(0, 1fr);
}
.runtime-body.sidebar-collapsed.configuring {
  grid-template-columns: 64px minmax(0, 1fr) 408px;
}
.runtime-body.headerless {
  height: 100vh;
}
.runtime-navigation {
  position: relative;
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 1px solid #e5e6eb;
  background: #fff;
}
.application-sidebar-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  height: 50px;
  padding: 0 12px;
  border-bottom: 1px solid #f2f3f5;
  font-size: 14px;
}
.base-app-title-wrapper {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
}
.base-app-title-content {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sidebar-title-actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 2px;
}
.sidebar-edit-trigger,
.sidebar-collapse-hint {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #8f959e;
  font-size: 18px;
  cursor: pointer;
}
.sidebar-edit-trigger {
  color: #4e5969;
  font-size: 16px;
}
.sidebar-edit-trigger:hover,
.sidebar-collapse-hint:hover {
  background: #f2f3f5;
  color: #1f2329;
}
.application-icon-slot,
.navigation-icon-slot,
.component-icon-slot {
  flex: 0 0 auto;
  border: 1px dashed #c9d2df;
  background: #f7f8fa;
}
.application-icon-slot {
  display: grid;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  overflow: hidden;
  border: 0;
  background: transparent;
  place-items: center;
}
.application-icon-slot :deep(img) {
  display: block;
  width: 18px;
  height: 18px;
  object-fit: contain;
}
.runtime-navigation.collapsed .base-app-title-content,
.runtime-navigation.collapsed .navigation-page > span:last-child,
.runtime-navigation.collapsed .navigation-group,
.runtime-navigation.collapsed .navigation-create {
  display: none;
}
.runtime-navigation.collapsed .application-sidebar-title {
  justify-content: center;
  padding: 0;
}
.runtime-navigation.collapsed .base-app-title-wrapper {
  display: none;
}
.runtime-navigation.collapsed .navigation-row {
  justify-content: center;
  padding-left: 0 !important;
}
.runtime-navigation.collapsed .navigation-page {
  justify-content: center;
}
.runtime-navigation.collapsed .navigation-more {
  display: none;
}
.navigation-list {
  min-height: 0;
  max-height: min(62vh, calc(100vh - 168px));
  overflow: auto;
  padding: 8px 6px;
}
.navigation-page,
.navigation-create {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 32px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #4e5969;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}
.navigation-page {
  gap: 8px;
  flex: 1;
  min-width: 0;
  padding: 0 6px;
}
.navigation-page:hover,
.navigation-page.active,
.navigation-create:hover {
  background: #f2f3f5;
  color: #165dff;
}
.navigation-icon-slot {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  display: inline-grid;
  place-items: center;
  font-size: 14px;
  line-height: 1;
}
.navigation-icon-slot:not(.empty) {
  border-color: transparent;
  background: transparent;
}
.navigation-row {
  display: flex;
  align-items: center;
  min-height: 32px;
  border-radius: 5px;
}
.navigation-row:hover {
  background: #f7f8fa;
}
.navigation-row.base-app-sidebar__node_selected {
  background: #f2f3f5;
  color: #1f2329;
}
.navigation-row.base-app-sidebar__node_selected .navigation-page {
  color: #1f2329;
}
.navigation-group {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #86909c;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.navigation-more {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #86909c;
  cursor: pointer;
}
.navigation-more:hover {
  background: #f2f3f5;
  color: #1f2329;
}
.navigation-create {
  gap: 8px;
  flex: 0 0 auto;
  justify-content: center;
  padding: 0;
  background: #f2f3f5;
  color: #4e5969;
}
.new_node_wrapper {
  padding: 4px 6px 8px;
}
.new-node-popover {
  display: grid;
  width: 260px;
  gap: 4px;
  padding: 4px;
}
.new-node-choice {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 9px;
  width: 100%;
  align-items: center;
  border: 0;
  border-radius: 7px;
  padding: 9px;
  background: transparent;
  color: #1f2329;
  text-align: left;
  cursor: pointer;
}
.new-node-choice:hover {
  background: #f2f6ff;
}
.new-node-choice-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 7px;
  background: #edf4ff;
  color: #3370ff;
  font-size: 18px;
}
.new-node-choice-icon.group {
  background: #f1f3f5;
  color: #646a73;
}
.new-node-choice > span:last-child {
  display: grid;
  min-width: 0;
  gap: 2px;
}
.new-node-choice strong {
  color: #1f2329;
  font-size: 13px;
  font-weight: 600;
}
.new-node-choice small {
  overflow: hidden;
  color: #86909c;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.application-template-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}
.application-template-card {
  display: grid;
  min-width: 0;
  min-height: 160px;
  grid-template-rows: 36px minmax(0, 1fr);
  align-content: start;
  gap: 12px;
  border: 1px solid #e7ebf1;
  border-radius: 10px;
  padding: 14px;
  background: #fff;
  color: #1f2329;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    transform 160ms ease,
    box-shadow 160ms ease;
}
.application-template-card:hover,
.application-template-card.selected {
  border-color: #8db4f8;
  background: #fbfdff;
  box-shadow: 0 8px 18px rgba(38, 91, 184, 0.08);
  transform: translateY(-2px);
}
.application-template-card > span:last-child {
  display: grid;
  min-width: 0;
  align-content: start;
  gap: 5px;
}
.application-template-card strong {
  color: #1f2329;
  font-size: 13px;
  font-weight: 600;
}
.application-template-card small {
  display: -webkit-box;
  overflow: hidden;
  color: #86909c;
  font-size: 12px;
  line-height: 18px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.application-template-card em {
  align-self: end;
  margin-top: auto;
  color: #3370ff;
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
}
.application-template-card em i {
  display: inline-block;
  margin-left: 2px;
  font-style: normal;
  transition: transform 160ms ease;
}
.application-template-card:hover em i {
  transform: translateX(3px);
}
.application-template-icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 9px;
  background: #edf4ff;
  color: #3370ff;
  font-size: 18px;
}
.application-template-icon.kind-intro {
  background: #f1f3f5;
  color: #4e5969;
}
.application-template-icon.kind-tree-table {
  background: #e9f8f1;
  color: #25816a;
}
.application-template-icon.kind-master-detail {
  background: #fff7e8;
  color: #ad7b2d;
}
.navigation-icon-picker {
  position: absolute;
  z-index: 12;
  right: -268px;
  bottom: 8px;
  width: 252px;
  padding: 10px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(31, 35, 41, 0.14);
}
.navigation-icon-picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #1f2329;
  font-size: 13px;
  font-weight: 600;
}
.navigation-icon-picker-head button {
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #8f959e;
  font-size: 18px;
  cursor: pointer;
}
.navigation-menu-icon {
  display: inline-grid;
  width: 16px;
  height: 16px;
  place-items: center;
  color: #646a73;
  font-size: 15px;
  line-height: 1;
}
.navigation-create span {
  color: #165dff;
  font-size: 18px;
  font-weight: 400;
}
.canvas-component-anchor {
  position: absolute;
  z-index: 9;
  touch-action: none;
  cursor: move;
  transition: filter 160ms ease;
}
.canvas-component-anchor.moving {
  z-index: 8;
  cursor: grabbing;
}
.canvas-component-anchor.moving .component-add-trigger {
  filter: drop-shadow(0 9px 16px rgba(37, 99, 235, 0.28));
}
.component-add-trigger {
  display: inline-flex;
  align-items: center;
  width: 46px;
  height: 46px;
  gap: 0;
  overflow: hidden;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #3370ff;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(51, 112, 255, 0.24);
  transition:
    width 180ms ease,
    border-radius 180ms ease,
    box-shadow 180ms ease,
    background 180ms ease;
}
.component-add-trigger:hover,
.component-add-trigger:focus-visible {
  width: 116px;
  border-radius: 23px;
  background: #2864e9;
  box-shadow: 0 7px 16px rgba(51, 112, 255, 0.28);
  outline: 0;
}
.component-add-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  color: currentColor;
  font-size: 27px;
  font-weight: 400;
  line-height: 1;
}
.component-add-label {
  width: 0;
  overflow: hidden;
  opacity: 0;
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  white-space: nowrap;
  transition:
    width 160ms ease,
    opacity 120ms ease;
}
.component-add-trigger:hover .component-add-label,
.component-add-trigger:focus-visible .component-add-label {
  width: 62px;
  opacity: 1;
}
.component-popover {
  width: min(420px, calc(100vw - 24px));
  max-height: min(560px, calc(100vh - 112px));
  padding: 14px;
  overflow: auto;
  background: #fff;
}
.component-search-input {
  position: sticky;
  z-index: 1;
  top: -14px;
  display: block;
  margin-bottom: 4px;
  padding: 2px 0 18px;
  background: #fff;
}
.component-search-input :deep(.n-input) {
  min-height: 38px;
  border-radius: 8px;
  background: #f7f8fa;
  box-shadow: inset 0 0 0 1px #edf0f5;
}
.component-search-input :deep(.n-input.n-input--focus) {
  background: #fff;
  box-shadow: inset 0 0 0 1px #5d8ef7;
}
.component-picker-groups {
  display: grid;
  gap: 20px;
}
.component-picker-group h3 {
  margin: 0 0 8px;
  color: #646a73;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
}
.component-picker-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
.component-picker-grid button {
  display: grid;
  min-width: 0;
  justify-items: center;
  gap: 6px;
  padding: 5px 2px 6px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #1f2329;
  cursor: pointer;
}
.component-picker-grid button:hover {
  border-color: #d5e5ff;
  background: #f5f9ff;
  color: #1456f0;
}
.component-icon-slot {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid #dce9fb;
  border-radius: 10px;
  background: #edf5ff;
  color: #5d8ef7;
}
.component-icon-slot svg {
  width: 21px;
  height: 21px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
}
.component-icon-slot img {
  display: block;
  width: 32px;
  height: 32px;
  object-fit: contain;
}
.component-icon-slot.kind-chart {
  background: #eef7ff;
  color: #4c89d9;
}
.component-icon-slot.kind-view {
  background: #f2f7ff;
  color: #6a8fd7;
}
.component-icon-slot.kind-other {
  background: #f4f6f8;
  color: #7c8797;
}
.component-item-title {
  width: 100%;
  overflow: hidden;
  color: currentColor;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.runtime-main {
  min-width: 0;
  min-height: 0;
  padding: 12px;
  overflow: auto;
}
.application-empty-state {
  display: flex;
  min-height: 0;
  align-items: stretch;
  justify-content: flex-start;
  flex-direction: column;
  gap: 16px;
  padding: clamp(28px, 5vh, 56px) clamp(20px, 6vw, 84px);
  background:
    linear-gradient(90deg, rgba(228, 237, 252, 0.46) 1px, transparent 1px),
    linear-gradient(rgba(228, 237, 252, 0.46) 1px, transparent 1px), #f8fafc;
  background-size: 28px 28px;
  overflow: auto;
}
.application-empty-intro {
  display: flex;
  max-width: 1120px;
  align-items: end;
  justify-content: space-between;
  gap: 28px;
}
.application-empty-eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  border-left: 2px solid #3370ff;
  padding-left: 7px;
  color: #3370ff;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
}
.application-empty-intro h1 {
  margin: 8px 0 5px;
  color: #1f2329;
  font-size: 24px;
  font-weight: 650;
  line-height: 1.35;
}
.application-empty-intro p {
  margin: 0;
  color: #646a73;
  font-size: 13px;
  line-height: 22px;
}
.application-create-group-card {
  display: grid;
  grid-template-columns: 32px minmax(108px, 1fr) auto;
  min-width: 196px;
  align-items: center;
  gap: 9px;
  border: 1px solid #dfe6f0;
  border-radius: 9px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.86);
  color: #1f2329;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    transform 160ms ease;
}
.application-create-group-card:hover {
  border-color: #a7c3f5;
  background: #fff;
  transform: translateY(-1px);
}
.application-create-group-icon {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 7px;
  background: #eef3fa;
  color: #52657e;
  font-size: 17px;
}
.application-create-group-card > span:nth-child(2) {
  display: grid;
  gap: 1px;
}
.application-create-group-card strong {
  font-size: 12px;
  font-weight: 600;
}
.application-create-group-card small {
  color: #86909c;
  font-size: 11px;
}
.application-create-group-card > i {
  color: #8f9caf;
  font-size: 16px;
  font-style: normal;
}
.application-template-section,
.application-component-section {
  max-width: 1120px;
  border: 1px solid #e4e9f1;
  border-radius: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 28px rgba(41, 63, 95, 0.035);
}
.application-empty-section-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 14px;
}
.application-section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #1f2329;
  font-size: 14px;
  font-weight: 650;
}
.application-empty-section-head span {
  color: #86909c;
  font-size: 12px;
}
.application-template-object-binding {
  display: grid;
  grid-template-columns: auto minmax(180px, 280px) auto;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  border-top: 1px solid #f2f3f5;
  padding-top: 10px;
  color: #4e5969;
  font-size: 12px;
}
.application-template-object-empty-icon {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 7px;
  background: #eef4ff;
  color: #3370ff;
  font-size: 16px;
}
.application-template-object-empty-copy {
  display: grid;
  min-width: 0;
  gap: 2px;
}
.application-template-object-empty-copy strong {
  color: #1f2329;
  font-size: 12px;
  font-weight: 600;
}
.application-template-object-empty-copy small {
  overflow: hidden;
  color: #86909c;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.application-component-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}
.application-component-grid button {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
  border: 1px solid transparent;
  border-radius: 7px;
  padding: 7px 11px 7px 7px;
  background: #f5f7fa;
  color: #4e5969;
  font-size: 12px;
  cursor: pointer;
}
.application-component-grid button:hover {
  border-color: #cfe0fb;
  background: #eef5ff;
  color: #165dff;
}
.application-component-grid .empty-component-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  font-size: 14px;
}
.application-empty-readonly {
  color: #86909c;
  font-size: 13px;
}
.runtime-inspector {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 12px;
  border-left: 1px solid #e5e6eb;
  background: #fff;
  overflow: hidden;
}
.runtime-inspector :deep(.list-grid-designer.panel-only) {
  height: auto;
  min-height: 0;
  flex: 1;
}
.runtime-inspector :deep(.list-grid-designer.panel-only .panel-collapse-button),
.runtime-inspector :deep(.list-grid-designer.panel-only .side-rail-toggle-button.right) {
  display: none;
}
.runtime-inspector :deep(.list-grid-designer.panel-only .block-property-panel),
.runtime-inspector :deep(.list-grid-designer.panel-only .property-panel) {
  min-height: 0;
  overflow: auto;
}
.runtime-inspector-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 34px;
  margin: -2px 0 10px;
  border-bottom: 1px solid #e5e6eb;
}
.runtime-inspector-tabs {
  display: flex;
  align-items: stretch;
  align-self: stretch;
  gap: 2px;
}
.runtime-inspector-tabs button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 0;
  border-bottom: 2px solid transparent;
  padding: 0 7px;
  background: transparent;
  color: #86909c;
  font-size: 12px;
  cursor: pointer;
}
.runtime-inspector-tabs button:hover {
  color: #4e5969;
  background: #f7f8fa;
}
.runtime-inspector-tabs button.active {
  border-bottom-color: #3370ff;
  color: #1456f0;
  font-weight: 600;
}
.runtime-inspector-close {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border: 0;
  border-radius: 5px;
  padding: 0;
  background: transparent;
  color: #8f959e;
  font-size: 18px;
  cursor: pointer;
}
.runtime-inspector-close:hover {
  background: #f2f3f5;
  color: #1f2329;
}
.application-form-source-config {
  display: grid;
  gap: 12px;
  margin-bottom: 4px;
  border: 1px solid #e1e9f6;
  border-radius: 8px;
  padding: 13px;
  background: #f8fbff;
}
.application-form-source-head {
  display: grid;
  gap: 4px;
}
.application-form-source-head strong {
  color: #1f2329;
  font-size: 13px;
}
.application-form-source-head span {
  color: #8f959e;
  font-size: 11px;
  line-height: 17px;
}
.application-form-source-config .form-asset-actions {
  margin: 0;
}
.runtime-form-selector {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px;
  align-items: center;
  gap: 6px;
}
.runtime-form-selector :deep(.n-base-selection) {
  min-width: 0;
}
.runtime-form-selector :deep(.n-base-selection-label) {
  font-size: 12px;
}
.runtime-form-selector :deep(.n-button) {
  min-width: 32px;
  height: 32px;
  border: 1px solid #dce5f2;
  border-radius: 6px;
  background: #fff;
  color: #4e5969;
}
.runtime-form-selector :deep(.n-button:hover) {
  border-color: #8bb4ff;
  color: #1456f0;
}
.form-asset-selector-trigger {
  display: flex;
  width: 100%;
  height: 32px;
  align-items: center;
  gap: 7px;
  padding: 0 9px;
  border: 1px solid #dcdfe4;
  border-radius: 6px;
  background: #fff;
  color: #1f2329;
  cursor: pointer;
  text-align: left;
}
.form-asset-selector-trigger.active {
  border-color: #3370ff;
  box-shadow: 0 0 0 2px rgba(51, 112, 255, 0.12);
}
.form-asset-selector-trigger > :first-child {
  flex: 0 0 auto;
  color: #3370ff;
  font-size: 16px;
}
.form-asset-selector-trigger > span:nth-child(2) {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}
.form-asset-selector-open,
.form-asset-selector-arrow {
  flex: 0 0 auto;
  color: #86909c;
  font-size: 14px;
}
.form-asset-selector-open:hover {
  color: #1456f0;
}
.form-asset-selector-menu {
  display: grid;
  width: 284px;
  gap: 6px;
  padding: 8px;
}
.form-asset-selector-option {
  display: flex;
  height: 34px;
  align-items: center;
  gap: 8px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #1f2329;
  cursor: pointer;
  text-align: left;
}
.form-asset-selector-option:hover,
.form-asset-selector-option.selected {
  background: #f2f3f5;
}
.form-asset-selector-option.selected {
  color: #1456f0;
}
.form-asset-selector-option > :first-child {
  margin-left: 8px;
  color: currentColor;
}
.form-asset-selector-option > span:nth-child(2) {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}
.form-asset-selector-check {
  margin-right: 8px;
  color: #1456f0;
  font-size: 14px;
}
.inspector-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin-bottom: 10px;
  color: #1f2329;
  font-size: 14px;
}
.inspector-head button {
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #86909c;
  font-size: 20px;
  cursor: pointer;
}
.inspector-head button:hover {
  background: #f2f3f5;
  color: #1f2329;
}
.page-surface {
  position: relative;
  box-sizing: border-box;
  min-height: calc(100vh - 80px);
  overflow: hidden;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #fff;
}
.object-page-card p {
  margin: 0;
  color: #86909c;
  font-size: 13px;
}
.object-page-card {
  display: grid;
  gap: 9px;
  margin: 10px 16px 0;
  padding: 12px;
  border: 1px solid #e5e6eb;
  border-radius: 7px;
  background: #fbfcfd;
}
.application-grid-host {
  position: relative;
  min-height: min(680px, calc(100vh - 104px));
  margin-top: 8px;
}
.application-page-flow {
  position: relative;
  display: block;
  min-height: 100%;
  padding: 0 16px 20px;
}
.application-page-block {
  position: relative;
  min-width: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  transition:
    left 260ms cubic-bezier(0.22, 0.8, 0.24, 1),
    top 260ms cubic-bezier(0.22, 0.8, 0.24, 1),
    width 220ms cubic-bezier(0.22, 0.8, 0.24, 1),
    height 220ms cubic-bezier(0.22, 0.8, 0.24, 1),
    border-color 160ms ease,
    box-shadow 160ms ease;
}
.application-page-block.editing {
  cursor: pointer;
}
.application-page-block.editing:hover {
  border-color: #b7d0ff;
}
.application-page-block::after {
  position: absolute;
  z-index: 2;
  inset: -2px;
  border: 1px solid transparent;
  border-radius: 6px;
  box-shadow: none;
  content: '';
  pointer-events: none;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}
.application-page-block.editing:hover::after {
  border-color: #60a5fa;
  box-shadow: 0 0 0 1px rgba(96, 165, 250, 0.12);
}
.application-page-block.selected {
  border-color: transparent;
}
.application-page-block.selected::after {
  border-color: #2563eb;
  box-shadow: none;
}
.application-page-block.selected.editing:hover,
.application-page-block.selected.editing:hover::after {
  border-color: #2563eb;
}
.application-page-block:has(.inline-rich-text.is-focused) {
  z-index: 80;
  overflow: visible;
}
.application-page-block.dragging {
  pointer-events: none;
  z-index: 1;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}
.application-page-block.dragging::after {
  border-color: transparent;
  box-shadow: none;
}
.application-page-block.dragging :deep(.grid-block) {
  visibility: hidden;
}
.application-page-block.dragging .page-block-node-overlay,
.application-page-block.dragging .page-block-resize-anchor {
  display: none;
}
.page-block-drag-preview {
  position: absolute;
  z-index: 8;
  overflow: hidden;
  border-color: #2563eb;
  background: #fff;
  box-shadow:
    0 0 0 1px rgba(37, 99, 235, 0.28),
    0 14px 32px rgba(31, 35, 41, 0.18);
  opacity: 0.96;
  pointer-events: none;
  will-change: left, top;
}
.page-block-drag-preview-shadow {
  z-index: 7;
  inset: auto;
  transform: translate(16px, 16px);
  transition: none;
  will-change: left, top;
}
.page-block-node-overlay {
  position: absolute;
  z-index: 3;
  top: 6px;
  right: 6px;
  left: 6px;
  height: 24px;
  opacity: 0;
  /* 透明的整条覆盖层不能抢走标题/富文本的第一次点击。 */
  pointer-events: none;
  transition: opacity 160ms ease;
}
.page-block-node-overlay .page-block-drag-handle,
.page-block-node-overlay .page-block-menu-trigger,
.page-block-node-overlay .page-block-color-picker {
  pointer-events: auto;
}
.application-page-block.editing:hover .page-block-node-overlay,
.application-page-block.selected .page-block-node-overlay {
  opacity: 1;
}
.page-block-drag-handle,
.page-block-menu-trigger {
  display: grid;
  position: absolute;
  width: 26px;
  height: 24px;
  place-items: center;
  border: 1px solid #b7d0ff;
  border-radius: 5px;
  background: #eff6ff;
  color: #1d4ed8;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.14);
}
.page-block-drag-handle {
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  cursor: grab;
}
.page-block-drag-handle svg {
  display: none;
}
.page-block-drag-handle::before {
  width: 17px;
  height: 10px;
  background-image: radial-gradient(circle at 1.5px 1.5px, currentColor 1.5px, transparent 1.7px);
  background-position: 0 0;
  background-size: 7px 7px;
  content: '';
}
.page-block-menu-trigger {
  top: 0;
  right: 0;
  padding: 0;
  background: #f2f3f5;
  color: #4e5969;
  cursor: pointer;
}
.page-block-drag-handle:active {
  cursor: grabbing;
}
.page-block-drag-handle:hover {
  background: #2563eb;
  color: #fff;
}
.page-block-menu-trigger:hover {
  background: #e5e6eb;
  color: #1f2329;
}
.page-block-color-picker {
  display: grid;
  width: 236px;
  gap: 10px;
  padding: 10px;
}
.page-block-color-picker-floating {
  position: absolute;
  z-index: 20;
  top: 30px;
  right: 0;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(31, 35, 41, 0.16);
}
.page-block-color-picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1f2329;
  font-size: 13px;
  font-weight: 600;
}
.page-block-color-picker-reset {
  height: 24px !important;
  width: auto !important;
  padding: 0 8px;
  border: 1px solid #dcdfe4 !important;
  border-radius: 5px !important;
  background: #fff !important;
  color: #4e5969 !important;
  font-size: 11px !important;
  line-height: 22px !important;
}
.page-block-color-presets {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}
.page-block-color-presets button {
  width: 100%;
  aspect-ratio: 1;
  cursor: pointer;
  border: 1px solid rgba(31, 35, 41, 0.1);
  border-radius: 4px;
}
.page-block-color-presets button.transparent {
  background: linear-gradient(135deg, transparent 46%, #f53f3f 47%, #f53f3f 53%, transparent 54%), #fff;
}
.page-block-color-picker-head button {
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #8f959e;
  cursor: pointer;
  font-size: 18px;
  line-height: 18px;
}
.page-block-color-picker-head button:hover {
  background: #f2f3f5;
  color: #1f2329;
}
.page-block-color-picker-transparent {
  height: 28px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #fff;
  color: #4e5969;
  cursor: pointer;
  font-size: 12px;
}
.page-block-color-picker-transparent:hover {
  border-color: #b7d0ff;
  background: #edf4ff;
  color: #1456f0;
}
.page-block-ghost {
  border: 1px dashed #8f959e !important;
  border-radius: 10px !important;
  background: #f1f2f4 !important;
  box-shadow: none;
}
.page-block-ghost :deep(.grid-block) {
  visibility: hidden;
}
.page-block-ghost .page-block-node-overlay,
.page-block-ghost .page-block-resize-anchor {
  display: none;
}
.page-block-move-shadow {
  position: absolute;
  z-index: 0;
  inset: 0;
  border: 1px dashed #8f959e;
  border-radius: 6px;
  background: rgba(225, 227, 230, 0.76);
  box-shadow: none;
  pointer-events: none;
}
.page-block-drag-shadow {
  border: 2px solid #2563eb !important;
  border-radius: 10px !important;
  background: #fff;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.85),
    0 12px 24px rgba(37, 99, 235, 0.16);
  opacity: 0.94;
}
.page-block-chosen {
  cursor: grabbing;
}
.application-page-block :deep(.grid-block) {
  position: relative;
  z-index: 1;
  height: 100% !important;
  min-height: 0;
}
.page-block-resize-anchor {
  position: absolute;
  z-index: 4;
  width: 10px;
  height: 10px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: #1d4ed8;
  box-shadow: 0 2px 6px rgba(29, 78, 216, 0.36);
}
.page-block-resize-anchor::before {
  position: absolute;
  inset: -8px;
  border-radius: 999px;
  content: '';
}
.anchor-top-left {
  top: -7px;
  left: -7px;
  cursor: nwse-resize;
}
.anchor-top {
  top: -7px;
  left: 50%;
  transform: translateX(-50%);
  cursor: ns-resize;
}
.anchor-top-right {
  top: -7px;
  right: -7px;
  cursor: nesw-resize;
}
.anchor-right {
  top: 50%;
  right: -7px;
  transform: translateY(-50%);
  cursor: ew-resize;
}
.anchor-bottom-right {
  right: -7px;
  bottom: -7px;
  cursor: nwse-resize;
}
.anchor-bottom {
  bottom: -7px;
  left: 50%;
  transform: translateX(-50%);
  cursor: ns-resize;
}
.anchor-bottom-left {
  bottom: -7px;
  left: -7px;
  cursor: nesw-resize;
}
.anchor-left {
  top: 50%;
  left: -7px;
  transform: translateY(-50%);
  cursor: ew-resize;
}
.grid-empty-guide {
  position: absolute;
  top: 44px;
  right: 40px;
  left: 40px;
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(300px, 1.1fr);
  grid-template-rows: auto auto;
  align-items: center;
  column-gap: clamp(32px, 6vw, 88px);
  row-gap: 22px;
  min-height: min(430px, calc(100% - 88px));
  overflow: hidden;
  border: 1px solid #e4edff;
  border-radius: 14px;
  padding: clamp(28px, 4vw, 52px);
  background: linear-gradient(118deg, #fff 0%, #fff 54%, #f6faff 100%);
}
.grid-empty-guide::before {
  position: absolute;
  width: 280px;
  height: 280px;
  border: 1px solid #e4efff;
  border-radius: 50%;
  top: -150px;
  right: -54px;
  content: '';
}
.empty-guide-copy {
  position: relative;
  z-index: 1;
  align-self: end;
}
.empty-guide-eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  color: #3370ff;
  background: #edf4ff;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}
.grid-empty-guide h2 {
  margin: 11px 0 8px;
  color: #1f2329;
  font-size: clamp(22px, 2vw, 30px);
  font-weight: 650;
  letter-spacing: -0.02em;
  line-height: 1.22;
}
.grid-empty-guide p {
  max-width: 360px;
  margin: 0;
  color: #646a73;
  font-size: 13px;
  line-height: 22px;
}
.page-recommendations {
  z-index: 1;
  display: grid;
  grid-column: 1;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
  align-self: start;
  max-width: 410px;
}
.page-recommendations button {
  display: grid;
  grid-template-rows: 30px auto;
  gap: 6px;
  min-height: 68px;
  border: 1px solid #e1e8f5;
  border-radius: 8px;
  padding: 9px 8px 8px;
  background: rgb(255 255 255 / 76%);
  color: #4e5969;
  font-size: 12px;
  text-align: center;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    color 160ms ease,
    transform 160ms ease;
}
.page-recommendations button:hover {
  border-color: #8bb4ff;
  background: #fff;
  color: #165dff;
  transform: translateY(-2px);
}
.empty-component-icon {
  display: inline-grid;
  width: 30px;
  height: 30px;
  place-self: center;
  place-items: center;
  border-radius: 8px;
  color: #3370ff;
  background: #edf4ff;
  font-size: 17px;
}
.empty-component-icon.kind-chart {
  color: #5e7ce0;
  background: #f0f3ff;
}
.empty-component-icon.kind-list {
  color: #25816a;
  background: #e9f8f1;
}
.empty-component-icon.kind-other {
  color: #ad7b2d;
  background: #fff7e8;
}
.empty-guide-preview {
  position: relative;
  z-index: 1;
  grid-row: 1 / span 2;
  grid-column: 2;
  width: min(100%, 500px);
  min-height: 270px;
  justify-self: end;
}
.empty-guide-page-sheet {
  position: absolute;
  top: 24px;
  right: 22px;
  bottom: 12px;
  left: 26px;
  border: 1px solid #dce9ff;
  border-radius: 9px;
  padding: 16px;
  background: #fff;
}
.empty-guide-page-sheet::before,
.empty-guide-page-sheet::after {
  position: absolute;
  z-index: -1;
  border: 1px solid #edf3ff;
  border-radius: 9px;
  background: #f8fbff;
  content: '';
}
.empty-guide-page-sheet::before {
  inset: -12px 12px 12px -12px;
}
.empty-guide-page-sheet::after {
  inset: 10px -12px -10px 16px;
}
.empty-guide-sheet-head,
.empty-guide-sheet-content,
.empty-guide-sheet-metrics {
  display: flex;
  align-items: center;
}
.empty-guide-sheet-head {
  gap: 7px;
}
.empty-guide-sheet-head i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #9cc2ff;
}
.empty-guide-sheet-head span {
  width: 74px;
  height: 7px;
  border-radius: 5px;
  background: #e5efff;
}
.empty-guide-sheet-head em {
  width: 30px;
  height: 14px;
  margin-left: auto;
  border-radius: 4px;
  background: #e8f2ff;
}
.empty-guide-sheet-title {
  display: grid;
  gap: 7px;
  margin-top: 22px;
}
.empty-guide-sheet-title b {
  width: 42%;
  height: 11px;
  border-radius: 4px;
  background: #b9d4ff;
}
.empty-guide-sheet-title span {
  width: 68%;
  height: 7px;
  border-radius: 4px;
  background: #edf3ff;
}
.empty-guide-sheet-metrics {
  gap: 8px;
  margin-top: 17px;
}
.empty-guide-sheet-metrics i {
  flex: 1;
  height: 45px;
  border: 1px solid #e9f1ff;
  border-radius: 6px;
  background: linear-gradient(145deg, #f4f8ff, #fff);
}
.empty-guide-sheet-content {
  gap: 10px;
  height: 84px;
  margin-top: 12px;
}
.empty-guide-sheet-list,
.empty-guide-sheet-chart {
  flex: 1;
  height: 100%;
  border: 1px solid #edf3ff;
  border-radius: 6px;
  padding: 10px;
}
.empty-guide-sheet-list {
  display: grid;
  align-content: center;
  gap: 6px;
}
.empty-guide-sheet-list i {
  height: 5px;
  border-radius: 5px;
  background: #e7f0ff;
}
.empty-guide-sheet-list i:nth-child(2n) {
  width: 72%;
}
.empty-guide-sheet-chart {
  display: flex;
  align-items: end;
  gap: 5px;
}
.empty-guide-sheet-chart i {
  flex: 1;
  border-radius: 3px 3px 1px 1px;
  background: #b8d4ff;
}
.empty-guide-sheet-chart i:nth-child(1) {
  height: 36%;
}
.empty-guide-sheet-chart i:nth-child(2) {
  height: 58%;
}
.empty-guide-sheet-chart i:nth-child(3) {
  height: 45%;
}
.empty-guide-sheet-chart i:nth-child(4) {
  height: 78%;
}
.empty-guide-sheet-chart i:nth-child(5) {
  height: 64%;
}
.empty-guide-float-card {
  position: absolute;
  z-index: 2;
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid #dfeaff;
  border-radius: 9px;
  background: #fff;
  color: #5083fb;
  font-size: 19px;
}
.float-list {
  top: 0;
  left: 0;
}
.float-chart {
  right: 0;
  bottom: 28px;
  color: #6e8ae7;
}
.float-filter {
  bottom: 0;
  left: 12%;
  color: #33a27c;
}
@media (max-width: 820px) {
  .grid-empty-guide {
    grid-template-columns: 1fr;
    padding: 28px;
  }
  .empty-guide-preview {
    display: none;
  }
  .page-recommendations {
    grid-column: 1;
  }
}
.navigation-action-tip {
  margin: 0 0 14px;
  color: #4e5969;
  font-size: 13px;
  line-height: 20px;
}
.application-form-asset-workbench {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: calc(100vh - 56px);
  min-height: 620px;
  background: #f7f8fa;
}
.application-form-asset-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border-bottom: 1px solid #e5e6eb;
  padding: 14px 24px;
  background: #fff;
}
.application-form-asset-head h1 {
  margin: 3px 0 2px;
  color: #1f2329;
  font-size: 18px;
  line-height: 26px;
}
.application-form-asset-head p,
.application-form-asset-crumb {
  margin: 0;
  color: #8f959e;
  font-size: 12px;
}
.application-form-asset-crumb {
  color: #646a73;
}
.application-form-asset-designer {
  min-height: 0;
  overflow: hidden;
}
.application-form-assets-popover {
  display: grid;
  width: 280px;
  max-height: min(420px, calc(100vh - 100px));
  gap: 4px;
  overflow: auto;
}
.application-form-assets-popover-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 2px 8px;
  color: #1f2329;
  font-size: 13px;
}
.application-form-assets-popover-head > div {
  display: grid;
  gap: 2px;
}
.application-form-assets-popover-head small {
  color: #8f959e;
  font-size: 11px;
}
.application-form-asset-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  border: 0;
  border-radius: 6px;
  padding: 8px;
  background: transparent;
  color: #646a73;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}
.application-form-asset-row > span:first-child {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.application-form-asset-row strong {
  overflow: hidden;
  color: #1f2329;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.application-form-asset-row small {
  color: #8f959e;
}
.application-form-asset-row:hover {
  background: #f2f3f5;
}
.form-asset-actions {
  display: flex;
  gap: 8px;
  margin: -4px 0 10px;
}
.form-asset-default-hint {
  margin: -2px 0 0;
  color: #86909c;
  font-size: 12px;
  line-height: 18px;
}
.form-asset-help {
  margin: 0 0 12px;
  color: #8f959e;
  font-size: 12px;
  line-height: 18px;
}
@media (max-width: 980px) {
  .runtime-body {
    grid-template-columns: 200px minmax(0, 1fr);
  }
  .runtime-body.configuring {
    grid-template-columns: 200px minmax(0, 1fr);
  }
  .runtime-inspector {
    position: fixed;
    z-index: 10;
    top: 56px;
    right: 0;
    bottom: 0;
    width: min(320px, 88vw);
    box-shadow: -8px 0 20px rgba(31, 35, 41, 0.08);
  }
  .runtime-header {
    height: auto;
    min-height: 56px;
    padding-block: 8px;
  }
  .canvas-component-anchor {
    left: 12px !important;
    bottom: 12px !important;
  }
  .canvas-component-anchor:not(.is-default-position) {
    bottom: auto !important;
  }
  .canvas-component-anchor :deep(.n-button) {
    padding-inline: 10px;
  }
  .application-empty-state {
    padding: 28px 24px;
  }
  .application-empty-intro {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }
  .application-template-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .application-template-card {
    min-height: 132px;
  }
  .application-template-object-binding {
    grid-template-columns: 1fr auto;
  }
  .application-template-object-binding > span {
    grid-column: 1 / -1;
  }
}
</style>
