package com.mdframe.forge.starter.trans.spi;

/**
 * 字典值提供者SPI
 * 由业务模块实现，根据字典类型和key返回对应的显示值
 */
public interface DictValueProvider {

    /**
     * 根据字典类型和键获取显示值
     *
     * @param dictType 字典类型
     * @param key      字典键
     * @return 字典值（显示文本），找不到时返回null
     */
    String getLabel(String dictType, String key);
}
