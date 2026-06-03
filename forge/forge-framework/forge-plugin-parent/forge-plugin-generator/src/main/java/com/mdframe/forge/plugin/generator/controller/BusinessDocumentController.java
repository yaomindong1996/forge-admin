package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentNoRulePreviewDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentRuntimeService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentNoRulePreviewVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentNoRuleTokenVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentRuntimeVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业务单据配置和运行态接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/document")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessDocumentController {

    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessDocumentRuntimeService documentRuntimeService;

    @GetMapping("/no-rule/tokens")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务单据", type = OperationType.QUERY, desc = "查询编号规则变量")
    public RespInfo<List<BusinessDocumentNoRuleTokenVO>> noRuleTokens() {
        return RespInfo.success(documentConfigService.listNoRuleTokens());
    }

    @PostMapping("/no-rule/preview")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务单据", type = OperationType.QUERY, desc = "预览单据编号规则")
    public RespInfo<BusinessDocumentNoRulePreviewVO> previewNoRule(@RequestBody BusinessDocumentNoRulePreviewDTO dto) {
        return RespInfo.success(documentConfigService.previewNoRule(dto));
    }

    @GetMapping("/config/{objectId}")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务单据", type = OperationType.QUERY, desc = "查询单据配置")
    public RespInfo<BusinessDocumentConfigVO> getConfig(@PathVariable Long objectId) {
        return RespInfo.success(documentConfigService.getConfig(objectId));
    }

    @PutMapping("/config/{objectId}")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务单据", type = OperationType.UPDATE, desc = "保存单据配置")
    public RespInfo<Void> saveConfig(@PathVariable Long objectId,
                                     @RequestBody BusinessDocumentConfigDTO dto) {
        documentConfigService.saveConfig(objectId, dto);
        return RespInfo.success();
    }

    @GetMapping("/{objectCode}/{recordId}/runtime")
    @SaCheckPermission("ai:businessDocument:view")
    @OperationLog(module = "业务单据", type = OperationType.QUERY, desc = "查询单据运行态")
    public RespInfo<BusinessDocumentRuntimeVO> runtime(@PathVariable String objectCode,
                                                       @PathVariable Long recordId) {
        return RespInfo.success(documentRuntimeService.getRuntime(objectCode, recordId));
    }
}
