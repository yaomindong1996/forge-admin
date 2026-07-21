package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.vo.JobExecutorCatalogVO;
import com.mdframe.forge.starter.job.annotation.JobHandler;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 已注册任务处理器目录，只收录显式任务注解。
 */
@Service
public class JobExecutorCatalogService {

    private static final String SOURCE_JOB_HANDLER = "JOB_HANDLER";

    private static final String SOURCE_SCHEDULED_JOB = "SCHEDULED_JOB";

    private final Map<String, CatalogEntry> entries = new ConcurrentHashMap<>();

    public void registerHandler(String beanName, String methodName, JobHandler annotation) {
        String code = StringUtils.trimToNull(annotation.value());
        if (code == null) {
            return;
        }
        boolean methodHandler = StringUtils.isNotBlank(methodName);
        JobExecutorCatalogVO item = JobExecutorCatalogVO.builder()
                .code(code)
                .displayName(resolveDisplayName(annotation.description(), code))
                .description(StringUtils.defaultIfBlank(annotation.description(), "本地任务处理器"))
                .group(StringUtils.defaultIfBlank(annotation.group(), "DEFAULT"))
                .source(SOURCE_JOB_HANDLER)
                .executeMode(methodHandler ? "BEAN" : "HANDLER")
                .executorBean(methodHandler ? beanName : null)
                .executorMethod(methodHandler ? methodName : null)
                .executorHandler(methodHandler ? null : code)
                .build();
        entries.put(entryKey(item), new CatalogEntry(item, beanName));
    }

    public void registerScheduledJob(String beanName, String methodName, ScheduledJob annotation) {
        if (!annotation.enabled()) {
            return;
        }
        String code = StringUtils.defaultIfBlank(annotation.name(), beanName + "." + methodName);
        JobExecutorCatalogVO item = JobExecutorCatalogVO.builder()
                .code(code)
                .displayName(resolveDisplayName(annotation.description(), code))
                .description(StringUtils.defaultIfBlank(annotation.description(), "注解登记的周期任务"))
                .group(StringUtils.defaultIfBlank(annotation.group(), "DEFAULT"))
                .source(SOURCE_SCHEDULED_JOB)
                .executeMode("BEAN")
                .executorBean(beanName)
                .executorMethod(methodName)
                .build();
        entries.put(entryKey(item), new CatalogEntry(item, beanName));
    }

    public List<JobExecutorCatalogVO> listExecutors() {
        List<JobExecutorCatalogVO> result = new ArrayList<>();
        entries.values().stream()
                .map(CatalogEntry::item)
                .sorted(Comparator.comparing(JobExecutorCatalogVO::getGroup)
                        .thenComparing(JobExecutorCatalogVO::getDisplayName)
                        .thenComparing(JobExecutorCatalogVO::getCode))
                .forEach(result::add);
        return result;
    }

    public JobExecutorCatalogVO find(String executeMode, String code) {
        if (StringUtils.isBlank(executeMode) || StringUtils.isBlank(code)) {
            return null;
        }
        CatalogEntry entry = entries.get(executeMode + ":" + code);
        return entry == null ? null : entry.item();
    }

    public JobExecutorCatalogVO findByTarget(String executeMode, String executorHandler,
                                             String executorBean, String executorMethod) {
        if ("HANDLER".equals(executeMode)) {
            return find(executeMode, executorHandler);
        }
        if (!"BEAN".equals(executeMode)) {
            return null;
        }
        return entries.values().stream()
                .map(CatalogEntry::item)
                .filter(item -> "BEAN".equals(item.getExecuteMode()))
                .filter(item -> StringUtils.equals(item.getExecutorBean(), executorBean))
                .filter(item -> StringUtils.equals(item.getExecutorMethod(), executorMethod))
                .findFirst()
                .orElse(null);
    }

    public String resolveHandlerBeanName(String handlerCode) {
        CatalogEntry entry = entries.get("HANDLER:" + handlerCode);
        return entry == null ? handlerCode : entry.beanName();
    }

    private String entryKey(JobExecutorCatalogVO item) {
        return item.getExecuteMode() + ":" + item.getCode();
    }

    private String resolveDisplayName(String description, String code) {
        String displayName = StringUtils.trimToNull(description);
        if (displayName == null) {
            return code;
        }
        int separator = displayName.indexOf('，');
        if (separator < 0) {
            separator = displayName.indexOf('。');
        }
        return separator > 0 ? displayName.substring(0, separator) : displayName;
    }

    private record CatalogEntry(JobExecutorCatalogVO item, String beanName) {
    }
}
