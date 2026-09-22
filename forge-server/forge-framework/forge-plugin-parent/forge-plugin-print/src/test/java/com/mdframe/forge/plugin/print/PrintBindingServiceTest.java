package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintBindingServiceTest extends PrintServiceFixture {

    PrintBindingSaveDTO bind(Long id, Long revision, Long template, boolean isDefault, int status) {
        return new PrintBindingSaveDTO(id, revision, source, template, PrintScene.DETAIL, isDefault, 0, status);
    }

    @Test
    void concurrentFirstBindingsSerializeOnApplicationRow() throws Exception {
        var one = create("one");
        var two = create("two");
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var first = workers.submit(() -> {
                start.await();
                return bindings.save(bind(null, null, one.id(), true, 1));
            });
            var second = workers.submit(() -> {
                start.await();
                return bindings.save(bind(null, null, two.id(), true, 1));
            });
            start.countDown();
            first.get(5, java.util.concurrent.TimeUnit.SECONDS);
            second.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_print_binding WHERE is_default=1 AND del_flag=0", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_print_binding WHERE del_flag=0", Integer.class)).isEqualTo(2);
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void defaultIsUniqueAndOldDefaultRevisionChanges() {
        var one = create("one");
        var two = create("two");
        var first = bindings.save(bind(null, null, one.id(), true, 1));
        bindings.save(bind(null, null, two.id(), true, 1));
        var all = bindings.list(new PrintBindingQueryDTO(2L, PrintSourceType.LOWCODE, "page_purchase", null, "purchase", PrintScene.DETAIL));
        assertThat(all.stream().filter(b -> b.isDefault())).hasSize(1);
        fails(409, () -> bindings.save(bind(first.id(), first.bindingRevision(), one.id(), true, 1)));
    }

    @Test
    void duplicateCreateUpdatesExistingBinding() {
        var one = create("one");
        var two = create("two");
        var first = bindings.save(bind(null, null, one.id(), true, 1));
        var second = bindings.save(bind(null, null, two.id(), false, 1));
        var again = bindings.save(bind(null, null, two.id(), true, 1));
        assertThat(again.id()).isEqualTo(second.id());
        assertThat(again.isDefault()).isTrue();
        assertThat(bindingMapper.selectScoped(1L, first.id()).getIsDefault()).isFalse();
        fails(409, () -> service.delete(one.id(), one.draftRevision()));
        var latestFirst = bindingMapper.selectScoped(1L, first.id());
        bindings.delete(latestFirst.getId(), latestFirst.getBindingRevision());
        service.delete(one.id(), one.draftRevision());
    }

    @Test
    void identityCannotBeChangedAndDisablingClearsDefault() {
        var one = create("one");
        var two = create("two");
        var first = bindings.save(bind(null, null, one.id(), true, 1));
        fails(403, () -> bindings.save(bind(first.id(), first.bindingRevision(), two.id(), true, 1)));
        assertThat(bindings.save(bind(first.id(), first.bindingRevision(), one.id(), true, 0)).isDefault()).isFalse();
        actor = new PrintActor(2L, 9L, 1L);
        fails(404, () -> bindings.delete(first.id(), 2L));
    }
}
