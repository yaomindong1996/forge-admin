package com.mdframe.forge.starter.id.config;

import com.mdframe.forge.starter.id.generator.FusionDisposableWorkerIdAssigner;
import com.mdframe.forge.starter.id.properties.UidCacheGeneratorProperties;
import com.mdframe.forge.starter.id.properties.UidDefaultGeneratorProperties;
import com.xfvape.uid.impl.CachedUidGenerator;
import com.xfvape.uid.impl.DefaultUidGenerator;
import com.xfvape.uid.worker.WorkerIdAssigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @Description: id 配置 提供了两种生成器: DefaultUidGenerator、CachedUidGenerator。如对UID生成性能有要求, 请使用CachedUidGenerator
 * 每秒下的并发序列，9 bits可支持每台服务器每秒512个并发。
 */
@Configuration
@ComponentScan(basePackages = {"com.xfvape.uid"})
public class IdGeneratorConfiguration {


    @Autowired
    private UidCacheGeneratorProperties uidCacheGeneratorProperties;

    @Autowired
    private UidDefaultGeneratorProperties uidDefaultGeneratorProperties;

    @Bean
    public WorkerIdAssigner disposableWorkerIdAssigner() {
        return new FusionDisposableWorkerIdAssigner();
    }

    @Bean(name = "cachedUidGenerator")
    @Scope("prototype")
    public CachedUidGenerator cachedUidGenerator() {
        CachedUidGenerator cachedUidGenerator = new CachedUidGenerator();
        cachedUidGenerator.setWorkerIdAssigner(disposableWorkerIdAssigner());
        cachedUidGenerator.setTimeBits(uidCacheGeneratorProperties.getTimeBits());
        cachedUidGenerator.setWorkerBits(uidCacheGeneratorProperties.getWorkerBits());
        cachedUidGenerator.setSeqBits(uidCacheGeneratorProperties.getSeqBits());
        cachedUidGenerator.setEpochStr(uidCacheGeneratorProperties.getEpochStr());
        //默认:3， 原bufferSize=8192, 扩容后bufferSize= 8192 << 3 = 65536
        cachedUidGenerator.setBoostPower(3);
        // <!-- 指定何时向RingBuffer中填充UID, 取值为百分比(0, 100), 默认为50 -->
        // <!-- 举例: bufferSize=1024, paddingFactor=50 -> threshold=1024 * 50 / 100 = 512. -->
        // <!-- 当环上可用UID数量 < 512时, 将自动对RingBuffer进行填充补全 -->
        cachedUidGenerator.setPaddingFactor(50);
        // <!-- 另外一种RingBuffer填充时机, 在Schedule线程中, 周期性检查填充 -->
        // <!-- 默认:不配置此项, 即不实用Schedule线程. 如需使用, 请指定Schedule线程时间间隔, 单位:秒 -->
        cachedUidGenerator.setScheduleInterval(60);
        return cachedUidGenerator;
    }


    @Bean(name = "defaultUidGenerator")
    @Scope("prototype")
    public DefaultUidGenerator defaultUidGenerator() {
        DefaultUidGenerator defaultUidGenerator = new DefaultUidGenerator();
        defaultUidGenerator.setWorkerIdAssigner(disposableWorkerIdAssigner());
        defaultUidGenerator.setTimeBits(uidDefaultGeneratorProperties.getTimeBits());
        defaultUidGenerator.setWorkerBits(uidDefaultGeneratorProperties.getWorkerBits());
        defaultUidGenerator.setSeqBits(uidDefaultGeneratorProperties.getSeqBits());
        defaultUidGenerator.setEpochStr(uidDefaultGeneratorProperties.getEpochStr());
        return defaultUidGenerator;
    }


}
