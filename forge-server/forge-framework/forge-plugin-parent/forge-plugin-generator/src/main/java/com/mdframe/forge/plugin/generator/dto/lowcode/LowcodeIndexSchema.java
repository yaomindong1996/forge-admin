package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码数据模型索引协议。
 */
@Data
public class LowcodeIndexSchema {

    private String indexName;

    /** NORMAL-普通索引，UNIQUE-唯一索引。 */
    private String indexType = "NORMAL";

    private List<String> fields = new ArrayList<>();

    private Boolean unique = false;

    /** 兼容旧版本自动索引标记；true 的历史配置不再参与 DDL 生成。 */
    private Boolean auto = false;

    private String remark;
}
