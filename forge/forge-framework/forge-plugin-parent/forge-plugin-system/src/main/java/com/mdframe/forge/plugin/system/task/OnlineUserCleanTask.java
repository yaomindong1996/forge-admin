package com.mdframe.forge.plugin.system.task;

import com.mdframe.forge.plugin.system.mapper.SysOnlineUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 在线用户清理定时任务
 * 定期清理过期的在线用户记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUserCleanTask {

    private final SysOnlineUserMapper onlineUserMapper;

    /**
     * 清理过期的在线用户
     * 每10分钟执行一次
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void cleanExpiredUsers() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int count = onlineUserMapper.cleanExpiredUsers(now);
            
            if (count > 0) {
                log.info("清理过期在线用户成功,清理数量: {}", count);
            }
        } catch (Exception e) {
            log.error("清理过期在线用户失败", e);
        }
    }
}
