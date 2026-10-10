package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 仅构造内存资源和 Mapper 查询元数据，不连接数据库或用户会话。 */
final class ResourceFeatureTestSupport {

    private ResourceFeatureTestSupport() {
    }

    static void initializeMetadata(Class<?>... entities) {
        for (Class<?> entity : entities) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "t3"), entity);
        }
    }

    static LoginUser user(boolean admin) {
        LoginUser user = new LoginUser();
        user.setUserId(91L);
        user.setTenantId(17L);
        user.setUserType(admin ? 0 : 2);
        user.setRoleIds(List.of(30L, 31L));
        return user;
    }

    static SysResource resource(long id, int type, String feature) {
        SysResource resource = new SysResource();
        resource.setId(id);
        resource.setParentId(0L);
        resource.setResourceType(type);
        resource.setResourceName("测试资源" + id);
        resource.setFeatureCode(feature);
        resource.setPerms("test:resource:" + id);
        resource.setVisible(1);
        resource.setMenuStatus(1);
        resource.setMinUserType(2);
        return resource;
    }

    static SysRoleResource binding(long resourceId) {
        SysRoleResource binding = new SysRoleResource();
        binding.setTenantId(17L);
        binding.setRoleId(30L);
        binding.setResourceId(resourceId);
        return binding;
    }

    static void assertRoleScope(Wrapper<?> wrapper) {
        assertThat(wrapper.getSqlSegment()).contains("role_id IN", "tenant_id =");
        assertThat(((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs().values())
                .containsExactlyInAnyOrder(30L, 31L, 17L);
    }
}
