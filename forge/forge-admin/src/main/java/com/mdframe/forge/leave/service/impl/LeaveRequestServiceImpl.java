package com.mdframe.forge.leave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.leave.dto.LeaveRequestDTO;
import com.mdframe.forge.leave.entity.LeaveRequest;
import com.mdframe.forge.leave.mapper.LeaveRequestMapper;
import com.mdframe.forge.leave.service.LeaveRequestService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 请假申请服务实现
 *
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest> implements LeaveRequestService {

    private final FlowInstanceService flowInstanceService;

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
        
        // 3. 构建流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("leaveType", dto.getLeaveType());
        variables.put("duration", dto.getDuration());
        variables.put("reason", dto.getReason());
        variables.put("startTime", dto.getStartTime());
        variables.put("endTime", dto.getEndTime());
        variables.put("initiator", leave.getApplyUserId());
        
        // 获取发起人直属领导
        String initiatorLeader = getInitiatorLeader(leave.getApplyUserId());
        variables.put("initiatorLeader", initiatorLeader);
        
        // 4. 构建流程标题
        String title = buildLeaveTitle(dto);
        
        // 5. 启动流程
        String processInstanceId = flowInstanceService.startProcess(
            PROCESS_KEY,
            businessKey,
            BUSINESS_TYPE,
            title,
            variables,
            leave.getApplyUserId(),
            leave.getApplyUserName(),
            leave.getApplyDeptId(),
            leave.getApplyDeptName()
        );
        
        // 6. 更新流程实例ID
        leave.setProcessInstanceId(processInstanceId);
        updateById(leave);
        
        log.info("请假申请提交成功，businessKey: {}, processInstanceId: {}", businessKey, processInstanceId);
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
    public void updateApproveInfo(String businessKey, String approveUserId, String approveUserName,
                                  String comment, String attachments, boolean approved) {
        LeaveRequest leave = getByBusinessKey(businessKey);
        if (leave != null) {
            leave.setApproveUserId(approveUserId);
            leave.setApproveUserName(approveUserName);
            leave.setApproveTime(LocalDateTime.now());
            leave.setApproveComment(comment);
            leave.setApproveAttachments(attachments);
            leave.setStatus(approved ? "approved" : "rejected");
            leave.setUpdateTime(LocalDateTime.now());
            updateById(leave);
            
            log.info("请假审批信息更新成功，businessKey: {}, approved: {}", businessKey, approved);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeave(String businessKey) {
        LeaveRequest leave = getByBusinessKey(businessKey);
        if (leave != null && "pending".equals(leave.getStatus())) {
            // 终止流程
            flowInstanceService.terminateProcess(businessKey,
                String.valueOf(SessionHelper.getUserId()), "用户主动撤销");
            
            // 更新状态
            leave.setStatus("canceled");
            leave.setUpdateTime(LocalDateTime.now());
            updateById(leave);
            
            log.info("请假申请已撤销，businessKey: {}", businessKey);
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

    /**
     * 构建请假标题
     */
    private String buildLeaveTitle(LeaveRequestDTO dto) {
        String leaveTypeName = getLeaveTypeName(dto.getLeaveType());
        return leaveTypeName + "申请 - " + dto.getApplyUserName();
    }

    /**
     * 获取请假类型名称
     */
    private String getLeaveTypeName(String leaveType) {
        switch (leaveType) {
            case "annual":
                return "年假";
            case "sick":
                return "病假";
            case "personal":
                return "事假";
            case "marriage":
                return "婚假";
            case "maternity":
                return "产假";
            default:
                return "请假";
        }
    }

    /**
     * 获取发起人直属领导
     * TODO: 需要根据实际组织架构服务实现
     */
    private String getInitiatorLeader(String userId) {
        // 这里需要调用组织架构服务获取用户的直属领导
        // 暂时返回空，实际使用时需要实现
        return null;
    }
}
