package com.mdframe.forge.plugin.system.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysPluginDelivery;
import com.mdframe.forge.plugin.system.entity.SysPluginArtifactRegistration;
import com.mdframe.forge.plugin.system.vo.PluginDeliveryTargetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface SysPluginDeliveryMapper extends BaseMapper<SysPluginDelivery> {
    void ensureTarget(@Param("tenant") Long tenant, @Param("id") String id, @Param("target") String target);
    PluginDeliveryTargetVO lockTarget(@Param("tenant") Long tenant, @Param("target") String target);
    List<PluginDeliveryTargetVO> targets(@Param("tenant") Long tenant);
    List<SysPluginDelivery> recent(@Param("tenant") Long tenant);
    int expire(@Param("tenant") Long tenant, @Param("now") java.time.LocalDateTime now);
    SysPluginDelivery lock(@Param("tenant") Long tenant, @Param("id") String id);
    SysPluginDelivery request(@Param("tenant") Long tenant, @Param("actor") Long actor,
                              @Param("request") String request);
    SysPluginArtifactRegistration candidate(@Param("tenant") Long tenant, @Param("task") String task,
                                            @Param("release") String release);
    List<SysPluginArtifactRegistration> candidates(@Param("tenant") Long tenant);
    int published(@Param("tenant") Long tenant, @Param("target") String target, @Param("release") String release);
    int occupy(@Param("tenant") Long tenant, @Param("target") String target, @Param("id") String id);
    int complete(@Param("tenant") Long tenant, @Param("row") SysPluginDelivery row);
}
