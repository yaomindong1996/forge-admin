package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.FlowEntrySubmitDTO;
import com.mdframe.forge.starter.flow.entity.FlowEntry;
import com.mdframe.forge.starter.flow.entity.FlowEntryFieldMapping;
import com.mdframe.forge.starter.flow.entity.FlowFillBatchItem;
import com.mdframe.forge.starter.flow.entity.FlowFormInstance;
import com.mdframe.forge.starter.flow.enums.FlowFillItemStatus;
import com.mdframe.forge.starter.flow.enums.FlowFormInstanceStatus;
import com.mdframe.forge.starter.flow.mapper.FlowEntryFieldMappingMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFillBatchItemMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.service.FlowBusinessObjectRuntimeAdapter;
import com.mdframe.forge.starter.flow.service.FlowEntryService;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.FlowRuntimeService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import com.mdframe.forge.starter.flow.vo.FlowEntryRuntimeVO;
import com.mdframe.forge.starter.flow.vo.FlowFormInstanceVO;
import com.mdframe.forge.starter.flow.vo.FlowStartResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程入口运行态服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowRuntimeServiceImpl implements FlowRuntimeService {

    private static final String PROCESS_ONLY = "PROCESS_ONLY";
    private static final String BUSINESS_OBJECT = "BUSINESS_OBJECT";
    private static final String HYBRID = "HYBRID";

    private final FlowEntryService flowEntryService;
    private final FlowInstanceService flowInstanceService;
    private final FlowFormInstanceMapper formInstanceMapper;
    private final FlowEntryFieldMappingMapper mappingMapper;
    private final FlowFillBatchItemMapper fillBatchItemMapper;
    private final FlowTaskService flowTaskService;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private FlowBusinessObjectRuntimeAdapter businessObjectRuntimeAdapter;

    @Override
    public FlowEntryRuntimeVO getRuntimeEntry(String entryCode) {
        return flowEntryService.getRuntimeEntry(entryCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowStartResultVO submitEntryForm(String entryCode, FlowEntrySubmitDTO dto) {
        FlowEntryRuntimeVO runtime = flowEntryService.getRuntimeEntry(entryCode);
        FlowEntry entry = runtime.getEntry();
        String dataMode = StringUtils.hasText(entry.getDataMode()) ? entry.getDataMode() : PROCESS_ONLY;
        Map<String, Object> formData = dto != null && dto.getFormData() != null ? dto.getFormData() : new HashMap<>();
        List<FlowEntryFieldMapping> mappings = mappingMapper.selectByEntryId(entry.getId());

        if (dto != null && dto.getBatchItemId() != null) {
            return submitBatchItem(runtime, mappings, formData, dto, dataMode);
        }
        return submitByMode(runtime, mappings, formData, dto, dataMode);
    }

    @Override
    public FlowFormInstanceVO getInstanceByProcessInstanceId(String processInstanceId) {
        Long tenantId = requireTenantIdForRead();
        FlowFormInstance instance = formInstanceMapper.selectByProcessInstanceIdAndTenantId(
                processInstanceId, tenantId);
        FlowFormInstanceVO vo = new FlowFormInstanceVO();
        vo.setInstance(instance);
        if (instance != null && StringUtils.hasText(instance.getProcessInstanceId())) {
            vo.setHistory(flowTaskService.getProcessHistory(instance.getProcessInstanceId()));
            try {
                vo.setDiagram(flowTaskService.getProcessDiagramInfo(instance.getProcessInstanceId(), false));
            } catch (Exception e) {
                log.warn("获取流程图详情失败: processInstanceId={}", instance.getProcessInstanceId(), e);
            }
        }
        return vo;
    }

    private FlowStartResultVO submitProcessOnly(FlowEntryRuntimeVO runtime,
                                               List<FlowEntryFieldMapping> mappings,
                                               Map<String, Object> formData,
                                               FlowEntrySubmitDTO dto) {
        FlowEntry entry = runtime.getEntry();
        Long instanceId = IdWorker.getId();
        String businessKey = resolveBusinessKey(entry, instanceId);
        String title = resolveTitle(entry, formData, dto);
        StarterContext starter = resolveStarter(dto);

        FlowFormInstance instance = new FlowFormInstance();
        instance.setId(instanceId);
        instance.setTenantId(resolveTenantId());
        instance.setEntryId(entry.getId());
        instance.setEntryCode(entry.getEntryCode());
        instance.setBusinessKey(businessKey);
        instance.setModelKey(entry.getModelKey());
        instance.setFormKey(runtime.getFormVersion().getFormKey());
        instance.setFormVersionId(runtime.getFormVersion().getId());
        instance.setFormVersion(runtime.getFormVersion().getVersion());
        instance.setSchemaSnapshot(runtime.getFormVersion().getFormSchema());
        instance.setFieldRegistry(runtime.getFormVersion().getFieldRegistry());
        instance.setFormData(toJson(formData));
        instance.setDataMode(PROCESS_ONLY);
        instance.setTitle(title);
        instance.setStartUserId(starter.userId);
        instance.setStartUserName(starter.userName);
        instance.setStartDeptId(starter.deptId);
        instance.setStartDeptName(starter.deptName);
        instance.setStatus(FlowFormInstanceStatus.DRAFT.getCode());
        instance.setSubmitTime(LocalDateTime.now());
        formInstanceMapper.insert(instance);

        Map<String, Object> variables = buildFlowVariables(formData, mappings, dto);
        variables.put("flowEntryCode", entry.getEntryCode());
        variables.put("flowFormInstanceId", String.valueOf(instanceId));

        String processInstanceId = flowInstanceService.startProcess(
                entry.getModelKey(),
                businessKey,
                PROCESS_ONLY,
                title,
                variables,
                toStringValue(starter.userId),
                starter.userName,
                toStringValue(starter.deptId),
                starter.deptName);

        formInstanceMapper.updateProcessInstance(instanceId, processInstanceId, FlowFormInstanceStatus.RUNNING.getCode());

        FlowStartResultVO result = new FlowStartResultVO();
        result.setFormInstanceId(instanceId);
        result.setBusinessKey(businessKey);
        result.setProcessInstanceId(processInstanceId);
        result.setDataMode(PROCESS_ONLY);
        return result;
    }

    private FlowStartResultVO submitBatchItem(FlowEntryRuntimeVO runtime,
                                              List<FlowEntryFieldMapping> mappings,
                                              Map<String, Object> formData,
                                              FlowEntrySubmitDTO dto,
                                              String dataMode) {
        FlowFillBatchItem item = fillBatchItemMapper.selectByIdForUpdate(dto.getBatchItemId());
        if (item == null) {
            throw new RuntimeException("填报明细不存在");
        }
        FlowEntry entry = runtime.getEntry();
        if (!entry.getEntryCode().equals(item.getEntryCode())) {
            throw new RuntimeException("填报明细与流程入口不匹配");
        }
        Map<String, Object> variables = dto.getVariables() == null ? new HashMap<>() : new HashMap<>(dto.getVariables());
        variables.put("flowBatchItemId", String.valueOf(item.getId()));
        variables.put("flowBatchId", item.getBatchId() == null ? null : String.valueOf(item.getBatchId()));
        variables.put("flowOrgId", item.getOrgId() == null ? null : String.valueOf(item.getOrgId()));
        variables.put("flowOrgName", item.getOrgName());
        variables.put("flowOwnerUserId", item.getOwnerUserId() == null ? null : String.valueOf(item.getOwnerUserId()));
        variables.put("flowOwnerUserName", item.getOwnerUserName());
        dto.setVariables(variables);

        FlowStartResultVO result = submitByMode(runtime, mappings, formData, dto, dataMode);
        FlowFillBatchItem update = new FlowFillBatchItem();
        update.setId(item.getId());
        update.setFormInstanceId(result.getFormInstanceId());
        update.setObjectCode(result.getObjectCode());
        update.setRecordId(result.getRecordId());
        update.setProcessInstanceId(result.getProcessInstanceId());
        update.setSubmitStatus(FlowFillItemStatus.SUBMITTED.getCode());
        update.setFlowStatus(FlowFillItemStatus.RUNNING.getCode());
        update.setSubmitTime(LocalDateTime.now());
        fillBatchItemMapper.updateById(update);
        return result;
    }

    private FlowStartResultVO submitByMode(FlowEntryRuntimeVO runtime,
                                           List<FlowEntryFieldMapping> mappings,
                                           Map<String, Object> formData,
                                           FlowEntrySubmitDTO dto,
                                           String dataMode) {
        FlowEntry entry = runtime.getEntry();
        if (BUSINESS_OBJECT.equals(dataMode)) {
            return submitBusinessObject(entry, mappings, formData, dto, false);
        }
        if (HYBRID.equals(dataMode)) {
            return submitBusinessObject(entry, mappings, formData, dto, true);
        }
        return submitProcessOnly(runtime, mappings, formData, dto);
    }

    private FlowStartResultVO submitBusinessObject(FlowEntry entry,
                                                   List<FlowEntryFieldMapping> mappings,
                                                   Map<String, Object> formData,
                                                   FlowEntrySubmitDTO dto,
                                                   boolean keepSnapshot) {
        if (businessObjectRuntimeAdapter == null) {
            throw new RuntimeException("业务对象落表扩展未启用，当前入口不能使用 " + entry.getDataMode() + " 模式");
        }
        FlowBusinessObjectRuntimeAdapter.BusinessRecordCreateResult record =
                businessObjectRuntimeAdapter.createBusinessRecord(entry, mappings, formData);
        if (record == null || record.getRecordId() == null) {
            throw new RuntimeException("业务对象落表失败，未返回业务记录ID");
        }

        Map<String, Object> variables = buildFlowVariables(formData, mappings, dto);
        if (record.getVariables() != null) {
            variables.putAll(record.getVariables());
        }
        String objectCode = StringUtils.hasText(record.getObjectCode()) ? record.getObjectCode() : entry.getObjectCode();
        String businessKey = StringUtils.hasText(record.getBusinessKey())
                ? record.getBusinessKey() : objectCode + ":" + record.getRecordId();
        String title = resolveTitle(entry, formData, dto);
        StarterContext starter = resolveStarter(dto);

        Long formInstanceId = null;
        if (keepSnapshot) {
            formInstanceId = IdWorker.getId();
            FlowEntryRuntimeVO runtime = flowEntryService.getRuntimeEntry(entry.getEntryCode());
            FlowFormInstance instance = new FlowFormInstance();
            instance.setId(formInstanceId);
            instance.setTenantId(resolveTenantId());
            instance.setEntryId(entry.getId());
            instance.setEntryCode(entry.getEntryCode());
            instance.setBusinessKey(businessKey);
            instance.setModelKey(entry.getModelKey());
            instance.setFormKey(runtime.getFormVersion().getFormKey());
            instance.setFormVersionId(runtime.getFormVersion().getId());
            instance.setFormVersion(runtime.getFormVersion().getVersion());
            instance.setSchemaSnapshot(runtime.getFormVersion().getFormSchema());
            instance.setFieldRegistry(runtime.getFormVersion().getFieldRegistry());
            instance.setFormData(toJson(formData));
            instance.setDataMode(HYBRID);
            instance.setObjectCode(objectCode);
            instance.setRecordId(record.getRecordId());
            instance.setTitle(title);
            instance.setStartUserId(starter.userId);
            instance.setStartUserName(starter.userName);
            instance.setStartDeptId(starter.deptId);
            instance.setStartDeptName(starter.deptName);
            instance.setStatus(FlowFormInstanceStatus.DRAFT.getCode());
            instance.setSubmitTime(LocalDateTime.now());
            formInstanceMapper.insert(instance);
            variables.put("flowFormInstanceId", String.valueOf(formInstanceId));
        }

        String processInstanceId = flowInstanceService.startProcess(
                entry.getModelKey(),
                businessKey,
                entry.getDataMode(),
                title,
                variables,
                toStringValue(starter.userId),
                starter.userName,
                toStringValue(starter.deptId),
                starter.deptName);

        if (formInstanceId != null) {
            formInstanceMapper.updateProcessInstance(formInstanceId, processInstanceId, FlowFormInstanceStatus.RUNNING.getCode());
        }
        businessObjectRuntimeAdapter.afterProcessStarted(entry, record, processInstanceId, variables);

        FlowStartResultVO result = new FlowStartResultVO();
        result.setFormInstanceId(formInstanceId);
        result.setBusinessKey(businessKey);
        result.setProcessInstanceId(processInstanceId);
        result.setDataMode(entry.getDataMode());
        result.setObjectCode(objectCode);
        result.setRecordId(record.getRecordId());
        return result;
    }

    private Map<String, Object> buildFlowVariables(Map<String, Object> formData,
                                                   List<FlowEntryFieldMapping> mappings,
                                                   FlowEntrySubmitDTO dto) {
        Map<String, Object> variables = new HashMap<>();
        if (formData != null) {
            variables.putAll(formData);
        }
        if (dto != null && dto.getVariables() != null) {
            variables.putAll(dto.getVariables());
        }
        if (mappings != null) {
            for (FlowEntryFieldMapping mapping : mappings) {
                if (mapping == null || !StringUtils.hasText(mapping.getFormField())) {
                    continue;
                }
                if (!"FLOW_VARIABLE".equals(mapping.getTargetType()) && !"FLOW_AND_BUSINESS".equals(mapping.getTargetType())) {
                    continue;
                }
                String variable = firstNotBlank(mapping.getFlowVariable(), mapping.getTargetField(), mapping.getFormField());
                variables.put(variable, formData == null ? null : formData.get(mapping.getFormField()));
            }
        }
        return variables;
    }

    private String resolveBusinessKey(FlowEntry entry, Long instanceId) {
        if (StringUtils.hasText(entry.getBusinessKeyTemplate())) {
            return entry.getBusinessKeyTemplate()
                    .replace("{entryCode}", entry.getEntryCode())
                    .replace("{instanceId}", String.valueOf(instanceId));
        }
        return "FLOW_FORM:" + entry.getEntryCode() + ":" + instanceId;
    }

    private String resolveTitle(FlowEntry entry, Map<String, Object> formData, FlowEntrySubmitDTO dto) {
        if (dto != null && StringUtils.hasText(dto.getTitle())) {
            return dto.getTitle();
        }
        String template = entry.getTitleTemplate();
        if (!StringUtils.hasText(template)) {
            return entry.getEntryName();
        }
        String title = template;
        if (formData != null) {
            for (Map.Entry<String, Object> item : formData.entrySet()) {
                String value = item.getValue() == null ? "" : String.valueOf(item.getValue());
                title = title.replace("{" + item.getKey() + "}", value)
                        .replace("${" + item.getKey() + "}", value);
            }
        }
        title = title.replace("{entryName}", entry.getEntryName())
                .replace("${entryName}", entry.getEntryName());
        return title;
    }

    private StarterContext resolveStarter(FlowEntrySubmitDTO dto) {
        StarterContext context = new StarterContext();
        LoginUser loginUser = null;
        try {
            loginUser = SessionHelper.getLoginUser();
        } catch (Exception ignored) {
            // Flow 服务独立部署调试时可能没有 Sa-Token 上下文。
        }
        if (loginUser != null) {
            context.userId = loginUser.getUserId();
            context.userName = firstNotBlank(loginUser.getRealName(), loginUser.getUsername());
            context.deptId = loginUser.getMainOrgId();
            context.deptName = loginUser.getDeptName();
        }
        if (dto != null) {
            context.userId = context.userId != null ? context.userId : parseLong(dto.getStartUserId());
            context.userName = firstNotBlank(context.userName, dto.getStartUserName());
            context.deptId = context.deptId != null ? context.deptId : parseLong(dto.getStartDeptId());
            context.deptName = firstNotBlank(context.deptName, dto.getStartDeptName());
        }
        return context;
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }

    private Long requireTenantIdForRead() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TENANT_REQUIRED");
        }
        return tenantId;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new HashMap<>() : value);
        } catch (Exception e) {
            throw new RuntimeException("表单数据序列化失败", e);
        }
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toStringValue(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private static class StarterContext {
        private Long userId;
        private String userName;
        private Long deptId;
        private String deptName;
    }
}
