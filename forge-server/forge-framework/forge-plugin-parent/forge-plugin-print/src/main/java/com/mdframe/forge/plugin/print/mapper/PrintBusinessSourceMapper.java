package com.mdframe.forge.plugin.print.mapper;

import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrintBusinessSourceMapper {

    int insert(PrintBusinessSource row);

    PrintBusinessSource selectScoped(@Param("tenantId") Long tenantId, @Param("id") Long id);

    PrintBusinessSource lockScoped(@Param("tenantId") Long tenantId, @Param("id") Long id);

    PrintBusinessSource selectByCode(@Param("tenantId") Long tenantId, @Param("sourceCode") String sourceCode);

    List<PrintBusinessSource> selectPage(@Param("tenantId") Long tenantId,
                                         @Param("sourceName") String sourceName,
                                         @Param("sourceType") String sourceType,
                                         @Param("status") Integer status,
                                         @Param("offset") long offset,
                                         @Param("limit") int limit);

    long countPage(@Param("tenantId") Long tenantId,
                   @Param("sourceName") String sourceName,
                   @Param("sourceType") String sourceType,
                   @Param("status") Integer status);

    long countTemplateReferences(@Param("tenantId") Long tenantId, @Param("id") Long id);

    int updateSource(@Param("row") PrintBusinessSource row,
                     @Param("expectedRevision") Long expectedRevision);

    int changeStatus(@Param("tenantId") Long tenantId,
                     @Param("id") Long id,
                     @Param("expectedRevision") Long expectedRevision,
                     @Param("status") Integer status,
                     @Param("actor") Long actor);

    int softDelete(@Param("tenantId") Long tenantId,
                   @Param("id") Long id,
                   @Param("expectedRevision") Long expectedRevision,
                   @Param("actor") Long actor);
}
