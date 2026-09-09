package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayAdapter;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowCc;
import com.mdframe.forge.starter.flow.entity.FlowRecordParticipant;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.enums.FlowCcStatus;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowCcMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowCcService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.FlowRecordParticipantService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程抄送服务实现
 */
@Slf4j
@Service
public class FlowCcServiceImpl extends ServiceImpl<FlowCcMapper, FlowCc> implements FlowCcService {

    @Autowired(required = false)
    private FlowBusinessListDisplayAdapter flowBusinessListDisplayAdapter;

    @Autowired(required = false)
    private FlowRecordParticipantService flowRecordParticipantService;

    @Autowired(required = false)
    private FlowBusinessMapper flowBusinessMapper;

    @Autowired(required = false)
    private FlowTaskMapper flowTaskMapper;

    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCc(String processInstanceId, String processDefKey, String taskId,
                       String title, String content, String businessKey,
                       List<String> ccUserIds, List<String> ccUserNames,
                       String sendUserId, String sendUserName) {
        validateSendContext(processInstanceId, processDefKey, taskId, businessKey,
                ccUserIds, sendUserId, false);
        persistCc(processInstanceId, processDefKey, taskId, title, content, businessKey,
                ccUserIds, ccUserNames, sendUserId, sendUserName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCcByCurrentUser(String processInstanceId, String processDefKey, String taskId,
                                    String title, String content, String businessKey,
                                    List<String> ccUserIds, List<String> ccUserNames,
                                    String sendUserId, String sendUserName) {
        String currentUserId = requireUserId();
        if (sendUserId == null || !currentUserId.equals(sendUserId.trim())) {
            throw new IllegalArgumentException("FLOW_CC_SENDER_MISMATCH");
        }
        validateSendContext(processInstanceId, processDefKey, taskId, businessKey,
                ccUserIds, currentUserId, true);
        persistCc(processInstanceId, processDefKey, taskId, title, content, businessKey,
                ccUserIds, ccUserNames, currentUserId, sendUserName);
    }

    private void persistCc(String processInstanceId, String processDefKey, String taskId,
                           String title, String content, String businessKey,
                           List<String> ccUserIds, List<String> ccUserNames,
                           String sendUserId, String sendUserName) {
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < ccUserIds.size(); i++) {
            FlowCc cc = new FlowCc();
            cc.setTenantId(requireTenant());
            cc.setProcessInstanceId(processInstanceId);
            cc.setProcessDefKey(processDefKey);
            cc.setTaskId(taskId);
            cc.setTitle(title);
            cc.setContent(content);
            cc.setBusinessKey(businessKey);
            cc.setCcUserId(ccUserIds.get(i));
            cc.setCcUserName(ccUserNames != null && i < ccUserNames.size() ? ccUserNames.get(i) : null);
            cc.setSendUserId(sendUserId);
            cc.setSendUserName(sendUserName);
            cc.setCcTime(now);
            cc.setIsRead(0);
            cc.setStatus(FlowCcStatus.ACTIVE.getCode());
            
            save(cc);
            recordCcParticipant(processInstanceId, businessKey, ccUserIds.get(i));
        }
        
        log.info("发送抄送：processInstanceId={}, ccUserIds={}", processInstanceId, ccUserIds);
    }

    /**
     * 校验人工/流程回调抄送的流程归属。流程回调可能没有 HTTP 会话，故不在这里强制当前会话用户。
     */
    private void validateSendContext(String processInstanceId, String processDefKey, String taskId,
                                     String businessKey, List<String> ccUserIds,
                                     String senderId, boolean requireCurrentUser) {
        Long tenantId = requireTenant();
        if (processInstanceId == null || processInstanceId.isBlank()
                || senderId == null || senderId.isBlank()
                || ccUserIds == null || ccUserIds.isEmpty() || ccUserIds.size() > 100) {
            throw new IllegalArgumentException("FLOW_CC_CONTEXT_INVALID");
        }
        if (requireCurrentUser && !senderId.trim().equals(requireUserId())) {
            throw new IllegalArgumentException("FLOW_CC_SENDER_MISMATCH");
        }
        if (flowBusinessMapper == null || flowTaskMapper == null) {
            throw new IllegalStateException("FLOW_CC_AUTHORIZATION_UNAVAILABLE");
        }
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceIdAndTenantId(
                processInstanceId.trim(), tenantId);
        // 流程启动阶段 TASK_CREATED 可能早于 process_instance_id 回写，使用已校验的业务键兜底。
        if (business == null && !requireCurrentUser && businessKey != null && !businessKey.isBlank()) {
            business = flowBusinessMapper.selectByBusinessKeyAndTenantId(tenantId, businessKey.trim());
        }
        if (business == null
                || (businessKey != null && !businessKey.isBlank()
                    && !businessKey.equals(business.getBusinessKey()))
                || (processDefKey != null && !processDefKey.isBlank()
                    && !processDefKey.equals(business.getProcessDefKey()))) {
            throw new IllegalArgumentException("FLOW_RESOURCE_NOT_FOUND");
        }

        boolean starter = senderId.trim().equals(String.valueOf(business.getApplyUserId()));
        boolean participant = flowTaskMapper.countProcessParticipant(
                processInstanceId.trim(), senderId.trim(), tenantId) > 0;
        if (taskId != null && !taskId.isBlank()) {
            FlowTask task = flowTaskMapper.selectByIdOrTaskIdAndTenant(taskId.trim(), tenantId);
            // serviceTask 抄送节点使用 BPMN activityId，不一定有 sys_flow_task 快照；
            // 内部回调可退回到已验证的流程发起人/历史参与人，人工接口必须命中真实任务。
            boolean serviceTaskCallback = task == null && !requireCurrentUser && (starter || participant);
            if (!serviceTaskCallback && (task == null || !processInstanceId.trim().equals(task.getProcessInstanceId())
                    || !isTaskParticipant(task, senderId.trim()))) {
                throw new IllegalArgumentException("FLOW_RESOURCE_NOT_FOUND");
            }
        } else if (!starter && !participant) {
            throw new IllegalArgumentException("FLOW_CC_SENDER_NOT_PARTICIPANT");
        }

        for (String ccUserId : ccUserIds) {
            if (ccUserId == null || ccUserId.isBlank()
                    || flowOrgIntegrationService == null
                    || !flowOrgIntegrationService.isUserAvailableForTenant(ccUserId.trim(), tenantId)) {
                throw new IllegalArgumentException("FLOW_CC_TARGET_INVALID");
            }
        }
    }

    private boolean isTaskParticipant(FlowTask task, String userId) {
        return userId.equals(String.valueOf(task.getAssignee()))
                || userId.equals(String.valueOf(task.getOwner()))
                || userId.equals(String.valueOf(task.getStartUserId()))
                || containsCsv(task.getCandidateUsers(), userId);
    }

    private boolean containsCsv(String csv, String value) {
        if (csv == null || csv.isBlank()) {
            return false;
        }
        for (String item : csv.split(",")) {
            if (value.equals(item.trim())) {
                return true;
            }
        }
        return false;
    }

    private void recordCcParticipant(String processInstanceId, String businessKey, String ccUserId) {
        if (flowRecordParticipantService == null) {
            return;
        }
        FlowBusiness business = null;
        Long tenantId = SessionHelper.getTenantId();
        if (flowBusinessMapper != null && processInstanceId != null && tenantId != null && tenantId > 0) {
            business = flowBusinessMapper.selectByProcessInstanceIdAndTenantId(processInstanceId, tenantId);
        }
        if (business != null) {
            flowRecordParticipantService.record(business, ccUserId, FlowRecordParticipant.CC);
            return;
        }
        flowRecordParticipantService.record(null, null, businessKey, processInstanceId,
                ccUserId, FlowRecordParticipant.CC);
    }

    @Override
    public IPage<FlowCc> myCc(Page<FlowCc> page, String userId, Integer isRead, String title) {
        return enrichCcPage(baseMapper.selectMyPage(
                page, userId, requireTenant(), isRead, normalizeSearchText(title)));
    }

    @Override
    public IPage<FlowCc> sentCc(Page<FlowCc> page, String userId, String title) {
        return enrichCcPage(baseMapper.selectSentPage(
                page, userId, requireTenant(), normalizeSearchText(title)));
    }

    private IPage<FlowCc> enrichCcPage(IPage<FlowCc> page) {
        if (flowBusinessListDisplayAdapter == null || page == null || page.getRecords() == null
                || page.getRecords().isEmpty()) {
            return page;
        }
        List<FlowBusinessListDisplayItem> items = page.getRecords().stream()
                .map(this::toDisplayItem)
                .collect(Collectors.toList());
        try {
            flowBusinessListDisplayAdapter.enrich(items);
            for (int i = 0; i < page.getRecords().size(); i++) {
                applyDisplayItem(page.getRecords().get(i), items.get(i));
            }
        } catch (Exception e) {
            log.warn("补齐流程抄送业务摘要失败，继续返回流程基础信息: {}", e.getMessage());
        }
        return page;
    }

    private FlowBusinessListDisplayItem toDisplayItem(FlowCc cc) {
        FlowBusinessListDisplayItem item = new FlowBusinessListDisplayItem();
        item.setBusinessKey(cc.getBusinessKey());
        item.setProcessInstanceId(cc.getProcessInstanceId());
        item.setProcessDefKey(cc.getProcessDefKey());
        item.setProcessName(cc.getProcessName());
        item.setProcessDefinitionName(cc.getProcessDefinitionName());
        item.setTaskId(cc.getTaskId());
        item.setTitle(cc.getTitle());
        item.setObjectCode(cc.getObjectCode());
        item.setRecordId(cc.getRecordId());
        item.setBusinessObjectName(cc.getBusinessObjectName());
        item.setBusinessSummary(cc.getBusinessSummary());
        return item;
    }

    private void applyDisplayItem(FlowCc cc, FlowBusinessListDisplayItem item) {
        if (item == null) {
            return;
        }
        cc.setObjectCode(firstNonBlank(item.getObjectCode(), cc.getObjectCode()));
        cc.setRecordId(item.getRecordId() != null ? item.getRecordId() : cc.getRecordId());
        cc.setBusinessObjectName(firstNonBlank(item.getBusinessObjectName(), cc.getBusinessObjectName()));
        cc.setBusinessSummary(firstNonBlank(item.getBusinessSummary(), cc.getBusinessSummary()));
        cc.setProcessName(firstNonBlank(cc.getProcessName(), item.getProcessName()));
        cc.setProcessDefinitionName(firstNonBlank(
                cc.getProcessDefinitionName(),
                item.getProcessDefinitionName(),
                cc.getProcessName(),
                cc.getProcessDefKey()));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(String id) {
        String userId = requireUserId();
        LambdaUpdateWrapper<FlowCc> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FlowCc::getId, id)
                .eq(FlowCc::getTenantId, requireTenant())
                .eq(FlowCc::getCcUserId, userId)
                .eq(FlowCc::getStatus, FlowCcStatus.ACTIVE.getCode())
                .set(FlowCc::getIsRead, 1)
                .set(FlowCc::getReadTime, LocalDateTime.now());
        if (!update(wrapper)) {
            throw new IllegalArgumentException("FLOW_CC_NOT_VISIBLE");
        }
        log.info("标记抄送已读：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkRead(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        LambdaUpdateWrapper<FlowCc> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(FlowCc::getId, ids)
                .eq(FlowCc::getTenantId, requireTenant())
                .eq(FlowCc::getCcUserId, requireUserId())
                .eq(FlowCc::getStatus, FlowCcStatus.ACTIVE.getCode())
                .set(FlowCc::getIsRead, 1)
                .set(FlowCc::getReadTime, LocalDateTime.now());
        update(wrapper);
        log.info("批量标记抄送已读：{}条", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead() {
        int updated = baseMapper.markAllRead(requireUserId(), requireTenant(), LocalDateTime.now());
        log.info("全部标记抄送已读：{}条", updated);
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String id, String reason) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("FLOW_CC_ID_REQUIRED");
        }
        String senderId = requireUserId();
        int updated = baseMapper.revokeBySender(id.trim(), requireTenant(), senderId,
                reason == null ? null : reason.trim(), LocalDateTime.now(), FlowCcStatus.REVOKED.getCode());
        if (updated == 0) {
            throw new IllegalArgumentException("FLOW_CC_REVOKE_NOT_ALLOWED");
        }
        log.info("撤回流程抄送: id={}, senderId={}", id, senderId);
    }

    @Override
    public long countUnread(String userId) {
        Long count = baseMapper.countWorkspaceUnread(userId, requireTenant());
        return count == null ? 0L : count;
    }

    @Override
    public FlowCc getVisibleById(String id, String userId) {
        if (id == null || id.isBlank() || userId == null || userId.isBlank()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<FlowCc>()
                .eq(FlowCc::getId, id)
                .eq(FlowCc::getTenantId, requireTenant())
                .and(w -> w.eq(FlowCc::getSendUserId, userId)
                        .or(x -> x.eq(FlowCc::getCcUserId, userId)
                                .eq(FlowCc::getStatus, FlowCcStatus.ACTIVE.getCode()))));
    }

    private Long requireTenant() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TENANT_REQUIRED");
        }
        return tenantId;
    }

    private String requireUserId() {
        Long userId = SessionHelper.getUserId();
        if (userId == null) {
            throw new IllegalStateException("FLOW_USER_REQUIRED");
        }
        return String.valueOf(userId);
    }

    /** 限制搜索词长度，避免超长模糊查询放大数据库压力。 */
    private String normalizeSearchText(String title) {
        if (title == null) {
            return null;
        }
        String normalized = title.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > 100 ? normalized.substring(0, 100) : normalized;
    }
}
