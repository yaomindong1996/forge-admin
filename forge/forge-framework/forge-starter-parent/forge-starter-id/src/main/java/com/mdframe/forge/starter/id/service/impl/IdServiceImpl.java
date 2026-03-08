package com.mdframe.forge.starter.id.service.impl;

import com.mdframe.forge.starter.id.entity.SysIdSequence;
import com.mdframe.forge.starter.id.mapper.SysIdSequenceMapper;
import com.mdframe.forge.starter.id.service.IIdService;
import com.xfvape.uid.UidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdServiceImpl implements IIdService {
    
    @Autowired
    @Qualifier("cachedUidGenerator")
    private UidGenerator uidGenerator;
    
    @Override
    public long nextId() {
        return uidGenerator.getUID();
    }
}
