package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

/**
 * 下载记录分页条件。用户范围由服务端按身份追加，不接受前端传 userId。
 */
@Data
public class SysFileDownloadLogQuery {

    private String fileName;

    private String source;

    private String username;

    private String startTime;

    private String endTime;
}
