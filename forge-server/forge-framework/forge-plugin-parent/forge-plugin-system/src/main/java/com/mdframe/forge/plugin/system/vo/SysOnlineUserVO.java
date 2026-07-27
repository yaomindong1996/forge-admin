package com.mdframe.forge.plugin.system.vo;

import com.mdframe.forge.plugin.system.entity.SysOnlineUser;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class SysOnlineUserVO {

    private Long sessionId;
    private Long userId;
    private String username;
    private String realName;
    private String deptName;
    private String ipAddress;
    private String loginLocation;
    private String browser;
    private String os;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;
    private LocalDateTime expireTime;
    private Integer status;
    private LocalDateTime logoutTime;
    private Integer logoutType;
    private Boolean banned;

    public static SysOnlineUserVO from(SysOnlineUser onlineUser) {
        SysOnlineUserVO vo = new SysOnlineUserVO();
        BeanUtils.copyProperties(onlineUser, vo);
        vo.setSessionId(onlineUser.getId());
        return vo;
    }
}
