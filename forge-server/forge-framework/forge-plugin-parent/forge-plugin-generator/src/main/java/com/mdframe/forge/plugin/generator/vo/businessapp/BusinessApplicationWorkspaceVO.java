package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用工作台首屏聚合视图。
 */
@Data
public class BusinessApplicationWorkspaceVO {

    private BusinessApplicationVO application;

    private BusinessApplicationReadinessVO readiness;

    /**
     * 工作台首屏资产快照。分区优先复用该快照，避免每次切换都重复查询。
     */
    private List<BusinessApplicationObjectVO> objects = new ArrayList<>();

    private List<BusinessAppVO> entries = new ArrayList<>();

    private List<BusinessExtensionVO> extensions = new ArrayList<>();

    private List<BusinessApplicationWorkspaceSectionVO> sections = new ArrayList<>();

    private List<BusinessApplicationReadinessIssueVO> issues = new ArrayList<>();

    private Long blockingCount;

    private Long warningCount;

    private LocalDateTime latestChangeTime;
}
