package com.mdframe.forge.plugin.generator.service.businessapp;

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
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowWithdrawDTO;
import com.mdframe.forge.plugin.generator.enums.BusinessDocumentFlowStatus;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowResubmitDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskActionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessRunMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.redisson.api.RedissonClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowBindingCodec.normalizeBindingConfig;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowBindingCodec.readBindingConfig;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowBindingCodec.resolveBindingName;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowBindingCodec.resolveFlowModelKey;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowBindingCodec.toConfigJson;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowBindingCodec.toDTO;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowJsonReader.readJsonObject;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRecordValues.read;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowIdentityCodec.buildBusinessKey;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowIdentityCodec.parseBusinessKey;

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
@FlowBind(modelKey = "*", businessType = "lowcode-business")
public class BusinessFlowService {

    private static final BusinessFlowStartContextAssembler START_CONTEXT_ASSEMBLER =
            BusinessFlowStartContextAssembler.standard();
    private static final BusinessFlowTaskAccessPolicy TASK_ACCESS_POLICY =
            new BusinessFlowTaskAccessPolicy();
    private static final BusinessFlowTaskChildPolicy TASK_CHILD_POLICY =
            new BusinessFlowTaskChildPolicy();
    private static final BusinessFlowTaskFormPolicy TASK_FORM_POLICY =
            new BusinessFlowTaskFormPolicy();
    @Autowired(required = false)
    private FlowClient flowClient;

    /**
     * 应用页面表单资产属于应用设计快照，不属于业务对象本身。
     * 使用字段注入保持已有测试/插件扩展的构造器兼容性。
     */
    @Autowired(required = false)
    private BusinessApplicationService businessApplicationService;

    /**
     * 流程绑定保存时同步低代码托管字段，避免只保存 binding 而沿用旧列表发布快照。
     */
    @Autowired(required = false)
    private BusinessFlowStatusFieldService flowStatusFieldService;

    /**
     * 业务流程运行和应用归属用于向前端返回服务端确认的打印身份。
     * 使用字段注入保持已有扩展和单元测试的构造器兼容性。
     */
    @Autowired(required = false)
    private BusinessProcessRunMapper businessProcessRunMapper;

    @Autowired(required = false)
    private BusinessApplicationObjectMapper businessApplicationObjectMapper;

    /**
     * 任务事件状态同步使用独立事务，避免内部回写失败把外层 FlowCallback 事务标成 rollback-only。
     */
    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    private final BusinessBindingMapper bindingMapper;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessFieldDesignService businessFieldDesignService;
    private final BusinessFlowVariableResolver variableResolver;
    private final BusinessCodeFormProviderRegistry codeFormProviderRegistry;
    private final BusinessFlowBindingResolver flowBindingResolver;
    private final BusinessRuntimeConfigResolver runtimeConfigResolver;
    private final BusinessFlowStatusRepairService statusRepairService;
    private final BusinessFlowTaskFormSchemaAssembler taskFormSchemaAssembler;
    private final BusinessFlowFormAssetAssembler formAssetAssembler;
    private final BusinessFlowTaskChildAssembler taskChildAssembler;
    private final BusinessFlowCodeFormCoordinator codeFormCoordinator;
    private final BusinessFlowTaskFormProfiler taskFormProfiler = new BusinessFlowTaskFormProfiler();
    private final BusinessFlowApplicationPageFormResolver applicationPageFormResolver;
    private final BusinessFlowTaskNodeFormResolver taskNodeFormResolver;
    private final BusinessFlowRuntimeContextResolver businessRuntimeContextResolver;
    private final BusinessFlowFormAssetCatalog formAssetCatalog;
    private final BusinessFlowBindingViewAssembler bindingViewAssembler;
    private final BusinessFlowTaskFormContextCoordinator taskFormContextCoordinator;
    private final BusinessFlowListDisplayEnricher businessListDisplayEnricher;
    private final BusinessFlowStartCoordinator startCoordinator;
    private final BusinessFlowTaskEventCoordinator taskEventCoordinator;
    private final BusinessFlowCallbackCoordinator callbackCoordinator;
    private final BusinessFlowTaskCommandCoordinator taskCommandCoordinator;

    public BusinessFlowService(BusinessBindingMapper bindingMapper,
                               BusinessFlowInstanceLinkMapper flowInstanceLinkMapper,
                               AiCrudConfigMapper crudConfigMapper,
                               BusinessObjectMapper businessObjectMapper,
                               BusinessDocumentConfigService documentConfigService,
                               BusinessDocumentRuntimeService documentRuntimeService,
                               DynamicCrudService dynamicCrudService,
                               BusinessFieldDesignService businessFieldDesignService,
                               BusinessFlowVariableResolver variableResolver,
                               BusinessCodeFormProviderRegistry codeFormProviderRegistry,
                               ApplicationEventPublisher applicationEventPublisher,
                               ObjectProvider<RedissonClient> redissonClientProvider,
                               ObjectProvider<BusinessActionExecutionService> actionExecutionServiceProvider) {
        this.bindingMapper = bindingMapper;
        this.flowInstanceLinkMapper = flowInstanceLinkMapper;
        this.crudConfigMapper = crudConfigMapper;
        this.businessObjectMapper = businessObjectMapper;
        this.documentConfigService = documentConfigService;
        this.dynamicCrudService = dynamicCrudService;
        this.businessFieldDesignService = businessFieldDesignService;
        this.variableResolver = variableResolver;
        this.codeFormProviderRegistry = codeFormProviderRegistry;
        this.flowBindingResolver = new BusinessFlowBindingResolver(bindingMapper);
        this.runtimeConfigResolver = new BusinessRuntimeConfigResolver(crudConfigMapper, this::resolveTenantId);
        this.statusRepairService = new BusinessFlowStatusRepairService(
                runtimeConfigResolver, dynamicCrudService, documentConfigService);
        this.taskFormSchemaAssembler = new BusinessFlowTaskFormSchemaAssembler(
                dynamicCrudService,
                runtimeConfigResolver,
                TASK_CHILD_POLICY,
                this::resolveTenantId);
        this.formAssetAssembler = new BusinessFlowFormAssetAssembler(
                businessFieldDesignService,
                taskFormSchemaAssembler::applyRuntimeCrudFormLayout,
                taskFormSchemaAssembler::appendRuntimeChildFieldCatalog);
        this.taskChildAssembler = new BusinessFlowTaskChildAssembler(
                TASK_CHILD_POLICY,
                TASK_FORM_POLICY,
                businessObjectMapper,
                businessFieldDesignService,
                formAssetAssembler,
                this::resolveTenantId);
        this.applicationPageFormResolver = new BusinessFlowApplicationPageFormResolver(
                () -> businessApplicationService,
                businessObjectMapper,
                this::resolveTenantId,
                taskFormProfiler::mark,
                taskFormProfiler::note);
        this.taskNodeFormResolver = new BusinessFlowTaskNodeFormResolver(
                () -> flowClient,
                applicationPageFormResolver,
                TASK_CHILD_POLICY,
                this::collectTaskFormAssets,
                taskFormProfiler::mark,
                taskFormProfiler::note);
        this.businessRuntimeContextResolver = new BusinessFlowRuntimeContextResolver(
                runtimeConfigResolver,
                documentConfigService,
                businessObjectMapper,
                flowInstanceLinkMapper,
                applicationPageFormResolver,
                taskNodeFormResolver,
                this::resolveTenantId,
                flowBindingResolver::selectForConfig,
                taskFormProfiler::mark,
                taskFormProfiler::note);
        this.formAssetCatalog = new BusinessFlowFormAssetCatalog(
                () -> businessApplicationService,
                this::resolveTenantId,
                applicationPageFormResolver,
                businessRuntimeContextResolver,
                runtimeConfigResolver,
                businessObjectMapper,
                bindingMapper,
                flowBindingResolver,
                businessFieldDesignService,
                formAssetAssembler,
                codeFormProviderRegistry,
                taskFormProfiler);
        this.bindingViewAssembler = new BusinessFlowBindingViewAssembler(
                runtimeConfigResolver, businessRuntimeContextResolver);
        this.codeFormCoordinator = new BusinessFlowCodeFormCoordinator(
                codeFormProviderRegistry,
                TASK_FORM_POLICY,
                code -> formAssetCatalog.readCodeAppMetadata(resolveTenantId(), code));
        this.taskFormContextCoordinator = new BusinessFlowTaskFormContextCoordinator(
                () -> flowClient,
                taskNodeFormResolver,
                businessRuntimeContextResolver,
                TASK_ACCESS_POLICY,
                TASK_FORM_POLICY,
                taskFormSchemaAssembler,
                taskChildAssembler,
                TASK_CHILD_POLICY,
                codeFormCoordinator,
                dynamicCrudService,
                taskFormProfiler,
                this::resolveTenantId,
                this::resolveUserId,
                () -> businessProcessRunMapper,
                () -> businessApplicationObjectMapper,
                formAssetCatalog::queryBusinessObject,
                formAssetCatalog::toBusinessObjectVO,
                formAssetCatalog::resolveBusinessFormSchema,
                this::resolveBusinessSummary);
        this.businessListDisplayEnricher = new BusinessFlowListDisplayEnricher(
                this::resolveTenantId,
                flowInstanceLinkMapper,
                businessRuntimeContextResolver,
                dynamicCrudService,
                codeFormProviderRegistry,
                formAssetCatalog::queryBusinessObject,
                formAssetCatalog::toBusinessObjectVO,
                flowBindingResolver::selectForConfig,
                this::resolveBusinessSummary);
        BusinessFlowStatusTransitionService statusTransitionService = new BusinessFlowStatusTransitionService(
                documentConfigService, dynamicCrudService);
        this.startCoordinator = new BusinessFlowStartCoordinator(
                () -> flowClient,
                flowInstanceLinkMapper,
                documentConfigService,
                documentRuntimeService,
                dynamicCrudService,
                businessRuntimeContextResolver,
                runtimeConfigResolver,
                flowBindingResolver,
                formAssetAssembler,
                statusRepairService,
                statusTransitionService,
                redissonClientProvider,
                this::resolveUserId,
                this::resolveActiveOrgId,
                this::resolveUsername);
        this.taskEventCoordinator = new BusinessFlowTaskEventCoordinator(
                () -> flowClient,
                flowInstanceLinkMapper,
                documentConfigService,
                runtimeConfigResolver,
                statusRepairService,
                () -> transactionManager,
                this::resolveTenantId,
                this::resolveUserId,
                flowBindingResolver::selectForConfig,
                statusTransitionService);
        this.callbackCoordinator = new BusinessFlowCallbackCoordinator(
                flowInstanceLinkMapper,
                documentConfigService,
                dynamicCrudService,
                runtimeConfigResolver,
                statusRepairService,
                statusTransitionService,
                taskEventCoordinator,
                actionExecutionServiceProvider,
                applicationEventPublisher,
                this::resolveTenantId,
                this::resolveUsername,
                flowBindingResolver::selectForConfig,
                this::resolveTerminalBusinessFlowResult);
        this.taskCommandCoordinator = new BusinessFlowTaskCommandCoordinator(
                () -> flowClient,
                flowInstanceLinkMapper,
                dynamicCrudService,
                taskNodeFormResolver,
                businessRuntimeContextResolver,
                taskFormContextCoordinator,
                taskEventCoordinator,
                callbackCoordinator,
                TASK_ACCESS_POLICY,
                TASK_FORM_POLICY,
                TASK_CHILD_POLICY,
                taskFormSchemaAssembler,
                taskChildAssembler,
                codeFormCoordinator,
                this::resolveTenantId,
                this::resolveUserId,
                formAssetCatalog::queryBusinessObject,
                formAssetCatalog::toBusinessObjectVO,
                formAssetCatalog::resolveBusinessFormSchema,
                this::resolveTerminalBusinessFlowResult);
    }

    /** 查询 Flowable 模型中需要发起人选择审批人的节点，供应用级流程启动页复用。 */
    public Map<String, Object> getFlowStartConfig(String modelKey) {
        if (flowClient == null || StringUtils.isBlank(modelKey)) {
            return Map.of("modelKey", modelKey, "initiatorSelectNodes", List.of());
        }
        FlowResult<Map<String, Object>> result = flowClient.getModelStartConfig(modelKey.trim());
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return Map.of("modelKey", modelKey.trim(), "initiatorSelectNodes", List.of());
        }
        return new LinkedHashMap<>(result.getData());
    }

    /**
     * 从业务记录发起流程
     *
     * @param objectCode 业务对象编码
     * @param recordId   业务记录ID
     * @param recordData 业务记录数据
     * @return 流程发起结果
     */
    public JSONObject startFlow(String objectCode, String recordId, Map<String, Object> recordData) {
        return startFlow(objectCode, recordId, recordData, null);
    }

    public JSONObject startFlow(String objectCode, String recordId, Map<String, Object> recordData,
                                Map<String, Object> requestedVariables) {
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
        BusinessFlowStartContextAssembler.StartContext startContext = START_CONTEXT_ASSEMBLER.assemble(
                bindingConfig, recordData, requestedVariables, objectCode);
        Map<String, Object> flowVariables = startContext.variables();

        // 3. 构建业务Key和标题
        String businessKey = objectCode + ":" + recordId;
        String title = startContext.title();

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
        String canonicalObjectCode = businessRuntimeContextResolver.resolveCanonicalObjectCode(tenantId, objectCode);
        AiBusinessBinding binding = flowBindingResolver.selectForConfig(
                tenantId, canonicalObjectCode, objectCode);

        if (binding == null) {
            AiBusinessDocumentConfig documentConfig = businessRuntimeContextResolver.resolveEnabledDocumentConfig(
                    tenantId, canonicalObjectCode, resolvePublishedRuntimeConfig(tenantId, objectCode));
            if (documentConfig == null || StringUtils.isBlank(documentConfig.getDefaultFlowKey())) {
                return null;
            }
            return bindingViewAssembler.fromLegacyDocument(canonicalObjectCode, documentConfig);
        }
        return bindingViewAssembler.fromBinding(canonicalObjectCode, binding);
    }

    /**
     * 查询正式业务发起入口需要补充的流程参数。
     */
    public Map<String, Object> getBusinessStartConfig(String objectCode) {
        BusinessFlowBindingVO binding = getFlowBinding(objectCode);
        if (binding == null || StringUtils.isBlank(binding.getFlowModelKey())) {
            throw new BusinessException("业务对象未配置主流程");
        }
        FlowResult<Map<String, Object>> result = flowClient.getModelStartConfig(binding.getFlowModelKey());
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null
                    ? "读取流程发起配置失败"
                    : StringUtils.defaultIfBlank(result.getMsg(), "读取流程发起配置失败"));
        }
        return result.getData() == null ? Map.of() : result.getData();
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
        businessListDisplayEnricher.enrich(items);
    }

    /**
     * 按流程模型 Key 反查已绑定的业务对象。
     */
    public List<BusinessBindingSummaryVO> listBusinessBindingsByModelKey(String modelKey) {
        String key = StringUtils.trimToNull(modelKey);
        if (key == null) {
            throw new BusinessException("流程模型Key不能为空");
        }
        List<BusinessBindingSummaryVO> rows = bindingMapper.selectFlowBindingsByModelKey(
                resolveTenantId(), key, BusinessBindingSummarySupport.flowModelKeyLikeNeedle(key));
        rows = BusinessBindingSummarySupport.retainMatchingModelKey(rows, key);
        BusinessBindingSummarySupport.enrichDisplay(rows);
        return rows;
    }

    /**
     * 查询业务对象可供流程节点绑定的表单资产。
     */
    public Map<String, Object> getFormAssets(String objectCode) {
        return formAssetCatalog.getFormAssets(objectCode, false, null);
    }

    /**
     * 查询业务对象可供流程节点绑定的表单资产。
     */
    public Map<String, Object> getFormAssets(String objectCode, boolean includeInternal) {
        return formAssetCatalog.getFormAssets(objectCode, includeInternal, null);
    }

    /**
     * 查询当前应用中当前业务对象实际可作为审批任务表单的页面资产。
     * applicationId 为空时保留对象级/代码表单兼容目录；有应用上下文时只返回该应用的真实页面表单，
     * 避免把业务对象字段注册表误显示成一个不存在的页面。
     */
    public Map<String, Object> getFormAssets(String objectCode,
                                             boolean includeInternal,
                                             Long applicationId) {
        return formAssetCatalog.getFormAssets(objectCode, includeInternal, applicationId);
    }

    /**
     * 从应用草稿中的页面节点和页面布局提取真实页面表单。
     * 表单 key 带应用/页面/资产三段稳定身份，运行时无需额外传 applicationId 即可重新解析页面。
     */
   public Map<String, Object> getCodeAppMetadata(String objectCode) {
        return formAssetCatalog.getCodeAppMetadata(objectCode);
    }

    /**
     * 只更新已有流程绑定中的代码应用元数据，避免字段/视图配置覆盖流程模型和变量映射。
     */
    public boolean saveCodeAppMetadata(String objectCode, Object metadata) {
        return formAssetCatalog.saveCodeAppMetadata(objectCode, metadata);
    }

    /**
     * 查询待办任务对应的业务表单上下文。
     */
    public BusinessTaskFormContextVO getTaskFormContext(BusinessTaskFormContextQueryDTO query) {
        return taskFormContextCoordinator.getTaskFormContext(query);
    }

    /**
     * 查询当前用户已签收、可直接办理的待办上下文。
     *
     * <p>该只读校验入口供受控流程动作在 elicitation 前确认真实办理权使用，
     * 候选但未签收的任务不会被视为可办理任务。</p>
     */
    public BusinessTaskFormContextVO getActionableTaskFormContext(BusinessTaskFormContextQueryDTO query) {
        return taskFormContextCoordinator.getActionableTaskFormContext(query);
    }

    /**
     * 查询历史/已办场景下的业务表单上下文，只用于只读展示，不校验运行中待办任务身份。
     */
    public BusinessTaskFormContextVO getTaskFormReadonlyContext(BusinessTaskFormContextQueryDTO query) {
        return taskFormContextCoordinator.getTaskFormReadonlyContext(query);
    }

    /**
     * 保存待办任务允许编辑的业务字段，并返回最新上下文。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessTaskFormContextVO saveTaskFormContext(BusinessTaskFormSaveDTO dto) {
        return taskCommandCoordinator.saveTaskFormContext(dto);
    }

    /**
     * 办理低代码业务待办。该入口在 Flowable 任务完成后同步业务流程实例和业务单据状态，
     * 避免低代码单据状态停留在发起时的 IN_PROCESS。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO completeBusinessTask(BusinessTaskActionDTO dto) {
        return taskCommandCoordinator.completeBusinessTask(dto);
    }

    /**
     * 仅供 FLOW_ACTION 本地审计恢复调用。此时远程任务可能已完成，不能再要求它处于 actionable；
     * Flow 服务仍以同租户、原签收人、动作、幂等键和请求摘要做最终裁决。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO recoverCapabilityTaskAction(BusinessTaskActionDTO dto) {
        return taskCommandCoordinator.recoverCapabilityTaskAction(dto);
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
        return taskCommandCoordinator.resubmit(dto);
    }

    /**
     * 发起人从业务记录撤回运行中的审批流程，兼容新版应用级流程和旧版主流程。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO withdrawDocumentFlow(BusinessFlowWithdrawDTO dto) {
        return taskCommandCoordinator.withdrawDocumentFlow(dto);
    }


    /**
     * 节点表单 / formRef 只要身份字段；避免把整份设计器 schema 拷进响应组装路径。
     */
   private List<Map<String, Object>> collectTaskFormAssets(String objectCode) {
        return formAssetCatalog.collectTaskFormAssets(objectCode);
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
    @Transactional(rollbackFor = Exception.class)
    public void saveFlowBinding(String objectCode, BusinessFlowBindingDTO dto) {
        if (dto == null) {
            throw new BusinessException("流程绑定配置不能为空");
        }
        Long tenantId = resolveTenantId();
        String canonicalObjectCode = businessRuntimeContextResolver.resolveCanonicalObjectCode(tenantId, objectCode);
        JSONObject config = normalizeBindingConfig(dto);
        bindingViewAssembler.ensureBusinessBinding(config, tenantId, canonicalObjectCode);
        String flowModelKey = config.getString("flowModelKey");
        if (StringUtils.isBlank(flowModelKey)) {
            throw new BusinessException("流程模型Key不能为空");
        }
        ensureLowcodeFlowStatusField(tenantId, canonicalObjectCode);
        AiBusinessBinding existing = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", canonicalObjectCode, "FLOW");

        if (existing != null) {
            existing.setTargetType("OBJECT");
            existing.setTargetCode(canonicalObjectCode);
            existing.setBindingType("FLOW");
            existing.setBindingConfig(config.toJSONString());
            existing.setBindingKey(flowModelKey);
            existing.setBindingName(resolveBindingName(config));
            existing.setStatus(EnableStatus.ENABLED.getCode());
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
            binding.setStatus(EnableStatus.ENABLED.getCode());
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
    @Transactional(rollbackFor = Exception.class)
    public void saveFlowBinding(String objectCode, JSONObject config) {
        saveFlowBinding(objectCode, toDTO(config));
    }

    /**
     * 绑定入口也必须经过字段一致性检查。流程设计器之外的旧触发器入口此前只写
     * ai_business_binding，导致对象草稿/发布版本仍停留在不含 flowStatus 的快照。
     */
    private void ensureLowcodeFlowStatusField(Long tenantId, String objectCode) {
        if (flowStatusFieldService == null || businessObjectMapper == null || crudConfigMapper == null) {
            return;
        }
        AiBusinessObject object = businessObjectMapper.selectFirstByObjectCode(tenantId, objectCode);
        if (object == null || object.getId() == null) {
            return;
        }
        String lookup = StringUtils.firstNonBlank(object.getConfigKey(), objectCode);
        AiCrudConfig runtimeConfig = crudConfigMapper.selectRuntimeByObjectCodeOrConfigKey(tenantId, lookup);
        if (runtimeConfig == null || !"LOWCODE".equalsIgnoreCase(runtimeConfig.getBuildMode())) {
            return;
        }
        flowStatusFieldService.ensure(object.getId());
    }

    /**
     * 手动按钮发起单据流程。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlow(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startCoordinator.start(dto, true, null, null, tenantId, false, false));
    }

    /**
     * 受控 FLOW_ACTION 专用发起入口。业务 key 永远固定为 objectCode:recordId，
     * 远程成功而本地回填失败时，同 key 重试可恢复原流程实例。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlowForCapability(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startCoordinator.start(dto, true, null, null, tenantId, true, false));
    }

    /**
     * 旧审批入口兼容发起。接口层仍有旧权限校验，这里不再重复要求新流程按钮权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlowForCompatibility(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startCoordinator.start(dto, false, null, null, tenantId, false, false));
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
                () -> startCoordinator.start(dto, false, userId, userName,
                        effectiveTenantId, false, false));
    }

    /**
     * 应用级业务流程审批节点发起 Flowable。显式模型 Key 来自已发布/草稿画布，
     * 业务对象运行配置允许尚未发布的工作台草稿。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startFromBusinessProcess(String flowModelKey, String businessKey, String title,
                                                          Long userId, String userName, Long tenantId, JSONObject variables) {
        if (StringUtils.isBlank(flowModelKey)) {
            throw new BusinessException("审批节点未配置已发布流程模型");
        }
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
                () -> startCoordinator.start(dto, false, userId, userName,
                        effectiveTenantId, false, true));
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
        String canonicalObjectCode = businessRuntimeContextResolver.resolveCanonicalObjectCode(
                resolveTenantId(), objectCode);
        String businessKey = buildBusinessKey(canonicalObjectCode, recordId);
        AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectLatestByBusinessKey(resolveTenantId(), businessKey);
        if (link == null) {
            BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
            vo.setObjectCode(canonicalObjectCode);
            vo.setRecordId(recordId);
            vo.setBusinessKey(businessKey);
            vo.setFlowStatus(BusinessDocumentFlowStatus.NOT_STARTED.getCode());
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
        callbackCoordinator.handleCallback(dto);
    }

    @FlowCallback(on = {
            FlowCallback.ON_TASK_CREATED,
            FlowCallback.ON_TASK_COMPLETED,
            FlowCallback.ON_COMPLETED,
            FlowCallback.ON_REJECTED,
            FlowCallback.ON_CANCELED
    })
    @Transactional(rollbackFor = Exception.class)
    public void handleFlowEngineEvent(FlowEventContext ctx) {
        if (ctx == null) {
            return;
        }
        if (FlowCallback.ON_TASK_CREATED.equals(ctx.getEvent())
                || FlowCallback.ON_TASK_COMPLETED.equals(ctx.getEvent())) {
            taskEventCoordinator.handleTaskEvent(ctx);
            return;
        }
        callbackCoordinator.handleEngineEvent(ctx);
    }

    /**
     * 任务级事件只维护流程运行期间的单据中间态（待修改 / 流程中）。
     * <p>
     * 终态一律由流程结束事件裁决，这里不写结束状态；单据状态同步失败也不能让审批动作失败，
     * 因此异常只记录日志，由发起人修改节点保存字段时的自愈逻辑兜底。
     */

    private AiCrudConfig resolvePublishedRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        return runtimeConfigResolver.published(tenantId, objectCodeOrConfigKey);
    }

    private AiCrudConfig resolveRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        return runtimeConfigResolver.runtime(tenantId, objectCodeOrConfigKey);
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
            String resolved = StringUtils.trimToNull(
                    BusinessApprovalTitleRenderer.render(template, recordData, Map.of(), null));
            if (resolved != null) {
                return resolved;
            }
        }
        String displayField = object == null ? null : StringUtils.trimToNull(object.getDisplayField());
        Object displayValue = displayField == null ? null : read(recordData, displayField);
        if (displayValue != null && StringUtils.isNotBlank(String.valueOf(displayValue))) {
            return String.valueOf(displayValue);
        }
        for (String field : List.of("orderNo", "businessNo", "title", "name", "code")) {
            Object value = read(recordData, field);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
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

    private Long resolveActiveOrgId() {
        try {
            return SessionHelper.getActiveOrgId();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUsername() {
        try {
            var loginUser = SessionHelper.getLoginUser();
            if (loginUser != null) {
                return StringUtils.firstNonBlank(loginUser.getRealName(), loginUser.getUsername());
            }
            return SessionHelper.getUsername();
        } catch (Exception e) {
            return null;
        }
    }

}
