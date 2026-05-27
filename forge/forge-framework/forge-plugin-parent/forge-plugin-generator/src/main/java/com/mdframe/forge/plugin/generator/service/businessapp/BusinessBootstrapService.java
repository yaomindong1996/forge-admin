package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessSuite;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeDomainMapper;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessSuiteMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * 低代码历史数据到业务应用平台的幂等映射服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessBootstrapService {

    private static final String DEFAULT_SUITE_CODE = "general";

    private final AiLowcodeDomainMapper lowcodeDomainMapper;
    private final AiLowcodeModelMapper lowcodeModelMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessSuiteMapper businessSuiteMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessAppMapper businessAppMapper;

    @Transactional(rollbackFor = Exception.class)
    public void syncSuitesFromLowcodeDomains() {
        Long tenantId = resolveTenantId();
        List<AiLowcodeDomain> domains = lowcodeDomainMapper.selectDomainList(tenantId, null, null);
        for (AiLowcodeDomain domain : domains) {
            if (domain == null || StringUtils.isBlank(domain.getDomainCode())) {
                continue;
            }
            if (businessSuiteMapper.selectBySuiteCode(tenantId, domain.getDomainCode()) != null) {
                continue;
            }
            AiBusinessSuite suite = new AiBusinessSuite();
            suite.setTenantId(tenantId);
            suite.setSuiteCode(domain.getDomainCode());
            suite.setSuiteName(StringUtils.defaultIfBlank(domain.getDomainName(), domain.getDomainCode()));
            suite.setIcon(domain.getIcon());
            suite.setDescription(domain.getDomainDesc());
            suite.setStatus("DISABLED".equals(domain.getStatus()) ? 0 : 1);
            suite.setSortOrder(domain.getSort() == null ? 0 : domain.getSort());
            suite.setOptions("{\"source\":\"ai_lowcode_domain\"}");
            businessSuiteMapper.insert(suite);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncObjectsFromLowcodeModels() {
        syncSuitesFromLowcodeDomains();
        Long tenantId = resolveTenantId();
        List<AiLowcodeModel> models = lowcodeModelMapper.selectModelList(tenantId, null, null, null);
        for (AiLowcodeModel model : models) {
            if (model == null || StringUtils.isBlank(model.getModelCode())) {
                continue;
            }
            String suiteCode = StringUtils.defaultIfBlank(model.getDomainCode(), DEFAULT_SUITE_CODE);
            ensureSuite(tenantId, suiteCode);
            if (businessObjectMapper.selectByObjectCode(tenantId, suiteCode, model.getModelCode()) != null) {
                continue;
            }
            AiBusinessObject object = new AiBusinessObject();
            object.setTenantId(tenantId);
            object.setSuiteCode(suiteCode);
            object.setObjectCode(model.getModelCode());
            object.setObjectName(StringUtils.defaultIfBlank(model.getModelName(), model.getModelCode()));
            object.setObjectType(Boolean.TRUE.equals(model.getMasterData()) ? "MASTER" : "TRANSACTION");
            object.setModelId(model.getId());
            object.setModelCode(model.getModelCode());
            object.setDescription(model.getModelDesc());
            object.setStatus("DISABLED".equals(model.getStatus()) ? 0 : 1);
            object.setSortOrder(0);
            object.setOptions("{\"source\":\"ai_lowcode_model\"}");
            businessObjectMapper.insert(object);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncAppsFromPublishedCrudConfigs() {
        syncObjectsFromLowcodeModels();
        Long tenantId = resolveTenantId();
        List<AiCrudConfig> configs = crudConfigMapper.selectPublishedLowcodeConfigs(tenantId);
        for (AiCrudConfig config : configs) {
            if (config == null || StringUtils.isBlank(config.getConfigKey())) {
                continue;
            }
            if (businessAppMapper.selectByConfigKey(tenantId, config.getConfigKey()) != null) {
                continue;
            }
            String suiteCode = StringUtils.defaultIfBlank(config.getDomainCode(), DEFAULT_SUITE_CODE);
            String objectCode = StringUtils.defaultIfBlank(config.getObjectCode(), config.getConfigKey());
            ensureSuite(tenantId, suiteCode);
            ensureObject(tenantId, suiteCode, objectCode, config);

            AiBusinessApp app = new AiBusinessApp();
            app.setTenantId(tenantId);
            app.setAppCode(toAppCode(config.getConfigKey()));
            app.setAppName(resolveAppName(config));
            app.setAppType("BUSINESS");
            app.setSuiteCode(suiteCode);
            app.setObjectCode(objectCode);
            app.setEntryMode("RUNTIME");
            app.setEntryUrl("/ai/crud-page/" + config.getConfigKey());
            app.setConfigKey(config.getConfigKey());
            app.setIcon("ionicons5:AppsOutline");
            app.setDescription(StringUtils.defaultIfBlank(config.getTableComment(), "低代码已发布应用入口"));
            app.setStatus("STOPPED".equals(config.getPublishStatus()) ? 0 : 1);
            app.setSortOrder(config.getMenuSort() == null ? 0 : config.getMenuSort());
            app.setOptions("{\"source\":\"ai_crud_config\"}");
            businessAppMapper.insert(app);
        }
    }

    private void ensureSuite(Long tenantId, String suiteCode) {
        if (businessSuiteMapper.selectBySuiteCode(tenantId, suiteCode) != null) {
            return;
        }
        AiBusinessSuite suite = new AiBusinessSuite();
        suite.setTenantId(tenantId);
        suite.setSuiteCode(suiteCode);
        suite.setSuiteName(suiteCode);
        suite.setDescription("低代码历史数据自动生成的业务套件");
        suite.setStatus(1);
        suite.setSortOrder(99);
        suite.setOptions("{\"source\":\"lowcode_compat\"}");
        businessSuiteMapper.insert(suite);
    }

    private void ensureObject(Long tenantId, String suiteCode, String objectCode, AiCrudConfig config) {
        if (businessObjectMapper.selectByObjectCode(tenantId, suiteCode, objectCode) != null) {
            return;
        }
        AiBusinessObject object = new AiBusinessObject();
        object.setTenantId(tenantId);
        object.setSuiteCode(suiteCode);
        object.setObjectCode(objectCode);
        object.setObjectName(StringUtils.defaultIfBlank(config.getObjectName(), resolveAppName(config)));
        object.setObjectType("TRANSACTION");
        object.setModelCode(config.getObjectCode());
        object.setDescription(config.getTableComment());
        object.setStatus(1);
        object.setSortOrder(0);
        object.setOptions("{\"source\":\"ai_crud_config\"}");
        businessObjectMapper.insert(object);
    }

    private String resolveAppName(AiCrudConfig config) {
        return StringUtils.defaultIfBlank(
                config.getAppName(),
                StringUtils.defaultIfBlank(config.getObjectName(),
                        StringUtils.defaultIfBlank(config.getTableComment(), config.getConfigKey()))
        );
    }

    private String toAppCode(String configKey) {
        String normalized = configKey.replaceAll("[^A-Za-z0-9_]", "_").toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("LC_")) {
            normalized = "LC_" + normalized;
        }
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
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
