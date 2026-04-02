package com.mdframe.forge.starter.message.config;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.aliyun.config.AlibabaConfig;
import org.dromara.sms4j.baidu.config.BaiduConfig;
import org.dromara.sms4j.budingyun.config.BudingV2Config;
import org.dromara.sms4j.chuanglan.config.ChuangLanConfig;
import org.dromara.sms4j.cloopen.config.CloopenConfig;
import org.dromara.sms4j.core.datainterface.SmsReadConfig;
import org.dromara.sms4j.ctyun.config.CtyunConfig;
import org.dromara.sms4j.danmi.config.DanMiConfig;
import org.dromara.sms4j.dingzhong.config.DingZhongConfig;
import org.dromara.sms4j.emay.config.EmayConfig;
import org.dromara.sms4j.huawei.config.HuaweiConfig;
import org.dromara.sms4j.jdcloud.config.JdCloudConfig;
import org.dromara.sms4j.jg.config.JgConfig;
import org.dromara.sms4j.lianlu.config.LianLuConfig;
import org.dromara.sms4j.luosimao.config.LuoSiMaoConfig;
import org.dromara.sms4j.mas.config.MasConfig;
import org.dromara.sms4j.netease.config.NeteaseConfig;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.qiniu.config.QiNiuConfig;
import org.dromara.sms4j.submail.config.SubMailConfig;
import org.dromara.sms4j.tencent.config.TencentConfig;
import org.dromara.sms4j.unisms.config.UniConfig;
import org.dromara.sms4j.yunpian.config.YunpianConfig;
import org.dromara.sms4j.zhutong.config.ZhutongConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author yaomindong
 * @date 2026/4/2
 */
@Component
@Slf4j
public class ReadConfig implements SmsReadConfig {
    
    @Autowired
    private SmsConfigProvider configProvider;
    
    @Override
    public BaseConfig getSupplierConfig(String configId) {
        if (configProvider != null) {
            try {
                SmsConfigProvider.SmsConfig config = configProvider.getSmsConfig();
                if (config != null && config.getConfigId().equals(configId)) {
                    log.info("自动加载短信配置成功: supplier={}", config.getSupplier());
                    return buildBaseConfig(config);
                } else {
                    log.info("未找到已启用的短信配置");
                }
            } catch (Exception e) {
                log.warn("自动加载短信配置失败: {}", e.getMessage());
            }
        }
        return null;
    }
    
    @Override
    public List<BaseConfig> getSupplierConfigList() {
        if (configProvider != null) {
            try {
                SmsConfigProvider.SmsConfig config = configProvider.getSmsConfig();
                if (config != null) {
                    log.info("自动加载短信配置成功: supplier={}", config.getSupplier());
                    return List.of(buildBaseConfig(config));
                } else {
                    log.info("未找到已启用的短信配置");
                }
            } catch (Exception e) {
                log.warn("自动加载短信配置失败: {}", e.getMessage());
            }
        }
        return List.of();
    }
    
    private BaseConfig buildBaseConfig(SmsConfigProvider.SmsConfig config) {
        BaseConfig baseConfig = createConfigBySupplier(config.getSupplier());
        
        baseConfig.setAccessKeyId(config.getAccessKeyId());
        baseConfig.setAccessKeySecret(config.getAccessKeySecret());
        baseConfig.setSignature(config.getSignature());
        baseConfig.setTemplateId(config.getTemplateId());
        baseConfig.setConfigId(config.getConfigId());
        baseConfig.setWeight(config.getWeight() != null ? config.getWeight() : 1);
        baseConfig.setRetryInterval(config.getRetryInterval() != null ? config.getRetryInterval() : 5);
        baseConfig.setMaxRetries(config.getMaxRetries() != null ? config.getMaxRetries() : 0);
        baseConfig.setMaximum(config.getMaximum());
        
        if (StrUtil.isNotBlank(config.getExtraConfig())) {
            try {
                Map<String, Object> extraMap = JSON.parseObject(config.getExtraConfig(), Map.class);
                applyExtraConfig(baseConfig, extraMap);
            } catch (Exception e) {
                log.warn("解析额外配置失败: {}", e.getMessage());
            }
        }
        
        return baseConfig;
    }
    
    private BaseConfig createConfigBySupplier(String supplier) {
        if (supplier == null) {
            return null;
        }
        
        return switch (supplier.toLowerCase()) {
            case "alibaba" -> new AlibabaConfig();
            case "tencent" -> new TencentConfig();
            case "huawei" -> new HuaweiConfig();
            case "jdcloud" -> new JdCloudConfig();
            case "cloopen" -> new CloopenConfig();
            case "netease" -> new NeteaseConfig();
            case "ctyun" -> new CtyunConfig();
            case "qiniu" -> new QiNiuConfig();
            case "unisms" -> new UniConfig();
            case "yunpian" -> new YunpianConfig();
            case "zhutong" -> new ZhutongConfig();
            case "lianlu" -> new LianLuConfig();
            case "dingzhong" -> new DingZhongConfig();
            case "chuanglan" -> new ChuangLanConfig();
            case "jiguang" -> new JgConfig();
            case "buding_v2" -> new BudingV2Config();
            case "mas" -> new MasConfig();
            case "baidu" -> new BaiduConfig();
            case "luosimao" -> new LuoSiMaoConfig();
            case "mysubmail" -> new SubMailConfig();
            case "danmi" -> new DanMiConfig();
            case "emay" -> new EmayConfig();
            default -> null;
        };
    }
    
    private void applyExtraConfig(BaseConfig baseConfig, Map<String, Object> extraMap) {
        if (extraMap == null || extraMap.isEmpty()) {
            return;
        }
        
        try {
            if (baseConfig instanceof AlibabaConfig alibabaConfig) {
                Object alibabaObj = extraMap.get("alibaba");
                if (alibabaObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> alibabaMap = (Map<String, Object>) alibabaObj;
                    if (alibabaMap.containsKey("templateName")) {
                        alibabaConfig.setTemplateName(String.valueOf(alibabaMap.get("templateName")));
                    }
                    if (alibabaMap.containsKey("requestUrl")) {
                        alibabaConfig.setRequestUrl(String.valueOf(alibabaMap.get("requestUrl")));
                    }
                    if (alibabaMap.containsKey("action")) {
                        alibabaConfig.setAction(String.valueOf(alibabaMap.get("action")));
                    }
                    if (alibabaMap.containsKey("version")) {
                        alibabaConfig.setVersion(String.valueOf(alibabaMap.get("version")));
                    }
                    if (alibabaMap.containsKey("regionId")) {
                        alibabaConfig.setRegionId(String.valueOf(alibabaMap.get("regionId")));
                    }
                }
            } else if (baseConfig instanceof TencentConfig tencentConfig) {
                Object tencentObj = extraMap.get("tencent");
                if (tencentObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tencentMap = (Map<String, Object>) tencentObj;
                    if (tencentMap.containsKey("sdkAppId")) {
                        tencentConfig.setSdkAppId(String.valueOf(tencentMap.get("sdkAppId")));
                    }
                    if (tencentMap.containsKey("territory")) {
                        tencentConfig.setTerritory(String.valueOf(tencentMap.get("territory")));
                    }
                    if (tencentMap.containsKey("codeUrl")) {
                        tencentConfig.setCodeUrl(String.valueOf(tencentMap.get("codeUrl")));
                    }
                    if (tencentMap.containsKey("connTimeout")) {
                        tencentConfig.setConnTimeout(Integer.valueOf(String.valueOf(tencentMap.get("connTimeout"))));
                    }
                    if (tencentMap.containsKey("requestUrl")) {
                        tencentConfig.setRequestUrl(String.valueOf(tencentMap.get("requestUrl")));
                    }
                    if (tencentMap.containsKey("action")) {
                        tencentConfig.setAction(String.valueOf(tencentMap.get("action")));
                    }
                    if (tencentMap.containsKey("version")) {
                        tencentConfig.setVersion(String.valueOf(tencentMap.get("version")));
                    }
                    if (tencentMap.containsKey("service")) {
                        tencentConfig.setService(String.valueOf(tencentMap.get("service")));
                    }
                }
            } else if (baseConfig instanceof HuaweiConfig huaweiConfig) {
                Object huaweiObj = extraMap.get("huawei");
                if (huaweiObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> huaweiMap = (Map<String, Object>) huaweiObj;
                    if (huaweiMap.containsKey("sender")) {
                        huaweiConfig.setSender(String.valueOf(huaweiMap.get("sender")));
                    }
                    if (huaweiMap.containsKey("url")) {
                        huaweiConfig.setUrl(String.valueOf(huaweiMap.get("url")));
                    }
                    if (huaweiMap.containsKey("statusCallBack")) {
                        huaweiConfig.setStatusCallBack(String.valueOf(huaweiMap.get("statusCallBack")));
                    }
                }
            } else if (baseConfig instanceof MasConfig masConfig) {
                Object masObj = extraMap.get("mas");
                if (masObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> masMap = (Map<String, Object>) masObj;
                    if (masMap.containsKey("ecName")) {
                        masConfig.setEcName(String.valueOf(masMap.get("ecName")));
                    }
                    if (masMap.containsKey("signature")) {
                        masConfig.setSignature(String.valueOf(masMap.get("signature")));
                    }
                    if (masMap.containsKey("templateId")) {
                        masConfig.setTemplateId(String.valueOf(masMap.get("templateId")));
                    }
                    if (masMap.containsKey("addSerial")) {
                        masConfig.setAddSerial(String.valueOf(masMap.get("addSerial")));
                    }
                    if (masMap.containsKey("action")) {
                        masConfig.setAction(String.valueOf(masMap.get("action")));
                    }
                    if (masMap.containsKey("requestUrl")) {
                        masConfig.setRequestUrl(String.valueOf(masMap.get("requestUrl")));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("应用额外配置失败: {}", e.getMessage());
        }
    }
}
