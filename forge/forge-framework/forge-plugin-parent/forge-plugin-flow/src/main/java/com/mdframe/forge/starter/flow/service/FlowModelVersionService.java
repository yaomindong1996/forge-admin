package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.dto.VersionCompareDTO;
import com.mdframe.forge.starter.flow.dto.VersionRevertDTO;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowModelVersion;
import com.mdframe.forge.starter.flow.vo.VersionCompareVO;
import com.mdframe.forge.starter.flow.vo.VersionDetailVO;
import com.mdframe.forge.starter.flow.vo.VersionRevertVO;

public interface FlowModelVersionService extends IService<FlowModelVersion> {

    IPage<FlowModelVersion> pageVersionList(Page<FlowModelVersion> page, String modelId);

    VersionDetailVO getVersionDetail(String versionId);

    VersionCompareVO compareVersions(VersionCompareDTO dto);

    VersionRevertVO revertVersion(VersionRevertDTO dto);

    void updateVersionTag(String versionId, String versionTag);

    void deleteVersion(String versionId);

    void insertVersionOnPublish(FlowModel model, String changeDescription);
}