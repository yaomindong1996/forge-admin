package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.entity.SysFileGroup;

import java.util.List;
import java.util.Map;

/**
 * 文件分组Service接口
 */
public interface ISysFileGroupService extends IService<SysFileGroup> {

    /**
     * 获取分组列表（带文件数量）
     */
    List<SysFileGroup> listGroupWithFileCount();

    /**
     * 获取文件统计数据
     */
    Map<String, Object> getFileStatistics();

    /**
     * 创建分组
     */
    boolean createGroup(SysFileGroup group);

    /**
     * 更新分组
     */
    boolean updateGroup(SysFileGroup group);

    /**
     * 删除分组
     */
    boolean deleteGroup(Long id);
}
