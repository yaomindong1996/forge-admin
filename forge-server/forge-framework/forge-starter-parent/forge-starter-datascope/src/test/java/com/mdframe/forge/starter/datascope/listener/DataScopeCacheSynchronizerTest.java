package com.mdframe.forge.starter.datascope.listener;

import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.event.DataScopeCacheRefreshedEvent;
import com.mdframe.forge.starter.datascope.service.impl.DataScopeServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.listener.StatusListener;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataScopeCacheSynchronizerTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldReloadOtherInstancesWithoutRebroadcastAndReloadOnResubscribe() {
        RedissonClient client = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        DataScopeServiceImpl localService = mock(DataScopeServiceImpl.class);
        when(client.getTopic(anyString(), eq(StringCodec.INSTANCE))).thenReturn(topic);
        when(topic.addListener(eq(String.class), any(MessageListener.class))).thenReturn(11);
        when(topic.addListener(any(StatusListener.class))).thenReturn(12);
        DataScopeCacheSynchronizer synchronizer = new DataScopeCacheSynchronizer(
                provider(client), provider(localService), new DataScopeProperties());
        try {
            synchronizer.subscribe();
            ArgumentCaptor<MessageListener<String>> listener = ArgumentCaptor.forClass(MessageListener.class);
            verify(topic).addListener(eq(String.class), listener.capture());

            synchronizer.broadcast(new DataScopeCacheRefreshedEvent());
            ArgumentCaptor<String> sender = ArgumentCaptor.forClass(String.class);
            verify(topic).publish(sender.capture());
            listener.getValue().onMessage("test", sender.getValue());
            verifyNoInteractions(localService);

            listener.getValue().onMessage("test", "another-instance");
            verify(localService, timeout(2000)).reloadLocalDataScopeCache();
            verify(localService, never()).refreshDataScopeCache();
            verify(topic, times(1)).publish(anyString());

            ArgumentCaptor<StatusListener> status = ArgumentCaptor.forClass(StatusListener.class);
            verify(topic).addListener(status.capture());
            status.getValue().onSubscribe("test");
            verify(localService, timeout(2000).times(2)).reloadLocalDataScopeCache();
        } finally {
            synchronizer.close();
        }
        verify(topic).removeListener(11, 12);
    }

    @Test
    void shouldSupportLocalModeWithoutRedis() {
        DataScopeServiceImpl service = mock(DataScopeServiceImpl.class);
        DataScopeCacheSynchronizer synchronizer = new DataScopeCacheSynchronizer(
                provider(null), provider(service), new DataScopeProperties());
        try {
            synchronizer.subscribe();
            synchronizer.broadcast(new DataScopeCacheRefreshedEvent());
            verifyNoInteractions(service);
        } finally {
            synchronizer.close();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        when(provider.getObject()).thenReturn(value);
        return provider;
    }
}
