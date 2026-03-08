package com.mdframe.forge.starter.id.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.id.service.IIdService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/id")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.id", name = "enable-api", havingValue = "true", matchIfMissing = true)
public class IdController {
    
    private final IIdService idService;
    
    /**
     * 单个ID
     */
    @GetMapping("/next")
    public RespInfo<Long> next() {
        return RespInfo.success(idService.nextId());
    }
}
