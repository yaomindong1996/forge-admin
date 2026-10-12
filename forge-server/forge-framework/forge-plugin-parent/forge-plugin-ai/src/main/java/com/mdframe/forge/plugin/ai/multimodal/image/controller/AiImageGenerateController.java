package com.mdframe.forge.plugin.ai.multimodal.image.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.multimodal.image.AiImageGenerationService;
import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import com.mdframe.forge.plugin.ai.multimodal.image.dto.AiImageGenerateDTO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

/**
 * AI图片生成控制器
 */
@RestController
@RequestMapping("/ai/image-generate")
@RequiredArgsConstructor
public class AiImageGenerateController {

    private final AiImageGenerationService imageGenerationService;

    /**
     * 发起图片生成
     */
    @SaCheckPermission("ai:image:generate")
    @PostMapping
    public RespInfo<Long> generate(@Validated @RequestBody AiImageGenerateDTO request) {
        Long recordId = imageGenerationService.generate(request.toRecord());
        return RespInfo.success(recordId);
    }

    /**
     * 分页查询生成记录
     */
    @SaCheckPermission("ai:image:page")
    @GetMapping("/page")
    public RespInfo<Page<AiImageGenerateRecord>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status) {
        return RespInfo.success(imageGenerationService.page(pageNum, pageSize, userId, status));
    }

    /**
     * 获取生成结果
     */
    @SaCheckPermission("ai:image:generate")
    @GetMapping("/{id}")
    public RespInfo<AiImageGenerateRecord> getResult(@PathVariable Long id) {
        return RespInfo.success(imageGenerationService.getResult(id));
    }
}
