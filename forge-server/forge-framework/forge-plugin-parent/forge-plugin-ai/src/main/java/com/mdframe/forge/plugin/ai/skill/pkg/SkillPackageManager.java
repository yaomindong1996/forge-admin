package com.mdframe.forge.plugin.ai.skill.pkg;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.agent.mapper.AiAgentMapper;
import com.mdframe.forge.plugin.ai.skill.pkg.domain.AiAgentSkillPackage;
import com.mdframe.forge.plugin.ai.skill.pkg.domain.SkillPackage;
import com.mdframe.forge.plugin.ai.skill.pkg.domain.SkillPackageFile;
import com.mdframe.forge.plugin.ai.skill.pkg.domain.SkillPackageVersion;
import com.mdframe.forge.plugin.ai.skill.pkg.dto.SkillPackageActionDTO;
import com.mdframe.forge.plugin.ai.skill.pkg.dto.SkillPackageBindDTO;
import com.mdframe.forge.plugin.ai.skill.pkg.mapper.AiAgentSkillPackageMapper;
import com.mdframe.forge.plugin.ai.skill.pkg.mapper.SkillPackageFileMapper;
import com.mdframe.forge.plugin.ai.skill.pkg.mapper.SkillPackageMapper;
import com.mdframe.forge.plugin.ai.skill.pkg.mapper.SkillPackageVersionMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 技能资料包（不可变版本与依赖闭包）管理器。
 */
@Component
@RequiredArgsConstructor
public class SkillPackageManager {

    public record Summary(Long versionId, Long packageId, String packageCode, String packageName,
                          String description, String versionName, String packageHash, String status,
                          Integer fileCount, Integer totalBytes, LocalDateTime createTime) {
    }

    public record Preview(SkillPackageArchive.PackageContent content, List<String> issues) {
    }

    public record Detail(Summary version, SkillPackageArchive.Manifest manifest,
                         List<SkillPackageArchive.FileContent> files, List<String> issues) {
    }

    public record Reference(Long packageId, Long versionId, String code, String version, String packageHash) {
    }

    public record Snapshot(String format, Long rootVersionId, List<Reference> references,
                           String instructions, String instructionsHash) {
    }

    private record Caller(Long tenantId, Long userId, Long departmentId) {
    }

    private final SkillPackageMapper packages;
    private final SkillPackageVersionMapper versions;
    private final SkillPackageFileMapper files;
    private final AiAgentSkillPackageMapper agentSkillPackages;
    private final SkillPackageArchive archive;
    private final AiAgentMapper agents;
    private final ObjectMapper mapper;

    public Page<Summary> page(int pageNum, int pageSize, String keyword, String status) {
        var ctx = caller();
        if (pageNum < 1 || pageSize < 1 || pageSize > 200 || (keyword != null && keyword.length() > 100)) {
            throw invalid("分页或搜索范围无效");
        }
        if (status != null && !status.isBlank()) {
            parseStatus(status);
        }
        var rows = packages.selectPage(new Page<>(pageNum, pageSize), ctx.tenantId(), keyword, status);
        return new Page<Summary>(rows.getCurrent(), rows.getSize(), rows.getTotal())
                .setRecords(rows.getRecords().stream().map(this::summary).toList());
    }

    public List<Summary> versions(Long packageId) {
        return packages.selectVersions(caller().tenantId(), packageId).stream()
                .map(this::summary)
                .toList();
    }

    public Preview preview(MultipartFile file) {
        var ctx = caller();
        var content = archive.read(file);
        return new Preview(content, issues(ctx.tenantId(), content));
    }

    @Transactional(rollbackFor = Exception.class)
    public Summary importPackage(MultipartFile file, String expectedArchiveHash) {
        var ctx = caller();
        var content = archive.read(file);
        if (!content.archiveHash().equals(expectedArchiveHash)) {
            throw conflict("压缩包与已核对预览不一致，请重新预览");
        }
        var existing = packages.selectExact(ctx.tenantId(), content.manifest().code(), content.manifest().version());
        if (existing != null) {
            if (!ctx.tenantId().equals(existing.getTenantId())) {
                throw denied();
            }
            return same(existing, content.packageHash());
        }
        var parent = packages.selectCode(ctx.tenantId(), content.manifest().code());
        if (parent == null) {
            parent = new SkillPackage();
            parent.setId(IdWorker.getId());
            parent.setPackageCode(content.manifest().code());
            parent.setPackageName(content.manifest().name());
            parent.setDescription(content.manifest().description());
            audit(parent, ctx);
            parent.setDelFlag(0L);
            try {
                if (packages.insert(parent) != 1) {
                    throw conflict("技能包创建失败");
                }
            } catch (DuplicateKeyException ex) {
                parent = packages.selectCode(ctx.tenantId(), content.manifest().code());
                if (parent == null) {
                    throw conflict("技能包正在导入，请回查原版本");
                }
            }
        }
        if (!ctx.tenantId().equals(parent.getTenantId())) {
            throw denied();
        }
        var row = new SkillPackageVersion();
        row.setId(IdWorker.getId());
        row.setPackageId(parent.getId());
        row.setVersionName(content.manifest().version());
        row.setPackageHash(content.packageHash());
        row.setArchiveHash(content.archiveHash());
        row.setManifestJson(content.manifestJson());
        row.setFileCount(content.files().size());
        row.setTotalBytes(content.totalBytes());
        row.setStatus(SkillPackageStatus.IMPORTED.getCode());
        row.setDelFlag(0L);
        audit(row, ctx);
        try {
            if (versions.insert(row) != 1) {
                throw conflict("技能版本导入失败");
            }
        } catch (DuplicateKeyException ex) {
            var raced = packages.selectExactCurrent(
                    ctx.tenantId(), content.manifest().code(), content.manifest().version());
            if (raced == null) {
                throw conflict("版本结果未确认，请回查原版本");
            }
            if (!ctx.tenantId().equals(raced.getTenantId())) {
                throw denied();
            }
            return same(raced, content.packageHash());
        }
        for (var item : content.files()) {
            var record = new SkillPackageFile();
            record.setId(IdWorker.getId());
            record.setVersionId(row.getId());
            record.setFilePath(item.path());
            record.setContent(item.content());
            record.setContentHash(item.sha256());
            record.setSizeBytes(item.sizeBytes());
            record.setDelFlag(0L);
            audit(record, ctx);
            if (files.insert(record) != 1) {
                throw conflict("技能文件保存失败");
            }
        }
        row.setPackageCode(content.manifest().code());
        row.setPackageName(content.manifest().name());
        return summary(row);
    }

    public Summary lookup(String code, String version, String hash) {
        if (!SkillPackageArchive.code(code) || !SkillPackageArchive.version(version)
                || hash == null || !hash.matches("[a-f0-9]{64}")) {
            throw invalid("回查版本参数无效");
        }
        var row = packages.selectExact(caller().tenantId(), code, version);
        return row == null ? null : same(row, hash);
    }

    public Detail detail(Long id) {
        var ctx = caller();
        var row = required(ctx.tenantId(), id);
        var content = content(row);
        return new Detail(summary(row), content.manifest(), content.files(), issues(ctx.tenantId(), content));
    }

    @Transactional(rollbackFor = Exception.class)
    public Summary activate(Long id, SkillPackageActionDTO dto) {
        var ctx = caller();
        var row = required(ctx.tenantId(), id);
        exactAction(row, dto);
        if (!SkillPackageStatus.IMPORTED.matches(row.getStatus())) {
            throw conflict("仅待启用版本可以启用；请导入新的版本");
        }
        var snapshot = snapshot(ctx.tenantId(), row, false);
        if (packages.changeState(ctx.tenantId(), id, row.getPackageHash(), row.getStatus(),
                SkillPackageStatus.READY.getCode(), json(snapshot), ctx.userId()) != 1) {
            throw conflict("版本状态已变化，请刷新");
        }
        row.setStatus(SkillPackageStatus.READY.getCode());
        return summary(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public Summary retire(Long id, SkillPackageActionDTO dto) {
        var ctx = caller();
        var row = required(ctx.tenantId(), id);
        exactAction(row, dto);
        if (SkillPackageStatus.RETIRED.matches(row.getStatus())) {
            return summary(row);
        }
        if (!SkillPackageStatus.IMPORTED.matches(row.getStatus())
                && !SkillPackageStatus.READY.matches(row.getStatus())) {
            throw conflict("技能版本状态无效");
        }
        if (packages.changeState(ctx.tenantId(), id, row.getPackageHash(), row.getStatus(),
                SkillPackageStatus.RETIRED.getCode(), null, ctx.userId()) != 1) {
            throw conflict("版本状态已变化，请刷新");
        }
        row.setStatus(SkillPackageStatus.RETIRED.getCode());
        return summary(row);
    }

    public List<Summary> listAgentPackages(Long agentId) {
        var ctx = caller();
        var agent = agents.selectById(agentId);
        if (agent == null || !ctx.tenantId().equals(agent.getTenantId())) {
            throw denied();
        }
        return packages.selectBoundByAgent(ctx.tenantId(), agentId).stream()
                .map(this::summary)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Summary bindAgent(SkillPackageBindDTO dto) {
        var ctx = caller();
        var agent = agents.selectById(dto.agentId());
        if (agent == null || !ctx.tenantId().equals(agent.getTenantId())) {
            throw denied();
        }
        var skill = required(ctx.tenantId(), dto.versionId());
        if (!SkillPackageStatus.READY.matches(skill.getStatus())) {
            throw conflict("仅已启用（READY）状态的技能版本允许绑定至智能体");
        }
        snapshot(ctx.tenantId(), skill, true);
        packages.softDeleteAgentPackage(ctx.tenantId(), agent.getId(), skill.getPackageId(), ctx.userId());
        var binding = new AiAgentSkillPackage();
        binding.setId(IdWorker.getId());
        binding.setAgentId(agent.getId());
        binding.setPackageId(skill.getPackageId());
        binding.setVersionId(skill.getId());
        binding.setDelFlag(0L);
        audit(binding, ctx);
        agentSkillPackages.insert(binding);
        return summary(skill);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindAgent(Long agentId, Long versionId) {
        var ctx = caller();
        var agent = agents.selectById(agentId);
        if (agent == null || !ctx.tenantId().equals(agent.getTenantId())) {
            throw denied();
        }
        packages.softDeleteAgentVersion(ctx.tenantId(), agentId, versionId, ctx.userId());
    }

    /**
     * 运行时从指定智能体已绑定的 READY 技能版本（及其依赖闭包）读取只读资料文件。
     */
    public String readRuntimeFile(Long tenantId, Long agentId, String code, String path) {
        var boundVersions = packages.selectBoundByAgent(tenantId, agentId);
        for (var root : boundVersions) {
            if (!SkillPackageStatus.READY.matches(root.getStatus())) {
                continue;
            }
            Snapshot frozen = parseSnapshot(root);
            if (frozen == null) {
                continue;
            }
            var ref = frozen.references().stream()
                    .filter(r -> r.code().equals(code))
                    .findFirst()
                    .orElse(null);
            if (ref == null) {
                continue;
            }
            var target = required(tenantId, ref.versionId());
            if (!SkillPackageStatus.READY.matches(target.getStatus())
                    || !Objects.equals(target.getPackageId(), ref.packageId())
                    || !Objects.equals(target.getVersionName(), ref.version())
                    || !Objects.equals(target.getPackageHash(), ref.packageHash())) {
                throw conflict("技能固定版本已失效");
            }
            var content = content(target);
            return content.files().stream()
                    .filter(file -> file.path().equals(path))
                    .findFirst()
                    .map(SkillPackageArchive.FileContent::content)
                    .orElseThrow(() -> invalid("当前固定技能版本没有此文件"));
        }
        return null;
    }

    public boolean hasBoundPackages(Long tenantId, Long agentId) {
        return tenantId != null && agentId != null && packages.countBoundByAgent(tenantId, agentId) > 0;
    }

    private Snapshot parseSnapshot(SkillPackageVersion row) {
        if (row.getSnapshotJson() == null || row.getSnapshotJson().isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(row.getSnapshotJson(), Snapshot.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private Snapshot snapshot(Long tenant, SkillPackageVersion root, boolean rootReady) {
        var selected = new TreeMap<String, SkillPackageVersion>();
        var texts = new TreeMap<String, String>();
        closure(tenant, root, rootReady, new HashSet<>(), selected, texts, 0);
        StringBuilder instructions = new StringBuilder();
        var references = new ArrayList<Reference>();
        for (var entry : selected.entrySet()) {
            var row = entry.getValue();
            references.add(new Reference(row.getPackageId(), row.getId(), entry.getKey(),
                    row.getVersionName(), row.getPackageHash()));
            instructions.append("\n技能参考 ").append(entry.getKey()).append('@').append(row.getVersionName())
                    .append("\n").append(texts.get(entry.getKey())).append('\n');
            if (instructions.length() > 32000) {
                throw invalid("技能及依赖说明合计超过32000字符");
            }
        }
        var snapshot = new Snapshot(SkillPackageArchive.FORMAT, root.getId(), List.copyOf(references),
                instructions.toString(), SkillPackageArchive.sha256(instructions.toString()));
        if (rootReady) {
            try {
                if (root.getSnapshotJson() == null
                        || !mapper.readTree(root.getSnapshotJson()).equals(mapper.readTree(json(snapshot)))) {
                    throw conflict("已启用技能快照不一致");
                }
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw conflict("已启用技能快照不可读");
            }
        }
        return snapshot;
    }

    private void closure(Long tenant, SkillPackageVersion row, boolean ready, Set<Long> visiting,
                         SortedMap<String, SkillPackageVersion> selected, SortedMap<String, String> texts, int depth) {
        if (depth > 16 || !tenant.equals(row.getTenantId())
                || (ready && !SkillPackageStatus.READY.matches(row.getStatus()))) {
            throw conflict("技能依赖未启用、越界或层级超限");
        }
        if (!visiting.add(row.getId())) {
            throw conflict("技能依赖包含循环");
        }
        var content = content(row);
        var manifest = content.manifest();
        compatibility(manifest);
        var previous = selected.putIfAbsent(manifest.code(), row);
        if (previous != null) {
            if (!previous.getId().equals(row.getId())) {
                throw conflict("技能依赖存在同编码不同版本冲突");
            }
            visiting.remove(row.getId());
            return;
        }
        if (selected.size() > 32) {
            throw conflict("技能依赖超过32个版本");
        }
        texts.put(manifest.code(), content.instructions());
        for (var dependency : manifest.dependencies()) {
            var next = resolveDependency(tenant, dependency);
            closure(tenant, next, true, visiting, selected, texts, depth + 1);
        }
        visiting.remove(row.getId());
    }

    private SkillPackageVersion resolveDependency(Long tenant, SkillPackageArchive.Dependency dependency) {
        var next = packages.selectExact(tenant, dependency.code(), dependency.version());
        if (next == null || !dependency.sha256().equals(next.getPackageHash())) {
            throw conflict("依赖缺失或摘要不符：" + dependency.code() + "@" + dependency.version());
        }
        return next;
    }

    private void compatibility(SkillPackageArchive.Manifest manifest) {
        if (!SkillPackageArchive.CONTRACT.equals(manifest.contractVersion())) {
            throw conflict("技能契约版本不兼容");
        }
    }

    private List<String> issues(Long tenant, SkillPackageArchive.PackageContent content) {
        try {
            compatibility(content.manifest());
            var selected = new TreeMap<String, SkillPackageVersion>();
            var texts = new TreeMap<String, String>();
            for (var dependency : content.manifest().dependencies()) {
                closure(tenant, resolveDependency(tenant, dependency), true, new HashSet<>(), selected, texts, 1);
            }
            if (selected.containsKey(content.manifest().code()) || selected.size() >= 32) {
                throw conflict("技能依赖包含循环、编码冲突或超过32个版本");
            }
            int characters = content.instructions().length() + content.manifest().code().length()
                    + content.manifest().version().length() + 10;
            for (var entry : selected.entrySet()) {
                characters += texts.get(entry.getKey()).length() + entry.getKey().length()
                        + entry.getValue().getVersionName().length() + 10;
            }
            if (characters > 32000) {
                throw invalid("技能及依赖说明合计超过32000字符");
            }
            return List.of();
        } catch (BusinessException ex) {
            return List.of(ex.getMessage());
        }
    }

    private SkillPackageArchive.PackageContent content(SkillPackageVersion row) {
        var records = packages.selectFiles(row.getTenantId(), row.getId());
        if (records.stream().anyMatch(f -> !row.getTenantId().equals(f.getTenantId())
                || !row.getId().equals(f.getVersionId()))) {
            throw denied();
        }
        var result = archive.stored(row.getManifestJson(),
                records.stream()
                        .map(f -> new SkillPackageArchive.FileContent(
                                f.getFilePath(), f.getContent(), f.getContentHash(), f.getSizeBytes()))
                        .toList());
        if (!row.getPackageHash().equals(result.packageHash())
                || !row.getVersionName().equals(result.manifest().version())
                || (row.getPackageCode() != null && !row.getPackageCode().equals(result.manifest().code()))
                || !Objects.equals(row.getFileCount(), result.files().size())
                || !Objects.equals(row.getTotalBytes(), result.totalBytes())) {
            throw conflict("技能版本固定内容校验失败");
        }
        return result;
    }

    private SkillPackageVersion required(Long tenant, Long id) {
        var row = id == null ? null : packages.selectVersion(tenant, id);
        if (row == null || !tenant.equals(row.getTenantId())) {
            throw denied();
        }
        return row;
    }

    private Summary summary(SkillPackageVersion row) {
        var m = archive.manifest(row.getManifestJson());
        return new Summary(row.getId(), row.getPackageId(), m.code(), m.name(), m.description(),
                row.getVersionName(), row.getPackageHash(), row.getStatus(),
                row.getFileCount(), row.getTotalBytes(), row.getCreateTime());
    }

    private Summary same(SkillPackageVersion row, String hash) {
        if (!hash.equals(row.getPackageHash())) {
            throw conflict("相同技能版本已存在不同内容，请使用新的版本号");
        }
        return summary(row);
    }

    private void exactAction(SkillPackageVersion row, SkillPackageActionDTO dto) {
        parseStatus(dto.expectedStatus());
        if (!row.getPackageHash().equals(dto.packageHash()) || !row.getStatus().equals(dto.expectedStatus())) {
            throw conflict("技能版本摘要或状态已变化，请刷新");
        }
    }

    private SkillPackageStatus parseStatus(String value) {
        try {
            return SkillPackageStatus.valueOf(value);
        } catch (Exception ex) {
            throw invalid("技能版本状态无效");
        }
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw invalid("技能快照序列化失败");
        }
    }

    private Caller caller() {
        Long tenantId = SessionHelper.getTenantId();
        Long userId = SessionHelper.getUserId();
        if (tenantId == null || tenantId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(401, "请重新登录后操作技能资料");
        }
        return new Caller(tenantId, userId, SessionHelper.getActiveOrgId());
    }

    private void audit(TenantEntity row, Caller ctx) {
        row.setTenantId(ctx.tenantId());
        row.setCreateBy(ctx.userId());
        row.setUpdateBy(ctx.userId());
        row.setCreateDept(ctx.departmentId());
        row.setCreateTime(LocalDateTime.now());
        row.setUpdateTime(LocalDateTime.now());
    }

    private static BusinessException denied() {
        return new BusinessException(403, "技能或智能体不属于当前租户");
    }

    private static BusinessException invalid(String text) {
        return new BusinessException(400, text);
    }

    private static BusinessException conflict(String text) {
        return new BusinessException(409, text);
    }
}
