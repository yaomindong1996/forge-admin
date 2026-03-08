package com.mdframe.forge.starter.id.generator;

import com.mdframe.forge.starter.id.entity.SysIdSequence;
import com.mdframe.forge.starter.id.mapper.SysIdSequenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 段模式序列生成器
 * 基于数据库号段分配，内存中高速生成ID，保证同业务类型严格递增
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SegmentSequenceGenerator {
    
    private final SysIdSequenceMapper sequenceMapper;
    
    /**
     * 各业务类型的号段缓存
     * key: bizKey
     * value: Segment对象
     */
    private final Map<String, Segment> segmentCache = new ConcurrentHashMap<>();
    
    /**
     * 默认步长
     */
    private static final int DEFAULT_STEP = 1000;
    
    /**
     * 预取阈值（剩余量低于此值时触发预取）
     */
    private static final int PREFETCH_THRESHOLD = 100;
    
    /**
     * 获取下一个ID
     */
    public long nextId(String bizKey) {
        Segment segment = segmentCache.computeIfAbsent(bizKey, this::loadSegment);
        
        long id = segment.next();
        
        // 剩余量不足时，异步预取下一段
        if (segment.remaining() <= PREFETCH_THRESHOLD) {
            tryPrefetch(bizKey);
        }
        
        return id;
    }
    
    /**
     * 批量获取ID
     */
    public long[] nextBatch(String bizKey, int size) {
        long[] ids = new long[size];
        for (int i = 0; i < size; i++) {
            ids[i] = nextId(bizKey);
        }
        return ids;
    }
    
    /**
     * 尝试预取下一段
     */
    private void tryPrefetch(String bizKey) {
        segmentCache.compute(bizKey, (key, oldSegment) -> {
            if (oldSegment == null || oldSegment.remaining() <= PREFETCH_THRESHOLD) {
                Segment newSegment = loadSegment(key);
                if (oldSegment != null) {
                    // 将新段设置为下一段，当前段用完后自动切换
                    oldSegment.setNext(newSegment);
                    return oldSegment;
                }
                return newSegment;
            }
            return oldSegment;
        });
    }
    
    /**
     * 从数据库加载一个新号段
     */
    private Segment loadSegment(String bizKey) {
        // 查询序列配置
        SysIdSequence sequence = sequenceMapper.selectById(bizKey);
        
        if (sequence == null) {
            // 首次使用，初始化序列
            sequence = initSequence(bizKey);
        }
        
        int step = sequence.getStep() != null ? sequence.getStep() : DEFAULT_STEP;
        int version = sequence.getVersion();
        
        // 使用乐观锁分配号段
        int updated = sequenceMapper.allocateSegment(bizKey, step, version);
        
        if (updated == 0) {
            // 版本冲突，说明有其他节点也在分配，重试一次
            log.warn("分配号段版本冲突，重试: {}", bizKey);
            sequence = sequenceMapper.selectById(bizKey);
            step = sequence.getStep() != null ? sequence.getStep() : DEFAULT_STEP;
            updated = sequenceMapper.allocateSegment(bizKey, step, sequence.getVersion());
            
            if (updated == 0) {
                throw new IllegalStateException("分配ID号段失败，请稍后重试: " + bizKey);
            }
        }
        
        // 重新查询获取更新后的maxId
        SysIdSequence updated序列 = sequenceMapper.selectById(bizKey);
        long end = updated序列.getMaxId();
        long start = end - step + 1;
        
        log.debug("成功分配号段: bizKey={}, range=[{}, {}]", bizKey, start, end);
        
        return new Segment(start, end);
    }
    
    /**
     * 初始化序列配置
     */
    private synchronized SysIdSequence initSequence(String bizKey) {
        // 双重检查，避免并发重复插入
        SysIdSequence existing = sequenceMapper.selectById(bizKey);
        if (existing != null) {
            return existing;
        }
        
        SysIdSequence sequence = new SysIdSequence();
        sequence.setBizKey(bizKey);
        sequence.setMaxId(0L);
        sequence.setStep(DEFAULT_STEP);
        sequence.setVersion(0);
        sequence.setResetPolicy("NONE");
        sequence.setSeqLength(8);
        
        sequenceMapper.insert(sequence);
        log.info("初始化序列配置: {}", bizKey);
        
        return sequence;
    }
    
    /**
     * 号段对象
     */
    static class Segment {
        private final long start;
        private final long end;
        private final AtomicLong cursor;
        private volatile Segment next;
        
        public Segment(long start, long end) {
            this.start = start;
            this.end = end;
            this.cursor = new AtomicLong(start - 1);
        }
        
        /**
         * 获取下一个ID
         */
        public long next() {
            long value = cursor.incrementAndGet();
            
            if (value <= end) {
                return value;
            }
            
            // 当前段耗尽，切换到下一段
            if (next != null) {
                return next.next();
            }
            
            throw new IllegalStateException("ID号段已耗尽，且没有预取的下一段");
        }
        
        /**
         * 剩余可用数量
         */
        public long remaining() {
            long current = cursor.get();
            return Math.max(0, end - current);
        }
        
        /**
         * 设置下一段
         */
        public void setNext(Segment next) {
            this.next = next;
        }
    }
}
