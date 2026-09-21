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
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcessRun;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowWithdrawDTO;
import com.mdframe.forge.plugin.generator.enums.BusinessDocumentFlowStatus;
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
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessRunMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessApprovalResultEvent;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
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
import com.mdframe.forge.starter.core.enums.EnableStatus;

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
    private static final Set<String> SERVER_OWNED_FLOW_VARIABLES = Set.of(
            "objectCode", "configKey", "recordId", "businessKey",
            "documentBusinessKey", "recordBusinessKey", "flowBusinessKey");
    /** 流程运行期间允许任务事件改写的单据状态，终态不在其中。 */
    private static final Set<String> RUNNING_DOCUMENT_STATUS_KEYS = Set.of(
            "DRAFT", "SUBMITTED", "IN_PROCESS", "NEED_MODIFY");

    @Autowired(required = false)
    private FlowClient flowClient;

    /**
     * 应用页面表单资产属于应用设计快照，不属于业务对象本身。
     * 使用字段注入保持已有测试/插件扩展的构造器兼容性。
     */
    @Autowired(required = false)
    private BusinessApplicationService businessApplicationService;

    /**
     * 业务流程运行和应用归属用于向前端返回服务端确认的打印身份。
     * 使用字段注入保持已有扩展和单元测试的构造器兼容性。
     */
    @Autowired(required = false)
    private BusinessProcessRunMapper businessProcessRunMapper;

    @Autowired(required = false)
    private BusinessApplicationObjectMapper businessApplicationObjectMapper;

    private final BusinessBindingMapper bindingMapper;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessDocumentRuntimeService documentRuntimeService;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessFieldDesignService businessFieldDesignService;
    private final BusinessFlowVariableResolver variableResolver;
    private final BusinessCodeFormProviderRegistry codeFormProviderRegistry;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final ObjectProvider<BusinessActionExecutionService> actionExecutionServiceProvider;
    private final Map<String, ReentrantLock> localFlowStartLocks = new ConcurrentHashMap<>();

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
        Map<String, Object> flowVariables = buildFlowVariables(bindingConfig, recordData);
        mergeRequestedFlowVariables(flowVariables, requestedVariables);

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
        if (items == null || items.isEmpty()) {
            return;
        }
        Long tenantId = resolveTenantId();
        Map<String, AiBusinessFlowInstanceLink> linkByBusinessKey = loadLinksByBusinessKey(tenantId, items);
        Map<String, BusinessRuntimeContext> contextCache = new HashMap<>();
        Map<String, AiBusinessObject> objectLookupCache = new HashMap<>();
        Map<String, BusinessListGroup> grouped = new LinkedHashMap<>();
        for (FlowBusinessListDisplayItem item : items) {
            if (item == null) {
                continue;
            }
            AiBusinessFlowInstanceLink link = linkByBusinessKey.get(StringUtils.trimToEmpty(item.getBusinessKey()));
            Map<String, Object> snapshotParams = readBusinessParamsSnapshot(link);
            if (!snapshotParams.isEmpty() && (item.getBusinessParams() == null || item.getBusinessParams().isEmpty())) {
                item.setBusinessParams(snapshotParams);
            }
            BusinessTaskFormContextQueryDTO itemQuery = new BusinessTaskFormContextQueryDTO();
            itemQuery.setObjectCode(item.getObjectCode());
            String snapshotConfigKey = firstNonBlankValue(
                    extractSnapshotValue(link, "configKey"),
                    extractSnapshotValue(link, "runtimeConfigKey"),
                    item.getBusinessParams() == null ? null : item.getBusinessParams().get("configKey"));
            itemQuery.setConfigKey(snapshotConfigKey);
            itemQuery.setSuiteCode(item.getBusinessParams() == null
                    ? null : textValue(item.getBusinessParams().get("suiteCode")));
            String hintedObjectCode = StringUtils.firstNonBlank(
                    link == null ? null : link.getObjectCode(),
                    StringUtils.trimToNull(item.getObjectCode()),
                    parseBusinessKeyObjectCode(item.getBusinessKey()));
            AiBusinessObject taskObject = null;
            if (StringUtils.isBlank(snapshotConfigKey) || StringUtils.isBlank(hintedObjectCode)) {
                String objectLookupKey = buildBusinessListObjectLookupKey(itemQuery, link, hintedObjectCode);
                taskObject = objectLookupCache.computeIfAbsent(objectLookupKey,
                        ignored -> resolveTaskBusinessObject(tenantId, itemQuery, link));
            }
            String objectCode = StringUtils.firstNonBlank(
                    taskObject == null ? null : taskObject.getObjectCode(),
                    hintedObjectCode);
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
            String runtimeLookupKey = StringUtils.firstNonBlank(
                    taskObject == null ? null : taskObject.getConfigKey(),
                    snapshotConfigKey,
                    objectCode);
            BusinessRuntimeContext context = contextCache.computeIfAbsent(runtimeLookupKey,
                    code -> resolveBusinessRuntimeContext(tenantId, code));
            String canonicalObjectCode = StringUtils.firstNonBlank(context.objectCode(), objectCode);
            String groupKey = StringUtils.firstNonBlank(context.configKey(), runtimeLookupKey, canonicalObjectCode);
            grouped.computeIfAbsent(groupKey,
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
        return getFormAssets(objectCode, includeInternal, null);
    }

    /**
     * 查询当前应用中当前业务对象实际可作为审批任务表单的页面资产。
     * applicationId 为空时保留对象级/代码表单兼容目录；有应用上下文时只返回该应用的真实页面表单，
     * 避免把业务对象字段注册表误显示成一个不存在的页面。
     */
    public Map<String, Object> getFormAssets(String objectCode,
                                             boolean includeInternal,
                                             Long applicationId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("objectCode", objectCode);
        result.put("formAssets", List.of());
        result.put("providerCatalog", List.of());
        result.put("warnings", List.of());
        if (StringUtils.isBlank(objectCode)) {
            return result;
        }

        if (applicationId != null && applicationId > 0 && businessApplicationService != null) {
            String canonicalObjectCode = resolveCanonicalObjectCode(resolveTenantId(), objectCode);
            Map<String, Object> applicationAssets = collectApplicationPageFormAssets(
                    applicationId, canonicalObjectCode);
            if (!applicationAssets.isEmpty()) {
                result.putAll(applicationAssets);
                result.put("providerCatalog", codeFormProviderRegistry.listProviderCatalog(
                        canonicalObjectCode, includeInternal));
                return result;
            }
            result.put("warnings", List.of("当前应用尚未配置属于该业务对象的表单页面，请先在应用中新增表单页面"));
            result.put("objectCode", canonicalObjectCode);
            result.put("providerCatalog", codeFormProviderRegistry.listProviderCatalog(
                    canonicalObjectCode, includeInternal));
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
        appendObjectFieldRegistryFallback(assets, object);
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
     * 从应用草稿中的页面节点和页面布局提取真实页面表单。
     * 表单 key 带应用/页面/资产三段稳定身份，运行时无需额外传 applicationId 即可重新解析页面。
     */
    private Map<String, Object> collectApplicationPageFormAssets(Long applicationId, String objectCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (applicationId == null || applicationId <= 0 || businessApplicationService == null) {
            return result;
        }
        try {
            var application = businessApplicationService.detail(applicationId);
            JSONObject options = readJsonObject(application.getOptions());
            JSONObject builder = readNestedObject(options.get("inAppBuilder"));
            JSONArray nodes = readNestedArray(builder.get("nodes"));
            JSONObject pages = readNestedObject(builder.get("pages"));
            JSONArray assets = readNestedArray(builder.get("formAssets"));
            if (nodes.isEmpty() || pages.isEmpty() || assets.isEmpty()) {
                return result;
            }
            Map<String, JSONObject> assetsById = new LinkedHashMap<>();
            for (int i = 0; i < assets.size(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (asset != null && StringUtils.isNotBlank(asset.getString("id"))) {
                    assetsById.put(asset.getString("id"), asset);
                }
            }
            List<Map<String, Object>> formAssets = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                if (node == null || !"page".equalsIgnoreCase(node.getString("type"))) {
                    continue;
                }
                JSONObject objectRef = readNestedObject(node.get("objectRef"));
                String pageObjectCode = StringUtils.firstNonBlank(
                        objectRef.getString("objectCode"), node.getString("objectCode"));
                if (!matchesApplicationObject(applicationId, objectCode, pageObjectCode, objectRef)) {
                    continue;
                }
                String pageId = StringUtils.trimToNull(node.getString("id"));
                if (pageId == null) {
                    continue;
                }
                JSONObject page = pages.getJSONObject(pageId);
                if (page == null) {
                    continue;
                }
                Set<String> referencedAssetIds = new LinkedHashSet<>();
                collectFormAssetIds(page, referencedAssetIds);
                String directAssetId = StringUtils.firstNonBlank(
                        node.getString("formAssetId"), objectRef.getString("formAssetId"));
                if (directAssetId != null) {
                    referencedAssetIds.add(directAssetId);
                }
                // Older object pages were persisted as a CRUD block without a
                // formAssetId on the block.  The application still owns the
                // form asset in inAppBuilder.formAssets, and when there is one
                // unambiguous asset it is the page's default task form.  Keep
                // this fallback here so historical pages can participate in
                // business-process task forms without asking users to rebind
                // the page manually.
                if (referencedAssetIds.isEmpty()) {
                    String defaultAssetId = resolveDefaultPageFormAssetId(
                            node, objectRef, assets);
                    if (defaultAssetId != null) {
                        referencedAssetIds.add(defaultAssetId);
                    }
                }
                for (String assetId : referencedAssetIds) {
                    JSONObject source = assetsById.get(assetId);
                    if (source == null) {
                        continue;
                    }
                    Map<String, Object> item = buildApplicationPageFormAsset(
                            applicationId, objectCode, node, objectRef, source, pageId);
                    String formKey = StringUtils.trimToNull(textValue(item.get("formKey")));
                    if (formKey != null && seen.add(formKey)) {
                        formAssets.add(item);
                    }
                }
            }
            if (formAssets.isEmpty()) {
                return result;
            }
            result.put("applicationId", String.valueOf(applicationId));
            result.put("objectCode", objectCode);
            result.put("formAssets", formAssets);
            result.put("warnings", List.of());
        } catch (Exception error) {
            log.debug("读取应用页面表单资产失败: applicationId={}, objectCode={}", applicationId, objectCode, error);
        }
        return result;
    }

    /**
     * 兼容业务对象编码唯一化前保存的页面引用。页面仍携带稳定 configKey 时，
     * 即使页面上的 objectCode 是旧编码，也应通过 configKey 解析到规范编码；
     * 解析结果必须与请求对象是同一个对象，否则会把应用内其它业务对象的
     * 页面表单误纳入当前对象的候选任务表单。
     */
    private boolean matchesApplicationObject(Long applicationId,
                                             String requestedObjectCode,
                                             String pageObjectCode,
                                             JSONObject objectRef) {
        if (StringUtils.equals(requestedObjectCode, pageObjectCode)) {
            return true;
        }
        if (applicationId == null || StringUtils.isBlank(requestedObjectCode) || objectRef == null) {
            return false;
        }
        String configKey = StringUtils.trimToNull(objectRef.getString("configKey"));
        if (configKey == null) {
            return false;
        }
        AiBusinessObject canonical = businessObjectMapper.selectByConfigKey(resolveTenantId(), configKey);
        return canonical != null
                && StringUtils.equals(canonical.getObjectCode(), requestedObjectCode);
    }

    private String resolveDefaultPageFormAssetId(JSONObject pageNode,
                                                  JSONObject objectRef,
                                                  JSONArray assets) {
        if (assets == null || assets.isEmpty()) {
            return null;
        }
        String requestedFormKey = StringUtils.firstNonBlank(
                pageNode == null ? null : pageNode.getString("formKey"),
                pageNode == null ? null : pageNode.getString("defaultFormKey"),
                objectRef == null ? null : objectRef.getString("formKey"),
                objectRef == null ? null : objectRef.getString("defaultFormKey"));
        if (requestedFormKey != null) {
            for (int i = 0; i < assets.size(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (asset != null && StringUtils.equals(requestedFormKey,
                        StringUtils.firstNonBlank(asset.getString("formKey"), asset.getString("id")))) {
                    return StringUtils.trimToNull(asset.getString("id"));
                }
            }
        }
        String markedDefault = null;
        String onlyAsset = null;
        int assetCount = 0;
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (asset == null || StringUtils.isBlank(asset.getString("id"))) {
                continue;
            }
            assetCount++;
            onlyAsset = asset.getString("id");
            if (Boolean.TRUE.equals(asset.getBoolean("default"))
                    || Boolean.TRUE.equals(asset.getBoolean("isDefault"))) {
                markedDefault = asset.getString("id");
            }
        }
        if (markedDefault != null) {
            return markedDefault;
        }
        return assetCount == 1 ? onlyAsset : null;
    }

    private Map<String, Object> buildApplicationPageFormAsset(Long applicationId,
                                                               String canonicalObjectCode,
                                                               JSONObject pageNode,
                                                               JSONObject objectRef,
                                                               JSONObject source,
                                                               String pageId) {
        Map<String, Object> item = new LinkedHashMap<>();
        String sourceAssetId = StringUtils.defaultIfBlank(source.getString("id"), "default");
        String formKey = "app_" + applicationId + "_page_" + pageId + "_form_" + sourceAssetId;
        String pageName = StringUtils.firstNonBlank(
                pageNode.getString("pageName"),
                pageNode.getString("name"),
                pageNode.getString("title"),
                pageId);
        String objectName = StringUtils.firstNonBlank(
                objectRef.getString("objectName"), pageName, objectRef.getString("objectCode"));
        JSONObject formDesignerSchema = readNestedObject(source.get("formDesignerSchema"));
        JSONObject schema = readNestedObject(source.get("schema"));
        if (schema.isEmpty()) {
            schema = formDesignerSchema;
        }
        List<Map<String, Object>> fields = readMapList(readNestedArray(source.get("fieldCatalog")));
        if (fields.isEmpty()) {
            fields = readMapList(readNestedArray(source.get("fields")));
        }
        if (fields.isEmpty()) {
            fields = collectBusinessFormFieldCatalog(schema);
        }
        fields = new ArrayList<>(fields);
        appendSchemaChildTableFields(schema, fields);
        appendRuntimeChildFieldCatalog(StringUtils.trimToNull(objectRef.getString("configKey")), fields);
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("applicationId", String.valueOf(applicationId));
        item.put("objectId", objectRef.getString("objectId"));
        item.put("objectCode", StringUtils.firstNonBlank(
                canonicalObjectCode, objectRef.getString("objectCode")));
        item.put("objectName", objectName);
        item.put("configKey", objectRef.getString("configKey"));
        item.put("formKey", formKey);
        item.put("sourceFormKey", source.getString("formKey"));
        item.put("formName", StringUtils.firstNonBlank(
                source.getString("formName"), source.getString("name"), pageName, objectName + "表单"));
        item.put("pageId", pageId);
        item.put("pageCode", StringUtils.firstNonBlank(pageNode.getString("pageCode"), pageNode.getString("pageKey"), pageId));
        item.put("pageName", pageName);
        item.put("pageType", pageNode.getString("pageType"));
        item.put("source", "applicationPage");
        item.put("sourceType", "applicationPageForm");
        item.put("fieldCatalog", fields);
        item.put("fields", fields);
        item.put("fieldCount", fields.size());
        item.put("fieldPreview", buildFieldPreview(fields));
        item.put("supportsSave", true);
        schema.put("formKey", formKey);
        schema.put("formName", item.get("formName"));
        schema.put("fieldCatalog", fields);
        schema.put("fields", fields);
        item.put("schema", schema);
        return item;
    }

    private void collectFormAssetIds(Object value, Set<String> result) {
        if (value instanceof Map<?, ?> map) {
            Object propsValue = map.get("props");
            if (propsValue instanceof Map<?, ?> props && props.get("formAssetId") != null) {
                String id = StringUtils.trimToNull(String.valueOf(props.get("formAssetId")));
                if (id != null) {
                    result.add(id);
                }
            }
            map.values().forEach(child -> collectFormAssetIds(child, result));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(child -> collectFormAssetIds(child, result));
        }
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
            binding.setStatus(EnableStatus.ENABLED.getCode());
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
        Map<String, Object> taskFormInfo = loadTaskFormInfo(effectiveQuery.getTaskId());
        validateTaskAccess(effectiveQuery, false, taskFormInfo);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, false, taskFormInfo);
        return attachPrintRuntimeIdentity(
                buildTaskFormContext(effectiveQuery, runtime, taskFormInfo), effectiveQuery);
    }

    /**
     * 查询当前用户已签收、可直接办理的待办上下文。
     *
     * <p>该只读校验入口供受控流程动作在 elicitation 前确认真实办理权使用，
     * 候选但未签收的任务不会被视为可办理任务。</p>
     */
    public BusinessTaskFormContextVO getActionableTaskFormContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        Map<String, Object> taskFormInfo = loadTaskFormInfo(effectiveQuery.getTaskId());
        validateTaskAccess(effectiveQuery, true, taskFormInfo);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, true, taskFormInfo);
        return attachPrintRuntimeIdentity(
                buildTaskFormContext(effectiveQuery, runtime, taskFormInfo), effectiveQuery);
    }

    /**
     * 查询历史/已办场景下的业务表单上下文，只用于只读展示，不校验运行中待办任务身份。
     */
    public BusinessTaskFormContextVO getTaskFormReadonlyContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, false);
        BusinessTaskFormContextVO context = attachPrintRuntimeIdentity(
                buildTaskFormContext(effectiveQuery, runtime), effectiveQuery);
        makeBusinessTaskFormReadonly(context);
        return context;
    }

    /**
     * 保存待办任务允许编辑的业务字段，并返回最新上下文。
     */
    @Transactional(rollbackFor = Exception.class)
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
        query.setObjectId(dto.getObjectId());
        query.setConfigKey(dto.getConfigKey());
        query.setSuiteCode(dto.getSuiteCode());
        query.setRecordId(dto.getRecordId());
        query.setFormKey(dto.getFormKey());

        Map<String, Object> taskFormInfo = loadTaskFormInfo(query.getTaskId());
        validateTaskAccess(query, true, taskFormInfo);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true, taskFormInfo);
        repairInitiatorModifyState(query, runtime, taskFormInfo);
        JSONObject nodeForm = resolveTaskNodeForm(runtime, query, taskFormInfo);
        TaskFormSaveResult saveResult = persistTaskFormData(dto, query, runtime, nodeForm);
        if (saveResult.context() != null) {
            return attachPrintRuntimeIdentity(saveResult.context(), query);
        }
        return attachPrintRuntimeIdentity(
                buildTaskFormContext(query, saveResult.runtime(), taskFormInfo), query);
    }

    private BusinessTaskFormContextVO attachPrintRuntimeIdentity(
            BusinessTaskFormContextVO context,
            BusinessTaskFormContextQueryDTO query) {
        if (context == null) {
            return null;
        }
        if (query != null) {
            if (StringUtils.isBlank(context.getProcessInstanceId())) {
                context.setProcessInstanceId(StringUtils.trimToNull(query.getProcessInstanceId()));
            }
            if (StringUtils.isBlank(context.getObjectCode())) {
                context.setObjectCode(StringUtils.trimToNull(query.getObjectCode()));
            }
            if (StringUtils.isBlank(context.getConfigKey())) {
                context.setConfigKey(StringUtils.trimToNull(query.getConfigKey()));
            }
        }

        Long tenantId = resolveTenantId();
        String processInstanceId = StringUtils.trimToNull(context.getProcessInstanceId());
        AiBusinessProcessRun run = businessProcessRunMapper == null || processInstanceId == null
                ? null
                : businessProcessRunMapper.selectByProcessInstanceId(tenantId, processInstanceId);
        if (run != null) {
            context.setProcessRunId(run.getId());
            if (run.getApplicationId() != null) {
                String runApplicationId = String.valueOf(run.getApplicationId());
                if (StringUtils.isNotBlank(context.getApplicationId())
                        && !StringUtils.equals(context.getApplicationId(), runApplicationId)) {
                    context.getWarnings().add("流程运行应用身份与表单页面不一致，打印将使用流程运行版本");
                }
                context.setApplicationId(runApplicationId);
            }
        }

        String objectCode = StringUtils.trimToNull(context.getObjectCode());
        if (StringUtils.isBlank(context.getApplicationId())
                && objectCode != null
                && businessApplicationObjectMapper != null) {
            List<Long> applicationIds = businessApplicationObjectMapper
                    .selectPublishedApplicationIdsByObjectIdentity(
                            tenantId, objectCode, StringUtils.trimToNull(context.getConfigKey()));
            if (applicationIds != null && applicationIds.size() == 1) {
                context.setApplicationId(String.valueOf(applicationIds.get(0)));
            } else if (applicationIds != null && applicationIds.size() > 1) {
                context.getWarnings().add("业务对象归属多个已发布应用，无法确定流程打印模板范围");
            }
        }
        return context;
    }

    private TaskFormSaveResult persistTaskFormData(BusinessTaskFormSaveDTO dto,
                                                   BusinessTaskFormContextQueryDTO query,
                                                   TaskFormRuntimeContext runtime,
                                                   JSONObject nodeForm) {
        if (nodeForm == null || nodeForm.isEmpty()) {
            throw new BusinessException("当前流程节点未配置业务表单权限");
        }
        String formMode = normalizeNodeFormMode(nodeForm.getString("formMode"));
        if ("BUSINESS_CODE_FORM".equals(formMode)) {
            List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
            BusinessTaskFormSaveDTO filteredDto = filterSaveDataByPermissions(dto, permissions);
            validateRequiredTaskFields(permissions, filteredDto.getData(), dto.getData() == null ? Map.of() : dto.getData());
            return new TaskFormSaveResult(runtime, saveBusinessCodeFormContext(filteredDto, nodeForm));
        }
        if (!"BUSINESS_OBJECT_FORM".equals(formMode)) {
            throw new BusinessException("当前节点不是平台可保存的业务表单，不能通过平台保存业务字段");
        }
        List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        if (permissions.isEmpty()) {
            String formKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(query.getFormKey()),
                    StringUtils.trimToNull(nodeForm.getString("formKey")));
            BusinessObjectVO object = queryBusinessObject(resolveTenantId(), runtime.objectCode(), runtime.configKey());
            JSONObject formSchema = resolveBusinessFormSchema(object, formKey, runtime.configKey());
            List<Map<String, Object>> fieldCatalog = resolveBusinessTaskCrudPageFields(runtime.configKey(), formKey, formSchema);
            permissions = normalizeBusinessObjectTaskPermissions(fieldCatalog, permissions);
        }
        List<Map<String, Object>> childrenConfig = resolveBusinessTaskChildrenConfig(runtime.configKey(), nodeForm);
        Map<String, DynamicCrudService.TaskChildPermission> childPermissions = buildTaskChildPermissions(childrenConfig, nodeForm);
        Set<String> writableFields = collectPermissionFields(permissions, "writable", true);
        boolean hasWritableChildren = childPermissions.values().stream()
                .anyMatch(permission -> !permission.writableFields().isEmpty()
                        || permission.allowCreate() || permission.allowUpdate() || permission.allowDelete());
        if (writableFields.isEmpty() && !hasWritableChildren) {
            throw new BusinessException("当前节点没有可编辑业务字段");
        }

        Map<String, Object> input = dto.getData() == null ? Map.of() : dto.getData();
        Map<String, Object> updateData = new LinkedHashMap<>();
        Map<String, Object> mainInput = extractTaskMainPayload(input);
        Map<String, Object> childrenInput = extractTaskChildrenPayload(input);
        for (String field : writableFields) {
            if (mainInput.containsKey(field)) {
                updateData.put(field, mainInput.get(field));
            }
        }
        validateRequiredTaskFields(permissions, updateData, mainInput);
        if (updateData.isEmpty() && childrenInput.isEmpty()) {
            throw new BusinessException("未提交可编辑业务字段");
        }

        if (runtime.recordId() == null) {
            if (!childrenInput.isEmpty()) {
                throw new BusinessException("业务待办尚未关联主记录，暂不支持新增子表明细");
            }
            if (StringUtils.isBlank(runtime.configKey())) {
                throw new BusinessException("业务对象缺少已发布运行配置，无法保存业务字段");
            }
            Map<String, Object> created = dynamicCrudService.insertInternal(runtime.configKey(), updateData);
            Long createdId = extractCreatedRecordId(created);
            if (createdId == null) {
                throw new BusinessException("保存业务单据失败");
            }
            ensureRuntimeLink(runtime, query, createdId);
            query.setRecordId(createdId);
            query.setObjectCode(runtime.objectCode());
            query.setBusinessKey(buildBusinessKey(runtime.objectCode(), createdId));
            TaskFormRuntimeContext createdRuntime = new TaskFormRuntimeContext(
                    runtime.objectCode(), createdId, query.getBusinessKey(), runtime.configKey(), runtime.bindingConfig());
            return new TaskFormSaveResult(createdRuntime, null);
        }

        Map<String, Object> taskData = new LinkedHashMap<>();
        taskData.put("main", updateData);
        if (!childrenInput.isEmpty()) {
            taskData.put("children", childrenInput);
        }
        dynamicCrudService.updateTaskEditableData(runtime.configKey(), runtime.recordId(), taskData,
                writableFields, childPermissions);
        return new TaskFormSaveResult(runtime, null);
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
        if (!"approve".equals(action) && !"reject".equals(action)
                && !"rejecttostart".equals(action) && !"return".equals(action)) {
            throw new BusinessException("当前业务待办仅支持同意、驳回、驳回至发起人或退回");
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
        query.setObjectId(dto.getObjectId());
        query.setConfigKey(dto.getConfigKey());
        query.setSuiteCode(dto.getSuiteCode());
        query.setRecordId(dto.getRecordId());
        query.setFormKey(dto.getFormKey());

        Map<String, Object> taskFormInfo = loadTaskFormInfo(query.getTaskId());
        validateTaskAccess(query, true, taskFormInfo);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true, taskFormInfo);
        if (dto.getData() != null && !dto.getData().isEmpty()) {
            JSONObject nodeForm = resolveTaskNodeForm(runtime, query, taskFormInfo);
            TaskFormSaveResult saveResult = persistTaskFormData(
                    toTaskFormSaveDTO(dto, query), query, runtime, nodeForm);
            runtime = saveResult.runtime();
        }
        Map<String, Object> variables = dto.getVariables() == null ? Map.of() : dto.getVariables();
        String userId = String.valueOf(resolveUserId());

        FlowResult<Void> result;
        if ("rejecttostart".equals(action)) {
            result = flowClient.rejectToStart(query.getTaskId(), userId, dto.getComment(), dto.getSignature(),
                    resolveTrustedTaskTenant(dto), dto.getIdempotencyKey(), dto.getRequestDigest());
        } else if ("reject".equals(action)) {
            result = flowClient.reject(query.getTaskId(), userId, dto.getComment(), dto.getSignature(),
                    resolveTrustedTaskTenant(dto), dto.getIdempotencyKey(), dto.getRequestDigest());
        } else if ("return".equals(action)) {
            result = flowClient.returnTask(query.getTaskId(), userId, dto.getComment(), dto.getSignature(),
                    StringUtils.trimToNull(dto.getTargetActivityId()));
        } else {
            result = flowClient.approve(query.getTaskId(), userId, dto.getComment(), dto.getSignature(), variables,
                    resolveTrustedTaskTenant(dto), dto.getIdempotencyKey(), dto.getRequestDigest(),
                    dto.getApprovalPointResults());
        }
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null
                    ? "业务待办办理失败"
                    : StringUtils.defaultIfBlank(result.getMsg(), "业务待办办理失败"));
        }

        return syncBusinessFlowStatusAfterTaskAction(runtime, query, action, variables);
    }

    private BusinessTaskFormSaveDTO toTaskFormSaveDTO(BusinessTaskActionDTO dto,
                                                      BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormSaveDTO saveDTO = new BusinessTaskFormSaveDTO();
        saveDTO.setTaskId(query.getTaskId());
        saveDTO.setBusinessKey(query.getBusinessKey());
        saveDTO.setProcessInstanceId(query.getProcessInstanceId());
        saveDTO.setProcessDefKey(query.getProcessDefKey());
        saveDTO.setTaskDefKey(query.getTaskDefKey());
        saveDTO.setObjectCode(query.getObjectCode());
        saveDTO.setObjectId(query.getObjectId());
        saveDTO.setConfigKey(query.getConfigKey());
        saveDTO.setSuiteCode(query.getSuiteCode());
        saveDTO.setRecordId(query.getRecordId());
        saveDTO.setFormKey(query.getFormKey());
        saveDTO.setData(dto.getData());
        return saveDTO;
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
            vo.setFlowStatus(BusinessDocumentFlowStatus.IN_PROCESS.getCode());
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

        BusinessDocumentFlowStatus targetStatus = "reject".equals(action)
                || "rejecttostart".equals(action)
                ? BusinessDocumentFlowStatus.NEED_MODIFY
                : BusinessDocumentFlowStatus.IN_PROCESS;
        applyRunningFlowState(link, targetStatus);
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

    private String resolveFlowBusinessKeyForStart(String businessKey, AiBusinessFlowInstanceLink latest) {
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

        Map<String, Object> taskFormInfo = loadTaskFormInfo(query.getTaskId());
        validateTaskAccess(query, true, taskFormInfo);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true, taskFormInfo);
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
            vo.setFlowStatus(BusinessDocumentFlowStatus.IN_PROCESS.getCode());
            vo.setMessage("已重提");
            return vo;
        }

        applyRunningFlowState(link, BusinessDocumentFlowStatus.IN_PROCESS);
        // 修改节点已经办完，待办随之失效；重提后的新审批待办由任务创建事件重建。
        link.setVariablesSnapshot(BusinessFlowLinkRuntimeState.writeModifyTask(
                mergeLinkVariablesSnapshot(link, variables), null));
        flowInstanceLinkMapper.updateById(link);
        return toRuntimeVO(link, "已重提");
    }

    /**
     * 发起人从业务记录撤回运行中的审批流程，兼容新版应用级流程和旧版主流程。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO withdrawDocumentFlow(BusinessFlowWithdrawDTO dto) {
        if (dto == null) {
            throw new BusinessException("撤回参数不能为空");
        }
        Long tenantId = resolveTenantId();
        Long userId = resolveUserId();
        if (userId == null) {
            throw new BusinessException("当前用户未登录，无法撤回流程");
        }
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法撤回流程");
        }

        String objectCode = StringUtils.trimToNull(dto.getObjectCode());
        if (StringUtils.isNotBlank(objectCode)) {
            objectCode = resolveCanonicalObjectCode(tenantId, objectCode);
        }
        String businessKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(dto.getBusinessKey()),
                objectCode != null && dto.getRecordId() != null
                        ? buildBusinessKey(objectCode, dto.getRecordId()) : null);
        AiBusinessFlowInstanceLink link = findRuntimeLink(
                tenantId, StringUtils.trimToNull(dto.getProcessInstanceId()), businessKey);
        if (link == null) {
            throw new BusinessException("未找到可撤回的流程实例");
        }
        if (isEndedLink(link) || !isRunningFlowStatus(link.getFlowStatus())) {
            throw new BusinessException("当前流程已结束，不能撤回");
        }
        if (!userId.equals(link.getStartUserId())) {
            throw new BusinessException("只有流程发起人可以撤回");
        }

        FlowResult<Void> result = flowClient.withdrawProcess(
                link.getProcessInstanceId(),
                String.valueOf(userId),
                StringUtils.defaultIfBlank(dto.getComment(), "申请人撤回"));
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null
                    ? "撤回失败"
                    : StringUtils.defaultIfBlank(result.getMsg(), "撤回失败"));
        }

        BusinessFlowCallbackDTO callback = new BusinessFlowCallbackDTO();
        callback.setProcessInstanceId(link.getProcessInstanceId());
        callback.setBusinessKey(link.getBusinessKey());
        callback.setResult(BusinessDocumentFlowStatus.CANCELED.getCode());
        callback.setFlowStatus(BusinessDocumentFlowStatus.CANCELED.getCode());
        callback.setTenantId(link.getTenantId());
        callback.setOperatorId(userId);
        handleFlowCallbackInternal(link, callback);
        return toRuntimeVO(link, "流程已撤回");
    }

    private boolean isRunningFlowStatus(String flowStatus) {
        return BusinessDocumentFlowStatus.STARTED.matches(flowStatus)
                || BusinessDocumentFlowStatus.RUNNING.matches(flowStatus)
                || BusinessDocumentFlowStatus.IN_PROCESS.matches(flowStatus)
                || BusinessDocumentFlowStatus.NEED_MODIFY.matches(flowStatus);
    }

    private void validateTaskAccess(BusinessTaskFormContextQueryDTO query, boolean writeRequired) {
        validateTaskAccess(query, writeRequired, loadTaskFormInfo(query == null ? null : query.getTaskId()));
    }

    private void validateTaskAccess(BusinessTaskFormContextQueryDTO query,
                                    boolean writeRequired,
                                    Map<String, Object> task) {
        if (query == null) {
            throw new BusinessException("业务待办表单参数不能为空");
        }
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

        if (task == null || task.isEmpty()) {
            throw new BusinessException("任务不存在或无权访问");
        }
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
        String taskBusinessKey = StringUtils.trimToNull(textValue(task.get("businessKey")));
        if (isSyntheticTestBusinessKey(taskBusinessKey)) {
            // 发起测试尚未绑定低代码单据，后续按流程实例补建记录，不能用请求里的 objectCode:id 覆盖任务 Key。
            query.setBusinessKey(taskBusinessKey);
            if (query.getRecordId() != null && StringUtils.isBlank(query.getProcessInstanceId())) {
                query.setRecordId(null);
            }
        } else if (StringUtils.isBlank(query.getBusinessKey())) {
            query.setBusinessKey(taskBusinessKey);
        }
        if (StringUtils.isBlank(query.getTaskDefKey())) {
            query.setTaskDefKey(StringUtils.trimToNull(textValue(task.get("taskDefKey"))));
        }
        if (StringUtils.isBlank(query.getProcessDefKey())) {
            query.setProcessDefKey(StringUtils.trimToNull(textValue(task.get("processDefKey"))));
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
        // 流程模型「发起测试」写入 FLOW_TEST:modelKey:ts，此时还没有 objectCode:recordId。
        // 待办暂存会按业务对象补建单据，请求 Key 可能是测试 Key 或单据 Key，不能当成串单。
        if (isSyntheticTestBusinessKey(actual)) {
            return;
        }
        throw new BusinessException("业务Key与当前任务不匹配");
    }

    private boolean isSyntheticTestBusinessKey(String businessKey) {
        String text = StringUtils.trimToNull(businessKey);
        return text != null && (text.startsWith("FLOW_TEST:") || "FLOW_TEST".equals(text));
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
        return buildTaskFormContext(query, runtime, Map.of());
    }

    private BusinessTaskFormContextVO buildTaskFormContext(BusinessTaskFormContextQueryDTO query,
                                                           TaskFormRuntimeContext runtime,
                                                           Map<String, Object> taskFormInfo) {
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
        if (taskFormInfo != null && !taskFormInfo.isEmpty()) {
            vo.setTaskFormInfo(new LinkedHashMap<>(taskFormInfo));
        }

        if (StringUtils.isBlank(runtime.objectCode())) {
            vo.getWarnings().add("未解析到业务对象");
            return vo;
        }

        JSONObject nodeForm = resolveTaskNodeForm(runtime, query, taskFormInfo);
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
            applyPageFormIdentity(vo, vo.getFormRef());
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
        BusinessObjectVO object = queryBusinessObject(resolveTenantId(), runtime.objectCode(), runtime.configKey());
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
        Map<String, Object> recordData = runtime.recordId() == null
                ? loadTaskVariablesAsRecord(query, taskFormInfo)
                : dynamicCrudService.selectById(runtime.configKey(), runtime.recordId());
        Map<String, Object> visibleRecordData = filterVisibleRecordData(recordData, fields);
        List<Map<String, Object>> childrenConfig = resolveBusinessTaskChildrenConfig(runtime.configKey(), nodeForm);
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
        applyPageFormIdentity(vo, vo.getFormRef());
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

    private void applyPageFormIdentity(BusinessTaskFormContextVO vo, Map<String, Object> formRef) {
        if (vo == null || formRef == null || formRef.isEmpty()) {
            return;
        }
        vo.setApplicationId(StringUtils.trimToNull(textValue(formRef.get("applicationId"))));
        vo.setPageId(StringUtils.trimToNull(textValue(formRef.get("pageId"))));
        vo.setPageCode(StringUtils.trimToNull(textValue(formRef.get("pageCode"))));
        vo.setPageName(StringUtils.trimToNull(textValue(formRef.get("pageName"))));
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
        List<Map<String, Object>> fallback = new ArrayList<>(collectBusinessFormFieldCatalog(formSchema));
        appendRuntimeChildFieldCatalog(configKey, fallback);
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
            List<Map<String, Object>> result = new ArrayList<>(layoutFields.isEmpty() ? runtimeFields : layoutFields);
            appendRuntimeChildFieldCatalog(options, result);
            return result;
        } catch (Exception e) {
            log.debug("读取动态 CRUD 详情表单 schema 失败: configKey={}, error={}", configKey, e.getMessage());
            return fallback;
        }
    }

    private void appendRuntimeChildFieldCatalog(String configKey, List<Map<String, Object>> fields) {
        if (StringUtils.isBlank(configKey) || fields == null) {
            return;
        }
        try {
            AiCrudConfig runtimeConfig = resolveRuntimeConfig(resolveTenantId(), configKey);
            if (runtimeConfig == null) {
                runtimeConfig = resolvePublishedRuntimeConfig(resolveTenantId(), configKey);
            }
            appendRuntimeChildFieldCatalog(readJsonObject(runtimeConfig == null ? null : runtimeConfig.getOptions()), fields);
        } catch (Exception e) {
            log.debug("读取待办子表字段目录失败: configKey={}, error={}", configKey, e.getMessage());
        }
    }

    private void appendRuntimeChildFieldCatalog(JSONObject options, List<Map<String, Object>> fields) {
        if (options == null || fields == null) {
            return;
        }
        Set<String> seen = fields.stream()
                .map(field -> StringUtils.firstNonBlank(textValue(field.get("field")), textValue(field.get("fieldCode"))))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        JSONObject masterDetailConfig = readNestedObject(options.get("masterDetailConfig"));
        for (Map<String, Object> child : readMapList(readNestedArray(masterDetailConfig.get("children")))) {
            String childKey = resolveBusinessTaskChildKey(child);
            for (Map<String, Object> rawField : readMapList(readNestedArray(child.get("fields")))) {
                Map<String, Object> field = normalizeRuntimeCrudFormField(rawField);
                if (field == null) {
                    continue;
                }
                String childField = StringUtils.firstNonBlank(
                        textValue(field.get("field")), textValue(field.get("fieldCode")));
                String permissionField = childKey + "." + childField;
                if (StringUtils.isBlank(childKey) || StringUtils.isBlank(childField) || !seen.add(permissionField)) {
                    continue;
                }
                field.put("scope", "child");
                field.put("childKey", childKey);
                field.put("childField", childField);
                field.put("childLabel", StringUtils.firstNonBlank(
                        textValue(child.get("label")),
                        textValue(child.get("modelName")),
                        textValue(child.get("relationName")),
                        childKey));
                field.put("relationName", StringUtils.firstNonBlank(
                        textValue(child.get("relationName")),
                        textValue(child.get("modelName")),
                        childKey));
                field.put("field", childField);
                field.put("fieldCode", childField);
                field.put("label", StringUtils.defaultIfBlank(textValue(rawField.get("label")), childField));
                fields.add(field);
            }
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

    private List<Map<String, Object>> resolveBusinessTaskChildrenConfig(String configKey, JSONObject nodeForm) {
        if (StringUtils.isBlank(configKey)) {
            return List.of();
        }
        try {
            AiCrudConfig runtimeConfig = dynamicCrudService.getRuntimeConfig(configKey);
            JSONObject options = runtimeConfig == null ? new JSONObject() : readJsonObject(runtimeConfig.getOptions());
            JSONObject masterDetailConfig = readNestedObject(options.get("masterDetailConfig"));
            List<Map<String, Object>> rawChildren = readMapList(readNestedArray(masterDetailConfig.get("children"))).stream()
                    .filter(this::isBusinessTaskDetailChild)
                    .toList();
            Map<String, Map<String, Object>> childPermissions = normalizeTaskChildPermissionMap(nodeForm);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> rawChild : rawChildren) {
                String childKey = resolveBusinessTaskChildKey(rawChild);
                Map<String, Object> childPermission = findChildPermission(childPermissions, childKey);
                if (childPermission != null && !readBooleanValue(childPermission.get("readable"), true)) {
                    continue;
                }
                Map<String, Object> child = new LinkedHashMap<>(rawChild);
                boolean fieldWritable = hasWritableTaskChildField(rawChild, nodeForm, childKey);
                child.put("allowCreate", childPermission != null
                        && readBooleanValue(childPermission.get("allowCreate"), false));
                // 字段勾了可编辑，已有行也要能改。行级「修改」只是额外开关，不能把字段权限盖掉。
                child.put("allowUpdate", fieldWritable || (childPermission != null
                        && readBooleanValue(childPermission.get("allowUpdate"), false)));
                child.put("allowDelete", childPermission != null
                        && readBooleanValue(childPermission.get("allowDelete"), false));
                child.put("readable", true);
                List<Map<String, Object>> visibleFields = applyTaskChildFieldPermissions(
                        readMapList(readNestedArray(rawChild.get("fields"))), nodeForm, childKey);
                child.put("fields", visibleFields);
                if (!visibleFields.isEmpty()) {
                    result.add(child);
                }
            }
            return result;
        } catch (Exception e) {
            log.debug("读取业务表单子表配置失败: configKey={}, error={}", configKey, e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> applyTaskChildFieldPermissions(List<Map<String, Object>> fields,
                                                                       JSONObject nodeForm,
                                                                       String childKey) {
        Map<String, Map<String, Object>> permissions = normalizeTaskFieldPermissionMap(nodeForm);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> source : fields) {
            String field = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(source.get("field"))),
                    StringUtils.trimToNull(textValue(source.get("fieldCode"))),
                    StringUtils.trimToNull(textValue(source.get("sourceField"))));
            if (field == null) {
                continue;
            }
            Map<String, Object> permission = findChildFieldPermission(permissions, childKey, field);
            boolean readable = permission == null || readBooleanValue(permission.get("readable"), true);
            if (!readable) {
                continue;
            }
            boolean writable = permission != null && readBooleanValue(permission.get("writable"), false);
            Map<String, Object> fieldConfig = new LinkedHashMap<>(source);
            fieldConfig.put("field", field);
            fieldConfig.put("fieldCode", field);
            fieldConfig.put("readable", true);
            fieldConfig.put("writable", writable);
            fieldConfig.put("readonly", !writable);
            fieldConfig.put("disabled", !writable);
            fieldConfig.put("required", writable && permission != null
                    && readBooleanValue(permission.get("required"), false));
            fieldConfig.put("scope", "child");
            fieldConfig.put("childKey", childKey);
            fieldConfig.put("childField", field);
            result.add(fieldConfig);
        }
        return result;
    }

    private boolean hasWritableTaskChildField(Map<String, Object> child,
                                               JSONObject nodeForm,
                                               String childKey) {
        Map<String, Map<String, Object>> permissions = normalizeTaskFieldPermissionMap(nodeForm);
        for (Map<String, Object> field : readMapList(readNestedArray(child.get("fields")))) {
            String fieldName = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(field.get("field"))),
                    StringUtils.trimToNull(textValue(field.get("fieldCode"))),
                    StringUtils.trimToNull(textValue(field.get("sourceField"))));
            Map<String, Object> permission = findChildFieldPermission(permissions, childKey, fieldName);
            if (permission != null && readBooleanValue(permission.get("writable"), false)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, DynamicCrudService.TaskChildPermission> buildTaskChildPermissions(
            List<Map<String, Object>> childrenConfig, JSONObject nodeForm) {
        Map<String, Map<String, Object>> nodeFieldPermissions = normalizeTaskFieldPermissionMap(nodeForm);
        Map<String, DynamicCrudService.TaskChildPermission> result = new LinkedHashMap<>();
        for (Map<String, Object> child : childrenConfig) {
            String childKey = resolveBusinessTaskChildKey(child);
            if (StringUtils.isBlank(childKey)) {
                continue;
            }
            Set<String> writableFields = new LinkedHashSet<>();
            boolean explicitFieldPermission = false;
            for (Map.Entry<String, Map<String, Object>> entry : nodeFieldPermissions.entrySet()) {
                String permissionKey = entry.getKey();
                int split = permissionKey == null ? -1 : permissionKey.lastIndexOf(':');
                if (split <= 0) {
                    continue;
                }
                String configuredChildKey = permissionKey.substring(0, split);
                String configuredField = permissionKey.substring(split + 1);
                if (!sameChildTableKey(configuredChildKey, childKey) || StringUtils.isBlank(configuredField)) {
                    continue;
                }
                explicitFieldPermission = true;
                if (readBooleanValue(entry.getValue().get("writable"), false)) {
                    writableFields.add(configuredField);
                }
            }
            if (!explicitFieldPermission) {
                readMapList(readNestedArray(child.get("fields"))).stream()
                        .filter(field -> readBooleanValue(field.get("writable"), false))
                        .flatMap(field -> java.util.stream.Stream.of(
                                textValue(field.get("field")),
                                textValue(field.get("fieldCode")),
                                textValue(field.get("sourceField"))))
                        .filter(StringUtils::isNotBlank)
                        .forEach(writableFields::add);
            }
            result.put(childKey, new DynamicCrudService.TaskChildPermission(
                    readBooleanValue(child.get("readable"), true),
                    readBooleanValue(child.get("allowCreate"), false),
                    readBooleanValue(child.get("allowUpdate"), false),
                    readBooleanValue(child.get("allowDelete"), false),
                    writableFields));
        }
        return result;
    }

    private Map<String, Map<String, Object>> normalizeTaskChildPermissionMap(JSONObject nodeForm) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        Object source = nodeForm == null ? null : nodeForm.get("childPermissions");
        if (source == null && nodeForm != null) {
            source = nodeForm.get("fieldPermissions");
        }
        JSONObject object = readNestedObject(source);
        JSONArray childArray = source instanceof List<?> || source instanceof JSONArray
                ? readNestedArray(source)
                : readNestedArray(object.get("children"));
        for (Map<String, Object> item : readMapList(childArray)) {
            String childKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(item.get("childKey"))),
                    StringUtils.trimToNull(textValue(item.get("relationKey"))),
                    StringUtils.trimToNull(textValue(item.get("key"))));
            if (childKey != null) {
                Map<String, Object> normalized = new LinkedHashMap<>(item);
                normalized.put("childKey", childKey);
                result.put(childKey, normalized);
            }
        }
        return result;
    }

    private Map<String, Map<String, Object>> normalizeTaskFieldPermissionMap(JSONObject nodeForm) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        Object source = nodeForm == null ? null : nodeForm.get("fieldPermissions");
        if (source == null && nodeForm != null) {
            source = nodeForm.get("childPermissions");
        }
        for (Map<String, Object> item : normalizeFieldPermissions(source)) {
            if (!"child".equalsIgnoreCase(textValue(item.get("scope")))) {
                continue;
            }
            String childKey = StringUtils.trimToNull(textValue(item.get("childKey")));
            String childField = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(item.get("childField"))),
                    StringUtils.trimToNull(textValue(item.get("field"))));
            if (childKey != null && childField != null) {
                result.put(taskChildPermissionKey(childKey, childField), item);
            }
        }
        return result;
    }

    private String taskChildPermissionKey(String childKey, String field) {
        return StringUtils.defaultString(childKey) + ":" + StringUtils.defaultString(field);
    }

    private boolean sameTaskFormKey(String configuredFormKey, String runtimeFormKey) {
        if (StringUtils.isAnyBlank(configuredFormKey, runtimeFormKey)) {
            return true;
        }
        if (StringUtils.equals(configuredFormKey, runtimeFormKey)) {
            return true;
        }
        return configuredFormKey.endsWith("_" + runtimeFormKey)
                || runtimeFormKey.endsWith("_" + configuredFormKey)
                || configuredFormKey.contains("_form_" + runtimeFormKey)
                || runtimeFormKey.contains("_form_" + configuredFormKey);
    }

    private boolean sameFieldName(String left, String right) {
        if (StringUtils.equals(left, right)) {
            return true;
        }
        if (StringUtils.isBlank(left) || StringUtils.isBlank(right)) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(snakeToCamel(left), snakeToCamel(right));
    }

    private boolean sameChildTableKey(String left, String right) {
        if (StringUtils.isBlank(left) || StringUtils.isBlank(right)) {
            return false;
        }
        if (StringUtils.equals(left, right)) {
            return true;
        }
        String shorter = left.length() <= right.length() ? left : right;
        String longer = left.length() <= right.length() ? right : left;
        return longer.endsWith("_" + shorter);
    }

    private Map<String, Object> findChildPermission(Map<String, Map<String, Object>> permissions, String childKey) {
        if (permissions == null || permissions.isEmpty() || StringUtils.isBlank(childKey)) {
            return null;
        }
        Map<String, Object> direct = permissions.get(childKey);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, Map<String, Object>> entry : permissions.entrySet()) {
            if (sameChildTableKey(entry.getKey(), childKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, Object> findChildFieldPermission(Map<String, Map<String, Object>> permissions,
                                                         String childKey,
                                                         String field) {
        if (permissions == null || permissions.isEmpty() || StringUtils.isBlank(field)) {
            return null;
        }
        Map<String, Object> direct = permissions.get(taskChildPermissionKey(childKey, field));
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, Map<String, Object>> entry : permissions.entrySet()) {
            String key = entry.getKey();
            int split = key == null ? -1 : key.lastIndexOf(':');
            if (split <= 0) {
                continue;
            }
            String configuredChildKey = key.substring(0, split);
            String configuredField = key.substring(split + 1);
            if (sameFieldName(configuredField, field) && sameChildTableKey(configuredChildKey, childKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractTaskMainPayload(Map<String, Object> data) {
        if (data != null && data.get("main") instanceof Map<?, ?> main) {
            return (Map<String, Object>) main;
        }
        if (data == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>(data);
        result.remove("children");
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractTaskChildrenPayload(Map<String, Object> data) {
        if (data != null && data.get("children") instanceof Map<?, ?> children) {
            return (Map<String, Object>) children;
        }
        return Map.of();
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
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map<String, Object> childConfig : childrenConfig) {
            String key = resolveBusinessTaskChildKey(childConfig);
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Object value = children.get(key);
            if (!(value instanceof List<?> rows)) {
                continue;
            }
            Set<String> visibleFields = readMapList(readNestedArray(childConfig.get("fields"))).stream()
                    .map(field -> StringUtils.firstNonBlank(
                            StringUtils.trimToNull(textValue(field.get("field"))),
                            StringUtils.trimToNull(textValue(field.get("fieldCode")))))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<Map<String, Object>> visibleRows = new ArrayList<>();
            for (Object rowValue : rows) {
                if (!(rowValue instanceof Map<?, ?> row)) {
                    continue;
                }
                Map<String, Object> visibleRow = new LinkedHashMap<>();
                row.forEach((rowKey, rowItem) -> {
                    String field = String.valueOf(rowKey);
                    if (visibleFields.contains(field)
                            || "id".equalsIgnoreCase(field)
                            || "_deleted".equalsIgnoreCase(field)) {
                        visibleRow.put(field, rowItem);
                    }
                });
                visibleRows.add(visibleRow);
            }
            filtered.put(key, visibleRows);
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
                StringUtils.trimToNull(textValue(child.get("modelCode"))),
                StringUtils.trimToNull(textValue(child.get("relationKey"))),
                StringUtils.trimToNull(textValue(child.get("key"))),
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
        if (context.getChildrenConfig() != null) {
            for (Map<String, Object> child : context.getChildrenConfig()) {
                if (child == null) {
                    continue;
                }
                child.put("allowCreate", false);
                child.put("allowUpdate", false);
                child.put("allowDelete", false);
                child.put("readable", true);
                for (Map<String, Object> field : readMapList(readNestedArray(child.get("fields")))) {
                    if (field == null) {
                        continue;
                    }
                    field.put("writable", false);
                    field.put("readonly", true);
                    field.put("disabled", true);
                }
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
        vo.setAllowMultiReturn(readNullableBooleanValue(source.get("allowMultiReturn")));
        vo.setAllowDirectSend(readNullableBooleanValue(source.get("allowDirectSend")));
        vo.setReturnSourceActivityId(StringUtils.trimToNull(textValue(source.get("returnSourceActivityId"))));
        vo.setReturnSourceActivityName(StringUtils.trimToNull(textValue(source.get("returnSourceActivityName"))));
        Object targets = source.get("returnTargets");
        if (targets instanceof List<?> list) {
            List<Map<String, Object>> returnTargets = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> target = new LinkedHashMap<>();
                    map.forEach((key, value) -> target.put(String.valueOf(key), value));
                    returnTargets.add(target);
                }
            }
            vo.setReturnTargets(returnTargets);
        }
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
        filtered.setObjectId(dto.getObjectId());
        filtered.setConfigKey(dto.getConfigKey());
        filtered.setSuiteCode(dto.getSuiteCode());
        filtered.setRecordId(dto.getRecordId());
        filtered.setFormKey(dto.getFormKey());
        filtered.setData(filteredData);
        return filtered;
    }

    private TaskFormRuntimeContext resolveTaskFormRuntimeContext(BusinessTaskFormContextQueryDTO query, boolean strict) {
        return resolveTaskFormRuntimeContext(query, strict, Map.of());
    }

    private TaskFormRuntimeContext resolveTaskFormRuntimeContext(BusinessTaskFormContextQueryDTO query,
                                                                 boolean strict,
                                                                 Map<String, Object> taskFormInfo) {
        Long tenantId = resolveTenantId();
        hydrateTaskFormQuery(query, taskFormInfo);
        hydrateApplicationPageFormIdentity(query);
        boolean syntheticTestKey = isSyntheticTestBusinessKey(query.getBusinessKey());
        AiBusinessFlowInstanceLink link = null;
        if (StringUtils.isNotBlank(query.getProcessInstanceId())) {
            link = flowInstanceLinkMapper.selectByProcessInstanceId(tenantId, query.getProcessInstanceId());
        }
        if (link == null && StringUtils.isNotBlank(query.getBusinessKey()) && !syntheticTestKey) {
            link = flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, query.getBusinessKey());
        }

        AiBusinessObject taskObject = resolveTaskBusinessObject(tenantId, query, link);

        String objectCode = StringUtils.firstNonBlank(
                taskObject == null ? null : taskObject.getObjectCode(),
                link == null ? null : link.getObjectCode(),
                StringUtils.trimToNull(query.getObjectCode()),
                parseBusinessKeyObjectCode(query.getBusinessKey()));
        Long recordId = link == null || link.getRecordId() == null
                ? query.getRecordId()
                : link.getRecordId();
        if (recordId == null) {
            recordId = parseBusinessKeyRecordId(query.getBusinessKey());
        }
        if (syntheticTestKey && link == null) {
            // 测试流程尚未绑单据时，忽略请求里的 recordId，避免误更新其它单据。
            recordId = null;
        }
        String businessKey = StringUtils.firstNonBlank(
                link == null ? null : link.getBusinessKey(),
                StringUtils.trimToNull(query.getBusinessKey()),
                objectCode != null && recordId != null ? buildBusinessKey(objectCode, recordId) : null);

        if (StringUtils.isBlank(objectCode)) {
            if (strict) {
                throw new BusinessException("未解析到业务对象或记录ID");
            }
            return new TaskFormRuntimeContext(null, null, businessKey, null, null);
        }
        if (recordId == null && strict) {
            // 发起测试等场景可能尚未落单据，保存时再创建记录。
        }

        String runtimeLookupKey = StringUtils.firstNonBlank(
                taskObject == null ? null : taskObject.getConfigKey(),
                StringUtils.trimToNull(query.getConfigKey()),
                objectCode);
        BusinessRuntimeContext businessContext = resolveBusinessRuntimeContext(tenantId, runtimeLookupKey);
        String canonicalObjectCode = StringUtils.firstNonBlank(businessContext.objectCode(), objectCode);
        String configKey = StringUtils.firstNonBlank(
                taskObject == null ? null : taskObject.getConfigKey(),
                StringUtils.trimToNull(query.getConfigKey()),
                businessContext.configKey());
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, canonicalObjectCode, objectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, tenantId, canonicalObjectCode);

        if (StringUtils.isBlank(configKey) && strict
                && !isBusinessCodeTaskForm(canonicalObjectCode, bindingConfig, query, taskFormInfo)) {
            throw new BusinessException("业务对象缺少已发布运行配置，无法保存待办业务字段");
        }
        return new TaskFormRuntimeContext(canonicalObjectCode, recordId, businessKey, configKey, bindingConfig);
    }

    /**
     * 已部署的历史 Flowable 定义可能只携带旧 objectCode，但应用页面表单 key 中包含稳定的
     * applicationId/pageId。优先从该页面资产恢复 objectId/configKey，避免待办再次落到同编码的其它对象。
     */
    private void hydrateApplicationPageFormIdentity(BusinessTaskFormContextQueryDTO query) {
        if (query == null || StringUtils.isBlank(query.getFormKey())) {
            return;
        }
        JSONObject asset = resolveApplicationPageFormAsset(query.getFormKey());
        if (asset == null || asset.isEmpty()) {
            return;
        }
        Long objectId = parseLongValue(asset.getString("objectId"));
        if (query.getObjectId() == null && objectId != null) {
            query.setObjectId(objectId);
        }
        String configKey = StringUtils.trimToNull(asset.getString("configKey"));
        if (StringUtils.isBlank(query.getConfigKey()) && configKey != null) {
            query.setConfigKey(configKey);
        }
        String objectCode = StringUtils.trimToNull(asset.getString("objectCode"));
        if (objectCode != null && objectId != null) {
            query.setObjectCode(objectCode);
        }
    }

    private void hydrateTaskFormQuery(BusinessTaskFormContextQueryDTO query) {
        hydrateTaskFormQuery(query, Map.of());
    }

    private void hydrateTaskFormQuery(BusinessTaskFormContextQueryDTO query,
                                      Map<String, Object> preloadedTaskFormInfo) {
        if (query == null || StringUtils.isBlank(query.getTaskId())) {
            return;
        }
        boolean missingIdentity = StringUtils.isBlank(query.getProcessInstanceId())
                || StringUtils.isBlank(query.getBusinessKey())
                || StringUtils.isBlank(query.getObjectCode())
                || query.getObjectId() == null
                || StringUtils.isBlank(query.getConfigKey())
                || query.getRecordId() == null;
        if (!missingIdentity) {
            return;
        }
        Map<String, Object> formInfo = preloadedTaskFormInfo == null || preloadedTaskFormInfo.isEmpty()
                ? loadTaskFormInfo(query.getTaskId())
                : preloadedTaskFormInfo;
        if (formInfo == null || formInfo.isEmpty()) {
            return;
        }
        Map<String, Object> formRef = readNestedObject(formInfo.get("formRef"));
        JSONObject variables = readNestedObject(formInfo.get("variables"));
        JSONObject variableFormRef = readNestedObject(variables.get("businessFormRef"));
        if (StringUtils.isBlank(query.getProcessInstanceId())) {
            query.setProcessInstanceId(StringUtils.trimToNull(textValue(formInfo.get("processInstanceId"))));
        }
        if (StringUtils.isBlank(query.getBusinessKey())) {
            query.setBusinessKey(StringUtils.trimToNull(textValue(formInfo.get("businessKey"))));
        }
        if (StringUtils.isBlank(query.getProcessDefKey())) {
            query.setProcessDefKey(StringUtils.trimToNull(textValue(formInfo.get("processDefKey"))));
        }
        if (StringUtils.isBlank(query.getTaskDefKey())) {
            query.setTaskDefKey(StringUtils.trimToNull(textValue(formInfo.get("taskDefKey"))));
        }
        if (StringUtils.isBlank(query.getFormKey())) {
            query.setFormKey(StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(formInfo.get("formKey"))),
                    StringUtils.trimToNull(textValue(formRef.get("formKey")))));
        }
        if (StringUtils.isBlank(query.getObjectCode())) {
            query.setObjectCode(StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(formInfo.get("objectCode"))),
                    StringUtils.trimToNull(textValue(formRef.get("objectCode"))),
                    StringUtils.trimToNull(textValue(variables.get("objectCode"))),
                    StringUtils.trimToNull(textValue(variableFormRef.get("objectCode")))));
        }
        if (query.getObjectId() == null) {
            query.setObjectId(firstLongValue(
                    formInfo.get("objectId"), formInfo.get("businessObjectId"), formRef.get("objectId"),
                    variables.get("objectId"), variables.get("businessObjectId"), variableFormRef.get("objectId")));
        }
        if (StringUtils.isBlank(query.getConfigKey())) {
            query.setConfigKey(StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(formInfo.get("configKey"))),
                    StringUtils.trimToNull(textValue(formRef.get("configKey"))),
                    StringUtils.trimToNull(textValue(variables.get("configKey"))),
                    StringUtils.trimToNull(textValue(variableFormRef.get("configKey")))));
        }
        if (StringUtils.isBlank(query.getSuiteCode())) {
            query.setSuiteCode(StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(formInfo.get("suiteCode"))),
                    StringUtils.trimToNull(textValue(formRef.get("suiteCode"))),
                    StringUtils.trimToNull(textValue(variables.get("suiteCode"))),
                    StringUtils.trimToNull(textValue(variableFormRef.get("suiteCode")))));
        }
        if (query.getRecordId() == null) {
            query.setRecordId(parseLongValue(textValue(formInfo.get("recordId"))));
            if (query.getRecordId() == null) {
                query.setRecordId(parseLongValue(textValue(formRef.get("recordId"))));
            }
        }
    }

    /**
     * 从流程关联和启动变量中恢复业务对象稳定身份。历史任务可能只有重复的 objectCode，
     * 因此 objectId/configKey/suiteCode 的解析必须先于编码兜底。
     */
    private AiBusinessObject resolveTaskBusinessObject(Long tenantId,
                                                       BusinessTaskFormContextQueryDTO query,
                                                       AiBusinessFlowInstanceLink link) {
        Map<String, Object> snapshot = link == null
                ? Map.of()
                : readJsonObject(link.getVariablesSnapshot());
        JSONObject snapshotFormRef = readNestedObject(snapshot.get("businessFormRef"));
        Long objectId = firstLongValue(
                query == null ? null : query.getObjectId(),
                snapshot.get("objectId"), snapshot.get("businessObjectId"), snapshot.get("targetObjectId"),
                snapshotFormRef.get("objectId"));
        if (objectId != null) {
            AiBusinessObject object = businessObjectMapper.selectByIdForTenant(tenantId, objectId);
            if (object != null) {
                return object;
            }
        }
        String configKey = StringUtils.firstNonBlank(
                query == null ? null : query.getConfigKey(),
                textValue(snapshot.get("configKey")),
                textValue(snapshot.get("runtimeConfigKey")),
                textValue(snapshotFormRef.get("configKey")));
        if (StringUtils.isNotBlank(configKey)) {
            AiBusinessObject object = businessObjectMapper.selectByConfigKey(tenantId, configKey);
            if (object != null) {
                return object;
            }
        }
        String objectCode = StringUtils.firstNonBlank(
                query == null ? null : query.getObjectCode(),
                textValue(snapshot.get("objectCode")),
                textValue(snapshotFormRef.get("objectCode")));
        String suiteCode = StringUtils.firstNonBlank(
                query == null ? null : query.getSuiteCode(),
                textValue(snapshot.get("suiteCode")),
                textValue(snapshotFormRef.get("suiteCode")));
        if (StringUtils.isNotBlank(objectCode) && StringUtils.isNotBlank(suiteCode)) {
            AiBusinessObject object = businessObjectMapper.selectByObjectCode(tenantId, suiteCode, objectCode);
            if (object != null) {
                return object;
            }
        }
        return StringUtils.isBlank(objectCode)
                ? null
                : businessObjectMapper.selectFirstByObjectCode(tenantId, objectCode);
    }

    private Long firstLongValue(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            Long parsed = value instanceof Number number
                    ? number.longValue()
                    : parseLongValue(textValue(value));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Map<String, Object> loadTaskVariablesAsRecord(BusinessTaskFormContextQueryDTO query) {
        return loadTaskVariablesAsRecord(query, Map.of());
    }

    private Map<String, Object> loadTaskVariablesAsRecord(BusinessTaskFormContextQueryDTO query,
                                                           Map<String, Object> preloadedTaskFormInfo) {
        Map<String, Object> formInfo = preloadedTaskFormInfo == null || preloadedTaskFormInfo.isEmpty()
                ? loadTaskFormInfo(query == null ? null : query.getTaskId())
                : preloadedTaskFormInfo;
        Object variables = formInfo.get("variables");
        if (!(variables instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private Long extractCreatedRecordId(Map<String, Object> record) {
        if (record == null || record.isEmpty()) {
            return null;
        }
        Object id = record.get("id");
        if (id == null) {
            id = record.get("ID");
        }
        return parseLongValue(textValue(id));
    }

    private void ensureRuntimeLink(TaskFormRuntimeContext runtime,
                                   BusinessTaskFormContextQueryDTO query,
                                   Long recordId) {
        if (runtime == null || query == null || recordId == null || StringUtils.isBlank(runtime.objectCode())) {
            return;
        }
        Long tenantId = resolveTenantId();
        String processInstanceId = StringUtils.trimToNull(query.getProcessInstanceId());
        String businessKey = buildBusinessKey(runtime.objectCode(), recordId);
        AiBusinessFlowInstanceLink existing = StringUtils.isBlank(processInstanceId)
                ? null
                : flowInstanceLinkMapper.selectByProcessInstanceId(tenantId, processInstanceId);
        if (existing != null) {
            existing.setObjectCode(runtime.objectCode());
            existing.setRecordId(recordId);
            existing.setBusinessKey(businessKey);
            flowInstanceLinkMapper.updateById(existing);
            return;
        }
        AiBusinessFlowInstanceLink link = new AiBusinessFlowInstanceLink();
        link.setTenantId(tenantId);
        link.setObjectCode(runtime.objectCode());
        link.setRecordId(recordId);
        link.setBusinessKey(businessKey);
        link.setFlowModelKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getProcessDefKey()),
                resolveFlowModelKey(runtime.bindingConfig())));
        link.setProcessInstanceId(processInstanceId);
        link.setFlowStatus(BusinessDocumentFlowStatus.RUNNING.getCode());
        link.setStartUserId(resolveUserId());
        link.setStartTime(LocalDateTime.now());
        link.setRoundNo(resolveNextRoundNo(tenantId, businessKey));
        flowInstanceLinkMapper.insert(link);
    }

    private int resolveNextRoundNo(Long tenantId, String businessKey) {
        return resolveNextRoundNo(flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, businessKey));
    }

    private int resolveNextRoundNo(AiBusinessFlowInstanceLink latest) {
        if (latest == null || latest.getRoundNo() == null || latest.getRoundNo() < 1) {
            return 1;
        }
        return latest.getRoundNo() + 1;
    }

    private boolean isBusinessCodeTaskForm(String objectCode, JSONObject bindingConfig, BusinessTaskFormContextQueryDTO query) {
        return isBusinessCodeTaskForm(objectCode, bindingConfig, query, Map.of());
    }

    private boolean isBusinessCodeTaskForm(String objectCode,
                                           JSONObject bindingConfig,
                                           BusinessTaskFormContextQueryDTO query,
                                           Map<String, Object> taskFormInfo) {
        JSONObject nodeForm = resolveTaskNodeForm(
                new TaskFormRuntimeContext(objectCode, null, null, null, bindingConfig), query, taskFormInfo);
        return nodeForm != null && "BUSINESS_CODE_FORM".equals(normalizeNodeFormMode(nodeForm.getString("formMode")));
    }

    private BusinessObjectVO queryBusinessObject(Long tenantId, String objectCode) {
        return queryBusinessObject(tenantId, objectCode, null);
    }

    /**
     * 查询待办展示对象。配置键是运行时的稳定身份，必须优先于可能来自历史数据的 objectCode。
     */
    private BusinessObjectVO queryBusinessObject(Long tenantId, String objectCode, String configKey) {
        if (StringUtils.isNotBlank(configKey)) {
            AiBusinessObject byConfigKey = businessObjectMapper.selectByConfigKey(tenantId, configKey);
            if (byConfigKey != null) {
                return toBusinessObjectVO(byConfigKey);
            }
        }
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
        query.setObjectCode(objectCode);
        List<BusinessObjectVO> objects = businessObjectMapper.selectObjectList(tenantId, query);
        return objects == null || objects.isEmpty() ? null : objects.get(0);
    }

    private BusinessObjectVO toBusinessObjectVO(AiBusinessObject object) {
        if (object == null) {
            return null;
        }
        BusinessObjectVO vo = new BusinessObjectVO();
        vo.setId(object.getId());
        vo.setSuiteCode(object.getSuiteCode());
        vo.setObjectCode(object.getObjectCode());
        vo.setObjectName(object.getObjectName());
        vo.setObjectType(object.getObjectType());
        vo.setModelId(object.getModelId());
        vo.setModelCode(object.getModelCode());
        vo.setDisplayField(object.getDisplayField());
        vo.setIcon(object.getIcon());
        vo.setDescription(object.getDescription());
        vo.setStatus(object.getStatus());
        vo.setSortOrder(object.getSortOrder());
        vo.setOptions(object.getOptions());
        vo.setDesignStatus(object.getDesignStatus());
        vo.setConfigKey(object.getConfigKey());
        vo.setLastPublishTime(object.getLastPublishTime());
        vo.setLastPublishVersion(object.getLastPublishVersion());
        vo.setDesignerOptions(object.getDesignerOptions());
        return vo;
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
        BusinessObjectVO object = context.businessObject() == null
                ? queryBusinessObject(tenantId, objectCode, context.configKey())
                : toBusinessObjectVO(context.businessObject());
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

    private String buildBusinessListObjectLookupKey(BusinessTaskFormContextQueryDTO query,
                                                    AiBusinessFlowInstanceLink link,
                                                    String hintedObjectCode) {
        String suiteObjectKey = query == null
                || StringUtils.isBlank(query.getSuiteCode())
                || StringUtils.isBlank(hintedObjectCode)
                ? null
                : query.getSuiteCode() + ":" + hintedObjectCode;
        return StringUtils.firstNonBlank(
                textValue(extractSnapshotValue(link, "objectId")),
                query == null ? null : query.getConfigKey(),
                suiteObjectKey,
                hintedObjectCode,
                "unknown");
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
            Map<String, Object> startParams = item.getBusinessParams();
            item.setObjectCode(objectCode);
            item.setRecordId(runtime.recordId());
            item.setBusinessType(StringUtils.firstNonBlank(item.getBusinessType(), objectCode));
            item.setBusinessParams(mergeBusinessListParams(startParams, recordData));
            applyStartDisplayExtensions(item, startParams);
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

    /**
     * 流程实例关联表保存的是完整启动变量快照，而列表扩展点历史上接收的是
     * businessParams 变量本身。优先提取嵌套值；旧数据没有该变量时保留整个快照，
     * 由列表扩展点自行忽略流程上下文字段。
     */
    private Map<String, Object> readBusinessParamsSnapshot(AiBusinessFlowInstanceLink link) {
        if (link == null || StringUtils.isBlank(link.getVariablesSnapshot())) {
            return Map.of();
        }
        JSONObject snapshot = readJsonObject(link.getVariablesSnapshot());
        Object businessParams = snapshot.get("businessParams");
        if (businessParams instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (key != null) {
                    result.put(String.valueOf(key), value);
                }
            });
            return result;
        }
        return snapshot.isEmpty() ? Map.of() : new LinkedHashMap<>(snapshot);
    }

    private String extractSnapshotValue(AiBusinessFlowInstanceLink link, String key) {
        if (link == null || StringUtils.isBlank(key)) {
            return null;
        }
        JSONObject snapshot = readJsonObject(link.getVariablesSnapshot());
        return StringUtils.trimToNull(textValue(snapshot.get(key)));
    }

    private String firstNonBlankValue(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            String text = StringUtils.trimToNull(textValue(value));
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private Map<String, Object> mergeBusinessListParams(Map<String, Object> startParams,
                                                        Map<String, Object> recordData) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (startParams != null && !startParams.isEmpty()) {
            merged.putAll(startParams);
        }
        if (recordData != null && !recordData.isEmpty()) {
            merged.putAll(recordData);
        }
        return merged.isEmpty() ? startParams : merged;
    }

    /**
     * 发起时传入的 displayFields 不能被业务记录覆盖。
     * 待办列表只渲染 displayExtensions，业务记录仍留在 businessParams 给自定义 SPI 使用。
     */
    private void applyStartDisplayExtensions(FlowBusinessListDisplayItem item, Map<String, Object> startParams) {
        if (item == null || (item.getDisplayExtensions() != null && !item.getDisplayExtensions().isEmpty())) {
            return;
        }
        if (startParams == null || startParams.isEmpty()) {
            return;
        }
        Object fields = startParams.get("displayFields");
        if (fields == null) {
            fields = startParams.get("fields");
        }
        if (!(fields instanceof List<?> || fields instanceof Map<?, ?>)) {
            return;
        }
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("fields", fields);
        item.setDisplayExtensions(extensions);
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
            Map<String, Object> startParams = item.getBusinessParams();
            item.setObjectCode(objectCode);
            item.setRecordId(runtime.recordId());
            item.setBusinessType(StringUtils.firstNonBlank(item.getBusinessType(), objectCode));
            item.setBusinessParams(mergeBusinessListParams(startParams, Map.of(
                    "recordId", runtime.recordId(),
                    "businessKey", runtime.businessKey())));
            applyStartDisplayExtensions(item, startParams);
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
        return resolveTaskNodeForm(runtime, query, Map.of());
    }

    private JSONObject resolveTaskNodeForm(TaskFormRuntimeContext runtime,
                                           BusinessTaskFormContextQueryDTO query,
                                           Map<String, Object> taskFormInfo) {
        JSONObject flowNodeForm = resolveFlowNodeForm(runtime, query, taskFormInfo);
        if (!flowNodeForm.isEmpty()) {
            return flowNodeForm;
        }
        return findNodeForm(runtime.bindingConfig(), query.getTaskDefKey());
    }

    private JSONObject resolveFlowNodeForm(TaskFormRuntimeContext runtime, BusinessTaskFormContextQueryDTO query) {
        return resolveFlowNodeForm(runtime, query, Map.of());
    }

    private JSONObject resolveFlowNodeForm(TaskFormRuntimeContext runtime,
                                           BusinessTaskFormContextQueryDTO query,
                                           Map<String, Object> taskFormInfo) {
        String objectCode = StringUtils.trimToNull(runtime.objectCode());
        if (StringUtils.isBlank(objectCode)) {
            return new JSONObject();
        }
        Map<String, Object> formInfo = loadFlowNodeFormInfo(runtime, query, taskFormInfo);
        String taskDefKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("taskDefKey"))),
                StringUtils.trimToNull(query.getTaskDefKey()));
        String configuredFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("formKey"))),
                StringUtils.trimToNull(query.getFormKey()));
        JSONObject runtimeFormRef = resolveRuntimeBusinessFormRef(formInfo);
        String runtimeFormKey = StringUtils.trimToNull(runtimeFormRef.getString("formKey"));
        JSONObject runtimeAsset = resolveBusinessTaskFormAsset(objectCode, runtimeFormKey);
        boolean useRuntimePageForm = StringUtils.isNotBlank(runtimeFormKey) && !runtimeAsset.isEmpty();
        String formKey = useRuntimePageForm ? runtimeFormKey : configuredFormKey;
        Object rawFormPermissions = formInfo.get("formFieldPermissions");
        List<Map<String, Object>> permissions = normalizeFieldPermissions(rawFormPermissions);
        List<Map<String, Object>> childPermissions = normalizeTaskChildPermissions(rawFormPermissions);
        JSONObject flowFormRef = readNestedObject(formInfo.get("formRef"));
        if (useRuntimePageForm) {
            JSONObject effectiveRuntimeRef = new JSONObject();
            effectiveRuntimeRef.putAll(runtimeAsset);
            effectiveRuntimeRef.putAll(runtimeFormRef);
            flowFormRef = effectiveRuntimeRef;
            // 只有业务流程换了另一张页面时才丢掉节点权限。
            // 同一张表单经常一边是页面 formKey，一边是带应用前缀的 formKey，不能因此把节点上配好的权限清空。
            if (!sameTaskFormKey(runtimeFormKey, configuredFormKey)) {
                permissions = List.of();
                childPermissions = List.of();
            }
        }
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
                putBoolean(defaultNodeForm, formInfo, "allowMultiReturn");
                putBoolean(defaultNodeForm, formInfo, "allowDirectSend");
                putText(defaultNodeForm, "returnSourceActivityId", textValue(formInfo.get("returnSourceActivityId")));
                putText(defaultNodeForm, "returnSourceActivityName", textValue(formInfo.get("returnSourceActivityName")));
                if (formInfo.get("returnTargets") instanceof List<?> targets) {
                    defaultNodeForm.put("returnTargets", targets);
                }
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
        putBoolean(nodeForm, formInfo, "allowMultiReturn");
        putBoolean(nodeForm, formInfo, "allowDirectSend");
        putText(nodeForm, "returnSourceActivityId", textValue(formInfo.get("returnSourceActivityId")));
        putText(nodeForm, "returnSourceActivityName", textValue(formInfo.get("returnSourceActivityName")));
        if (formInfo.get("returnTargets") instanceof List<?> targets) {
            nodeForm.put("returnTargets", targets);
        }
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
        if (!childPermissions.isEmpty()) {
            nodeForm.put("childPermissions", childPermissions);
        }
        return nodeForm;
    }

    /**
     * Reads the concrete page form selected by an outer application business-process
     * approval node. Earlier runs only have the compatibility variable {@code formKey};
     * newer runs also carry a structured {@code businessFormRef}.
     */
    private JSONObject resolveRuntimeBusinessFormRef(Map<String, Object> formInfo) {
        JSONObject variables = readNestedObject(formInfo == null ? null : formInfo.get("variables"));
        if (variables.isEmpty()) {
            return new JSONObject();
        }
        JSONObject formRef = readNestedObject(variables.get("businessFormRef"));
        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(variables.getString("businessFormKey")),
                StringUtils.trimToNull(formRef.getString("formKey")),
                StringUtils.trimToNull(variables.getString("formKey")));
        if (formKey == null) {
            return new JSONObject();
        }
        formRef.put("formKey", formKey);
        return formRef;
    }

    private Map<String, Object> loadFlowNodeFormInfo(TaskFormRuntimeContext runtime,
                                                     BusinessTaskFormContextQueryDTO query) {
        return loadFlowNodeFormInfo(runtime, query, Map.of());
    }

    private Map<String, Object> loadFlowNodeFormInfo(TaskFormRuntimeContext runtime,
                                                     BusinessTaskFormContextQueryDTO query,
                                                     Map<String, Object> preloadedTaskFormInfo) {
        Map<String, Object> taskFormInfo = preloadedTaskFormInfo == null || preloadedTaskFormInfo.isEmpty()
                ? loadTaskFormInfo(query.getTaskId())
                : preloadedTaskFormInfo;
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
        JSONObject applicationAsset = resolveApplicationPageFormAsset(formKey);
        if (!applicationAsset.isEmpty()
                && StringUtils.equals(objectCode, StringUtils.trimToNull(applicationAsset.getString("objectCode")))) {
            return applicationAsset;
        }
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

    private JSONObject resolveApplicationPageFormAsset(String formKey) {
        String key = StringUtils.trimToNull(formKey);
        if (key == null || businessApplicationService == null || !key.startsWith("app_")) {
            return new JSONObject();
        }
        int pageMarker = key.indexOf("_page_");
        if (pageMarker <= 4) {
            return new JSONObject();
        }
        Long applicationId;
        try {
            applicationId = Long.valueOf(key.substring(4, pageMarker));
        } catch (NumberFormatException error) {
            return new JSONObject();
        }
        try {
            var application = businessApplicationService.detail(applicationId);
            JSONObject options = readJsonObject(application.getOptions());
            JSONObject builder = readNestedObject(options.get("inAppBuilder"));
            JSONArray nodes = readNestedArray(builder.get("nodes"));
            JSONObject pages = readNestedObject(builder.get("pages"));
            JSONArray assets = readNestedArray(builder.get("formAssets"));
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject pageNode = nodes.getJSONObject(i);
                if (pageNode == null || !"page".equalsIgnoreCase(pageNode.getString("type"))) {
                    continue;
                }
                String pageId = StringUtils.trimToNull(pageNode.getString("id"));
                JSONObject page = pageId == null ? null : pages.getJSONObject(pageId);
                if (page == null) {
                    continue;
                }
                Set<String> referencedIds = new LinkedHashSet<>();
                collectFormAssetIds(page, referencedIds);
                JSONObject objectRef = readNestedObject(pageNode.get("objectRef"));
                String directAssetId = StringUtils.firstNonBlank(
                        pageNode.getString("formAssetId"), objectRef.getString("formAssetId"));
                if (directAssetId != null) {
                    referencedIds.add(directAssetId);
                }
                // 历史对象页面可能只有 CRUD 区块，没有在区块上保存 formAssetId。
                // 当应用仍只维护一个表单资产时，可以安全恢复该页面的默认表单，
                // 避免任务运行时因为引用链缺一段而回退到错误的对象表单。
                if (referencedIds.isEmpty()) {
                    String defaultAssetId = resolveDefaultPageFormAssetId(
                            pageNode, objectRef, assets);
                    if (defaultAssetId != null) {
                        referencedIds.add(defaultAssetId);
                    }
                }
                for (int assetIndex = 0; assetIndex < assets.size(); assetIndex++) {
                    JSONObject source = assets.getJSONObject(assetIndex);
                    if (source == null || !referencedIds.contains(source.getString("id"))) {
                        continue;
                    }
                    JSONObject resolved = readNestedObject(buildApplicationPageFormAsset(
                            applicationId,
                            resolveCanonicalObjectCode(resolveTenantId(), StringUtils.firstNonBlank(
                                    objectRef.getString("objectCode"), objectRef.getString("configKey"))),
                            pageNode, objectRef, source, pageId));
                    if (StringUtils.equals(key, resolved.getString("formKey"))) {
                        return resolved;
                    }
                }
            }
            return new JSONObject();
        } catch (Exception error) {
            log.debug("解析应用页面表单资产失败: formKey={}", formKey, error);
            return new JSONObject();
        }
    }

    private JSONObject resolveApplicationPageFormSchema(String formKey) {
        JSONObject asset = resolveApplicationPageFormAsset(formKey);
        if (asset.isEmpty()) {
            return new JSONObject();
        }
        JSONObject schema = readNestedObject(asset.get("schema"));
        if (schema.isEmpty()) {
            schema.put("formKey", asset.getString("formKey"));
            schema.put("formName", asset.getString("formName"));
            schema.put("fieldCatalog", asset.get("fieldCatalog"));
            schema.put("fields", asset.get("fields"));
        }
        return schema;
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
            appendObjectFieldRegistryFallback(assets, object);
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
        appendObjectFieldRegistryFallback(assets, object);
        return assets;
    }

    private JSONObject resolveBusinessFormSchema(BusinessObjectVO object, String formKey, String configKey) {
        JSONObject applicationSchema = resolveApplicationPageFormSchema(formKey);
        if (!applicationSchema.isEmpty()) {
            return applicationSchema;
        }
        AiCrudConfig runtimeConfig = resolveRuntimeConfigForBusinessForm(object, configKey);
        if (object == null) {
            return buildRuntimeCrudFormSchema(null, runtimeConfig, formKey);
        }
        JSONObject designerOptions = readJsonObject(object.getDesignerOptions());
        JSONObject formSchema = readNestedObject(designerOptions.get("formDesignerSchema"));
        if (formSchema.isEmpty()) {
            JSONObject runtimeSchema = buildRuntimeCrudFormSchema(object, runtimeConfig, formKey);
            return runtimeSchema.isEmpty() ? buildObjectFieldRegistryFormSchema(object, formKey) : runtimeSchema;
        }
        String targetFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formKey),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(formSchema.getString("formKey")));

        JSONObject byForms = findFormSchemaInArray(readNestedArray(formSchema.get("forms")), targetFormKey);
        if (!byForms.isEmpty()) {
            return collectBusinessFormFieldCatalog(byForms).isEmpty()
                    ? buildObjectFieldRegistryFormSchema(object, targetFormKey)
                    : byForms;
        }
        JSONObject settings = readNestedObject(formSchema.get("settings"));
        JSONObject byAssets = findFormSchemaInArray(readNestedArray(settings.get("formAssets")), targetFormKey);
        if (!byAssets.isEmpty()) {
            return collectBusinessFormFieldCatalog(byAssets).isEmpty()
                    ? buildObjectFieldRegistryFormSchema(object, targetFormKey)
                    : byAssets;
        }
        String rootFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formSchema.getString("formKey")),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")));
        if (StringUtils.isBlank(targetFormKey) || StringUtils.equals(targetFormKey, rootFormKey)) {
            return collectBusinessFormFieldCatalog(formSchema).isEmpty()
                    ? buildObjectFieldRegistryFormSchema(object, targetFormKey)
                    : formSchema;
        }
        JSONObject runtimeFormSchema = buildRuntimeCrudFormSchema(object, runtimeConfig, targetFormKey);
        if (!runtimeFormSchema.isEmpty()) {
            return runtimeFormSchema;
        }
        return buildObjectFieldRegistryFormSchema(object, targetFormKey);
    }

    private JSONObject buildObjectFieldRegistryFormSchema(BusinessObjectVO object, String requestedFormKey) {
        if (object == null || object.getId() == null) {
            return new JSONObject();
        }
        try {
            List<Map<String, Object>> sourceFields = new ArrayList<>();
            businessFieldDesignService.listFields(object.getId()).forEach(field ->
                    sourceFields.add(new LinkedHashMap<>(
                            JSON.parseObject(JSON.toJSONString(field), JSONObject.class))));
            List<Map<String, Object>> fields = normalizeRuntimeCrudFormFields(sourceFields);
            if (fields.isEmpty()) {
                return new JSONObject();
            }
            String formKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(requestedFormKey), object.getObjectCode());
            JSONObject schema = new JSONObject();
            schema.put("formKey", formKey);
            schema.put("defaultFormKey", formKey);
            schema.put("formName", StringUtils.defaultIfBlank(object.getObjectName(), object.getObjectCode()) + "表单");
            JSONArray components = new JSONArray();
            fields.forEach(field -> components.add(toRuntimeCrudFormComponent(field)));
            schema.put("components", components);
            return schema;
        } catch (Exception e) {
            log.debug("读取业务对象字段注册表表单 schema 失败: objectId={}, error={}", object.getId(), e.getMessage());
            return new JSONObject();
        }
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
            if ("child".equalsIgnoreCase(textValue(permission.get("scope")))) {
                continue;
            }
            String field = StringUtils.trimToNull(textValue(permission.get("field")));
            if (field != null) {
                putPermissionAliases(permissionMap, field, permission);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> field : fieldCatalog) {
            if (isChildTaskFormField(field)) {
                continue;
            }
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

    /**
     * 子表字段只通过 childrenConfig 渲染。字段目录里带 scope=child，
     * 或列表设计选出的 modelCode__sourceField，都不能再进主表单。
     */
    private boolean isChildTaskFormField(Map<String, Object> field) {
        if (field == null) {
            return false;
        }
        if ("child".equalsIgnoreCase(textValue(field.get("scope")))
                || StringUtils.isNotBlank(textValue(field.get("childKey")))) {
            return true;
        }
        String fieldCode = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(field.get("field"))),
                StringUtils.trimToNull(textValue(field.get("fieldCode"))));
        return fieldCode != null && fieldCode.contains("__");
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
            if ("child".equalsIgnoreCase(textValue(permission.get("scope")))) {
                continue;
            }
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
            if ("child".equalsIgnoreCase(textValue(permission.get("scope")))) {
                continue;
            }
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
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":") || isSyntheticTestBusinessKey(businessKey)) {
            return null;
        }
        return StringUtils.trimToNull(businessKey.split(":", 2)[0]);
    }

    private Long parseBusinessKeyRecordId(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":") || isSyntheticTestBusinessKey(businessKey)) {
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
                () -> startDocumentFlowInternal(dto, false, userId, userName, effectiveTenantId, false, false));
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
                () -> startDocumentFlowInternal(dto, false, userId, userName, effectiveTenantId, false, true));
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
            handleFlowEngineTaskEvent(ctx);
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
                    ctx.getEvent(), ctx.getProcessInstanceId(), ctx.getBusinessKey(), e.getMessage(), e);
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

    /**
     * 任务级事件只维护流程运行期间的单据中间态（待修改 / 流程中）。
     * <p>
     * 终态一律由流程结束事件裁决，这里不写结束状态；单据状态同步失败也不能让审批动作失败，
     * 因此异常只记录日志，由发起人修改节点保存字段时的自愈逻辑兜底。
     */
    private void handleFlowEngineTaskEvent(FlowEventContext ctx) {
        Long tenantId = ctx.getTenantId() != null ? ctx.getTenantId() : resolveTenantId();
        BusinessFlowCallbackDTO probe = new BusinessFlowCallbackDTO();
        probe.setProcessInstanceId(StringUtils.trimToNull(ctx.getProcessInstanceId()));
        probe.setBusinessKey(StringUtils.trimToNull(ctx.getBusinessKey()));
        AiBusinessFlowInstanceLink link = findCallbackLink(tenantId, probe);
        if (link == null || isEndedLink(link)) {
            return;
        }
        Long effectiveTenantId = link.getTenantId() != null ? link.getTenantId() : tenantId;
        try {
            TenantContextHolder.executeWithTenant(effectiveTenantId, () -> {
                if (FlowCallback.ON_TASK_COMPLETED.equals(ctx.getEvent())) {
                    handleTaskCompletedEvent(link, ctx);
                } else {
                    handleTaskCreatedEvent(link, ctx);
                }
            });
        } catch (Exception e) {
            log.warn("[低代码流程回调] 任务事件同步单据状态失败: event={}, processInstanceId={}, taskId={}, error={}",
                    ctx.getEvent(), ctx.getProcessInstanceId(), ctx.getTaskId(), e.getMessage());
        }
    }

    /**
     * 新待办产生。令牌回到发起人且流程上存在驳回痕迹时，视为发起人修改节点：
     * 记录待办并把单据切到待修改；否则说明已进入审批节点，纠正回流程中。
     */
    private void handleTaskCreatedEvent(AiBusinessFlowInstanceLink link, FlowEventContext ctx) {
        if (isInitiatorModifyNode(ctx) || (isInitiatorTask(link, ctx) && hasRejectEvidence(link, ctx))) {
            writeModifyTask(link, new BusinessFlowLinkRuntimeState.ModifyTask(
                    StringUtils.trimToNull(ctx.getTaskId()),
                    StringUtils.trimToNull(ctx.getTaskDefKey()),
                    StringUtils.trimToNull(ctx.getTaskName()),
                    StringUtils.trimToNull(ctx.getAssigneeId())));
            applyRunningFlowState(link, BusinessDocumentFlowStatus.NEED_MODIFY);
            log.info("[低代码流程回调] 进入发起人修改节点，单据切换为待修改: businessKey={}, taskId={}, taskDefKey={}",
                    link.getBusinessKey(), ctx.getTaskId(), ctx.getTaskDefKey());
            return;
        }
        writeModifyTask(link, null);
        applyRunningFlowState(link, BusinessDocumentFlowStatus.IN_PROCESS);
    }

    private boolean isInitiatorModifyNode(FlowEventContext ctx) {
        String taskDefKey = StringUtils.trimToNull(ctx == null ? null : ctx.getTaskDefKey());
        return "Forge_InitiatorModify".equals(taskDefKey)
                || (taskDefKey != null && taskDefKey.startsWith("Forge_InitiatorModify"));
    }

    /**
     * 待办办理完成。审批人驳回时先落待修改，紧随的任务创建事件会补齐修改待办；
     * 若 BPMN 的驳回分支直接走到结束事件，终态事件会把状态覆盖成已驳回。
     */
    private void handleTaskCompletedEvent(AiBusinessFlowInstanceLink link, FlowEventContext ctx) {
        boolean rejected = isRejectTaskAction(ctx.getVariables());
        if (isRecordedModifyTask(link, ctx.getTaskId())) {
            writeModifyTask(link, null);
            if (!rejected) {
                applyRunningFlowState(link, BusinessDocumentFlowStatus.IN_PROCESS);
            }
            return;
        }
        if (rejected) {
            applyRunningFlowState(link, BusinessDocumentFlowStatus.NEED_MODIFY);
        }
    }

    /**
     * 兜底修复发起人修改节点状态。任务事件可能因回调丢失或时序问题没落地，
     * 发起人在修改节点保存字段时补一次，避免单据一直停在流程中而拿不到重提入口。
     */
    private void repairInitiatorModifyState(BusinessTaskFormContextQueryDTO query,
                                            TaskFormRuntimeContext runtime,
                                            Map<String, Object> taskFormInfo) {
        Long userId = resolveUserId();
        if (userId == null) {
            return;
        }
        String businessKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getBusinessKey()),
                runtime == null ? null : StringUtils.trimToNull(runtime.businessKey()));
        AiBusinessFlowInstanceLink link = findRuntimeLink(
                resolveTenantId(), StringUtils.trimToNull(query.getProcessInstanceId()), businessKey);
        if (link == null || isEndedLink(link) || !userId.equals(link.getStartUserId())) {
            return;
        }
        if (isRecordedModifyTask(link, query.getTaskId())) {
            return;
        }
        // 发起人也可能本身就是某个审批节点的处理人，必须确认这次流转是驳回引起的。
        if (!isRejectTaskAction(readFlowProcessVariables(resolveFlowEngineBusinessKey(link)))) {
            return;
        }
        writeModifyTask(link, new BusinessFlowLinkRuntimeState.ModifyTask(
                query.getTaskId(),
                StringUtils.firstNonBlank(
                        StringUtils.trimToNull(query.getTaskDefKey()),
                        textValue(taskFormInfo == null ? null : taskFormInfo.get("taskDefKey"))),
                textValue(taskFormInfo == null ? null : taskFormInfo.get("taskName")),
                String.valueOf(userId)));
        applyRunningFlowState(link, BusinessDocumentFlowStatus.NEED_MODIFY);
        log.info("[低代码流程] 修复发起人修改节点状态: businessKey={}, taskId={}",
                link.getBusinessKey(), query.getTaskId());
    }

    private boolean isInitiatorTask(AiBusinessFlowInstanceLink link, FlowEventContext ctx) {
        String assigneeId = StringUtils.trimToNull(ctx.getAssigneeId());
        if (assigneeId == null) {
            return false;
        }
        String initiatorId = StringUtils.firstNonBlank(
                link.getStartUserId() == null ? null : String.valueOf(link.getStartUserId()),
                StringUtils.trimToNull(ctx.getStartUserId()));
        return StringUtils.isNotBlank(initiatorId) && initiatorId.equals(assigneeId);
    }

    private boolean isRecordedModifyTask(AiBusinessFlowInstanceLink link, String taskId) {
        BusinessFlowLinkRuntimeState.ModifyTask recorded =
                BusinessFlowLinkRuntimeState.readModifyTask(link.getVariablesSnapshot());
        return recorded != null && StringUtils.isNotBlank(taskId) && taskId.equals(recorded.taskId());
    }

    /**
     * 任务创建事件不携带流程变量，需要回查流程实例变量确认这次流转是驳回引起的，
     * 避免把“发起人本身就是首个审批人”误判成发起人修改节点。
     */
    private boolean hasRejectEvidence(AiBusinessFlowInstanceLink link, FlowEventContext ctx) {
        if (isRejectTaskAction(ctx.getVariables())) {
            return true;
        }
        return isRejectTaskAction(readFlowProcessVariables(resolveFlowEngineBusinessKey(link)));
    }

    private Map<String, Object> readFlowProcessVariables(String businessKey) {
        if (flowClient == null || StringUtils.isBlank(businessKey)) {
            return Map.of();
        }
        try {
            FlowResult<Map<String, Object>> result = flowClient.getProcessVariables(businessKey);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return Map.of();
            }
            return result.getData();
        } catch (Exception e) {
            log.debug("[低代码流程回调] 读取流程变量失败: businessKey={}, error={}", businessKey, e.getMessage());
            return Map.of();
        }
    }

    private boolean isRejectTaskAction(Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return false;
        }
        String approvalResult = textValue(variables.get("approvalResult"));
        if (StringUtils.isNotBlank(approvalResult) && "reject".equalsIgnoreCase(approvalResult.trim())) {
            return true;
        }
        if (readBooleanValue(variables.get("rejectToStart"), false)) {
            return true;
        }
        Object approved = variables.get("approved");
        return approved != null && !readBooleanValue(approved, true);
    }

    /**
     * 合并流程变量到关联快照。发起时写入的 {@code flowBusinessKey}、{@code statusField}
     * 是后续读取引擎状态和回写状态字段的依据，整体覆盖会导致这些键丢失。
     */
    private String mergeLinkVariablesSnapshot(AiBusinessFlowInstanceLink link, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return link.getVariablesSnapshot();
        }
        JSONObject snapshot = readJsonObject(link.getVariablesSnapshot());
        snapshot.putAll(variables);
        return JSON.toJSONString(snapshot);
    }

    private void writeModifyTask(AiBusinessFlowInstanceLink link,
                                 BusinessFlowLinkRuntimeState.ModifyTask task) {
        BusinessFlowLinkRuntimeState.ModifyTask recorded =
                BusinessFlowLinkRuntimeState.readModifyTask(link.getVariablesSnapshot());
        if (task == null ? recorded == null : task.equals(recorded)) {
            // 每次任务创建都会走清除分支，没有变化时不要产生无意义的 UPDATE。
            return;
        }
        String snapshot = BusinessFlowLinkRuntimeState.writeModifyTask(link.getVariablesSnapshot(), task);
        link.setVariablesSnapshot(snapshot);
        AiBusinessFlowInstanceLink update = new AiBusinessFlowInstanceLink();
        update.setId(link.getId());
        update.setVariablesSnapshot(snapshot);
        flowInstanceLinkMapper.updateById(update);
    }

    /**
     * 写入流程运行期间的单据状态。只在单据当前仍处于运行态时翻转，
     * 已经落定为通过/驳回/取消/关闭的单据不再被任务事件改写。
     */
    private void applyRunningDocumentStatus(AiBusinessFlowInstanceLink link, String targetStatusKey) {
        if (link.getRecordId() == null || StringUtils.isBlank(targetStatusKey)) {
            return;
        }
        AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByObjectCode(
                link.getTenantId(), link.getObjectCode());
        AiCrudConfig runtimeConfig = documentConfig == null
                ? resolvePublishedRuntimeConfig(link.getTenantId(), link.getObjectCode())
                : null;
        Map<String, Object> startVariables = readJsonObject(link.getVariablesSnapshot());
        AiCrudConfig statusRuntimeConfig = resolveStatusWriteConfig(link, startVariables, runtimeConfig);
        String currentStatusKey = resolveCurrentDocumentStatusKey(
                link, documentConfig, statusRuntimeConfig, startVariables);
        if (targetStatusKey.equals(currentStatusKey)) {
            return;
        }
        if (currentStatusKey != null && !RUNNING_DOCUMENT_STATUS_KEYS.contains(currentStatusKey)) {
            return;
        }
        AiBusinessBinding binding = selectMainFlowBindingForConfig(link.getTenantId(), link.getObjectCode());
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, link.getTenantId(), link.getObjectCode());
        if (StringUtils.isBlank(configuredStatusField(startVariables))) {
            updateBusinessFlowStatus(documentConfig, runtimeConfig, bindingConfig,
                    link.getRecordId(), targetStatusKey);
        }
        syncConfiguredStatusField(statusRuntimeConfig, link.getRecordId(), startVariables, targetStatusKey);
    }

    /**
     * 统一维护流程运行期间的双状态：低代码记录状态与流程关联状态必须一致。
     * 终态关联不接受延迟到达的任务级事件，避免已通过/已撤回后被改回流程中。
     */
    private void applyRunningFlowState(AiBusinessFlowInstanceLink link,
                                       BusinessDocumentFlowStatus targetStatus) {
        if (link == null || targetStatus == null || isEndedLink(link)) {
            return;
        }
        applyRunningDocumentStatus(link, targetStatus.getCode());
        boolean changed = !targetStatus.matches(link.getFlowStatus())
                || link.getResult() != null
                || link.getEndTime() != null;
        if (!changed) {
            return;
        }
        link.setFlowStatus(targetStatus.getCode());
        link.setResult(null);
        link.setEndTime(null);
        flowInstanceLinkMapper.updateById(link);
    }

    /**
     * 反查单据当前状态对应的标准状态键。无法判定时返回 {@code null}，由调用方按“允许写入”处理。
     */
    private String resolveCurrentDocumentStatusKey(AiBusinessFlowInstanceLink link,
                                                   AiBusinessDocumentConfig documentConfig,
                                                   AiCrudConfig statusRuntimeConfig,
                                                   Map<String, Object> startVariables) {
        String statusField = configuredStatusField(startVariables);
        if (StringUtils.isNotBlank(statusField)) {
            // 独立 flowStatus 字段直接存标准状态键，不需要反查映射。
            return textValue(readRecordField(
                    statusRuntimeConfig == null ? null : statusRuntimeConfig.getConfigKey(),
                    link.getRecordId(), statusField));
        }
        if (documentConfig == null || StringUtils.isBlank(documentConfig.getStatusField())) {
            return null;
        }
        String storedValue = textValue(readRecordField(
                documentConfig.getConfigKey(), link.getRecordId(), documentConfig.getStatusField()));
        if (StringUtils.isBlank(storedValue)) {
            return null;
        }
        for (Map.Entry<String, String> entry : documentConfigService.toVO(documentConfig)
                .getStatusMapping().entrySet()) {
            if (storedValue.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Object readRecordField(String configKey, Long recordId, String field) {
        if (StringUtils.isAnyBlank(configKey, field) || recordId == null) {
            return null;
        }
        try {
            Map<String, Object> record = dynamicCrudService.selectByIdAllowDraft(configKey, recordId);
            return record == null ? null : record.get(field);
        } catch (Exception e) {
            log.debug("[低代码流程回调] 读取单据状态失败: configKey={}, recordId={}, field={}, error={}",
                    configKey, recordId, field, e.getMessage());
            return null;
        }
    }

    private BusinessFlowRuntimeVO startDocumentFlowInternal(BusinessFlowStartDTO dto,
                                                            boolean checkPermission,
                                                            Long starterUserId,
                                                            String starterUserName,
                                                            Long tenantId,
                                                            boolean stableBusinessKey) {
        return startDocumentFlowInternal(dto, checkPermission, starterUserId, starterUserName,
                tenantId, stableBusinessKey, false);
    }

    private BusinessFlowRuntimeVO startDocumentFlowInternal(BusinessFlowStartDTO dto,
                                                            boolean checkPermission,
                                                            Long starterUserId,
                                                            String starterUserName,
                                                            Long tenantId,
                                                            boolean stableBusinessKey,
                                                            boolean allowDraftRuntime) {
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

        FlowStartContext startContext = resolveFlowStartContext(tenantId, dto.getObjectCode(), allowDraftRuntime);
        AiBusinessDocumentConfig documentConfig = startContext.documentConfig();
        AiCrudConfig runtimeConfig = startContext.runtimeConfig();
        String objectCode = startContext.objectCode();
        String configKey = startContext.configKey();
        Map<String, Object> recordData = allowDraftRuntime
                ? dynamicCrudService.selectByIdAllowDraft(configKey, dto.getRecordId())
                : dynamicCrudService.selectById(configKey, dto.getRecordId());
        if (recordData == null) {
            log.warn("[低代码流程启动] 业务记录查询为空: tenantId={}, objectCode={}, configKey={}, recordId={}, "
                            + "starterUserId={}, activeOrgId={}, checkPermission={}, stableBusinessKey={}",
                    tenantId, objectCode, configKey, dto.getRecordId(),
                    starterUserId != null ? starterUserId : resolveUserId(),
                    resolveActiveOrgId(), checkPermission, stableBusinessKey);
            throw new BusinessException(
                    404,
                    "记录不存在或无权限访问，请使用当前委托用户可见的已保存业务记录 ID");
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
        AiBusinessBinding binding = selectFlowBindingForStart(tenantId, objectCode, requestedObjectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, runtimeConfig, documentConfig);
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

        AiBusinessFlowInstanceLink latestLink = flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, businessKey);
        if (documentConfig != null) {
            BusinessDocumentConfigVO documentConfigVO = documentConfigService.toVO(documentConfig, runtimeConfig, binding);
            documentRuntimeService.validateStartAllowed(
                    objectCode, dto.getRecordId(), documentConfigVO, recordData, latestLink, checkPermission);
        }
        if (isRunningFlowLink(latestLink)) {
            return toRuntimeVO(latestLink, "当前单据已有流转中的流程");
        }
        log.info("[低代码流程启动] 主流程解析成功: tenantId={}, objectCode={}, recordId={}, configKey={}, " +
                        "flowModelKey={}, bindingId={}, bindingType={}",
                tenantId, objectCode, dto.getRecordId(), configKey, flowModelKey,
                binding == null ? null : binding.getId(), binding == null ? null : binding.getBindingType());

        Map<String, Object> flowVariables = buildFlowVariables(bindingConfig, recordData);
        mergeRequestedFlowVariables(flowVariables, dto.getVariables());
        flowVariables.put("objectCode", objectCode);
        flowVariables.put("configKey", configKey);
        flowVariables.put("recordId", dto.getRecordId());
        flowVariables.put("businessKey", businessKey);
        String flowBusinessKey = stableBusinessKey
                ? businessKey : resolveFlowBusinessKeyForStart(businessKey, latestLink);
        flowVariables.put("documentBusinessKey", businessKey);
        flowVariables.put("recordBusinessKey", businessKey);
        flowVariables.put("flowBusinessKey", flowBusinessKey);

        String userName = StringUtils.defaultIfBlank(starterUserName, resolveUsername());
        String title = applyTitleTemplate(
                StringUtils.defaultIfBlank(dto.getTitle(), buildFlowTitle(bindingConfig, recordData, objectCode)),
                recordData,
                objectCode,
                userName,
                resolveRuntimeCrudObjectName(null, runtimeConfig));
        Long userId = starterUserId != null ? starterUserId : resolveUserId();
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
        link.setFlowStatus(BusinessDocumentFlowStatus.RUNNING.getCode());
        link.setStartUserId(userId);
        link.setStartTime(LocalDateTime.now());
        link.setRoundNo(resolveNextRoundNo(latestLink));
        link.setVariablesSnapshot(JSON.toJSONString(flowVariables));
        flowInstanceLinkMapper.insert(link);

        AiCrudConfig statusRuntimeConfig = runtimeConfig != null
                ? runtimeConfig : resolvePublishedRuntimeConfig(tenantId, objectCode);
        if (StringUtils.isBlank(configuredStatusField(dto.getVariables()))) {
            updateBusinessFlowStatus(documentConfig, runtimeConfig, bindingConfig, dto.getRecordId(), BusinessDocumentFlowStatus.IN_PROCESS.getCode());
        }
        syncConfiguredStatusField(statusRuntimeConfig, dto.getRecordId(), dto.getVariables(), BusinessDocumentFlowStatus.IN_PROCESS.getCode());
        return toRuntimeVO(link, "流程已发起");
    }

    private boolean isRunningFlowLink(AiBusinessFlowInstanceLink link) {
        if (link == null) {
            return false;
        }
        return BusinessDocumentFlowStatus.STARTED.matches(link.getFlowStatus())
                || BusinessDocumentFlowStatus.RUNNING.matches(link.getFlowStatus())
                || BusinessDocumentFlowStatus.IN_PROCESS.matches(link.getFlowStatus())
                || BusinessDocumentFlowStatus.NEED_MODIFY.matches(link.getFlowStatus())
                || (link.getEndTime() == null && StringUtils.isBlank(link.getResult()));
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
            // 上次回调可能已经把关联标成结束，但草稿对象没写上 flowStatus。结束态仍补写一次。
            String result = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(link.getResult()),
                    resolveTerminalBusinessFlowResult(link.getFlowStatus()),
                    normalizeCallbackResult(dto));
            reconcileRecordFlowStatus(link, result);
            log.info("流程回调已处理，跳过重复回调: processInstanceId={}, result={}",
                    link.getProcessInstanceId(), result);
            publishBusinessProcessApprovalResult(link, result);
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
        Map<String, Object> startVariables = readJsonObject(link.getVariablesSnapshot());
        AiCrudConfig statusRuntimeConfig = resolveStatusWriteConfig(link, startVariables, runtimeConfig);
        if (StringUtils.isBlank(configuredStatusField(startVariables))) {
            updateBusinessFlowStatus(documentConfig, runtimeConfig, bindingConfig, link.getRecordId(), result);
        }
        syncConfiguredStatusField(statusRuntimeConfig, link.getRecordId(), startVariables, result);

        link.setFlowStatus(result);
        link.setResult(result);
        link.setEndTime(LocalDateTime.now());
        link.setVariablesSnapshot(BusinessFlowLinkRuntimeState.writeModifyTask(
                mergeLinkVariablesSnapshot(link, dto.getVariables()), null));
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
        publishBusinessProcessApprovalResult(link, result);
    }

    private void publishBusinessProcessApprovalResult(AiBusinessFlowInstanceLink link, String result) {
        if (link == null || link.getTenantId() == null
                || StringUtils.isAnyBlank(link.getProcessInstanceId(), result)) {
            return;
        }
        String normalized = result.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVED", "REJECTED", "CANCELED", "FAILED").contains(normalized)) {
            return;
        }
        applicationEventPublisher.publishEvent(new BusinessProcessApprovalResultEvent(
                link.getTenantId(), link.getProcessInstanceId(), normalized));
    }

    private String resolveStartConfigKey(AiBusinessDocumentConfig documentConfig,
                                         AiCrudConfig runtimeConfig,
                                         String fallbackConfigKey,
                                         boolean allowDraftRuntime) {
        if (documentConfig != null) {
            if (StringUtils.isNotBlank(documentConfig.getConfigKey())) {
                return documentConfig.getConfigKey();
            }
            if (allowDraftRuntime && StringUtils.isNotBlank(fallbackConfigKey)) {
                return fallbackConfigKey;
            }
            throw new BusinessException("单据缺少发布配置，无法发起主流程");
        }
        if (runtimeConfig != null && StringUtils.isNotBlank(runtimeConfig.getConfigKey())) {
            return runtimeConfig.getConfigKey();
        }
        if (allowDraftRuntime && StringUtils.isNotBlank(fallbackConfigKey)) {
            return fallbackConfigKey;
        }
        throw new BusinessException("业务对象缺少已发布运行配置，无法发起主流程");
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

    private AiCrudConfig resolveRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        if (StringUtils.isBlank(objectCodeOrConfigKey)) {
            return null;
        }
        return crudConfigMapper.selectRuntimeByObjectCodeOrConfigKey(
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
                || BusinessDocumentFlowStatus.APPROVED.matches(link.getResult())
                || BusinessDocumentFlowStatus.REJECTED.matches(link.getResult())
                || BusinessDocumentFlowStatus.CANCELED.matches(link.getResult())
                || BusinessDocumentFlowStatus.APPROVED.matches(link.getFlowStatus())
                || BusinessDocumentFlowStatus.REJECTED.matches(link.getFlowStatus())
                || BusinessDocumentFlowStatus.CANCELED.matches(link.getFlowStatus());
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
        return resolveFlowStartContext(tenantId, objectCodeOrConfigKey, false);
    }

    private FlowStartContext resolveFlowStartContext(Long tenantId, String objectCodeOrConfigKey, boolean allowDraftRuntime) {
        BusinessRuntimeContext context = resolveBusinessRuntimeContext(tenantId, objectCodeOrConfigKey, allowDraftRuntime);
        String configKey = resolveStartConfigKey(
                context.documentConfig(), context.runtimeConfig(), context.configKey(), allowDraftRuntime);
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
        return resolveBusinessRuntimeContext(tenantId, objectCodeOrConfigKey, false);
    }

    private BusinessRuntimeContext resolveBusinessRuntimeContext(Long tenantId, String objectCodeOrConfigKey,
                                                                 boolean allowDraftRuntime) {
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
        if (runtimeConfig == null && allowDraftRuntime) {
            runtimeConfig = resolveRuntimeConfig(tenantId, StringUtils.firstNonBlank(
                    documentConfig == null ? null : documentConfig.getConfigKey(),
                    businessObject == null ? null : businessObject.getConfigKey(),
                    canonicalObjectCode,
                    requestedObjectCode));
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
        if (object == null && StringUtils.isNotBlank(objectCodeOrConfigKey)) {
            object = businessObjectMapper.selectFirstByObjectCode(effectiveTenantId, objectCodeOrConfigKey);
        }
        return object;
    }

    private boolean isBindingEnabled(AiBusinessBinding binding) {
        return binding != null && !EnableStatus.DISABLED.matches(binding.getStatus());
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

    private void mergeRequestedFlowVariables(Map<String, Object> target, Map<String, Object> requestedVariables) {
        if (requestedVariables == null || requestedVariables.isEmpty()) {
            return;
        }
        List<String> reserved = requestedVariables.keySet().stream()
                .filter(SERVER_OWNED_FLOW_VARIABLES::contains)
                .sorted()
                .toList();
        if (!reserved.isEmpty()) {
            throw new BusinessException("启动变量不能覆盖服务端业务上下文：" + String.join(", ", reserved));
        }
        target.putAll(requestedVariables);
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
        return applyTitleTemplate(titleTemplate, recordData, objectCode);
    }

    private String applyTitleTemplate(String titleTemplate, Map<String, Object> recordData, String objectCode) {
        return applyTitleTemplate(titleTemplate, recordData, objectCode, null, null);
    }

    private String applyTitleTemplate(String titleTemplate,
                                      Map<String, Object> recordData,
                                      String objectCode,
                                      String starterName,
                                      String objectName) {
        String fallback = StringUtils.defaultIfBlank(objectCode, "业务") + " 审批申请";
        if (StringUtils.isBlank(titleTemplate)) {
            return fallback;
        }
        Map<String, String> extras = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(objectCode)) {
            extras.put("objectCode", objectCode);
        }
        if (StringUtils.isNotBlank(objectName)) {
            extras.put("objectName", objectName);
        }
        if (StringUtils.isNotBlank(starterName)) {
            extras.put("starterName", starterName);
            extras.put("initiatorName", starterName);
            extras.put("initiator", starterName);
        }
        return BusinessApprovalTitleRenderer.render(titleTemplate, recordData, extras, fallback);
    }

    private void reconcileRecordFlowStatus(AiBusinessFlowInstanceLink link, String result) {
        if (link == null || link.getRecordId() == null || StringUtils.isBlank(result)) {
            return;
        }
        Map<String, Object> startVariables = readJsonObject(link.getVariablesSnapshot());
        syncConfiguredStatusField(resolveStatusWriteConfig(link, startVariables, null),
                link.getRecordId(), startVariables, result);
    }

    /**
     * 发起时允许草稿运行配置写 flowStatus。结束回调必须走同一条路，
     * 不能只查已发布配置，否则未发布对象会一直停在审批中。
     */
    private AiCrudConfig resolveStatusWriteConfig(AiBusinessFlowInstanceLink link,
                                                  Map<String, Object> startVariables,
                                                  AiCrudConfig preferred) {
        if (preferred != null && StringUtils.isNotBlank(preferred.getConfigKey())) {
            return preferred;
        }
        String snapshotConfigKey = startVariables == null ? null : textValue(startVariables.get("configKey"));
        String lookup = StringUtils.firstNonBlank(snapshotConfigKey, link == null ? null : link.getObjectCode());
        Long tenantId = link == null ? null : link.getTenantId();
        AiCrudConfig published = resolvePublishedRuntimeConfig(tenantId, lookup);
        if (published != null) {
            return published;
        }
        String objectCode = link == null ? null : link.getObjectCode();
        if (StringUtils.isNotBlank(objectCode) && !StringUtils.equals(lookup, objectCode)) {
            published = resolvePublishedRuntimeConfig(tenantId, objectCode);
            if (published != null) {
                return published;
            }
        }
        AiCrudConfig draft = resolveRuntimeConfig(tenantId, lookup);
        if (draft != null) {
            return draft;
        }
        return resolveRuntimeConfig(tenantId, objectCode);
    }

    private void syncConfiguredStatusField(AiCrudConfig runtimeConfig,
                                           Long recordId,
                                           Map<String, Object> variables,
                                           String statusKey) {
        if (runtimeConfig == null || StringUtils.isBlank(runtimeConfig.getConfigKey()) || recordId == null) {
            return;
        }
        String statusField = configuredStatusField(variables);
        if (StringUtils.isBlank(statusField)) {
            return;
        }
        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put(statusField, statusKey);
        // 与流程关联状态在同一事务内提交；失败必须交由回调层处理，不能吞掉异常继续更新 link。
        dynamicCrudService.updateInternalFieldsByIdAllowDraft(runtimeConfig.getConfigKey(), recordId, updateData);
    }

    private String configuredStatusField(Map<String, Object> variables) {
        String statusField = firstNonBlankText(
                variables == null ? null : variables.get("flowStatusField"),
                variables == null ? null : variables.get("statusField"));
        if (StringUtils.isBlank(statusField)) {
            return "";
        }
        if (!Set.of("flowStatus", "flow_status").contains(statusField)) {
            throw new BusinessException("流程状态字段必须使用独立字段 flowStatus");
        }
        return statusField;
    }

    private String firstNonBlankText(Object... values) {
        if (values == null) {
            return "";
        }
        for (Object value : values) {
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return "";
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
        vo.setStatus(EnableStatus.ENABLED.getCode());
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
        ensureBusinessBinding(config, runtimeConfig, documentConfig);
    }

    private void ensureBusinessBinding(JSONObject config,
                                       AiCrudConfig runtimeConfig,
                                       AiBusinessDocumentConfig documentConfig) {
        if (config == null) {
            return;
        }
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

    private List<Map<String, Object>> collectObjectFieldRegistryFormAssets(BusinessObjectVO object) {
        if (object == null || object.getId() == null || StringUtils.isBlank(object.getObjectCode())) {
            return List.of();
        }
        List<Map<String, Object>> fieldCatalog;
        try {
            fieldCatalog = normalizeRuntimeCrudFormFields(readMapList(readNestedArray(
                    businessFieldDesignService.listFields(object.getId()))));
        } catch (Exception e) {
            log.warn("读取业务对象字段目录失败: objectId={}, objectCode={}, error={}",
                    object.getId(), object.getObjectCode(), e.getMessage());
            return List.of();
        }
        if (fieldCatalog.isEmpty()) {
            return List.of();
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("objectCode", object.getObjectCode());
        item.put("objectName", object.getObjectName());
        item.put("configKey", object.getConfigKey());
        item.put("formKey", object.getObjectCode());
        item.put("formName", StringUtils.defaultIfBlank(object.getObjectName(), object.getObjectCode()) + "表单");
        item.put("viewKey", "default");
        item.put("source", "objectFieldRegistry");
        item.put("sourceType", "businessObjectFieldRegistry");
        item.put("fieldCatalog", fieldCatalog);
        item.put("fields", fieldCatalog);
        item.put("fieldCount", fieldCatalog.size());
        item.put("fieldPreview", buildFieldPreview(fieldCatalog));
        item.put("supportsSave", true);
        return List.of(item);
    }

    private void appendObjectFieldRegistryFallback(List<Map<String, Object>> assets, BusinessObjectVO object) {
        if (assets == null || assets.stream().anyMatch(this::hasFormAssetFields)) {
            return;
        }
        List<Map<String, Object>> fallback = collectObjectFieldRegistryFormAssets(object);
        if (fallback.isEmpty()) {
            return;
        }
        if (!assets.isEmpty()) {
            Map<String, Object> existing = assets.get(0);
            Map<String, Object> generated = fallback.get(0);
            String mode = StringUtils.defaultIfBlank(textValue(existing.get("formMode")), textValue(existing.get("type")));
            String formKey = StringUtils.trimToNull(textValue(existing.get("formKey")));
            if ("BUSINESS_OBJECT_FORM".equalsIgnoreCase(mode) && formKey != null) {
                generated.put("formKey", formKey);
                generated.put("formName", StringUtils.defaultIfBlank(textValue(existing.get("formName")),
                        textValue(generated.get("formName"))));
                generated.put("providerKey", existing.get("providerKey"));
            }
        }
        appendUniqueFormAssets(assets, fallback);
    }

    private boolean hasFormAssetFields(Map<String, Object> asset) {
        if (asset == null) {
            return false;
        }
        List<Map<String, Object>> fields = readMapList(readNestedArray(asset.get("fieldCatalog")));
        if (fields.isEmpty()) {
            fields = readMapList(readNestedArray(asset.get("fields")));
        }
        return !fields.isEmpty();
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
        } else if (runtimeConfig != null) {
            fieldCatalog = new ArrayList<>(fieldCatalog);
            appendRuntimeChildFieldCatalog(readJsonObject(runtimeConfig.getOptions()), fieldCatalog);
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
        Map<String, Integer> positions = new LinkedHashMap<>();
        for (int index = 0; index < target.size(); index++) {
            String key = formAssetIdentity(target.get(index));
            if (StringUtils.isNotBlank(key)) {
                positions.putIfAbsent(key, index);
            }
        }
        for (Map<String, Object> asset : source) {
            String key = formAssetIdentity(asset);
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Integer existingIndex = positions.get(key);
            if (existingIndex == null) {
                positions.put(key, target.size());
                target.add(asset);
            } else {
                mergeFormAssetMetadata(target.get(existingIndex), asset);
            }
        }
    }

    private void mergeFormAssetMetadata(Map<String, Object> target, Map<String, Object> source) {
        if (target == null || source == null) {
            return;
        }
        List<Map<String, Object>> targetFields = readMapList(readNestedArray(target.get("fieldCatalog")));
        if (targetFields.isEmpty()) {
            targetFields = readMapList(readNestedArray(target.get("fields")));
        }
        List<Map<String, Object>> sourceFields = readMapList(readNestedArray(source.get("fieldCatalog")));
        if (sourceFields.isEmpty()) {
            sourceFields = readMapList(readNestedArray(source.get("fields")));
        }
        if (sourceFields.size() > targetFields.size()) {
            List<Map<String, Object>> mergedFields = new ArrayList<>(sourceFields);
            target.put("fieldCatalog", mergedFields);
            target.put("fields", mergedFields);
            target.put("fieldCount", mergedFields.size());
            target.put("fieldPreview", buildFieldPreview(mergedFields));
        }
        for (String key : List.of("configKey", "objectCode", "objectName", "viewKey",
                "providerKey", "providerName", "formUrl")) {
            if (StringUtils.isBlank(textValue(target.get(key))) && StringUtils.isNotBlank(textValue(source.get(key)))) {
                target.put(key, source.get(key));
            }
        }
        if (!Boolean.TRUE.equals(readNullableBooleanValue(target.get("supportsSave")))
                && Boolean.TRUE.equals(readNullableBooleanValue(source.get("supportsSave")))) {
            target.put("supportsSave", true);
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
            List<Map<String, Object>> result = new ArrayList<>(normalizeRuntimeCrudFormFields(
                    layoutFields.isEmpty() ? fields : layoutFields));
            appendRuntimeChildFieldCatalog(options, result);
            return result;
        }
        JSONObject modelSchema = readJsonObject(runtimeConfig.getModelSchema());
        List<Map<String, Object>> result = new ArrayList<>(normalizeRuntimeCrudFormFields(
                readMapList(readNestedArray(modelSchema.get("fields")))));
        appendRuntimeChildFieldCatalog(options, result);
        return result;
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
        appendSchemaChildTableFields(schema, result);
        if (result.isEmpty()) {
            JSONArray catalog = readNestedArray(schema.get("fieldCatalog"));
            if (catalog.isEmpty()) {
                catalog = readNestedArray(schema.get("fields"));
            }
            for (int i = 0; i < catalog.size(); i++) {
                JSONObject field = catalog.getJSONObject(i);
                if (field == null) {
                    continue;
                }
                String code = StringUtils.firstNonBlank(
                        StringUtils.trimToNull(field.getString("field")),
                        StringUtils.trimToNull(field.getString("fieldCode")),
                        StringUtils.trimToNull(readNestedObject(field.get("fieldBinding")).getString("fieldCode")));
                if (code == null || !seen.add(code)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>(field);
                item.put("field", code);
                item.put("fieldCode", code);
                item.put("label", StringUtils.firstNonBlank(
                        StringUtils.trimToNull(field.getString("label")),
                        StringUtils.trimToNull(field.getString("fieldName")), code));
                item.put("type", normalizeTaskFormFieldType(StringUtils.firstNonBlank(
                        StringUtils.trimToNull(field.getString("type")),
                        StringUtils.trimToNull(field.getString("componentType")), "input")));
                item.putIfAbsent("componentType", item.get("type"));
                result.add(item);
            }
        }
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
                    StringUtils.trimToNull(textValue(field.get("fieldName"))),
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

    private void appendSchemaChildTableFields(JSONObject schema, List<Map<String, Object>> fields) {
        if (schema == null || fields == null) {
            return;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> field : fields) {
            String childKey = StringUtils.trimToNull(textValue(field.get("childKey")));
            String childField = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(field.get("childField"))),
                    "child".equalsIgnoreCase(textValue(field.get("scope")))
                            ? StringUtils.trimToNull(textValue(field.get("field"))) : null);
            if (childKey != null && childField != null) {
                seen.add(childKey + ":" + childField);
            }
        }
        appendSchemaChildTableComponents(readNestedArray(schema.get("components")), fields, seen);
    }

    private void appendSchemaChildTableComponents(JSONArray components,
                                                  List<Map<String, Object>> fields,
                                                  Set<String> seen) {
        if (components == null || fields == null) {
            return;
        }
        for (int i = 0; i < components.size(); i++) {
            JSONObject component = components.getJSONObject(i);
            if (component == null) {
                continue;
            }
            String componentKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(component.getString("componentKey")),
                    StringUtils.trimToNull(component.getString("type")));
            JSONObject props = readNestedObject(component.get("props"));
            if ("subTable".equalsIgnoreCase(componentKey) || "childTable".equalsIgnoreCase(componentKey)) {
                String childKey = StringUtils.firstNonBlank(
                        StringUtils.trimToNull(props.getString("modelCode")),
                        StringUtils.trimToNull(props.getString("relationKey")),
                        StringUtils.trimToNull(component.getString("modelCode")),
                        StringUtils.trimToNull(component.getString("relationKey")));
                String childLabel = StringUtils.firstNonBlank(
                        StringUtils.trimToNull(props.getString("header")),
                        StringUtils.trimToNull(props.getString("relationName")),
                        StringUtils.trimToNull(component.getString("label")),
                        childKey);
                JSONArray columns = readNestedArray(props.get("columns"));
                if (columns.isEmpty()) {
                    columns = readNestedArray(props.get("fields"));
                }
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    Object rawColumn = columns.get(columnIndex);
                    JSONObject column = rawColumn instanceof JSONObject jsonColumn
                            ? jsonColumn
                            : rawColumn instanceof Map<?, ?> ? readNestedObject(rawColumn) : null;
                    String childField = column == null
                            ? StringUtils.trimToNull(textValue(rawColumn))
                            : StringUtils.firstNonBlank(
                                    StringUtils.trimToNull(column.getString("fieldCode")),
                                    StringUtils.trimToNull(column.getString("field")),
                                    StringUtils.trimToNull(column.getString("sourceField")));
                    if (childKey == null || childField == null || !seen.add(childKey + ":" + childField)) {
                        continue;
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("field", childField);
                    item.put("fieldCode", childField);
                    item.put("label", column == null
                            ? childField
                            : StringUtils.firstNonBlank(
                                    StringUtils.trimToNull(column.getString("fieldLabel")),
                                    StringUtils.trimToNull(column.getString("label")),
                                    childField));
                    item.put("scope", "child");
                    item.put("childKey", childKey);
                    item.put("childField", childField);
                    item.put("childLabel", childLabel);
                    item.put("relationName", StringUtils.defaultIfBlank(
                            StringUtils.trimToNull(props.getString("relationName")), childLabel));
                    fields.add(item);
                }
            }
            appendSchemaChildTableComponents(readNestedArray(component.get("children")), fields, seen);
        }
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
        // 带子表时 formFieldPermissions 是 JSON 对象字符串（version/fields/children）。
        // 按数组解析会失败并丢掉 fields，暂存时子表 writableFields 为空，已提交的 fieldInput 会被拒绝。
        if (permissions instanceof String stringValue) {
            String text = StringUtils.trimToEmpty(stringValue);
            if (text.startsWith("{")) {
                return normalizeFieldPermissions(readNestedObject(text));
            }
        }
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
            String scope = "child".equalsIgnoreCase(source.getString("scope")) || source.getString("childKey") != null
                    ? "child" : "main";
            String childKey = StringUtils.trimToNull(source.getString("childKey"));
            String childField = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(source.getString("childField")), field);
            String permissionKey = "child".equals(scope)
                    ? "child:" + StringUtils.defaultString(childKey) + ":" + StringUtils.defaultString(childField)
                    : "main:" + StringUtils.defaultString(field);
            if (field == null || !seen.add(permissionKey)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", "child".equals(scope) ? childField : field);
            item.put("fieldCode", "child".equals(scope) ? childField : field);
            if ("child".equals(scope)) {
                item.put("scope", "child");
                item.put("childKey", childKey);
                item.put("childField", childField);
            }
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

    private List<Map<String, Object>> normalizeTaskChildPermissions(Object source) {
        JSONObject object = readNestedObject(source);
        JSONArray children = source instanceof List<?> || source instanceof JSONArray
                ? readNestedArray(source)
                : readNestedArray(object.get("children"));
        if (children.isEmpty() && source instanceof String stringSource) {
            children = readNestedArray(stringSource);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < children.size(); i++) {
            JSONObject child = children.getJSONObject(i);
            if (child == null) {
                continue;
            }
            String childKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(child.getString("childKey")),
                    StringUtils.trimToNull(child.getString("relationKey")),
                    StringUtils.trimToNull(child.getString("key")));
            if (childKey == null || !seen.add(childKey)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(child);
            item.put("childKey", childKey);
            item.put("readable", readBooleanValue(child.get("readable"), true));
            item.put("allowCreate", readBooleanValue(child.get("allowCreate"), false));
            item.put("allowUpdate", readBooleanValue(child.get("allowUpdate"), false));
            item.put("allowDelete", readBooleanValue(child.get("allowDelete"), false));
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

    private record TaskFormSaveResult(TaskFormRuntimeContext runtime,
                                      BusinessTaskFormContextVO context) {
    }

    private record TaskFormRuntimeContext(String objectCode,
                                          Long recordId,
                                          String businessKey,
                                          String configKey,
                                          JSONObject bindingConfig) {
    }
}
