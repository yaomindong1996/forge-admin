package com.mdframe.forge.starter.id.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@TableName("worker_node")
@Getter
@Setter
public class WorkerNodePO {

    @TableId(value = "work_node_id",type = IdType.AUTO)
    private Long workNodeId;

    @TableField(value = "host_name")
    private String hostName;

    @TableField(value = "port")
    private String port;

    @TableField(value = "type")
    private Integer type;

    @TableField(value = "launch_date")
    private LocalDateTime launchDate;

    @TableField(value = "modified")
    private LocalDateTime modified;

    @TableField(value = "created")
    private LocalDateTime created;
}
