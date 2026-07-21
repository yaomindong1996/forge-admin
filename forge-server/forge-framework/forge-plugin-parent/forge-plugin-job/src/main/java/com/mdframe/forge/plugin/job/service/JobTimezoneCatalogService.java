package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.constant.JobScheduleType;
import com.mdframe.forge.plugin.job.vo.JobTimezoneOptionVO;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

/**
 * 与当前 JVM 时区数据库一致的 IANA 时区目录。
 */
@Service
public class JobTimezoneCatalogService {

    private static final List<String> PREFERRED_TIMEZONES = List.of(
            JobScheduleType.DEFAULT_TIMEZONE,
            "UTC",
            "Asia/Hong_Kong",
            "Asia/Tokyo",
            "Europe/London",
            "America/New_York"
    );

    public List<JobTimezoneOptionVO> listTimezones(Clock clock) {
        Instant referenceTime = (clock == null ? Clock.systemUTC() : clock).instant();
        return ZoneId.getAvailableZoneIds().stream()
                .sorted(Comparator.comparingInt(this::priority).thenComparing(String::compareTo))
                .map(value -> toOption(value, referenceTime))
                .toList();
    }

    private JobTimezoneOptionVO toOption(String value, Instant referenceTime) {
        ZoneOffset zoneOffset = ZoneId.of(value).getRules().getOffset(referenceTime);
        String offset = "UTC" + (ZoneOffset.UTC.equals(zoneOffset) ? "+00:00" : zoneOffset.getId());
        return JobTimezoneOptionVO.builder()
                .label(value + " (当前 " + offset + ")")
                .value(value)
                .offset(offset)
                .build();
    }

    private int priority(String value) {
        int index = PREFERRED_TIMEZONES.indexOf(value);
        return index < 0 ? PREFERRED_TIMEZONES.size() : index;
    }
}
