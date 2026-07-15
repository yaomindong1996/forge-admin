package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用模板初始化参数。
 */
@Data
public class BusinessApplicationTemplateInitializeDTO {

    /** SINGLE_CRUD、TREE_TABLE、MASTER_DETAIL。 */
    private String templateCode;

    private BusinessApplicationTemplateObjectSourceDTO primarySource;

    private BusinessApplicationTemplateObjectSourceDTO treeSource;

    private String primaryObjectName;

    private String primaryObjectCode;

    private String treeObjectName;

    private String treeObjectCode;

    private String treeLabelField;

    private String treeKeyField;

    private String treeParentField;

    private String primaryTreeField;

    private String primaryKeyField;

    private List<BusinessApplicationTemplateDetailDTO> details = new ArrayList<>();
}
