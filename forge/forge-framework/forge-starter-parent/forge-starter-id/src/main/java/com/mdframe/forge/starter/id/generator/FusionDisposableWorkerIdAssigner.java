package com.mdframe.forge.starter.id.generator;


import com.mdframe.forge.starter.id.entity.WorkerNodePO;
import com.mdframe.forge.starter.id.service.IWorkNodePOAtomicService;
import com.xfvape.uid.utils.DockerUtils;
import com.xfvape.uid.utils.NetUtils;
import com.xfvape.uid.worker.WorkerIdAssigner;
import com.xfvape.uid.worker.WorkerNodeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;

/**
 * 覆盖实现WorkerIdAssigner  ，将worker分配给UidGenerator后将其丢弃
 *
 * @author haoxd
 */

@Slf4j
public class FusionDisposableWorkerIdAssigner implements WorkerIdAssigner {


    @Autowired
    @Qualifier("workNodePOAtomicServiceImpl")
    private IWorkNodePOAtomicService workNodeService;

    /**
     * Assign worker id base on database.<p>
     * If there is host name & port in the environment, we considered that the node runs in Docker container<br>
     * Otherwise, the node runs on an actual machine.
     *
     * @return assigned worker id
     */
    @Override
    public long assignWorkerId() {
        WorkerNodePO workNode = buildWorkerNode();
        workNodeService.addWorkerNode(workNode);
        log.info("Add worker node:" + workNode);
        return workNode.getWorkNodeId();
    }

    /**
     * Build worker node entity by IP and PORT
     */
    private WorkerNodePO buildWorkerNode() {
        WorkerNodePO workNode = new WorkerNodePO();
        if (DockerUtils.isDocker()) {
            workNode.setType(WorkerNodeType.CONTAINER.value());
            workNode.setHostName(DockerUtils.getDockerHost());
            workNode.setPort(DockerUtils.getDockerPort());
        } else {
            workNode.setType(WorkerNodeType.ACTUAL.value());
            workNode.setHostName(NetUtils.getLocalAddress());
            workNode.setPort(System.currentTimeMillis() + "-" + RandomUtils.nextInt(100000));
        }
        workNode.setLaunchDate(LocalDateTime.now());
        return workNode;
    }

}
