package com.mdframe.forge.starter.social.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSocialBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    private String platform;

    private String platformName;

    private String platformLogo;

    private Boolean bound;

    private String nickname;

    private String email;

    private String avatar;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bindTime;
}
