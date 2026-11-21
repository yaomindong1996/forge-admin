package com.mdframe.forge.plugin.ai.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.resource.domain.McpConnection;
import com.mdframe.forge.plugin.ai.resource.domain.McpTool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface McpResourceMapper extends BaseMapper<McpConnection> {

    List<McpConnection> connections(@Param("tenant") Long tenant);

    McpConnection connection(@Param("tenant") Long tenant, @Param("id") Long id);

    McpConnection byCode(@Param("tenant") Long tenant, @Param("code") String code);

    int updateConnection(@Param("row") McpConnection row, @Param("expected") int expected);

    int deleteConnection(@Param("tenant") Long tenant, @Param("id") Long id,
                         @Param("expected") int expected, @Param("actor") Long actor);

    int syncResult(@Param("tenant") Long tenant, @Param("id") Long id, @Param("expected") int expected,
                   @Param("state") String state, @Param("error") String error, @Param("actor") Long actor);

    List<McpTool> tools(@Param("tenant") Long tenant, @Param("connectionId") Long connectionId);

    McpTool tool(@Param("tenant") Long tenant, @Param("key") String key);

    int invalidate(@Param("tenant") Long tenant, @Param("id") Long id, @Param("actor") Long actor);

    int insertTool(@Param("row") McpTool row);

    int touchTool(@Param("tenant") Long tenant, @Param("key") String key,
                  @Param("version") int version, @Param("actor") Long actor);

    int bindingCount(@Param("tenant") Long tenant, @Param("source") String source, @Param("key") String key);
}
