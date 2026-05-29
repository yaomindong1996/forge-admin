package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectRelationDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectDesignerService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectRelationService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessRelationRuntimeService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessRelationRuntimeVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务应用平台对象关系接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/object/{objectId}/relations")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessObjectRelationController {

    private final BusinessObjectRelationService relationService;
    private final BusinessRelationRuntimeService relationRuntimeService;
    private final BusinessObjectDesignerService designerService;

    @GetMapping
    @SaCheckPermission("ai:businessObject:relation")
    @OperationLog(module = "业务对象关系", type = OperationType.QUERY, desc = "查询业务对象关系")
    public RespInfo<List<BusinessObjectRelationVO>> list(@PathVariable Long objectId) {
        return RespInfo.success(relationService.listByObject(objectId));
    }

    @PostMapping
    @SaCheckPermission("ai:businessObject:relation")
    @OperationLog(module = "业务对象关系", type = OperationType.UPDATE, desc = "保存业务对象关系")
    public RespInfo<Void> saveRelations(@PathVariable Long objectId,
                                        @RequestBody List<BusinessObjectRelationDTO> relations) {
        relationService.saveRelations(objectId, relations);
        designerService.syncModelRelations(objectId);
        return RespInfo.success();
    }

    @DeleteMapping("/{relationId}")
    @SaCheckPermission("ai:businessObject:relation")
    @OperationLog(module = "业务对象关系", type = OperationType.DELETE, desc = "删除业务对象关系")
    public RespInfo<Void> deleteRelation(@PathVariable Long objectId, @PathVariable Long relationId) {
        relationService.deleteRelation(objectId, relationId);
        designerService.syncModelRelations(objectId);
        return RespInfo.success();
    }

    @GetMapping("/runtime")
    @SaCheckPermission("ai:businessRelation:runtime")
    @OperationLog(module = "业务对象关系", type = OperationType.QUERY, desc = "查询对象关系运行入口")
    public RespInfo<List<BusinessRelationRuntimeVO>> relationRuntime(@PathVariable Long objectId) {
        return RespInfo.success(relationRuntimeService.relationRuntime(objectId));
    }
}
