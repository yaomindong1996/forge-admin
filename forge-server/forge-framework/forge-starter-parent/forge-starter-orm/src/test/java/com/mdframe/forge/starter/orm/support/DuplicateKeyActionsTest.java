package com.mdframe.forge.starter.orm.support;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateKeyActionsTest {

    @Test
    void insertIgnoreConflictReturnsZeroOnDuplicate() {
        assertThat(DuplicateKeyActions.insertIgnoreConflict(() -> {
            throw new DuplicateKeyException("dup");
        })).isZero();
        assertThat(DuplicateKeyActions.insertIgnoreConflict(() -> 1)).isEqualTo(1);
    }

    @Test
    void upsertUpdatesFirstThenInsertsThenRetriesUpdateOnRace() {
        AtomicInteger updates = new AtomicInteger();
        AtomicInteger inserts = new AtomicInteger();
        assertThat(DuplicateKeyActions.upsert(() -> {
            updates.incrementAndGet();
            return 1;
        }, () -> {
            inserts.incrementAndGet();
            return 1;
        })).isEqualTo(1);
        assertThat(updates.get()).isEqualTo(1);
        assertThat(inserts.get()).isZero();

        updates.set(0);
        inserts.set(0);
        assertThat(DuplicateKeyActions.upsert(() -> {
            updates.incrementAndGet();
            return 0;
        }, () -> {
            inserts.incrementAndGet();
            return 1;
        })).isEqualTo(1);
        assertThat(updates.get()).isEqualTo(1);
        assertThat(inserts.get()).isEqualTo(1);

        updates.set(0);
        inserts.set(0);
        assertThat(DuplicateKeyActions.upsert(() -> updates.incrementAndGet() == 1 ? 0 : 1, () -> {
            inserts.incrementAndGet();
            throw new DuplicateKeyException("race");
        })).isEqualTo(1);
        assertThat(updates.get()).isEqualTo(2);
        assertThat(inserts.get()).isEqualTo(1);
    }
}
