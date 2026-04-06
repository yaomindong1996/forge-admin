package com.mdframe.forge.starter.idempotent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfoDTO {
    
    private String token;
    
    private int expireSeconds;
    
    private long createTime;
}