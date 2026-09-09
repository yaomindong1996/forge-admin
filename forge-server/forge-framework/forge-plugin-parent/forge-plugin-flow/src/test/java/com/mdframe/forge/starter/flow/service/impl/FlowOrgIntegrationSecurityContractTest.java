package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowOrgIntegrationSecurityContractTest {

    @Test
    void userDisplayLookupMustRequireTenantAndEnabledMembership() throws IOException {
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowOrgIntegrationServiceImpl.java"));
        String mapper = Files.readString(Path.of(
                "../forge-plugin-system/src/main/resources/mapper/SysUserMapper.xml"));
        String roleMapper = Files.readString(Path.of(
                "../forge-plugin-system/src/main/resources/mapper/SysUserOrgRoleMapper.xml"));
        String roleLookup = Files.readString(Path.of(
                "../forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml"));
        String orgMapper = Files.readString(Path.of(
                "../forge-plugin-system/src/main/resources/mapper/SysOrgMapper.xml"));
        String userOrgMapper = Files.readString(Path.of(
                "../forge-plugin-system/src/main/resources/mapper/SysUserOrgMapper.xml"));
        String postMapper = Files.readString(Path.of(
                "../forge-plugin-system/src/main/resources/mapper/SysPostMapper.xml"));
        String roleListMapper = Files.readString(Path.of(
                "../forge-plugin-system/src/main/resources/mapper/SysRoleMapper.xml"));

        assertTrue(service.contains("缺少可信租户上下文"));
        assertTrue(service.contains("sysUserMapper.selectFlowUserInfo(tenantId, id)"));
        assertTrue(service.contains("getUserInfoBatch(List<String> userIds)"));
        assertTrue(service.contains("sysUserMapper.selectFlowUserInfoBatch(tenantId, ids)"));
        String taskService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        assertTrue(taskService.contains("Set<String> userIdsToLoad"));
        assertTrue(taskService.contains("getUserInfoBatch(new ArrayList<>(userIdsToLoad))"));
        assertTrue(service.contains("sysUserMapper.selectFlowUsers(tenantId, trimToNull(keyword), orgId)"));
        assertTrue(service.contains("sysUserOrgRoleMapper.selectUserIdsByRoleIdsAcrossOrg"));
        assertTrue(mapper.contains("<select id=\"selectFlowUserInfo\""));
        assertTrue(mapper.contains("<select id=\"selectFlowUserInfoBatch\""));
        assertTrue(mapper.contains("u.id IN"));
        assertTrue(mapper.contains("u.tenant_id = #{tenantId}"));
        assertTrue(mapper.contains("LIMIT 200"));
        assertTrue(roleMapper.contains("selectUserIdsByRoleIdsAcrossOrg"));
        assertTrue(roleMapper.contains("uor.tenant_id = #{tenantId}"));
        assertTrue(roleLookup.contains("selectActiveFlowRoleByKey"));
        assertTrue(roleLookup.contains("r.tenant_id = #{tenantId}"));
        assertTrue(service.contains("selectOrgAndChildrenIdsByTenant(oid, tenantId)"));
        assertTrue(service.contains("selectOrgChildrenByParentId(oid, tenantId)"));
        assertTrue(orgMapper.contains("<select id=\"selectOrgAndChildrenIdsByTenant\""));
        assertTrue(orgMapper.contains("tenant_id = #{tenantId}"));
        assertTrue(orgMapper.contains("org_status = 1"));
        assertTrue(service.contains("sysOrgMapper.selectFlowOrgById(tenantId, id)"));
        assertTrue(service.contains("sysUserOrgMapper.selectFlowMainOrgByUser(tenantId, uid)"));
        assertTrue(service.contains("sysUserOrgMapper.selectFlowAnyOrgByUser(tenantId, uid)"));
        assertTrue(service.contains("sysUserOrgMapper.countFlowUserOrg(tenantId, uid, oid)"));
        assertTrue(service.contains("sysOrgMapper.selectFlowOrgList(tenantId)"));
        assertTrue(service.contains("sysRoleMapper.selectActiveFlowRoles(tenantId)"));
        assertTrue(service.contains("sysPostMapper.selectFlowPosts(tenantId, queryOrgId)"));
        assertTrue(orgMapper.contains("<select id=\"selectFlowOrgById\""));
        assertTrue(orgMapper.contains("<select id=\"selectFlowOrgList\""));
        assertTrue(userOrgMapper.contains("<select id=\"selectFlowMainOrgByUser\""));
        assertTrue(userOrgMapper.contains("<select id=\"selectFlowAnyOrgByUser\""));
        assertTrue(userOrgMapper.contains("<select id=\"countFlowUserOrg\""));
        assertTrue(userOrgMapper.contains("uo.tenant_id = #{tenantId}"));
        assertTrue(postMapper.contains("<select id=\"selectFlowPosts\""));
        assertTrue(postMapper.contains("p.tenant_id = #{tenantId}"));
        assertTrue(roleListMapper.contains("<select id=\"selectActiveFlowRoles\""));
        assertTrue(roleListMapper.contains("r.tenant_id = #{tenantId}"));
    }
}
