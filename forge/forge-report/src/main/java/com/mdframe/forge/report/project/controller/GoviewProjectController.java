package com.mdframe.forge.report.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.report.project.domain.GoviewProject;
import com.mdframe.forge.report.project.service.GoviewProjectService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Go-View 项目 Controller
 */
@RestController
@RequestMapping("/goview/project")
@RequiredArgsConstructor
public class GoviewProjectController {

    private final GoviewProjectService projectService;

    /**
     * 分页查询项目列表
     */
    @GetMapping("/page")
    public RespInfo<Page<GoviewProject>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName) {
        
        LambdaQueryWrapper<GoviewProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(projectName != null, GoviewProject::getProjectName, projectName)
               .orderByDesc(GoviewProject::getCreateTime);
        
        Page<GoviewProject> page = projectService.page(new Page<>(pageNum, pageSize), wrapper);
        return RespInfo.success(page);
    }

    /**
     * 查询项目详情
     */
    @GetMapping("/{id}")
    public RespInfo<GoviewProject> getById(@PathVariable Long id) {
        GoviewProject project = projectService.getById(id);
        return RespInfo.success(project);
    }

    /**
     * 创建项目
     */
    @PostMapping
    public RespInfo<Void> create(@RequestBody GoviewProject project) {
        projectService.save(project);
        return RespInfo.success();
    }

    /**
     * 更新项目
     */
    @PutMapping
    public RespInfo<Void> update(@RequestBody GoviewProject project) {
        projectService.updateById(project);
        return RespInfo.success();
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        projectService.removeById(id);
        return RespInfo.success();
    }

    /**
     * 发布项目
     */
    @PostMapping("/publish/{id}")
    public RespInfo<Void> publish(@PathVariable Long id, @RequestParam String publishUrl) {
        projectService.publishProject(id, publishUrl);
        return RespInfo.success();
    }
}
