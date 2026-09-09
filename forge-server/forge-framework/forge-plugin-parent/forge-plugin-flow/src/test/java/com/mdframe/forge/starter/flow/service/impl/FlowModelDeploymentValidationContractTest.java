package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowModelDeploymentValidationContractTest {

    @Test
    void deploymentMustValidateExecutableBpmnStructureBeforeDeploying() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowModelServiceImpl.java"));
        int deployStart = source.indexOf("public String deployModel(String id, String changeDescription)");
        int repositoryStart = source.indexOf("if (repositoryService == null)", deployStart);
        assertTrue(deployStart >= 0 && repositoryStart > deployStart);
        String preflight = source.substring(deployStart, repositoryStart);
        assertTrue(preflight.contains("validateSequenceFlowRefs"));
        assertTrue(preflight.contains("validateBpmnStructure"));
        assertTrue(preflight.contains("validateExecutableNodesAndGatewayConditions"));
        assertTrue(source.contains("缺少开始节点"));
        assertTrue(source.contains("缺少结束节点"));
        assertTrue(source.contains("悬空连线"));
        assertTrue(source.contains("暂不支持的执行类型"));
        assertTrue(source.contains("未配置处理人、候选用户或候选组"));
        assertTrue(source.contains("缺少条件表达式或默认分支"));
    }
}
