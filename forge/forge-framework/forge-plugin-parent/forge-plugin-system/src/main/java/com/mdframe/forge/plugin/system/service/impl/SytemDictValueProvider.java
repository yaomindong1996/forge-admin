package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.service.ISysDictDataService;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date 2025/11/28
 */
@Component
@Slf4j
public class SytemDictValueProvider implements DictValueProvider {
    
    @Autowired
    private ISysDictDataService sysDictDataService;
    
    @Override
    public String getLabel(String dictType, String key) {
        SysDictData sysDictData = sysDictDataService.lambdaQuery().eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDictValue, key).last("limit 1").one();
        if (sysDictData == null) {
            return null;
        }
        return sysDictData.getDictLabel();
    }
}
