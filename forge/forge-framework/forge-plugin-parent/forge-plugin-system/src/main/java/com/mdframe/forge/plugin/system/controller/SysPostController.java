package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.SysPostDTO;
import com.mdframe.forge.plugin.system.dto.SysPostQuery;
import com.mdframe.forge.plugin.system.entity.SysPost;
import com.mdframe.forge.plugin.system.service.ISysPostService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理Controller
 */
@RestController
@RequestMapping("/system/post")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysPostController {

    private final ISysPostService postService;

    /**
     * 分页查询岗位列表
     */
    @GetMapping("/page")
    public RespInfo<IPage<SysPost>> page(SysPostQuery query) {
        IPage<SysPost> page = postService.selectPostPage(query);
        return RespInfo.success(page);
    }

    /**
     * 查询岗位列表
     */
    @GetMapping("/list")
    public RespInfo<List<SysPost>> list(SysPostQuery query) {
        List<SysPost> list = postService.selectPostList(query);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询岗位详情
     */
    @PostMapping("/getById")
    public RespInfo<SysPost> getById(@RequestParam Long id) {
        SysPost post = postService.selectPostById(id);
        return RespInfo.success(post);
    }

    /**
     * 新增岗位
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysPostDTO dto) {
        boolean result = postService.insertPost(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改岗位
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysPostDTO dto) {
        boolean result = postService.updatePost(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除岗位
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long id) {
        boolean result = postService.deletePostById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除岗位
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        boolean result = postService.deletePostByIds(ids);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }
}
