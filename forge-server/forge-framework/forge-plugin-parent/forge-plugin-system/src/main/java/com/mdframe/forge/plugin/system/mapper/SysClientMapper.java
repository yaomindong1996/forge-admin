package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.entity.SysClient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysClientMapper extends BaseMapper<SysClient> {

    Page<SysClient> selectClientPage(Page<SysClient> page,
                                     @Param("clientCode") String clientCode,
                                     @Param("clientName") String clientName,
                                     @Param("status") Integer status);

    List<SysClient> selectEnabledClients(@Param("tenantId") Long tenantId);

    SysClient selectByClientCode(@Param("clientCode") String clientCode);

    SysClient selectByAppId(@Param("appId") String appId);

    int updateClientIfUnchanged(@Param("client") SysClient client,
                                @Param("expectedAuthMethod") String expectedAuthMethod,
                                @Param("expectedSecret") String expectedSecret,
                                @Param("appSecret") String appSecret,
                                @Param("secretChanged") boolean secretChanged);

    int updateAppSecretIfUnchanged(@Param("id") Long id,
                                   @Param("expectedAuthMethod") String expectedAuthMethod,
                                   @Param("expectedSecret") String expectedSecret,
                                   @Param("appSecret") String appSecret);

    int countLegacyPlaintextSecrets();
}
