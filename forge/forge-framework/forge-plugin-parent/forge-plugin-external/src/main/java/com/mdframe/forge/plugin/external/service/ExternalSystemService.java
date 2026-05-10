package com.mdframe.forge.plugin.external.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.external.dto.ExternalSystemQuery;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;

import java.util.List;

public interface ExternalSystemService extends IService<ExternalSystem> {

    IPage<ExternalSystem> page(ExternalSystemQuery query);

    List<ExternalSystem> listAll();

    ExternalSystem getByCode(String systemCode);
}