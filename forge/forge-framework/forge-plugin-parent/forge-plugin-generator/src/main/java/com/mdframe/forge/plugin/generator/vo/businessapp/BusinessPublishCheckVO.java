package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务对象发布检查结果。
 */
@Data
public class BusinessPublishCheckVO {

    /** PASS/WARN/BLOCK */
    private String overallStatus;

    private Boolean publishable;

    private Integer passCount;

    private Integer warnCount;

    private Integer blockCount;

    private List<BusinessPublishCheckItemVO> items = new ArrayList<>();

    private List<BusinessPublishCheckItemVO> passItems = new ArrayList<>();

    private List<BusinessPublishCheckItemVO> warnItems = new ArrayList<>();

    private List<BusinessPublishCheckItemVO> blockItems = new ArrayList<>();
}
