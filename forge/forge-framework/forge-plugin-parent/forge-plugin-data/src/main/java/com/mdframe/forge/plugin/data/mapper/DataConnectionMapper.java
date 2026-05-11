package com.mdframe.forge.plugin.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataConnectionMapper extends BaseMapper<DataConnection> {

    IPage<DataConnection> selectConnectionPage(Page<DataConnection> page, @Param("tenantId") Long tenantId, 
        @Param("connectionName") String connectionName, @Param("dbType") String dbType, @Param("status") Integer status);

    List<DataConnection> selectConnectionList(@Param("tenantId") Long tenantId);

    DataConnection selectConnectionByCode(@Param("connectionCode") String connectionCode, @Param("tenantId") Long tenantId);

    int selectDatasetCountByConnectionId(@Param("connectionId") Long connectionId, @Param("tenantId") Long tenantId);
}