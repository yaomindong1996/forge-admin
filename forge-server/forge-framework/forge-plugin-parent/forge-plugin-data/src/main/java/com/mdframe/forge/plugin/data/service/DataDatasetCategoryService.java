package com.mdframe.forge.plugin.data.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.plugin.data.dto.DataDatasetCategorySaveDTO;
import com.mdframe.forge.plugin.data.entity.DataDatasetCategory;

import java.util.List;

public interface DataDatasetCategoryService extends IService<DataDatasetCategory> {

    List<DataDatasetCategory> listTree();

    DataDatasetCategory getByCode(String categoryCode);

    DataDatasetCategory saveCategory(DataDatasetCategorySaveDTO dto);

    DataDatasetCategory updateCategory(DataDatasetCategorySaveDTO dto);

    void deleteCategory(Long id);
}
