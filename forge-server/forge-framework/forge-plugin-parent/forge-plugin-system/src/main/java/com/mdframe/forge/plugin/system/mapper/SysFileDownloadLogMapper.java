package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysFileDownloadLogQuery;
import com.mdframe.forge.plugin.system.entity.SysFileDownloadLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysFileDownloadLogMapper extends BaseMapper<SysFileDownloadLog> {

    Page<SysFileDownloadLog> selectDownloadLogPage(Page<SysFileDownloadLog> page,
                                                    @Param("query") SysFileDownloadLogQuery query,
                                                    @Param("userId") Long userId);

    Long selectMetadataFileSize(@Param("fileId") String fileId);
}
