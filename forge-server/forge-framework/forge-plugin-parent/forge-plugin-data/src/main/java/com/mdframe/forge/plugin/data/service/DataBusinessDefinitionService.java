package com.mdframe.forge.plugin.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.plugin.data.dto.DataBusinessDefinitionSaveDTO;
import com.mdframe.forge.plugin.data.entity.DataBusinessDefinition;
import com.mdframe.forge.plugin.data.vo.DataBusinessAiContextVO;
import com.mdframe.forge.plugin.data.vo.DataBusinessDefinitionDetailVO;

import java.util.List;

public interface DataBusinessDefinitionService extends IService<DataBusinessDefinition> {

    IPage<DataBusinessDefinition> page(String businessName, Integer status, Integer pageNum, Integer pageSize);

    List<DataBusinessDefinition> listEnabled();

    DataBusinessDefinitionDetailVO getDetail(Long id);

    DataBusinessDefinition saveBusiness(DataBusinessDefinitionSaveDTO dto);

    DataBusinessDefinition updateBusiness(DataBusinessDefinitionSaveDTO dto);

    void deleteBusiness(Long id);

    DataBusinessAiContextVO getAiContext(Long id);
}
