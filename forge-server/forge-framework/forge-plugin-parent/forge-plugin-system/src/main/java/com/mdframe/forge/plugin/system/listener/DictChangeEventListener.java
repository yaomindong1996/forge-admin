package com.mdframe.forge.plugin.system.listener;

import com.mdframe.forge.plugin.system.service.impl.SytemDictValueProvider;
import com.mdframe.forge.plugin.system.service.ISysDictDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class DictChangeEventListener {

    private final ISysDictDataService dictDataService;
    private final SytemDictValueProvider dictValueProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDictChange(DictChangeEvent event) {
        for (String dictType : event.getDictTypes()) {
            dictDataService.clearDictDataCache(dictType);
            dictValueProvider.clearCache(dictType);
        }
    }
}
