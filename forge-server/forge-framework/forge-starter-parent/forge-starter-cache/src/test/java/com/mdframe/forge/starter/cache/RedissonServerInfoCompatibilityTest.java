package com.mdframe.forge.starter.cache;

import com.mdframe.forge.starter.cache.service.impl.RedissonCacheServiceImpl;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisCluster;
import org.redisson.api.redisnode.RedisClusterMaster;
import org.redisson.api.redisnode.RedisMaster;
import org.redisson.api.redisnode.RedisMasterSlave;
import org.redisson.api.redisnode.RedisNode;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.api.redisnode.RedisSentinelMasterSlave;
import org.redisson.api.redisnode.RedisSingle;
import org.redisson.config.Config;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 只 Mock Redisson 新节点 API，不建立 Redis 连接。 */
class RedissonServerInfoCompatibilityTest {

    private final RedissonClient client = mock(RedissonClient.class);
    private final RedisMaster master = mock(RedisMaster.class);

    @Test
    void readsSingleServerInfo() {
        var config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:1");
        var nodes = mock(RedisSingle.class);
        when(client.getRedisNodes(RedisNodes.SINGLE)).thenReturn(nodes);
        when(nodes.getInstance()).thenReturn(master);
        assertInfo(config, master);
    }

    @Test
    void readsMasterSlaveInfoFromMaster() {
        var config = new Config();
        config.useMasterSlaveServers().setMasterAddress("redis://127.0.0.1:1");
        var nodes = mock(RedisMasterSlave.class);
        when(client.getRedisNodes(RedisNodes.MASTER_SLAVE)).thenReturn(nodes);
        when(nodes.getMaster()).thenReturn(master);
        assertInfo(config, master);
    }

    @Test
    void readsSentinelInfoFromDataMasterNotSentinel() {
        var config = new Config();
        config.useSentinelServers().setMasterName("fixture").addSentinelAddress("redis://127.0.0.1:1");
        var nodes = mock(RedisSentinelMasterSlave.class);
        when(client.getRedisNodes(RedisNodes.SENTINEL_MASTER_SLAVE)).thenReturn(nodes);
        when(nodes.getMaster()).thenReturn(master);
        assertInfo(config, master);
    }

    @Test
    void readsClusterMasterInfo() {
        var config = new Config();
        config.useClusterServers().addNodeAddress("redis://127.0.0.1:1");
        var nodes = mock(RedisCluster.class);
        var clusterMaster = mock(RedisClusterMaster.class);
        when(client.getRedisNodes(RedisNodes.CLUSTER)).thenReturn(nodes);
        when(nodes.getMasters()).thenReturn(List.of(clusterMaster));
        assertInfo(config, clusterMaster);
    }

    private void assertInfo(Config config, RedisNode node) {
        when(client.getConfig()).thenReturn(config);
        when(node.info(RedisNode.InfoSection.SERVER)).thenReturn(Map.of("redis_version", "fixture-4"));
        when(node.info(RedisNode.InfoSection.MEMORY)).thenReturn(Map.of("used_memory", "1024"));
        var service = new RedissonCacheServiceImpl(client);
        assertThat(service.getServerInfo()).containsEntry("redisVersion", "fixture-4").doesNotContainKey("error");
        assertThat(service.getMemoryInfo()).containsEntry("usedMemory", "1024").doesNotContainKey("error");
    }
}
