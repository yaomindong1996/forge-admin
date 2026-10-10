package com.mdframe.forge.plugin.system.vo;
import com.mdframe.forge.plugin.system.entity.SysPluginDelivery;
import java.time.LocalDateTime;
/** 永不返回leaseHash、机器配置或原始执行日志。 */
public record PluginDeliveryVO(String id, String targetId, String taskId, String releaseId,
                               String previousReleaseId, String action, String status,
                               Boolean cosVerified, Boolean runtimeVerified, String failureCode,
                               String backupReference, String note, LocalDateTime createTime,
                               String reconcileNote, Long reconciledBy, LocalDateTime reconciledTime) {
    public static PluginDeliveryVO of(SysPluginDelivery value) {
        return new PluginDeliveryVO(value.getId(), value.getTargetId(), value.getTaskId(), value.getReleaseId(),
                value.getPreviousReleaseId(), value.getAction(), value.getStatus(), value.getCosVerified(),
                value.getRuntimeVerified(), value.getFailureCode(), value.getBackupReference(),
                value.getNote(), value.getCreateTime(), value.getReconcileNote(),
                value.getReconciledBy(), value.getReconciledTime());
    }
}
