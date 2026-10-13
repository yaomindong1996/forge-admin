<template>
  <div class="application-runtime-page" :class="{ 'is-loading': loading, 'is-ready': !loading && application }">
    <!-- 骨架屏加载态（与路由 Suspense fallback 同款，避免 chunk 加载完又闪一下） -->
    <ApplicationRuntimeSkeleton v-if="loading" />
    <template v-else-if="application">
      <header v-if="!formDesignerMode" class="runtime-header">
        <div class="runtime-brand">
          <n-button quaternary circle :aria-label="resolveRuntimeBackLabel()" @click="handleRuntimeBack">
            <template #icon>
              <NIcon><ArrowBackOutline /></NIcon>
            </template>
          </n-button>
          <button type="button" class="runtime-breadcrumb" :title="resolveRuntimeBackLabel()" @click="handleRuntimeBack">
            <span>{{ resolveRuntimeBackLabel() }}</span>
            <span aria-hidden="true">›</span>
          </button>
          <div class="runtime-brand-copy">
            <span class="runtime-brand-app-icon" aria-hidden="true"><NIcon><FolderOpenOutline /></NIcon></span>
            <div class="runtime-design-title">
              <span>{{ resolveRuntimeBrandEyebrow() }}</span>
              <strong>{{ application.applicationName || '未命名应用' }}</strong>
            </div>
            <span class="runtime-brand-status">{{ resolveRuntimeBrandStatus() }}</span>
          </div>
        </div>
        <nav class="runtime-app-tabs" aria-label="应用导航">
          <!-- 非编辑模式：应用级 Tab -->
          <template v-if="!editing && !isDraftPreviewMode()">
            <button type="button" class="runtime-app-tab" :class="{ active: runtimeViewMode === 'pages' }" @click="switchRuntimeView('pages')">
              页面管理
            </button>
            <button type="button" class="runtime-app-tab" :class="{ active: runtimeViewMode === 'process' }" @click="switchRuntimeView('process')">
              业务流程
            </button>
            <button type="button" class="runtime-app-tab" :class="{ active: runtimeViewMode === 'enhance' }" @click="switchRuntimeView('enhance')">
              增强
            </button>
            <button type="button" class="runtime-app-tab" :class="{ active: runtimeViewMode === 'settings' }" @click="switchRuntimeView('settings')">
              应用设置
            </button>
            <button type="button" class="runtime-app-tab" @click="switchRuntimeView('publish')">
              应用发布
            </button>
          </template>
          <!-- 编辑 / 草稿预览：页面级 Tab（预览始终保留，避免切设置/发布/列表后丢失） -->
          <template v-else>
            <button
              type="button"
              class="runtime-app-tab"
              :class="{
                active: isDraftPreviewMode() && !['settings', 'publish'].includes(activePageDesignTab),
                'is-loading': previewTabLoading,
              }"
              :disabled="previewTabLoading"
              :aria-busy="previewTabLoading ? 'true' : undefined"
              @click="switchPageDesignTab('preview')"
            >
              {{ previewTabLoading ? '打开预览中…' : '预览' }}
            </button>
            <button
              v-if="showPageDesignTab"
              type="button"
              class="runtime-app-tab"
              :class="{ active: editing && isPageDesignTabActive }"
              @click="switchPageDesignTab('page')"
            >
              页面设计
            </button>
            <button
              v-if="showFormDesignTab"
              type="button"
              class="runtime-app-tab"
              :class="{ active: editing && activePageDesignTab === 'form' }"
              @click="switchPageDesignTab('form')"
            >
              表单设计
            </button>
            <button
              v-if="showListDesignTab"
              type="button"
              class="runtime-app-tab"
              :class="{ active: editing && activePageDesignTab === 'list' }"
              @click="switchPageDesignTab('list')"
            >
              列表设计
            </button>
            <button
              type="button"
              class="runtime-app-tab"
              :class="{ active: (editing || isDraftPreviewMode()) && activePageDesignTab === 'settings' }"
              @click="switchPageDesignTab('settings')"
            >
              页面设置
            </button>
            <button
              type="button"
              class="runtime-app-tab"
              :class="{ active: (editing || isDraftPreviewMode()) && activePageDesignTab === 'publish' }"
              @click="switchPageDesignTab('publish')"
            >
              发布
            </button>
          </template>
        </nav>
        <div v-if="!formDesignerMode" class="runtime-header-actions">
          <n-popover
            v-if="editing"
            trigger="click"
            placement="bottom-end"
            :show-arrow="false"
            :disabled="!pageBuilderResourceActive"
          >
            <template #trigger>
              <n-button
                quaternary
                :disabled="!pageBuilderResourceActive"
                title="页面表单资产"
              >
                <NIcon><FolderOpenOutline /></NIcon>
                页面资源
              </n-button>
            </template>
            <div class="application-form-assets-popover">
              <div class="application-form-assets-popover-head">
                <div>
                  <strong>页面表单资产</strong>
                  <small>可设为列表「新增 / 编辑」使用的表单</small>
                </div>
                <n-button size="tiny" type="primary" @click="createStandaloneFormAsset">
                  新建表单
                </n-button>
              </div>
              <div
                v-for="asset in formAssets"
                :key="asset.id || asset.formKey"
                class="application-form-asset-row"
                :class="{ 'is-bound': isFormAssetBoundToListCrud(asset.id || asset.formKey) }"
              >
                <button type="button" class="application-form-asset-main" @click="openFormAssetDesigner(asset.id || asset.formKey)">
                  <span>
                    <strong>{{ asset.name || asset.formKey || asset.id }}</strong>
                    <small>
                      {{ resolveFormAssetFields(asset).length }} 个字段
                      <template v-if="isFormAssetBoundToListCrud(asset.id || asset.formKey)"> · 当前新增/编辑表单</template>
                    </small>
                  </span>
                </button>
                <div class="application-form-asset-ops">
                  <n-button
                    size="tiny"
                    quaternary
                    type="primary"
                    :disabled="isFormAssetBoundToListCrud(asset.id || asset.formKey)"
                    @click.stop="setFormAssetAsListCrudForm(asset.id || asset.formKey)"
                  >
                    {{ isFormAssetBoundToListCrud(asset.id || asset.formKey) ? '已设为新增编辑' : '设为新增编辑表单' }}
                  </n-button>
                  <n-button size="tiny" quaternary @click.stop="openFormAssetDesigner(asset.id || asset.formKey)">
                    编辑
                  </n-button>
                </div>
              </div>
              <n-empty v-if="!formAssets.length" size="small" description="还没有页面表单，先创建一个" />
            </div>
          </n-popover>
          <n-button
            v-if="editing || (canEditApplication && dirty)"
            :disabled="!dirty && !embeddedDesignerDirty"
            :loading="saving || embeddedDesignerSaving"
            title="保存当前设计草稿"
            secondary
            @click="saveCurrentDesignerSection"
          >
            <template #icon>
              <NIcon><SaveOutline /></NIcon>
            </template>
            保存草稿
          </n-button>
          <n-button v-if="editing" quaternary :disabled="!previewableResourceActive" title="预览草稿" @click="openDraftPreview">
            <NIcon><EyeOutline /></NIcon>
            预览
          </n-button>
          <n-dropdown v-if="editing" trigger="click" placement="bottom-end" :options="runtimeHeaderMoreOptions" @select="handleRuntimeHeaderMoreSelect">
            <n-button quaternary circle aria-label="更多操作">
              <template #icon>
                <NIcon><EllipsisHorizontalOutline /></NIcon>
              </template>
            </n-button>
          </n-dropdown>
        </div>
      </header>

      <ApplicationDesignerResourceTree
        v-if="!editing && false"
        :groups="designerResourceGroups"
        :active-group-key="designerSection"
        :active-key="activeDesignerResource?.key || ''"
        :application-name="application.applicationName"
        :node-menu-options="resolveResourceNodeMenuOptions"
        @select="selectDesignerResource"
        @create-page="openPageTypeSelector()"
        @node-action="handleResourceNodeAction"
      />

      <section v-if="showFormDesignWorkbench" class="application-form-asset-workbench">
        <DesignerAsyncLoader
          v-if="designerTransitionLoading"
          title="正在准备页面设计"
          description="正在保存页面草稿并挂载表单资产，请稍候"
        />
        <template v-else>
          <!-- 表单设计器模式：极简顶栏（返回 + 数据对象名 + 保存） -->
          <div v-if="formDesignerMode" class="form-designer-topbar">
            <div class="form-designer-topbar-left">
              <n-button quaternary circle size="small" title="返回页面设计" @click="returnToPageDesigner">
                <template #icon>
                  <NIcon><ArrowBackOutline /></NIcon>
                </template>
              </n-button>
              <template v-if="activePageShapeDesign">
                <span class="form-designer-topbar-label">数据对象</span>
                <n-input
                  v-model:value="activePageShapeDesign.objectName"
                  size="tiny"
                  class="form-designer-topbar-name"
                  maxlength="100"
                  placeholder="对象名称"
                  @update:value="syncActivePageShapeObject"
                />
              </template>
              <span v-else class="form-designer-topbar-title">{{ activeFormAsset?.name || '表单设计' }}</span>
            </div>
            <div class="form-designer-topbar-actions">
              <span v-if="activeFormDataState.status === 'error'" class="form-data-save-error">{{ activeFormDataState.message }}</span>
              <n-button size="small" secondary @click="returnToPageDesigner">
                返回
              </n-button>
              <n-button
                size="small"
                type="primary"
                :disabled="!canSaveActiveFormDesigner || saving"
                :loading="saving"
                @click="saveActiveFormDesigner(true)"
              >
                {{ activeFormDataState.status === 'error' ? '重试' : '保存' }}
              </n-button>
            </div>
          </div>
          <div v-if="activeFormAsset" class="application-form-asset-designer">
            <ForgeFormDesigner
              :key="activeFormAsset.id"
              :model-value="activeFormDesignerSchema"
              :fields="activeFormFields"
              :object-code="activePageShapeDesign?.objectCode || activeFormDesignerContext?.objectCode || application.applicationCode"
              :object-name="activePageShapeDesign?.objectName || activeFormDesignerContext?.objectName || activeFormAsset.name"
              :relations="activeFormDesignerRelations"
              :actions="activeFormDesignerActions"
              :enable-sections-view="false"
              :derive-sections-from-layout="true"
              @update:model-value="updateActiveFormDesignerSchema"
            />
          </div>
          <n-empty v-else description="当前页面还没有表单，先创建一个再设计字段">
            <template #extra>
              <n-button type="primary" @click="createFormAssetForCurrentPage">
                创建表单
              </n-button>
            </template>
          </n-empty>
        </template>
      </section>

      <section v-else-if="editing && activePageDesignTab === 'list'" class="application-design-section">
        <BusinessObjectDesignerPage
          v-if="pageDesignObject"
          :key="`page-list:${pageDesignObject.objectId || pageDesignObject.objectCode}`"
          ref="embeddedDesignerRef"
          embedded
          :embedded-object-code="pageDesignObject.objectCode"
          :embedded-object-id="pageDesignObject.objectId"
          :embedded-suite-code="application.suiteCode"
          initial-panel="list"
          :embedded-nav-panels="['list']"
          :application-pages="appPageTargetOptions"
          @dirty-change="embeddedDesignerDirty = $event"
          @saved="handleEmbeddedDesignerSaved"
        />
        <n-empty v-else description="当前页面尚未绑定数据对象，无法设计列表">
          <template #extra>
            <n-button type="primary" @click="openObjectSetup">
              配置数据对象
            </n-button>
          </template>
        </n-empty>
      </section>

      <!-- 编辑/草稿预览：页面设置（须排在自由布局画布之前，避免兜底抢占） -->
      <section v-else-if="(editing || isDraftPreviewMode()) && activePageDesignTab === 'settings'" class="runtime-inline-panel">
        <PageDesignSettingsPanel
          v-if="currentNode"
          :node="currentNode"
          :application="application"
          :objects="objects"
          :layout-polluted="currentPageLayoutPolluted"
          :restoring="restoringObjectPageLayout"
          @update="patchCurrentPageNode"
          @restore-layout="restoreCurrentObjectPageLayout"
        />
        <n-empty v-else description="请先选择要设置的页面" />
      </section>

      <!-- 编辑/草稿预览：发布（预览页可直接改挂载并跳转应用发布，避免来回跳） -->
      <section v-else-if="(editing || isDraftPreviewMode()) && activePageDesignTab === 'publish'" class="runtime-inline-panel">
        <PageDesignPublishPanel
          v-if="currentNode"
          :application="application"
          :node="currentNode"
          :page-id="currentNode.id"
          :page-title="currentNode.title"
          :dirty="dirty"
          :saving="saving"
          :config-key="pageDesignObject?.configKey || currentNode.objectRef?.configKey || ''"
          :objects="objects"
          @save="saveCurrentDesignerSection"
          @preview="openDraftPreview"
          @update="patchCurrentPageNode"
        />
        <n-empty v-else description="请先选择要发布的页面" />
      </section>

      <div
        v-else-if="(!editing && runtimeViewMode === 'pages') || showFreeLayoutCanvas"
        class="runtime-body"
        :class="{
          'configuring': editing && configPanelVisible,
          'sidebar-collapsed': sidebarCollapsed,
          'editing-canvas': showFreeLayoutCanvas,
        }"
      >
        <aside
          v-if="!editing"
          class="runtime-navigation base-app-sidebar__vertical no-page-group"
          :class="{ collapsed: sidebarCollapsed }"
        >
          <div class="title_wrapper">
            <div class="application-sidebar-title base-app-sidebar__title_bar">
              <div class="base-app-title-wrapper">
                <span class="application-icon-slot" aria-hidden="true">
                  <AuthImage :src="tenantStore.systemLogo" :fallback="defaultLogo" alt="" />
                </span>
                <n-input
                  v-if="renamingApplication"
                  ref="renameInputRef"
                  v-model:value="renameApplicationValue"
                  size="tiny"
                  class="sidebar-rename-input"
                  :disabled="renameSaving"
                  placeholder="应用名称"
                  @keydown.enter.prevent="confirmRenameApplication"
                  @keydown.escape.prevent="cancelRenameApplication"
                  @blur="confirmRenameApplication"
                />
                <strong
                  v-else
                  class="base-app-title-content"
                  :class="{ editable: canEditApplication && !editing }"
                  :title="canEditApplication && !editing ? '点击修改应用名称' : ''"
                  @click="canEditApplication && !editing && startRenameApplication()"
                >{{ application.applicationName }}</strong>
              </div>
              <div class="sidebar-title-actions">
                <button class="sidebar-collapse-hint" type="button" :aria-label="sidebarCollapsed ? '展开页面菜单' : '收起页面菜单'" :title="sidebarCollapsed ? '展开页面菜单' : '收起页面菜单'" @click="sidebarCollapsed = !sidebarCollapsed">
                  {{ sidebarCollapsed ? '»' : '«' }}
                </button>
              </div>
            </div>
          </div>
          <div class="navigation-list scroll_wrapper">
            <div class="list_wrapper base-app-sidebar__list_vertical">
              <div
                v-for="item in pageManagementSystemPages"
                :key="item.id"
                class="navigation-row base-app-sidebar__node_vertical"
                :class="{ 'base-app-sidebar__node_selected': item.id === selectedNodeId }"
              >
                <button
                  class="navigation-page"
                  :class="{ active: item.id === selectedNodeId }"
                  type="button"
                  @click="selectPageManagementNode(item.id)"
                >
                  <span class="navigation-icon-slot" aria-hidden="true">
                    <NIcon v-if="item.icon"><component :is="resolveSystemPageIcon(item.icon)" /></NIcon>
                  </span>
                  <span>{{ item.title }}</span>
                </button>
                <button
                  v-if="item.id === WORKBENCH_PAGE_ID && canEditApplication"
                  type="button"
                  class="navigation-action navigation-edit-icon"
                  title="设计个人工作台"
                  @click.stop="enterWorkbenchDesign()"
                >
                  <NIcon size="14">
                    <CreateOutline />
                  </NIcon>
                </button>
              </div>
              <div class="navigation-section-divider" />
              <template v-for="item in navigationNodes" :key="item.id">
                <div class="navigation-row base-app-sidebar__node_vertical" :class="{ 'base-app-sidebar__node_selected': item.id === selectedNodeId, 'is-nav-hidden': !isNavigationVisible(item) }" :style="{ paddingLeft: `${12 + item.depth * 16}px` }">
                  <button
                    v-if="item.type === 'page'"
                    class="navigation-page"
                    :class="{ active: item.id === selectedNodeId }"
                    type="button"
                    @click="selectPageManagementNode(item.id)"
                  >
                    <span v-if="item.icon" class="navigation-icon-slot" aria-hidden="true">
                      <IconRenderer v-if="item.icon" :icon="item.icon" :size="16" />
                    </span>
                    <span>{{ item.title }}</span>
                  </button>
                  <button
                    v-else
                    type="button"
                    class="navigation-group"
                    :class="{ collapsed: isGroupCollapsed(item.id) }"
                    :aria-expanded="!isGroupCollapsed(item.id)"
                    @click="toggleGroupExpanded(item.id)"
                  >
                    <NIcon size="12" class="navigation-group-chevron">
                      <ArrowDownOutline />
                    </NIcon>
                    <span>{{ item.title }}</span>
                  </button>
                  <!-- 页面项的编辑按钮（hover 显示） -->
                  <button
                    v-if="item.type === 'page' && canEditApplication"
                    type="button"
                    class="navigation-action navigation-edit-icon"
                    title="设计页面"
                    @click.stop="enterPageDesign(item.id)"
                  >
                    <NIcon size="14">
                      <CreateOutline />
                    </NIcon>
                  </button>
                  <!-- “更多”下拉菜单（hover 显示），包含重命名/图标/隐藏/复制/移动/删除 -->
                  <n-dropdown
                    v-if="canEditApplication"
                    trigger="click"
                    placement="bottom-end"
                    :options="resolveNavigationMoreOptions(item)"
                    @select="key => handleNavigationMoreSelect(key, item)"
                  >
                    <button type="button" class="navigation-action navigation-more-icon" :aria-label="`${item.title}更多操作`" title="更多操作" @click.stop>
                      <span aria-hidden="true">•••</span>
                    </button>
                  </n-dropdown>
                </div>
              </template>
            </div>
          </div>
          <div v-if="canEditApplication" class="new_node_wrapper">
            <n-popover v-model:show="newNodePopoverVisible" trigger="click" placement="right-end" :show-arrow="false">
              <template #trigger>
                <button type="button" class="navigation-create base-app-sidebar__new_node_vertical">
                  <span>+</span>新建
                </button>
              </template>
              <div class="new-node-popover">
                <button type="button" class="new-node-choice" @click="openPageTypeSelector()">
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
          <section v-if="currentSystemPage && !editing" class="page-surface">
            <PageManagementSystemView
              :view="currentSystemPage.view"
              :title="currentSystemPage.title"
              :navigation-routes="systemPageNavigationRoutes"
              :workbench-page="workbenchPage"
              :objects="objects"
              :application-id="String(application?.id || '')"
              :application-code="application?.applicationCode || ''"
            />
          </section>
          <section v-else-if="!currentNode" class="application-empty-state">
            <div class="application-empty-intro">
              <div>
                <span class="application-empty-eyebrow">页面管理</span>
                <h1>开始设计你的第一个页面</h1>
                <p>选择页面形态，直接在页面上添加字段，系统会自动为你生成数据表。</p>
                <n-space v-if="canEditApplication">
                  <n-button type="primary" class="application-first-page-button" @click="openPageTypeSelector()">
                    创建数据页
                  </n-button>
                  <n-button secondary @click="openCustomPageSelector()">
                    创建自由布局页面
                  </n-button>
                  <n-button secondary @click="openExcelPageImport()">
                    从 Excel 创建页面
                  </n-button>
                </n-space>
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
          <section v-else-if="!editing" class="page-surface is-fill">
            <n-alert
              v-if="currentPageLayoutPolluted"
              type="warning"
              :bordered="false"
              class="page-layout-pollution-alert"
              title="该列表/表单页被自由布局组件污染"
            >
              <div class="page-layout-pollution-alert__body">
                <span>中间预览里的自由布局内容可以一键清掉，恢复为原来的列表+表单（AiCrud）布局。</span>
                <n-button type="warning" size="small" :loading="restoringObjectPageLayout" @click="restoreCurrentObjectPageLayout">
                  恢复列表/表单布局
                </n-button>
              </div>
            </n-alert>
            <PortalPageRenderer
              :key="`portal:${portalCrudConfigRevision}`"
              :node="currentNode"
              :page="currentPage"
              :objects="objects"
              :entries="workspaceEntries"
              :extensions="workspaceExtensions"
              :form-assets="formAssets"
              :application-id="String(application?.id || '')"
              :application-code="application?.applicationCode || ''"
              :page-id="currentNode?.id || ''"
              :configurable="false"
              :design-preview="usePortalDesignPreview"
              :crud-config-revision="portalCrudConfigRevision"
              :seed-runtime-crud-props="portalCrudSeed"
              :form-fields-resolver="resolvePortalFormFields"
              fill-host
            />
          </section>
          <section v-else class="page-surface">
            <n-alert
              v-if="currentPageLayoutPolluted"
              type="warning"
              :bordered="false"
              class="page-layout-pollution-alert"
              title="检测到自由布局组件污染了对象页"
            >
              <div class="page-layout-pollution-alert__body">
                <span>点下方按钮可清空这些组件，并恢复为列表+表单页标准布局。</span>
                <n-button type="warning" size="small" :loading="restoringObjectPageLayout" @click="restoreCurrentObjectPageLayout">
                  恢复列表/表单布局
                </n-button>
              </div>
            </n-alert>
            <section v-if="currentNode?.pageType === 'object'" class="object-page-card">
              <strong>{{ currentNode.objectRef?.objectName || currentNode.title || '未绑定数据对象' }}</strong>
              <p>{{ currentNode.objectRef?.valid === false ? '绑定的数据对象已不可用，请重新选择。' : '该页面复用已有对象的列表、表单、详情和数据管理配置。' }}</p>
              <n-space v-if="editing && currentNode.objectRef?.objectCode" size="small">
                <n-button type="primary" secondary @click="openFormAssetDesignerForPage(currentNode.id)">
                  编辑表单设计
                </n-button>
                <n-button v-if="currentPageLayoutPolluted" secondary type="warning" :loading="restoringObjectPageLayout" @click="restoreCurrentObjectPageLayout">
                  恢复列表/表单布局
                </n-button>
              </n-space>
            </section>

            <div v-if="editing && currentNode" class="canvas-component-anchor" :class="{ 'moving': componentButtonMoveCtx, 'is-default-position': !hasCustomComponentButtonPosition }" :style="componentButtonStyle" @pointerdown.capture="startComponentButtonMove">
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
                          :draggable="false"
                          @pointerdown.stop="startCatalogPointerDrag($event, item)"
                          @dragstart="handleComponentCatalogDragStart($event, item)"
                          @dragend="handleComponentCatalogDragEnd"
                          @click="handleComponentCatalogClick(item, $event)"
                        >
                          <span class="component-icon-slot" :class="`kind-${group.key}`" aria-hidden="true">
                            <img v-if="resolveComponentIcon(item)" :src="resolveComponentIcon(item)" alt="">
                            <svg v-else-if="group.key === 'list'" viewBox="0 0 24 24"><path d="M7 6h11M7 12h11M7 18h11M3.5 6h.01M3.5 12h.01M3.5 18h.01" /></svg>
                            <svg v-else-if="group.key === 'chart'" viewBox="0 0 24 24"><path d="M4 19V5m0 14h16M8 16v-4m4 4V8m4 8V6" /></svg>
                            <svg v-else-if="group.key === 'view'" viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="14" rx="2" /><path d="M4 9h16M8 13h8" /></svg>
                            <svg v-else viewBox="0 0 24 24"><path d="M12 4v16M4 12h16" /><circle cx="12" cy="12" r="7" /></svg>
                          </span>
                          <span class="component-item-copy">
                            <span class="component-item-heading">
                              <strong>{{ item.title }}</strong>
                              <small v-if="item.techTitle" :title="`技术组件：${item.techTitle}`">{{ item.techTitle }}</small>
                            </span>
                            <span class="component-item-desc">{{ item.desc }}</span>
                          </span>
                        </button>
                      </div>
                    </section>
                  </div>
                  <n-empty v-else size="small" description="没有匹配的组件" />
                </div>
              </n-popover>
            </div>

            <div class="application-grid-host">
              <div
                v-if="editCanvasBootstrapping"
                class="edit-canvas-bootstrapping"
                aria-busy="true"
                aria-label="画布加载中"
              >
                <n-skeleton height="28px" width="36%" :sharp="false" />
                <n-skeleton height="14px" width="72%" :sharp="false" style="margin-top: 14px" />
                <n-skeleton height="14px" width="58%" :sharp="false" style="margin-top: 8px" />
                <n-skeleton height="160px" :sharp="false" style="margin-top: 20px" />
                <n-skeleton height="160px" :sharp="false" style="margin-top: 12px" />
                <n-skeleton height="120px" width="80%" :sharp="false" style="margin-top: 12px" />
              </div>
              <draggable
                v-show="!editCanvasBootstrapping"
                :model-value="pageBlocks"
                item-key="id"
                handle=".page-block-drag-handle"
                class="application-page-flow"
                :class="{ 'is-editing': editing, 'is-flow-stack': pageFlowStackMode }"
                :style="{ minHeight: `${pageFlowHeight}px`, padding: pageFlowStackMode ? pageCanvasPaddingCss : '0px' }"
                :disabled="true"
                :animation="180"
                :force-fallback="true"
                fallback-class="page-block-drag-shadow"
                :fallback-on-body="true"
                :fallback-tolerance="2"
                ghost-class="page-block-ghost"
                chosen-class="page-block-chosen"
                @dragenter="handlePageFlowDragOver"
                @dragover="handlePageFlowDragOver"
                @drop="handlePageFlowDrop"
                @click="handlePageFlowBlankClick"
                @update:model-value="updatePageBlocks"
              >
                <template #item="{ element: block }">
                  <section
                    class="application-page-block"
                    :class="{ selected: editing && selectedPageBlockId === block.id, editing, dragging: draggingPageBlockId === block.id, 'is-resize-collision': resizeCollisionBlockIds.includes(block.id) }"
                    :style="resolvePageBlockShellStyle(block)"
                    :data-page-block-id="block.id"
                    :data-page-block-type="block.blockType"
                    :data-grid-container-id="isSimpleBodyContainer(block) ? block.id : undefined"
                    :data-grid-cell-key="isSimpleBodyContainer(block) ? '__body__' : undefined"
                    :data-page-container-id="isSimpleBodyContainer(block) ? block.id : undefined"
                    :data-page-container-type="isSimpleBodyContainer(block) ? block.blockType : undefined"
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
                      :runtime-crud-loading="isPageBlockRuntimeCrudLoading(block)"
                      :runtime-tree-active-key="runtimeTreeActiveKeyByBlockId[block.id] || '__all__'"
                      :data-source-configured="isPageBlockDataSourceConfigured(block)"
                      :runtime-interactive="!editing && !isDraftMode"
                      :block-fields-resolver="resolvePageBlockFields"
                      :runtime-crud-props-resolver="resolvePageBlockRuntimeCrudProps"
                      :runtime-crud-loading-resolver="isPageBlockRuntimeCrudLoading"
                      :data-source-configured-resolver="isPageBlockDataSourceConfigured"
                      show-data-source-guide
                      :selected="false"
                      :selected-block-id="selectedPageBlockId"
                      :inline-text-editing="editing"
                      :readonly="!editing"
                      :catalog-drag-block-type="catalogDragBlockType"
                      :active-drop-cell="activePageFlowGridTarget"
                      :active-drop-container="activePageFlowContainerTarget"
                      :nested-moving-block-id="nestedMovingPageBlockId"
                      @block-activate="selectPageBlock"
                      @inline-text-update="handleInlineTextUpdate"
                      @child-block-select="handleNestedPageBlockSelect"
                      @child-block-menu-select="handleNestedPageBlockMenuSelect"
                      @child-block-move-start="handleNestedPageBlockMoveStart"
                      @child-block-resize-start="handleNestedPageBlockResizeStart"
                      @tab-drop="handlePageFlowTabDrop"
                      @grid-cell-drop="handlePageFlowGridCellDrop"
                      @grid-cell-insert="handlePageFlowGridCellDrop"
                      @container-insert="handlePageFlowContainerInsert"
                      @container-clear="handlePageFlowContainerClear"
                      @request-data-source="handlePageBlockDataSourceRequest"
                      @runtime-tree-select="handleRuntimeTreeSelect"
                    />
                  </section>
                </template>
              </draggable>
              <div
                v-if="dragPreview"
                class="page-block-drag-ghost"
                :style="{ left: `${dragPreview.x}px`, top: `${dragPreview.y}px`, width: `${dragPreview.width}px`, height: `${dragPreview.height}px` }"
                aria-hidden="true"
              >
                <span>{{ dragPreviewBlock?.label || dragPreviewBlock?.blockType || '组件' }}</span>
              </div>
              <div
                v-if="pageAlignGuides.visible"
                class="page-align-guides"
                aria-hidden="true"
              >
                <div
                  v-if="pageAlignGuides.cross"
                  class="page-align-cross-v"
                  :style="{ left: `${pageAlignGuides.cross.x}px` }"
                />
                <div
                  v-if="pageAlignGuides.cross"
                  class="page-align-cross-h"
                  :style="{ top: `${pageAlignGuides.cross.y}px` }"
                />
                <div
                  v-for="(x, idx) in pageAlignGuides.x"
                  :key="`snap-x-${idx}-${x}`"
                  class="page-align-snap-v"
                  :style="{ left: `${x}px` }"
                />
                <div
                  v-for="(y, idx) in pageAlignGuides.y"
                  :key="`snap-y-${idx}-${y}`"
                  class="page-align-snap-h"
                  :style="{ top: `${y}px` }"
                />
              </div>
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
          <div v-if="!selectedPageBlock && inspectorTab === 'properties'" class="page-canvas-padding-panel">
            <div class="page-canvas-padding-head">
              <strong>页面内边距</strong>
              <span>作用于画布与最终运行页，默认 24</span>
            </div>
            <div class="page-canvas-padding-grid">
              <label>
                <span>上</span>
                <n-input-number
                  size="small"
                  :value="pageCanvasPadding.top"
                  :min="0"
                  :max="120"
                  :show-button="false"
                  @update:value="updatePageCanvasPadding({ top: $event })"
                />
              </label>
              <label>
                <span>右</span>
                <n-input-number
                  size="small"
                  :value="pageCanvasPadding.right"
                  :min="0"
                  :max="120"
                  :show-button="false"
                  @update:value="updatePageCanvasPadding({ right: $event })"
                />
              </label>
              <label>
                <span>下</span>
                <n-input-number
                  size="small"
                  :value="pageCanvasPadding.bottom"
                  :min="0"
                  :max="120"
                  :show-button="false"
                  @update:value="updatePageCanvasPadding({ bottom: $event })"
                />
              </label>
              <label>
                <span>左</span>
                <n-input-number
                  size="small"
                  :value="pageCanvasPadding.left"
                  :min="0"
                  :max="120"
                  :show-button="false"
                  @update:value="updatePageCanvasPadding({ left: $event })"
                />
              </label>
            </div>
            <n-button size="tiny" secondary @click="updatePageCanvasPadding(DEFAULT_PAGE_PADDING)">
              恢复默认 24
            </n-button>
          </div>
          <div v-if="selectedPageBlock && inspectorTab === 'data'" class="application-form-source-config">
            <div class="application-form-source-head">
              <strong>{{ selectedPageBlockSupportsDataSource ? '业务数据' : '组件数据' }}</strong>
              <span>{{ selectedPageBlockSupportsDataSource ? '选择业务对象后，画布会自动生成可用字段' : selectedPageBlock.label }}</span>
            </div>
            <div v-if="selectedPageBlockSupportsDataSource" class="page-data-source-selector">
              <div class="page-data-source-selector-head">
                <span>业务对象</span>
                <small>{{ selectedPageBlockUsesObjectRuntime ? '已连接' : '未选择' }}</small>
              </div>
              <n-select
                size="small"
                filterable
                :value="selectedPageBlockRuntimeObjectId || null"
                :options="runtimeObjectFormOptions"
                :disabled="!runtimeObjectFormOptions.length"
                placeholder="选择业务对象"
                @update:value="updateSelectedPageBlockRuntimeObject"
              />
              <div class="page-data-source-actions">
                <n-button
                  v-if="selectedPageBlockUsesObjectRuntime"
                  size="tiny"
                  secondary
                  @click="openObjectDesigner(selectedPageBlockObjectDesignerPanel, selectedPageBlockRuntimeObjectRef)"
                >
                  在对象设计器中精调
                </n-button>
                <n-button
                  v-if="selectedPageBlockIsCrud"
                  size="tiny"
                  secondary
                  @click="openSelectedBlockFormDesigner"
                >
                  设计数据表单
                </n-button>
                <n-button v-if="!runtimeObjectFormOptions.length" size="tiny" secondary @click="openObjectSetup">
                  管理业务对象
                </n-button>
              </div>
            </div>
            <template v-if="selectedPageBlockIsCrud">
              <div v-if="selectedPageBlockUsesObjectRuntime" class="crud-data-storage-card ready">
                <div class="crud-object-source-icon">
                  <NIcon><CubeOutline /></NIcon>
                </div>
                <div>
                  <strong>表单数据已准备完成</strong>
                  <p>当前页面可以新增、编辑、查询和保存数据。</p>
                </div>
              </div>
              <div v-else class="crud-data-storage-card" :class="selectedPageBlockFormDataState.status">
                <div class="crud-object-source-icon">
                  <NIcon><CubeOutline /></NIcon>
                </div>
                <div>
                  <strong>{{ selectedPageBlockFormDataState.status === 'error' ? '表单数据暂未准备完成' : selectedPageBlockFormAsset ? '保存表单后自动准备数据' : '先选择或新建一个表单' }}</strong>
                  <p>{{ selectedPageBlockFormDataState.message || (selectedPageBlockFormAsset ? '无需另外配置数据模型，系统会自动完成数据存储和页面连接。' : '表单决定要录入和展示哪些字段。') }}</p>
                </div>
                <div v-if="selectedPageBlockFormDataState.status === 'error'" class="crud-object-source-actions">
                  <n-button size="small" type="primary" :loading="saving" @click="saveDraft">
                    重新准备
                  </n-button>
                </div>
              </div>
            </template>
            <n-empty
              v-if="selectedPageBlock && !supportsFormAsset(selectedPageBlock)"
              size="small"
              description="该组件没有可绑定的数据表单"
            />
            <p
              v-else-if="supportsFormAsset(selectedPageBlock)"
              class="form-asset-default-hint"
            >
              新增/编辑表单请在顶部「页面资源」里点「设为新增编辑表单」。
            </p>
          </div>
          <ListPageGridDesigner
            v-if="inspectorTab === 'properties' && selectedPageBlock"
            panel-only
            :model-value="designerGridLayout"
            :model-schema="applicationGridModelSchema"
            :fields="selectedPageBlockFields"
            :form-designer-schema="selectedPageBlockFormDesignerSchema"
            :pages="appPageTargetOptions"
            :active-block-id="selectedPageBlockId"
            @update:model-value="updateCurrentGridLayout"
          />
        </aside>
      </div>

      <!-- 编辑/草稿预览：页面设置（与上方主分支条件一致，避免嵌套分支漏接） -->
      <section v-else-if="(editing || isDraftPreviewMode()) && activePageDesignTab === 'settings'" class="runtime-inline-panel">
        <PageDesignSettingsPanel
          v-if="currentNode"
          :node="currentNode"
          :application="application"
          :objects="objects"
          @update="patchCurrentPageNode"
        />
        <n-empty v-else description="请先选择要设置的页面" />
      </section>

      <!-- 编辑/草稿预览：发布 -->
      <section v-else-if="(editing || isDraftPreviewMode()) && activePageDesignTab === 'publish'" class="runtime-inline-panel">
        <PageDesignPublishPanel
          v-if="currentNode"
          :application="application"
          :node="currentNode"
          :page-id="currentNode.id"
          :page-title="currentNode.title"
          :dirty="dirty"
          :saving="saving"
          :config-key="pageDesignObject?.configKey || currentNode.objectRef?.configKey || ''"
          :objects="objects"
          @save="saveCurrentDesignerSection"
          @preview="openDraftPreview"
          @update="patchCurrentPageNode"
        />
        <n-empty v-else description="请先选择要发布的页面" />
      </section>

      <section
        v-show="!editing && (runtimeViewMode === 'process' || runtimeViewMode === 'enhance' || runtimeViewMode === 'settings')"
        class="runtime-inline-panel"
        :class="{
          'runtime-process-panel': runtimeViewMode === 'process',
          'runtime-enhance-panel': runtimeViewMode === 'enhance',
        }"
      >
        <!-- keep-alive：二次进入 Tab 不重挂；首次慢主要是 async chunk + 列表接口，空闲时预取 chunk -->
        <keep-alive>
          <ApplicationProcessPanel
            v-if="runtimeViewMode === 'process'"
            :key="'runtime-process'"
            :application="application"
            :initial-objects="processSelectableObjects"
            @changed="refreshWorkspaceMetadata"
            @navigate="handleProcessPanelNavigate"
            @open-designer="openProcessDesigner"
          />
          <ApplicationExtensionsPanel
            v-else-if="runtimeViewMode === 'enhance'"
            :key="'runtime-enhance'"
            embedded
            :application="application"
            :initial-extensions="workspaceExtensions"
            :initial-objects="objects"
            :initial-entries="workspaceEntries"
            :initial-pages="builder?.nodes || []"
            :context-page-id="enhanceContextPageId"
            @changed="handleExtensionsChanged"
            @open-designer="openEmbeddedObjectActions"
          />
          <ApplicationSettingsPanel
            v-else-if="runtimeViewMode === 'settings'"
            :key="'runtime-settings'"
            :application="application"
            @saved="refreshWorkspaceMetadata"
          />
        </keep-alive>
      </section>
    </template>
    <n-result
      v-else
      status="warning"
      :title="loadErrorTitle"
      :description="loadError || '没有找到可用的应用运行配置。'"
      class="runtime-load-result"
    >
      <template #footer>
        <n-space justify="center">
          <n-button @click="router.push('/app-center')">
            返回应用中心
          </n-button>
          <n-button v-if="canEditApplication && !isDraftMode" secondary @click="editing = true">
            进入页面设计
          </n-button>
          <n-button type="primary" @click="load">
            重新加载
          </n-button>
        </n-space>
      </template>
    </n-result>
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
          <n-alert type="error" :bordered="false" class="navigation-action-danger">
            <div class="navigation-action-tip">
              <strong>危险操作</strong>
              <p>{{ navigationDeleteTip }}</p>
              <ul v-if="navigationDeleteImpact.impactedObjects.length" class="navigation-action-impact">
                <li v-for="item in navigationDeleteImpact.impactedObjects" :key="String(item.objectId || item.objectCode)">
                  {{ item.objectName || item.objectCode }}
                  <template v-if="item.objectCode">
                    （{{ item.objectCode }}）
                  </template>
                  <template v-if="item.tableName">
                    · 物理表 <code>{{ item.tableName }}</code>
                  </template>
                </li>
              </ul>
              <p class="navigation-action-tip-secondary">
                数据库物理表及表内业务数据不会被删除；如需删表请由管理员在数据源侧另行处理。
              </p>
            </div>
          </n-alert>
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
            {{ navigationActionMode === 'delete' ? '确认删除' : '确认' }}
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

    <n-drawer v-model:show="objectSetupVisible" width="min(1080px, 96vw)">
      <n-drawer-content title="高级数据设置" closable>
        <ApplicationObjectsPanel
          v-if="application"
          :application="application"
          :initial-objects="objects"
          :open-designer-after-create="false"
          @changed="handleApplicationObjectsChanged"
          @open-designer="payload => openObjectDesigner(payload.panel, payload)"
        />
      </n-drawer-content>
    </n-drawer>

    <PageTypeSelector
      v-model:show="pageTypeSelectorVisible"
      :default-parent-id="pageTypeSelectorParentId"
      :default-page-type="pageTypeSelectorDefaultType"
      @confirm="handlePageTypeSelection"
    />

    <PageSystemMenuMountDialog
      v-model:show="systemMenuMountVisible"
      :node="systemMenuMountNode"
      :page-title="systemMenuMountNode?.title || ''"
      :saving="saving"
      @confirm="confirmSystemMenuMount"
      @unmount="unmountSystemMenu"
    />

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

    <n-modal
      v-model:show="embeddedProcessDesignerVisible"
      :mask-closable="false"
      :auto-focus="false"
      class="embedded-process-designer-modal"
    >
      <div class="embedded-process-designer-shell">
        <BusinessProcessPage
          v-if="embeddedProcessDesignerVisible"
          embedded
          :process-id="embeddedProcessDesignerId"
          @close="embeddedProcessDesignerVisible = false"
          @saved="handleEmbeddedProcessDesignerSaved"
        />
      </div>
    </n-modal>
  </div>
</template>

<script>
import { applicationRuntimeLocalComponents } from './runtime-modules/applicationRuntimeLocalComponents'
import { useApplicationRuntime } from './runtime-modules/useApplicationRuntime'

export default {
  name: 'ApplicationRuntimePage',
  components: {
    ...applicationRuntimeLocalComponents,
  },
  setup() {
    return useApplicationRuntime()
  },
}
</script>

<style scoped src="./runtime-modules/application-runtime-shell.css"></style>
<style scoped src="./runtime-modules/application-runtime-canvas.css"></style>
