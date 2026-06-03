package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
public class BusinessFlowService {

    @Autowired(required = false)
    private FlowClient flowClient;

    private final BusinessBindingMapper bindingMapper;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessDocumentRuntimeService documentRuntimeService;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessFlowVariableResolver variableResolver;
    private final ApplicationEventPublisher applicationEventPublisher;

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
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, objectCode);

        if (binding == null) {
            AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByObjectCode(objectCode);
            if (documentConfig == null || StringUtils.isBlank(documentConfig.getDefaultFlowKey())) {
                return null;
            }
            return legacyDocumentFlowToVO(objectCode, documentConfig);
        }
        return toVO(objectCode, binding);
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
        result.put("objectCode", objectCode);
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
        JSONObject config = normalizeBindingConfig(dto);
        String flowModelKey = config.getString("flowModelKey");
        if (StringUtils.isBlank(flowModelKey)) {
            throw new BusinessException("流程模型Key不能为空");
        }
        Long tenantId = resolveTenantId();
        AiBusinessBinding existing = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", objectCode, "FLOW");

        if (existing != null) {
            existing.setTargetType("OBJECT");
            existing.setTargetCode(objectCode);
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
            binding.setTargetCode(objectCode);
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
        documentConfigService.syncDefaultFlowKeyByObjectCode(tenantId, objectCode, flowModelKey);
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
                () -> startDocumentFlowInternal(dto, true, null, null, tenantId));
    }

    /**
     * 旧审批入口兼容发起。接口层仍有旧权限校验，这里不再重复要求新流程按钮权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlowForCompatibility(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startDocumentFlowInternal(dto, false, null, null, tenantId));
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
                () -> startDocumentFlowInternal(dto, false, userId, userName, effectiveTenantId));
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
        String businessKey = buildBusinessKey(objectCode, recordId);
        AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectLatestByBusinessKey(resolveTenantId(), businessKey);
        if (link == null) {
            BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
            vo.setObjectCode(objectCode);
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

    private BusinessFlowRuntimeVO startDocumentFlowInternal(BusinessFlowStartDTO dto,
                                                            boolean checkPermission,
                                                            Long starterUserId,
                                                            String starterUserName,
                                                            Long tenantId) {
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

        AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByObjectCode(tenantId, dto.getObjectCode());
        AiCrudConfig runtimeConfig = documentConfig == null
                ? resolvePublishedRuntimeConfig(tenantId, dto.getObjectCode())
                : null;
        String configKey = resolveStartConfigKey(documentConfig, runtimeConfig);
        Map<String, Object> recordData = dynamicCrudService.selectById(configKey, dto.getRecordId());
        if (recordData == null) {
            throw new BusinessException("记录不存在或无权限访问");
        }

        String businessKey = buildBusinessKey(dto.getObjectCode(), dto.getRecordId());
        if (documentConfig != null) {
            documentRuntimeService.validateStartAllowed(dto.getObjectCode(), dto.getRecordId(), checkPermission);
        }
        AiBusinessFlowInstanceLink runningLink = flowInstanceLinkMapper.selectRunningByBusinessKey(tenantId, businessKey);
        if (runningLink != null) {
            return toRuntimeVO(runningLink, "当前单据已有流转中的流程");
        }

        AiBusinessBinding binding = selectFlowBindingForStart(tenantId, dto.getObjectCode());
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
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
                    tenantId, dto.getObjectCode(), dto.getRecordId(), checkPermission, configKey,
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
                tenantId, dto.getObjectCode(), dto.getRecordId(), configKey, flowModelKey,
                binding == null ? null : binding.getId(), binding == null ? null : binding.getBindingType());

        Map<String, Object> flowVariables = buildFlowVariables(bindingConfig, recordData);
        if (dto.getVariables() != null) {
            flowVariables.putAll(dto.getVariables());
        }
        flowVariables.putIfAbsent("objectCode", dto.getObjectCode());
        flowVariables.putIfAbsent("recordId", dto.getRecordId());
        flowVariables.putIfAbsent("businessKey", businessKey);

        String title = StringUtils.defaultIfBlank(dto.getTitle(), buildFlowTitle(bindingConfig, recordData, dto.getObjectCode()));
        Long userId = starterUserId != null ? starterUserId : resolveUserId();
        String userName = StringUtils.defaultIfBlank(starterUserName, resolveUsername());
        FlowResult<String> result = flowClient.startProcess(
                flowModelKey, businessKey, title, flowVariables,
                userId == null ? null : String.valueOf(userId), userName, null, null);
        if (result == null || !result.isSuccess() || StringUtils.isBlank(result.getData())) {
            throw new BusinessException("流程发起失败: " + (result == null ? "无返回结果" : result.getMsg()));
        }

        AiBusinessFlowInstanceLink link = new AiBusinessFlowInstanceLink();
        link.setTenantId(tenantId);
        link.setObjectCode(dto.getObjectCode());
        link.setRecordId(dto.getRecordId());
        link.setBusinessKey(businessKey);
        link.setFlowModelKey(flowModelKey);
        link.setProcessInstanceId(result.getData());
        link.setFlowStatus("RUNNING");
        link.setStartUserId(userId);
        link.setStartTime(LocalDateTime.now());
        link.setVariablesSnapshot(JSON.toJSONString(flowVariables));
        flowInstanceLinkMapper.insert(link);

        if (documentConfig != null) {
            updateDocumentStatus(documentConfig, dto.getRecordId(), "IN_PROCESS");
        }
        return toRuntimeVO(link, "流程已发起");
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
        String configKey = documentConfig != null ? documentConfig.getConfigKey() : runtimeConfig == null ? null : runtimeConfig.getConfigKey();
        Map<String, Object> previousData = StringUtils.isBlank(configKey)
                ? null
                : dynamicCrudService.selectById(configKey, link.getRecordId());
        String result = normalizeCallbackResult(dto);
        if (documentConfig != null) {
            updateDocumentStatus(documentConfig, link.getRecordId(), result);
        }

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

    private AiCrudConfig resolvePublishedRuntimeConfig(Long tenantId, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        return crudConfigMapper.selectPublishedByObjectCode(
                tenantId != null ? tenantId : resolveTenantId(), objectCode);
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
        if (normalized.contains("CANCEL") || normalized.contains("WITHDRAW")) {
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

    private AiBusinessBinding selectFlowBindingForStart(Long tenantId, String objectCode) {
        AiBusinessBinding binding = bindingMapper.selectBindingByTypeAndCode(tenantId, "OBJECT", objectCode, "FLOW");
        if (isBindingEnabled(binding)) {
            return binding;
        }
        AiBusinessBinding legacyApprovalBinding = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", objectCode, "APPROVAL");
        if (isBindingEnabled(legacyApprovalBinding)) {
            log.info("[低代码流程启动] 使用历史审批绑定作为主流程: tenantId={}, objectCode={}, binding={}",
                    tenantId, objectCode, describeBinding(legacyApprovalBinding));
            return legacyApprovalBinding;
        }
        if (binding != null || legacyApprovalBinding != null) {
            log.warn("[低代码流程启动] 未找到启用的主流程绑定: tenantId={}, objectCode={}, flowBinding={}, approvalBinding={}",
                    tenantId, objectCode, describeBinding(binding), describeBinding(legacyApprovalBinding));
        } else {
            log.warn("[低代码流程启动] 未找到主流程绑定记录: tenantId={}, objectCode={}", tenantId, objectCode);
        }
        return null;
    }

    private AiBusinessBinding selectMainFlowBindingForConfig(Long tenantId, String objectCode) {
        AiBusinessBinding binding = bindingMapper.selectBindingByTypeAndCode(tenantId, "OBJECT", objectCode, "FLOW");
        if (isBindingEnabled(binding)) {
            return binding;
        }
        AiBusinessBinding legacyApprovalBinding = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", objectCode, "APPROVAL");
        if (isBindingEnabled(legacyApprovalBinding)) {
            return legacyApprovalBinding;
        }
        return binding != null ? binding : legacyApprovalBinding;
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
        BusinessFlowBindingVO vo = new BusinessFlowBindingVO();
        vo.setBindingId(binding.getId());
        vo.setObjectCode(objectCode);
        vo.setFlowModelKey(StringUtils.defaultIfBlank(resolveFlowModelKey(config), binding.getBindingKey()));
        vo.setFlowModelName(StringUtils.defaultIfBlank(config.getString("flowModelName"), binding.getBindingName()));
        vo.setTitleTemplate(config.getString("titleTemplate"));
        vo.setStartMode(normalizeStartMode(config.getString("startMode")));
        vo.setVariableMapping(normalizeVariableMapping(config.getJSONArray("variableMapping")));
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
        vo.setStatus(1);
        vo.setCompatibilitySource("DOCUMENT_DEFAULT_FLOW");
        vo.setComplete(false);
        vo.setGaps(List.of("历史默认流程缺少变量映射，请在流程与自动化中保存一次主流程"));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("configured", true);
        summary.put("flowModelKey", documentConfig.getDefaultFlowKey());
        summary.put("flowModelName", documentConfig.getDefaultFlowKey());
        summary.put("startMode", "MANUAL");
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
        dto.setVariableMapping(normalizeVariableMapping(source.getJSONArray("variableMapping")));
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
        config.put("conditionFlows", dto.getConditionFlows() == null ? new ArrayList<>() : dto.getConditionFlows());
        config.put("options", dto.getOptions() == null ? new LinkedHashMap<>() : dto.getOptions());
        return config;
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
        config.put("variableMapping", binding.getVariableMapping());
        config.put("conditionFlows", binding.getConditionFlows());
        config.put("options", binding.getOptions());
        return config;
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

    private record BusinessKeyParts(String objectCode, Long recordId) {
    }
}
