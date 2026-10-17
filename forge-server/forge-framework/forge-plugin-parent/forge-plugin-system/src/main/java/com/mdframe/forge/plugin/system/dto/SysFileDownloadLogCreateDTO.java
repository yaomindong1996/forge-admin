package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 对象存储直链下载补记。下载人只取当前登录用户。
 */
@Data
public class SysFileDownloadLogCreateDTO {

    @Size(max = 64, message = "文件ID过长")
    private String fileId;

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名过长")
    private String fileName;

    private Long fileSize;
}
