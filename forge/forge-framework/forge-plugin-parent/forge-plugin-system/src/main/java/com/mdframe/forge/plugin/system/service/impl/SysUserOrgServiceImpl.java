package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.service.ISysUserOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @date 2025/12/4
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SysUserOrgServiceImpl extends ServiceImpl<SysUserOrgMapper, SysUserOrg> implements ISysUserOrgService {

    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;

    /**
     * 绑定用户组织关系（主组织变更时同步regionCode）
     */
    public boolean bindUserOrg(Long userId, Long orgId, Integer isMain) {
        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setUserId(userId);
        userOrg.setOrgId(orgId);
        userOrg.setIsMain(isMain);
        
        boolean result = this.save(userOrg);
        
        // 如果是主组织，同步更新用户的regionCode
        if (result && isMain != null && isMain == 1) {
            syncUserRegionCode(userId, orgId);
        }
        
        return result;
    }

    /**
     * 更新用户主组织（同步regionCode）
     */
    public boolean updateUserMainOrg(Long userId, Long newOrgId) {
        // 1. 清除旧的主组织标记
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getIsMain, 1);
        SysUserOrg oldMainOrg = this.getOne(wrapper);
        if (oldMainOrg != null) {
            oldMainOrg.setIsMain(0);
            this.updateById(oldMainOrg);
        }
        
        // 2. 设置新的主组织
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getOrgId, newOrgId);
        SysUserOrg newOrg = this.getOne(wrapper);
        if (newOrg != null) {
            newOrg.setIsMain(1);
            this.updateById(newOrg);
        } else {
            newOrg = new SysUserOrg();
            newOrg.setUserId(userId);
            newOrg.setOrgId(newOrgId);
            newOrg.setIsMain(1);
            this.save(newOrg);
        }
        
        // 3. 同步regionCode
        syncUserRegionCode(userId, newOrgId);
        
        return true;
    }

    /**
     * 同步用户regionCode
     */
    private void syncUserRegionCode(Long userId, Long orgId) {
        SysOrg org = orgMapper.selectById(orgId);
        if (org != null && StrUtil.isNotBlank(org.getRegionCode())) {
            SysUser user = userMapper.selectById(userId);
            if (user != null) {
                user.setRegionCode(org.getRegionCode());
                userMapper.updateById(user);
                log.info("同步用户regionCode: userId={}, orgId={}, regionCode={}",
                        userId, orgId, org.getRegionCode());
            }
        }
    }
}
