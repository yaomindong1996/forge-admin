package com.mdframe.forge.starter.idempotent.controller;

import com.mdframe.forge.starter.idempotent.dto.TokenInfoDTO;
import com.mdframe.forge.starter.idempotent.service.TokenService;
import com.mdframe.forge.framework.core.domain.model.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/idempotent/token")
@RequiredArgsConstructor
public class IdempotentTokenController {
    
    private final TokenService tokenService;
    private final com.mdframe.forge.starter.idempotent.properties.TokenProperties tokenProperties;
    
    @PostMapping("/generate")
    public RespInfo<TokenInfoDTO> generateToken(@RequestParam(required = false) String prefix) {
        String token = tokenService.generateToken(prefix);
        int expire = tokenProperties.getExpire();
        
        TokenInfoDTO tokenInfo = new TokenInfoDTO(token, expire, System.currentTimeMillis());
        return RespInfo.success(tokenInfo);
    }
    
    @PostMapping("/batch-generate")
    public RespInfo<List<TokenInfoDTO>> batchGenerateToken(
            @RequestParam int count,
            @RequestParam(required = false) String prefix) {
        
        if (count <= 0 || count > 100) {
            return RespInfo.error("count参数必须在1-100之间");
        }
        
        List<TokenInfoDTO> tokens = new ArrayList<>();
        int expire = tokenProperties.getExpire();
        long createTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            String token = tokenService.generateToken(prefix);
            tokens.add(new TokenInfoDTO(token, expire, createTime));
        }
        
        return RespInfo.success(tokens);
    }
    
    @PostMapping("/validate")
    public RespInfo<Boolean> validateToken(@RequestParam String token, @RequestParam(required = false) String prefix) {
        boolean valid = tokenService.validateToken(token, prefix);
        return RespInfo.success(valid);
    }
}