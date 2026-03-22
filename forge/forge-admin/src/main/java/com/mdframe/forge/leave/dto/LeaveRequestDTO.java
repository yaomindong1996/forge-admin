package com.mdframe.forge.leave.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假申请数据传输对象
 *
 * @author forge
 */
@Data
public class LeaveRequestDTO {

    /**
     * 主键ID（更新时使用）
     */
    private String id;

    /**
     * 业务Key
     */
    private String businessKey;

    // ========== 申请人信息 ==========

    /**
     * 申请人ID
     */
    private String applyUserId;

    /**
     * 申请人姓名
     */
    private String applyUserName;

    /**
     * 申请部门ID
     */
    private String applyDeptId;

    /**
     * 申请部门名称
     */
    private String applyDeptName;

    // ========== 请假信息 ==========

    /**
     * 请假类型
     */
    private String leaveType;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 请假天数
     */
    private BigDecimal duration;

    /**
     * 请假原因
     */
    private String reason;

    /**
     * 附件JSON列表
     */
    private String attachments;

    // ========== 审批信息 ==========

    /**
     * 审批意见
     */
    private String approveComment;

    /**
     * 审批附件JSON
     */
    private String approveAttachments;
}
