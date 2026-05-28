package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 能力挂接视图。
 */
@Data
public class BusinessBindingVO {

    private Long id;

    private String targetType;

    private Long targetId;

    private String targetCode;

    private String bindingType;

    private String bindingKey;

    private String bindingName;

    private String bindingConfig;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private Boolean canOpen;

    private String openType;

    private String entryUrl;

    private String actionLabel;

    private String statusMessage;

    private String nextAction;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
