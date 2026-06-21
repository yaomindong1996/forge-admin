package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 低代码数据模型协议。
 */
@Data
public class LowcodeModelSchema {

    /** 协议版本：旧版为空，新版为 2 */
    private Integer schemaVersion;

    private LowcodeDomainRef domain;

    private LowcodeObjectSchema object;

    /** SINGLE-单表，TREE-树形单表，MASTER_DETAIL-主子表协议预留 */
    private String appType;

    /** EXISTING-绑定已有表，CREATE-在线创建新表 */
    private String tableMode;

    /** 绑定已有表时的代码生成表模型引用。 */
    private LowcodeSourceTableRef sourceTable;

    private String tableName;

    private String businessName;

    private LowcodeTreeConfig treeConfig;

    private List<LowcodeFieldSchema> fields = new ArrayList<>();

    /** 领域内业务对象关系协议，当前运行时暂不改变单表查询行为 */
    private List<LowcodeRelationSchema> relations = new ArrayList<>();

    /** 用户自定义索引协议，支持单字段和联合索引。 */
    private List<LowcodeIndexSchema> indexes = new ArrayList<>();

    /** 用户自定义唯一约束，运行态动态 CRUD 会强制校验。 */
    private List<LowcodeUniqueConstraintSchema> uniqueConstraints = new ArrayList<>();

    /** 通用校验规则扩展，兼容 type=UNIQUE 的规则写法。 */
    private List<Map<String, Object>> validationRules = new ArrayList<>();

    private LowcodePolicySchema policies = new LowcodePolicySchema();

    /** 主子表协议预留，当前运行时暂不启用 */
    private List<LowcodeModelSchema> children = new ArrayList<>();
}
