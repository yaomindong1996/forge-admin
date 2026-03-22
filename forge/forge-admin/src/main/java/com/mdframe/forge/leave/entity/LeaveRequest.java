package com.mdframe.forge.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假申请实体类
 *
 * @author forge
 */
@Data
@TableName("biz_leave_request")
public class LeaveRequest {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 业务Key（关联流程）
     */
    private String businessKey;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

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
     * annual-年假/sick-病假/personal-事假/marriage-婚假/maternity-产假
     */
    private String leaveType;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
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
     * 状态
     * draft-草稿/pending-审批中/approved-已通过/rejected-已驳回/canceled-已取消
     */
    private String status;

    /**
     * 审批人ID
     */
    private String approveUserId;

    /**
     * 审批人姓名
     */
    private String approveUserName;

    /**
     * 审批时间
     */
    private LocalDateTime approveTime;

    /**
     * 审批意见
     */
    private String approveComment;

    /**
     * 审批附件JSON
     */
    private String approveAttachments;

    // ========== 系统字段 ==========

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer deleted;
}
