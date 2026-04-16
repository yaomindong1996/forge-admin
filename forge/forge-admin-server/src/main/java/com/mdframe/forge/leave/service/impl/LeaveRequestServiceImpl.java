package com.mdframe.forge.leave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.flow.client.annotation.FlowStart;
import com.mdframe.forge.leave.dto.LeaveRequestDTO;
import com.mdframe.forge.leave.entity.LeaveRequest;
import com.mdframe.forge.leave.mapper.LeaveRequestMapper;
import com.mdframe.forge.leave.service.LeaveRequestService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 请假申请服务实现
 *
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FlowBind(modelKey = "leave_process", businessType = "leave")
public class LeaveRequestServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest> implements LeaveRequestService {

    private final FlowClient flowClient;

    /**
     * 流程定义Key
     */
    private static final String PROCESS_KEY = "leave_process";

    /**
     * 业务类型
     */
    private static final String BUSINESS_TYPE = "leave";

    @Override
    @Transactional(rollbackFor = Exception.class)
    @FlowStart(
        businessKeySpEl = "#result",
        titleSpEl       = "(#dto.applyUserName != null ? #dto.applyUserName : '申请人') + ' 的请假申请'",
        userIdSpEl      = "#dto.applyUserId",
        userNameSpEl    = "#dto.applyUserName",
        deptIdSpEl      = "#dto.applyDeptId",
        deptNameSpEl    = "#dto.applyDeptName"
    )
    public String submitLeave(LeaveRequestDTO dto) {
        // 1. 生成业务Key
        String businessKey = generateBusinessKey();

        // 2. 保存业务数据
        LeaveRequest leave = new LeaveRequest();
        BeanUtils.copyProperties(dto, leave);
        leave.setBusinessKey(businessKey);
        leave.setStatus("pending");
        leave.setCreateTime(LocalDateTime.now());

        // 设置申请人信息（如果未提供）
        if (leave.getApplyUserId() == null) {
            leave.setApplyUserId(String.valueOf(SessionHelper.getUserId()));
            leave.setApplyUserName(SessionHelper.getUsername());
        }

        save(leave);

        log.info("请假申请已保存，businessKey: {}，流程将由 @FlowStart 切面自动发起", businessKey);
        return businessKey;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveDraft(LeaveRequestDTO dto) {
        // 生成业务Key
        String businessKey = generateBusinessKey();
        
        // 保存业务数据
        LeaveRequest leave = new LeaveRequest();
        BeanUtils.copyProperties(dto, leave);
        leave.setBusinessKey(businessKey);
        leave.setStatus("draft");
        leave.setCreateTime(LocalDateTime.now());
        
        // 设置申请人信息
        if (leave.getApplyUserId() == null) {
            leave.setApplyUserId(String.valueOf(SessionHelper.getUserId()));
            leave.setApplyUserName(SessionHelper.getUsername());
        }
        
        save(leave);
        
        log.info("请假草稿保存成功，businessKey: {}", businessKey);
        return businessKey;
    }

    @Override
    public LeaveRequest getByBusinessKey(String businessKey) {
        return getOne(new LambdaQueryWrapper<LeaveRequest>()
            .eq(LeaveRequest::getBusinessKey, businessKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeave(String businessKey) {
        LeaveRequest leave = getByBusinessKey(businessKey);
        if (leave != null && "pending".equals(leave.getStatus())) {
            // 通过 FlowClient 终止流程
            FlowResult<Void> result = flowClient.terminateProcess(
                    businessKey,
                    String.valueOf(SessionHelper.getUserId()),
                    "用户主动撤销");
            if (!result.isSuccess()) {
                log.warn("终止流程失败，businessKey: {}, msg: {}", businessKey, result.getMsg());
            }

            // 更新状态
            leave.setStatus("canceled");
            leave.setUpdateTime(LocalDateTime.now());
            updateById(leave);

            log.info("请假申请已撤销，businessKey: {}", businessKey);
        }
    }

    // ==================== 流程事件回调（由 FlowEventSubscriber 通过 Redis 分发） ====================

    /**
     * 流程审批通过回调
     */
    @FlowCallback(on = FlowCallback.ON_COMPLETED)
    public void onLeaveApproved(FlowEventContext ctx) {
        LeaveRequest leave = getByBusinessKey(ctx.getBusinessKey());
        if (leave != null) {
            leave.setStatus("approved");
            leave.setApproveComment(ctx.getLastComment());
            leave.setApproveTime(LocalDateTime.now());
            leave.setUpdateTime(LocalDateTime.now());
            updateById(leave);
            log.info("请假审批通过，businessKey: {}", ctx.getBusinessKey());
        }
    }

    /**
     * 流程驳回回调
     */
    @FlowCallback(on = FlowCallback.ON_REJECTED)
    public void onLeaveRejected(FlowEventContext ctx) {
        LeaveRequest leave = getByBusinessKey(ctx.getBusinessKey());
        if (leave != null) {
            leave.setStatus("rejected");
            leave.setApproveComment(ctx.getLastComment());
            leave.setApproveTime(LocalDateTime.now());
            leave.setUpdateTime(LocalDateTime.now());
            updateById(leave);
            log.info("请假申请被驳回，businessKey: {}", ctx.getBusinessKey());
        }
    }

    /**
     * 流程撤回/取消回调
     */
    @FlowCallback(on = FlowCallback.ON_CANCELED)
    public void onLeaveCanceled(FlowEventContext ctx) {
        LeaveRequest leave = getByBusinessKey(ctx.getBusinessKey());
        if (leave != null && !"canceled".equals(leave.getStatus())) {
            leave.setStatus("canceled");
            leave.setUpdateTime(LocalDateTime.now());
            updateById(leave);
            log.info("请假申请已取消，businessKey: {}", ctx.getBusinessKey());
        }
    }

    @Override
    public Page<LeaveRequest> pageMyLeave(Page<LeaveRequest> page, String status) {
        String currentUserId = String.valueOf(SessionHelper.getUserId());
        
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaveRequest::getApplyUserId, currentUserId);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(LeaveRequest::getStatus, status);
        }
        
        wrapper.orderByDesc(LeaveRequest::getCreateTime);
        
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateDraft(LeaveRequestDTO dto) {
        LeaveRequest leave = getByBusinessKey(dto.getBusinessKey());
        if (leave != null && "draft".equals(leave.getStatus())) {
            BeanUtils.copyProperties(dto, leave, "id", "businessKey", "status", "createTime");
            leave.setUpdateTime(LocalDateTime.now());
            updateById(leave);
            
            log.info("请假草稿更新成功，businessKey: {}", dto.getBusinessKey());
            return dto.getBusinessKey();
        }
        return null;
    }

    /**
     * 生成业务Key
     */
    private String generateBusinessKey() {
        return "LEAVE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
