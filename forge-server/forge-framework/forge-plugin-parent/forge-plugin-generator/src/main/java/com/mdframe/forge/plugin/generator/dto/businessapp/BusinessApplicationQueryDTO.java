package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.List;

/**
 * 业务应用查询参数。
 */
@Data
public class BusinessApplicationQueryDTO {

    private String keyword;

    private String applicationCode;

    private String suiteCode;

    private List<String> suiteCodes;

    private Integer status;

    private String designStatus;
}
