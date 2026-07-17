package com.mdframe.forge.starter.id.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.id.service.ISequenceService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 序列生成HTTP接口
 */
@RestController
@RequestMapping("/sequence")
@RequiredArgsConstructor
@Validated
@SaCheckPermission("system:sequence:use")
@ConditionalOnProperty(
        prefix = "forge.id.sequence",
        name = "enable-api",
        havingValue = "true",
        matchIfMissing = false)
public class SequenceController {

    private static final String BIZ_KEY_PATTERN = "^[A-Za-z0-9][A-Za-z0-9:_./-]{0,99}$";

    private final ISequenceService sequenceService;

    /**
     * 获取下一个序列ID（纯数字）
     *
     * @param bizKey 业务键
     * @return 序列ID
     */
    @PostMapping("/next")
    public RespInfo<Long> next(
            @RequestParam
            @Size(max = 100, message = "业务序列键长度不能超过100")
            @Pattern(regexp = BIZ_KEY_PATTERN, message = "业务序列键包含不允许的字符") String bizKey) {
        return RespInfo.success(sequenceService.nextId(bizKey));
    }

    /**
     * 批量获取序列ID
     * 
     * @param bizKey 业务键
     * @param size 批量大小
     * @return ID数组
     */
    @PostMapping("/nextBatch")
    public RespInfo<long[]> nextBatch(
            @RequestParam
            @Size(max = 100, message = "业务序列键长度不能超过100")
            @Pattern(regexp = BIZ_KEY_PATTERN, message = "业务序列键包含不允许的字符") String bizKey,
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
    @PostMapping("/nextFormatted")
    public RespInfo<String> nextFormatted(
            @RequestParam
            @Size(max = 100, message = "业务序列键长度不能超过100")
            @Pattern(regexp = BIZ_KEY_PATTERN, message = "业务序列键包含不允许的字符") String bizKey) {
        return RespInfo.success(sequenceService.nextFormatted(bizKey));
    }

    /**
     * 批量获取格式化序列号
     * 
     * @param bizKey 业务键
     * @param size 批量大小
     * @return 格式化序列号数组
     */
    @PostMapping("/nextFormattedBatch")
    public RespInfo<String[]> nextFormattedBatch(
            @RequestParam
            @Size(max = 100, message = "业务序列键长度不能超过100")
            @Pattern(regexp = BIZ_KEY_PATTERN, message = "业务序列键包含不允许的字符") String bizKey,
            @RequestParam(defaultValue = "10") int size) {
        if (size <= 0 || size > 1000) {
            return RespInfo.error("批量大小必须在1-1000之间");
        }
        return RespInfo.success(sequenceService.nextFormattedBatch(bizKey, size));
    }
}
