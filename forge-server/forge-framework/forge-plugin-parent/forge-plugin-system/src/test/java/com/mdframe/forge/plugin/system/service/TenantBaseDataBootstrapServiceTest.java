package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.listener.DictChangeEvent;
import com.mdframe.forge.plugin.system.mapper.SysConfigMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictDataMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictTypeMapper;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.TenantBaseDataBootstrapMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantBaseDataBootstrapServiceTest {

    @Test
    void shouldCopySelectedPackagesOnly() {
        AtomicInteger orgInserts = new AtomicInteger();
        AtomicInteger roleInserts = new AtomicInteger();
        AtomicInteger messageCopies = new AtomicInteger();
        AtomicReference<Object> published = new AtomicReference<>();

        TenantBaseDataBootstrapMapper bootstrapMapper = proxy(TenantBaseDataBootstrapMapper.class, (method, args) -> {
            switch (method) {
                case "countActiveDictTypes", "countActiveConfigs" -> {
                    Long tenantId = (Long) args[0];
                    return tenantId.equals(1L) ? 1L : 0L;
                }
                case "countActiveOrgs", "countRolesByKey" -> {
                    return 0L;
                }
                case "selectActiveDictTypes" -> {
                    SysDictType type = new SysDictType();
                    type.setDictType("sys_user_sex");
                    type.setDictName("性别");
                    type.setDictStatus(1);
                    return List.of(type);
                }
                case "selectActiveDictData" -> {
                    SysDictData parent = new SysDictData();
                    parent.setDictCode(10L);
                    parent.setDictType("sys_user_sex");
                    parent.setDictLabel("父");
                    parent.setDictValue("P");
                    parent.setDictStatus(1);
                    SysDictData child = new SysDictData();
                    child.setDictCode(11L);
                    child.setDictType("sys_user_sex");
                    child.setDictLabel("子");
                    child.setDictValue("C");
                    child.setParentDictCode(10L);
                    child.setDictStatus(1);
                    return List.of(parent, child);
                }
                case "selectActiveConfigs" -> {
                    SysConfig config = new SysConfig();
                    config.setConfigKey("sys.password.minLength");
                    config.setConfigName("最小长度");
                    config.setConfigValue("6");
                    config.setConfigType("Y");
                    return List.of(config);
                }
                case "selectTemplateAdminRoleId" -> {
                    return 1L;
                }
                case "copyRoleResourcesExcludingTenantMgmt" -> {
                    return 3;
                }
                case "copyMessageTemplates" -> {
                    messageCopies.incrementAndGet();
                    return 2;
                }
                case "copyMessageBizTypes" -> {
                    return 1;
                }
                default -> {
                    return defaultValue(method);
                }
            }
        });

        AtomicLong nextDictCode = new AtomicLong(100);
        SysDictTypeMapper typeMapper = proxy(SysDictTypeMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                return 1;
            }
            return defaultValue(method);
        });
        SysDictDataMapper dataMapper = proxy(SysDictDataMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                SysDictData row = (SysDictData) args[0];
                row.setDictCode(nextDictCode.getAndIncrement());
                return 1;
            }
            if ("updateById".equals(method)) {
                return 1;
            }
            return defaultValue(method);
        });
        SysConfigMapper configMapper = proxy(SysConfigMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                return 1;
            }
            return defaultValue(method);
        });
        SysOrgMapper orgMapper = proxy(SysOrgMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                SysOrg org = (SysOrg) args[0];
                assertThat(org.getOrgName()).isEqualTo("演示租户");
                orgInserts.incrementAndGet();
                return 1;
            }
            return defaultValue(method);
        });
        SysRoleMapper roleMapper = proxy(SysRoleMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                SysRole role = (SysRole) args[0];
                role.setId(88L);
                assertThat(role.getRoleKey()).isEqualTo("tenant_admin");
                roleInserts.incrementAndGet();
                return 1;
            }
            return defaultValue(method);
        });

        TenantBaseDataBootstrapService service = new TenantBaseDataBootstrapService(
                bootstrapMapper, typeMapper, dataMapper, configMapper, orgMapper, roleMapper, published::set);

        TenantBaseDataBootstrapService.BootstrapResult result = service.ensureFromDefaultTenant(
                9L,
                List.of(
                        TenantBootstrapPackages.DICT_AND_CONFIG,
                        TenantBootstrapPackages.ROOT_ORG,
                        TenantBootstrapPackages.TENANT_ADMIN_ROLE,
                        TenantBootstrapPackages.MESSAGE_TEMPLATES
                ),
                "演示租户");

        assertThat(result.appliedPackages()).containsExactlyInAnyOrder(
                TenantBootstrapPackages.DICT_AND_CONFIG,
                TenantBootstrapPackages.ROOT_ORG,
                TenantBootstrapPackages.TENANT_ADMIN_ROLE,
                TenantBootstrapPackages.MESSAGE_TEMPLATES);
        assertThat(result.dictTypesCopied()).isEqualTo(1);
        assertThat(result.dictDataCopied()).isEqualTo(2);
        assertThat(result.configsCopied()).isEqualTo(1);
        assertThat(orgInserts.get()).isEqualTo(1);
        assertThat(roleInserts.get()).isEqualTo(1);
        assertThat(messageCopies.get()).isEqualTo(1);
        assertThat(result.packageCounts().get("messageTemplates")).isEqualTo(2);
        assertThat(published.get()).isInstanceOf(DictChangeEvent.class);
    }

    @Test
    void shouldSkipAllWhenPackagesEmpty() {
        AtomicInteger inserts = new AtomicInteger();
        TenantBaseDataBootstrapService service = new TenantBaseDataBootstrapService(
                proxy(TenantBaseDataBootstrapMapper.class, (method, args) -> {
                    inserts.incrementAndGet();
                    return defaultValue(method);
                }),
                proxy(SysDictTypeMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysDictDataMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysConfigMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysOrgMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysRoleMapper.class, (method, args) -> defaultValue(method)),
                event -> {
                });

        TenantBaseDataBootstrapService.BootstrapResult result =
                service.ensureFromDefaultTenant(9L, List.of(), "x");
        assertThat(result.appliedPackages()).isEmpty();
        assertThat(inserts.get()).isZero();
    }

    @Test
    void shouldNoopForDefaultTenant() {
        TenantBaseDataBootstrapService service = new TenantBaseDataBootstrapService(
                proxy(TenantBaseDataBootstrapMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysDictTypeMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysDictDataMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysConfigMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysOrgMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysRoleMapper.class, (method, args) -> defaultValue(method)),
                event -> {
                });
        assertThat(service.ensureFromDefaultTenant(1L, null, "默认").appliedPackages()).isEmpty();
    }

    private interface Invoker {
        Object invoke(String method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invoker invoker) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return invoker.invoke(method.getName(), args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "countActiveDictTypes", "countActiveConfigs", "countActiveOrgs", "countRolesByKey" -> 0L;
            case "selectActiveDictTypes", "selectActiveDictData", "selectActiveConfigs" -> List.of();
            case "selectTemplateAdminRoleId" -> null;
            case "insert", "updateById", "copyRoleResourcesExcludingTenantMgmt",
                 "copyMessageTemplates", "copyMessageBizTypes", "copyFlowCommentPhrases",
                 "copyFlowSpelTemplates", "copyDataScopeConfigs" -> 0;
            default -> null;
        };
    }
}
