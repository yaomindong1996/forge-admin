package com.mdframe.forge.plugin.generator.vo.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码 DDL 预览结果。
 */
@Data
public class LowcodeDdlPreviewVO {

    private String tableName;

    private Boolean tableExists;

    private Boolean executable;

    private List<String> ddlStatements = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();

    /**
     * 可在线自动执行的语句数：CREATE/ADD，以及兼容扩容（同类型加长、放宽可空）。
     */
    private Integer onlineSafeDdlCount;

    /**
     * 需导出人工审核的语句数：改类型族、缩短长度、删列等。
     */
    private Integer manualReviewDdlCount;
}
