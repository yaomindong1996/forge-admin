package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApprovalRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
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

    private final BusinessFlowService flowService;
    private final BusinessDocumentRuntimeService documentRuntimeService;

    /**
     * 查询审批运行状态
     *
     * @param targetCode 目标对象编码
     * @param recordId   记录 ID
     * @return 审批运行状态
     */
    public BusinessApprovalRuntimeVO getApprovalRuntime(String targetCode, Long recordId) {
        BusinessApprovalRuntimeVO vo = new BusinessApprovalRuntimeVO();
        vo.setTargetCode(targetCode);
        vo.setRecordId(recordId);

        BusinessDocumentRuntimeVO runtime = documentRuntimeService.getRuntime(targetCode, recordId);
        BusinessFlowBindingVO binding = flowService.getFlowBinding(targetCode);
        if (!Boolean.TRUE.equals(runtime.getDocumentEnabled())) {
            boolean hasFlow = binding != null
                    && StringUtils.isNotBlank(binding.getFlowModelKey())
                    && !Integer.valueOf(0).equals(binding.getStatus());
            vo.setHasFlow(hasFlow);
            vo.setCanStart(hasFlow && recordId != null);
            if (binding != null) {
                vo.setFlowDefinitionId(binding.getFlowModelKey());
                vo.setFlowDefinitionName(StringUtils.defaultIfBlank(binding.getFlowModelName(), binding.getFlowModelKey()));
            }
            vo.setApprovalStatus("NONE");
            vo.setApprovalStatusLabel(hasFlow ? "可发起" : "未配置");
            if (hasFlow && recordId == null) {
                vo.setMessage("请先保存记录后再发起主流程");
                vo.setNextAction("SAVE_RECORD");
                vo.setNextActionLabel("保存记录");
            } else if (hasFlow) {
                vo.setMessage("已配置主流程，当前对象未启用完整单据状态能力");
                vo.setNextAction("START_FLOW");
                vo.setNextActionLabel("发起主流程");
            } else {
                vo.setMessage(StringUtils.defaultIfBlank(runtime.getMessage(), "请先在流程与自动化中配置主流程"));
                vo.setNextAction("CONFIGURE_FLOW");
                vo.setNextActionLabel("配置流程");
            }
            return vo;
        }

        vo.setHasFlow((binding != null && StringUtils.isNotBlank(binding.getFlowModelKey()))
                || !"CONFIG_FLOW".equals(runtime.getNextAction()));
        if (binding != null) {
            vo.setFlowDefinitionId(binding.getFlowModelKey());
            vo.setFlowDefinitionName(StringUtils.defaultIfBlank(binding.getFlowModelName(), binding.getFlowModelKey()));
        }
        if (StringUtils.isNotBlank(runtime.getProcessInstanceId())) {
            vo.setCanStart(false);
            vo.setApprovalStatus(toApprovalStatus(runtime));
            vo.setApprovalStatusLabel(toApprovalStatusLabel(vo.getApprovalStatus()));
            vo.setMessage(runtime.getMessage());
            vo.setNextAction("VIEW_FLOW");
            vo.setNextActionLabel("查看流程");
            return vo;
        }

        boolean canStart = runtime.getAvailableActions() != null && runtime.getAvailableActions().contains("START_FLOW");
        vo.setCanStart(canStart);
        vo.setApprovalStatus("NONE");
        vo.setApprovalStatusLabel(canStart ? "可发起" : "不可发起");
        vo.setMessage(runtime.getMessage());
        vo.setNextAction(canStart ? "START_FLOW" : runtime.getNextAction());
        vo.setNextActionLabel(canStart ? "发起主流程" : "配置流程");

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
        BusinessFlowStartDTO dto = new BusinessFlowStartDTO();
        dto.setObjectCode(targetCode);
        dto.setRecordId(recordId);
        BusinessFlowRuntimeVO runtime = flowService.startDocumentFlowForCompatibility(dto);
        log.info("审批兼容入口已转发到流程服务: targetCode={}, recordId={}, processInstanceId={}",
                targetCode, recordId, runtime.getProcessInstanceId());
        return runtime.getLinkId();
    }

    private String toApprovalStatus(BusinessDocumentRuntimeVO runtime) {
        if ("APPROVED".equalsIgnoreCase(runtime.getFlowStatus()) || "APPROVED".equalsIgnoreCase(runtime.getDocumentStatus())) {
            return "APPROVED";
        }
        if ("REJECTED".equalsIgnoreCase(runtime.getFlowStatus()) || "REJECTED".equalsIgnoreCase(runtime.getDocumentStatus())) {
            return "REJECTED";
        }
        if ("CANCELED".equalsIgnoreCase(runtime.getFlowStatus()) || "CANCELED".equalsIgnoreCase(runtime.getDocumentStatus())) {
            return "CANCELED";
        }
        return "PENDING";
    }

    private String toApprovalStatusLabel(String status) {
        return switch (status) {
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            case "CANCELED" -> "已撤回";
            case "PENDING" -> "审批中";
            default -> "未发起";
        };
    }
}
