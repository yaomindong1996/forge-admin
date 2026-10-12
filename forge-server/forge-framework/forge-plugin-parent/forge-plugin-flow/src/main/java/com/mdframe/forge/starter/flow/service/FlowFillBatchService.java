package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.starter.flow.dto.FlowFillBatchQueryDTO;
import com.mdframe.forge.starter.flow.entity.FlowFillBatch;
import com.mdframe.forge.starter.flow.entity.FlowFillBatchItem;

import java.util.List;

/**
 * 组织填报批次服务。
 */
public interface FlowFillBatchService extends IService<FlowFillBatch> {

    IPage<FlowFillBatch> pageBatches(FlowFillBatchQueryDTO query, Integer pageNum, Integer pageSize);

    List<FlowFillBatchItem> listItems(Long batchId);

    void saveBatchConfig(FlowFillBatch batch);

    void publishBatch(Long id);

    void deleteBatch(Long id);
}
