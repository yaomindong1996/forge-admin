package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据Mapper接口
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    /**
     * 按类型查询有效字典项。
     */
    List<SysDictData> selectEnabledByType(@Param("dictType") String dictType);

    /**
     * 统计同一字典类型下相同键值的有效字典项数量
     *
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @param excludeDictCode 修改时需要排除的字典编码
     * @return 匹配数量
     */
    int countByDictTypeAndValue(@Param("dictType") String dictType,
                                @Param("dictValue") String dictValue,
                                @Param("excludeDictCode") Long excludeDictCode);
}
