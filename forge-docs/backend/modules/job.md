# 定时任务模块

基于 Quartz 的分布式定时任务模块。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-job</artifactId>
</dependency>
```

## 使用示例

### 注解方式

```java
@Component
public class MyJob {
    
    @ScheduledJob(cron = "0 0 2 * * ?")
    public void execute() {
        // 每天凌晨 2 点执行
        System.out.println("执行定时任务");
    }
}
```

### 实现 JobHandler 接口

```java
@Component
@JobHandler("myJob")
public class MyJobHandler implements JobHandler {
    
    @Override
    public void execute(JobExecutionContext context) {
        // 任务逻辑
    }
}
```

## 注解说明

### @ScheduledJob

```java
@ScheduledJob(
    cron = "0 0 2 * * ?",    // Cron 表达式
    description = "每日统计任务",  // 描述
    enabled = true           // 是否启用
)
```

### @JobHandler

```java
@JobHandler("jobName")  // 任务名称
```

## Cron 表达式

```
秒 分 时 日 月 周
0  0  2  *  *  ?   每天凌晨 2 点
0  */5 *  *  *  ?   每 5 分钟
0  0  12 *  *  ?   每天中午 12 点
0  0  12 ? * WED    每周三中午 12 点
0  0  12 1  *  ?   每月 1 号中午 12 点
```

## 动态管理任务

```java
@Autowired
private QuartzJobExecutor jobExecutor;

// 添加任务
jobExecutor.addJob("myJob", "0 0 2 * * ?", MyJob.class);

// 暂停任务
jobExecutor.pauseJob("myJob");

// 恢复任务
jobExecutor.resumeJob("myJob");

// 删除任务
jobExecutor.deleteJob("myJob");

// 立即执行
jobExecutor.triggerJob("myJob");
```

## 数据库表

Quartz 需要创建以下表：

- QRTZ_JOB_DETAILS
- QRTZ_TRIGGERS
- QRTZ_SIMPLE_TRIGGERS
- QRTZ_CRON_TRIGGERS
- QRTZ_BLOB_TRIGGERS
- QRTZ_CALENDARS
- QRTZ_PAUSED_TRIGGER_GRPS
- QRTZ_FIRED_TRIGGERS
- QRTZ_SCHEDULER_STATE
- QRTZ_LOCKS

## 配置

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never
    properties:
      org:
        quartz:
          scheduler:
            instanceName: ForgeScheduler
            instanceId: AUTO
          jobStore:
            class: org.quartz.impl.jdbcjobstore.JobStoreTX
            driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
            tablePrefix: QRTZ_
            isClustered: true
            clusterCheckinInterval: 10000
```