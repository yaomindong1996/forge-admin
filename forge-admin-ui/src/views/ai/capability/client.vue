<template>
  <div class="capability-client-page">
    <CapabilityPageHeader title="接入系统" description="每个外部系统创建一个客户端，独立管理凭据、可调用能力和实际操作人。" active="client" />
    <AiCrudPage
      v-show="!workbenchVisible"
      ref="crudRef"
      :api-config="{
        list: 'get@/ai/capability/client/page',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :show-render-mode-switch="false"
      class="platform-list"
    >
      <template #toolbar-start>
        <n-button v-if="canAdd" type="primary" @click="openAddModal">
          接入新系统
        </n-button>
      </template>
    </AiCrudPage>

    <CapabilityClientWorkbenchModal
      v-model:show="workbenchVisible"
      :client="workbenchClient"
      :initial-tab="workbenchInitialTab"
      :can-rotate="canRotate"
      :can-edit="canEdit"
      :can-revoke="canRevoke"
      :can-grant="canGrant"
      :can-grant-query="canGrantQuery"
      :can-grant-revoke="canGrantRevoke"
      :can-log-query="canLogQuery"
      @rotate-secret="handleRotateSecret"
      @rotate-signing-key="handleRotateSigningKey"
      @configure-identity="openIdentityFromWorkbench"
      @revoke="handleRevoke"
    />

    <!-- 新增客户端弹窗 -->
    <n-modal
      v-model:show="addVisible"
      title="接入一个外部系统"
      preset="card"
      style="width: min(560px, calc(100vw - 32px)); max-height: 90vh; overflow: auto"
    >
      <n-form
        ref="addFormRef"
        :model="addForm"
        :rules="addRules"
        label-placement="top"
        label-width="110px"
      >
        <n-form-item label="系统标识" path="clientCode">
          <div class="form-control-stack">
            <n-input v-model:value="addForm.clientCode" placeholder="如 erp-sync，创建后不可修改" />
            <p>外围系统的稳定标识，用于日志检索和接入识别；创建后不能修改。</p>
          </div>
        </n-form-item>
        <n-form-item label="系统名称" path="clientName">
          <n-input v-model:value="addForm.clientName" placeholder="例如：ERP 系统、企业门户" />
        </n-form-item>
        <n-form-item label="谁来执行操作" path="actorMode">
          <div class="form-control-stack">
            <n-select
              v-model:value="addForm.actorMode"
              :options="actorModeOptions"
              placeholder="请选择主体模式"
            />
            <p>有真实操作人选“用户委托”；后台任务选“服务身份”；两种都要用选“混合模式”。</p>
          </div>
        </n-form-item>
        <n-alert v-if="!actorModeOptions.length" type="error" class="form-alert">
          执行身份选项加载失败，请刷新后重试或联系管理员。
        </n-alert>
        <n-form-item v-if="requiresServiceIdentity" label="服务账号" path="serviceUserId">
          <div class="form-control-stack">
            <UserSelectPicker
              v-model="addForm.serviceUserId"
              v-model:label-value="serviceUserLabel"
              title="选择机器客户端服务账号"
              placeholder="请选择服务账号"
              :clearable="false"
              @select="handleServiceUserSelect"
            />
            <p>服务身份调用时使用该 Forge 用户的角色和权限，请使用专用最小权限账号。</p>
          </div>
        </n-form-item>
        <n-form-item v-if="requiresServiceIdentity" label="生效组织" path="activeOrgId">
          <div class="form-control-stack">
            <n-select
              v-model:value="addForm.activeOrgId"
              :options="serviceOrgOptions"
              :loading="serviceOrgLoading"
              :disabled="!addForm.serviceUserId"
              placeholder="请选择服务账号所属组织"
              filterable
            >
              <template #empty>
                <n-empty size="small" description="该账号未绑定可用组织" />
              </template>
            </n-select>
            <p>决定服务账号执行能力时的数据组织上下文和数据权限范围。</p>
          </div>
        </n-form-item>
        <n-form-item label="认证模式" path="authModes">
          <div class="form-control-stack">
            <n-select
              v-model:value="addForm.authModes"
              multiple
              placeholder="默认 OAUTH"
              :options="authModeOptions"
              :disabled="addForm.actorMode === 'USER_DELEGATION'"
              clearable
            />
            <p>OAuth 适合标准 Token 接入和用户委托；HMAC 适合无 OAuth 组件的服务间签名调用。</p>
          </div>
        </n-form-item>
        <n-alert v-if="!authModeOptions.length" type="error" class="form-alert">
          认证模式字典尚未初始化，请先确认 Flyway V1.0.74 已成功执行。
        </n-alert>
        <n-form-item label="过期时间" path="expiresAt">
          <div class="form-control-stack">
            <n-date-picker
              v-model:value="addForm.expiresAt"
              type="datetime"
              placeholder="不填则长期有效"
              clearable
              class="w-full"
            />
            <p>到期后所有认证立即失效；生产客户端建议设置有效期并定期轮换凭据。</p>
          </div>
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input
            v-model:value="addForm.remark"
            type="textarea"
            placeholder="请输入备注"
            :rows="2"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="addVisible = false">
            取消
          </n-button>
          <n-button
            type="primary"
            :loading="addLoading"
            :disabled="!authModeOptions.length || !actorModeOptions.length"
            @click="handleAddSubmit"
          >
            创建接入凭据
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 客户端签名用户断言配置 -->
    <n-modal
      v-model:show="userAssertionVisible"
      preset="card"
      title="用户身份断言"
      class="user-assertion-modal"
      style="width: min(920px, calc(100vw - 32px))"
      :mask-closable="false"
      @after-leave="clearUserAssertionState"
    >
      <n-spin :show="userAssertionLoading">
        <template v-if="userAssertionConfig">
          <div class="assertion-heading">
            <div>
              <strong>{{ userAssertionConfig.clientName }}</strong>
              <span>{{ userAssertionConfig.clientCode }} · AppId {{ userAssertionConfig.clientId }}</span>
            </div>
            <n-tag :type="userAssertionConfig.enabled ? 'success' : 'default'" size="small">
              {{ userAssertionConfig.enabled ? '已启用' : '未启用' }}
            </n-tag>
          </div>

          <n-alert type="info" :show-icon="true" class="assertion-alert">
            外围系统使用当前客户端私钥签发最长 {{ userAssertionConfig.maxTtlSeconds }} 秒的 RS256 JWT；Forge 按当前受控映射规则解析真实普通用户。
          </n-alert>

          <div class="assertion-protocol-grid">
            <div>
              <span>Issuer</span>
              <code>{{ userAssertionConfig.issuer }}</code>
            </div>
            <div>
              <span>Audience</span>
              <code>{{ userAssertionConfig.audience }}</code>
            </div>
            <div class="protocol-wide">
              <span>Subject Token Type</span>
              <code>{{ userAssertionConfig.subjectTokenType }}</code>
            </div>
            <div>
              <span>当前 kid</span>
              <code>{{ userAssertionConfig.keyId || '尚未生成' }}</code>
            </div>
            <div>
              <span>密钥版本</span>
              <code>{{ userAssertionConfig.keyVersion ? `v${userAssertionConfig.keyVersion}` : '-' }}</code>
            </div>
          </div>

          <div class="assertion-actions">
            <n-button type="primary" :loading="userAssertionRotating" @click="handleRotateUserAssertionKey">
              <template #icon>
                <i class="i-material-symbols:key-vertical-outline-rounded" />
              </template>
              {{ userAssertionConfig.keyVersion ? '轮换 RSA 密钥' : '生成 RSA 密钥' }}
            </n-button>
            <n-button
              v-if="userAssertionConfig.enabled"
              type="warning"
              :loading="userAssertionDisabling"
              @click="handleDisableUserAssertion"
            >
              <template #icon>
                <i class="i-material-symbols:pause-circle-outline-rounded" />
              </template>
              停用用户断言
            </n-button>
          </div>

          <section class="mapping-section">
            <div class="mapping-heading">
              <div>
                <h3>外围用户映射</h3>
                <p>原始外围标识只生成 SHA-256 指纹，不会保存到数据库。自动匹配必须由管理员为当前客户端显式启用。</p>
              </div>
            </div>
            <div class="mapping-rule-row">
              <div>
                <strong>映射规则</strong>
                <p>{{ mappingRuleDescription }}</p>
              </div>
              <n-select
                :value="userAssertionConfig.mappingMode || 'PREBOUND'"
                :options="mappingModeOptions"
                :loading="mappingRuleUpdating"
                style="width: 220px"
                @update:value="handleMappingRuleChange"
              />
            </div>
            <n-form
              ref="mappingFormRef"
              :model="mappingForm"
              :rules="mappingRules"
              label-placement="top"
              class="mapping-form"
            >
              <n-form-item label="外围用户标识（JWT sub）" path="externalSubject">
                <n-input
                  v-model:value="mappingForm.externalSubject"
                  maxlength="512"
                  show-count
                  placeholder="请输入外围系统中稳定且唯一的用户标识"
                />
              </n-form-item>
              <n-form-item label="Forge 普通用户" path="userId">
                <UserSelectPicker
                  v-model="mappingForm.userId"
                  v-model:label-value="mappingUserLabel"
                  title="选择要绑定的 Forge 普通用户"
                  placeholder="请选择已分配组织和角色的普通用户"
                  :clearable="true"
                />
              </n-form-item>
              <n-button type="primary" :loading="mappingSubmitting" @click="handleAddUserAssertionMapping">
                <template #icon>
                  <i class="i-material-symbols:link-rounded" />
                </template>
                添加映射
              </n-button>
            </n-form>

            <div class="mapping-toolbar">
              <n-input
                v-model:value="mappingKeyword"
                clearable
                placeholder="搜索外围标识提示、用户名或姓名"
                @keyup.enter="searchMappingPage"
              />
              <n-button :loading="mappingPageLoading" @click="searchMappingPage">
                查询
              </n-button>
            </div>

            <n-data-table
              :columns="mappingColumns"
              :data="mappingRows"
              :row-key="row => row.id"
              :bordered="false"
              :loading="mappingPageLoading"
              :pagination="mappingPagination"
              :scroll-x="760"
              remote
              size="small"
              class="mapping-table"
              @update:page="handleMappingPageChange"
              @update:page-size="handleMappingPageSizeChange"
            />
          </section>
        </template>
      </n-spin>
    </n-modal>

    <!-- 一次性凭据展示弹窗 -->
    <n-modal
      v-model:show="issuedVisible"
      preset="card"
      :title="issuedCredential?.title || '凭据已生成'"
      style="width: min(640px, calc(100vw - 32px))"
      :mask-closable="false"
      :closable="false"
      @after-leave="clearIssuedCredential"
    >
      <n-alert type="warning" :show-icon="true">
        以下凭据仅展示一次。关闭前请完成保存，之后无法从系统再次查看。
      </n-alert>

      <div v-if="issuedCredential?.clientSecret" class="issued-block">
        <div class="issued-label">
          客户端密钥（clientSecret）
        </div>
        <div class="issued-value">
          <code>{{ issuedCredential.clientSecret }}</code>
          <n-button quaternary circle aria-label="复制客户端密钥" @click="copy(issuedCredential.clientSecret, '客户端密钥已复制')">
            <template #icon>
              <i class="i-material-symbols:content-copy-outline-rounded" />
            </template>
          </n-button>
        </div>
      </div>

      <div v-if="issuedCredential?.signingKey" class="issued-block">
        <div class="issued-label">
          签名密钥（signingKey）
        </div>
        <div class="issued-value">
          <code>{{ issuedCredential.signingKey }}</code>
          <n-button quaternary circle aria-label="复制签名密钥" @click="copy(issuedCredential.signingKey, '签名密钥已复制')">
            <template #icon>
              <i class="i-material-symbols:content-copy-outline-rounded" />
            </template>
          </n-button>
        </div>
      </div>

      <div v-if="issuedCredential?.privateKeyPem" class="issued-block">
        <div class="issued-label">
          用户断言私钥（PKCS#8 PEM）
        </div>
        <n-input
          :value="issuedCredential.privateKeyPem"
          type="textarea"
          readonly
          :autosize="{ minRows: 8, maxRows: 14 }"
          class="private-key-value"
        />
        <n-space class="private-key-actions">
          <n-button size="small" @click="copy(issuedCredential.privateKeyPem, '用户断言私钥已复制')">
            <template #icon>
              <i class="i-material-symbols:content-copy-outline-rounded" />
            </template>
            复制私钥
          </n-button>
          <n-button size="small" @click="downloadIssuedPrivateKey">
            <template #icon>
              <i class="i-material-symbols:download-rounded" />
            </template>
            下载 PEM
          </n-button>
        </n-space>
      </div>

      <div class="issued-meta">
        <span>客户端 ID / AppId</span>
        <strong>{{ issuedCredential?.clientId || '-' }}</strong>
        <span>客户端编码</span>
        <strong>{{ issuedCredential?.clientCode || '-' }}</strong>
        <template v-if="issuedCredential?.keyPrefix">
          <span>凭据前缀</span>
          <strong>{{ issuedCredential.keyPrefix }}</strong>
        </template>
        <template v-if="issuedCredential?.credentialVersion != null">
          <span>密钥版本</span>
          <strong>v{{ issuedCredential.credentialVersion }}</strong>
        </template>
        <template v-if="issuedCredential?.signingKeyVersion != null">
          <span>签名密钥版本</span>
          <strong>v{{ issuedCredential.signingKeyVersion }}</strong>
        </template>
        <template v-if="issuedCredential?.keyId">
          <span>用户断言 kid</span>
          <strong>{{ issuedCredential.keyId }}</strong>
        </template>
        <template v-if="issuedCredential?.keyVersion != null">
          <span>用户断言密钥版本</span>
          <strong>v{{ issuedCredential.keyVersion }}</strong>
        </template>
        <template v-if="issuedCredential?.audience">
          <span>Audience</span>
          <strong>{{ issuedCredential.audience }}</strong>
          <span>最大有效期</span>
          <strong>{{ issuedCredential.maxTtlSeconds }} 秒</strong>
        </template>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button v-if="newlyCreatedClientId && canGrantQuery" type="primary" @click="continueToAuthorization">
            我已保存，继续授权
          </n-button>
          <n-button :type="newlyCreatedClientId && canGrantQuery ? 'default' : 'primary'" @click="issuedVisible = false">
            我已安全保存
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { AiCrudPage } from '@/components/ai-form'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import { copy } from '@/utils/clipboard'

import CapabilityClientWorkbenchModal from './components/CapabilityClientWorkbenchModal.vue'
import CapabilityPageHeader from './components/CapabilityPageHeader.vue'
import { useCapabilityClients } from './useCapabilityClients'

defineOptions({ name: 'CapabilityClient' })

const {
  authModeOptions,
  actorModeOptions,
  mappingModeOptions,
  canAdd,
  canRotate,
  canEdit,
  canRevoke,
  canGrant,
  canGrantQuery,
  canGrantRevoke,
  canLogQuery,
  crudRef,
  workbenchVisible,
  workbenchInitialTab,
  newlyCreatedClientId,
  continueToAuthorization,
  workbenchClient,
  addVisible,
  addLoading,
  addFormRef,
  serviceUserLabel,
  serviceOrgOptions,
  serviceOrgLoading,
  addForm,
  addRules,
  requiresServiceIdentity,
  openAddModal,
  handleServiceUserSelect,
  handleAddSubmit,
  issuedVisible,
  issuedCredential,
  clearIssuedCredential,
  downloadIssuedPrivateKey,
  userAssertionVisible,
  userAssertionLoading,
  userAssertionRotating,
  userAssertionDisabling,
  userAssertionConfig,
  mappingFormRef,
  mappingSubmitting,
  mappingPageLoading,
  mappingRuleUpdating,
  mappingRows,
  mappingKeyword,
  mappingPagination,
  mappingUserLabel,
  mappingForm,
  mappingRules,
  mappingRuleDescription,
  mappingColumns,
  openIdentityFromWorkbench,
  clearUserAssertionState,
  searchMappingPage,
  handleMappingPageChange,
  handleMappingPageSizeChange,
  handleMappingRuleChange,
  handleRotateUserAssertionKey,
  handleDisableUserAssertion,
  handleAddUserAssertionMapping,
  handleRotateSecret,
  handleRotateSigningKey,
  handleRevoke,
  searchSchema,
  tableColumns,
} = useCapabilityClients()
</script>

<style scoped>
.capability-client-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.platform-list {
  flex: 1;
  min-height: 0;
}

.w-full {
  width: 100%;
}

.form-alert {
  margin-bottom: 18px;
}

.form-control-stack {
  width: 100%;
}

.form-control-stack > p {
  margin: 6px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.55;
}

.issued-block {
  margin-top: 16px;
}

.issued-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.issued-value {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-secondary);
  border-radius: 4px;
  padding: 8px 12px;
}

.issued-value code {
  flex: 1;
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: 13px;
  word-break: break-all;
}

.issued-meta {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px 16px;
  margin-top: 16px;
  font-size: 13px;
}

.issued-meta span {
  color: var(--text-secondary);
}

.issued-meta strong {
  font-family: 'Courier New', 'Consolas', monospace;
}

.user-assertion-modal :deep(.n-spin-container) {
  min-height: 260px;
}

.assertion-heading,
.assertion-actions,
.mapping-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.assertion-heading > div {
  min-width: 0;
}

.assertion-heading strong,
.assertion-heading span {
  display: block;
}

.assertion-heading strong {
  color: var(--text-primary);
  font-size: 15px;
}

.assertion-heading span {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.assertion-alert {
  margin-top: 16px;
}

.assertion-protocol-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
  border-top: 1px solid var(--border-light);
  border-left: 1px solid var(--border-light);
}

.assertion-protocol-grid > div {
  min-width: 0;
  padding: 11px 13px;
  border-right: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.assertion-protocol-grid span,
.assertion-protocol-grid code {
  display: block;
}

.assertion-protocol-grid span {
  margin-bottom: 5px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.assertion-protocol-grid code {
  overflow-wrap: anywhere;
  color: var(--text-primary);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
}

.protocol-wide {
  grid-column: 1 / -1;
}

.assertion-actions {
  justify-content: flex-start;
  margin-top: 16px;
}

.mapping-section {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

.mapping-heading h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 14px;
}

.mapping-heading p {
  margin: 5px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
}

.mapping-form {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(240px, 1fr) auto;
  align-items: end;
  gap: 12px;
  margin-top: 14px;
}

.mapping-form :deep(.n-form-item) {
  min-width: 0;
  margin-bottom: 0;
}

.mapping-form > .n-button {
  margin-bottom: 1px;
}

.mapping-rule-row,
.mapping-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 14px;
  padding: 13px 14px;
  border-radius: 6px;
  background: var(--bg-secondary);
}

.mapping-rule-row > div {
  min-width: 0;
}

.mapping-rule-row strong {
  color: var(--text-primary);
  font-size: 13px;
}

.mapping-rule-row p {
  margin: 5px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.mapping-toolbar {
  justify-content: flex-start;
  padding: 0;
  background: transparent;
}

.mapping-toolbar .n-input {
  max-width: 360px;
}

.mapping-table {
  margin-top: 16px;
  border-top: 1px solid var(--border-light);
}

.private-key-value :deep(textarea) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.private-key-actions {
  margin-top: 10px;
}

@media (max-width: 760px) {
  .assertion-protocol-grid,
  .mapping-form {
    grid-template-columns: 1fr;
  }

  .protocol-wide {
    grid-column: auto;
  }

  .assertion-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .mapping-rule-row,
  .mapping-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .mapping-rule-row .n-select,
  .mapping-toolbar .n-input {
    width: 100% !important;
    max-width: none;
  }
}
</style>
