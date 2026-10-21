package com.mdframe.forge.plugin.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.plugin.system.entity.SysContentInstalledAsset;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentInstalledAssetVO {

    private Long id;

    private String kind;

    private String assetKey;

    private String assetName;

    private String assetVersion;

    private String sourceFileName;

    private String remark;

    /** 仅详情/选用时返回完整包 */
    private String packageJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public static ContentInstalledAssetVO summary(SysContentInstalledAsset row) {
        ContentInstalledAssetVO vo = base(row);
        vo.setPackageJson(null);
        return vo;
    }

    public static ContentInstalledAssetVO detail(SysContentInstalledAsset row) {
        return base(row);
    }

    private static ContentInstalledAssetVO base(SysContentInstalledAsset row) {
        ContentInstalledAssetVO vo = new ContentInstalledAssetVO();
        if (row == null) {
            return vo;
        }
        vo.setId(row.getId());
        vo.setKind(row.getKind());
        vo.setAssetKey(row.getAssetKey());
        vo.setAssetName(row.getAssetName());
        vo.setAssetVersion(row.getAssetVersion());
        vo.setSourceFileName(row.getSourceFileName());
        vo.setRemark(row.getRemark());
        vo.setPackageJson(row.getPackageJson());
        vo.setCreateTime(row.getCreateTime());
        vo.setUpdateTime(row.getUpdateTime());
        return vo;
    }
}
