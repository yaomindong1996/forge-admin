package com.mdframe.forge.plugin.system.vo;

import java.time.LocalDateTime;

/** 只展示审查结论；不返回幂等摘要、请求ID或包。 */
public record PluginTaskReviewVO(String id, String decision, String previousStatus, String targetStatus,
                                 Long reviewedBy, LocalDateTime reviewedTime, Boolean executorStopped,
                                 Boolean notDeployed, Boolean artifactsReviewed, Boolean migrationsReviewed,
                                 String note) {
}
