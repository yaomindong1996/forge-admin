package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.ContactMemberQuery;
import com.mdframe.forge.plugin.system.vo.ContactMemberVO;
import com.mdframe.forge.plugin.system.vo.ContactOrgVO;
import com.mdframe.forge.plugin.system.vo.ContactSummaryVO;

import java.util.List;

/**
 * 通讯录服务，范围固定为当前登录租户
 */
public interface ISysContactsService {

    ContactSummaryVO getSummary();

    List<ContactOrgVO> listChildOrgs(Long parentId);

    IPage<ContactMemberVO> pageMembers(ContactMemberQuery query);

    /**
     * 成员不存在、已停用或不在当前租户时返回 null
     */
    ContactMemberVO getMember(Long userId);
}
