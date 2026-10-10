package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.ContactMemberQuery;
import com.mdframe.forge.plugin.system.service.ISysContactsService;
import com.mdframe.forge.plugin.system.vo.ContactMemberVO;
import com.mdframe.forge.plugin.system.vo.ContactOrgVO;
import com.mdframe.forge.plugin.system.vo.ContactSummaryVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通讯录（移动端）
 * <p>登录即可访问，范围固定为当前租户；不提供导出，避免批量拉取成员联系方式。</p>
 */
@RestController
@RequestMapping("/system/contacts")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysContactsController {

    private final ISysContactsService contactsService;

    @GetMapping("/summary")
    public RespInfo<ContactSummaryVO> summary() {
        return RespInfo.success(contactsService.getSummary());
    }

    @GetMapping("/orgs")
    public RespInfo<List<ContactOrgVO>> orgs(@RequestParam(required = false) Long parentId) {
        return RespInfo.success(contactsService.listChildOrgs(parentId));
    }

    @GetMapping("/members")
    public RespInfo<IPage<ContactMemberVO>> members(ContactMemberQuery query) {
        return RespInfo.success(contactsService.pageMembers(query));
    }

    @GetMapping("/members/{userId}")
    public RespInfo<ContactMemberVO> member(@PathVariable Long userId) {
        ContactMemberVO member = contactsService.getMember(userId);
        if (member == null) {
            return RespInfo.error("成员不存在或已停用");
        }
        return RespInfo.success(member);
    }
}
