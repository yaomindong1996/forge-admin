package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessSuite;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessSuiteDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessSuiteQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessSuiteMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessSuiteSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessSuiteVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 业务应用平台业务套件服务。
 */
@Service
public class BusinessSuiteService extends ServiceImpl<BusinessSuiteMapper, AiBusinessSuite> {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");

    public Page<BusinessSuiteVO> page(Integer pageNum, Integer pageSize, BusinessSuiteQueryDTO query) {
        Page<BusinessSuiteVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectSuitePage(page, resolveTenantId(), normalizeQuery(query));
    }

    public List<BusinessSuiteVO> list(BusinessSuiteQueryDTO query) {
        return baseMapper.selectSuiteList(resolveTenantId(), normalizeQuery(query));
    }

    public BusinessSuiteVO detail(Long id) {
        BusinessSuiteVO vo = baseMapper.selectSuiteDetail(resolveTenantId(), id);
        if (vo == null) {
            throw new BusinessException("业务套件不存在");
        }
        return vo;
    }

    public List<BusinessSuiteSummaryVO> summary() {
        return baseMapper.selectSuiteSummary(resolveTenantId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BusinessSuiteDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务套件不能为空");
        }
        AiBusinessSuite suite = new AiBusinessSuite();
        copyDtoToEntity(dto, suite, true);
        save(suite);
        return suite.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(BusinessSuiteDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("业务套件ID不能为空");
        }
        AiBusinessSuite suite = requireEntity(dto.getId());
        copyDtoToEntity(dto, suite, false);
        updateById(suite);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiBusinessSuite suite = requireEntity(id);
        Long tenantId = resolveTenantId();
        if (baseMapper.countObjectsBySuite(tenantId, suite.getSuiteCode()) > 0) {
            throw new BusinessException("该业务套件已存在业务对象，不能删除");
        }
        if (baseMapper.countAppsBySuite(tenantId, suite.getSuiteCode()) > 0) {
            throw new BusinessException("该业务套件已存在应用入口，不能删除");
        }
        removeById(suite.getId());
    }

    public AiBusinessSuite requireByCode(String suiteCode) {
        String code = StringUtils.trimToNull(suiteCode);
        if (StringUtils.isBlank(code)) {
            throw new BusinessException("业务套件编码不能为空");
        }
        AiBusinessSuite suite = baseMapper.selectBySuiteCode(resolveTenantId(), code);
        if (suite == null) {
            throw new BusinessException("业务套件不存在: " + code);
        }
        return suite;
    }

    public AiBusinessSuite requireEntity(Long id) {
        if (id == null) {
            throw new BusinessException("业务套件ID不能为空");
        }
        AiBusinessSuite suite = getById(id);
        if (suite == null) {
            throw new BusinessException("业务套件不存在");
        }
        return suite;
    }

    private void copyDtoToEntity(BusinessSuiteDTO dto, AiBusinessSuite suite, boolean create) {
        String suiteCode = StringUtils.trimToNull(dto.getSuiteCode());
        String suiteName = StringUtils.trimToNull(dto.getSuiteName());
        if (StringUtils.isBlank(suiteCode) || !CODE_PATTERN.matcher(suiteCode).matches()) {
            throw new BusinessException("套件编码格式不正确（字母开头，仅含字母、数字和下划线，2-64字符）");
        }
        if (StringUtils.isBlank(suiteName)) {
            throw new BusinessException("套件名称不能为空");
        }
        Long excludeId = create ? null : suite.getId();
        if (baseMapper.countBySuiteCode(resolveTenantId(), suiteCode, excludeId) > 0) {
            throw new BusinessException("套件编码已存在: " + suiteCode);
        }
        suite.setTenantId(resolveTenantId());
        suite.setSuiteCode(suiteCode);
        suite.setSuiteName(suiteName);
        suite.setIcon(StringUtils.trimToNull(dto.getIcon()));
        suite.setDescription(StringUtils.trimToNull(dto.getDescription()));
        suite.setStatus(normalizeStatus(dto.getStatus()));
        suite.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        suite.setOptions(StringUtils.trimToNull(dto.getOptions()));
    }

    private BusinessSuiteQueryDTO normalizeQuery(BusinessSuiteQueryDTO query) {
        BusinessSuiteQueryDTO result = query == null ? new BusinessSuiteQueryDTO() : query;
        result.setKeyword(StringUtils.trimToNull(result.getKeyword()));
        result.setSuiteCode(StringUtils.trimToNull(result.getSuiteCode()));
        return result;
    }

    private Integer normalizeStatus(Integer status) {
        int value = status == null ? 1 : status;
        if (value != 0 && value != 1) {
            throw new BusinessException("状态值不正确");
        }
        return value;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
