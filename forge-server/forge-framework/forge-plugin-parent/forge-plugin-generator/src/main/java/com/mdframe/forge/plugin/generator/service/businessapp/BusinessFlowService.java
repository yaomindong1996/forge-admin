package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowResubmitDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskActionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 业务流程服务。
 * <p>
 * 负责业务对象与流程引擎的动态集成：
 * - 读取 ai_business_binding (binding_type=FLOW) 中的流程绑定配置
 * - 从业务记录动态发起流程（不依赖硬编码注解）
 * - 处理流程回调，更新业务记录状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FlowBind(modelKey = "*", businessType = "lowcode-business")
public class BusinessFlowService {

    private static final String FLOW_START_LOCK_PREFIX = "forge:business-flow:start:";
    private static final long FLOW_START_LOCK_WAIT_SECONDS = 5L;

    @Autowired(required = false)
    private FlowClient flowClient;

    private final BusinessBindingMapper bindingMapper;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessDocumentRuntimeService documentRuntimeService;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessFlowVariableResolver variableResolver;
    private final BusinessCodeFormProviderRegistry codeFormProviderRegistry;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final ObjectProvider<BusinessActionExecutionService> actionExecutionServiceProvider;
    private final Map<String, ReentrantLock> localFlowStartLocks = new ConcurrentHashMap<>();

    /**
     * 从业务记录发起流程
     *
     * @param objectCode 业务对象编码
     * @param recordId   业务记录ID
     * @param recordData 业务记录数据
     * @return 流程发起结果
     */
    public JSONObject startFlow(String objectCode, String recordId, Map<String, Object> recordData) {
        if (flowClient == null) {
            throw new RuntimeException("流程服务未配置，无法发起主流程");
        }
        Long tenantId = resolveTenantId();

        // 1. 查询该对象的 FLOW 绑定配置
        AiBusinessBinding flowBinding = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", objectCode, "FLOW");

        if (flowBinding == null || flowBinding.getBindingConfig() == null) {
            throw new RuntimeException("业务对象 [" + objectCode + "] 未配置流程绑定");
        }

        JSONObject bindingConfig = readBindingConfig(flowBinding.getBindingConfig());
        String flowModelKey = resolveFlowModelKey(bindingConfig);

        if (flowModelKey == null || flowModelKey.isBlank()) {
            throw new RuntimeException("流程绑定配置中缺少 flowModelKey");
        }

        // 2. 构建流程变量
        Map<String, Object> flowVariables = buildFlowVariables(bindingConfig, recordData);

        // 3. 构建业务Key和标题
        String businessKey = objectCode + ":" + recordId;
        String title = buildFlowTitle(bindingConfig, recordData, objectCode);

        // 4. 发起流程
        Long userId = resolveUserId();
        String userName = resolveUsername();

        FlowResult<String> result = flowClient.startProcess(
                flowModelKey, businessKey, title,
                flowVariables, String.valueOf(userId), userName, null, null);

        if (!result.isSuccess()) {
            throw new RuntimeException("流程发起失败: " + result.getMsg());
        }

        JSONObject response = new JSONObject();
        response.put("flowModelKey", flowModelKey);
        response.put("businessKey", businessKey);
        response.put("processInstanceId", result.getData());
        response.put("status", "STARTED");
        return response;
    }

    /**
     * 查询业务对象的流程绑定配置
     */
    public BusinessFlowBindingVO getFlowBinding(String objectCode) {
        Long tenantId = resolveTenantId();
        String canonicalObjectCode = resolveCanonicalObjectCode(tenantId, objectCode);
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, canonicalObjectCode, objectCode);

        if (binding == null) {
            AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, canonicalObjectCode,
                    resolvePublishedRuntimeConfig(tenantId, objectCode));
            if (documentConfig == null || StringUtils.isBlank(documentConfig.getDefaultFlowKey())) {
                return null;
            }
            return legacyDocumentFlowToVO(canonicalObjectCode, documentConfig);
        }
        return toVO(canonicalObjectCode, binding);
    }

    /**
     * 查询流程模型变量候选项和字段映射建议。
     */
    public Map<String, Object> getVariableCandidates(String modelKey, String objectCode) {
        if (StringUtils.isBlank(modelKey)) {
            throw new BusinessException("流程模型Key不能为空");
        }
        return variableResolver.resolve(modelKey, objectCode);
    }

    /**
     * 批量补齐流程任务/抄送列表中的业务对象名称和业务摘要。
     */
    public void enrichBusinessListDisplay(List<FlowBusinessListDisplayItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Long tenantId = resolveTenantId();
        Map<String, AiBusinessFlowInstanceLink> linkByBusinessKey = loadLinksByBusinessKey(tenantId, items);
        Map<String, BusinessRuntimeContext> contextCache = new HashMap<>();
        Map<String, BusinessListGroup> grouped = new LinkedHashMap<>();
        for (FlowBusinessListDisplayItem item : items) {
            if (item == null) {
                continue;
            }
            AiBusinessFlowInstanceLink link = linkByBusinessKey.get(StringUtils.trimToEmpty(item.getBusinessKey()));
            String objectCode = StringUtils.firstNonBlank(
                    link == null ? null : link.getObjectCode(),
                    StringUtils.trimToNull(item.getObjectCode()),
                    parseBusinessKeyObjectCode(item.getBusinessKey()));
            Long recordId = link == null || link.getRecordId() == null
                    ? item.getRecordId()
                    : link.getRecordId();
            if (recordId == null) {
                recordId = parseBusinessKeyRecordId(item.getBusinessKey());
            }
            String businessKey = StringUtils.firstNonBlank(
                    link == null ? null : link.getBusinessKey(),
                    StringUtils.trimToNull(item.getBusinessKey()),
                    objectCode != null && recordId != null ? buildBusinessKey(objectCode, recordId) : null);
            if (StringUtils.isBlank(objectCode) || recordId == null) {
                item.setProcessDefinitionName(StringUtils.firstNonBlank(
                        item.getProcessDefinitionName(), item.getProcessName(), item.getProcessDefKey()));
                continue;
            }
            BusinessRuntimeContext context = contextCache.computeIfAbsent(objectCode,
                    code -> resolveBusinessRuntimeContext(tenantId, code));
            String canonicalObjectCode = StringUtils.firstNonBlank(context.objectCode(), objectCode);
            grouped.computeIfAbsent(canonicalObjectCode,
                            key -> new BusinessListGroup(context, new ArrayList<>()))
                    .runtimes()
                    .add(new BusinessListRuntime(item, canonicalObjectCode, recordId, businessKey));
        }
        grouped.forEach((objectCode, group) -> enrichBusinessListGroup(tenantId, group.context(), group.runtimes()));
    }

    /**
     * 按流程模型 Key 反查已绑定的业务对象。
     */
    public List<BusinessBindingSummaryVO> listBusinessBindingsByModelKey(String modelKey) {
        String key = StringUtils.trimToNull(modelKey);
        if (key == null) {
            throw new BusinessException("流程模型Key不能为空");
        }
        return bindingMapper.selectFlowBindingsByModelKey(resolveTenantId(), key);
    }

    /**
     * 查询业务对象可供流程节点绑定的表单资产。
     */
    public Map<String, Object> getFormAssets(String objectCode) {
        return getFormAssets(objectCode, false);
    }

    /**
     * 查询业务对象可供流程节点绑定的表单资产。
     */
    public Map<String, Object> getFormAssets(String objectCode, boolean includeInternal) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("objectCode", objectCode);
        result.put("formAssets", List.of());
        result.put("providerCatalog", List.of());
        result.put("warnings", List.of());
        if (StringUtils.isBlank(objectCode)) {
            return result;
        }

        Long tenantId = resolveTenantId();
        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> providerCatalog = codeFormProviderRegistry.listProviderCatalog(objectCode, includeInternal);
        BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
        query.setObjectCode(objectCode);
        List<BusinessObjectVO> objects = businessObjectMapper.selectObjectList(tenantId, query);
        if (objects == null || objects.isEmpty()) {
            AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, objectCode);
            JSONObject metadata = readCodeAppMetadata(tenantId, objectCode);
            List<Map<String, Object>> assets = new ArrayList<>(collectRuntimeCrudFormAssets(null, runtimeConfig));
            appendUniqueFormAssets(assets, mergeCodeAppAssets(
                    objectCode, codeFormProviderRegistry.listAssets(objectCode, includeInternal), metadata, includeInternal));
            if (assets.isEmpty()) {
                warnings.add("业务对象不存在或无权限访问，且未找到代码表单资产: " + objectCode);
            } else if (runtimeConfig == null) {
                warnings.add("当前编码未匹配低代码业务对象，仅显示代码表单资产");
            }
            result.put("formAssets", assets);
            if (runtimeConfig != null) {
                result.put("objectCode", StringUtils.defaultIfBlank(runtimeConfig.getObjectCode(), objectCode));
                result.put("objectName", StringUtils.defaultIfBlank(runtimeConfig.getObjectName(), runtimeConfig.getAppName()));
                result.put("configKey", runtimeConfig.getConfigKey());
            }
            result.put("providerCatalog", providerCatalog);
            result.put("codeAppMetadata", sanitizeCodeAppMetadata(metadata, includeInternal));
            result.put("warnings", warnings);
            return result;
        }

        BusinessObjectVO object = objects.get(0);
        AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, StringUtils.firstNonBlank(
                object.getConfigKey(), object.getObjectCode(), objectCode));
        JSONObject designerOptions = readJsonObject(object.getDesignerOptions());
        JSONObject formSchema = readNestedObject(designerOptions.get("formDesignerSchema"));
        List<Map<String, Object>> assets = new ArrayList<>(collectBusinessFormAssets(object, formSchema));
        appendUniqueFormAssets(assets, collectRuntimeCrudFormAssets(object, runtimeConfig));
        JSONObject metadata = readCodeAppMetadata(tenantId, object.getObjectCode());
        appendUniqueFormAssets(assets, mergeCodeAppAssets(
                object.getObjectCode(), codeFormProviderRegistry.listAssets(object.getObjectCode(), includeInternal),
                metadata, includeInternal));
        if (assets.isEmpty()) {
            warnings.add("业务对象尚未配置低代码表单资产");
        }
        result.put("objectId", object.getId());
        result.put("objectCode", object.getObjectCode());
        result.put("objectName", object.getObjectName());
        result.put("configKey", runtimeConfig == null ? object.getConfigKey() : runtimeConfig.getConfigKey());
        result.put("formAssets", assets);
        result.put("providerCatalog", providerCatalog);
        result.put("codeAppMetadata", sanitizeCodeAppMetadata(metadata, includeInternal));
        result.put("warnings", warnings);
        return result;
    }

    /**
     * 查询代码应用配置化元数据。未配置时返回空 Map，调用方继续使用 Provider 默认资产。
     */
    public Map<String, Object> getCodeAppMetadata(String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return Map.of();
        }
        return new LinkedHashMap<>(sanitizeCodeAppMetadata(readCodeAppMetadata(resolveTenantId(), objectCode), true));
    }

    /**
     * 只更新已有流程绑定中的代码应用元数据，避免字段/视图配置覆盖流程模型和变量映射。
     */
    public boolean saveCodeAppMetadata(String objectCode, Object metadata) {
        if (StringUtils.isBlank(objectCode) || !(metadata instanceof Map<?, ?> || metadata instanceof JSONObject)) {
            return false;
        }
        Long tenantId = resolveTenantId();
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, objectCode);
        boolean created = false;
        if (binding == null) {
            binding = new AiBusinessBinding();
            binding.setTenantId(tenantId);
            binding.setTargetType("OBJECT");
            binding.setTargetCode(objectCode);
            binding.setBindingType("FLOW");
            binding.setBindingName(objectCode + "业务表单资产配置");
            binding.setStatus(1);
            binding.setSortOrder(0);
            created = true;
        }
        JSONObject config = readBindingConfig(binding.getBindingConfig());
        JSONObject options = readNestedObject(config.get("options"));
        options.put("codeAppMetadata", readNestedObject(metadata));
        config.put("options", options);
        binding.setBindingConfig(config.toJSONString());
        if (created) {
            bindingMapper.insert(binding);
        } else {
            bindingMapper.updateById(binding);
        }
        return true;
    }

    /**
     * 查询待办任务对应的业务表单上下文。
     */
    public BusinessTaskFormContextVO getTaskFormContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        validateTaskAccess(effectiveQuery, false);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, false);
        return buildTaskFormContext(effectiveQuery, runtime);
    }

    /**
     * 查询当前用户已签收、可直接办理的待办上下文。
     *
     * <p>该只读校验入口供受控流程动作在 elicitation 前确认真实办理权使用，
     * 候选但未签收的任务不会被视为可办理任务。</p>
     */
    public BusinessTaskFormContextVO getActionableTaskFormContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        validateTaskAccess(effectiveQuery, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, true);
        return buildTaskFormContext(effectiveQuery, runtime);
    }

    /**
     * 查询历史/已办场景下的业务表单上下文，只用于只读展示，不校验运行中待办任务身份。
     */
    public BusinessTaskFormContextVO getTaskFormReadonlyContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, false);
        BusinessTaskFormContextVO context = buildTaskFormContext(effectiveQuery, runtime);
        makeBusinessTaskFormReadonly(context);
        return context;
    }

    /**
     * 保存待办任务允许编辑的业务字段，并返回最新上下文。
     */
    public BusinessTaskFormContextVO saveTaskFormContext(BusinessTaskFormSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务待办表单参数不能为空");
        }
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());
        query.setObjectCode(dto.getObjectCode());
        query.setRecordId(dto.getRecordId());
        query.setFormKey(dto.getFormKey());

        validateTaskAccess(query, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true);
        JSONObject nodeForm = resolveTaskNodeForm(runtime, query);
        if (nodeForm == null || nodeForm.isEmpty()) {
            throw new BusinessException("当前流程节点未配置业务表单权限");
        }
        String formMode = normalizeNodeFormMode(nodeForm.getString("formMode"));
        if ("BUSINESS_CODE_FORM".equals(formMode)) {
            List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
            BusinessTaskFormSaveDTO filteredDto = filterSaveDataByPermissions(dto, permissions);
            validateRequiredTaskFields(permissions, filteredDto.getData(), dto.getData() == null ? Map.of() : dto.getData());
            return saveBusinessCodeFormContext(filteredDto, nodeForm);
        }
        if (!"BUSINESS_OBJECT_FORM".equals(formMode)) {
            throw new BusinessException("当前节点不是平台可保存的业务表单，不能通过平台保存业务字段");
        }
        List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        if (permissions.isEmpty()) {
            String formKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(query.getFormKey()),
                    StringUtils.trimToNull(nodeForm.getString("formKey")));
            BusinessObjectVO object = queryBusinessObject(resolveTenantId(), runtime.objectCode());
            JSONObject formSchema = resolveBusinessFormSchema(object, formKey, runtime.configKey());
            List<Map<String, Object>> fieldCatalog = resolveBusinessTaskCrudPageFields(runtime.configKey(), formKey, formSchema);
            permissions = normalizeBusinessObjectTaskPermissions(fieldCatalog, permissions);
        }
        Set<String> writableFields = collectPermissionFields(permissions, "writable", true);
        if (writableFields.isEmpty()) {
            throw new BusinessException("当前节点没有可编辑业务字段");
        }

        Map<String, Object> input = dto.getData() == null ? Map.of() : dto.getData();
        Map<String, Object> updateData = new LinkedHashMap<>();
        for (String field : writableFields) {
            if (input.containsKey(field)) {
                updateData.put(field, input.get(field));
            }
        }
        validateRequiredTaskFields(permissions, updateData, input);
        if (updateData.isEmpty()) {
            throw new BusinessException("未提交可编辑业务字段");
        }

        dynamicCrudService.updateInternalFieldsById(runtime.configKey(), runtime.recordId(), updateData);
        return buildTaskFormContext(query, runtime);
    }

    /**
     * 办理低代码业务待办。该入口在 Flowable 任务完成后同步业务流程实例和业务单据状态，
     * 避免低代码单据状态停留在发起时的 IN_PROCESS。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO completeBusinessTask(BusinessTaskActionDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务待办办理参数不能为空");
        }
        String action = StringUtils.defaultIfBlank(dto.getAction(), "approve").trim().toLowerCase();
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new BusinessException("当前业务待办仅支持同意或驳回");
        }
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法办理业务待办");
        }

        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());
        query.setObjectCode(dto.getObjectCode());
        query.setRecordId(dto.getRecordId());
        query.setFormKey(dto.getFormKey());

        validateTaskAccess(query, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true);
        Map<String, Object> variables = dto.getVariables() == null ? Map.of() : dto.getVariables();
        String userId = String.valueOf(resolveUserId());

        FlowResult<Void> result = "reject".equals(action)
                ? flowClient.reject(query.getTaskId(), userId, dto.getComment(), dto.getSignature(),
                        resolveTrustedTaskTenant(dto), dto.getIdempotencyKey(), dto.getRequestDigest())
                : flowClient.approve(query.getTaskId(), userId, dto.getComment(), dto.getSignature(), variables,
                        resolveTrustedTaskTenant(dto), dto.getIdempotencyKey(), dto.getRequestDigest());
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null
                    ? "业务待办办理失败"
                    : StringUtils.defaultIfBlank(result.getMsg(), "业务待办办理失败"));
        }

        return syncBusinessFlowStatusAfterTaskAction(runtime, query, action, variables);
    }

    /**
     * 仅供 FLOW_ACTION 本地审计恢复调用。此时远程任务可能已完成，不能再要求它处于 actionable；
     * Flow 服务仍以同租户、原签收人、动作、幂等键和请求摘要做最终裁决。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO recoverCapabilityTaskAction(BusinessTaskActionDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getTaskId())
                || StringUtils.isBlank(dto.getIdempotencyKey())
                || StringUtils.isBlank(dto.getRequestDigest())) {
            throw new BusinessException(409, "FLOW_RECOVERY_EVIDENCE_REQUIRED");
        }
        String action = StringUtils.defaultIfBlank(dto.getAction(), "approve").trim().toLowerCase();
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new BusinessException(409, "POLICY_MISMATCH");
        }
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法恢复业务待办");
        }
        Long tenantId = resolveTrustedTaskTenant(dto);
        String userId = String.valueOf(resolveUserId());
        Map<String, Object> variables = Map.of();
        FlowResult<Void> result = "reject".equals(action)
                ? flowClient.reject(dto.getTaskId(), userId, dto.getComment(), dto.getSignature(),
                        tenantId, dto.getIdempotencyKey(), dto.getRequestDigest())
                : flowClient.approve(dto.getTaskId(), userId, dto.getComment(), dto.getSignature(), variables,
                        tenantId, dto.getIdempotencyKey(), dto.getRequestDigest());
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null
                    ? "业务待办恢复失败"
                    : StringUtils.defaultIfBlank(result.getMsg(), "业务待办恢复失败"));
        }

        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setObjectCode(dto.getObjectCode());
        query.setRecordId(dto.getRecordId());
        if (StringUtils.isNotBlank(dto.getObjectCode()) && dto.getRecordId() != null) {
            String objectCode = resolveCanonicalObjectCode(tenantId, dto.getObjectCode());
            query.setBusinessKey(buildBusinessKey(objectCode, dto.getRecordId()));
        }
        return syncBusinessFlowStatusAfterTaskAction(null, query, action, variables);
    }

    private Long resolveTrustedTaskTenant(BusinessTaskActionDTO dto) {
        Long currentTenantId = resolveTenantId();
        if (dto.getTenantId() != null && !dto.getTenantId().equals(currentTenantId)) {
            throw new BusinessException(403, "FLOW_TASK_TENANT_MISMATCH");
        }
        return currentTenantId;
    }

    private BusinessFlowRuntimeVO syncBusinessFlowStatusAfterTaskAction(TaskFormRuntimeContext runtime,
                                                                        BusinessTaskFormContextQueryDTO query,
                                                                        String action,
                                                                        Map<String, Object> variables) {
        String businessKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getBusinessKey()),
                runtime == null ? null : StringUtils.trimToNull(runtime.businessKey()));
        String processInstanceId = StringUtils.trimToNull(query.getProcessInstanceId());
        AiBusinessFlowInstanceLink link = findRuntimeLink(resolveTenantId(), processInstanceId, businessKey);
        if (link == null) {
            BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
            vo.setObjectCode(runtime == null ? null : runtime.objectCode());
            vo.setRecordId(runtime == null ? null : runtime.recordId());
            vo.setBusinessKey(businessKey);
            vo.setProcessInstanceId(processInstanceId);
            vo.setFlowStatus("IN_PROCESS");
            vo.setMessage("业务待办已办理，未找到低代码流程实例关联");
            return vo;
        }

        String engineStatus = readFlowEngineBusinessStatus(resolveFlowEngineBusinessKey(link));
        String terminalResult = resolveTerminalBusinessFlowResult(engineStatus);
        if (StringUtils.isNotBlank(terminalResult)) {
            BusinessFlowCallbackDTO callback = new BusinessFlowCallbackDTO();
            callback.setProcessInstanceId(StringUtils.firstNonBlank(processInstanceId, link.getProcessInstanceId()));
            callback.setBusinessKey(link.getBusinessKey());
            callback.setResult(terminalResult);
            callback.setFlowStatus(engineStatus);
            callback.setTenantId(link.getTenantId());
            callback.setOperatorId(resolveUserId());
            callback.setVariables(variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables));
            handleFlowCallbackInternal(link, callback);
            return toRuntimeVO(link, "业务待办已办理，流程已结束");
        }

        if (!"IN_PROCESS".equalsIgnoreCase(link.getFlowStatus())) {
            link.setFlowStatus("IN_PROCESS");
            flowInstanceLinkMapper.updateById(link);
        }
        return toRuntimeVO(link, "业务待办已办理，流程继续流转");
    }

    private String readFlowEngineBusinessStatus(String businessKey) {
        if (flowClient == null || StringUtils.isBlank(businessKey)) {
            return null;
        }
        try {
            FlowResult<Map<String, Object>> status = flowClient.getProcessStatus(businessKey);
            if (status == null || !status.isSuccess() || status.getData() == null) {
                return null;
            }
            return StringUtils.trimToNull(textValue(status.getData().get("status")));
        } catch (Exception e) {
            log.debug("[低代码流程状态] 读取 Flowable 业务状态失败: businessKey={}, error={}",
                    businessKey, e.getMessage());
            return null;
        }
    }

    private String resolveFlowBusinessKeyForStart(Long tenantId, String businessKey) {
        AiBusinessFlowInstanceLink latest = flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, businessKey);
        if (latest == null) {
            return businessKey;
        }
        return businessKey + ":R" + (latest.getId() == null ? System.currentTimeMillis() : latest.getId() + 1);
    }

    private String resolveFlowEngineBusinessKey(AiBusinessFlowInstanceLink link) {
        if (link == null) {
            return null;
        }
        JSONObject variables = readJsonObject(link.getVariablesSnapshot());
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(variables.get("flowBusinessKey"))),
                StringUtils.trimToNull(link.getBusinessKey()));
    }

    private String resolveTerminalBusinessFlowResult(String engineStatus) {
        String normalized = StringUtils.trimToEmpty(engineStatus).toUpperCase();
        if (normalized.contains("APPROVED") || normalized.contains("COMPLETED")) {
            return "APPROVED";
        }
        if (normalized.contains("REJECT")) {
            return "REJECTED";
        }
        if (normalized.contains("CANCEL") || normalized.contains("TERMINAT") || normalized.contains("WITHDRAW")) {
            return "CANCELED";
        }
        return null;
    }

    /**
     * 驳回修改后重提。复杂代码业务可先在业务页保存主数据，再调用该接口完成修改节点。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO resubmit(BusinessFlowResubmitDTO dto) {
        if (dto == null) {
            throw new BusinessException("重提参数不能为空");
        }
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());

        validateTaskAccess(query, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true);
        Map<String, Object> variables = dto.getVariables() == null ? Map.of() : dto.getVariables();
        FlowResult<Void> result = flowClient.approve(
                query.getTaskId(),
                String.valueOf(resolveUserId()),
                StringUtils.defaultIfBlank(dto.getComment(), "修改后重提"),
                variables);
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null ? "重提失败" : StringUtils.defaultIfBlank(result.getMsg(), "重提失败"));
        }

        AiBusinessFlowInstanceLink link = findRuntimeLink(resolveTenantId(), query.getProcessInstanceId(), runtime.businessKey());
        if (link == null) {
            BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
            vo.setObjectCode(runtime.objectCode());
            vo.setRecordId(runtime.recordId());
            vo.setBusinessKey(runtime.businessKey());
            vo.setProcessInstanceId(query.getProcessInstanceId());
            vo.setFlowStatus("IN_PROCESS");
            vo.setMessage("已重提");
            return vo;
        }

        AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByObjectCode(
                link.getTenantId(), link.getObjectCode());
        AiCrudConfig runtimeConfig = documentConfig == null
                ? resolvePublishedRuntimeConfig(link.getTenantId(), link.getObjectCode())
                : null;
        AiBusinessBinding binding = selectMainFlowBindingForConfig(link.getTenantId(), link.getObjectCode());
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, link.getTenantId(), link.getObjectCode());
        updateBusinessFlowStatus(documentConfig, runtimeConfig, bindingConfig, link.getRecordId(), "IN_PROCESS");

        link.setFlowStatus("IN_PROCESS");
        link.setResult(null);
        link.setEndTime(null);
        if (!variables.isEmpty()) {
            link.setVariablesSnapshot(JSON.toJSONString(variables));
        }
        flowInstanceLinkMapper.updateById(link);
        return toRuntimeVO(link, "已重提");
    }

    private void validateTaskAccess(BusinessTaskFormContextQueryDTO query, boolean writeRequired) {
        String taskId = StringUtils.trimToNull(query.getTaskId());
        if (taskId == null) {
            throw new BusinessException("任务ID不能为空");
        }
        query.setTaskId(taskId);
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法校验任务身份");
        }
        Long currentUserId = resolveUserId();
        if (currentUserId == null) {
            throw new BusinessException("当前登录用户不能为空");
        }

        Map<String, Object> task = loadFlowTaskDetail(taskId);
        Integer status = readIntegerValue(task.get("status"));
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("当前任务已处理，不能访问待办业务表单");
        }

        String currentUser = String.valueOf(currentUserId);
        String assignee = StringUtils.trimToNull(textValue(task.get("assignee")));
        boolean claimedByCurrentUser = StringUtils.equals(assignee, currentUser);
        boolean unclaimedCandidate = StringUtils.isBlank(assignee)
                && (csvContains(task.get("candidateUsers"), currentUser)
                || StringUtils.isNotBlank(textValue(task.get("candidateGroups"))));

        if (!claimedByCurrentUser && !unclaimedCandidate) {
            throw new BusinessException("无权访问当前任务业务表单");
        }
        if (writeRequired && !claimedByCurrentUser) {
            throw new BusinessException("请先签收任务后再保存业务字段");
        }

        assertTaskFieldMatches(query.getProcessInstanceId(), task.get("processInstanceId"), "流程实例");
        assertBusinessKeyMatches(query.getBusinessKey(), task.get("businessKey"));
        assertTaskFieldMatches(query.getTaskDefKey(), task.get("taskDefKey"), "任务节点");
        assertProcessDefinitionMatches(query.getProcessDefKey(), task.get("processDefKey"));

        if (StringUtils.isBlank(query.getProcessInstanceId())) {
            query.setProcessInstanceId(StringUtils.trimToNull(textValue(task.get("processInstanceId"))));
        }
        if (StringUtils.isBlank(query.getBusinessKey())) {
            query.setBusinessKey(StringUtils.trimToNull(textValue(task.get("businessKey"))));
        }
        if (StringUtils.isBlank(query.getTaskDefKey())) {
            query.setTaskDefKey(StringUtils.trimToNull(textValue(task.get("taskDefKey"))));
        }
        if (StringUtils.isBlank(query.getProcessDefKey())) {
            query.setProcessDefKey(StringUtils.trimToNull(textValue(task.get("processDefKey"))));
        }
    }

    private Map<String, Object> loadFlowTaskDetail(String taskId) {
        try {
            FlowResult<Map<String, Object>> result = flowClient.getTaskDetail(taskId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                String message = result == null ? null : result.getMsg();
                throw new BusinessException(StringUtils.defaultIfBlank(message, "任务不存在或无权访问"));
            }
            return result.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("业务待办表单任务身份校验失败: taskId={}, error={}", taskId, e.getMessage());
            throw new BusinessException("任务身份校验失败，请稍后重试");
        }
    }

    private void assertTaskFieldMatches(String requestedValue, Object actualValue, String label) {
        String requested = StringUtils.trimToNull(requestedValue);
        String actual = StringUtils.trimToNull(textValue(actualValue));
        if (requested != null && actual != null && !StringUtils.equals(requested, actual)) {
            throw new BusinessException(label + "与当前任务不匹配");
        }
    }

    private void assertBusinessKeyMatches(String requestedValue, Object actualValue) {
        String requested = StringUtils.trimToNull(requestedValue);
        String actual = StringUtils.trimToNull(textValue(actualValue));
        if (requested == null || actual == null || StringUtils.equals(requested, actual)) {
            return;
        }
        if (isSameDocumentBusinessKey(requested, actual)) {
            return;
        }
        throw new BusinessException("业务Key与当前任务不匹配");
    }

    private boolean isSameDocumentBusinessKey(String left, String right) {
        String leftKey = normalizeDocumentBusinessKey(left);
        String rightKey = normalizeDocumentBusinessKey(right);
        return StringUtils.isNotBlank(leftKey) && StringUtils.equals(leftKey, rightKey);
    }

    private String normalizeDocumentBusinessKey(String businessKey) {
        String text = StringUtils.trimToNull(businessKey);
        if (text == null) {
            return null;
        }
        int retryIndex = text.indexOf(":R");
        if (retryIndex <= 0) {
            return text;
        }
        String retryNo = text.substring(retryIndex + 2);
        if (StringUtils.isNumeric(retryNo)) {
            return text.substring(0, retryIndex);
        }
        return text;
    }

    private void assertProcessDefinitionMatches(String requestedValue, Object actualValue) {
        String requested = StringUtils.trimToNull(requestedValue);
        String actual = StringUtils.trimToNull(textValue(actualValue));
        if (requested == null || actual == null || StringUtils.equals(requested, actual)) {
            return;
        }
        String requestedKey = extractProcessDefinitionKey(requested);
        String actualKey = extractProcessDefinitionKey(actual);
        if (StringUtils.isNotBlank(requestedKey) && StringUtils.equals(requestedKey, actualKey)) {
            return;
        }
        if (isUuidLike(requested) || isUuidLike(actual)) {
            return;
        }
        log.debug("忽略流程定义标识表示差异: requested={}, actual={}", requested, actual);
    }

    private String extractProcessDefinitionKey(String value) {
        String text = StringUtils.trimToNull(value);
        if (text == null) {
            return null;
        }
        int separator = text.indexOf(':');
        return separator > 0 ? text.substring(0, separator) : text;
    }

    private boolean isUuidLike(String value) {
        String text = StringUtils.trimToNull(value);
        if (text == null || text.length() != 36) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                if (ch != '-') {
                    return false;
                }
                continue;
            }
            boolean hex = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private boolean csvContains(Object csvValue, String expected) {
        String csv = StringUtils.trimToNull(textValue(csvValue));
        if (csv == null || StringUtils.isBlank(expected)) {
            return false;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            if (StringUtils.equals(StringUtils.trimToEmpty(part), expected)) {
                return true;
            }
        }
        return false;
    }

    private Integer readIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = StringUtils.trimToNull(String.valueOf(value));
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BusinessTaskFormContextVO buildTaskFormContext(BusinessTaskFormContextQueryDTO query,
                                                           TaskFormRuntimeContext runtime) {
        BusinessTaskFormContextVO vo = new BusinessTaskFormContextVO();
        vo.setTaskId(StringUtils.trimToNull(query.getTaskId()));
        vo.setBusinessKey(runtime.businessKey());
        vo.setProcessInstanceId(StringUtils.trimToNull(query.getProcessInstanceId()));
        vo.setProcessDefKey(StringUtils.trimToNull(query.getProcessDefKey()));
        vo.setTaskDefKey(StringUtils.trimToNull(query.getTaskDefKey()));
        vo.setObjectCode(runtime.objectCode());
        vo.setRecordId(runtime.recordId());
        vo.setConfigKey(runtime.configKey());
        vo.setFormType("none");

        if (StringUtils.isBlank(runtime.objectCode()) || runtime.recordId() == null) {
            vo.getWarnings().add("未解析到业务对象或记录");
            return vo;
        }

        JSONObject nodeForm = resolveTaskNodeForm(runtime, query);
        if (nodeForm == null || nodeForm.isEmpty()) {
            vo.getWarnings().add("当前节点未配置业务表单策略");
            return vo;
        }
        String formMode = normalizeNodeFormMode(nodeForm.getString("formMode"));
        if (!"BUSINESS_OBJECT_FORM".equals(formMode)) {
            if ("BUSINESS_CODE_FORM".equals(formMode)) {
                return buildBusinessCodeFormContext(query, nodeForm, runtime);
            }
            vo.setFormType(formMode);
            vo.setFormKey(StringUtils.trimToNull(nodeForm.getString("formKey")));
            vo.setFormName(StringUtils.trimToNull(nodeForm.getString("formName")));
            vo.setProviderKey(StringUtils.trimToNull(nodeForm.getString("providerKey")));
            vo.setFormUrl(StringUtils.trimToNull(nodeForm.getString("formUrl")));
            vo.setEditMode(normalizeNodeEditMode(nodeForm.getString("editMode")));
            vo.setFormRef(readNestedObject(nodeForm.get("formRef")));
            applyApprovalPolicy(vo, nodeForm);
            vo.getWarnings().add("当前节点表单类型暂不由低代码业务表单渲染: " + formMode);
            return vo;
        }
        if (StringUtils.isBlank(runtime.configKey())) {
            vo.getWarnings().add("业务对象缺少已发布运行配置，无法加载低代码业务表单");
            return vo;
        }

        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getFormKey()),
                StringUtils.trimToNull(nodeForm.getString("formKey")));
        BusinessObjectVO object = queryBusinessObject(resolveTenantId(), runtime.objectCode());
        vo.setBusinessObjectName(object == null ? runtime.objectCode() : object.getObjectName());
        JSONObject formSchema = resolveBusinessFormSchema(object, formKey, runtime.configKey());
        if (formSchema.isEmpty()) {
            vo.getWarnings().add("未找到节点引用的低代码表单资产: " + formKey);
            return vo;
        }

        List<Map<String, Object>> fieldCatalog = resolveBusinessTaskCrudPageFields(runtime.configKey(), formKey, formSchema);
        List<Map<String, Object>> permissions = normalizeBusinessObjectTaskPermissions(
                fieldCatalog, normalizeFieldPermissions(nodeForm.get("fieldPermissions")));
        List<Map<String, Object>> fields = buildTaskFormFields(fieldCatalog, permissions);
        Map<String, Object> recordData = dynamicCrudService.selectById(runtime.configKey(), runtime.recordId());
        Map<String, Object> visibleRecordData = filterVisibleRecordData(recordData, fields);
        List<Map<String, Object>> childrenConfig = resolveBusinessTaskChildrenConfig(runtime.configKey());
        logBusinessTaskChildren("raw", runtime.configKey(), runtime.recordId(), childrenConfig, visibleRecordData);
        filterVisibleRecordChildren(visibleRecordData, childrenConfig);
        logBusinessTaskChildren("filtered", runtime.configKey(), runtime.recordId(), childrenConfig, visibleRecordData);
        vo.setBusinessSummary(resolveBusinessSummary(object, runtime, recordData));

        vo.setConfigured(true);
        vo.setFormType("business-object");
        vo.setFormKey(StringUtils.firstNonBlank(formKey, formSchema.getString("formKey")));
        vo.setFormName(StringUtils.defaultIfBlank(nodeForm.getString("formName"), formSchema.getString("formName")));
        vo.setViewKey(StringUtils.defaultIfBlank(nodeForm.getString("viewKey"), "default"));
        vo.setEditMode(resolveBusinessObjectTaskEditMode(nodeForm, permissions));
        applyBusinessObjectFormLayout(vo, formSchema, runtime.configKey());
        vo.setFormRef(readNestedObject(nodeForm.get("formRef")));
        vo.setFieldPermissions(permissions);
        vo.setFields(fields);
        vo.setFormAssets(resolveBusinessTaskFormAssets(formSchema, runtime.configKey(), formKey));
        vo.setChildrenConfig(childrenConfig);
        vo.setRecordData(visibleRecordData);
        applyApprovalPolicy(vo, nodeForm);
        if (fields.isEmpty()) {
            vo.getWarnings().add("当前业务表单没有可展示字段");
        }
        return vo;
    }

    private void applyBusinessObjectFormLayout(BusinessTaskFormContextVO vo, JSONObject formSchema, String configKey) {
        JSONObject settings = readNestedObject(formSchema == null ? null : formSchema.get("settings"));
        JSONObject layout = readNestedObject(settings.get("layout"));
        JSONObject runtimeOptions = readRuntimeConfigOptions(configKey);
        vo.setGridCols(Math.max(1, integerValue(
                firstNonNull(layout.get("gridCols"),
                        layout.get("gridColumns"),
                        settings.get("gridCols"),
                        settings.get("gridColumns"),
                        runtimeOptions.get("editGridCols")),
                1)));
        vo.setLabelPlacement(StringUtils.defaultIfBlank(
                StringUtils.firstNonBlank(
                        textValue(layout.get("labelPlacement")),
                        textValue(settings.get("labelPlacement")),
                        textValue(runtimeOptions.get("editLabelPlacement"))),
                "left"));
        vo.setLabelWidth(StringUtils.defaultIfBlank(
                StringUtils.firstNonBlank(
                        textValue(layout.get("labelWidth")),
                        textValue(settings.get("labelWidth")),
                        textValue(runtimeOptions.get("editLabelWidth"))),
                "100"));
    }

    private List<Map<String, Object>> resolveBusinessTaskCrudPageFields(String configKey,
                                                                        String formKey,
                                                                        JSONObject formSchema) {
        List<Map<String, Object>> fallback = collectBusinessFormFieldCatalog(formSchema);
        if (StringUtils.isBlank(configKey)) {
            return fallback;
        }
        try {
            AiCrudConfig runtimeConfig = dynamicCrudService.getRuntimeConfig(configKey);
            if (runtimeConfig == null) {
                return fallback;
            }
            JSONObject options = readJsonObject(runtimeConfig.getOptions());
            if (!shouldUseCrudPageDefaultFormSchema(formKey, formSchema, options)) {
                return fallback;
            }
            List<Map<String, Object>> runtimeFields = readMapList(readNestedArray(runtimeConfig.getEditSchema()));
            if (runtimeFields.isEmpty()) {
                return fallback;
            }
            List<Map<String, Object>> layoutFields = applyRuntimeCrudFormLayout(
                    runtimeFields, readNestedArray(options.get("editFormLayout")));
            return layoutFields.isEmpty() ? runtimeFields : layoutFields;
        } catch (Exception e) {
            log.debug("读取动态 CRUD 详情表单 schema 失败: configKey={}, error={}", configKey, e.getMessage());
            return fallback;
        }
    }

    private boolean shouldUseCrudPageDefaultFormSchema(String formKey, JSONObject formSchema, JSONObject options) {
        String requestedKey = StringUtils.trimToNull(formKey);
        if (requestedKey == null) {
            return true;
        }
        String currentFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formSchema == null ? null : formSchema.getString("formKey")),
                StringUtils.trimToNull(formSchema == null ? null : formSchema.getString("defaultFormKey")));
        JSONObject designerSchema = readNestedObject(options == null ? null : options.get("formDesignerSchema"));
        String defaultFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(designerSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(designerSchema.getString("formKey")),
                currentFormKey);
        return StringUtils.isBlank(defaultFormKey) || StringUtils.equals(requestedKey, defaultFormKey);
    }

    private List<Map<String, Object>> applyRuntimeCrudFormLayout(List<Map<String, Object>> fields, JSONArray layout) {
        if (fields == null || fields.isEmpty() || layout == null || layout.isEmpty()) {
            return fields == null ? List.of() : fields;
        }
        Map<String, Map<String, Object>> fieldMap = new LinkedHashMap<>();
        for (Map<String, Object> field : fields) {
            String fieldCode = StringUtils.trimToNull(textValue(field.get("field")));
            if (fieldCode != null) {
                fieldMap.put(fieldCode, field);
            }
        }
        Set<String> usedFields = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> layoutNodes = readMapList(layout);
        for (Map<String, Object> node : layoutNodes) {
            Map<String, Object> hydrated = hydrateRuntimeCrudLayoutNode(node, fieldMap, usedFields);
            if (hydrated != null) {
                result.add(hydrated);
            }
        }
        for (Map<String, Object> field : fields) {
            String fieldCode = StringUtils.trimToNull(textValue(field.get("field")));
            if (fieldCode != null && !usedFields.contains(fieldCode)) {
                result.add(field);
            }
        }
        return result;
    }

    private Map<String, Object> hydrateRuntimeCrudLayoutNode(Map<String, Object> node,
                                                             Map<String, Map<String, Object>> fieldMap,
                                                             Set<String> usedFields) {
        if (node == null || node.isEmpty()) {
            return null;
        }
        String fieldCode = StringUtils.trimToNull(textValue(node.get("field")));
        String nodeType = resolveRuntimeCrudLayoutNodeType(node);
        if (fieldCode != null && ("field".equals(nodeType) || fieldMap.containsKey(fieldCode))) {
            Map<String, Object> field = fieldMap.get(fieldCode);
            if (field == null) {
                return null;
            }
            usedFields.add(fieldCode);
            Map<String, Object> item = new LinkedHashMap<>(field);
            item.put("nodeType", "field");
            item.put("key", StringUtils.defaultIfBlank(textValue(node.get("key")), fieldCode));
            if (node.get("span") != null) {
                item.put("span", node.get("span"));
            }
            if (node.get("gridStyle") != null) {
                item.put("gridStyle", node.get("gridStyle"));
            }
            return item;
        }

        List<Map<String, Object>> children = new ArrayList<>();
        for (Map<String, Object> child : readMapList(readNestedArray(node.get("children")))) {
            Map<String, Object> hydrated = hydrateRuntimeCrudLayoutNode(child, fieldMap, usedFields);
            if (hydrated != null) {
                children.add(hydrated);
            }
        }
        if (children.isEmpty() && !isStandaloneRuntimeCrudLayoutNode(node)) {
            return null;
        }
        Map<String, Object> item = new LinkedHashMap<>(node);
        item.put("nodeType", nodeType);
        item.put("children", children);
        return item;
    }

    private String resolveRuntimeCrudLayoutNodeType(Map<String, Object> node) {
        String key = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(node.get("componentKey"))),
                StringUtils.trimToNull(textValue(node.get("type"))),
                StringUtils.trimToNull(textValue(node.get("nodeType"))));
        if (Set.of("title", "fcTitle", "sectionTitle", "groupTitle", "groupHeader",
                "GroupHeader", "titleBlock", "section").contains(key)) {
            return "groupTitle";
        }
        if (Set.of("divider", "elDivider", "AiFormSectionTitle", "aiFormSectionTitle",
                "formSectionTitle", "FormSectionTitle").contains(key)) {
            return "divider";
        }
        return StringUtils.defaultIfBlank(key, "layout");
    }

    private boolean isStandaloneRuntimeCrudLayoutNode(Map<String, Object> node) {
        String key = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(node.get("componentKey"))),
                StringUtils.trimToNull(textValue(node.get("type"))),
                StringUtils.trimToNull(textValue(node.get("nodeType"))));
        return Set.of("title", "fcTitle", "sectionTitle", "groupTitle", "groupHeader", "GroupHeader",
                "titleBlock", "section", "divider", "elDivider", "AiFormSectionTitle", "aiFormSectionTitle",
                "formSectionTitle", "FormSectionTitle", "button", "table", "tableGrid", "AiCrudPage",
                "aiCrudPage", "crud", "crudBlock").contains(key);
    }

    private List<Map<String, Object>> resolveBusinessTaskFormAssets(JSONObject formSchema,
                                                                    String configKey,
                                                                    String activeFormKey) {
        if (StringUtils.isBlank(configKey)) {
            return List.of();
        }
        try {
            AiCrudConfig runtimeConfig = dynamicCrudService.getRuntimeConfig(configKey);
            JSONObject options = runtimeConfig == null ? new JSONObject() : readJsonObject(runtimeConfig.getOptions());
            List<Map<String, Object>> configuredAssets = readMapList(readNestedArray(options.get("formAssets")));
            if (!configuredAssets.isEmpty()) {
                return configuredAssets;
            }
            JSONObject designerSchema = readNestedObject(options.get("formDesignerSchema"));
            JSONArray forms = readNestedArray(designerSchema.get("forms"));
            if (forms.isEmpty()) {
                return List.of();
            }
            String currentKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(activeFormKey),
                    StringUtils.trimToNull(formSchema == null ? null : formSchema.getString("formKey")),
                    StringUtils.trimToNull(designerSchema.getString("defaultFormKey")));
            List<Map<String, Object>> assets = new ArrayList<>();
            for (int i = 0; i < forms.size(); i++) {
                JSONObject form = forms.getJSONObject(i);
                if (form == null) {
                    continue;
                }
                String itemKey = StringUtils.trimToNull(form.getString("formKey"));
                if (itemKey == null || StringUtils.equals(itemKey, currentKey)) {
                    continue;
                }
                Map<String, Object> asset = new LinkedHashMap<>();
                asset.put("formKey", itemKey);
                asset.put("formName", StringUtils.defaultIfBlank(form.getString("formName"), itemKey));
                asset.put("usage", readNestedArray(form.get("usage")));
                asset.put("schema", readNestedObject(form.get("schema")));
                assets.add(asset);
            }
            return assets;
        } catch (Exception e) {
            log.debug("读取业务表单资产失败: configKey={}, error={}", configKey, e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> resolveBusinessTaskChildrenConfig(String configKey) {
        if (StringUtils.isBlank(configKey)) {
            return List.of();
        }
        try {
            AiCrudConfig runtimeConfig = dynamicCrudService.getRuntimeConfig(configKey);
            JSONObject options = runtimeConfig == null ? new JSONObject() : readJsonObject(runtimeConfig.getOptions());
            JSONObject masterDetailConfig = readNestedObject(options.get("masterDetailConfig"));
            return readMapList(readNestedArray(masterDetailConfig.get("children"))).stream()
                    .filter(this::isBusinessTaskDetailChild)
                    .toList();
        } catch (Exception e) {
            log.debug("读取业务表单子表配置失败: configKey={}, error={}", configKey, e.getMessage());
            return List.of();
        }
    }

    private boolean isBusinessTaskDetailChild(Map<String, Object> child) {
        if (child == null || child.isEmpty()) {
            return false;
        }
        if (Boolean.FALSE.equals(child.get("showInDetail"))) {
            return false;
        }
        if (readMapList(readNestedArray(child.get("fields"))).isEmpty()) {
            return false;
        }
        String relationType = StringUtils.defaultIfBlank(textValue(child.get("relationType")), "ONE_TO_MANY")
                .trim()
                .toUpperCase(Locale.ROOT);
        return !Set.of("REFERENCE", "LOOKUP", "OBJECT_REFERENCE", "OBJECTREFERENCE", "MANY_TO_ONE", "ONE_TO_ONE")
                .contains(relationType);
    }

    @SuppressWarnings("unchecked")
    private void filterVisibleRecordChildren(Map<String, Object> recordData, List<Map<String, Object>> childrenConfig) {
        if (recordData == null || !recordData.containsKey("children")) {
            return;
        }
        if (childrenConfig == null || childrenConfig.isEmpty()) {
            recordData.remove("children");
            return;
        }
        Object childrenValue = recordData.get("children");
        if (!(childrenValue instanceof Map<?, ?> children)) {
            return;
        }
        Set<String> allowedKeys = childrenConfig.stream()
                .map(this::resolveBusinessTaskChildKey)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String key : allowedKeys) {
            Object value = children.get(key);
            if (value instanceof List<?>) {
                filtered.put(key, value);
            }
        }
        recordData.put("children", filtered);
    }

    private void logBusinessTaskChildren(String stage,
                                         String configKey,
                                         Object recordId,
                                         List<Map<String, Object>> childrenConfig,
                                         Map<String, Object> recordData) {
        log.info("[审批表单子表] stage={}, configKey={}, recordId={}, childrenConfig={}, children={}",
                stage, configKey, recordId, summarizeBusinessTaskChildrenConfig(childrenConfig),
                summarizeBusinessTaskChildrenData(recordData == null ? null : recordData.get("children")));
    }

    private List<Map<String, Object>> summarizeBusinessTaskChildrenConfig(List<Map<String, Object>> childrenConfig) {
        if (childrenConfig == null || childrenConfig.isEmpty()) {
            return List.of();
        }
        return childrenConfig.stream()
                .map(child -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("key", resolveBusinessTaskChildKey(child));
                    item.put("modelCode", textValue(child.get("modelCode")));
                    item.put("tableName", textValue(child.get("tableName")));
                    item.put("relationType", textValue(child.get("relationType")));
                    item.put("sourceField", textValue(child.get("sourceField")));
                    item.put("targetField", textValue(child.get("targetField")));
                    item.put("fieldCount", readMapList(readNestedArray(child.get("fields"))).size());
                    return item;
                })
                .toList();
    }

    private Map<String, Object> summarizeBusinessTaskChildrenData(Object childrenValue) {
        if (!(childrenValue instanceof Map<?, ?> children) || children.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : children.entrySet()) {
            Object value = entry.getValue();
            Map<String, Object> item = new LinkedHashMap<>();
            if (value instanceof List<?> list) {
                item.put("rows", list.size());
                item.put("rowIds", list.stream()
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .limit(5)
                        .map(row -> ((Map<?, ?>) row).get("id"))
                        .toList());
                item.put("firstFields", list.stream()
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .findFirst()
                        .map(row -> ((Map<?, ?>) row).keySet().stream().limit(12).toList())
                        .orElse(List.of()));
            } else {
                item.put("type", value == null ? "null" : value.getClass().getSimpleName());
            }
            result.put(String.valueOf(entry.getKey()), item);
        }
        return result;
    }

    private String resolveBusinessTaskChildKey(Map<String, Object> child) {
        if (child == null) {
            return null;
        }
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(child.get("key"))),
                StringUtils.trimToNull(textValue(child.get("modelCode"))),
                StringUtils.trimToNull(textValue(child.get("tableName"))),
                "children");
    }

    private JSONObject readRuntimeConfigOptions(String configKey) {
        if (StringUtils.isBlank(configKey)) {
            return new JSONObject();
        }
        try {
            AiCrudConfig runtimeConfig = dynamicCrudService.getRuntimeConfig(configKey);
            return runtimeConfig == null ? new JSONObject() : readJsonObject(runtimeConfig.getOptions());
        } catch (Exception e) {
            log.debug("读取业务表单运行态布局失败: configKey={}, error={}", configKey, e.getMessage());
            return new JSONObject();
        }
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Integer integerValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = StringUtils.trimToNull(String.valueOf(value));
        if (text == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void makeBusinessTaskFormReadonly(BusinessTaskFormContextVO context) {
        if (context == null) {
            return;
        }
        if (context.getFields() != null) {
            for (Map<String, Object> field : context.getFields()) {
                if (field == null) {
                    continue;
                }
                field.put("writable", false);
                field.put("readonly", true);
                field.put("disabled", true);
            }
        }
        if (context.getFieldPermissions() != null) {
            for (Map<String, Object> permission : context.getFieldPermissions()) {
                if (permission == null) {
                    continue;
                }
                permission.put("writable", false);
                permission.put("readonly", true);
                permission.put("disabled", true);
            }
        }
    }

    private BusinessTaskFormContextVO buildBusinessCodeFormContext(BusinessTaskFormContextQueryDTO query,
                                                                   JSONObject nodeForm,
                                                                   TaskFormRuntimeContext runtime) {
        JSONObject formRef = readNestedObject(nodeForm.get("formRef"));
        String providerKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("providerKey")),
                StringUtils.trimToNull(formRef.getString("providerKey")));
        List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        BusinessTaskFormContextQueryDTO effectiveQuery = enrichTaskFormQuery(query, runtime, nodeForm, formRef);
        BusinessTaskFormContextVO fallback = buildBusinessCodeFormFallback(effectiveQuery, nodeForm, runtime, formRef, permissions, providerKey);
        if (StringUtils.isBlank(providerKey)) {
            fallback.getWarnings().add("当前代码表单缺少 providerKey，无法加载业务表单");
            return fallback;
        }
        return codeFormProviderRegistry.find(providerKey)
                .map(provider -> applyBusinessCodeFieldPermissions(
                        applyBusinessCodeMetadataFields(
                                mergeBusinessCodeFormBase(provider.buildContext(effectiveQuery, new LinkedHashMap<>(formRef), permissions),
                                        fallback),
                                runtime.objectCode()),
                        permissions))
                .orElseGet(() -> {
                    fallback.getWarnings().add("代码表单Provider未注册: " + providerKey);
                    return fallback;
                });
    }

    private BusinessTaskFormContextVO saveBusinessCodeFormContext(BusinessTaskFormSaveDTO dto, JSONObject nodeForm) {
        JSONObject formRef = readNestedObject(nodeForm.get("formRef"));
        String providerKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("providerKey")),
                StringUtils.trimToNull(formRef.getString("providerKey")));
        if (StringUtils.isBlank(providerKey)) {
            throw new BusinessException("当前代码表单缺少 providerKey，无法保存业务字段");
        }
        List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        BusinessTaskFormContextVO context = codeFormProviderRegistry.require(providerKey)
                .saveContext(dto, new LinkedHashMap<>(formRef), permissions);
        return applyBusinessCodeFieldPermissions(applyBusinessCodeMetadataFields(context, dto.getObjectCode()), permissions);
    }

    private BusinessTaskFormContextVO buildBusinessCodeFormFallback(BusinessTaskFormContextQueryDTO query,
                                                                   JSONObject nodeForm,
                                                                   TaskFormRuntimeContext runtime,
                                                                   JSONObject formRef,
                                                                   List<Map<String, Object>> permissions,
                                                                   String providerKey) {
        BusinessTaskFormContextVO vo = new BusinessTaskFormContextVO();
        vo.setConfigured(true);
        vo.setFormType("business-code");
        vo.setTaskId(StringUtils.trimToNull(query.getTaskId()));
        vo.setBusinessKey(runtime.businessKey());
        vo.setProcessInstanceId(StringUtils.trimToNull(query.getProcessInstanceId()));
        vo.setProcessDefKey(StringUtils.trimToNull(query.getProcessDefKey()));
        vo.setTaskDefKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getTaskDefKey()),
                StringUtils.trimToNull(nodeForm.getString("taskDefKey"))));
        vo.setObjectCode(runtime.objectCode());
        vo.setBusinessObjectName(StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("objectName")),
                StringUtils.trimToNull(formRef.getString("objectName")),
                StringUtils.trimToNull(formRef.getString("businessName")),
                runtime.objectCode()));
        vo.setRecordId(runtime.recordId());
        vo.setConfigKey(runtime.configKey());
        vo.setFormKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("formKey")),
                StringUtils.trimToNull(formRef.getString("formKey"))));
        vo.setFormName(StringUtils.trimToNull(nodeForm.getString("formName")));
        vo.setProviderKey(providerKey);
        vo.setFormUrl(StringUtils.firstNonBlank(
                StringUtils.trimToNull(nodeForm.getString("formUrl")),
                StringUtils.trimToNull(formRef.getString("formUrl"))));
        vo.setViewKey(StringUtils.defaultIfBlank(nodeForm.getString("viewKey"), "default"));
        vo.setEditMode(normalizeNodeEditMode(nodeForm.getString("editMode")));
        vo.setFormRef(new LinkedHashMap<>(formRef));
        vo.setFieldPermissions(permissions);
        applyApprovalPolicy(vo, nodeForm);
        return vo;
    }

    private void applyApprovalPolicy(BusinessTaskFormContextVO vo, JSONObject source) {
        if (vo == null || source == null) {
            return;
        }
        vo.setAllowApprove(readNullableBooleanValue(source.get("allowApprove")));
        vo.setAllowDelegate(readNullableBooleanValue(source.get("allowDelegate")));
        vo.setAllowReject(readNullableBooleanValue(source.get("allowReject")));
        vo.setAllowRejectToStart(readNullableBooleanValue(source.get("allowRejectToStart")));
        vo.setAllowReturn(readNullableBooleanValue(source.get("allowReturn")));
        vo.setAllowTerminate(readNullableBooleanValue(source.get("allowTerminate")));
        vo.setRequireSignature(readNullableBooleanValue(source.get("requireSignature")));
        vo.setRequireComment(readNullableBooleanValue(source.get("requireComment")));
    }

    private BusinessTaskFormContextQueryDTO enrichTaskFormQuery(BusinessTaskFormContextQueryDTO query,
                                                               TaskFormRuntimeContext runtime,
                                                               JSONObject nodeForm,
                                                               JSONObject formRef) {
        BusinessTaskFormContextQueryDTO source = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        BusinessTaskFormContextQueryDTO result = new BusinessTaskFormContextQueryDTO();
        result.setTaskId(StringUtils.trimToNull(source.getTaskId()));
        result.setBusinessKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getBusinessKey()),
                runtime == null ? null : StringUtils.trimToNull(runtime.businessKey())));
        result.setProcessInstanceId(StringUtils.trimToNull(source.getProcessInstanceId()));
        result.setProcessDefKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getProcessDefKey()),
                nodeForm == null ? null : StringUtils.trimToNull(nodeForm.getString("processDefKey"))));
        result.setTaskDefKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getTaskDefKey()),
                nodeForm == null ? null : StringUtils.trimToNull(nodeForm.getString("taskDefKey"))));
        result.setObjectCode(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getObjectCode()),
                runtime == null ? null : StringUtils.trimToNull(runtime.objectCode())));
        result.setRecordId(source.getRecordId() != null ? source.getRecordId() : runtime == null ? null : runtime.recordId());
        result.setFormKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(source.getFormKey()),
                nodeForm == null ? null : StringUtils.trimToNull(nodeForm.getString("formKey")),
                formRef == null ? null : StringUtils.trimToNull(formRef.getString("formKey"))));
        return result;
    }

    private BusinessTaskFormContextVO mergeBusinessCodeFormBase(BusinessTaskFormContextVO source,
                                                               BusinessTaskFormContextVO fallback) {
        if (source == null) {
            return fallback;
        }
        if (source.getConfigured() == null) {
            source.setConfigured(true);
        }
        if (StringUtils.isBlank(source.getFormType())) {
            source.setFormType("business-code");
        }
        if (StringUtils.isBlank(source.getTaskId())) {
            source.setTaskId(fallback.getTaskId());
        }
        if (StringUtils.isBlank(source.getBusinessKey())) {
            source.setBusinessKey(fallback.getBusinessKey());
        }
        if (StringUtils.isBlank(source.getProcessInstanceId())) {
            source.setProcessInstanceId(fallback.getProcessInstanceId());
        }
        if (StringUtils.isBlank(source.getProcessDefKey())) {
            source.setProcessDefKey(fallback.getProcessDefKey());
        }
        if (StringUtils.isBlank(source.getTaskDefKey())) {
            source.setTaskDefKey(fallback.getTaskDefKey());
        }
        if (StringUtils.isBlank(source.getObjectCode())) {
            source.setObjectCode(fallback.getObjectCode());
        }
        if (StringUtils.isBlank(source.getBusinessObjectName())) {
            source.setBusinessObjectName(fallback.getBusinessObjectName());
        }
        if (StringUtils.isBlank(source.getBusinessSummary())) {
            source.setBusinessSummary(fallback.getBusinessSummary());
        }
        if (source.getRecordId() == null) {
            source.setRecordId(fallback.getRecordId());
        }
        if (StringUtils.isBlank(source.getConfigKey())) {
            source.setConfigKey(fallback.getConfigKey());
        }
        if (StringUtils.isBlank(source.getFormKey())) {
            source.setFormKey(fallback.getFormKey());
        }
        if (StringUtils.isBlank(source.getFormName())) {
            source.setFormName(fallback.getFormName());
        }
        if (StringUtils.isBlank(source.getProviderKey())) {
            source.setProviderKey(fallback.getProviderKey());
        }
        if (StringUtils.isBlank(source.getFormUrl())) {
            source.setFormUrl(fallback.getFormUrl());
        }
        if (StringUtils.isBlank(source.getViewKey())) {
            source.setViewKey(fallback.getViewKey());
        }
        if (StringUtils.isBlank(source.getEditMode())) {
            source.setEditMode(fallback.getEditMode());
        }
        if (source.getFormRef() == null || source.getFormRef().isEmpty()) {
            source.setFormRef(fallback.getFormRef());
        }
        if (source.getFieldPermissions() == null || source.getFieldPermissions().isEmpty()) {
            source.setFieldPermissions(fallback.getFieldPermissions());
        }
        return source;
    }

    private BusinessTaskFormContextVO applyBusinessCodeFieldPermissions(BusinessTaskFormContextVO context,
                                                                        List<Map<String, Object>> permissions) {
        if (context == null) {
            return context;
        }
        Map<String, Map<String, Object>> permissionMap = new LinkedHashMap<>();
        List<Map<String, Object>> safePermissions = permissions == null ? List.of() : permissions;
        for (Map<String, Object> permission : safePermissions) {
            String field = StringUtils.trimToNull(textValue(permission.get("field")));
            if (field != null) {
                putPermissionAliases(permissionMap, field, permission);
            }
        }
        List<Map<String, Object>> filteredFields = new ArrayList<>();
        List<Map<String, Object>> sourceFields = context.getFields() == null ? List.of() : context.getFields();
        for (Map<String, Object> source : sourceFields) {
            if (source == null) {
                continue;
            }
            String field = StringUtils.trimToNull(textValue(source.get("field")));
            if (field == null) {
                continue;
            }
            if (readBooleanValue(source.get("internal"), false) || readBooleanValue(source.get("systemField"), false)) {
                continue;
            }
            Map<String, Object> permission = permissionMap.get(field);
            boolean readable = permission != null
                    ? readBooleanValue(permission.get("readable"), true)
                    : true;
            if (!readable) {
                continue;
            }
            boolean writable = permission != null && readBooleanValue(permission.get("writable"), false);
            boolean required = writable && permission != null && readBooleanValue(permission.get("required"), false);
            Map<String, Object> item = new LinkedHashMap<>(source);
            item.put("readable", true);
            item.put("writable", writable);
            item.put("required", required);
            item.put("readonly", !writable);
            item.put("disabled", !writable);
            Map<String, Object> props = new LinkedHashMap<>(readNestedObject(item.get("props")));
            props.put("disabled", !writable);
            item.put("props", props);
            filteredFields.add(item);
        }
        context.setFields(filteredFields);
        context.setRecordData(filterVisibleRecordData(context.getRecordData(), filteredFields));
        return context;
    }

    private BusinessTaskFormSaveDTO filterSaveDataByPermissions(BusinessTaskFormSaveDTO dto,
                                                                List<Map<String, Object>> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            throw new BusinessException("当前节点没有可编辑业务字段");
        }
        Set<String> writableFields = collectPermissionFields(permissions, "writable", true);
        if (writableFields.isEmpty()) {
            throw new BusinessException("当前节点没有可编辑业务字段");
        }
        Map<String, Object> input = dto.getData() == null ? Map.of() : dto.getData();
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (String field : writableFields) {
            if (input.containsKey(field)) {
                filteredData.put(field, input.get(field));
            }
        }
        BusinessTaskFormSaveDTO filtered = new BusinessTaskFormSaveDTO();
        filtered.setTaskId(dto.getTaskId());
        filtered.setBusinessKey(dto.getBusinessKey());
        filtered.setProcessInstanceId(dto.getProcessInstanceId());
        filtered.setProcessDefKey(dto.getProcessDefKey());
        filtered.setTaskDefKey(dto.getTaskDefKey());
        filtered.setObjectCode(dto.getObjectCode());
        filtered.setRecordId(dto.getRecordId());
        filtered.setFormKey(dto.getFormKey());
        filtered.setData(filteredData);
        return filtered;
    }

    private TaskFormRuntimeContext resolveTaskFormRuntimeContext(BusinessTaskFormContextQueryDTO query, boolean strict) {
        Long tenantId = resolveTenantId();
        AiBusinessFlowInstanceLink link = null;
        if (StringUtils.isNotBlank(query.getProcessInstanceId())) {
            link = flowInstanceLinkMapper.selectByProcessInstanceId(tenantId, query.getProcessInstanceId());
        }
        if (link == null && StringUtils.isNotBlank(query.getBusinessKey())) {
            link = flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, query.getBusinessKey());
        }

        String objectCode = StringUtils.firstNonBlank(
                link == null ? null : link.getObjectCode(),
                StringUtils.trimToNull(query.getObjectCode()),
                parseBusinessKeyObjectCode(query.getBusinessKey()));
        Long recordId = link == null || link.getRecordId() == null
                ? query.getRecordId()
                : link.getRecordId();
        if (recordId == null) {
            recordId = parseBusinessKeyRecordId(query.getBusinessKey());
        }
        String businessKey = StringUtils.firstNonBlank(
                link == null ? null : link.getBusinessKey(),
                StringUtils.trimToNull(query.getBusinessKey()),
                objectCode != null && recordId != null ? buildBusinessKey(objectCode, recordId) : null);

        if (StringUtils.isBlank(objectCode) || recordId == null) {
            if (strict) {
                throw new BusinessException("未解析到业务对象或记录ID");
            }
            return new TaskFormRuntimeContext(null, null, businessKey, null, null);
        }

        BusinessRuntimeContext businessContext = resolveBusinessRuntimeContext(tenantId, objectCode);
        String canonicalObjectCode = StringUtils.firstNonBlank(businessContext.objectCode(), objectCode);
        String configKey = businessContext.configKey();
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, canonicalObjectCode, objectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, tenantId, canonicalObjectCode);

        if (StringUtils.isBlank(configKey) && strict && !isBusinessCodeTaskForm(canonicalObjectCode, bindingConfig, query)) {
            throw new BusinessException("业务对象缺少已发布运行配置，无法保存待办业务字段");
        }
        return new TaskFormRuntimeContext(canonicalObjectCode, recordId, businessKey, configKey, bindingConfig);
    }

    private boolean isBusinessCodeTaskForm(String objectCode, JSONObject bindingConfig, BusinessTaskFormContextQueryDTO query) {
        JSONObject nodeForm = resolveTaskNodeForm(
                new TaskFormRuntimeContext(objectCode, null, null, null, bindingConfig), query);
        return nodeForm != null && "BUSINESS_CODE_FORM".equals(normalizeNodeFormMode(nodeForm.getString("formMode")));
    }

    private BusinessObjectVO queryBusinessObject(Long tenantId, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
        query.setObjectCode(objectCode);
        List<BusinessObjectVO> objects = businessObjectMapper.selectObjectList(tenantId, query);
        return objects == null || objects.isEmpty() ? null : objects.get(0);
    }

    private Map<String, AiBusinessFlowInstanceLink> loadLinksByBusinessKey(Long tenantId,
                                                                           List<FlowBusinessListDisplayItem> items) {
        Set<String> businessKeys = new LinkedHashSet<>();
        for (FlowBusinessListDisplayItem item : items) {
            String businessKey = item == null ? null : StringUtils.trimToNull(item.getBusinessKey());
            if (businessKey != null) {
                businessKeys.add(businessKey);
            }
        }
        if (businessKeys.isEmpty()) {
            return Map.of();
        }
        List<AiBusinessFlowInstanceLink> links = flowInstanceLinkMapper.selectLatestByBusinessKeys(tenantId, businessKeys);
        Map<String, AiBusinessFlowInstanceLink> result = new LinkedHashMap<>();
        if (links != null) {
            for (AiBusinessFlowInstanceLink link : links) {
                if (link != null && StringUtils.isNotBlank(link.getBusinessKey())) {
                    result.put(link.getBusinessKey(), link);
                }
            }
        }
        return result;
    }

    private void enrichBusinessListGroup(Long tenantId, BusinessRuntimeContext context, List<BusinessListRuntime> runtimes) {
        String objectCode = context == null ? null : context.objectCode();
        if (StringUtils.isBlank(objectCode) || runtimes == null || runtimes.isEmpty()) {
            return;
        }
        BusinessObjectVO object = queryBusinessObject(tenantId, objectCode);
        AiCrudConfig runtimeConfig = context.runtimeConfig();
        AiBusinessDocumentConfig documentConfig = context.documentConfig();
        String configKey = context.configKey();
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, objectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        String objectName = StringUtils.firstNonBlank(
                object == null ? null : object.getObjectName(),
                documentConfig == null ? null : documentConfig.getDocumentName(),
                runtimeConfig == null ? null : runtimeConfig.getObjectName(),
                objectCode);

        if (StringUtils.isNotBlank(configKey)) {
            enrichLowcodeBusinessListGroup(objectCode, objectName, configKey, object, bindingConfig, runtimes);
            return;
        }
        enrichCodeBusinessListGroup(objectCode, objectName, runtimes);
    }

    private void enrichLowcodeBusinessListGroup(String objectCode,
                                                String objectName,
                                                String configKey,
                                                BusinessObjectVO object,
                                                JSONObject bindingConfig,
                                                List<BusinessListRuntime> runtimes) {
        List<Long> recordIds = runtimes.stream()
                .map(BusinessListRuntime::recordId)
                .distinct()
                .toList();
        Map<Object, Map<String, Object>> records = dynamicCrudService.selectByIds(configKey, recordIds);
        for (BusinessListRuntime runtime : runtimes) {
            FlowBusinessListDisplayItem item = runtime.item();
            Map<String, Object> recordData = findBatchRecord(records, runtime.recordId());
            TaskFormRuntimeContext taskRuntime = new TaskFormRuntimeContext(
                    objectCode, runtime.recordId(), runtime.businessKey(), configKey, bindingConfig);
            item.setObjectCode(objectCode);
            item.setRecordId(runtime.recordId());
            item.setBusinessObjectName(objectName);
            item.setBusinessSummary(StringUtils.firstNonBlank(
                    resolveBusinessSummary(object, taskRuntime, recordData),
                    item.getBusinessSummary()));
            item.setProcessDefinitionName(StringUtils.firstNonBlank(
                    item.getProcessDefinitionName(),
                    item.getProcessName(),
                    bindingConfig.getString("flowModelName"),
                    item.getProcessDefKey()));
        }
    }

    private Map<String, Object> findBatchRecord(Map<Object, Map<String, Object>> records, Long recordId) {
        if (records == null || records.isEmpty() || recordId == null) {
            return Map.of();
        }
        Map<String, Object> record = records.get(recordId);
        if (record != null) {
            return record;
        }
        record = records.get(String.valueOf(recordId));
        return record == null ? Map.of() : record;
    }

    private void enrichCodeBusinessListGroup(String objectCode,
                                             String fallbackObjectName,
                                             List<BusinessListRuntime> runtimes) {
        List<Map<String, Object>> assets = codeFormProviderRegistry.listAssets(objectCode, true);
        if (assets.isEmpty()) {
            applyBusinessListFallback(objectCode, fallbackObjectName, runtimes);
            return;
        }
        Map<String, Object> asset = assets.get(0);
        String providerKey = StringUtils.trimToNull(textValue(asset.get("providerKey")));
        String objectName = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(asset.get("objectName"))),
                StringUtils.trimToNull(textValue(asset.get("businessName"))),
                StringUtils.trimToNull(textValue(asset.get("appName"))),
                fallbackObjectName,
                objectCode);
        Map<Long, String> summaries = providerKey == null
                ? Map.of()
                : codeFormProviderRegistry.find(providerKey)
                        .map(provider -> provider.buildSummaries(objectCode, collectRecordIds(runtimes)))
                        .orElse(Map.of());
        if (summaries == null) {
            summaries = Map.of();
        }
        for (BusinessListRuntime runtime : runtimes) {
            FlowBusinessListDisplayItem item = runtime.item();
            item.setObjectCode(objectCode);
            item.setRecordId(runtime.recordId());
            item.setBusinessObjectName(objectName);
            item.setBusinessSummary(StringUtils.firstNonBlank(summaries.get(runtime.recordId()), item.getBusinessSummary()));
            item.setProcessDefinitionName(StringUtils.firstNonBlank(
                    item.getProcessDefinitionName(), item.getProcessName(), item.getProcessDefKey()));
        }
    }

    private Collection<Long> collectRecordIds(List<BusinessListRuntime> runtimes) {
        List<Long> ids = new ArrayList<>();
        for (BusinessListRuntime runtime : runtimes) {
            if (runtime.recordId() != null && !ids.contains(runtime.recordId())) {
                ids.add(runtime.recordId());
            }
        }
        return ids;
    }

    private void applyBusinessListFallback(String objectCode,
                                           String objectName,
                                           List<BusinessListRuntime> runtimes) {
        for (BusinessListRuntime runtime : runtimes) {
            FlowBusinessListDisplayItem item = runtime.item();
            item.setObjectCode(objectCode);
            item.setRecordId(runtime.recordId());
            item.setBusinessObjectName(StringUtils.firstNonBlank(objectName, item.getBusinessObjectName(), objectCode));
            item.setProcessDefinitionName(StringUtils.firstNonBlank(
                    item.getProcessDefinitionName(), item.getProcessName(), item.getProcessDefKey()));
        }
    }

    private JSONObject findNodeForm(JSONObject bindingConfig, String taskDefKey) {
        JSONArray nodeForms = bindingConfig == null ? null : bindingConfig.getJSONArray("nodeForms");
        if (nodeForms == null || nodeForms.isEmpty() || StringUtils.isBlank(taskDefKey)) {
            return new JSONObject();
        }
        for (int i = 0; i < nodeForms.size(); i++) {
            JSONObject nodeForm = nodeForms.getJSONObject(i);
            if (nodeForm != null && taskDefKey.equals(nodeForm.getString("taskDefKey"))) {
                return nodeForm;
            }
        }
        return new JSONObject();
    }

    private JSONObject resolveTaskNodeForm(TaskFormRuntimeContext runtime, BusinessTaskFormContextQueryDTO query) {
        JSONObject flowNodeForm = resolveFlowNodeForm(runtime, query);
        if (!flowNodeForm.isEmpty()) {
            return flowNodeForm;
        }
        return findNodeForm(runtime.bindingConfig(), query.getTaskDefKey());
    }

    private JSONObject resolveFlowNodeForm(TaskFormRuntimeContext runtime, BusinessTaskFormContextQueryDTO query) {
        String objectCode = StringUtils.trimToNull(runtime.objectCode());
        if (StringUtils.isBlank(objectCode)) {
            return new JSONObject();
        }
        Map<String, Object> formInfo = loadFlowNodeFormInfo(runtime, query);
        String taskDefKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("taskDefKey"))),
                StringUtils.trimToNull(query.getTaskDefKey()));
        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("formKey"))),
                StringUtils.trimToNull(query.getFormKey()));
        List<Map<String, Object>> permissions = normalizeFieldPermissions(formInfo.get("formFieldPermissions"));
        JSONObject flowFormRef = readNestedObject(formInfo.get("formRef"));
        JSONObject asset = resolveBusinessTaskFormAsset(objectCode, formKey);
        if (asset.isEmpty() && StringUtils.isBlank(formKey) && permissions.isEmpty()) {
            if (StringUtils.isNotBlank(runtime.configKey())) {
                JSONObject defaultNodeForm = new JSONObject();
                putText(defaultNodeForm, "taskDefKey", taskDefKey);
                putText(defaultNodeForm, "taskName", textValue(formInfo.get("taskName")));
                defaultNodeForm.put("formMode", "BUSINESS_OBJECT_FORM");
                defaultNodeForm.put("editMode", "EDITABLE");
                defaultNodeForm.put("viewKey", "default");
                putBoolean(defaultNodeForm, formInfo, "allowApprove");
                putBoolean(defaultNodeForm, formInfo, "allowDelegate");
                putBoolean(defaultNodeForm, formInfo, "allowReject");
                putBoolean(defaultNodeForm, formInfo, "allowRejectToStart");
                putBoolean(defaultNodeForm, formInfo, "allowReturn");
                putBoolean(defaultNodeForm, formInfo, "allowTerminate");
                putBoolean(defaultNodeForm, formInfo, "requireSignature");
                putBoolean(defaultNodeForm, formInfo, "requireComment");
                return defaultNodeForm;
            }
            return new JSONObject();
        }

        JSONObject nodeForm = new JSONObject();
        putText(nodeForm, "taskDefKey", taskDefKey);
        putText(nodeForm, "taskName", textValue(formInfo.get("taskName")));
        putText(nodeForm, "formKey", StringUtils.firstNonBlank(
                formKey,
                StringUtils.trimToNull(flowFormRef.getString("formKey")),
                asset.getString("formKey")));
        putText(nodeForm, "formName", StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("formName"))),
                StringUtils.trimToNull(flowFormRef.getString("formName")),
                asset.getString("formName")));
        putText(nodeForm, "providerKey", StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("providerKey"))),
                StringUtils.trimToNull(flowFormRef.getString("providerKey")),
                asset.getString("providerKey")));
        putText(nodeForm, "formUrl", StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("formUrl"))),
                StringUtils.trimToNull(flowFormRef.getString("formUrl")),
                asset.getString("formUrl")));
        putText(nodeForm, "viewKey", StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("viewKey"))),
                StringUtils.trimToNull(flowFormRef.getString("viewKey")),
                asset.getString("viewKey"),
                "default"));
        String formMode = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("formMode"))),
                StringUtils.trimToNull(flowFormRef.getString("formMode")),
                StringUtils.trimToNull(flowFormRef.getString("type")),
                asset.getString("formMode"),
                runtime.configKey() == null ? "BUSINESS_CODE_FORM" : "BUSINESS_OBJECT_FORM");
        putText(nodeForm, "formMode", normalizeNodeFormMode(formMode));
        putBoolean(nodeForm, formInfo, "allowApprove");
        putBoolean(nodeForm, formInfo, "allowDelegate");
        putBoolean(nodeForm, formInfo, "allowReject");
        putBoolean(nodeForm, formInfo, "allowRejectToStart");
        putBoolean(nodeForm, formInfo, "allowReturn");
        putBoolean(nodeForm, formInfo, "allowTerminate");
        putBoolean(nodeForm, formInfo, "requireSignature");
        putBoolean(nodeForm, formInfo, "requireComment");
        boolean businessObjectDefaultWritable = permissions.isEmpty()
                && "BUSINESS_OBJECT_FORM".equals(normalizeNodeFormMode(formMode));
        nodeForm.put("editMode", permissions.stream().anyMatch(item -> readBooleanValue(item.get("writable"), false))
                || businessObjectDefaultWritable ? "EDITABLE" : "READONLY");
        JSONObject effectiveFormRef = new JSONObject();
        if (!asset.isEmpty()) {
            effectiveFormRef.putAll(asset);
        }
        if (!flowFormRef.isEmpty()) {
            effectiveFormRef.putAll(flowFormRef);
        }
        putText(effectiveFormRef, "formKey", nodeForm.getString("formKey"));
        putText(effectiveFormRef, "formMode", nodeForm.getString("formMode"));
        putText(effectiveFormRef, "type", nodeForm.getString("formMode"));
        putText(effectiveFormRef, "formName", nodeForm.getString("formName"));
        putText(effectiveFormRef, "providerKey", nodeForm.getString("providerKey"));
        putText(effectiveFormRef, "formUrl", nodeForm.getString("formUrl"));
        putText(effectiveFormRef, "viewKey", nodeForm.getString("viewKey"));
        if (!effectiveFormRef.isEmpty()) {
            nodeForm.put("formRef", effectiveFormRef);
        }
        if (!permissions.isEmpty()) {
            nodeForm.put("fieldPermissions", permissions);
        }
        return nodeForm;
    }

    private Map<String, Object> loadFlowNodeFormInfo(TaskFormRuntimeContext runtime,
                                                     BusinessTaskFormContextQueryDTO query) {
        Map<String, Object> taskFormInfo = loadTaskFormInfo(query.getTaskId());
        if (isCompleteFlowNodeFormInfo(taskFormInfo)) {
            return taskFormInfo;
        }
        Map<String, Object> processFormInfo = loadProcessFormInfo(runtime, query);
        if (taskFormInfo.isEmpty()) {
            return processFormInfo;
        }
        if (processFormInfo.isEmpty()) {
            return taskFormInfo;
        }
        Map<String, Object> merged = new LinkedHashMap<>(processFormInfo);
        taskFormInfo.forEach((key, value) -> {
            if (hasTextValue(value) || value instanceof Map<?, ?> || value instanceof List<?>) {
                merged.put(key, value);
            }
        });
        return merged;
    }

    private boolean isCompleteFlowNodeFormInfo(Map<String, Object> formInfo) {
        if (formInfo == null || formInfo.isEmpty()) {
            return false;
        }
        Object formType = formInfo.get("formType");
        if (!hasTextValue(formType)) {
            return false;
        }
        if ("none".equalsIgnoreCase(String.valueOf(formType))) {
            return true;
        }
        return hasTextValue(formInfo.get("formKey"))
                || hasTextValue(formInfo.get("formUrl"))
                || hasTextValue(formInfo.get("formJson"))
                || hasTextValue(formInfo.get("formFieldPermissions"));
    }

    private Map<String, Object> loadTaskFormInfo(String taskId) {
        if (flowClient == null || StringUtils.isBlank(taskId)) {
            return Map.of();
        }
        try {
            FlowResult<Map<String, Object>> result = flowClient.getTaskFormInfo(taskId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return Map.of();
            }
            return result.getData();
        } catch (Exception e) {
            log.warn("读取流程节点表单配置失败: taskId={}, error={}", taskId, e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> loadProcessFormInfo(TaskFormRuntimeContext runtime,
                                                    BusinessTaskFormContextQueryDTO query) {
        if (flowClient == null || query == null) {
            return Map.of();
        }
        String processInstanceId = StringUtils.trimToNull(query.getProcessInstanceId());
        String businessKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getBusinessKey()),
                runtime == null ? null : StringUtils.trimToNull(runtime.businessKey()));
        String processDefKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getProcessDefKey()),
                runtime == null || runtime.bindingConfig() == null ? null : resolveFlowModelKey(runtime.bindingConfig()));
        String taskId = StringUtils.trimToNull(query.getTaskId());
        String taskDefKey = StringUtils.trimToNull(query.getTaskDefKey());
        if (StringUtils.isBlank(processInstanceId)
                && StringUtils.isBlank(businessKey)
                && StringUtils.isBlank(processDefKey)
                && StringUtils.isBlank(taskId)
                && StringUtils.isBlank(taskDefKey)) {
            return Map.of();
        }
        try {
            FlowResult<Map<String, Object>> result = flowClient.getProcessFormInfo(
                    processInstanceId,
                    businessKey,
                    processDefKey,
                    taskId,
                    taskDefKey);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return Map.of();
            }
            return result.getData();
        } catch (Exception e) {
            log.warn("读取流程实例表单配置失败: processInstanceId={}, businessKey={}, processDefKey={}, taskDefKey={}, error={}",
                    processInstanceId, businessKey, processDefKey, taskDefKey, e.getMessage());
            return Map.of();
        }
    }

    private boolean hasTextValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence sequence) {
            return StringUtils.isNotBlank(sequence.toString());
        }
        return true;
    }

    private JSONObject resolveBusinessTaskFormAsset(String objectCode, String formKey) {
        List<Map<String, Object>> assets = collectTaskFormAssets(objectCode);
        if (assets.isEmpty()) {
            return new JSONObject();
        }
        if (StringUtils.isNotBlank(formKey)) {
            for (Map<String, Object> asset : assets) {
                if (StringUtils.equals(formKey, StringUtils.trimToNull(textValue(asset.get("formKey"))))) {
                    return readNestedObject(asset);
                }
            }
        }
        return assets.size() == 1 ? readNestedObject(assets.get(0)) : new JSONObject();
    }

    private List<Map<String, Object>> collectTaskFormAssets(String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return List.of();
        }
        Long tenantId = resolveTenantId();
        BusinessObjectVO object = queryBusinessObject(tenantId, objectCode);
        List<Map<String, Object>> assets = new ArrayList<>();
        if (object != null) {
            AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, StringUtils.firstNonBlank(
                    object.getConfigKey(), object.getObjectCode(), objectCode));
            JSONObject designerOptions = readJsonObject(object.getDesignerOptions());
            JSONObject formSchema = readNestedObject(designerOptions.get("formDesignerSchema"));
            assets.addAll(collectBusinessFormAssets(object, formSchema));
            appendUniqueFormAssets(assets, collectRuntimeCrudFormAssets(object, runtimeConfig));
            JSONObject metadata = readCodeAppMetadata(tenantId, object.getObjectCode());
            appendUniqueFormAssets(assets, mergeCodeAppAssets(
                    object.getObjectCode(), codeFormProviderRegistry.listAssets(object.getObjectCode()),
                    metadata, false));
        } else {
            AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, objectCode);
            appendUniqueFormAssets(assets, collectRuntimeCrudFormAssets(null, runtimeConfig));
            JSONObject metadata = readCodeAppMetadata(tenantId, objectCode);
            appendUniqueFormAssets(assets, mergeCodeAppAssets(
                    objectCode, codeFormProviderRegistry.listAssets(objectCode), metadata, false));
        }
        return assets;
    }

    private JSONObject resolveBusinessFormSchema(BusinessObjectVO object, String formKey, String configKey) {
        AiCrudConfig runtimeConfig = resolveRuntimeConfigForBusinessForm(object, configKey);
        if (object == null) {
            return buildRuntimeCrudFormSchema(null, runtimeConfig, formKey);
        }
        JSONObject designerOptions = readJsonObject(object.getDesignerOptions());
        JSONObject formSchema = readNestedObject(designerOptions.get("formDesignerSchema"));
        if (formSchema.isEmpty()) {
            return buildRuntimeCrudFormSchema(object, runtimeConfig, formKey);
        }
        String targetFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formKey),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(formSchema.getString("formKey")));

        JSONObject byForms = findFormSchemaInArray(readNestedArray(formSchema.get("forms")), targetFormKey);
        if (!byForms.isEmpty()) {
            return byForms;
        }
        JSONObject settings = readNestedObject(formSchema.get("settings"));
        JSONObject byAssets = findFormSchemaInArray(readNestedArray(settings.get("formAssets")), targetFormKey);
        if (!byAssets.isEmpty()) {
            return byAssets;
        }
        String rootFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formSchema.getString("formKey")),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")));
        if (StringUtils.isBlank(targetFormKey) || StringUtils.equals(targetFormKey, rootFormKey)) {
            return formSchema;
        }
        JSONObject runtimeFormSchema = buildRuntimeCrudFormSchema(object, runtimeConfig, targetFormKey);
        if (!runtimeFormSchema.isEmpty()) {
            return runtimeFormSchema;
        }
        return new JSONObject();
    }

    private JSONObject findFormSchemaInArray(JSONArray forms, String formKey) {
        if (forms == null || forms.isEmpty() || StringUtils.isBlank(formKey)) {
            return new JSONObject();
        }
        for (int i = 0; i < forms.size(); i++) {
            JSONObject form = forms.getJSONObject(i);
            if (form == null) {
                continue;
            }
            JSONObject schema = readNestedObject(form.get("schema"));
            JSONObject candidate = schema.isEmpty() ? form : schema;
            String candidateKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(form.getString("formKey")),
                    StringUtils.trimToNull(candidate.getString("formKey")),
                    StringUtils.trimToNull(candidate.getString("defaultFormKey")));
            if (StringUtils.equals(formKey, candidateKey)) {
                return candidate;
            }
        }
        return new JSONObject();
    }

    private JSONObject readCodeAppMetadata(Long tenantId, String objectCode) {
        if (tenantId == null || StringUtils.isBlank(objectCode)) {
            return new JSONObject();
        }
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, objectCode);
        if (binding == null) {
            return new JSONObject();
        }
        JSONObject config = readBindingConfig(binding.getBindingConfig());
        JSONObject options = readNestedObject(config.get("options"));
        return readNestedObject(options.get("codeAppMetadata"));
    }

    private Map<String, Object> sanitizeCodeAppMetadata(JSONObject metadata, boolean includeInternal) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>(metadata);
        List<Map<String, Object>> fields = normalizeCodeAppMetadataFields(metadata.get("fields"), includeInternal);
        if (fields.isEmpty()) {
            fields = firstCodeAppAssetFields(metadata, includeInternal);
        }
        result.put("fields", fields);

        List<Map<String, Object>> formAssets = readMapList(readNestedArray(metadata.get("formAssets")));
        if (!formAssets.isEmpty()) {
            List<Map<String, Object>> normalizedAssets = new ArrayList<>();
            for (Map<String, Object> asset : formAssets) {
                Map<String, Object> item = new LinkedHashMap<>(asset);
                List<Map<String, Object>> assetFields = normalizeCodeAppMetadataFields(
                        item.get("fields") == null ? item.get("fieldCatalog") : item.get("fields"), includeInternal);
                if (assetFields.isEmpty()) {
                    assetFields = fields;
                }
                item.put("fields", assetFields);
                item.put("fieldCatalog", assetFields);
                item.put("fieldCount", assetFields.size());
                item.put("fieldPreview", buildCodeAppFieldPreview(assetFields));
                normalizedAssets.add(item);
            }
            result.put("formAssets", normalizedAssets);
        }
        return result;
    }

    private List<Map<String, Object>> mergeCodeAppAssets(String objectCode,
                                                         List<Map<String, Object>> providerAssets,
                                                         JSONObject metadata,
                                                         boolean includeInternal) {
        if (metadata == null || metadata.isEmpty()) {
            return providerAssets == null ? List.of() : providerAssets;
        }
        List<Map<String, Object>> configuredAssets = readMapList(readNestedArray(metadata.get("formAssets")));
        Set<String> removedAssetKeys = readStringSet(metadata.get("removedFormAssetKeys"));
        List<Map<String, Object>> globalFields = normalizeCodeAppMetadataFields(metadata.get("fields"), includeInternal);
        Set<String> globalHiddenFields = includeInternal ? Set.of() : collectNonPublicCodeAppFieldCodes(metadata.get("fields"));
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> usedConfiguredAssetKeys = new LinkedHashSet<>();
        for (Map<String, Object> providerAsset : providerAssets == null ? List.<Map<String, Object>>of() : providerAssets) {
            if (providerAsset == null) {
                continue;
            }
            String providerAssetKey = codeAppAssetKey(providerAsset);
            if (StringUtils.isNotBlank(providerAssetKey) && removedAssetKeys.contains(providerAssetKey)) {
                continue;
            }
            Map<String, Object> configuredAsset = findConfiguredCodeAppAsset(configuredAssets, providerAsset);
            Map<String, Object> item = new LinkedHashMap<>(providerAsset);
            if (configuredAsset != null) {
                mergeCodeAppAssetDisplay(item, configuredAsset);
                String configuredKey = codeAppAssetKey(configuredAsset);
                if (StringUtils.isNotBlank(configuredKey)) {
                    usedConfiguredAssetKeys.add(configuredKey);
                }
            }
            List<Map<String, Object>> providerFields = readMapList(readNestedArray(
                    providerAsset.get("fields") == null ? providerAsset.get("fieldCatalog") : providerAsset.get("fields")));
            List<Map<String, Object>> configuredFields = globalFields;
            if (configuredFields.isEmpty() && configuredAsset != null) {
                configuredFields = normalizeCodeAppMetadataFields(
                        configuredAsset.get("fields") == null ? configuredAsset.get("fieldCatalog") : configuredAsset.get("fields"),
                        includeInternal);
            }
            Set<String> hiddenFields = new LinkedHashSet<>(globalHiddenFields);
            if (!includeInternal && configuredAsset != null) {
                hiddenFields.addAll(collectNonPublicCodeAppFieldCodes(
                        configuredAsset.get("fields") == null ? configuredAsset.get("fieldCatalog") : configuredAsset.get("fields")));
            }
            List<Map<String, Object>> fields = mergeCodeAppFields(providerFields, configuredFields, hiddenFields);
            if (!fields.isEmpty()) {
                item.put("fields", fields);
                item.put("fieldCatalog", fields);
                item.put("fieldCount", fields.size());
                item.put("fieldPreview", buildCodeAppFieldPreview(fields));
                item.put("metadataConfigured", true);
            }
            item.put("objectCode", StringUtils.defaultIfBlank(textValue(item.get("objectCode")), objectCode));
            result.add(item);
        }
        for (Map<String, Object> configuredAsset : configuredAssets) {
            String configuredKey = codeAppAssetKey(configuredAsset);
            if (StringUtils.isNotBlank(configuredKey) && removedAssetKeys.contains(configuredKey)) {
                continue;
            }
            if (StringUtils.isNotBlank(configuredKey) && usedConfiguredAssetKeys.contains(configuredKey)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(configuredAsset);
            List<Map<String, Object>> fields = normalizeCodeAppMetadataFields(
                    item.get("fields") == null ? item.get("fieldCatalog") : item.get("fields"),
                    includeInternal);
            if (fields.isEmpty()) {
                fields = globalFields;
            }
            item.put("objectCode", StringUtils.defaultIfBlank(textValue(item.get("objectCode")), objectCode));
            item.put("fields", fields);
            item.put("fieldCatalog", fields);
            item.put("fieldCount", fields.size());
            item.put("fieldPreview", buildCodeAppFieldPreview(fields));
            item.put("metadataConfigured", true);
            result.add(item);
        }
        return result;
    }

    private String codeAppAssetKey(Map<String, Object> asset) {
        if (asset == null) {
            return null;
        }
        String formKey = StringUtils.trimToNull(textValue(asset.get("formKey")));
        if (StringUtils.isNotBlank(formKey)) {
            return "form:" + formKey;
        }
        String providerKey = StringUtils.trimToNull(textValue(asset.get("providerKey")));
        return StringUtils.isBlank(providerKey) ? null : "provider:" + providerKey;
    }

    private Set<String> readStringSet(Object source) {
        Set<String> result = new LinkedHashSet<>();
        if (source == null) {
            return result;
        }
        if (source instanceof String text && !StringUtils.trimToEmpty(text).startsWith("[")) {
            for (String item : text.split(",")) {
                String value = StringUtils.trimToNull(item);
                if (value != null) {
                    result.add(value);
                }
            }
            return result;
        }
        JSONArray array = readNestedArray(source);
        for (int i = 0; i < array.size(); i++) {
            String value = StringUtils.trimToNull(textValue(array.get(i)));
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private List<Map<String, Object>> mergeCodeAppFields(List<Map<String, Object>> providerFields,
                                                         List<Map<String, Object>> configuredFields,
                                                         Set<String> hiddenFields) {
        Map<String, Map<String, Object>> configuredMap = new LinkedHashMap<>();
        for (Map<String, Object> configured : configuredFields == null ? List.<Map<String, Object>>of() : configuredFields) {
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(configured.get("field"))),
                    StringUtils.trimToNull(textValue(configured.get("fieldCode"))),
                    StringUtils.trimToNull(textValue(configured.get("code"))));
            if (fieldCode != null) {
                configuredMap.put(fieldCode, configured);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> providerField : providerFields == null ? List.<Map<String, Object>>of() : providerFields) {
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(providerField.get("field"))),
                    StringUtils.trimToNull(textValue(providerField.get("fieldCode"))),
                    StringUtils.trimToNull(textValue(providerField.get("code"))));
            if (fieldCode == null || hiddenFields.contains(fieldCode) || !seen.add(fieldCode)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(providerField);
            Map<String, Object> configured = configuredMap.get(fieldCode);
            if (configured != null) {
                mergeNonNull(item, configured);
            }
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            result.add(item);
        }

        for (Map<String, Object> configured : configuredFields == null ? List.<Map<String, Object>>of() : configuredFields) {
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(configured.get("field"))),
                    StringUtils.trimToNull(textValue(configured.get("fieldCode"))),
                    StringUtils.trimToNull(textValue(configured.get("code"))));
            if (fieldCode == null || !seen.add(fieldCode)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(configured);
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            result.add(item);
        }
        return result;
    }

    private Set<String> collectNonPublicCodeAppFieldCodes(Object source) {
        JSONArray array = readNestedArray(source);
        if (array.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject field = array.getJSONObject(i);
            if (field == null) {
                continue;
            }
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(field.getString("field")),
                    StringUtils.trimToNull(field.getString("fieldCode")),
                    StringUtils.trimToNull(field.getString("code")));
            if (fieldCode != null && !isPublicCodeAppField(field)) {
                result.add(fieldCode);
            }
        }
        return result;
    }

    private void mergeCodeAppAssetDisplay(Map<String, Object> target, Map<String, Object> configured) {
        for (String key : List.of("appName", "objectName", "businessName", "formKey", "formName",
                "formMode", "type", "providerKey", "providerName", "formUrl", "description")) {
            String value = StringUtils.trimToNull(textValue(configured.get(key)));
            if (value != null) {
                target.put(key, value);
            }
        }
        if (configured.containsKey("supportsSave")) {
            target.put("supportsSave", readBooleanValue(configured.get("supportsSave"), true));
        }
    }

    private Map<String, Object> findConfiguredCodeAppAsset(List<Map<String, Object>> configuredAssets,
                                                           Map<String, Object> providerAsset) {
        if (configuredAssets == null || configuredAssets.isEmpty()) {
            return null;
        }
        String formKey = StringUtils.trimToNull(textValue(providerAsset.get("formKey")));
        String providerKey = StringUtils.trimToNull(textValue(providerAsset.get("providerKey")));
        for (Map<String, Object> asset : configuredAssets) {
            if (asset == null) {
                continue;
            }
            if (StringUtils.isNotBlank(formKey) && StringUtils.equals(formKey, StringUtils.trimToNull(textValue(asset.get("formKey"))))) {
                return asset;
            }
            if (StringUtils.isNotBlank(providerKey)
                    && StringUtils.equals(providerKey, StringUtils.trimToNull(textValue(asset.get("providerKey"))))) {
                return asset;
            }
        }
        return null;
    }

    private List<Map<String, Object>> firstCodeAppAssetFields(JSONObject metadata, boolean includeInternal) {
        List<Map<String, Object>> assets = readMapList(readNestedArray(metadata.get("formAssets")));
        for (Map<String, Object> asset : assets) {
            List<Map<String, Object>> fields = normalizeCodeAppMetadataFields(
                    asset.get("fields") == null ? asset.get("fieldCatalog") : asset.get("fields"), includeInternal);
            if (!fields.isEmpty()) {
                return fields;
            }
        }
        return List.of();
    }

    private List<Map<String, Object>> normalizeCodeAppMetadataFields(Object source, boolean includeInternal) {
        JSONArray array = readNestedArray(source);
        if (array.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject field = array.getJSONObject(i);
            if (field == null) {
                continue;
            }
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(field.getString("field")),
                    StringUtils.trimToNull(field.getString("fieldCode")),
                    StringUtils.trimToNull(field.getString("code")));
            if (fieldCode == null || !seen.add(fieldCode)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(field);
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            item.putIfAbsent("label", fieldCode);
            item.putIfAbsent("componentType", StringUtils.defaultIfBlank(textValue(item.get("type")), "input"));
            item.putIfAbsent("type", normalizeTaskFormFieldType(textValue(item.get("componentType"))));
            item.putIfAbsent("visible", true);
            item.putIfAbsent("readonly", !readBooleanValue(item.get("writable"), true));
            if (!includeInternal && !isPublicCodeAppField(item)) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    private boolean isPublicCodeAppField(Map<String, Object> field) {
        if (field == null) {
            return false;
        }
        return readBooleanValue(field.get("visible"), true)
                && !readBooleanValue(field.get("internal"), false)
                && !readBooleanValue(field.get("systemField"), false);
    }

    private boolean isPublicCodeAppFormField(Map<String, Object> field) {
        return isPublicCodeAppField(field) && readBooleanValue(field.get("formVisible"), true);
    }

    private List<String> buildCodeAppFieldPreview(List<Map<String, Object>> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        List<String> preview = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            String text = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(field.get("label"))),
                    StringUtils.trimToNull(textValue(field.get("field"))));
            if (text != null) {
                preview.add(text);
            }
            if (preview.size() >= 5) {
                break;
            }
        }
        return preview;
    }

    private BusinessTaskFormContextVO applyBusinessCodeMetadataFields(BusinessTaskFormContextVO context,
                                                                      String objectCode) {
        if (context == null) {
            return null;
        }
        String code = StringUtils.firstNonBlank(StringUtils.trimToNull(objectCode), StringUtils.trimToNull(context.getObjectCode()));
        List<Map<String, Object>> configuredFields = normalizeCodeAppMetadataFields(
                readCodeAppMetadata(resolveTenantId(), code).get("fields"), false);
        if (configuredFields.isEmpty() || context.getFields() == null || context.getFields().isEmpty()) {
            return context;
        }
        Map<String, Map<String, Object>> configuredMap = new LinkedHashMap<>();
        for (Map<String, Object> field : configuredFields) {
            String fieldCode = StringUtils.trimToNull(textValue(field.get("field")));
            if (fieldCode != null) {
                putPermissionAliases(configuredMap, fieldCode, field);
            }
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> source : context.getFields()) {
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(source.get("field"))),
                    StringUtils.trimToNull(textValue(source.get("fieldCode"))));
            Map<String, Object> configured = fieldCode == null ? null : configuredMap.get(fieldCode);
            if (configured == null || !isPublicCodeAppFormField(configured)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(source);
            mergeNonNull(item, configured);
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            filtered.add(item);
        }
        context.setFields(filtered);
        context.setRecordData(filterVisibleRecordData(context.getRecordData(), filtered));
        return context;
    }

    private void mergeNonNull(Map<String, Object> target, Map<String, Object> source) {
        if (target == null || source == null) {
            return;
        }
        source.forEach((key, value) -> {
            if (value != null) {
                target.put(key, value);
            }
        });
    }

    private List<Map<String, Object>> buildTaskFormFields(List<Map<String, Object>> fieldCatalog,
                                                          List<Map<String, Object>> permissions) {
        Map<String, Map<String, Object>> permissionMap = new LinkedHashMap<>();
        for (Map<String, Object> permission : permissions) {
            String field = StringUtils.trimToNull(textValue(permission.get("field")));
            if (field != null) {
                putPermissionAliases(permissionMap, field, permission);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> field : fieldCatalog) {
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(field.get("field"))),
                    StringUtils.trimToNull(textValue(field.get("fieldCode"))));
            if (fieldCode == null) {
                continue;
            }
            Map<String, Object> permission = permissionMap.get(fieldCode);
            boolean readable = permission != null
                    ? readBooleanValue(permission.get("readable"), true)
                    : true;
            if (!readable) {
                continue;
            }
            boolean writable = permission != null && readBooleanValue(permission.get("writable"), false);
            boolean required = writable && permission != null && readBooleanValue(permission.get("required"), false);
            Map<String, Object> item = new LinkedHashMap<>(field);
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            item.put("label", StringUtils.defaultIfBlank(textValue(field.get("label")), fieldCode));
            String rawType = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(item.get("type"))),
                    StringUtils.trimToNull(textValue(item.get("componentType"))),
                    StringUtils.trimToNull(textValue(item.get("componentKey"))));
            String normalizedType = normalizeTaskFormFieldType(rawType);
            item.put("type", normalizedType);
            item.putIfAbsent("componentType", normalizedType);
            item.putIfAbsent("dataType", StringUtils.trimToEmpty(textValue(field.get("dataType"))));
            Map<String, Object> props = new LinkedHashMap<>(readNestedObject(item.get("props")));
            String dictType = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(item.get("dictType"))),
                    StringUtils.trimToNull(textValue(props.get("dictType"))));
            if (dictType != null) {
                item.put("dictType", dictType);
            }
            item.put("readable", true);
            item.put("writable", writable);
            item.put("required", required);
            item.put("readonly", !writable);
            item.put("disabled", !writable);
            props.put("disabled", !writable);
            props.put("readonly", !writable);
            if (item.get("dictType") != null) {
                props.put("dictType", item.get("dictType"));
            }
            item.put("props", props);
            result.add(item);
        }
        return result;
    }

    private String resolveBusinessObjectTaskEditMode(JSONObject nodeForm, List<Map<String, Object>> permissions) {
        if (permissions != null && permissions.stream().anyMatch(item -> readBooleanValue(item.get("writable"), false))) {
            return "EDITABLE";
        }
        return normalizeNodeEditMode(nodeForm == null ? null : nodeForm.getString("editMode"));
    }

    private List<Map<String, Object>> normalizeBusinessObjectTaskPermissions(List<Map<String, Object>> fieldCatalog,
                                                                             List<Map<String, Object>> permissions) {
        if (permissions != null && !permissions.isEmpty()) {
            return permissions;
        }
        if (fieldCatalog == null || fieldCatalog.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> field : fieldCatalog) {
            if (field == null) {
                continue;
            }
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(field.get("field"))),
                    StringUtils.trimToNull(textValue(field.get("fieldCode"))));
            if (fieldCode == null || !seen.add(fieldCode)) {
                continue;
            }
            if (readBooleanValue(field.get("internal"), false) || readBooleanValue(field.get("systemField"), false)) {
                continue;
            }
            JSONObject props = readNestedObject(field.get("props"));
            boolean writable = !readBooleanValue(field.get("readonly"), false)
                    && !readBooleanValue(field.get("disabled"), false)
                    && !readBooleanValue(props.get("readonly"), false)
                    && !readBooleanValue(props.get("disabled"), false);
            Map<String, Object> permission = new LinkedHashMap<>();
            permission.put("field", fieldCode);
            permission.put("fieldCode", fieldCode);
            permission.put("label", StringUtils.defaultIfBlank(textValue(field.get("label")), fieldCode));
            permission.put("visible", true);
            permission.put("editable", writable);
            permission.put("readable", true);
            permission.put("writable", writable);
            permission.put("required", writable && readBooleanValue(field.get("required"), false));
            result.add(permission);
        }
        return result;
    }

    private String normalizeTaskFormFieldType(String componentType) {
        String type = StringUtils.defaultIfBlank(componentType, "input").trim();
        return switch (type) {
            case "textarea" -> "textarea";
            case "inputNumber", "input-number", "integer", "decimal", "money", "number" -> "number";
            case "dictSelect" -> "dictSelect";
            case "select", "radio", "radioButton", "checkbox", "date", "datetime", "daterange", "datetimerange",
                    "month", "year", "time", "timerange", "switch", "imageUpload", "fileUpload", "slider", "rate",
                    "color", "regionTreeSelect", "treeSelect", "transfer", "customSelect", "objectReference",
                    "recordSelector", "userSelect", "orgTreeSelect", "cascader", "text", "slot" -> type;
            case "upload" -> "fileUpload";
            default -> "input";
        };
    }

    private Map<String, Object> filterVisibleRecordData(Map<String, Object> recordData, List<Map<String, Object>> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (recordData == null || fields == null) {
            return result;
        }
        for (Map<String, Object> field : fields) {
            String fieldCode = StringUtils.trimToNull(textValue(field.get("field")));
            if (fieldCode != null) {
                result.put(fieldCode, readRecordValue(recordData, fieldCode));
            }
            for (String displayField : collectReferenceDisplayFields(field)) {
                Object displayValue = readRecordValue(recordData, displayField);
                if (displayValue != null || containsRecordField(recordData, displayField)) {
                    result.put(displayField, displayValue);
                }
            }
        }
        Object children = recordData.get("children");
        if (children instanceof Map<?, ?> || children instanceof List<?>) {
            result.put("children", children);
        }
        return result;
    }

    private Set<String> collectReferenceDisplayFields(Map<String, Object> field) {
        Set<String> result = new LinkedHashSet<>();
        if (field == null) {
            return result;
        }
        Map<String, Object> props = new LinkedHashMap<>(readNestedObject(field.get("props")));
        boolean selectionField = isSelectionLikeTaskField(field, props);
        addTextFieldName(result, field.get("referenceDisplayField"));
        addTextFieldName(result, field.get("displayField"));
        addTextFieldName(result, field.get("labelField"));
        addTextFieldName(result, field.get("targetLabelField"));
        addTextFieldName(result, field.get("labelValueField"));
        addTextFieldName(result, field.get("targetField"));
        addTextFieldName(result, props.get("referenceDisplayField"));
        addTextFieldName(result, props.get("displayField"));
        addTextFieldName(result, props.get("labelField"));
        addTextFieldName(result, props.get("targetLabelField"));
        addTextFieldName(result, props.get("labelValueField"));
        addTextFieldName(result, props.get("targetField"));
        String fieldCode = StringUtils.trimToNull(textValue(field.get("field")));
        if (selectionField && fieldCode != null) {
            result.add(fieldCode + "Name");
            if (fieldCode.endsWith("Id")) {
                result.add(fieldCode.substring(0, fieldCode.length() - 2) + "Name");
            }
        }
        result.remove(fieldCode);
        return result;
    }

    private boolean isSelectionLikeTaskField(Map<String, Object> field, Map<String, Object> props) {
        String type = normalizeTaskFormFieldType(StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(field.get("type"))),
                StringUtils.trimToNull(textValue(field.get("componentType"))),
                StringUtils.trimToNull(textValue(field.get("componentKey")))));
        return Set.of("objectReference", "recordSelector", "userSelect", "orgTreeSelect",
                        "treeSelect", "cascader", "select", "dictSelect").contains(type)
                || StringUtils.isNotBlank(textValue(field.get("referenceObjectCode")))
                || StringUtils.isNotBlank(textValue(props.get("referenceObjectCode")))
                || StringUtils.isNotBlank(textValue(field.get("referenceDisplayField")))
                || StringUtils.isNotBlank(textValue(props.get("referenceDisplayField")));
    }

    private void addTextFieldName(Set<String> target, Object value) {
        String text = StringUtils.trimToNull(textValue(value));
        if (text != null) {
            target.add(text);
        }
    }

    private void putPermissionAliases(Map<String, Map<String, Object>> permissionMap,
                                      String field,
                                      Map<String, Object> permission) {
        if (permissionMap == null || StringUtils.isBlank(field) || permission == null) {
            return;
        }
        permissionMap.putIfAbsent(field, permission);
        String camelField = snakeToCamel(field);
        if (StringUtils.isNotBlank(camelField)) {
            permissionMap.putIfAbsent(camelField, permission);
        }
        String snakeField = camelToSnake(field);
        if (StringUtils.isNotBlank(snakeField)) {
            permissionMap.putIfAbsent(snakeField, permission);
        }
    }

    private Set<String> collectPermissionFields(List<Map<String, Object>> permissions, String permissionKey, boolean expected) {
        Set<String> result = new LinkedHashSet<>();
        if (permissions == null) {
            return result;
        }
        for (Map<String, Object> permission : permissions) {
            if (readBooleanValue(permission.get(permissionKey), false) == expected) {
                String field = StringUtils.trimToNull(textValue(permission.get("field")));
                if (field != null) {
                    result.add(field);
                }
            }
        }
        return result;
    }

    private void validateRequiredTaskFields(List<Map<String, Object>> permissions,
                                            Map<String, Object> updateData,
                                            Map<String, Object> input) {
        if (permissions == null) {
            return;
        }
        for (Map<String, Object> permission : permissions) {
            boolean required = readBooleanValue(permission.get("required"), false);
            boolean writable = readBooleanValue(permission.get("writable"), false);
            if (!required || !writable) {
                continue;
            }
            String field = StringUtils.trimToNull(textValue(permission.get("field")));
            if (field == null) {
                continue;
            }
            if (!input.containsKey(field)) {
                throw new BusinessException("请填写必填字段: " + field);
            }
            Object value = updateData.get(field);
            if (value == null || StringUtils.isBlank(String.valueOf(value))) {
                throw new BusinessException("请填写必填字段: " + field);
            }
        }
    }

    private String parseBusinessKeyObjectCode(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            return null;
        }
        return StringUtils.trimToNull(businessKey.split(":", 2)[0]);
    }

    private Long parseBusinessKeyRecordId(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            return null;
        }
        String value = StringUtils.trimToNull(businessKey.split(":", 2)[1]);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLongValue(String value) {
        String text = StringUtils.trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 旧触发器路径兼容返回，保留 config 包装结构。
     */
    public JSONObject getFlowBindingLegacy(String objectCode) {
        BusinessFlowBindingVO binding = getFlowBinding(objectCode);
        if (binding == null) {
            return null;
        }
        JSONObject result = new JSONObject();
        result.put("bindingId", binding.getBindingId());
        result.put("bindingName", StringUtils.defaultIfBlank(binding.getFlowModelName(), binding.getFlowModelKey()));
        result.put("objectCode", binding.getObjectCode());
        result.put("status", binding.getStatus());
        result.put("config", toConfigJson(binding));
        return result;
    }

    /**
     * 保存流程绑定配置
     */
    public void saveFlowBinding(String objectCode, BusinessFlowBindingDTO dto) {
        if (dto == null) {
            throw new BusinessException("流程绑定配置不能为空");
        }
        Long tenantId = resolveTenantId();
        String canonicalObjectCode = resolveCanonicalObjectCode(tenantId, objectCode);
        JSONObject config = normalizeBindingConfig(dto);
        ensureBusinessBinding(config, tenantId, canonicalObjectCode);
        String flowModelKey = config.getString("flowModelKey");
        if (StringUtils.isBlank(flowModelKey)) {
            throw new BusinessException("流程模型Key不能为空");
        }
        AiBusinessBinding existing = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", canonicalObjectCode, "FLOW");

        if (existing != null) {
            existing.setTargetType("OBJECT");
            existing.setTargetCode(canonicalObjectCode);
            existing.setBindingType("FLOW");
            existing.setBindingConfig(config.toJSONString());
            existing.setBindingKey(flowModelKey);
            existing.setBindingName(resolveBindingName(config));
            existing.setStatus(1);
            bindingMapper.updateById(existing);
            log.info("[低代码流程绑定] 更新主流程绑定: tenantId={}, objectCode={}, bindingId={}, flowModelKey={}",
                    tenantId, objectCode, existing.getId(), flowModelKey);
        } else {
            AiBusinessBinding binding = new AiBusinessBinding();
            binding.setTenantId(tenantId);
            binding.setTargetType("OBJECT");
            binding.setTargetCode(canonicalObjectCode);
            binding.setBindingType("FLOW");
            binding.setBindingKey(flowModelKey);
            binding.setBindingName(resolveBindingName(config));
            binding.setBindingConfig(config.toJSONString());
            binding.setStatus(1);
            binding.setSortOrder(0);
            bindingMapper.insert(binding);
            log.info("[低代码流程绑定] 创建主流程绑定: tenantId={}, objectCode={}, bindingId={}, flowModelKey={}",
                    tenantId, objectCode, binding.getId(), flowModelKey);
        }
        documentConfigService.syncDefaultFlowKeyByObjectCode(tenantId, canonicalObjectCode, flowModelKey);
    }

    /**
     * 旧触发器路径保存兼容，读取 field/variable 后只落 formField/flowVariable。
     */
    public void saveFlowBinding(String objectCode, JSONObject config) {
        saveFlowBinding(objectCode, toDTO(config));
    }

    /**
     * 手动按钮发起单据流程。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlow(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startDocumentFlowInternal(dto, true, null, null, tenantId, false));
    }

    /**
     * 受控 FLOW_ACTION 专用发起入口。业务 key 永远固定为 objectCode:recordId，
     * 远程成功而本地回填失败时，同 key 重试可恢复原流程实例。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlowForCapability(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startDocumentFlowInternal(dto, true, null, null, tenantId, true));
    }

    /**
     * 旧审批入口兼容发起。接口层仍有旧权限校验，这里不再重复要求新流程按钮权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlowForCompatibility(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startDocumentFlowInternal(dto, false, null, null, tenantId, false));
    }

    /**
     * 由触发器调用的流程发起（内部方法）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startFlowFromTrigger(String flowModelKey, String businessKey, String title,
                                                      Long userId, String userName, JSONObject variables) {
        return startFlowFromTrigger(flowModelKey, businessKey, title, userId, userName, resolveTenantId(), variables);
    }

    /**
     * 由触发器调用的流程发起（内部方法）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startFlowFromTrigger(String flowModelKey, String businessKey, String title,
                                                      Long userId, String userName, Long tenantId, JSONObject variables) {
        BusinessKeyParts parts = parseBusinessKey(businessKey);
        BusinessFlowStartDTO dto = new BusinessFlowStartDTO();
        dto.setObjectCode(parts.objectCode());
        dto.setRecordId(parts.recordId());
        dto.setFlowModelKey(flowModelKey);
        dto.setTitle(title);
        if (variables != null) {
            dto.setVariables(new LinkedHashMap<>(variables));
        }
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        return TenantContextHolder.executeWithTenant(effectiveTenantId,
                () -> startDocumentFlowInternal(dto, false, userId, userName, effectiveTenantId, false));
    }

    /**
     * 查询单据流程状态。
     */
    public BusinessFlowRuntimeVO getFlowStatus(String objectCode, Long recordId) {
        if (StringUtils.isBlank(objectCode)) {
            throw new BusinessException("业务对象编码不能为空");
        }
        if (recordId == null) {
            throw new BusinessException("记录ID不能为空");
        }
        String canonicalObjectCode = resolveCanonicalObjectCode(resolveTenantId(), objectCode);
        String businessKey = buildBusinessKey(canonicalObjectCode, recordId);
        AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectLatestByBusinessKey(resolveTenantId(), businessKey);
        if (link == null) {
            BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
            vo.setObjectCode(canonicalObjectCode);
            vo.setRecordId(recordId);
            vo.setBusinessKey(businessKey);
            vo.setFlowStatus("NOT_STARTED");
            vo.setMessage("尚未发起主流程");
            return vo;
        }
        return toRuntimeVO(link, null);
    }

    /**
     * 处理流程引擎回调，按流程结果回写单据状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleFlowCallback(BusinessFlowCallbackDTO dto) {
        if (dto == null || (StringUtils.isBlank(dto.getProcessInstanceId()) && StringUtils.isBlank(dto.getBusinessKey()))) {
            throw new BusinessException("流程回调缺少流程实例ID或业务Key");
        }
        Long tenantId = dto.getTenantId() != null ? dto.getTenantId() : resolveTenantId();
        AiBusinessFlowInstanceLink link = findCallbackLink(tenantId, dto);
        if (link == null) {
            throw new BusinessException("未找到流程实例关联");
        }
        Long effectiveTenantId = link.getTenantId() != null ? link.getTenantId() : tenantId;
        TenantContextHolder.executeWithTenant(effectiveTenantId, () -> handleFlowCallbackInternal(link, dto));
    }

    @FlowCallback(on = {
            FlowCallback.ON_COMPLETED,
            FlowCallback.ON_REJECTED,
            FlowCallback.ON_CANCELED
    })
    public void handleFlowEngineEvent(FlowEventContext ctx) {
        if (ctx == null) {
            return;
        }
        BusinessFlowCallbackDTO dto = new BusinessFlowCallbackDTO();
        dto.setProcessInstanceId(StringUtils.trimToNull(ctx.getProcessInstanceId()));
        dto.setBusinessKey(StringUtils.trimToNull(ctx.getBusinessKey()));
        dto.setFlowStatus(ctx.getEvent());
        dto.setResult(resolveFlowEventResult(ctx.getEvent()));
        dto.setTenantId(ctx.getTenantId());
        dto.setNodeKey(ctx.getTaskDefKey());
        dto.setNodeName(ctx.getTaskName());
        dto.setOperatorId(parseLongValue(ctx.getAssigneeId()));
        dto.setVariables(ctx.getVariables() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(ctx.getVariables()));
        Long tenantId = dto.getTenantId() != null ? dto.getTenantId() : resolveTenantId();
        AiBusinessFlowInstanceLink link = findCallbackLink(tenantId, dto);
        if (link == null) {
            log.debug("[低代码流程回调] 忽略未绑定业务对象的流程事件: event={}, processInstanceId={}, businessKey={}",
                    ctx.getEvent(), ctx.getProcessInstanceId(), ctx.getBusinessKey());
            return;
        }
        Long effectiveTenantId = link.getTenantId() != null ? link.getTenantId() : tenantId;
        try {
            TenantContextHolder.executeWithTenant(effectiveTenantId, () -> handleFlowCallbackInternal(link, dto));
        } catch (Exception e) {
            log.warn("[低代码流程回调] 处理流程事件失败: event={}, processInstanceId={}, businessKey={}, error={}",
                    ctx.getEvent(), ctx.getProcessInstanceId(), ctx.getBusinessKey(), e.getMessage());
            throw e;
        }
    }

    private String resolveFlowEventResult(String event) {
        if (FlowCallback.ON_REJECTED.equals(event)) {
            return "REJECTED";
        }
        if (FlowCallback.ON_CANCELED.equals(event)) {
            return "CANCELED";
        }
        if (FlowCallback.ON_COMPLETED.equals(event)) {
            return "APPROVED";
        }
        return event;
    }

    private BusinessFlowRuntimeVO startDocumentFlowInternal(BusinessFlowStartDTO dto,
                                                            boolean checkPermission,
                                                            Long starterUserId,
                                                            String starterUserName,
                                                            Long tenantId,
                                                            boolean stableBusinessKey) {
        if (dto == null) {
            throw new BusinessException("发起主流程参数不能为空");
        }
        if (StringUtils.isBlank(dto.getObjectCode())) {
            throw new BusinessException("业务对象编码不能为空");
        }
        if (dto.getRecordId() == null) {
            throw new BusinessException("请先保存记录后再发起主流程");
        }
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法发起主流程");
        }

        FlowStartContext startContext = resolveFlowStartContext(tenantId, dto.getObjectCode());
        AiBusinessDocumentConfig documentConfig = startContext.documentConfig();
        AiCrudConfig runtimeConfig = startContext.runtimeConfig();
        String objectCode = startContext.objectCode();
        String configKey = startContext.configKey();
        Map<String, Object> recordData = dynamicCrudService.selectById(configKey, dto.getRecordId());
        if (recordData == null) {
            throw new BusinessException("记录不存在或无权限访问");
        }

        String businessKey = buildBusinessKey(objectCode, dto.getRecordId());
        return executeWithFlowStartLock(tenantId, businessKey, () -> startDocumentFlowLocked(
                dto, checkPermission, starterUserId, starterUserName, tenantId, documentConfig,
                runtimeConfig, objectCode, configKey, recordData, businessKey,
                startContext.requestedObjectCode(), stableBusinessKey));
    }

    private BusinessFlowRuntimeVO startDocumentFlowLocked(BusinessFlowStartDTO dto,
                                                          boolean checkPermission,
                                                          Long starterUserId,
                                                          String starterUserName,
                                                          Long tenantId,
                                                          AiBusinessDocumentConfig documentConfig,
                                                          AiCrudConfig runtimeConfig,
                                                          String objectCode,
                                                          String configKey,
                                                          Map<String, Object> recordData,
                                                          String businessKey,
                                                          String requestedObjectCode,
                                                          boolean stableBusinessKey) {
        if (documentConfig != null) {
            documentRuntimeService.validateStartAllowed(objectCode, dto.getRecordId(), checkPermission);
        }
        AiBusinessFlowInstanceLink runningLink = flowInstanceLinkMapper.selectRunningByBusinessKey(tenantId, businessKey);
        if (runningLink != null) {
            return toRuntimeVO(runningLink, "当前单据已有流转中的流程");
        }

        AiBusinessBinding binding = selectFlowBindingForStart(tenantId, objectCode, requestedObjectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, tenantId, objectCode);
        String dtoFlowModelKey = StringUtils.trimToNull(dto.getFlowModelKey());
        String bindingConfigFlowModelKey = resolveFlowModelKey(bindingConfig);
        String bindingKey = binding == null ? null : StringUtils.trimToNull(binding.getBindingKey());
        String documentDefaultFlowKey = documentConfig == null ? null : StringUtils.trimToNull(documentConfig.getDefaultFlowKey());
        String flowModelKey = StringUtils.firstNonBlank(
                dtoFlowModelKey,
                bindingConfigFlowModelKey,
                bindingKey,
                documentDefaultFlowKey);
        if (StringUtils.isBlank(flowModelKey)) {
            log.warn("[低代码流程启动] 主流程解析失败: tenantId={}, objectCode={}, recordId={}, checkPermission={}, " +
                            "configKey={}, documentConfigId={}, documentEnabled={}, documentDefaultFlowKey={}, " +
                            "runtimeConfigId={}, runtimeConfigKey={}, binding={}, dtoFlowModelKey={}, " +
                            "bindingConfigFlowModelKey={}, bindingConfigPreview={}",
                    tenantId, objectCode, dto.getRecordId(), checkPermission, configKey,
                    documentConfig == null ? null : documentConfig.getId(),
                    documentConfig == null ? null : documentConfig.getDocumentEnabled(),
                    documentDefaultFlowKey,
                    runtimeConfig == null ? null : runtimeConfig.getId(),
                    runtimeConfig == null ? null : runtimeConfig.getConfigKey(),
                    describeBinding(binding), dtoFlowModelKey, bindingConfigFlowModelKey, previewBindingConfig(bindingConfig));
            throw new BusinessException("请先在流程与自动化中配置主流程");
        }
        log.info("[低代码流程启动] 主流程解析成功: tenantId={}, objectCode={}, recordId={}, configKey={}, " +
                        "flowModelKey={}, bindingId={}, bindingType={}",
                tenantId, objectCode, dto.getRecordId(), configKey, flowModelKey,
                binding == null ? null : binding.getId(), binding == null ? null : binding.getBindingType());

        Map<String, Object> flowVariables = buildFlowVariables(bindingConfig, recordData);
        if (dto.getVariables() != null) {
            flowVariables.putAll(dto.getVariables());
        }
        flowVariables.putIfAbsent("objectCode", objectCode);
        flowVariables.putIfAbsent("configKey", configKey);
        flowVariables.putIfAbsent("recordId", dto.getRecordId());
        flowVariables.putIfAbsent("businessKey", businessKey);
        String flowBusinessKey = stableBusinessKey
                ? businessKey : resolveFlowBusinessKeyForStart(tenantId, businessKey);
        flowVariables.putIfAbsent("documentBusinessKey", businessKey);
        flowVariables.putIfAbsent("recordBusinessKey", businessKey);
        flowVariables.put("flowBusinessKey", flowBusinessKey);

        String title = StringUtils.defaultIfBlank(dto.getTitle(), buildFlowTitle(bindingConfig, recordData, objectCode));
        Long userId = starterUserId != null ? starterUserId : resolveUserId();
        String userName = StringUtils.defaultIfBlank(starterUserName, resolveUsername());
        FlowResult<String> result = stableBusinessKey
                ? flowClient.startProcessForDelegatedUser(
                        flowModelKey, flowBusinessKey, objectCode, title, flowVariables)
                : flowClient.startProcess(
                        flowModelKey, flowBusinessKey, title, flowVariables,
                        userId == null ? null : String.valueOf(userId), userName, null, null);
        if (result == null || !result.isSuccess() || StringUtils.isBlank(result.getData())) {
            throw new BusinessException("流程发起失败: " + (result == null ? "无返回结果" : result.getMsg()));
        }

        AiBusinessFlowInstanceLink link = new AiBusinessFlowInstanceLink();
        link.setTenantId(tenantId);
        link.setObjectCode(objectCode);
        link.setRecordId(dto.getRecordId());
        link.setBusinessKey(businessKey);
        link.setFlowModelKey(flowModelKey);
        link.setProcessInstanceId(result.getData());
        link.setFlowStatus("RUNNING");
        link.setStartUserId(userId);
        link.setStartTime(LocalDateTime.now());
        link.setVariablesSnapshot(JSON.toJSONString(flowVariables));
        flowInstanceLinkMapper.insert(link);

        updateBusinessFlowStatus(documentConfig, runtimeConfig, bindingConfig, dto.getRecordId(), "IN_PROCESS");
        return toRuntimeVO(link, "流程已发起");
    }

    private BusinessFlowRuntimeVO executeWithFlowStartLock(Long tenantId,
                                                           String businessKey,
                                                           Supplier<BusinessFlowRuntimeVO> supplier) {
        String lockKey = buildFlowStartLockKey(tenantId, businessKey);
        FlowStartLockHandle lockHandle = acquireFlowStartLock(lockKey);
        boolean unlockInFinally = true;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                unlockInFinally = false;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        unlockFlowStartLock(lockHandle);
                    }
                });
            }
            return supplier.get();
        } finally {
            if (unlockInFinally) {
                unlockFlowStartLock(lockHandle);
            }
        }
    }

    private FlowStartLockHandle acquireFlowStartLock(String lockKey) {
        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient != null) {
            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (!lock.tryLock(FLOW_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                    throw new BusinessException("流程正在发起，请勿重复提交");
                }
                return new FlowStartLockHandle(lockKey, lock, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("流程发起锁等待被中断，请稍后重试");
            }
        }

        ReentrantLock localLock = localFlowStartLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());
        try {
            if (!localLock.tryLock(FLOW_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new BusinessException("流程正在发起，请勿重复提交");
            }
            return new FlowStartLockHandle(lockKey, null, localLock);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("流程发起锁等待被中断，请稍后重试");
        }
    }

    private void unlockFlowStartLock(FlowStartLockHandle lockHandle) {
        if (lockHandle == null) {
            return;
        }
        RLock redissonLock = lockHandle.redissonLock();
        if (redissonLock != null) {
            try {
                if (redissonLock.isHeldByCurrentThread()) {
                    redissonLock.unlock();
                }
            } catch (Exception e) {
                log.warn("[低代码流程启动] 释放流程发起分布式锁失败: lockKey={}, error={}",
                        lockHandle.lockKey(), e.getMessage());
            }
            return;
        }

        ReentrantLock localLock = lockHandle.localLock();
        if (localLock != null && localLock.isHeldByCurrentThread()) {
            localLock.unlock();
            if (!localLock.isLocked() && !localLock.hasQueuedThreads()) {
                localFlowStartLocks.remove(lockHandle.lockKey(), localLock);
            }
        }
    }

    private String buildFlowStartLockKey(Long tenantId, String businessKey) {
        return FLOW_START_LOCK_PREFIX
                + safeLockToken(tenantId) + ":"
                + safeLockToken(businessKey);
    }

    private String safeLockToken(Object value) {
        if (value == null) {
            return "null";
        }
        return String.valueOf(value).replaceAll("[^A-Za-z0-9:_-]", "_");
    }

    private void handleFlowCallbackInternal(AiBusinessFlowInstanceLink link, BusinessFlowCallbackDTO dto) {
        if (isEndedLink(link)) {
            log.info("流程回调已处理，跳过重复回调: processInstanceId={}, result={}",
                    link.getProcessInstanceId(), link.getResult());
            return;
        }
        AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByObjectCode(
                link.getTenantId(), link.getObjectCode());
        AiCrudConfig runtimeConfig = documentConfig == null
                ? resolvePublishedRuntimeConfig(link.getTenantId(), link.getObjectCode())
                : null;
        AiBusinessBinding binding = selectMainFlowBindingForConfig(link.getTenantId(), link.getObjectCode());
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, link.getTenantId(), link.getObjectCode());
        String configKey = documentConfig != null ? documentConfig.getConfigKey() : runtimeConfig == null ? null : runtimeConfig.getConfigKey();
        Map<String, Object> previousData = StringUtils.isBlank(configKey)
                ? null
                : dynamicCrudService.selectById(configKey, link.getRecordId());
        String result = normalizeCallbackResult(dto);
        updateBusinessFlowStatus(documentConfig, runtimeConfig, bindingConfig, link.getRecordId(), result);

        link.setFlowStatus(result);
        link.setResult(result);
        link.setEndTime(LocalDateTime.now());
        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            link.setVariablesSnapshot(JSON.toJSONString(dto.getVariables()));
        }
        flowInstanceLinkMapper.updateById(link);

        Map<String, Object> currentData = StringUtils.isBlank(configKey)
                ? null
                : dynamicCrudService.selectById(configKey, link.getRecordId());
        executeFlowCallbackAction(link, bindingConfig, result, dto);
        if (StringUtils.isNotBlank(configKey)) {
            currentData = dynamicCrudService.selectById(configKey, link.getRecordId());
        }
        if (documentConfig != null) {
            publishFlowResultEvent(link, documentConfig, result, previousData, currentData, dto);
        } else if (runtimeConfig != null) {
            publishFlowResultEvent(link, runtimeConfig, result, previousData, currentData, dto);
        }
    }

    private String resolveStartConfigKey(AiBusinessDocumentConfig documentConfig, AiCrudConfig runtimeConfig) {
        if (documentConfig != null) {
            if (StringUtils.isBlank(documentConfig.getConfigKey())) {
                throw new BusinessException("单据缺少发布配置，无法发起主流程");
            }
            return documentConfig.getConfigKey();
        }
        if (runtimeConfig == null || StringUtils.isBlank(runtimeConfig.getConfigKey())) {
            throw new BusinessException("业务对象缺少已发布运行配置，无法发起主流程");
        }
        return runtimeConfig.getConfigKey();
    }

    private AiBusinessFlowInstanceLink findCallbackLink(Long tenantId, BusinessFlowCallbackDTO dto) {
        if (StringUtils.isNotBlank(dto.getProcessInstanceId())) {
            AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectByProcessInstanceId(
                    tenantId, dto.getProcessInstanceId());
            if (link != null) {
                return link;
            }
        }
        if (StringUtils.isNotBlank(dto.getBusinessKey())) {
            return flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, dto.getBusinessKey());
        }
        return null;
    }

    private AiBusinessFlowInstanceLink findRuntimeLink(Long tenantId, String processInstanceId, String businessKey) {
        if (StringUtils.isNotBlank(processInstanceId)) {
            AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectByProcessInstanceId(tenantId, processInstanceId);
            if (link != null) {
                return link;
            }
        }
        if (StringUtils.isNotBlank(businessKey)) {
            return flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, businessKey);
        }
        return null;
    }

    private void updateDocumentStatus(AiBusinessDocumentConfig config, Long recordId, String statusKey) {
        if (StringUtils.isBlank(config.getStatusField())) {
            throw new BusinessException("单据状态字段未配置");
        }
        if (StringUtils.isBlank(config.getConfigKey())) {
            throw new BusinessException("单据缺少动态运行配置，无法更新状态");
        }
        String statusValue = resolveDocumentStatusValue(config, statusKey);
        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put(config.getStatusField(), statusValue);
        dynamicCrudService.updateInternalFieldsById(config.getConfigKey(), recordId, updateData);
    }

    private void updateBusinessFlowStatus(AiBusinessDocumentConfig documentConfig,
                                          AiCrudConfig runtimeConfig,
                                          JSONObject bindingConfig,
                                          Long recordId,
                                          String statusKey) {
        if (documentConfig != null) {
            updateDocumentStatus(documentConfig, recordId, statusKey);
            return;
        }
        BusinessFlowBindingDTO.BusinessBindingDTO businessBinding = toBusinessBindingDTO(
                bindingConfig == null ? null : bindingConfig.getJSONObject("businessBinding"));
        if (businessBinding == null || StringUtils.isBlank(businessBinding.getStatusField())) {
            return;
        }
        String mode = normalizeBusinessBindingMode(businessBinding.getMode());
        if ("ADAPTER".equals(mode)) {
            log.debug("[低代码流程状态] Adapter 模式跳过平台直接回写: recordId={}, status={}", recordId, statusKey);
            return;
        }
        if (runtimeConfig == null || StringUtils.isBlank(runtimeConfig.getConfigKey())) {
            throw new BusinessException("业务表绑定缺少低代码运行配置，无法更新流程状态");
        }
        validateBusinessBindingRuntimeTable(businessBinding, runtimeConfig);
        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put(businessBinding.getStatusField(), resolveBusinessBindingStatusValue(bindingConfig, statusKey));
        dynamicCrudService.updateInternalFieldsById(runtimeConfig.getConfigKey(), recordId, updateData);
    }

    private String resolveBusinessBindingStatusValue(JSONObject bindingConfig, String statusKey) {
        JSONObject document = bindingConfig == null ? null : bindingConfig.getJSONObject("document");
        JSONObject statusMapping = document == null ? null : document.getJSONObject("statusMapping");
        if (statusMapping != null) {
            return StringUtils.defaultIfBlank(statusMapping.getString(statusKey), statusKey);
        }
        return statusKey;
    }

    private void executeFlowCallbackAction(AiBusinessFlowInstanceLink link,
                                           JSONObject bindingConfig,
                                           String result,
                                           BusinessFlowCallbackDTO dto) {
        String actionCode = resolveFlowCallbackActionCode(bindingConfig, result);
        if (StringUtils.isBlank(actionCode)) {
            return;
        }
        BusinessActionExecutionService actionExecutionService = actionExecutionServiceProvider.getIfAvailable();
        if (actionExecutionService == null) {
            throw new BusinessException("动作执行服务未启用，无法执行流程回调动作");
        }
        BusinessActionExecuteDTO request = new BusinessActionExecuteDTO();
        request.setObjectCode(link.getObjectCode());
        request.setRecordId(link.getRecordId() == null ? null : String.valueOf(link.getRecordId()));
        request.setActionCode(actionCode);
        request.setIdempotencyKey(buildFlowCallbackActionIdempotencyKey(link, result, actionCode));
        request.setContext(buildFlowCallbackActionContext(link, result, dto));
        try {
            actionExecutionService.execute(request);
        } catch (BusinessException e) {
            log.warn("[低代码流程回调] 动作执行失败: objectCode={}, recordId={}, result={}, actionCode={}, error={}",
                    link.getObjectCode(), link.getRecordId(), result, actionCode, e.getMessage());
            throw new BusinessException("流程回调动作执行失败: " + e.getMessage());
        }
    }

    private String resolveFlowCallbackActionCode(JSONObject bindingConfig, String result) {
        if (bindingConfig == null || StringUtils.isBlank(result)) {
            return null;
        }
        JSONObject options = bindingConfig.getJSONObject("options");
        JSONObject callbackActions = options == null ? null : options.getJSONObject("callbackActions");
        if (callbackActions == null || callbackActions.isEmpty()) {
            callbackActions = bindingConfig.getJSONObject("callbackActions");
        }
        if (callbackActions == null || callbackActions.isEmpty()) {
            return null;
        }
        String normalizedResult = StringUtils.defaultString(result).toUpperCase();
        return StringUtils.firstNonBlank(
                callbackActions.getString(normalizedResult),
                callbackActions.getString(normalizedResult.toLowerCase()),
                switch (normalizedResult) {
                    case "APPROVED" -> callbackActions.getString("approvedActionCode");
                    case "REJECTED" -> callbackActions.getString("rejectedActionCode");
                    case "CANCELED" -> callbackActions.getString("canceledActionCode");
                    default -> null;
                }
        );
    }

    private String buildFlowCallbackActionIdempotencyKey(AiBusinessFlowInstanceLink link, String result, String actionCode) {
        return "flowCallback:"
                + StringUtils.defaultString(link.getProcessInstanceId(), link.getBusinessKey())
                + ":" + StringUtils.defaultString(result)
                + ":" + StringUtils.defaultString(actionCode);
    }

    private Map<String, Object> buildFlowCallbackActionContext(AiBusinessFlowInstanceLink link,
                                                               String result,
                                                               BusinessFlowCallbackDTO dto) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("source", "FLOW_CALLBACK");
        context.put("flowResult", result);
        context.put("processInstanceId", link.getProcessInstanceId());
        context.put("businessKey", link.getBusinessKey());
        context.put("flowModelKey", link.getFlowModelKey());
        context.put("operatorId", dto.getOperatorId() != null ? dto.getOperatorId() : link.getStartUserId());
        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            context.put("variables", dto.getVariables());
        }
        return context;
    }

    private void validateBusinessBindingRuntimeTable(BusinessFlowBindingDTO.BusinessBindingDTO businessBinding,
                                                     AiCrudConfig runtimeConfig) {
        String bindingTable = StringUtils.trimToNull(businessBinding.getTableName());
        if (bindingTable == null) {
            return;
        }
        String runtimeTable = StringUtils.firstNonBlank(runtimeConfig.getRuntimeTableName(), runtimeConfig.getTableName());
        if (StringUtils.isNotBlank(runtimeTable) && !bindingTable.equalsIgnoreCase(runtimeTable)) {
            throw new BusinessException("业务表绑定与发布运行表不一致，禁止直接回写状态");
        }
    }

    private String resolveDocumentStatusValue(AiBusinessDocumentConfig config, String statusKey) {
        Map<String, String> statusMapping = documentConfigService.toVO(config).getStatusMapping();
        return StringUtils.defaultIfBlank(statusMapping.get(statusKey), statusKey);
    }

    private void publishFlowResultEvent(AiBusinessFlowInstanceLink link,
                                        AiBusinessDocumentConfig config,
                                        String result,
                                        Map<String, Object> previousData,
                                        Map<String, Object> currentData,
                                        BusinessFlowCallbackDTO dto) {
        String eventType = switch (result) {
            case "APPROVED" -> BusinessEvent.FLOW_APPROVED;
            case "REJECTED" -> BusinessEvent.FLOW_REJECTED;
            case "CANCELED" -> BusinessEvent.FLOW_CANCELED;
            default -> null;
        };
        if (eventType == null) {
            return;
        }
        BusinessEvent event = BusinessEvent.builder()
                .eventType(eventType)
                .suiteCode(config.getSuiteCode())
                .objectCode(link.getObjectCode())
                .configKey(config.getConfigKey())
                .recordId(String.valueOf(link.getRecordId()))
                .recordData(currentData)
                .previousData(previousData)
                .operatorId(dto.getOperatorId() != null ? dto.getOperatorId() : link.getStartUserId())
                .operatorName(resolveUsername())
                .tenantId(link.getTenantId())
                .build();
        applicationEventPublisher.publishEvent(event);
    }

    private void publishFlowResultEvent(AiBusinessFlowInstanceLink link,
                                        AiCrudConfig config,
                                        String result,
                                        Map<String, Object> previousData,
                                        Map<String, Object> currentData,
                                        BusinessFlowCallbackDTO dto) {
        String eventType = switch (result) {
            case "APPROVED" -> BusinessEvent.FLOW_APPROVED;
            case "REJECTED" -> BusinessEvent.FLOW_REJECTED;
            case "CANCELED" -> BusinessEvent.FLOW_CANCELED;
            default -> null;
        };
        if (eventType == null) {
            return;
        }
        BusinessEvent event = BusinessEvent.builder()
                .eventType(eventType)
                .objectCode(link.getObjectCode())
                .configKey(config.getConfigKey())
                .recordId(String.valueOf(link.getRecordId()))
                .recordData(currentData)
                .previousData(previousData)
                .operatorId(dto.getOperatorId() != null ? dto.getOperatorId() : link.getStartUserId())
                .operatorName(resolveUsername())
                .tenantId(link.getTenantId())
                .build();
        applicationEventPublisher.publishEvent(event);
    }

    private AiCrudConfig resolvePublishedRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        if (StringUtils.isBlank(objectCodeOrConfigKey)) {
            return null;
        }
        return crudConfigMapper.selectPublishedByObjectCodeOrConfigKey(
                tenantId != null ? tenantId : resolveTenantId(), objectCodeOrConfigKey);
    }

    private String normalizeCallbackResult(BusinessFlowCallbackDTO dto) {
        String value = StringUtils.firstNonBlank(dto.getResult(), dto.getFlowStatus());
        if (StringUtils.isBlank(value)) {
            throw new BusinessException("流程回调缺少结果状态");
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("COMPLETED") || normalized.contains("APPROVED") || "APPROVE".equals(normalized)) {
            return "APPROVED";
        }
        if (normalized.contains("REJECT")) {
            return "REJECTED";
        }
        if (normalized.contains("CANCEL") || normalized.contains("WITHDRAW") || normalized.contains("TERMINAT")) {
            return "CANCELED";
        }
        throw new BusinessException("不支持的流程回调结果: " + value);
    }

    private boolean isEndedLink(AiBusinessFlowInstanceLink link) {
        return link.getEndTime() != null
                || "APPROVED".equalsIgnoreCase(link.getResult())
                || "REJECTED".equalsIgnoreCase(link.getResult())
                || "CANCELED".equalsIgnoreCase(link.getResult());
    }

    private AiBusinessBinding selectFlowBindingForStart(Long tenantId, String objectCode, String... fallbackCodes) {
        BindingLookupResult result = selectMainFlowBinding(tenantId, objectCode, fallbackCodes);
        if (isBindingEnabled(result.binding())) {
            if ("APPROVAL".equalsIgnoreCase(result.binding().getBindingType())) {
                log.info("[低代码流程启动] 使用历史审批绑定作为主流程: tenantId={}, objectCode={}, binding={}",
                        tenantId, result.matchedObjectCode(), describeBinding(result.binding()));
            }
            return result.binding();
        }
        if (result.binding() != null) {
            log.warn("[低代码流程启动] 未找到启用的主流程绑定: tenantId={}, objectCodes={}, binding={}",
                    tenantId, result.candidates(), describeBinding(result.binding()));
        } else {
            log.warn("[低代码流程启动] 未找到主流程绑定记录: tenantId={}, objectCodes={}", tenantId, result.candidates());
        }
        return null;
    }

    private AiBusinessBinding selectMainFlowBindingForConfig(Long tenantId, String objectCode, String... fallbackCodes) {
        return selectMainFlowBinding(tenantId, objectCode, fallbackCodes).binding();
    }

    private BindingLookupResult selectMainFlowBinding(Long tenantId, String objectCode, String... fallbackCodes) {
        List<String> candidates = objectCodeCandidates(objectCode, fallbackCodes);
        AiBusinessBinding firstDisabled = null;
        String disabledObjectCode = null;
        for (String candidate : candidates) {
            AiBusinessBinding binding = bindingMapper.selectBindingByTypeAndCode(tenantId, "OBJECT", candidate, "FLOW");
            if (isBindingEnabled(binding)) {
                return new BindingLookupResult(binding, candidate, candidates);
            }
            if (firstDisabled == null && binding != null) {
                firstDisabled = binding;
                disabledObjectCode = candidate;
            }
            AiBusinessBinding legacyApprovalBinding = bindingMapper.selectBindingByTypeAndCode(
                    tenantId, "OBJECT", candidate, "APPROVAL");
            if (isBindingEnabled(legacyApprovalBinding)) {
                return new BindingLookupResult(legacyApprovalBinding, candidate, candidates);
            }
            if (firstDisabled == null && legacyApprovalBinding != null) {
                firstDisabled = legacyApprovalBinding;
                disabledObjectCode = candidate;
            }
        }
        return new BindingLookupResult(firstDisabled, disabledObjectCode, candidates);
    }

    private List<String> objectCodeCandidates(String objectCode, String... fallbackCodes) {
        Set<String> candidates = new LinkedHashSet<>();
        addObjectCodeCandidate(candidates, objectCode);
        if (fallbackCodes != null) {
            for (String fallbackCode : fallbackCodes) {
                addObjectCodeCandidate(candidates, fallbackCode);
            }
        }
        return new ArrayList<>(candidates);
    }

    private void addObjectCodeCandidate(Set<String> candidates, String objectCode) {
        String normalized = StringUtils.trimToNull(objectCode);
        if (StringUtils.isNotBlank(normalized)) {
            candidates.add(normalized);
        }
    }

    private FlowStartContext resolveFlowStartContext(Long tenantId, String objectCodeOrConfigKey) {
        BusinessRuntimeContext context = resolveBusinessRuntimeContext(tenantId, objectCodeOrConfigKey);
        String configKey = resolveStartConfigKey(context.documentConfig(), context.runtimeConfig());
        return new FlowStartContext(
                context.requestedObjectCode(),
                context.objectCode(),
                configKey,
                context.documentConfig(),
                context.runtimeConfig());
    }

    private String resolveCanonicalObjectCode(Long tenantId, String objectCodeOrConfigKey) {
        if (StringUtils.isBlank(objectCodeOrConfigKey)) {
            return objectCodeOrConfigKey;
        }
        BusinessRuntimeContext context = resolveBusinessRuntimeContext(tenantId, objectCodeOrConfigKey);
        return StringUtils.firstNonBlank(context.objectCode(), StringUtils.trimToNull(objectCodeOrConfigKey));
    }

    private BusinessRuntimeContext resolveBusinessRuntimeContext(Long tenantId, String objectCodeOrConfigKey) {
        String requestedObjectCode = StringUtils.trimToNull(objectCodeOrConfigKey);
        if (requestedObjectCode == null) {
            return new BusinessRuntimeContext(null, null, null, null, null, null);
        }
        AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, requestedObjectCode);
        AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, requestedObjectCode, runtimeConfig);
        AiBusinessObject businessObject = resolveBusinessObject(tenantId, requestedObjectCode, runtimeConfig, documentConfig);
        String canonicalObjectCode = StringUtils.firstNonBlank(
                documentConfig == null ? null : documentConfig.getObjectCode(),
                businessObject == null ? null : businessObject.getObjectCode(),
                runtimeConfig == null ? null : runtimeConfig.getObjectCode(),
                requestedObjectCode);

        if (documentConfig == null && !StringUtils.equals(canonicalObjectCode, requestedObjectCode)) {
            documentConfig = resolveEnabledDocumentConfig(tenantId, canonicalObjectCode, runtimeConfig);
        }
        if (runtimeConfig == null) {
            runtimeConfig = resolvePublishedRuntimeConfig(tenantId, StringUtils.firstNonBlank(
                    documentConfig == null ? null : documentConfig.getConfigKey(),
                    businessObject == null ? null : businessObject.getConfigKey(),
                    canonicalObjectCode));
        }
        if (businessObject == null && !StringUtils.equals(canonicalObjectCode, requestedObjectCode)) {
            businessObject = resolveBusinessObject(tenantId, canonicalObjectCode, runtimeConfig, documentConfig);
        }
        String configKey = StringUtils.firstNonBlank(
                documentConfig == null ? null : documentConfig.getConfigKey(),
                runtimeConfig == null ? null : runtimeConfig.getConfigKey(),
                businessObject == null ? null : businessObject.getConfigKey());
        return new BusinessRuntimeContext(
                requestedObjectCode,
                canonicalObjectCode,
                configKey,
                documentConfig,
                runtimeConfig,
                businessObject);
    }

    private AiBusinessDocumentConfig resolveEnabledDocumentConfig(Long tenantId, String objectCodeOrConfigKey,
                                                                  AiCrudConfig runtimeConfig) {
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        AiBusinessDocumentConfig config = documentConfigService.selectEnabledByObjectCode(effectiveTenantId, objectCodeOrConfigKey);
        if (config != null) {
            return config;
        }
        config = documentConfigService.selectEnabledByConfigKey(effectiveTenantId, objectCodeOrConfigKey);
        if (config != null || runtimeConfig == null) {
            return config;
        }
        config = documentConfigService.selectEnabledByConfigKey(effectiveTenantId, runtimeConfig.getConfigKey());
        if (config != null) {
            return config;
        }
        return documentConfigService.selectEnabledByObjectCode(effectiveTenantId, runtimeConfig.getObjectCode());
    }

    private AiBusinessObject resolveBusinessObject(Long tenantId, String objectCodeOrConfigKey,
                                                   AiCrudConfig runtimeConfig,
                                                   AiBusinessDocumentConfig documentConfig) {
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        AiBusinessObject object = null;
        if (documentConfig != null && StringUtils.isNotBlank(documentConfig.getConfigKey())) {
            object = businessObjectMapper.selectByConfigKey(effectiveTenantId, documentConfig.getConfigKey());
        }
        if (object == null && runtimeConfig != null && StringUtils.isNotBlank(runtimeConfig.getConfigKey())) {
            object = businessObjectMapper.selectByConfigKey(effectiveTenantId, runtimeConfig.getConfigKey());
        }
        if (object == null && StringUtils.isNotBlank(objectCodeOrConfigKey)) {
            object = businessObjectMapper.selectByConfigKey(effectiveTenantId, objectCodeOrConfigKey);
        }
        return object;
    }

    private boolean isBindingEnabled(AiBusinessBinding binding) {
        return binding != null && !Integer.valueOf(0).equals(binding.getStatus());
    }

    private String describeBinding(AiBusinessBinding binding) {
        if (binding == null) {
            return "null";
        }
        return "id=" + binding.getId()
                + ", type=" + binding.getBindingType()
                + ", status=" + binding.getStatus()
                + ", targetCode=" + binding.getTargetCode()
                + ", bindingKey=" + binding.getBindingKey()
                + ", bindingName=" + binding.getBindingName()
                + ", configBlank=" + StringUtils.isBlank(binding.getBindingConfig());
    }

    private String previewBindingConfig(JSONObject bindingConfig) {
        if (bindingConfig == null || bindingConfig.isEmpty()) {
            return "{}";
        }
        return StringUtils.left(bindingConfig.toJSONString(), 400);
    }

    private BusinessFlowRuntimeVO toRuntimeVO(AiBusinessFlowInstanceLink link, String message) {
        BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
        vo.setLinkId(link.getId());
        vo.setObjectCode(link.getObjectCode());
        vo.setRecordId(link.getRecordId());
        vo.setBusinessKey(link.getBusinessKey());
        vo.setFlowModelKey(link.getFlowModelKey());
        vo.setProcessInstanceId(link.getProcessInstanceId());
        vo.setFlowStatus(link.getFlowStatus());
        vo.setResult(link.getResult());
        vo.setStartTime(link.getStartTime());
        vo.setEndTime(link.getEndTime());
        vo.setMessage(message);
        return vo;
    }

    private String buildBusinessKey(String objectCode, Long recordId) {
        return objectCode + ":" + recordId;
    }

    private BusinessKeyParts parseBusinessKey(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            throw new BusinessException("业务Key格式错误，应为 objectCode:recordId");
        }
        String[] parts = businessKey.split(":", 2);
        if (StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
            throw new BusinessException("业务Key格式错误，应为 objectCode:recordId");
        }
        try {
            return new BusinessKeyParts(parts[0], Long.valueOf(parts[1]));
        } catch (NumberFormatException e) {
            throw new BusinessException("业务Key中的记录ID必须是数字");
        }
    }

    /**
     * 查询流程状态
     */
    public JSONObject getFlowStatus(String businessKey) {
        try {
            FlowResult<Map<String, Object>> result = flowClient.getProcessStatus(businessKey);
            if (result.isSuccess()) {
                JSONObject status = new JSONObject();
                status.put("businessKey", businessKey);
                status.put("data", result.getData());
                return status;
            }
        } catch (Exception e) {
            log.debug("查询流程状态失败: businessKey={}", businessKey);
        }
        return null;
    }

    /**
     * 构建流程变量
     */
    private Map<String, Object> buildFlowVariables(JSONObject bindingConfig, Map<String, Object> recordData) {
        Map<String, Object> variables = new HashMap<>();
        JSONArray variableMapping = bindingConfig.getJSONArray("variableMapping");

        if (recordData != null) {
            for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                putBusinessFieldVariable(variables, entry.getKey(), entry.getValue());
            }
        }

        if (variableMapping != null && recordData != null) {
            for (int i = 0; i < variableMapping.size(); i++) {
                JSONObject mapping = variableMapping.getJSONObject(i);
                String formField = StringUtils.defaultIfBlank(mapping.getString("formField"), mapping.getString("field"));
                String flowVariable = StringUtils.defaultIfBlank(mapping.getString("flowVariable"), mapping.getString("variable"));
                Object value = readRecordValue(recordData, formField);
                if (value != null && StringUtils.isNotBlank(flowVariable)) {
                    variables.put(flowVariable, value);
                }
            }
        }

        return variables;
    }

    private void putBusinessFieldVariable(Map<String, Object> variables, String field, Object value) {
        String key = StringUtils.trimToNull(field);
        if (key == null || value == null || value instanceof Map<?, ?> || value instanceof Iterable<?>) {
            return;
        }
        variables.putIfAbsent(key, value);
        variables.putIfAbsent(snakeToCamel(key), value);
        variables.putIfAbsent(camelToSnake(key), value);
    }

    /**
     * 构建流程标题
     */
    private String buildFlowTitle(JSONObject bindingConfig, Map<String, Object> recordData, String objectCode) {
        String titleTemplate = bindingConfig.getString("titleTemplate");
        if (titleTemplate != null && !titleTemplate.isBlank() && recordData != null) {
            // 兼容历史 ${fieldName} 与 Flyway 友好的 {fieldName} 模板。
            for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                titleTemplate = replaceTemplateValue(titleTemplate, entry.getKey(), entry.getValue());
            }
            return titleTemplate;
        }
        return objectCode + " 审批申请";
    }

    private String resolveBusinessSummary(BusinessObjectVO object,
                                          TaskFormRuntimeContext runtime,
                                          Map<String, Object> recordData) {
        if (recordData == null || recordData.isEmpty()) {
            return null;
        }
        JSONObject objectOptions = object == null ? new JSONObject() : readJsonObject(object.getOptions());
        JSONObject designerOptions = object == null ? new JSONObject() : readJsonObject(object.getDesignerOptions());
        String template = StringUtils.firstNonBlank(
                StringUtils.trimToNull(objectOptions.getString("summaryExpression")),
                StringUtils.trimToNull(designerOptions.getString("summaryExpression")),
                runtime.bindingConfig() == null ? null : StringUtils.trimToNull(runtime.bindingConfig().getString("titleTemplate")));
        if (StringUtils.isNotBlank(template)) {
            String resolved = template;
            for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                resolved = replaceTemplateValue(resolved, entry.getKey(), entry.getValue());
            }
            resolved = StringUtils.trimToNull(resolved);
            if (resolved != null) {
                return resolved;
            }
        }
        String displayField = object == null ? null : StringUtils.trimToNull(object.getDisplayField());
        Object displayValue = displayField == null ? null : readRecordValue(recordData, displayField);
        if (displayValue != null && StringUtils.isNotBlank(String.valueOf(displayValue))) {
            return String.valueOf(displayValue);
        }
        for (String field : List.of("orderNo", "businessNo", "title", "name", "code")) {
            Object value = readRecordValue(recordData, field);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private String replaceTemplateValue(String template, String key, Object value) {
        if (StringUtils.isBlank(template) || StringUtils.isBlank(key)) {
            return template;
        }
        String text = value != null ? String.valueOf(value) : "";
        String result = replaceTemplateToken(template, key, text);
        result = replaceTemplateToken(result, snakeToCamel(key), text);
        return replaceTemplateToken(result, camelToSnake(key), text);
    }

    private String replaceTemplateToken(String template, String key, String value) {
        if (StringUtils.isBlank(key)) {
            return template;
        }
        return template.replace("${" + key + "}", value)
                .replace("{" + key + "}", value);
    }

    private BusinessFlowBindingVO toVO(String objectCode, AiBusinessBinding binding) {
        JSONObject config = readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(config, binding.getTenantId(), objectCode);
        BusinessFlowBindingVO vo = new BusinessFlowBindingVO();
        vo.setBindingId(binding.getId());
        vo.setObjectCode(objectCode);
        vo.setFlowModelKey(StringUtils.defaultIfBlank(resolveFlowModelKey(config), binding.getBindingKey()));
        vo.setFlowModelName(StringUtils.defaultIfBlank(config.getString("flowModelName"), binding.getBindingName()));
        vo.setTitleTemplate(config.getString("titleTemplate"));
        vo.setStartMode(normalizeStartMode(config.getString("startMode")));
        vo.setBusinessBinding(toBusinessBindingDTO(config.getJSONObject("businessBinding")));
        vo.setVariableMapping(normalizeVariableMapping(config.getJSONArray("variableMapping")));
        vo.setNodeForms(normalizeNodeForms(readMapList(config.getJSONArray("nodeForms"))));
        vo.setConditionFlows(readMapList(config.getJSONArray("conditionFlows")));
        vo.setOptions(readOptions(config.getJSONObject("options")));
        vo.setStatus(binding.getStatus());
        enrichBindingSummary(vo, "AI_BUSINESS_BINDING");
        return vo;
    }

    private BusinessFlowBindingVO legacyDocumentFlowToVO(String objectCode, AiBusinessDocumentConfig documentConfig) {
        BusinessFlowBindingVO vo = new BusinessFlowBindingVO();
        vo.setObjectCode(objectCode);
        vo.setFlowModelKey(documentConfig.getDefaultFlowKey());
        vo.setFlowModelName(documentConfig.getDefaultFlowKey());
        vo.setStartMode("MANUAL");
        vo.setBusinessBinding(defaultBusinessBinding(null, documentConfig));
        vo.setStatus(1);
        vo.setCompatibilitySource("DOCUMENT_DEFAULT_FLOW");
        vo.setComplete(false);
        vo.setGaps(List.of("历史默认流程缺少变量映射，请在流程与自动化中保存一次主流程"));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("configured", true);
        summary.put("flowModelKey", documentConfig.getDefaultFlowKey());
        summary.put("flowModelName", documentConfig.getDefaultFlowKey());
        summary.put("startMode", "MANUAL");
        summary.put("businessBinding", vo.getBusinessBinding());
        summary.put("variableMappingCount", 0);
        summary.put("complete", false);
        summary.put("gaps", vo.getGaps());
        summary.put("compatibilitySource", "DOCUMENT_DEFAULT_FLOW");
        vo.setMainFlowSummary(summary);
        return vo;
    }

    private void enrichBindingSummary(BusinessFlowBindingVO vo, String compatibilitySource) {
        List<String> gaps = new ArrayList<>();
        if (StringUtils.isBlank(vo.getFlowModelKey())) {
            gaps.add("未配置主流程");
        }
        if (StringUtils.isBlank(vo.getStartMode())) {
            gaps.add("发起方式未配置");
        }
        if (vo.getVariableMapping() == null || vo.getVariableMapping().isEmpty()) {
            gaps.add("变量映射缺失");
        }
        boolean complete = gaps.isEmpty();
        vo.setComplete(complete);
        vo.setGaps(gaps);
        vo.setCompatibilitySource(compatibilitySource);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("configured", StringUtils.isNotBlank(vo.getFlowModelKey()));
        summary.put("bindingId", vo.getBindingId());
        summary.put("flowModelKey", vo.getFlowModelKey());
        summary.put("flowModelName", vo.getFlowModelName());
        summary.put("startMode", vo.getStartMode());
        summary.put("businessBinding", vo.getBusinessBinding());
        summary.put("variableMappingCount", vo.getVariableMapping() == null ? 0 : vo.getVariableMapping().size());
        summary.put("complete", complete);
        summary.put("gaps", gaps);
        summary.put("compatibilitySource", compatibilitySource);
        vo.setMainFlowSummary(summary);
    }

    private BusinessFlowBindingDTO toDTO(JSONObject config) {
        JSONObject source = config == null ? new JSONObject() : config;
        BusinessFlowBindingDTO dto = new BusinessFlowBindingDTO();
        dto.setFlowModelKey(resolveFlowModelKey(source));
        dto.setFlowModelName(source.getString("flowModelName"));
        dto.setTitleTemplate(source.getString("titleTemplate"));
        dto.setStartMode(normalizeStartMode(source.getString("startMode")));
        dto.setBusinessBinding(toBusinessBindingDTO(source.getJSONObject("businessBinding")));
        dto.setVariableMapping(normalizeVariableMapping(source.getJSONArray("variableMapping")));
        dto.setNodeForms(normalizeNodeForms(readMapList(source.getJSONArray("nodeForms"))));
        dto.setConditionFlows(readMapList(source.getJSONArray("conditionFlows")));
        dto.setOptions(readOptions(source.getJSONObject("options")));
        return dto;
    }

    private JSONObject normalizeBindingConfig(BusinessFlowBindingDTO dto) {
        JSONObject config = new JSONObject();
        config.put("flowModelKey", StringUtils.trimToNull(dto.getFlowModelKey()));
        config.put("flowModelName", StringUtils.trimToNull(dto.getFlowModelName()));
        config.put("titleTemplate", StringUtils.trimToNull(dto.getTitleTemplate()));
        config.put("startMode", normalizeStartMode(dto.getStartMode()));
        JSONObject businessBinding = normalizeBusinessBinding(dto.getBusinessBinding());
        if (!businessBinding.isEmpty()) {
            config.put("businessBinding", businessBinding);
        }
        JSONArray variableMapping = new JSONArray();
        if (dto.getVariableMapping() != null) {
            for (BusinessFlowBindingDTO.VariableMappingDTO item : dto.getVariableMapping()) {
                if (item == null || StringUtils.isBlank(item.getFormField()) || StringUtils.isBlank(item.getFlowVariable())) {
                    continue;
                }
                JSONObject mapping = new JSONObject();
                mapping.put("formField", item.getFormField().trim());
                mapping.put("flowVariable", item.getFlowVariable().trim());
                mapping.put("label", StringUtils.trimToNull(item.getLabel()));
                variableMapping.add(mapping);
            }
        }
        config.put("variableMapping", variableMapping);
        config.put("nodeForms", normalizeNodeForms(dto.getNodeForms()));
        config.put("conditionFlows", dto.getConditionFlows() == null ? new ArrayList<>() : dto.getConditionFlows());
        config.put("options", dto.getOptions() == null ? new LinkedHashMap<>() : dto.getOptions());
        return config;
    }

    private void ensureBusinessBinding(JSONObject config, Long tenantId, String objectCode) {
        if (config == null) {
            return;
        }
        AiCrudConfig runtimeConfig = resolvePublishedRuntimeConfig(tenantId, objectCode);
        AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, objectCode, runtimeConfig);
        JSONObject defaults = normalizeBusinessBinding(defaultBusinessBinding(runtimeConfig, documentConfig));
        JSONObject current = config.getJSONObject("businessBinding");
        if (current == null || current.isEmpty()) {
            if (!defaults.isEmpty()) {
                config.put("businessBinding", defaults);
            }
            return;
        }
        mergeBusinessBindingDefaults(current, defaults);
        config.put("businessBinding", current);
    }

    private BusinessFlowBindingDTO.BusinessBindingDTO defaultBusinessBinding(AiCrudConfig runtimeConfig,
                                                                            AiBusinessDocumentConfig documentConfig) {
        BusinessFlowBindingDTO.BusinessBindingDTO binding = new BusinessFlowBindingDTO.BusinessBindingDTO();
        binding.setMode("LOWCODE_OBJECT");
        if (runtimeConfig != null) {
            binding.setTableName(StringUtils.firstNonBlank(runtimeConfig.getRuntimeTableName(), runtimeConfig.getTableName()));
            binding.setPrimaryKeyField(StringUtils.firstNonBlank(
                    runtimeConfig.getPrimaryKeyField(),
                    runtimeConfig.getPrimaryKeyColumn(),
                    "id"));
        } else {
            binding.setPrimaryKeyField("id");
        }
        binding.setTenantField("tenant_id");
        if (documentConfig != null) {
            binding.setStatusField(StringUtils.trimToNull(documentConfig.getStatusField()));
            binding.setOwnerField(StringUtils.trimToNull(documentConfig.getOwnerField()));
        }
        return binding;
    }

    private JSONObject normalizeBusinessBinding(BusinessFlowBindingDTO.BusinessBindingDTO binding) {
        JSONObject result = new JSONObject();
        if (binding == null) {
            return result;
        }
        putText(result, "mode", normalizeBusinessBindingMode(binding.getMode()));
        putText(result, "tableName", binding.getTableName());
        putText(result, "primaryKeyField", binding.getPrimaryKeyField());
        putText(result, "tenantField", binding.getTenantField());
        putText(result, "statusField", binding.getStatusField());
        putText(result, "titleField", binding.getTitleField());
        putText(result, "ownerField", binding.getOwnerField());
        return result;
    }

    private BusinessFlowBindingDTO.BusinessBindingDTO toBusinessBindingDTO(JSONObject source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        BusinessFlowBindingDTO.BusinessBindingDTO binding = new BusinessFlowBindingDTO.BusinessBindingDTO();
        binding.setMode(normalizeBusinessBindingMode(source.getString("mode")));
        binding.setTableName(StringUtils.trimToNull(source.getString("tableName")));
        binding.setPrimaryKeyField(StringUtils.trimToNull(source.getString("primaryKeyField")));
        binding.setTenantField(StringUtils.trimToNull(source.getString("tenantField")));
        binding.setStatusField(StringUtils.trimToNull(source.getString("statusField")));
        binding.setTitleField(StringUtils.trimToNull(source.getString("titleField")));
        binding.setOwnerField(StringUtils.trimToNull(source.getString("ownerField")));
        return binding;
    }

    private void mergeBusinessBindingDefaults(JSONObject current, JSONObject defaults) {
        if (current == null || defaults == null || defaults.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.isBlank(current.getString(key)) && entry.getValue() != null) {
                current.put(key, entry.getValue());
            }
        }
    }

    private String normalizeBusinessBindingMode(String mode) {
        String normalized = StringUtils.defaultIfBlank(mode, "LOWCODE_OBJECT").trim().toUpperCase();
        if ("BUSINESS_TABLE".equals(normalized) || "ADAPTER".equals(normalized)) {
            return normalized;
        }
        return "LOWCODE_OBJECT";
    }

    private void putText(JSONObject target, String key, String value) {
        String text = StringUtils.trimToNull(value);
        if (text != null) {
            target.put(key, text);
        }
    }

    private void putBoolean(JSONObject target, Map<String, Object> source, String key) {
        if (target == null || source == null || !source.containsKey(key)) {
            return;
        }
        Boolean value = readNullableBooleanValue(source.get(key));
        if (value != null) {
            target.put(key, value);
        }
    }

    private List<Map<String, Object>> collectBusinessFormAssets(BusinessObjectVO object, JSONObject formSchema) {
        if (object == null || formSchema == null || formSchema.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        appendBusinessFormAsset(result, seen, object, formSchema, "default");

        JSONArray forms = readNestedArray(formSchema.get("forms"));
        for (int i = 0; i < forms.size(); i++) {
            JSONObject form = forms.getJSONObject(i);
            JSONObject schema = readNestedObject(form.get("schema"));
            appendBusinessFormAsset(result, seen, object, schema.isEmpty() ? form : schema, "form");
        }

        JSONObject settings = readNestedObject(formSchema.get("settings"));
        JSONArray formAssets = readNestedArray(settings.get("formAssets"));
        for (int i = 0; i < formAssets.size(); i++) {
            JSONObject asset = formAssets.getJSONObject(i);
            JSONObject schema = readNestedObject(asset.get("schema"));
            appendBusinessFormAsset(result, seen, object, schema.isEmpty() ? asset : schema, "asset");
        }
        return result;
    }

    private List<Map<String, Object>> collectRuntimeCrudFormAssets(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        if (runtimeConfig == null) {
            return List.of();
        }
        List<Map<String, Object>> designerAssets = collectRuntimeDesignerFormAssets(
                object, runtimeConfig, readRuntimeCrudFormDesignerSchema(runtimeConfig));
        if (!designerAssets.isEmpty()) {
            return designerAssets;
        }
        List<Map<String, Object>> fieldCatalog = collectRuntimeCrudFormFieldCatalog(runtimeConfig);
        if (fieldCatalog.isEmpty()) {
            return List.of();
        }
        String formKey = resolveRuntimeCrudFormKey(object, runtimeConfig);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("objectCode", resolveRuntimeCrudObjectCode(object, runtimeConfig));
        item.put("objectName", resolveRuntimeCrudObjectName(object, runtimeConfig));
        item.put("configKey", runtimeConfig.getConfigKey());
        item.put("formKey", formKey);
        item.put("formName", resolveRuntimeCrudFormName(object, runtimeConfig));
        item.put("viewKey", "default");
        item.put("source", "runtimeCrud");
        item.put("sourceType", "businessObjectRuntime");
        item.put("fieldCatalog", fieldCatalog);
        item.put("fields", fieldCatalog);
        item.put("fieldCount", fieldCatalog.size());
        item.put("fieldPreview", buildFieldPreview(fieldCatalog));
        item.put("supportsSave", true);
        return List.of(item);
    }

    private List<Map<String, Object>> collectRuntimeDesignerFormAssets(BusinessObjectVO object,
                                                                       AiCrudConfig runtimeConfig,
                                                                       JSONObject formSchema) {
        if (runtimeConfig == null || formSchema == null || formSchema.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        appendRuntimeDesignerFormAsset(result, seen, object, runtimeConfig, formSchema, "runtimeDesignerDefault");

        JSONArray forms = readNestedArray(formSchema.get("forms"));
        for (int i = 0; i < forms.size(); i++) {
            JSONObject form = forms.getJSONObject(i);
            JSONObject schema = readNestedObject(form.get("schema"));
            appendRuntimeDesignerFormAsset(result, seen, object, runtimeConfig, schema.isEmpty() ? form : schema,
                    "runtimeDesignerForm");
        }

        JSONObject settings = readNestedObject(formSchema.get("settings"));
        JSONArray formAssets = readNestedArray(settings.get("formAssets"));
        for (int i = 0; i < formAssets.size(); i++) {
            JSONObject asset = formAssets.getJSONObject(i);
            JSONObject schema = readNestedObject(asset.get("schema"));
            appendRuntimeDesignerFormAsset(result, seen, object, runtimeConfig, schema.isEmpty() ? asset : schema,
                    "runtimeDesignerAsset");
        }
        return result;
    }

    private void appendRuntimeDesignerFormAsset(List<Map<String, Object>> result,
                                                Set<String> seen,
                                                BusinessObjectVO object,
                                                AiCrudConfig runtimeConfig,
                                                JSONObject schema,
                                                String source) {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(schema.getString("formKey")),
                StringUtils.trimToNull(schema.getString("defaultFormKey")),
                resolveRuntimeCrudFormKey(object, runtimeConfig));
        if (formKey == null || !seen.add(formKey)) {
            return;
        }
        List<Map<String, Object>> fieldCatalog = collectBusinessFormFieldCatalog(schema);
        if (fieldCatalog.isEmpty()) {
            fieldCatalog = collectRuntimeCrudFormFieldCatalog(runtimeConfig);
        }
        if (fieldCatalog.isEmpty()) {
            return;
        }
        String objectName = resolveRuntimeCrudObjectName(object, runtimeConfig);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("objectCode", resolveRuntimeCrudObjectCode(object, runtimeConfig));
        item.put("objectName", objectName);
        item.put("configKey", runtimeConfig.getConfigKey());
        item.put("formKey", formKey);
        item.put("formName", StringUtils.firstNonBlank(
                StringUtils.trimToNull(schema.getString("formName")),
                StringUtils.isBlank(objectName) ? null : objectName + "表单",
                formKey));
        item.put("viewKey", "default");
        item.put("source", source);
        item.put("sourceType", "businessObjectRuntime");
        item.put("fieldCatalog", fieldCatalog);
        item.put("fields", fieldCatalog);
        item.put("fieldCount", fieldCatalog.size());
        item.put("fieldPreview", buildFieldPreview(fieldCatalog));
        item.put("supportsSave", true);
        result.add(item);
    }

    private void appendUniqueFormAssets(List<Map<String, Object>> target, List<Map<String, Object>> source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        Set<String> seen = target.stream()
                .map(this::formAssetIdentity)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Map<String, Object> asset : source) {
            String key = formAssetIdentity(asset);
            if (StringUtils.isBlank(key) || !seen.add(key)) {
                continue;
            }
            target.add(asset);
        }
    }

    private String formAssetIdentity(Map<String, Object> asset) {
        if (asset == null) {
            return "";
        }
        String formKey = StringUtils.trimToNull(textValue(asset.get("formKey")));
        if (formKey == null) {
            return "";
        }
        return StringUtils.defaultIfBlank(textValue(asset.get("formMode")), textValue(asset.get("type")))
                + "::" + StringUtils.defaultString(textValue(asset.get("providerKey")))
                + "::" + formKey;
    }

    private JSONObject buildRuntimeCrudFormSchema(BusinessObjectVO object, AiCrudConfig runtimeConfig, String requestedFormKey) {
        if (runtimeConfig == null) {
            return new JSONObject();
        }
        JSONObject designerSchema = resolveRuntimeDesignerFormSchema(
                readRuntimeCrudFormDesignerSchema(runtimeConfig), requestedFormKey);
        if (!designerSchema.isEmpty()) {
            return designerSchema;
        }
        String formKey = resolveRuntimeCrudFormKey(object, runtimeConfig);
        if (!matchesRuntimeCrudFormKey(requestedFormKey, formKey, runtimeConfig)) {
            return new JSONObject();
        }
        List<Map<String, Object>> fieldCatalog = collectRuntimeCrudFormFieldCatalog(runtimeConfig);
        if (fieldCatalog.isEmpty()) {
            return new JSONObject();
        }
        JSONObject schema = new JSONObject();
        schema.put("schemaVersion", "runtime-crud");
        schema.put("formKey", formKey);
        schema.put("defaultFormKey", formKey);
        schema.put("formName", resolveRuntimeCrudFormName(object, runtimeConfig));
        schema.put("objectCode", resolveRuntimeCrudObjectCode(object, runtimeConfig));
        schema.put("objectName", resolveRuntimeCrudObjectName(object, runtimeConfig));

        JSONObject settings = new JSONObject();
        JSONObject layout = new JSONObject();
        JSONObject runtimeOptions = readJsonObject(runtimeConfig.getOptions());
        layout.put("gridColumns", Math.max(1, integerValue(runtimeOptions.get("editGridCols"), 2)));
        layout.put("labelPlacement", StringUtils.defaultIfBlank(textValue(runtimeOptions.get("editLabelPlacement")), "left"));
        layout.put("labelWidth", StringUtils.defaultIfBlank(textValue(runtimeOptions.get("editLabelWidth")), "100"));
        settings.put("layout", layout);
        schema.put("settings", settings);

        JSONArray components = new JSONArray();
        for (Map<String, Object> field : fieldCatalog) {
            components.add(toRuntimeCrudFormComponent(field));
        }
        schema.put("components", components);
        return schema;
    }

    private JSONObject resolveRuntimeDesignerFormSchema(JSONObject formSchema, String formKey) {
        if (formSchema == null || formSchema.isEmpty()) {
            return new JSONObject();
        }
        String targetFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formKey),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(formSchema.getString("formKey")));

        JSONObject byForms = findFormSchemaInArray(readNestedArray(formSchema.get("forms")), targetFormKey);
        if (!byForms.isEmpty()) {
            return byForms;
        }
        JSONObject settings = readNestedObject(formSchema.get("settings"));
        JSONObject byAssets = findFormSchemaInArray(readNestedArray(settings.get("formAssets")), targetFormKey);
        if (!byAssets.isEmpty()) {
            return byAssets;
        }
        String rootFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formSchema.getString("formKey")),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")));
        if (StringUtils.isBlank(targetFormKey) || StringUtils.equals(targetFormKey, rootFormKey)) {
            return formSchema;
        }
        return new JSONObject();
    }

    private boolean matchesRuntimeCrudFormKey(String requestedFormKey, String runtimeFormKey, AiCrudConfig runtimeConfig) {
        String requested = StringUtils.trimToNull(requestedFormKey);
        if (requested == null) {
            return true;
        }
        return StringUtils.equals(requested, runtimeFormKey)
                || StringUtils.equals(requested, runtimeConfig.getConfigKey())
                || StringUtils.equals(requested, runtimeConfig.getObjectCode());
    }

    private AiCrudConfig resolveRuntimeConfigForBusinessForm(BusinessObjectVO object, String configKey) {
        String lookupKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(configKey),
                object == null ? null : StringUtils.trimToNull(object.getConfigKey()),
                object == null ? null : StringUtils.trimToNull(object.getObjectCode()));
        return resolvePublishedRuntimeConfig(resolveTenantId(), lookupKey);
    }

    private JSONObject readRuntimeCrudFormDesignerSchema(AiCrudConfig runtimeConfig) {
        JSONObject options = readJsonObject(runtimeConfig == null ? null : runtimeConfig.getOptions());
        return readNestedObject(options.get("formDesignerSchema"));
    }

    private String resolveRuntimeCrudFormKey(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        JSONObject options = readJsonObject(runtimeConfig == null ? null : runtimeConfig.getOptions());
        JSONObject designerSchema = readNestedObject(options.get("formDesignerSchema"));
        String objectCode = resolveRuntimeCrudObjectCode(object, runtimeConfig);
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(designerSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(designerSchema.getString("formKey")),
                StringUtils.trimToNull(textValue(options.get("defaultFormKey"))),
                StringUtils.trimToNull(textValue(options.get("formKey"))),
                StringUtils.isBlank(objectCode) ? null : objectCode + "_default_form",
                runtimeConfig == null ? null : runtimeConfig.getConfigKey());
    }

    private String resolveRuntimeCrudFormName(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        JSONObject options = readJsonObject(runtimeConfig == null ? null : runtimeConfig.getOptions());
        JSONObject designerSchema = readNestedObject(options.get("formDesignerSchema"));
        String objectName = resolveRuntimeCrudObjectName(object, runtimeConfig);
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(designerSchema.getString("formName")),
                StringUtils.trimToNull(textValue(options.get("formName"))),
                StringUtils.isBlank(objectName) ? null : objectName + "表单",
                runtimeConfig == null ? null : runtimeConfig.getAppName(),
                resolveRuntimeCrudFormKey(object, runtimeConfig));
    }

    private String resolveRuntimeCrudObjectCode(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        return StringUtils.firstNonBlank(
                object == null ? null : StringUtils.trimToNull(object.getObjectCode()),
                runtimeConfig == null ? null : StringUtils.trimToNull(runtimeConfig.getObjectCode()),
                runtimeConfig == null ? null : StringUtils.trimToNull(runtimeConfig.getConfigKey()));
    }

    private String resolveRuntimeCrudObjectName(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        return StringUtils.firstNonBlank(
                object == null ? null : StringUtils.trimToNull(object.getObjectName()),
                runtimeConfig == null ? null : StringUtils.trimToNull(runtimeConfig.getObjectName()),
                runtimeConfig == null ? null : StringUtils.trimToNull(runtimeConfig.getAppName()),
                resolveRuntimeCrudObjectCode(object, runtimeConfig));
    }

    private List<Map<String, Object>> collectRuntimeCrudFormFieldCatalog(AiCrudConfig runtimeConfig) {
        if (runtimeConfig == null) {
            return List.of();
        }
        JSONObject options = readJsonObject(runtimeConfig.getOptions());
        List<Map<String, Object>> fields = readMapList(readNestedArray(runtimeConfig.getEditSchema()));
        if (!fields.isEmpty()) {
            List<Map<String, Object>> layoutFields = applyRuntimeCrudFormLayout(
                    fields, readNestedArray(options.get("editFormLayout")));
            return normalizeRuntimeCrudFormFields(layoutFields.isEmpty() ? fields : layoutFields);
        }
        JSONObject modelSchema = readJsonObject(runtimeConfig.getModelSchema());
        return normalizeRuntimeCrudFormFields(readMapList(readNestedArray(modelSchema.get("fields"))));
    }

    private List<Map<String, Object>> normalizeRuntimeCrudFormFields(List<Map<String, Object>> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> field : fields) {
            Map<String, Object> item = normalizeRuntimeCrudFormField(field);
            String fieldCode = item == null ? null : StringUtils.trimToNull(textValue(item.get("field")));
            if (fieldCode != null && seen.add(fieldCode)) {
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, Object> normalizeRuntimeCrudFormField(Map<String, Object> field) {
        if (field == null || field.isEmpty()) {
            return null;
        }
        String fieldCode = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(field.get("field"))),
                StringUtils.trimToNull(textValue(field.get("fieldCode"))),
                StringUtils.trimToNull(textValue(field.get("prop"))),
                StringUtils.trimToNull(textValue(field.get("name"))),
                StringUtils.trimToNull(textValue(field.get("model"))));
        if (fieldCode == null
                || readBooleanValue(field.get("systemField"), false)
                || readBooleanValue(field.get("internal"), false)
                || (field.containsKey("formVisible") && !readBooleanValue(field.get("formVisible"), true))) {
            return null;
        }
        JSONObject props = readNestedObject(field.get("props"));
        Map<String, Object> item = new LinkedHashMap<>(field);
        item.put("field", fieldCode);
        item.put("fieldCode", fieldCode);
        item.put("label", StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(field.get("label"))),
                StringUtils.trimToNull(textValue(field.get("title"))),
                StringUtils.trimToNull(textValue(field.get("fieldName"))),
                fieldCode));
        String componentType = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(field.get("componentType"))),
                StringUtils.trimToNull(textValue(field.get("type"))),
                StringUtils.trimToNull(textValue(field.get("componentKey"))),
                "input");
        item.put("type", normalizeTaskFormFieldType(componentType));
        item.put("componentType", componentType);
        if (field.get("dataType") != null) {
            item.put("dataType", textValue(field.get("dataType")));
        }
        String dictType = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(field.get("dictType"))),
                StringUtils.trimToNull(props.getString("dictType")));
        if (dictType != null) {
            item.put("dictType", dictType);
        }
        item.put("required", readBooleanValue(field.get("required"), false));
        item.putIfAbsent("readable", true);
        item.putIfAbsent("writable", !readBooleanValue(field.get("readonly"), false));
        return item;
    }

    private JSONObject toRuntimeCrudFormComponent(Map<String, Object> field) {
        JSONObject component = new JSONObject();
        String fieldCode = StringUtils.trimToEmpty(textValue(field.get("field")));
        String componentType = StringUtils.defaultIfBlank(textValue(field.get("componentType")), "input");
        component.put("id", fieldCode);
        component.put("key", fieldCode);
        component.put("type", componentType);
        component.put("componentType", componentType);
        component.put("label", StringUtils.defaultIfBlank(textValue(field.get("label")), fieldCode));
        component.put("field", fieldCode);

        JSONObject binding = new JSONObject();
        binding.put("mode", "field");
        binding.put("fieldCode", fieldCode);
        binding.put("dataType", StringUtils.trimToEmpty(textValue(field.get("dataType"))));
        component.put("fieldBinding", binding);

        JSONObject props = readNestedObject(field.get("props"));
        props.put("field", fieldCode);
        props.put("label", component.getString("label"));
        if (field.get("dictType") != null) {
            props.put("dictType", field.get("dictType"));
        }
        component.put("props", props);

        JSONObject validation = new JSONObject();
        validation.put("required", readBooleanValue(field.get("required"), false));
        component.put("validation", validation);
        return component;
    }

    private void appendBusinessFormAsset(List<Map<String, Object>> result,
                                         Set<String> seen,
                                         BusinessObjectVO object,
                                         JSONObject schema,
                                         String source) {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(schema.getString("formKey")),
                StringUtils.trimToNull(schema.getString("defaultFormKey")));
        if (formKey == null || !seen.add(formKey)) {
            return;
        }
        String formName = StringUtils.defaultIfBlank(schema.getString("formName"), object.getObjectName() + "表单");
        List<Map<String, Object>> fieldCatalog = collectBusinessFormFieldCatalog(schema);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("objectCode", object.getObjectCode());
        item.put("objectName", object.getObjectName());
        item.put("formKey", formKey);
        item.put("formName", formName);
        item.put("viewKey", "default");
        item.put("source", source);
        item.put("sourceType", "businessObject");
        item.put("fieldCatalog", fieldCatalog);
        item.put("fields", fieldCatalog);
        item.put("fieldCount", fieldCatalog.size());
        item.put("fieldPreview", buildFieldPreview(fieldCatalog));
        item.put("supportsSave", true);
        result.add(item);
    }

    private List<Map<String, Object>> collectBusinessFormFieldCatalog(JSONObject schema) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        collectBusinessFormFieldComponents(readNestedArray(schema.get("components")), result, seen);
        return result;
    }

    private List<String> buildFieldPreview(List<Map<String, Object>> fields) {
        List<String> preview = new ArrayList<>();
        if (fields == null) {
            return preview;
        }
        for (Map<String, Object> field : fields) {
            String label = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(field.get("label"))),
                    StringUtils.trimToNull(textValue(field.get("field"))),
                    StringUtils.trimToNull(textValue(field.get("fieldCode"))));
            if (label != null) {
                preview.add(label);
            }
            if (preview.size() >= 5) {
                break;
            }
        }
        return preview;
    }

    private void collectBusinessFormFieldComponents(JSONArray components,
                                                    List<Map<String, Object>> result,
                                                    Set<String> seen) {
        if (components == null) {
            return;
        }
        for (int i = 0; i < components.size(); i++) {
            JSONObject component = components.getJSONObject(i);
            if (component == null) {
                continue;
            }
            JSONObject binding = readNestedObject(component.get("fieldBinding"));
            JSONObject props = readNestedObject(component.get("props"));
            String field = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(binding.getString("fieldCode")),
                    StringUtils.trimToNull(component.getString("field")),
                    StringUtils.trimToNull(props.getString("field")));
            if (field != null && seen.add(field)) {
                JSONObject validation = readNestedObject(component.get("validation"));
                Map<String, Object> item = new LinkedHashMap<>(component);
                item.put("field", field);
                item.put("fieldCode", field);
                item.put("label", StringUtils.firstNonBlank(
                        StringUtils.trimToNull(component.getString("label")),
                        StringUtils.trimToNull(props.getString("label")),
                        StringUtils.trimToNull(props.getString("title")),
                        field));
                String componentType = StringUtils.firstNonBlank(
                        StringUtils.trimToNull(component.getString("type")),
                        StringUtils.trimToNull(component.getString("componentType")),
                        StringUtils.trimToNull(component.getString("componentKey")));
                item.put("type", normalizeTaskFormFieldType(componentType));
                item.put("componentType", StringUtils.defaultIfBlank(componentType, "input"));
                item.putIfAbsent("dataType", StringUtils.trimToEmpty(binding.getString("dataType")));
                String dictType = StringUtils.firstNonBlank(
                        StringUtils.trimToNull(textValue(item.get("dictType"))),
                        StringUtils.trimToNull(props.getString("dictType")));
                if (dictType != null) {
                    item.put("dictType", dictType);
                }
                item.putIfAbsent("required", readBooleanValue(validation.get("required"), false));
                result.add(item);
            }
            collectBusinessFormFieldComponents(readNestedArray(component.get("children")), result, seen);
        }
    }

    private JSONObject readJsonObject(String json) {
        if (StringUtils.isBlank(json)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private JSONObject readNestedObject(Object value) {
        if (value == null) {
            return new JSONObject();
        }
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (value instanceof Map<?, ?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONObject() : JSON.parseObject(text);
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    private JSONArray readNestedArray(Object value) {
        if (value == null) {
            return new JSONArray();
        }
        if (value instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        if (value instanceof List<?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONArray() : JSON.parseArray(text);
            } catch (Exception e) {
                return new JSONArray();
            }
        }
        return new JSONArray();
    }

    private void putIfText(Map<String, Object> target, String key, Object value) {
        String text = StringUtils.trimToNull(value == null ? null : String.valueOf(value));
        if (text != null && !"null".equalsIgnoreCase(text)) {
            target.put(key, text);
        }
    }

    private String normalizeStartMode(String startMode) {
        String normalized = StringUtils.defaultIfBlank(startMode, "MANUAL").trim().toUpperCase();
        if ("MANUAL_AND_TRIGGER".equals(normalized) || "MANUAL_TRIGGER".equals(normalized) || "BOTH".equals(normalized)) {
            return "BOTH";
        }
        if ("AUTO".equals(normalized) || "AUTOMATIC".equals(normalized)) {
            return "TRIGGER";
        }
        if ("TRIGGER".equals(normalized)) {
            return "TRIGGER";
        }
        return "MANUAL";
    }

    private List<BusinessFlowBindingDTO.VariableMappingDTO> normalizeVariableMapping(JSONArray variableMapping) {
        List<BusinessFlowBindingDTO.VariableMappingDTO> result = new ArrayList<>();
        if (variableMapping == null) {
            return result;
        }
        for (int i = 0; i < variableMapping.size(); i++) {
            JSONObject mapping = variableMapping.getJSONObject(i);
            if (mapping == null) {
                continue;
            }
            String formField = StringUtils.defaultIfBlank(mapping.getString("formField"), mapping.getString("field"));
            String flowVariable = StringUtils.defaultIfBlank(mapping.getString("flowVariable"), mapping.getString("variable"));
            if (StringUtils.isBlank(formField) || StringUtils.isBlank(flowVariable)) {
                continue;
            }
            BusinessFlowBindingDTO.VariableMappingDTO item = new BusinessFlowBindingDTO.VariableMappingDTO();
            item.setFormField(formField.trim());
            item.setFlowVariable(flowVariable.trim());
            item.setLabel(StringUtils.trimToNull(mapping.getString("label")));
            result.add(item);
        }
        return result;
    }

    private JSONObject readBindingConfig(String bindingConfig) {
        if (StringUtils.isBlank(bindingConfig)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(bindingConfig);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private String resolveFlowModelKey(JSONObject config) {
        if (config == null) {
            return null;
        }
        return StringUtils.firstNonBlank(
                config.getString("flowModelKey"),
                config.getString("flowKey"),
                config.getString("processDefinitionKey"),
                config.getString("modelKey")
        );
    }

    private String resolveBindingName(JSONObject config) {
        return StringUtils.defaultIfBlank(config.getString("flowModelName"), config.getString("flowModelKey") + " 流程");
    }

    private JSONObject toConfigJson(BusinessFlowBindingVO binding) {
        JSONObject config = new JSONObject();
        config.put("flowModelKey", binding.getFlowModelKey());
        config.put("flowModelName", binding.getFlowModelName());
        config.put("titleTemplate", binding.getTitleTemplate());
        config.put("startMode", binding.getStartMode());
        JSONObject businessBinding = normalizeBusinessBinding(binding.getBusinessBinding());
        if (!businessBinding.isEmpty()) {
            config.put("businessBinding", businessBinding);
        }
        config.put("variableMapping", binding.getVariableMapping());
        config.put("nodeForms", binding.getNodeForms());
        config.put("conditionFlows", binding.getConditionFlows());
        config.put("options", binding.getOptions());
        return config;
    }

    private List<Map<String, Object>> normalizeNodeForms(List<Map<String, Object>> nodeForms) {
        if (nodeForms == null || nodeForms.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> source : nodeForms) {
            if (source == null) {
                continue;
            }
            String taskDefKey = StringUtils.trimToNull(textValue(source.get("taskDefKey")));
            if (taskDefKey == null || !seen.add(taskDefKey)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            putIfText(item, "taskDefKey", taskDefKey);
            putIfText(item, "taskName", source.get("taskName"));
            String formMode = normalizeNodeFormMode(textValue(source.get("formMode")));
            putIfText(item, "formMode", formMode);
            putIfText(item, "formKey", source.get("formKey"));
            putIfText(item, "formName", source.get("formName"));
            putIfText(item, "providerKey", source.get("providerKey"));
            putIfText(item, "formUrl", source.get("formUrl"));
            putIfText(item, "viewKey", source.get("viewKey"));
            putIfText(item, "editMode", normalizeNodeEditMode(textValue(source.get("editMode"))));
            Object formRef = source.get("formRef");
            if (formRef instanceof Map<?, ?> || formRef instanceof JSONObject) {
                item.put("formRef", readNestedObject(formRef));
            }
            List<Map<String, Object>> fieldPermissions = normalizeFieldPermissions(source.get("fieldPermissions"));
            if (fieldPermissions.isEmpty()) {
                fieldPermissions = normalizeFieldSelections(source);
            }
            if (!fieldPermissions.isEmpty()) {
                item.put("fieldPermissions", fieldPermissions);
            }
            result.add(item);
        }
        return result;
    }

    private String normalizeNodeFormMode(String formMode) {
        String normalized = StringUtils.defaultIfBlank(formMode, "BUSINESS_OBJECT_FORM").trim().toUpperCase();
        if ("BUSINESS_CODE_FORM".equals(normalized) || "EXTERNAL".equals(normalized)) {
            return normalized;
        }
        return "BUSINESS_OBJECT_FORM";
    }

    private String normalizeNodeEditMode(String editMode) {
        String normalized = StringUtils.defaultIfBlank(editMode, "READONLY").trim().toUpperCase();
        if ("EDITABLE".equals(normalized) || "MODIFY_RESUBMIT".equals(normalized)) {
            return normalized;
        }
        return "READONLY";
    }

    private List<Map<String, Object>> normalizeFieldPermissions(Object permissions) {
        if (permissions instanceof Map<?, ?> || permissions instanceof JSONObject) {
            JSONObject object = readNestedObject(permissions);
            JSONArray fields = readNestedArray(object.get("fields"));
            if (!fields.isEmpty()) {
                return normalizeFieldPermissions(fields);
            }
            return buildFieldPermissionsFromSelections(
                    readFieldSet(object, "visibleFields", "visible", "readableFields", "readable"),
                    readFieldSet(object, "writableFields", "writable"),
                    readFieldSet(object, "requiredFields", "required"));
        }
        JSONArray array = readNestedArray(permissions);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject source = array.getJSONObject(i);
            if (source == null) {
                continue;
            }
            String field = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(source.getString("field")),
                    StringUtils.trimToNull(source.getString("fieldCode")),
                    StringUtils.trimToNull(source.getString("code")));
            if (field == null || !seen.add(field)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", field);
            item.put("fieldCode", field);
            putIfText(item, "label", source.getString("label"));
            boolean readable = readBooleanValue(source.get("readable"), readBooleanValue(source.get("visible"), true));
            boolean writable = readable && readBooleanValue(source.get("writable"), readBooleanValue(source.get("editable"), true));
            item.put("visible", readable);
            item.put("editable", writable);
            item.put("readable", readable);
            item.put("writable", writable);
            item.put("required", writable && readBooleanValue(source.get("required"), false));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> normalizeFieldSelections(Map<String, Object> source) {
        return buildFieldPermissionsFromSelections(
                readFieldSet(source, "visibleFields", "visible", "readableFields", "readable"),
                readFieldSet(source, "writableFields", "writable"),
                readFieldSet(source, "requiredFields", "required"));
    }

    private List<Map<String, Object>> buildFieldPermissionsFromSelections(Set<String> visible,
                                                                          Set<String> writable,
                                                                          Set<String> required) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(visible);
        fields.addAll(writable);
        fields.addAll(required);
        if (fields.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String field : fields) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", field);
            boolean readable = visible.isEmpty() || visible.contains(field);
            boolean editable = readable && writable.contains(field);
            item.put("fieldCode", field);
            item.put("visible", readable);
            item.put("editable", editable);
            item.put("readable", readable);
            item.put("writable", editable);
            item.put("required", editable && required.contains(field));
            result.add(item);
        }
        return result;
    }

    private Set<String> readFieldSet(Map<String, Object> source, String... keys) {
        Set<String> result = new LinkedHashSet<>();
        if (source == null || keys == null) {
            return result;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            JSONArray array = readNestedArray(value);
            for (int i = 0; i < array.size(); i++) {
                String field = StringUtils.trimToNull(array.getString(i));
                if (field != null) {
                    result.add(field);
                }
            }
        }
        return result;
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? null : text;
    }

    private boolean readBooleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    private Boolean readNullableBooleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    private List<Map<String, Object>> readMapList(JSONArray array) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                result.add(new LinkedHashMap<>(item));
            }
        }
        return result;
    }

    private Map<String, Object> readOptions(JSONObject options) {
        if (options == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(options);
    }

    private Object readRecordValue(Map<String, Object> recordData, String field) {
        if (recordData == null || StringUtils.isBlank(field)) {
            return null;
        }
        Object value = readRecordValueFromFlatMap(recordData, field);
        if (value != null || containsRecordField(recordData, field)) {
            return value;
        }
        Object main = recordData.get("main");
        if (main instanceof Map<?, ?> mainMap) {
            Map<String, Object> mainRecord = new LinkedHashMap<>();
            mainMap.forEach((key, item) -> {
                if (key != null) {
                    mainRecord.put(String.valueOf(key), item);
                }
            });
            return readRecordValueFromFlatMap(mainRecord, field);
        }
        return null;
    }

    private Object readRecordValueFromFlatMap(Map<String, Object> recordData, String field) {
        if (recordData.containsKey(field)) {
            return recordData.get(field);
        }
        String camelField = snakeToCamel(field);
        if (recordData.containsKey(camelField)) {
            return recordData.get(camelField);
        }
        String snakeField = camelToSnake(field);
        if (recordData.containsKey(snakeField)) {
            return recordData.get(snakeField);
        }
        return null;
    }

    private boolean containsRecordField(Map<String, Object> recordData, String field) {
        if (recordData == null || StringUtils.isBlank(field)) {
            return false;
        }
        return recordData.containsKey(field)
                || recordData.containsKey(snakeToCamel(field))
                || recordData.containsKey(camelToSnake(field));
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        return tenantId != null ? tenantId : 1L;
    }

    private Long resolveUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUsername() {
        try {
            return SessionHelper.getUsername();
        } catch (Exception e) {
            return null;
        }
    }

    private record FlowStartContext(String requestedObjectCode,
                                    String objectCode,
                                    String configKey,
                                    AiBusinessDocumentConfig documentConfig,
                                    AiCrudConfig runtimeConfig) {
    }

    private record BusinessRuntimeContext(String requestedObjectCode,
                                          String objectCode,
                                          String configKey,
                                          AiBusinessDocumentConfig documentConfig,
                                          AiCrudConfig runtimeConfig,
                                          AiBusinessObject businessObject) {
    }

    private record BindingLookupResult(AiBusinessBinding binding,
                                       String matchedObjectCode,
                                       List<String> candidates) {
    }

    private record FlowStartLockHandle(String lockKey,
                                       RLock redissonLock,
                                       ReentrantLock localLock) {
    }

    private record BusinessKeyParts(String objectCode, Long recordId) {
    }

    private record BusinessListGroup(BusinessRuntimeContext context,
                                     List<BusinessListRuntime> runtimes) {
    }

    private record BusinessListRuntime(FlowBusinessListDisplayItem item,
                                       String objectCode,
                                       Long recordId,
                                       String businessKey) {
    }

    private record TaskFormRuntimeContext(String objectCode,
                                          Long recordId,
                                          String businessKey,
                                          String configKey,
                                          JSONObject bindingConfig) {
    }
}
