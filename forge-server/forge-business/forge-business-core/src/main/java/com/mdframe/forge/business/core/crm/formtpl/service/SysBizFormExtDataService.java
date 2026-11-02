package com.mdframe.forge.business.core.crm.formtpl.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.mdframe.forge.business.core.crm.formtpl.BizFormObjectTypes;
import com.mdframe.forge.business.core.crm.formtpl.domain.SysBizFormExtData;
import com.mdframe.forge.business.core.crm.formtpl.dto.SysBizFormExtSaveDTO;
import com.mdframe.forge.business.core.crm.formtpl.mapper.SysBizFormExtDataMapper;
import com.mdframe.forge.business.core.crm.formtpl.vo.SysBizFormExtDataVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysBizFormExtDataService {
    private final SysBizFormExtDataMapper mapper;
    private final IdentifierGenerator identifierGenerator;

    public SysBizFormExtDataVO get(String objectType, Long bizId) {
        BizFormObjectTypes.Def def = BizFormObjectTypes.require(objectType);
        if (def.storage() != BizFormObjectTypes.ExtStorage.SIDE_TABLE) {
            throw new BusinessException("该对象扩展值不走侧表存储");
        }
        if (bizId == null) {
            throw new BusinessException("业务主键不能为空");
        }
        SysBizFormExtData row = mapper.selectByObjectAndBizId(tenant(), def.objectType(), bizId);
        SysBizFormExtDataVO vo = new SysBizFormExtDataVO();
        vo.setObjectType(def.objectType());
        vo.setBizId(bizId);
        vo.setExtJson(row == null ? "{}" : (StringUtils.hasText(row.getExtJson()) ? row.getExtJson() : "{}"));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysBizFormExtDataVO save(SysBizFormExtSaveDTO dto) {
        BizFormObjectTypes.Def def = BizFormObjectTypes.require(dto.getObjectType());
        if (def.storage() != BizFormObjectTypes.ExtStorage.SIDE_TABLE) {
            throw new BusinessException("该对象扩展值不走侧表存储");
        }
        String json = StringUtils.hasText(dto.getExtJson()) ? dto.getExtJson().trim() : "{}";
        SysBizFormExtData existing = mapper.selectByObjectAndBizId(tenant(), def.objectType(), dto.getBizId());
        if (existing == null) {
            SysBizFormExtData created = new SysBizFormExtData();
            created.setId(identifierGenerator.nextId(created).longValue());
            created.setTenantId(tenant());
            created.setObjectType(def.objectType());
            created.setBizId(dto.getBizId());
            created.setExtJson(json);
            created.setDelFlag(0L);
            mapper.insert(created);
        }
        else {
            existing.setExtJson(json);
            mapper.updateById(existing);
        }
        return get(def.objectType(), dto.getBizId());
    }

    private Long tenant() {
        Long id = SessionHelper.getTenantId();
        if (id == null) {
            throw new BusinessException("租户未核实");
        }
        return id;
    }
}
