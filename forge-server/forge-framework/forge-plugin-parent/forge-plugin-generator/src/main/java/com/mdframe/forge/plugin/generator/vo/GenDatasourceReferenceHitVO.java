package com.mdframe.forge.plugin.generator.vo;

import lombok.Data;

/**
 * 数据源引用统计（删除前校验用）。
 */
@Data
public class GenDatasourceReferenceHitVO {

    /** 引用来源名称，例如「低代码运行配置」 */
    private String refLabel;

    /** 引用条数 */
    private Long refCount;
}
