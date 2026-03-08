package com.mdframe.forge.starter.id.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.id.service.ISequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * 序列生成HTTP接口
 */
@RestController
@RequestMapping("/sequence")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.id.sequence", name = "enable-api", havingValue = "true", matchIfMissing = true)
public class SequenceController {
    
    private final ISequenceService sequenceService;
    
    /**
     * 获取下一个序列ID（纯数字）
     * 
     * @param bizKey 业务键
     * @return 序列ID
     */
    @GetMapping("/next")
    public RespInfo<Long> next(@RequestParam String bizKey) {
        return RespInfo.success(sequenceService.nextId(bizKey));
    }
    
    /**
     * 批量获取序列ID
     * 
     * @param bizKey 业务键
     * @param size 批量大小
     * @return ID数组
     */
    @GetMapping("/nextBatch")
    public RespInfo<long[]> nextBatch(@RequestParam String bizKey, 
                                      @RequestParam(defaultValue = "10") int size) {
        if (size <= 0 || size > 1000) {
            return RespInfo.error("批量大小必须在1-1000之间");
        }
        return RespInfo.success(sequenceService.nextBatch(bizKey, size));
    }
    
    /**
     * 获取格式化序列号
     * 
     * @param bizKey 业务键
     * @return 格式化序列号
     */
    @GetMapping("/nextFormatted")
    public RespInfo<String> nextFormatted(@RequestParam String bizKey) {
        return RespInfo.success(sequenceService.nextFormatted(bizKey));
    }
    
    /**
     * 批量获取格式化序列号
     * 
     * @param bizKey 业务键
     * @param size 批量大小
     * @return 格式化序列号数组
     */
    @GetMapping("/nextFormattedBatch")
    public RespInfo<String[]> nextFormattedBatch(@RequestParam String bizKey,
                                                 @RequestParam(defaultValue = "10") int size) {
        if (size <= 0 || size > 1000) {
            return RespInfo.error("批量大小必须在1-1000之间");
        }
        return RespInfo.success(sequenceService.nextFormattedBatch(bizKey, size));
    }
}
