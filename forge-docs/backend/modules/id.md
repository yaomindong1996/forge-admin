# 分布式 ID 模块

提供分布式唯一 ID 生成能力，支持雪花算法和号段模式。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-id</artifactId>
</dependency>
```

## 配置

```yaml
forge:
  id:
    type: snowflake  # snowflake | segment
    snowflake:
      worker-id: 1
      datacenter-id: 1
    segment:
      step: 1000
```

## 使用示例

### 雪花算法 ID

```java
@Autowired
private IIdService idService;

public Long generateId() {
    return idService.nextId();
}
```

### 业务序列号

```java
@Autowired
private ISequenceService sequenceService;

public String generateOrderNo() {
    // 生成订单号：ORD20240115001
    return sequenceService.nextSequence("ORDER_NO", "ORD", "yyyyMMdd", 3);
}
```

## API 参考

### IIdService

```java
public interface IIdService {
    /**
     * 获取下一个 ID
     */
    long nextId();
}
```

### ISequenceService

```java
public interface ISequenceService {
    /**
     * 获取下一个序列号
     * @param bizType 业务类型
     * @param prefix 前缀
     * @param dateFormat 日期格式
     * @param seqLength 序列号长度
     */
    String nextSequence(String bizType, String prefix, String dateFormat, int seqLength);
    
    /**
     * 获取下一个纯数字序列号
     */
    Long nextSequence(String bizType);
}
```

## ID 生成策略

### 雪花算法 (Snowflake)

- 64 位长整型
- 包含时间戳、机器 ID、序列号
- 支持每毫秒生成 4096 个 ID
- 趋势递增，适合数据库索引

### 号段模式 (Segment)

- 从数据库批量获取 ID 号段
- 内存中分配，减少数据库访问
- 适合高并发场景
- 支持业务隔离

## 配置说明

### 雪花算法配置

```yaml
forge:
  id:
    type: snowflake
    snowflake:
      worker-id: 1      # 工作机器 ID (0-31)
      datacenter-id: 1  # 数据中心 ID (0-31)
```

### 号段模式配置

```yaml
forge:
  id:
    type: segment
    segment:
      step: 1000        # 每次获取的号段大小
```

## 数据库表

号段模式需要创建序列表：

```sql
CREATE TABLE sys_id_sequence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    max_id BIGINT NOT NULL COMMENT '当前最大 ID',
    step INT NOT NULL COMMENT '步长',
    version INT NOT NULL COMMENT '版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz_type (biz_type)
);
```