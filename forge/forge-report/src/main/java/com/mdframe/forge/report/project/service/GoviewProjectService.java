package com.mdframe.forge.report.project.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.report.project.domain.GoviewProject;
import com.mdframe.forge.report.project.mapper.GoviewProjectMapper;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Go-View 项目 Service
 */
@Service
public class GoviewProjectService extends ServiceImpl<GoviewProjectMapper, GoviewProject> {

    /**
     * 发布项目
     */
    public void publishProject(Long id, String publishUrl) {
        GoviewProject project = getById(id);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        project.setPublishStatus("1");
        project.setPublishUrl(publishUrl);
        project.setPublishTime(new Date());
        updateById(project);
    }
}
