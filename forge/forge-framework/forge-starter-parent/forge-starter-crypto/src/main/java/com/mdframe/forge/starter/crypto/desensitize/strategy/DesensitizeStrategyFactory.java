package com.mdframe.forge.starter.crypto.desensitize.strategy;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脱敏策略工厂
 *
 * @author forge
 */
@Slf4j
public class DesensitizeStrategyFactory {

    private final Map<DesensitizeType, DesensitizeStrategy> strategies = new ConcurrentHashMap<>();

    public DesensitizeStrategyFactory() {
        initDefaultStrategies();
    }

    private void initDefaultStrategies() {
        register(DesensitizeType.PHONE, new PhoneDesensitizeStrategy());
        register(DesensitizeType.ID_CARD, new IdCardDesensitizeStrategy());
        register(DesensitizeType.EMAIL, new EmailDesensitizeStrategy());
        register(DesensitizeType.BANK_CARD, new BankCardDesensitizeStrategy());
        register(DesensitizeType.NAME, new NameDesensitizeStrategy());
        register(DesensitizeType.ADDRESS, new AddressDesensitizeStrategy());
        register(DesensitizeType.PASSWORD, new PasswordDesensitizeStrategy());
        register(DesensitizeType.CAR_LICENSE, new CarLicenseDesensitizeStrategy());
    }

    public void register(DesensitizeType type, DesensitizeStrategy strategy) {
        if (type != null && strategy != null) {
            strategies.put(type, strategy);
            log.debug("注册脱敏策略: {}", type);
        }
    }

    public DesensitizeStrategy getStrategy(DesensitizeType type) {
        return strategies.get(type);
    }

    public boolean hasStrategy(DesensitizeType type) {
        return strategies.containsKey(type);
    }
}
