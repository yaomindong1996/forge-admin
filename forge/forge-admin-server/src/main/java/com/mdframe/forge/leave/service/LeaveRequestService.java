package com.mdframe.forge.leave.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.leave.dto.LeaveRequestDTO;
import com.mdframe.forge.leave.entity.LeaveRequest;

/**
 * 请假申请服务接口
 *
 * @author forge
 */
public interface LeaveRequestService extends IService<LeaveRequest> {

    /**
     * 提交请假申请（启动流程）
     *
     * @param dto 请假申请数据
     * @return 业务Key
     */
    String submitLeave(LeaveRequestDTO dto);

    /**
     * 保存草稿
     *
     * @param dto 请假申请数据
     * @return 业务Key
     */
    String saveDraft(LeaveRequestDTO dto);

    /**
     * 根据businessKey获取请假详情
     *
     * @param businessKey 业务Key
     * @return 请假申请详情
     */
    LeaveRequest getByBusinessKey(String businessKey);

    /**
     * 撤销申请
     *
     * @param businessKey 业务Key
     */
    void cancelLeave(String businessKey);

    /**
     * 分页查询当前用户的请假列表
     *
     * @param page 分页参数
     * @param status 状态（可选）
     * @return 分页结果
     */
    Page<LeaveRequest> pageMyLeave(Page<LeaveRequest> page, String status);

    /**
     * 更新草稿
     *
     * @param dto 请假申请数据
     * @return 业务Key
     */
    String updateDraft(LeaveRequestDTO dto);
}
