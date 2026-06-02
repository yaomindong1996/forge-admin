package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 业务触发器场景模板。
 */
@Data
public class BusinessTriggerScenarioTemplateVO {

    private String scenarioType;

    private String scenarioName;

    private String description;

    private String eventType;

    private String actionType;

    private String receiverRule;
}
