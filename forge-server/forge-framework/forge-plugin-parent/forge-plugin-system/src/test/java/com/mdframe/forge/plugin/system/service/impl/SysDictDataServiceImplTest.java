package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.dto.SysDictDataDTO;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.listener.DictChangeEventListener;
import com.mdframe.forge.plugin.system.mapper.SysDictDataMapper;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheConfig;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheEvict;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheable;
import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SysDictDataServiceImplTest {

    @Test
    void dictionaryReadAndEvictionShouldDeclareManagedCacheContracts() throws NoSuchMethodException {
        ForgeCacheConfig config = SysDictDataServiceImpl.class.getAnnotation(ForgeCacheConfig.class);
        assertThat(config).isNotNull();
        assertThat(config.name()).isEqualTo("system:dict-data");
        assertThat(config.mode()).isEqualTo(CacheMode.MULTI);
        assertThat(config.scope()).isEqualTo(CacheScope.TENANT);
        assertThat(config.localTtlSeconds()).isEqualTo(60);
        assertThat(config.redisTtlSeconds()).isEqualTo(1800);
        assertThat(config.localMaxSize()).isEqualTo(2000);

        Method read = SysDictDataServiceImpl.class.getDeclaredMethod("selectDictDataByType", String.class);
        assertThat(read.getAnnotation(ForgeCacheable.class).key()).isEqualTo("#dictType");

        Method clearOne = SysDictDataServiceImpl.class.getDeclaredMethod("clearDictDataCache", String.class);
        assertThat(clearOne.getAnnotation(ForgeCacheEvict.class).key()).isEqualTo("#dictType");

        Method clearAll = SysDictDataServiceImpl.class.getDeclaredMethod("clearDictDataCache");
        assertThat(clearAll.getAnnotation(ForgeCacheEvict.class).allEntries()).isTrue();
    }

    @Test
    void dictionaryChangeListenerShouldInvalidateOnlyAfterCommit() throws NoSuchMethodException {
        Method listener = DictChangeEventListener.class.getDeclaredMethod(
                "onDictChange", com.mdframe.forge.plugin.system.listener.DictChangeEvent.class);
        TransactionalEventListener annotation = listener.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isTrue();
    }

    @Test
    void selectByTypeShouldLoadEnabledDataThroughMapperXmlContract() {
        MapperStub stub = new MapperStub();
        stub.enabledData = List.of(entity(1L, "sys_status", "1"));

        List<SysDictData> result = service(stub).selectDictDataByType("sys_status");

        assertThat(result).containsExactlyElementsOf(stub.enabledData);
        assertThat(stub.selectedType.get()).isEqualTo("sys_status");
    }

    @Test
    void insertShouldRejectDuplicateValueInSameDictType() {
        MapperStub stub = new MapperStub();
        stub.duplicateCount = 1;
        SysDictDataServiceImpl service = service(stub);

        assertThatThrownBy(() -> service.insertDictData(dto(null, "sys_status", "1")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("同一字典下的字典键值不能重复");

        assertThat(stub.uniqueCheckArguments.get()).containsExactly("sys_status", "1", null);
        assertThat(stub.inserted.get()).isFalse();
    }

    @Test
    void insertShouldSaveWhenValueIsUnique() {
        MapperStub stub = new MapperStub();
        SysDictDataServiceImpl service = service(stub);

        assertThat(service.insertDictData(dto(null, "sys_status", "1"))).isTrue();

        assertThat(stub.uniqueCheckArguments.get()).containsExactly("sys_status", "1", null);
        assertThat(stub.inserted.get()).isTrue();
    }

    @Test
    void updateShouldRejectValueOwnedByAnotherDictItem() {
        MapperStub stub = new MapperStub();
        stub.existing.set(entity(2L, "sys_status", "0"));
        stub.duplicateCount = 1;
        SysDictDataServiceImpl service = service(stub);

        assertThatThrownBy(() -> service.updateDictData(dto(2L, "sys_status", "1")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("同一字典下的字典键值不能重复");

        assertThat(stub.uniqueCheckArguments.get()).containsExactly("sys_status", "1", 2L);
        assertThat(stub.updated.get()).isFalse();
    }

    @Test
    void updateShouldExcludeCurrentDictItemFromUniqueCheck() {
        MapperStub stub = new MapperStub();
        stub.existing.set(entity(2L, "sys_status", "1"));
        SysDictDataServiceImpl service = service(stub);

        assertThat(service.updateDictData(dto(2L, "sys_status", "1"))).isTrue();

        assertThat(stub.uniqueCheckArguments.get()).containsExactly("sys_status", "1", 2L);
        assertThat(stub.updated.get()).isTrue();
    }

    private SysDictDataServiceImpl service(MapperStub stub) {
        return new SysDictDataServiceImpl(stub.mapper(), event -> { });
    }

    private SysDictDataDTO dto(Long dictCode, String dictType, String dictValue) {
        SysDictDataDTO dto = new SysDictDataDTO();
        dto.setDictCode(dictCode);
        dto.setDictType(dictType);
        dto.setDictValue(dictValue);
        return dto;
    }

    private SysDictData entity(Long dictCode, String dictType, String dictValue) {
        SysDictData dictData = new SysDictData();
        dictData.setDictCode(dictCode);
        dictData.setDictType(dictType);
        dictData.setDictValue(dictValue);
        return dictData;
    }

    private static final class MapperStub {

        private final AtomicReference<SysDictData> existing = new AtomicReference<>();
        private final AtomicReference<Object[]> uniqueCheckArguments = new AtomicReference<>();
        private final AtomicBoolean inserted = new AtomicBoolean();
        private final AtomicBoolean updated = new AtomicBoolean();
        private final AtomicReference<String> selectedType = new AtomicReference<>();
        private List<SysDictData> enabledData = List.of();
        private int duplicateCount;

        private SysDictDataMapper mapper() {
            return (SysDictDataMapper) Proxy.newProxyInstance(
                    SysDictDataMapper.class.getClassLoader(),
                    new Class<?>[]{SysDictDataMapper.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "countByDictTypeAndValue" -> {
                            uniqueCheckArguments.set(args);
                            yield duplicateCount;
                        }
                        case "selectById" -> existing.get();
                        case "selectEnabledByType" -> {
                            selectedType.set((String) args[0]);
                            yield enabledData;
                        }
                        case "insert" -> {
                            inserted.set(true);
                            yield 1;
                        }
                        case "updateById" -> {
                            updated.set(true);
                            yield 1;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
