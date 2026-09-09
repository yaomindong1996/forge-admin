package com.mdframe.forge.starter.datascope.listener;

import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.event.DataScopeCacheRefreshedEvent;
import com.mdframe.forge.starter.datascope.service.impl.DataScopeServiceImpl;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.BaseStatusListener;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 数据权限本地快照的跨实例失效通知，订阅重连时补载最新数据。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeCacheSynchronizer {
    private final ObjectProvider<RedissonClient> redissonProvider;
    private final ObjectProvider<DataScopeServiceImpl> serviceProvider;
    private final DataScopeProperties properties;
    private final String instanceId = UUID.randomUUID().toString();
    // 合并密集通知，最多保留一个待执行刷新；数据库加载不占用订阅回调线程。
    private final ThreadPoolExecutor reloadExecutor = new ThreadPoolExecutor(1, 1, 0,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), runnable -> {
                Thread thread = new Thread(runnable, "forge-datascope-refresh");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.DiscardOldestPolicy());
    private volatile RTopic topic;
    private int messageListenerId;
    private int statusListenerId;

    @Order(-100)
    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        RedissonClient client = redissonProvider.getIfAvailable();
        if (client == null) {
            log.info("未配置 Redis，数据权限使用本地刷新模式");
            return;
        }
        topic = client.getTopic(properties.getCacheRefreshTopic(), StringCodec.INSTANCE);
        messageListenerId = topic.addListener(String.class, (channel, sender) -> {
            if (!instanceId.equals(sender)) {
                scheduleReload();
            }
        });
        statusListenerId = topic.addListener(new BaseStatusListener() {
            @Override
            public void onSubscribe(String channel) {
                scheduleReload();
            }
        });
    }

    @EventListener
    public void broadcast(DataScopeCacheRefreshedEvent event) {
        if (topic != null) {
            topic.publish(instanceId);
        }
    }

    private void scheduleReload() {
        if (!reloadExecutor.isShutdown()) {
            reloadExecutor.execute(() -> {
                try {
                    serviceProvider.getObject().reloadLocalDataScopeCache();
                } catch (Exception exception) {
                    log.error("跨实例数据权限刷新失败，后续请求将重试加载", exception);
                }
            });
        }
    }

    @PreDestroy
    public void close() {
        reloadExecutor.shutdownNow();
        if (topic != null) {
            topic.removeListener(messageListenerId, statusListenerId);
        }
    }
}
