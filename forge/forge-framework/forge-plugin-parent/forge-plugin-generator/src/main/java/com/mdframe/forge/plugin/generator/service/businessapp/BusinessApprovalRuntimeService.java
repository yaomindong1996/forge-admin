package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApprovalRuntimeVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 审批运行服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessApprovalRuntimeService {

    private final BusinessBindingMapper bindingMapper;

    /**
     * 查询审批运行状态
     *
     * @param targetCode 目标对象编码
     * @param recordId   记录 ID
     * @return 审批运行状态
     */
    public BusinessApprovalRuntimeVO getApprovalRuntime(String targetCode, Long recordId) {
        Long tenantId = resolveTenantId();

        BusinessApprovalRuntimeVO vo = new BusinessApprovalRuntimeVO();
        vo.setTargetCode(targetCode);
        vo.setRecordId(recordId);

        // 查询审批能力挂接
        AiBusinessBinding approvalBinding = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", targetCode, "APPROVAL");

        if (approvalBinding == null) {
            vo.setHasFlow(false);
            vo.setCanStart(false);
            vo.setApprovalStatus("NONE");
            vo.setApprovalStatusLabel("未配置");
            vo.setMessage("审批能力未配置，请先配置审批流程");
            vo.setNextAction("CONFIGURE_APPROVAL");
            vo.setNextActionLabel("配置审批流程");
            return vo;
        }

        vo.setHasFlow(true);
        vo.setFlowDefinitionName(approvalBinding.getBindingName());

        // 检查是否有流程配置
        String flowKey = approvalBinding.getBindingKey();
        if (StringUtils.isBlank(flowKey)) {
            vo.setCanStart(false);
            vo.setApprovalStatus("NONE");
            vo.setApprovalStatusLabel("未配置流程");
            vo.setMessage("审批流程未配置，请先配置流程");
            vo.setNextAction("CONFIGURE_FLOW");
            vo.setNextActionLabel("配置流程");
            return vo;
        }

        // 检查是否有记录 ID
        if (recordId == null) {
            vo.setCanStart(false);
            vo.setApprovalStatus("NONE");
            vo.setApprovalStatusLabel("无记录");
            vo.setMessage("请先保存记录后再发起审批");
            return vo;
        }

        // 可以发起审批
        vo.setCanStart(true);
        vo.setApprovalStatus("NONE");
        vo.setApprovalStatusLabel("可发起");
        vo.setMessage("可以发起审批");
        vo.setNextAction("START_APPROVAL");
        vo.setNextActionLabel("发起审批");

        return vo;
    }

    /**
     * 发起审批
     *
     * @param targetCode 目标对象编码
     * @param recordId   记录 ID
     * @return 审批流程实例 ID
     */
    public Long startApproval(String targetCode, Long recordId) {
        Long tenantId = resolveTenantId();

        // 查询审批能力挂接
        AiBusinessBinding approvalBinding = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", targetCode, "APPROVAL");

        if (approvalBinding == null || StringUtils.isBlank(approvalBinding.getBindingKey())) {
            return null;
        }

        // 这里应该调用流程引擎服务发起审批
        // 暂时返回模拟的流程实例 ID
        // TODO: 集成 Flowable 流程引擎
        log.info("发起审批: targetCode={}, recordId={}, flowKey={}", targetCode, recordId, approvalBinding.getBindingKey());

        return recordId;
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
