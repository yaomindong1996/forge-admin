package com.mdframe.forge.plugin.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通讯录成员
 * <p>只包含展示字段；账号状态、证件号、密码等管理字段不得加入。</p>
 */
@Data
public class ContactMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String realName;

    /**
     * 头像文件 ID
     */
    private String avatar;

    /**
     * 同租户成员之间完整可见，禁止写入日志或导出
     */
    private String phone;

    private String email;

    private List<String> orgNames = new ArrayList<>();

    private List<String> postNames = new ArrayList<>();
}
