package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.service.MessageReceiverResolver;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.service.ISysUserOrgService;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 默认的接收对象解析器实现
 * 实际项目中需要集成用户服务和组织服务
 */
@Slf4j
@Service
public class DefaultMessageReceiverResolver implements MessageReceiverResolver {
    
    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysUserOrgService sysUserOrgService;
    
    
    @Override
    public void processUsersByScope(String sendScope,
                                   Set<Long> userIds,
                                   Set<Long> orgIds,
                                   Set<Long> tenantIds,
                                   Consumer<List<Long>> batchConsumer,
                                   int batchSize) {
        
        if ("ALL".equals(sendScope)) {
            // 全员：分批查询所有用户
            processAllUsers(batchConsumer, batchSize);
        } else if ("ORG".equals(sendScope) && orgIds != null && !orgIds.isEmpty()) {
            // 指定组织：分批查询组织下的用户
            processUsersByOrgs(orgIds, batchConsumer, batchSize);
        } else if ("USERS".equals(sendScope) && userIds != null && !userIds.isEmpty()) {
            // 指定人员：直接分批处理
            processSpecifiedUsers(userIds, batchConsumer, batchSize);
        }
        
        // 如果指定了tenantIds，额外处理租户维度
        if (tenantIds != null && !tenantIds.isEmpty()) {
            processUsersByTenants(tenantIds, batchConsumer, batchSize);
        }
    }
    
    /**
     * 分批处理所有用户（全员发送）
     */
    private void processAllUsers(Consumer<List<Long>> batchConsumer, int batchSize) {
         int pageNum = 1;
         while (true) {
             Page<SysUser> page = sysUserService.page(new Page<>(pageNum, batchSize),
                 new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserStatus, 1));
             if (page.getRecords().isEmpty()) {
                 break;
             }
             List<Long> userIds = page.getRecords().stream()
                 .map(SysUser::getId)
                 .collect(Collectors.toList());
             batchConsumer.accept(userIds);
             if (!page.hasNext()) {
                 break;
             }
             pageNum++;
         }
    }
    
    /**
     * 分批处理指定组织下的用户
     */
    private void processUsersByOrgs(Set<Long> orgIds, Consumer<List<Long>> batchConsumer, int batchSize) {
         int pageNum = 1;
         while (true) {
             Page<SysUserOrg> page = sysUserOrgService.page(new Page<>(pageNum, batchSize),
                 new LambdaQueryWrapper<SysUserOrg>().in(SysUserOrg::getOrgId, orgIds));
             if (page.getRecords().isEmpty()) {
                 break;
             }

             List<Long> userIds = page.getRecords().stream()
                 .map(SysUserOrg::getUserId)
                 .distinct()
                 .collect(Collectors.toList());
             batchConsumer.accept(userIds);

             if (!page.hasNext()) {
                 break;
             }
             pageNum++;
         }
    }
    
    /**
     * 分批处理指定的用户ID列表
     */
    private void processSpecifiedUsers(Set<Long> userIds, Consumer<List<Long>> batchConsumer, int batchSize) {
        List<Long> userIdList = new ArrayList<>(userIds);
        
        // 分批处理
        for (int i = 0; i < userIdList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, userIdList.size());
            List<Long> batch = userIdList.subList(i, end);
            batchConsumer.accept(batch);
        }
    }
    
    /**
     * 分批处理指定租户下的用户
     */
    private void processUsersByTenants(Set<Long> tenantIds, Consumer<List<Long>> batchConsumer, int batchSize) {
         int pageNum = 1;
         while (true) {
             Page<SysUser> page = sysUserService.page(new Page<>(pageNum, batchSize),
                 new LambdaQueryWrapper<SysUser>().in(SysUser::getTenantId, tenantIds));
             if (page.getRecords().isEmpty()) {
                 break;
             }

             List<Long> userIds = page.getRecords().stream()
                 .map(SysUser::getId)
                 .collect(Collectors.toList());
             batchConsumer.accept(userIds);

             if (!page.hasNext()) {
                 break;
             }
             pageNum++;
         }
        
        log.warn("需要集成租户服务以分批获取租户下的用户");
    }
}
