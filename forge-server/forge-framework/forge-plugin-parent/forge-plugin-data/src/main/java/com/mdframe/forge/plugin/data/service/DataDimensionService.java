package com.mdframe.forge.plugin.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.plugin.data.dto.DataDimensionItemDTO;
import com.mdframe.forge.plugin.data.dto.DataDimensionSaveDTO;
import com.mdframe.forge.plugin.data.entity.DataDimension;
import com.mdframe.forge.plugin.data.entity.DataDimensionItem;

import java.util.List;

public interface DataDimensionService extends IService<DataDimension> {

    IPage<DataDimension> page(String dimensionName, String sourceType, Integer status, Integer pageNum, Integer pageSize);

    List<DataDimension> listEnabled();

    DataDimension saveDimension(DataDimensionSaveDTO dto);

    DataDimension updateDimension(DataDimensionSaveDTO dto);

    void deleteDimension(Long id);

    List<DataDimensionItem> listItems(Long dimensionId);

    void saveItems(Long dimensionId, List<DataDimensionItemDTO> items);

    List<DataDimensionItem> syncItems(Long dimensionId);
}
