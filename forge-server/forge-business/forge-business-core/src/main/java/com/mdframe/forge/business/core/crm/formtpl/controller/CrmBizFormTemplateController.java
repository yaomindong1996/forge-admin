package com.mdframe.forge.business.core.crm.formtpl.controller;

import com.mdframe.forge.business.core.crm.formtpl.BizFormObjectTypes;
import com.mdframe.forge.business.core.crm.formtpl.dto.CrmBizFormTemplateSaveDTO;
import com.mdframe.forge.business.core.crm.formtpl.dto.SysBizFormExtSaveDTO;
import com.mdframe.forge.business.core.crm.formtpl.service.CrmBizFormTemplateService;
import com.mdframe.forge.business.core.crm.formtpl.service.SysBizFormExtDataService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/biz-form")
@RequiredArgsConstructor
@ApiEncrypt
@ApiDecrypt
public class CrmBizFormTemplateController {
    private final CrmBizFormTemplateService service;
    private final SysBizFormExtDataService extDataService;

    /** 录入页运行时方案：打开新建/编辑时拉取 */
    @GetMapping("/scheme")
    public RespInfo<?> scheme(@RequestParam String objectType) {
        BizFormObjectTypes.checkSchemePermission(objectType);
        return RespInfo.success(service.scheme(objectType));
    }

    /** 配置弹窗：默认模版详情（含未启用字段） */
    @GetMapping("/template/{objectType}/default")
    public RespInfo<?> detailDefault(@PathVariable String objectType) {
        BizFormObjectTypes.checkEditPermission(objectType);
        return RespInfo.success(service.detailDefault(objectType));
    }

    /** 保存默认模版字段设置与自定义字段 */
    @PutMapping("/template/default")
    public RespInfo<?> saveDefault(@Valid @RequestBody CrmBizFormTemplateSaveDTO dto) {
        BizFormObjectTypes.checkEditPermission(dto.getObjectType());
        return RespInfo.success(service.saveDefault(dto));
    }

    /** 侧表扩展值：仅 SIDE_TABLE 对象（平台 /system） */
    @GetMapping("/ext")
    public RespInfo<?> getExt(@RequestParam String objectType, @RequestParam Long bizId) {
        BizFormObjectTypes.checkSchemePermission(objectType);
        return RespInfo.success(extDataService.get(objectType, bizId));
    }

    @PutMapping("/ext")
    public RespInfo<?> saveExt(@Valid @RequestBody SysBizFormExtSaveDTO dto) {
        BizFormObjectTypes.checkSchemePermission(dto.getObjectType());
        return RespInfo.success(extDataService.save(dto));
    }
}
