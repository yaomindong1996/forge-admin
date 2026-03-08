package com.mdframe.forge.starter.id.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.id.entity.SysIdSequence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysIdSequenceMapper extends BaseMapper<SysIdSequence> {
    /**
     * 乐观锁更新maxId（分配一个新段）
     */
    @Update("UPDATE sys_id_sequence SET max_id = max_id + #{step}, version = version + 1 WHERE biz_key = #{bizKey} AND version = #{version}")
    int allocateSegment(@Param("bizKey") String bizKey, @Param("step") int step, @Param("version") int version);
}
