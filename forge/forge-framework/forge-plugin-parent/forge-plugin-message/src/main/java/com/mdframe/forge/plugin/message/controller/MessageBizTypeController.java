package com.mdframe.forge.plugin.message.controller;

import com.mdframe.forge.plugin.message.domain.entity.SysMessageBizType;
import com.mdframe.forge.plugin.message.service.MessageBizTypeService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/message/bizType")
public class MessageBizTypeController {
    
    private final MessageBizTypeService messageBizTypeService;
    
    public MessageBizTypeController(MessageBizTypeService messageBizTypeService) {
        this.messageBizTypeService = messageBizTypeService;
    }
    
    /**
     * 分页查询
     */
    @GetMapping("/page")
    public RespInfo<?> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizName,
            @RequestParam(required = false) Integer enabled) {
        return RespInfo.success(messageBizTypeService.lambdaQuery()
            .like(bizType != null, SysMessageBizType::getBizType, bizType)
            .like(bizName != null, SysMessageBizType::getBizName, bizName)
            .eq(enabled != null, SysMessageBizType::getEnabled, enabled)
            .orderByAsc(SysMessageBizType::getSort)
            .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize)));
    }
    
    /**
     * 查询所有启用的业务类型（下拉选择用）
     */
    @GetMapping("/list/enabled")
    public RespInfo<?> listEnabled() {
        List<SysMessageBizType> list = messageBizTypeService.listEnabled();
        return RespInfo.success(list);
    }
    
    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public RespInfo<?> getById(@PathVariable Long id) {
        return RespInfo.success(messageBizTypeService.getById(id));
    }
    
    /**
     * 新增
     */
    @PostMapping
    public RespInfo<?> save(@RequestBody SysMessageBizType entity) {
        messageBizTypeService.save(entity);
        return RespInfo.success();
    }
    
    /**
     * 修改
     */
    @PutMapping
    public RespInfo<?> update(@RequestBody SysMessageBizType entity) {
        messageBizTypeService.updateById(entity);
        return RespInfo.success();
    }
    
    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public RespInfo<?> delete(@PathVariable Long id) {
        messageBizTypeService.removeById(id);
        return RespInfo.success();
    }
}