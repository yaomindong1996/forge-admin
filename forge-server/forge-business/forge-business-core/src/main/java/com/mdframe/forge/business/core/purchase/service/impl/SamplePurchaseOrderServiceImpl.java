package com.mdframe.forge.business.core.purchase.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.business.core.purchase.domain.SamplePurchaseOrder;
import com.mdframe.forge.business.core.purchase.enums.SamplePurchaseOrderStatus;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderDTO;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderQuery;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderSubmitDTO;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderTaskSaveDTO;
import com.mdframe.forge.business.core.purchase.mapper.SamplePurchaseOrderMapper;
import com.mdframe.forge.business.core.purchase.service.SamplePurchaseOrderService;
import com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowBpmn;
import com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowDefinition;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderFlowInitVO;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderVO;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 采购单审批测试服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FlowBind(modelKey = SamplePurchaseOrderService.MODEL_KEY, businessType = SamplePurchaseOrderService.BUSINESS_TYPE)
public class SamplePurchaseOrderServiceImpl extends ServiceImpl<SamplePurchaseOrderMapper, SamplePurchaseOrder>
        implements SamplePurchaseOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FlowClient flowClient;
    private final IdentifierGenerator identifierGenerator;
    private final BusinessBindingMapper businessBindingMapper;

    @Override
    public IPage<SamplePurchaseOrderVO> page(PageQuery pageQuery, SamplePurchaseOrderQuery query) {
        Page<SamplePurchaseOrderVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        IPage<SamplePurchaseOrderVO> result = getBaseMapper().selectPage(page, resolveTenantId(), query);
        reconcileStatusesWithActiveTasks(result.getRecords());
        return result;
    }

    @Override
    public SamplePurchaseOrderVO detail(Long id) {
        if (id == null) {
            throw new BusinessException("采购单ID不能为空");
        }
        SamplePurchaseOrderVO detail = getBaseMapper().selectDetail(resolveTenantId(), id);
        if (detail == null) {
            throw new BusinessException("采购单不存在");
        }
        reconcileStatusesWithActiveTasks(List.of(detail));
        return detail;
    }

    @Override
    public List<SamplePurchaseOrderVO> detailsByIds(Collection<Long> ids) {
        List<Long> normalizedIds = ids == null ? List.of() : ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        List<SamplePurchaseOrderVO> details = getBaseMapper().selectDetailsByIds(resolveTenantId(), normalizedIds);
        reconcileStatusesWithActiveTasks(details);
        return details;
    }

    @Override
    public SamplePurchaseOrderVO detailByBusinessKey(String businessKey) {
        if (!StringUtils.hasText(businessKey)) {
            throw new BusinessException("采购单业务Key不能为空");
        }
        SamplePurchaseOrder entity = getBaseMapper().selectByBusinessKey(resolveTenantId(), businessKey.trim());
        if (entity == null) {
            throw new BusinessException("采购单不存在");
        }
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SamplePurchaseOrderDTO dto) {
        validateBase(dto);
        SamplePurchaseOrder entity = new SamplePurchaseOrder();
        entity.setId(identifierGenerator.nextId(entity).longValue());
        entity.setTenantId(resolveTenantId());
        applyBaseFields(entity, dto);
        entity.setStatus(SamplePurchaseOrderStatus.DRAFT.getCode());
        entity.setApplicantId(SessionHelper.getUserId());
        entity.setApplicantName(resolveUsername());
        entity.setApplicantDeptId(SessionHelper.getMainOrgId());
        entity.setApplicantDeptName(null);
        if (!StringUtils.hasText(entity.getOrderNo())) {
            entity.setOrderNo(generateOrderNo(entity.getId()));
        }
        save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SamplePurchaseOrderDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("采购单ID不能为空");
        }
        validateBase(dto);
        SamplePurchaseOrder entity = requireEntity(dto.getId());
        if (!SamplePurchaseOrderStatus.DRAFT.matches(entity.getStatus())
                && !SamplePurchaseOrderStatus.NEED_MODIFY.matches(entity.getStatus())) {
            throw new BusinessException("只有草稿或待修改采购单允许编辑");
        }
        applyBaseFields(entity, dto);
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SamplePurchaseOrder entity = requireEntity(id);
        if (!SamplePurchaseOrderStatus.deletableCodes().contains(entity.getStatus())) {
            throw new BusinessException("当前状态不允许删除");
        }
        removeById(id);
    }

    @Override
    public String submit(Long id, SamplePurchaseOrderSubmitDTO dto) {
        SamplePurchaseOrder entity = requireEntity(id);
        if (!SamplePurchaseOrderStatus.DRAFT.matches(entity.getStatus())) {
            throw new BusinessException("只有草稿采购单允许发起流程");
        }
        validateSubmit(dto);
        ensureFlowModel();

        String businessKey = buildBusinessKey(entity.getId());
        List<String> countersignUserList = dto.getCountersignUserIds().stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .toList();
        List<String> ccRoleKeys = normalizeRoleKeys(dto.getCcRoleKeys());
        String userId = SessionHelper.getUserId() == null ? null : String.valueOf(SessionHelper.getUserId());
        String userName = resolveUsername();

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_BUSINESS_KEY, businessKey);
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_OBJECT_CODE, BUSINESS_TYPE);
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_RECORD_ID, entity.getId());
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_PURCHASE_ORDER_ID, entity.getId());
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_ORDER_NO, entity.getOrderNo());
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_TITLE, entity.getTitle());
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_AMOUNT_CENT, entity.getAmountCent());
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_INITIATOR, userId);
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_DEPT_LEADER_ID, String.valueOf(dto.getDeptLeaderId()));
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_ENGINEERING_MANAGER_ID, String.valueOf(dto.getEngineeringManagerId()));
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_COUNTERSIGN_USER_LIST, countersignUserList);
        variables.put(SamplePurchaseOrderFlowDefinition.VAR_CC_ROLE_KEYS, ccRoleKeys);

        String flowTitle = buildConfiguredFlowTitle(entity, businessKey, variables);

        // 流程启动（远程 RPC）在事务外执行，避免长事务占用 DB 连接
        FlowResult<String> result = flowClient.startProcess(
                MODEL_KEY,
                businessKey,
                BUSINESS_TYPE,
                flowTitle,
                variables,
                userId,
                userName,
                SessionHelper.getMainOrgId() == null ? null : String.valueOf(SessionHelper.getMainOrgId()),
                null);
        if (result == null || !result.isSuccess() || !StringUtils.hasText(result.getData())) {
            throw new BusinessException("流程发起失败: " + (result == null ? "无返回结果" : result.getMsg()));
        }

        // 流程启动成功后更新单据状态（自动提交，不占用长事务）
        entity.setBusinessKey(businessKey);
        entity.setProcessInstanceId(result.getData());
        entity.setDeptLeaderId(dto.getDeptLeaderId());
        entity.setEngineeringManagerId(dto.getEngineeringManagerId());
        entity.setCountersignUserIds(joinLongs(dto.getCountersignUserIds()));
        entity.setCcRoleKeys(String.join(",", ccRoleKeys));
        markInProcess(entity);
        try {
            updateById(entity);
        } catch (Exception e) {
            log.error("流程已启动但单据状态更新失败，请人工处理: orderId={}, processInstanceId={}",
                    entity.getId(), result.getData(), e);
            throw new BusinessException("流程已启动但单据状态更新失败，请联系管理员处理");
        }
        return result.getData();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SamplePurchaseOrderVO saveTaskFields(SamplePurchaseOrderTaskSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException("保存参数不能为空");
        }
        SamplePurchaseOrder entity = resolveTaskEntity(dto);
        String taskDefKey = StringUtils.hasText(dto.getTaskDefKey()) ? dto.getTaskDefKey() : "";
        switch (taskDefKey) {
            case SamplePurchaseOrderFlowDefinition.NODE_APPLICANT_MODIFY -> {
                ensureApplicantModifyState(entity, dto);
                if (StringUtils.hasText(dto.getTitle())) {
                    entity.setTitle(dto.getTitle().trim());
                }
                if (StringUtils.hasText(dto.getSupplierName())) {
                    entity.setSupplierName(dto.getSupplierName().trim());
                }
                if (dto.getAmountCent() != null) {
                    if (dto.getAmountCent() <= 0) {
                        throw new BusinessException("采购金额必须大于0");
                    }
                    entity.setAmountCent(dto.getAmountCent());
                }
                if (dto.getNeedDate() != null) {
                    entity.setNeedDate(dto.getNeedDate());
                }
                entity.setPurchaseItems(dto.getPurchaseItems());
                entity.setApplicantModifyRemark(dto.getApplicantModifyRemark());
            }
            case SamplePurchaseOrderFlowDefinition.NODE_DEPT_LEADER_APPROVE -> {
                ensureApprovalState(entity, dto);
                entity.setDeptLeaderRemark(dto.getDeptLeaderRemark());
                entity.setArrivalListFileIds(dto.getArrivalListFileIds());
            }
            case SamplePurchaseOrderFlowDefinition.NODE_ENGINEERING_MANAGER_APPROVE -> {
                ensureApprovalState(entity, dto);
                entity.setEngineeringManagerRemark(dto.getEngineeringManagerRemark());
            }
            case SamplePurchaseOrderFlowDefinition.NODE_PURCHASE_COUNTERSIGN -> {
                ensureApprovalState(entity, dto);
                entity.setCountersignRemark(dto.getCountersignRemark());
            }
            default -> throw new BusinessException("当前节点不允许保存采购单字段: " + taskDefKey);
        }
        updateById(entity);
        return detail(entity.getId());
    }

    @Override
    public SamplePurchaseOrderFlowInitVO ensureFlowModel() {
        String bpmnXml = SamplePurchaseOrderFlowBpmn.build();
        FlowResult<Map<String, Object>> current = flowClient.getModelByKey(MODEL_KEY);
        Map<String, Object> model = current == null ? null : current.getData();
        boolean created = model == null || model.get("id") == null;
        boolean shouldDeploy = created;
        boolean preserved = false;

        if (created) {
            FlowResult<Map<String, Object>> createdResult = flowClient.createModel(buildFlowModelPayload(null, bpmnXml));
            if (createdResult == null || !createdResult.isSuccess() || createdResult.getData() == null) {
                throw new BusinessException("创建采购单测试流程失败: "
                        + (createdResult == null ? "无返回结果" : createdResult.getMsg()));
            }
            model = createdResult.getData();
        } else {
            String existingBpmnXml = Objects.toString(model.get("bpmnXml"), "");
            Integer status = toInteger(model.get("status"));
            shouldDeploy = status == null || status != 1 || !StringUtils.hasText(Objects.toString(model.get("deploymentId"), ""));
            if (!StringUtils.hasText(existingBpmnXml)) {
                FlowResult<Map<String, Object>> updatedResult = flowClient.updateModel(buildFlowModelPayload(model.get("id"), bpmnXml));
                if (updatedResult == null || !updatedResult.isSuccess() || updatedResult.getData() == null) {
                    throw new BusinessException("更新采购单测试流程失败: "
                            + (updatedResult == null ? "无返回结果" : updatedResult.getMsg()));
                }
                model = updatedResult.getData();
                shouldDeploy = true;
            } else {
                preserved = true;
            }
        }

        String deploymentId = Objects.toString(model.get("deploymentId"), null);
        if (shouldDeploy) {
            FlowResult<String> deployedResult = flowClient.deployModel(Objects.toString(model.get("id"), ""));
            if (deployedResult == null || !deployedResult.isSuccess()) {
                throw new BusinessException("发布采购单测试流程失败: "
                        + (deployedResult == null ? "无返回结果" : deployedResult.getMsg()));
            }
            deploymentId = deployedResult.getData();
        }

        SamplePurchaseOrderFlowInitVO vo = new SamplePurchaseOrderFlowInitVO();
        vo.setModelKey(MODEL_KEY);
        vo.setModelId(Objects.toString(model.get("id"), null));
        vo.setDeploymentId(deploymentId);
        vo.setStatus(shouldDeploy ? 1 : toInteger(model.get("status")));
        vo.setMessage(shouldDeploy
                ? preserved ? "采购单测试流程已发布，已保留流程设计器中的节点配置" : "采购单测试流程已初始化并发布"
                : preserved ? "采购单测试流程已存在，已保留流程设计器中的节点配置" : "采购单测试流程已存在，无需重复发布");
        return vo;
    }

    @FlowCallback(on = {
            FlowCallback.ON_TASK_CREATED,
            FlowCallback.ON_TASK_COMPLETED,
            FlowCallback.ON_COMPLETED,
            FlowCallback.ON_REJECTED,
            FlowCallback.ON_CANCELED
    })
    @Transactional(rollbackFor = Exception.class)
    public void handleFlowEvent(FlowEventContext context) {
        if (context == null || !StringUtils.hasText(context.getBusinessKey())) {
            return;
        }
        SamplePurchaseOrder entity = getBaseMapper().selectByBusinessKey(
                resolveTenantId(context.getTenantId()), context.getBusinessKey());
        if (entity == null) {
            log.warn("采购单流程回调未找到业务记录: event={}, businessKey={}",
                    context.getEvent(), context.getBusinessKey());
            return;
        }
        if (FlowCallback.ON_TASK_COMPLETED.equals(context.getEvent())) {
            handleTaskCompleted(entity, context);
            return;
        }
        if (FlowCallback.ON_TASK_CREATED.equals(context.getEvent())) {
            handleTaskCreated(entity, context);
            return;
        }
        if (FlowCallback.ON_COMPLETED.equals(context.getEvent())) {
            copyFinishVariables(entity, context.getVariables());
            markApproved(entity);
            updateById(entity);
            return;
        }
        if (FlowCallback.ON_REJECTED.equals(context.getEvent())) {
            markRejected(entity, firstText(context.getLastComment(), context.getComment(), entity.getRejectReason()));
            updateById(entity);
            return;
        }
        if (FlowCallback.ON_CANCELED.equals(context.getEvent())) {
            markCanceled(entity, firstText(context.getLastComment(), context.getComment(), entity.getRejectReason()));
            updateById(entity);
        }
    }

    private void handleTaskCreated(SamplePurchaseOrder entity, FlowEventContext context) {
        if (SamplePurchaseOrderFlowDefinition.isApplicantModifyNode(context.getTaskDefKey())) {
            if (!SamplePurchaseOrderStatus.IN_PROCESS.matches(entity.getStatus())) {
                return;
            }
            markNeedModify(entity, firstText(context.getComment(), context.getLastComment(), entity.getRejectReason()));
            updateById(entity);
            log.info("采购单进入申请人修改节点，状态自动切换为待修改: businessKey={}, taskId={}",
                    entity.getBusinessKey(), context.getTaskId());
            return;
        }
        if (SamplePurchaseOrderFlowDefinition.isApprovalNode(context.getTaskDefKey())
                && SamplePurchaseOrderStatus.NEED_MODIFY.matches(entity.getStatus())) {
            markInProcess(entity);
            updateById(entity);
            log.info("采购单进入审批节点，状态自动切换为审批中: businessKey={}, taskId={}, taskDefKey={}",
                    entity.getBusinessKey(), context.getTaskId(), context.getTaskDefKey());
        }
    }

    private void handleTaskCompleted(SamplePurchaseOrder entity, FlowEventContext context) {
        Map<String, Object> variables = context.getVariables();
        copyFinishVariables(entity, variables);
        if (isRejectAction(context)) {
            if (SamplePurchaseOrderFlowDefinition.isApplicantModifyNode(context.getTaskDefKey())) {
                markRejected(entity, firstText(context.getComment(), context.getLastComment(), entity.getRejectReason()));
            } else {
                markNeedModify(entity, firstText(context.getComment(), context.getLastComment(), entity.getRejectReason()));
            }
            updateById(entity);
            return;
        }
        if (SamplePurchaseOrderFlowDefinition.isApplicantModifyNode(context.getTaskDefKey())) {
            markInProcess(entity);
            updateById(entity);
        }
    }

    private boolean isRejectAction(FlowEventContext context) {
        if (context == null || context.getVariables() == null || context.getVariables().isEmpty()) {
            return false;
        }
        Map<String, Object> variables = context.getVariables();
        String approvalResult = Objects.toString(
                variables.get(SamplePurchaseOrderFlowDefinition.VAR_APPROVAL_RESULT), null);
        if (SamplePurchaseOrderFlowDefinition.isRejectAction(approvalResult)) {
            return true;
        }
        Object approved = variables.get(SamplePurchaseOrderFlowDefinition.VAR_APPROVED);
        if (approved instanceof Boolean value) {
            return !value;
        }
        if (approved != null) {
            return "false".equalsIgnoreCase(String.valueOf(approved))
                    || "0".equals(String.valueOf(approved));
        }
        return false;
    }

    private void ensureApplicantModifyState(SamplePurchaseOrder entity, SamplePurchaseOrderTaskSaveDTO dto) {
        if (SamplePurchaseOrderStatus.IN_PROCESS.matches(entity.getStatus())
                && dto != null
                && SamplePurchaseOrderFlowDefinition.isApplicantModifyNode(dto.getTaskDefKey())) {
            markNeedModify(entity, entity.getRejectReason());
            log.info("采购单申请人修改节点保存前修复待修改状态: businessKey={}, taskId={}",
                    entity.getBusinessKey(), dto.getTaskId());
            return;
        }
        if (!SamplePurchaseOrderStatus.NEED_MODIFY.matches(entity.getStatus())) {
            throw new BusinessException("当前采购单不是待修改状态，不能执行申请人修改节点");
        }
    }

    private void ensureApprovalState(SamplePurchaseOrder entity, SamplePurchaseOrderTaskSaveDTO dto) {
        if (SamplePurchaseOrderStatus.NEED_MODIFY.matches(entity.getStatus())
                && dto != null
                && SamplePurchaseOrderFlowDefinition.isApprovalNode(dto.getTaskDefKey())) {
            markInProcess(entity);
            log.info("采购单审批节点保存前修复审批中状态: businessKey={}, taskId={}, taskDefKey={}",
                    entity.getBusinessKey(), dto.getTaskId(), dto.getTaskDefKey());
            return;
        }
        if (!SamplePurchaseOrderStatus.IN_PROCESS.matches(entity.getStatus())) {
            throw new BusinessException("当前采购单不是审批中状态，不能保存审批节点字段");
        }
    }

    private void markInProcess(SamplePurchaseOrder entity) {
        entity.setStatus(SamplePurchaseOrderStatus.IN_PROCESS.getCode());
    }

    private void reconcileStatusesWithActiveTasks(List<SamplePurchaseOrderVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<String> businessKeys = new LinkedHashSet<>();
        for (SamplePurchaseOrderVO record : records) {
            if (record == null || !isRunningStatus(record.getStatus()) || !StringUtils.hasText(record.getBusinessKey())) {
                continue;
            }
            businessKeys.add(record.getBusinessKey());
        }
        if (businessKeys.isEmpty()) {
            return;
        }
        List<Map<String, Object>> activeTasks = getBaseMapper().selectActiveTaskDefKeysByBusinessKeys(resolveTenantId(), businessKeys);
        if (activeTasks == null || activeTasks.isEmpty()) {
            return;
        }
        Map<String, String> activeTaskIndex = new LinkedHashMap<>();
        for (Map<String, Object> task : activeTasks) {
            String businessKey = Objects.toString(task.get("businessKey"), "");
            String taskDefKey = Objects.toString(task.get("taskDefKey"), "");
            if (StringUtils.hasText(businessKey) && StringUtils.hasText(taskDefKey)) {
                activeTaskIndex.putIfAbsent(businessKey, taskDefKey);
            }
        }
        for (SamplePurchaseOrderVO record : records) {
            if (record == null || !isRunningStatus(record.getStatus())) {
                continue;
            }
            String taskDefKey = activeTaskIndex.get(record.getBusinessKey());
            String expectedStatus = expectedStatusByActiveTask(taskDefKey);
            if (!StringUtils.hasText(expectedStatus) || Objects.equals(expectedStatus, record.getStatus())) {
                continue;
            }
            SamplePurchaseOrder update = new SamplePurchaseOrder();
            update.setId(record.getId());
            update.setStatus(expectedStatus);
            updateById(update);
            record.setStatus(expectedStatus);
            log.info("采购单状态按当前待办节点对账修复: businessKey={}, taskDefKey={}, status={}",
                    record.getBusinessKey(), taskDefKey, expectedStatus);
        }
    }

    private boolean isRunningStatus(String status) {
        return SamplePurchaseOrderStatus.IN_PROCESS.matches(status)
                || SamplePurchaseOrderStatus.NEED_MODIFY.matches(status);
    }

    private String expectedStatusByActiveTask(String taskDefKey) {
        if (SamplePurchaseOrderFlowDefinition.isApplicantModifyNode(taskDefKey)) {
            return SamplePurchaseOrderStatus.NEED_MODIFY.getCode();
        }
        if (SamplePurchaseOrderFlowDefinition.isApprovalNode(taskDefKey)) {
            return SamplePurchaseOrderStatus.IN_PROCESS.getCode();
        }
        return null;
    }

    private void markNeedModify(SamplePurchaseOrder entity, String rejectReason) {
        entity.setStatus(SamplePurchaseOrderStatus.NEED_MODIFY.getCode());
        entity.setRejectReason(rejectReason);
    }

    private void markApproved(SamplePurchaseOrder entity) {
        entity.setStatus(SamplePurchaseOrderStatus.APPROVED.getCode());
    }

    private void markRejected(SamplePurchaseOrder entity, String rejectReason) {
        entity.setStatus(SamplePurchaseOrderStatus.REJECTED.getCode());
        entity.setRejectReason(rejectReason);
    }

    private void markCanceled(SamplePurchaseOrder entity, String rejectReason) {
        entity.setStatus(SamplePurchaseOrderStatus.CANCELED.getCode());
        entity.setRejectReason(rejectReason);
    }

    private void copyFinishVariables(SamplePurchaseOrder entity, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        Object arrivalListFileIds = variables.get(SamplePurchaseOrderFlowDefinition.FIELD_ARRIVAL_LIST_FILE_IDS);
        if (arrivalListFileIds != null) {
            entity.setArrivalListFileIds(String.valueOf(arrivalListFileIds));
        }
    }

    private Map<String, Object> buildFlowModelPayload(Object id, String bpmnXml) {
        return SamplePurchaseOrderFlowDefinition.flowModelPayload(id, resolveTenantId(), bpmnXml);
    }

    private String buildConfiguredFlowTitle(SamplePurchaseOrder entity,
                                            String businessKey,
                                            Map<String, Object> variables) {
        String fallback = "采购单审批-" + entity.getOrderNo();
        JSONObject bindingConfig = loadFlowBindingConfig();
        String template = trimToNull(bindingConfig.getString("titleTemplate"));
        if (template == null) {
            return fallback;
        }
        Map<String, Object> titleData = new LinkedHashMap<>();
        if (variables != null) {
            titleData.putAll(variables);
        }
        titleData.put("id", entity.getId());
        titleData.put("businessKey", businessKey);
        titleData.put("objectCode", BUSINESS_TYPE);
        titleData.put("businessType", BUSINESS_TYPE);
        titleData.put("modelKey", MODEL_KEY);
        titleData.put(SamplePurchaseOrderFlowDefinition.FIELD_ORDER_NO, entity.getOrderNo());
        titleData.put(SamplePurchaseOrderFlowDefinition.FIELD_TITLE, entity.getTitle());
        titleData.put(SamplePurchaseOrderFlowDefinition.FIELD_SUPPLIER_NAME, entity.getSupplierName());
        titleData.put(SamplePurchaseOrderFlowDefinition.FIELD_AMOUNT_CENT, entity.getAmountCent());
        titleData.put(SamplePurchaseOrderFlowDefinition.FIELD_NEED_DATE, entity.getNeedDate());
        titleData.put(SamplePurchaseOrderFlowDefinition.FIELD_APPLICANT_NAME, entity.getApplicantName());
        String title = template;
        for (Map.Entry<String, Object> entry : titleData.entrySet()) {
            title = replaceTemplateValue(title, entry.getKey(), entry.getValue());
        }
        return StringUtils.hasText(title) ? title : fallback;
    }

    private JSONObject loadFlowBindingConfig() {
        AiBusinessBinding binding = businessBindingMapper.selectBindingByTypeAndCode(
                resolveTenantId(), "OBJECT", BUSINESS_TYPE, "FLOW");
        if (binding == null) {
            binding = businessBindingMapper.selectBindingByTypeAndCode(
                    resolveTenantId(), "OBJECT", BUSINESS_TYPE, "APPROVAL");
        }
        if (binding == null || !StringUtils.hasText(binding.getBindingConfig())) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(binding.getBindingConfig());
        } catch (Exception e) {
            log.warn("采购单流程标题模板配置解析失败: bindingId={}", binding.getId(), e);
            return new JSONObject();
        }
    }

    private String replaceTemplateValue(String template, String key, Object value) {
        if (!StringUtils.hasText(template) || !StringUtils.hasText(key)) {
            return template;
        }
        String text = value == null ? "" : String.valueOf(value);
        String result = replaceTemplateToken(template, key, text);
        result = replaceTemplateToken(result, snakeToCamel(key), text);
        return replaceTemplateToken(result, camelToSnake(key), text);
    }

    private String replaceTemplateToken(String template, String key, String value) {
        if (!StringUtils.hasText(key)) {
            return template;
        }
        return template.replace("${" + key + "}", value)
                .replace("{" + key + "}", value);
    }

    private String snakeToCamel(String value) {
        if (!StringUtils.hasText(value) || !value.contains("_")) {
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
        if (!StringUtils.hasText(value)) {
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateBase(SamplePurchaseOrderDTO dto) {
        if (dto == null) {
            throw new BusinessException("采购单参数不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException("采购主题不能为空");
        }
        if (!StringUtils.hasText(dto.getSupplierName())) {
            throw new BusinessException("供应商不能为空");
        }
        if (dto.getAmountCent() == null || dto.getAmountCent() <= 0) {
            throw new BusinessException("采购金额必须大于0");
        }
    }

    private void validateSubmit(SamplePurchaseOrderSubmitDTO dto) {
        if (dto == null) {
            throw new BusinessException("审批配置不能为空");
        }
        if (dto.getDeptLeaderId() == null) {
            throw new BusinessException("请选择部门负责人");
        }
        if (dto.getEngineeringManagerId() == null) {
            throw new BusinessException("请选择工程部经理");
        }
        if (dto.getCountersignUserIds() == null || dto.getCountersignUserIds().stream().filter(Objects::nonNull).distinct().count() < 2) {
            throw new BusinessException("会签节点至少选择2名人员");
        }
    }

    private void applyBaseFields(SamplePurchaseOrder entity, SamplePurchaseOrderDTO dto) {
        BeanUtils.copyProperties(dto, entity,
                SamplePurchaseOrderFlowDefinition.FIELD_ID,
                SamplePurchaseOrderFlowDefinition.FIELD_STATUS,
                SamplePurchaseOrderFlowDefinition.FIELD_BUSINESS_KEY,
                SamplePurchaseOrderFlowDefinition.FIELD_PROCESS_INSTANCE_ID);
        if (StringUtils.hasText(dto.getTitle())) {
            entity.setTitle(dto.getTitle().trim());
        }
        if (StringUtils.hasText(dto.getSupplierName())) {
            entity.setSupplierName(dto.getSupplierName().trim());
        }
    }

    private SamplePurchaseOrder resolveTaskEntity(SamplePurchaseOrderTaskSaveDTO dto) {
        if (dto.getId() != null) {
            return requireEntity(dto.getId());
        }
        if (StringUtils.hasText(dto.getBusinessKey())) {
            SamplePurchaseOrder entity = getBaseMapper().selectByBusinessKey(resolveTenantId(), dto.getBusinessKey());
            if (entity != null) {
                return entity;
            }
        }
        throw new BusinessException("未找到待办关联的采购单");
    }

    private SamplePurchaseOrder requireEntity(Long id) {
        if (id == null) {
            throw new BusinessException("采购单ID不能为空");
        }
        SamplePurchaseOrder entity = getById(id);
        if (entity == null || !Objects.equals(entity.getTenantId(), resolveTenantId())) {
            throw new BusinessException("采购单不存在");
        }
        return entity;
    }

    private String buildBusinessKey(Long id) {
        return SamplePurchaseOrderFlowDefinition.businessKey(id);
    }

    private String generateOrderNo(Long id) {
        String suffix = String.valueOf(id);
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        return "PO" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + suffix;
    }

    private String resolveUsername() {
        return StringUtils.hasText(SessionHelper.getUsername()) ? SessionHelper.getUsername() : "system";
    }

    private Long resolveTenantId() {
        return resolveTenantId(null);
    }

    private Long resolveTenantId(Long eventTenantId) {
        if (eventTenantId != null) {
            return eventTenantId;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        tenantId = SessionHelper.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private List<String> normalizeRoleKeys(List<String> roleKeys) {
        List<String> source = roleKeys == null || roleKeys.isEmpty() ? List.of("admin") : roleKeys;
        Set<String> result = new LinkedHashSet<>();
        for (String roleKey : source) {
            if (StringUtils.hasText(roleKey)) {
                result.add(roleKey.trim());
            }
        }
        if (result.isEmpty()) {
            result.add("admin");
        }
        return new ArrayList<>(result);
    }

    private String joinLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstText(String... values) {
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
