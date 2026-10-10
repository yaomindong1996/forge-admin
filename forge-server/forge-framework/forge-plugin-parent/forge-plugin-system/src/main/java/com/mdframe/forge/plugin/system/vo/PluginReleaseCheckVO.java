package com.mdframe.forge.plugin.system.vo;

/** 只证明核验时点的元数据绑定，不是签名、制品独立复验或部署授权。 */
public record PluginReleaseCheckVO(int protocolVersion, String checkId, String taskId, Integer revision,
                                   String reviewId, String workerId, String serverResultSha256,
                                   String manifestSha256, String checkedAt,
                                   boolean liveTaskApprovalVerified, boolean deployed) {
}
