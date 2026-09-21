package com.mdframe.forge.plugin.print.mapper;

import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 所有调用须先做来源授权；多语句写入由服务事务及应用/模板行锁串行化。
 */
@Mapper
public interface PrintTemplateMapper {

    int insert(PrintTemplate row);

    PrintTemplate selectScoped(@Param("tenantId") Long tenantId, @Param("id") Long id);

    PrintTemplate lockScoped(@Param("tenantId") Long tenantId, @Param("id") Long id);

    List<PrintTemplate> selectApplication(@Param("tenantId") Long tenantId, @Param("applicationId") Long applicationId, @Param("offset") long offset, @Param("limit") int limit);

    long countApplication(@Param("tenantId") Long tenantId, @Param("applicationId") Long applicationId);

    int updateDraft(@Param("row") PrintTemplate row, @Param("expectedRevision") Long expectedRevision);

    int publish(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("expectedRevision") Long expectedRevision, @Param("versionId") Long versionId, @Param("actor") Long actor);

    int changeStatus(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("expectedRevision") Long expectedRevision, @Param("status") Integer status, @Param("actor") Long actor);

    int softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("expectedRevision") Long expectedRevision, @Param("actor") Long actor);
}
