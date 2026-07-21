package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.List;

/**
 * 调度时间与 IANA 时区的统一解释入口。
 */
@Service
public class JobScheduleDomainService {

    public ZoneId requireZoneId(String timezone) {
        String normalized = StringUtils.trimToNull(timezone);
        if (normalized == null || !ZoneId.getAvailableZoneIds().contains(normalized)) {
            throw new BusinessException("时区无效，请选择IANA时区");
        }
        try {
            return ZoneId.of(normalized);
        } catch (RuntimeException exception) {
            throw new BusinessException("时区无效，请选择IANA时区");
        }
    }

    public Instant resolveOnceInstant(LocalDateTime fireOnceTime, ZoneId zoneId) {
        if (fireOnceTime == null) {
            throw new BusinessException("一次性执行时间不能为空");
        }
        if (zoneId == null) {
            throw new BusinessException("时区不能为空");
        }

        ZoneRules rules = zoneId.getRules();
        List<ZoneOffset> validOffsets = rules.getValidOffsets(fireOnceTime);
        if (validOffsets.isEmpty()) {
            throw new BusinessException("所选时间在该时区不存在，请避开夏令时切换时段");
        }

        // DST 重复时间固定采用切换前的较早偏移，保证同一配置始终解析为同一时刻。
        return fireOnceTime.toInstant(validOffsets.get(0));
    }
}
