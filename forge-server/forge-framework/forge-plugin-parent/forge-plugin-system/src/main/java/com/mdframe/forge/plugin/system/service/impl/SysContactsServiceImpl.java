package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.ContactMemberQuery;
import com.mdframe.forge.plugin.system.mapper.SysContactsMapper;
import com.mdframe.forge.plugin.system.service.ISysContactsService;
import com.mdframe.forge.plugin.system.vo.ContactMemberLabelVO;
import com.mdframe.forge.plugin.system.vo.ContactMemberVO;
import com.mdframe.forge.plugin.system.vo.ContactOrgVO;
import com.mdframe.forge.plugin.system.vo.ContactSummaryVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysContactsServiceImpl implements ISysContactsService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_KEYWORD_LENGTH = 50;

    private final SysContactsMapper contactsMapper;

    @Override
    public ContactSummaryVO getSummary() {
        Long tenantId = requireTenantId();
        ContactSummaryVO summary = new ContactSummaryVO();
        summary.setTenantName(contactsMapper.selectTenantName(tenantId));
        summary.setMemberCount(contactsMapper.countMembers(tenantId));
        ContactOrgVO mainOrg = contactsMapper.selectMainOrg(tenantId, SessionHelper.getUserId());
        if (mainOrg != null) {
            summary.setMyOrgId(mainOrg.getId());
            summary.setMyOrgName(mainOrg.getOrgName());
        }
        return summary;
    }

    @Override
    public List<ContactOrgVO> listChildOrgs(Long parentId) {
        return contactsMapper.selectChildOrgs(requireTenantId(), parentId);
    }

    @Override
    public IPage<ContactMemberVO> pageMembers(ContactMemberQuery query) {
        Long tenantId = requireTenantId();
        ContactMemberQuery normalized = normalizeQuery(query);
        Page<ContactMemberVO> page = new Page<>(normalized.getPageNum(), normalized.getPageSize());
        IPage<ContactMemberVO> result = contactsMapper.selectMemberPage(page, tenantId, normalized);
        fillLabels(tenantId, result.getRecords());
        return result;
    }

    @Override
    public ContactMemberVO getMember(Long userId) {
        if (userId == null) {
            return null;
        }
        Long tenantId = requireTenantId();
        ContactMemberVO member = contactsMapper.selectMember(tenantId, userId);
        if (member != null) {
            fillLabels(tenantId, List.of(member));
        }
        return member;
    }

    static ContactMemberQuery normalizeQuery(ContactMemberQuery query) {
        ContactMemberQuery source = query == null ? new ContactMemberQuery() : query;
        ContactMemberQuery normalized = new ContactMemberQuery();
        normalized.setOrgId(source.getOrgId());
        String keyword = StringUtils.hasText(source.getKeyword()) ? source.getKeyword().trim() : null;
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        normalized.setKeyword(keyword);
        Integer pageNum = source.getPageNum();
        normalized.setPageNum(pageNum == null || pageNum < 1 ? 1 : pageNum);
        Integer pageSize = source.getPageSize();
        int size = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
        normalized.setPageSize(Math.min(size, MAX_PAGE_SIZE));
        return normalized;
    }

    private void fillLabels(Long tenantId, List<ContactMemberVO> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        List<Long> userIds = members.stream().map(ContactMemberVO::getUserId).toList();
        Map<Long, List<String>> orgNames = groupLabels(contactsMapper.selectOrgNames(tenantId, userIds));
        Map<Long, List<String>> postNames = groupLabels(contactsMapper.selectPostNames(tenantId, userIds));
        assign(members, orgNames, ContactMemberVO::setOrgNames);
        assign(members, postNames, ContactMemberVO::setPostNames);
    }

    private static Map<Long, List<String>> groupLabels(List<ContactMemberLabelVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return rows.stream()
                .filter(row -> row.getUserId() != null && StringUtils.hasText(row.getLabel()))
                .collect(Collectors.groupingBy(ContactMemberLabelVO::getUserId,
                        Collectors.mapping(ContactMemberLabelVO::getLabel,
                                Collectors.collectingAndThen(Collectors.toList(),
                                        list -> list.stream().distinct().toList()))));
    }

    private static void assign(List<ContactMemberVO> members, Map<Long, List<String>> labels,
                               BiConsumer<ContactMemberVO, List<String>> setter) {
        for (ContactMemberVO member : members) {
            setter.accept(member, labels.getOrDefault(member.getUserId(), List.of()));
        }
    }

    private static Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("当前登录状态缺少租户信息");
        }
        return tenantId;
    }
}
