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
    
    /** 脱敏类型（非DB字段，由 desensitizeConfig 回写），如 PHONE/ID_CARD/EMAIL 等 */
    @TableField(exist = false)
    private String desensitizeType;
    
    private String validateRule;
    
    private Integer aiRecommended;
    
    private Integer sort;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
