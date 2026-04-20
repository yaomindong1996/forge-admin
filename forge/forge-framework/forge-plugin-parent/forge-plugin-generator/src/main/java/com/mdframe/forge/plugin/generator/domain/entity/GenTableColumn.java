package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代码生成表字段配置实体类
 */
@Data
@TableName("gen_table_column")
public class GenTableColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "column_id", type = IdType.AUTO)
    private Long columnId;

    private Long tableId;
    
    private String columnName;
    
    private String columnComment;
    
    private String columnType;
    
    private String javaType;
    
    private String javaField;
    
    private Integer isPk;
    
    private Integer isIncrement;
    
    private Integer isRequired;
    
    private Integer isInsert;
    
    private Integer isEdit;
    
    private Integer isList;
    
    private Integer isQuery;
    
    private String queryType;
    
    private String htmlType;
    
    private String dictType;
    
    private String validateRule;
    
    private Integer aiRecommended;
    
    private Integer sort;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
