package com.mdframe.forge.starter.id.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.id.entity.WorkerNodePO;

public interface IWorkNodePOAtomicService extends IService<WorkerNodePO> {

    WorkerNodePO getWorkerNodeByHostPort(String host, String port);

    void addWorkerNode(WorkerNodePO workNode);
}
