package com.mdframe.forge.plugin.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.data.entity.DataConnection;

import java.util.List;

public interface DataConnectionService extends IService<DataConnection> {

    IPage<DataConnection> page(String connectionName, String dbType, Integer status, Integer pageNum, Integer pageSize);

    List<DataConnection> listAll();

    DataConnection getByCode(String connectionCode);

    boolean hasDatasetReference(Long connectionId);
}