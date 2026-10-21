package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 安装或更新租户内容资产。
 */
@Data
public class ContentInstalledAssetInstallDTO {

    @NotBlank(message = "资产类型不能为空")
    @Size(max = 32, message = "资产类型过长")
    private String kind;

    @Size(max = 128, message = "资产键过长")
    private String assetKey;

    @NotBlank(message = "资产名称不能为空")
    @Size(max = 200, message = "资产名称过长")
    private String assetName;

    @Size(max = 64, message = "版本号过长")
    private String assetVersion;

    /** 完整内容包 JSON（含 kind/name/payload）或仅 payload 对象 JSON */
    @NotBlank(message = "模板包内容不能为空")
    private String packageJson;

    @Size(max = 255, message = "文件名过长")
    private String sourceFileName;

    @Size(max = 500, message = "备注过长")
    private String remark;
}
