package com.mdframe.forge.starter.id.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.id.entity.SysIdSequence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysIdSequenceMapper extends BaseMapper<SysIdSequence> {
    /**
     * 乐观锁更新maxId（分配一个新段）
     */
    int allocateSegment(@Param("bizKey") String bizKey, @Param("step") int step, @Param("version") int version);

    /**
     * 查询旧编码规则在同一周期已经分配的最大水位。
     *
     * <p>旧 key 包含调用方 scope，新结构化规则会把多个旧 scope 合并到稳定规则 key，
     * 因此取相同规则、相同周期下所有旧 scope 的最大值作为新 key 的安全起点。</p>
     */
    Long selectLegacyMaxId(@Param("escapedLegacyKeyPattern") String escapedLegacyKeyPattern,
                           @Param("legacyPeriod") String legacyPeriod);
}
