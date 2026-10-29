package com.mdframe.forge.starter.message.sms;

import java.util.concurrent.atomic.AtomicLong;

public class LocalSmsRouteSequence implements SmsRouteSequence {

    private final AtomicLong sequence = new AtomicLong();

    @Override
    public long next(long tenantId) {
        return Math.max(0L, sequence.getAndIncrement());
    }
}
