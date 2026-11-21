package com.mdframe.forge.plugin.ai.skill.pkg.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.skill.pkg.SkillPackageManager;
import com.mdframe.forge.plugin.ai.skill.pkg.dto.SkillPackageActionDTO;
import com.mdframe.forge.plugin.ai.skill.pkg.dto.SkillPackageBindDTO;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/ai/skill/package")
@RequiredArgsConstructor
public class SkillPackageController {

    private final SkillPackageManager packages;

    @GetMapping("/page")
    @SaCheckPermission("ai:skill:package:query")
    public RespInfo<Page<SkillPackageManager.Summary>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return RespInfo.success(packages.page(pageNum, pageSize, keyword, status));
    }

    @GetMapping("/versions")
    @SaCheckPermission("ai:skill:package:query")
    public RespInfo<List<SkillPackageManager.Summary>> versions(@RequestParam Long packageId) {
        return RespInfo.success(packages.versions(packageId));
    }

    @GetMapping("/versions/{id}")
    @SaCheckPermission("ai:skill:package:query")
    public RespInfo<SkillPackageManager.Detail> detail(@PathVariable Long id) {
        return RespInfo.success(packages.detail(id));
    }

    @GetMapping("/lookup")
    @SaCheckPermission("ai:skill:package:query")
    public RespInfo<SkillPackageManager.Summary> lookup(
            @RequestParam String code,
            @RequestParam String version,
            @RequestParam String packageHash) {
        return RespInfo.success(packages.lookup(code, version, packageHash));
    }

    @PostMapping("/preview")
    @SaCheckPermission({"ai:skill:package:query", "ai:skill:package:import"})
    public RespInfo<SkillPackageManager.Preview> preview(@RequestPart MultipartFile file) {
        return RespInfo.success(packages.preview(file));
    }

    @PostMapping("/import")
    @SaCheckPermission({"ai:skill:package:query", "ai:skill:package:import"})
    @OperationLog(module = "技能包", desc = "导入固定版本", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<SkillPackageManager.Summary> importPackage(
            @RequestPart MultipartFile file,
            @RequestParam String expectedArchiveHash) {
        return RespInfo.success(packages.importPackage(file, expectedArchiveHash));
    }

    @PostMapping("/versions/{id}/activate")
    @SaCheckPermission({"ai:skill:package:query", "ai:skill:package:activate"})
    @OperationLog(module = "技能包", desc = "启用固定版本", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<SkillPackageManager.Summary> activate(
            @PathVariable Long id,
            @Valid @RequestBody SkillPackageActionDTO dto) {
        return RespInfo.success(packages.activate(id, dto));
    }

    @PostMapping("/versions/{id}/retire")
    @SaCheckPermission({"ai:skill:package:query", "ai:skill:package:activate"})
    @OperationLog(module = "技能包", desc = "停用固定版本", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<SkillPackageManager.Summary> retire(
            @PathVariable Long id,
            @Valid @RequestBody SkillPackageActionDTO dto) {
        return RespInfo.success(packages.retire(id, dto));
    }

    @GetMapping("/agent/{agentId}")
    @SaCheckPermission("ai:skill:package:query")
    public RespInfo<List<SkillPackageManager.Summary>> agentPackages(@PathVariable Long agentId) {
        return RespInfo.success(packages.listAgentPackages(agentId));
    }

    @PostMapping("/bind")
    @SaCheckPermission({"ai:skill:package:query", "ai:skill:package:bind"})
    @OperationLog(module = "技能包", desc = "绑定智能体技能资料包", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<SkillPackageManager.Summary> bind(@Valid @RequestBody SkillPackageBindDTO dto) {
        return RespInfo.success(packages.bindAgent(dto));
    }

    @DeleteMapping("/agent/{agentId}/{versionId}")
    @SaCheckPermission({"ai:skill:package:query", "ai:skill:package:bind"})
    @OperationLog(module = "技能包", desc = "解绑智能体技能资料包", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> unbind(@PathVariable Long agentId, @PathVariable Long versionId) {
        packages.unbindAgent(agentId, versionId);
        return RespInfo.success();
    }
}
