package com.mdframe.forge.starter.id.service.impl;

import com.mdframe.forge.starter.id.entity.SysIdSequence;
import com.mdframe.forge.starter.id.generator.SegmentSequenceGenerator;
import com.mdframe.forge.starter.id.mapper.SysIdSequenceMapper;
import com.mdframe.forge.starter.id.service.ISequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 序列生成服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SequenceServiceImpl implements ISequenceService {
    
    private final SegmentSequenceGenerator segmentGenerator;
    private final SysIdSequenceMapper sequenceMapper;
    
    @Override
    public long nextId(String bizKey) {
        return segmentGenerator.nextId(bizKey);
    }
    
    @Override
    public long[] nextBatch(String bizKey, int size) {
        return segmentGenerator.nextBatch(bizKey, size);
    }
    
    @Override
    public String nextFormatted(String bizKey) {
        // 获取序列配置
        SysIdSequence config = sequenceMapper.selectById(bizKey);
        
        if (config == null) {
            // 使用默认配置生成
            long id = nextId(bizKey);
            return String.format("%08d", id);
        }
        
        // 根据重置策略确定实际的bizKey
        String effectiveBizKey = getEffectiveBizKey(bizKey, config);
        
        // 生成序列号
        long seqNum = segmentGenerator.nextId(effectiveBizKey);
        
        // 构建格式化字符串
        return buildFormattedSequence(config, seqNum);
    }
    
    @Override
    public String[] nextFormattedBatch(String bizKey, int size) {
        String[] results = new String[size];
        for (int i = 0; i < size; i++) {
            results[i] = nextFormatted(bizKey);
        }
        return results;
    }
    
    /**
     * 根据重置策略获取有效的bizKey
     * 例如：DAILY策略下，bizKey会追加日期，每天独立计数
     */
    private String getEffectiveBizKey(String bizKey, SysIdSequence config) {
        String resetPolicy = config.getResetPolicy();
        
        if ("DAILY".equalsIgnoreCase(resetPolicy)) {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            return bizKey + ":" + date;
        } else if ("HOURLY".equalsIgnoreCase(resetPolicy)) {
            String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
            return bizKey + ":" + hour;
        }
        
        return bizKey;
    }
    
    /**
     * 构建格式化的序列号
     * 格式：[前缀][日期时间][补零的序列号]
     */
    private String buildFormattedSequence(SysIdSequence config, long seqNum) {
        StringBuilder sb = new StringBuilder();
        
        // 1. 添加前缀
        if (config.getPrefix() != null && !config.getPrefix().isEmpty()) {
            sb.append(config.getPrefix());
        }
        
        // 2. 根据重置策略添加日期时间
        String resetPolicy = config.getResetPolicy();
        if ("DAILY".equalsIgnoreCase(resetPolicy)) {
            sb.append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        } else if ("HOURLY".equalsIgnoreCase(resetPolicy)) {
            sb.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH")));
        }
        
        // 3. 添加补零的序列号
        int length = config.getSeqLength() != null ? config.getSeqLength() : 8;
        String seqStr = String.format("%0" + length + "d", seqNum);
        sb.append(seqStr);
        
        return sb.toString();
    }
}
