package com.mdframe.forge.plugin.print.mapper;

import com.mdframe.forge.plugin.print.entity.PrintExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 所有调用须先做来源授权；多语句写入由服务事务及应用/模板行锁串行化。
 */
@Mapper
public interface PrintExecutionMapper {

    int insert(PrintExecution row);

    PrintExecution selectOwned(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("actor") Long actor);

    int recordEvent(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("actor") Long actor, @Param("result") String result, @Param("pageCount") Integer pageCount, @Param("errorCode") String errorCode);
}
