package com.mdframe.forge.starter.id.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.id.entity.WorkerNodePO;
import com.mdframe.forge.starter.id.mapper.WorkerNodePOMapper;
import com.mdframe.forge.starter.id.service.IWorkNodePOAtomicService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service("workNodePOAtomicServiceImpl")
public class IWorkNodePOAtomicServiceImpl extends ServiceImpl<WorkerNodePOMapper, WorkerNodePO> implements IWorkNodePOAtomicService {

    @Override
    public WorkerNodePO getWorkerNodeByHostPort(String host, String port) {
        QueryWrapper<WorkerNodePO> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().eq(WorkerNodePO::getHostName, host).eq(WorkerNodePO::getPort, port);
        return getOne(queryWrapper);
    }

    @Override
    public void addWorkerNode(WorkerNodePO workNode) {
        workNode.setCreated(LocalDateTime.now());
        workNode.setModified(LocalDateTime.now());
        save(workNode);
    }
}
