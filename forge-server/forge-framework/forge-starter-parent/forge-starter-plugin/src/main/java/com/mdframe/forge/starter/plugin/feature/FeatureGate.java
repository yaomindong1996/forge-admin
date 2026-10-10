package com.mdframe.forge.starter.plugin.feature;

/**
 * 功能使用权扩展点，独立于登录、RBAC 及更新维保权益。
 * 企业实现不得将永久使用权直接绑定到年度维保到期时间。
 */
public interface FeatureGate {

    boolean isEnabled(String featureCode);

    String edition();
}
