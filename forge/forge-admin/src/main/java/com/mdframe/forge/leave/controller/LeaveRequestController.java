package com.mdframe.forge.leave.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.leave.dto.LeaveRequestDTO;
import com.mdframe.forge.leave.entity.LeaveRequest;
import com.mdframe.forge.leave.service.LeaveRequestService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 请假申请控制器
 *
 * @author forge
 */
@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    /**
     * 提交请假申请
     */
    @PostMapping("/submit")
    public RespInfo<String> submit(@RequestBody LeaveRequestDTO dto) {
        String businessKey = leaveRequestService.submitLeave(dto);
        return RespInfo.success("提交成功", businessKey);
    }

    /**
     * 保存草稿
     */
    @PostMapping("/draft")
    public RespInfo<String> saveDraft(@RequestBody LeaveRequestDTO dto) {
        String businessKey = leaveRequestService.saveDraft(dto);
        return RespInfo.success("保存成功", businessKey);
    }

    /**
     * 更新草稿
     */
    @PutMapping("/draft")
    public RespInfo<String> updateDraft(@RequestBody LeaveRequestDTO dto) {
        String businessKey = leaveRequestService.updateDraft(dto);
        return RespInfo.success("更新成功", businessKey);
    }

    /**
     * 获取请假详情
     */
    @GetMapping("/detail/{businessKey}")
    public RespInfo<LeaveRequest> detail(@PathVariable String businessKey) {
        LeaveRequest leave = leaveRequestService.getByBusinessKey(businessKey);
        return RespInfo.success(leave);
    }

    /**
     * 分页查询我的请假列表
     */
    @GetMapping("/page")
    public RespInfo<Page<LeaveRequest>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        Page<LeaveRequest> page = new Page<>(pageNum, pageSize);
        Page<LeaveRequest> result = leaveRequestService.pageMyLeave(page, status);
        return RespInfo.success(result);
    }

    /**
     * 撤销申请
     */
    @PostMapping("/cancel/{businessKey}")
    public RespInfo<Void> cancel(@PathVariable String businessKey) {
        leaveRequestService.cancelLeave(businessKey);
        return RespInfo.success("撤销成功", null);
    }

    /**
     * 删除草稿
     */
    @DeleteMapping("/{businessKey}")
    public RespInfo<Void> delete(@PathVariable String businessKey) {
        LeaveRequest leave = leaveRequestService.getByBusinessKey(businessKey);
        if (leave != null && "draft".equals(leave.getStatus())) {
            leaveRequestService.removeById(leave.getId());
            return RespInfo.success("删除成功", null);
        }
        return RespInfo.error("只能删除草稿状态的申请");
    }
}
