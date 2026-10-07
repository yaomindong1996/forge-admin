package com.mdframe.forge.plugin.system.vo;

import com.mdframe.forge.plugin.system.dto.PluginBuildResultDTO;

import java.time.LocalDateTime;

/** 无凭证/租约摘要/私有文件路径，结果仅为执行器报告，不是服务端复验或部署证明。 */
public record PluginBuildExecutionVO(String workerId, String phase, String sourceCommit, String image,
                                      LocalDateTime startedTime, LocalDateTime leaseExpiresTime,
                                      LocalDateTime deadlineTime, LocalDateTime finishedTime,
                                      boolean leaseExpired, PluginBuildResultDTO result, String resultSha256) {
}
