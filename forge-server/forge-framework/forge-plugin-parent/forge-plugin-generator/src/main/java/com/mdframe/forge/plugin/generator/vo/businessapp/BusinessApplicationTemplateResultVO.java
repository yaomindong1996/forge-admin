package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用模板初始化结果。
 */
@Data
public class BusinessApplicationTemplateResultVO {

    private Long applicationId;

    private String templateCode;

    private Long primaryObjectId;

    private String primaryObjectCode;

    private List<BusinessApplicationObjectVO> objects = new ArrayList<>();
}
