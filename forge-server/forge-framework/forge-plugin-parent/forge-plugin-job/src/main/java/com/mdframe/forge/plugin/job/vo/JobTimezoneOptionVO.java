package com.mdframe.forge.plugin.job.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 可配置的 IANA 时区选项。
 */
@Data
@Builder
public class JobTimezoneOptionVO {

    private String label;

    private String value;

    private String offset;
}
