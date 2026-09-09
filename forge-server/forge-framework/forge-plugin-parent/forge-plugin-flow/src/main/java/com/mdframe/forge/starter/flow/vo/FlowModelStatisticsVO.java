package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

/** 流程模型目录状态统计。 */
@Data
public class FlowModelStatisticsVO {

    private long total;
    private long designing;
    private long deployed;
    private long suspended;
    private long disabled;
}
