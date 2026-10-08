package com.mdframe.forge.plugin.system.service.plugin;

import lombok.Data;
import lombok.ToString;
import java.time.LocalDateTime;

/** 一条查询的内部投影；禁止把整个投影返回到机器或管理端。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class PluginReleaseApprovalSnapshot {
    private String taskId;
    private String pluginId;
    private String pluginVersion;
    private String operationType;
    private String taskStatus;
    private String activePluginId;
    private Integer revision;
    private String archiveSha256;
    private String previewJson;
    private String workerId;
    private String sourceCommit;
    private String image;
    private String buildPhase;
    private LocalDateTime finishedTime;
    private String resultJson;
    private String resultSha256;
    private String reviewId;
    private Integer expectedRevision;
    private String reviewPackageSha256;
    private String reviewResultSha256;
    private String decision;
    private String previousStatus;
    private String targetStatus;
    private Boolean executorStopped;
    private Boolean notDeployed;
    private Boolean artifactsReviewed;
    private Boolean migrationsReviewed;
}
