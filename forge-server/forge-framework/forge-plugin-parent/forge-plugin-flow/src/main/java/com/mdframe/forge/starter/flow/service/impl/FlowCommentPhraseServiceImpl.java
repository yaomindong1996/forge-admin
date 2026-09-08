package com.mdframe.forge.starter.flow.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.FlowCommentPhraseCreateDTO;
import com.mdframe.forge.starter.flow.dto.FlowCommentPhraseQuery;
import com.mdframe.forge.starter.flow.dto.FlowCommentPhraseUpdateDTO;
import com.mdframe.forge.starter.flow.entity.FlowCommentPhrase;
import com.mdframe.forge.starter.flow.enums.FlowCommentPhraseOwnerType;
import com.mdframe.forge.starter.flow.enums.FlowCommentPhraseScene;
import com.mdframe.forge.starter.flow.mapper.FlowCommentPhraseMapper;
import com.mdframe.forge.starter.flow.service.FlowCommentPhraseService;
import com.mdframe.forge.starter.flow.vo.FlowCommentPhraseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/** 常用审批意见：企业意见按租户共享，个人意见仅当前用户可见。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowCommentPhraseServiceImpl implements FlowCommentPhraseService {

    private static final long TENANT_OWNER_USER_ID = 0L;
    private static final int MAX_CONTENT_LENGTH = 200;
    private static final int MAX_PERSONAL = 30;
    private static final int MAX_TENANT = 100;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String MANAGE_PERMISSION = "flow:comment-phrase:manage";
    private static final String VIEW_PERMISSION = "flow:comment-phrase:view";
    private static final String SUPER_PERMISSION = "*:*:*";

    private final FlowCommentPhraseMapper phraseMapper;

    @Override
    public List<FlowCommentPhraseVO> listUsable(String scene) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        String safeScene = normalizeSceneFilter(scene);
        List<FlowCommentPhraseVO> phrases = phraseMapper.selectUsable(tenantId, userId, safeScene);
        return phrases == null ? Collections.emptyList() : phrases;
    }

    @Override
    public List<FlowCommentPhraseVO> listMine() {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        List<FlowCommentPhraseVO> phrases = phraseMapper.selectMine(tenantId, userId);
        return phrases == null ? Collections.emptyList() : phrases;
    }

    @Override
    public IPage<FlowCommentPhraseVO> page(FlowCommentPhraseQuery query) {
        FlowCommentPhraseQuery safeQuery = query == null ? new FlowCommentPhraseQuery() : query;
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        int ownerType = resolvePageOwnerType(safeQuery.getOwnerType());
        Long ownerUserId = FlowCommentPhraseOwnerType.TENANT.matches(ownerType) ? TENANT_OWNER_USER_ID : userId;
        if (FlowCommentPhraseOwnerType.TENANT.matches(ownerType)) {
            requireViewPermission();
        }
        Page<FlowCommentPhraseVO> page = new Page<>(safeQuery.getPageNum(),
                Math.min(safeQuery.getPageSize(), MAX_PAGE_SIZE));
        return phraseMapper.selectPageByOwner(page, tenantId, ownerType, ownerUserId,
                trimToNull(safeQuery.getKeyword()), normalizeSceneFilter(safeQuery.getScene()),
                safeQuery.getStatus());
    }

    @Override
    public FlowCommentPhraseVO getById(Long id) {
        validateId(id);
        FlowCommentPhrase existing = loadVisible(id);
        FlowCommentPhraseVO vo = phraseMapper.selectVoByIdAndTenant(existing.getId(), existing.getTenantId());
        if (vo == null) {
            throw notFound();
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowCommentPhraseVO create(FlowCommentPhraseCreateDTO request) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        validateCreate(request);
        int ownerType = request.getOwnerType() == null
                ? FlowCommentPhraseOwnerType.USER.getCode()
                : request.getOwnerType();
        Long ownerUserId = resolveOwnerUserId(ownerType, userId);
        if (FlowCommentPhraseOwnerType.TENANT.matches(ownerType)) {
            requireManagePermission();
        }
        String content = request.getContent().trim();
        String scene = request.getScene().trim();
        int maxSize = FlowCommentPhraseOwnerType.TENANT.matches(ownerType) ? MAX_TENANT : MAX_PERSONAL;
        if (phraseMapper.countByOwner(tenantId, ownerType, ownerUserId) >= maxSize) {
            throw new BusinessException(400, FlowCommentPhraseOwnerType.TENANT.matches(ownerType)
                    ? "企业常用意见最多100条"
                    : "个人常用意见最多30条");
        }
        if (phraseMapper.countByContent(tenantId, ownerType, ownerUserId, scene, content, null) > 0) {
            throw new BusinessException(409, "相同场景下已存在该意见");
        }
        FlowCommentPhrase phrase = new FlowCommentPhrase();
        phrase.setTenantId(tenantId);
        phrase.setOwnerType(ownerType);
        phrase.setUserId(ownerUserId);
        phrase.setScene(scene);
        phrase.setContent(content);
        phrase.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        phrase.setStatus(request.getStatus() == null ? EnableStatus.ENABLED.getCode() : request.getStatus());
        phrase.setDelFlag(0L);
        phrase.setCreateBy(userId);
        phrase.setCreateDept(SessionHelper.getActiveOrgId());
        phrase.setCreateTime(LocalDateTime.now());
        phrase.setUpdateBy(userId);
        phrase.setUpdateTime(LocalDateTime.now());
        phraseMapper.insert(phrase);
        log.info("创建常用审批意见: tenantId={}, ownerType={}, phraseId={}", tenantId, ownerType, phrase.getId());
        return getById(phrase.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowCommentPhraseVO update(FlowCommentPhraseUpdateDTO request) {
        validateUpdate(request);
        FlowCommentPhrase existing = loadWritable(request.getId());
        String content = request.getContent().trim();
        String scene = request.getScene().trim();
        if (phraseMapper.countByContent(existing.getTenantId(), existing.getOwnerType(), existing.getUserId(),
                scene, content, existing.getId()) > 0) {
            throw new BusinessException(409, "相同场景下已存在该意见");
        }
        FlowCommentPhrase patch = new FlowCommentPhrase();
        patch.setId(existing.getId());
        patch.setContent(content);
        patch.setScene(scene);
        patch.setSortOrder(request.getSortOrder() == null ? existing.getSortOrder() : request.getSortOrder());
        patch.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        patch.setUpdateBy(requireUserId());
        patch.setUpdateTime(LocalDateTime.now());
        if (phraseMapper.updateByIdAndTenant(patch, existing.getTenantId()) == 0) {
            throw notFound();
        }
        return getById(existing.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FlowCommentPhrase existing = loadWritable(id);
        Long operatorId = requireUserId();
        if (phraseMapper.logicallyDeleteByIdAndTenant(existing.getId(), existing.getTenantId(), operatorId) == 0) {
            throw notFound();
        }
        log.info("删除常用审批意见: tenantId={}, phraseId={}, operatorId={}",
                existing.getTenantId(), existing.getId(), operatorId);
    }

    private FlowCommentPhrase loadVisible(Long id) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        FlowCommentPhrase existing = phraseMapper.selectByIdAndTenant(id, tenantId);
        if (existing == null) {
            throw notFound();
        }
        if (FlowCommentPhraseOwnerType.TENANT.matches(existing.getOwnerType())) {
            if (!canViewTenantPhrases()) {
                throw notFound();
            }
            return existing;
        }
        if (!FlowCommentPhraseOwnerType.USER.matches(existing.getOwnerType())
                || existing.getUserId() == null
                || !existing.getUserId().equals(userId)) {
            throw notFound();
        }
        return existing;
    }

    private FlowCommentPhrase loadWritable(Long id) {
        validateId(id);
        FlowCommentPhrase existing = loadVisible(id);
        if (FlowCommentPhraseOwnerType.TENANT.matches(existing.getOwnerType())) {
            requireManagePermission();
        }
        return existing;
    }

    private void validateCreate(FlowCommentPhraseCreateDTO request) {
        if (request == null) {
            throw new BusinessException(400, "请求不能为空");
        }
        validateContent(request.getContent());
        validateScene(request.getScene());
        if (request.getOwnerType() != null && !FlowCommentPhraseOwnerType.isValid(request.getOwnerType())) {
            throw new BusinessException(400, "意见归属不合法");
        }
        validateStatus(request.getStatus());
    }

    private void validateUpdate(FlowCommentPhraseUpdateDTO request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(400, "意见ID不合法");
        }
        validateContent(request.getContent());
        validateScene(request.getScene());
        validateStatus(request.getStatus());
    }

    private void validateContent(String content) {
        if (!StringUtils.hasText(content) || content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(400, "审批意见不能为空且长度不能超过200");
        }
    }

    private void validateScene(String scene) {
        if (!FlowCommentPhraseScene.isValid(scene)) {
            throw new BusinessException(400, "适用场景不合法");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && !EnableStatus.ENABLED.matches(status) && !EnableStatus.DISABLED.matches(status)) {
            throw new BusinessException(400, "意见状态不合法");
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "意见ID不合法");
        }
    }

    private int resolvePageOwnerType(Integer ownerType) {
        if (ownerType == null) {
            return canViewTenantPhrases()
                    ? FlowCommentPhraseOwnerType.TENANT.getCode()
                    : FlowCommentPhraseOwnerType.USER.getCode();
        }
        if (!FlowCommentPhraseOwnerType.isValid(ownerType)) {
            throw new BusinessException(400, "意见归属不合法");
        }
        return ownerType;
    }

    private Long resolveOwnerUserId(int ownerType, Long userId) {
        if (FlowCommentPhraseOwnerType.TENANT.matches(ownerType)) {
            return TENANT_OWNER_USER_ID;
        }
        return userId;
    }

    private String normalizeSceneFilter(String scene) {
        String trimmed = trimToNull(scene);
        if (trimmed == null) {
            return null;
        }
        if (!FlowCommentPhraseScene.isValid(trimmed)) {
            throw new BusinessException(400, "适用场景不合法");
        }
        return trimmed;
    }

    private void requireViewPermission() {
        if (!canViewTenantPhrases()) {
            throw new BusinessException(403, "没有权限查看企业常用意见");
        }
    }

    private void requireManagePermission() {
        if (!canManageTenantPhrases()) {
            throw new BusinessException(403, "没有权限维护企业常用意见");
        }
    }

    private boolean canViewTenantPhrases() {
        return hasPermission(VIEW_PERMISSION) || canManageTenantPhrases();
    }

    private boolean canManageTenantPhrases() {
        return hasPermission(MANAGE_PERMISSION) || hasPermission(SUPER_PERMISSION);
    }

    private boolean hasPermission(String permission) {
        try {
            return StpUtil.hasPermission(permission);
        } catch (Exception ex) {
            log.debug("检查常用审批意见权限失败: permission={}", permission, ex);
            return false;
        }
    }

    private Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException(403, "无法确定当前租户，禁止管理常用审批意见");
        }
        return tenantId;
    }

    private Long requireUserId() {
        Long userId = SessionHelper.getUserId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(403, "无法确定当前用户，禁止管理常用审批意见");
        }
        return userId;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BusinessException notFound() {
        return new BusinessException(404, "常用审批意见不存在");
    }
}
