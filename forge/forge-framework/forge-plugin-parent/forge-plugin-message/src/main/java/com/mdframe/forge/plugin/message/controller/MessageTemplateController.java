package com.mdframe.forge.plugin.message.controller;

import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;
import com.mdframe.forge.plugin.message.service.MessageTemplateService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 消息模板管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/message/template")
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class MessageTemplateController {

    private final MessageTemplateService templateService;

    public MessageTemplateController(MessageTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * 创建模板
     */
    @PostMapping
    public RespInfo<?> create(@RequestBody SysMessageTemplate template) {
        templateService.create(template);
        return RespInfo.success();
    }

    /**
     * 更新模板
     */
    @PutMapping
    public RespInfo<?> update(@RequestBody SysMessageTemplate template) {
        templateService.update(template);
        return RespInfo.success();
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public RespInfo<?> delete(@PathVariable Long id) {
        templateService.delete(id);
        return RespInfo.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public RespInfo<?> getById(@PathVariable Long id) {
        return RespInfo.success(templateService.getById(id));
    }

    /**
     * 根据模板编码查询
     */
    @GetMapping("/code/{templateCode}")
    public RespInfo<?> getByCode(@PathVariable String templateCode) {
        return RespInfo.success(templateService.getByCode(templateCode));
    }

    /**
     * 分页查询模板列表
     */
    @GetMapping("/page")
    public RespInfo<?> page(@RequestParam(required = false) String type,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return RespInfo.success(templateService.page(type, keyword, pageNum, pageSize));
    }
}
