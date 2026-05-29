package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessLayoutDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectActionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignerDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectPublishDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFieldDesignService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessLayoutDesignService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectDesignVersionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectDesignerService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectPublishService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFieldVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessLayoutVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectDesignVersionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectDesignerVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessReadinessItemVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业务对象设计器接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/object")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessObjectDesignerController {

    private final BusinessObjectDesignerService designerService;
    private final BusinessFieldDesignService fieldDesignService;
    private final BusinessLayoutDesignService layoutDesignService;
    private final BusinessObjectActionService actionService;
    private final BusinessObjectPublishService publishService;
    private final BusinessObjectDesignVersionService designVersionService;

    @GetMapping("/{objectId}/designer")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象设计器", type = OperationType.QUERY, desc = "查询业务对象设计器")
    public RespInfo<BusinessObjectDesignerVO> getDesigner(@PathVariable Long objectId) {
        return RespInfo.success(designerService.getDesigner(objectId));
    }

    @PutMapping("/{objectId}/designer")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象设计器", type = OperationType.UPDATE, desc = "保存业务对象设计器")
    public RespInfo<Void> saveDesigner(@PathVariable Long objectId,
                                       @RequestBody BusinessObjectDesignerDTO dto) {
        designerService.saveDesigner(objectId, dto);
        return RespInfo.success();
    }

    @GetMapping("/{objectId}/fields")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象字段", type = OperationType.QUERY, desc = "查询业务字段")
    public RespInfo<List<BusinessFieldVO>> listFields(@PathVariable Long objectId) {
        return RespInfo.success(fieldDesignService.listFields(objectId));
    }

    @PostMapping("/{objectId}/fields")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象字段", type = OperationType.ADD, desc = "新增业务字段")
    public RespInfo<BusinessFieldVO> addField(@PathVariable Long objectId,
                                              @RequestBody BusinessFieldDTO dto) {
        return RespInfo.success(fieldDesignService.addField(objectId, dto));
    }

    @PutMapping("/{objectId}/fields/sort")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象字段", type = OperationType.UPDATE, desc = "业务字段排序")
    public RespInfo<List<BusinessFieldVO>> sortFields(@PathVariable Long objectId,
                                                      @RequestBody List<String> fieldCodes) {
        return RespInfo.success(fieldDesignService.sortFields(objectId, fieldCodes));
    }

    @PutMapping("/{objectId}/fields/{fieldCode}")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象字段", type = OperationType.UPDATE, desc = "修改业务字段")
    public RespInfo<BusinessFieldVO> updateField(@PathVariable Long objectId,
                                                 @PathVariable String fieldCode,
                                                 @RequestBody BusinessFieldDTO dto) {
        return RespInfo.success(fieldDesignService.updateField(objectId, fieldCode, dto));
    }

    @DeleteMapping("/{objectId}/fields/{fieldCode}")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象字段", type = OperationType.DELETE, desc = "删除业务字段")
    public RespInfo<Void> deleteField(@PathVariable Long objectId, @PathVariable String fieldCode) {
        fieldDesignService.deleteField(objectId, fieldCode);
        return RespInfo.success();
    }

    @GetMapping("/{objectId}/layout/{layoutKey}")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象布局", type = OperationType.QUERY, desc = "查询业务布局")
    public RespInfo<BusinessLayoutVO> getLayout(@PathVariable Long objectId,
                                                @PathVariable String layoutKey) {
        return RespInfo.success(layoutDesignService.getLayout(objectId, layoutKey));
    }

    @PutMapping("/{objectId}/layout/form")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象布局", type = OperationType.UPDATE, desc = "保存表单布局")
    public RespInfo<Void> saveFormLayout(@PathVariable Long objectId,
                                         @RequestBody BusinessLayoutDTO dto) {
        layoutDesignService.saveFormLayout(objectId, dto);
        return RespInfo.success();
    }

    @PutMapping("/{objectId}/layout/list")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象布局", type = OperationType.UPDATE, desc = "保存列表布局")
    public RespInfo<Void> saveListLayout(@PathVariable Long objectId,
                                         @RequestBody BusinessLayoutDTO dto) {
        layoutDesignService.saveListLayout(objectId, dto);
        return RespInfo.success();
    }

    @PutMapping("/{objectId}/layout/detail")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象布局", type = OperationType.UPDATE, desc = "保存详情布局")
    public RespInfo<Void> saveDetailLayout(@PathVariable Long objectId,
                                           @RequestBody BusinessLayoutDTO dto) {
        layoutDesignService.saveDetailLayout(objectId, dto);
        return RespInfo.success();
    }

    @PostMapping("/{objectId}/layout/preview")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象布局", type = OperationType.QUERY, desc = "预览业务布局")
    public RespInfo<AiCrudConfigRenderVO> previewLayout(@PathVariable Long objectId,
                                                        @RequestBody BusinessLayoutDTO dto) {
        return RespInfo.success(layoutDesignService.previewLayout(objectId, dto));
    }

    @GetMapping("/{objectId}/actions")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象操作", type = OperationType.QUERY, desc = "查询业务对象操作")
    public RespInfo<List<BusinessObjectActionVO>> listActions(@PathVariable Long objectId) {
        return RespInfo.success(actionService.listActions(objectId));
    }

    @PutMapping("/{objectId}/actions")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象操作", type = OperationType.UPDATE, desc = "保存业务对象操作")
    public RespInfo<Void> saveActions(@PathVariable Long objectId,
                                      @RequestBody List<BusinessObjectActionDTO> actions) {
        actionService.saveActions(objectId, actions);
        return RespInfo.success();
    }

    @GetMapping("/{objectId}/permission-summary")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象权限", type = OperationType.QUERY, desc = "查询业务对象权限摘要")
    public RespInfo<BusinessReadinessItemVO> permissionSummary(@PathVariable Long objectId) {
        return RespInfo.success(actionService.permissionSummary(objectId));
    }

    @GetMapping("/{objectId}/publish-check")
    @SaCheckPermission("ai:businessObject:publish")
    @OperationLog(module = "业务对象发布", type = OperationType.QUERY, desc = "业务对象发布检查")
    public RespInfo<BusinessPublishCheckVO> publishCheck(@PathVariable Long objectId) {
        return RespInfo.success(publishService.publishCheck(objectId));
    }

    @PostMapping("/{objectId}/publish")
    @SaCheckPermission("ai:businessObject:publish")
    @OperationLog(module = "业务对象发布", type = OperationType.UPDATE, desc = "发布业务对象")
    public RespInfo<Long> publish(@PathVariable Long objectId,
                                  @RequestBody(required = false) BusinessObjectPublishDTO dto) {
        return RespInfo.success(publishService.publish(objectId, dto));
    }

    @GetMapping("/{objectId}/versions")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象版本", type = OperationType.QUERY, desc = "查询业务对象设计版本")
    public RespInfo<List<BusinessObjectDesignVersionVO>> versions(@PathVariable Long objectId) {
        return RespInfo.success(designVersionService.listByObjectId(objectId));
    }

    @PostMapping("/{objectId}/versions/{versionId}/rollback")
    @SaCheckPermission("ai:businessObject:design")
    @OperationLog(module = "业务对象版本", type = OperationType.UPDATE, desc = "回滚业务对象设计版本")
    public RespInfo<Void> rollback(@PathVariable Long objectId, @PathVariable Long versionId) {
        publishService.rollback(objectId, versionId);
        return RespInfo.success();
    }
}
