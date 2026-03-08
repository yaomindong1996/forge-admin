package com.mdframe.forge.starter.id.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_id_sequence")
public class SysIdSequence {
    
    @TableId
    private String bizKey;
    
    /**
     * 当前最大ID（已分配到的最大值）
     */
    private Long maxId;
    
    /**
     * 步长（每次分配的区间大小）
     */
    private Integer step;
    
    /**
     * 版本（乐观锁）
     */
    private Integer version;
    
    /**
     * 重置策略：NONE/DAILY/HOURLY
     */
    private String resetPolicy;
    
    /**
     * 序列长度（左侧补零）
     */
    private Integer seqLength;
    
    /**
     * 前缀（如业务码）
     */
    private String prefix;
}
