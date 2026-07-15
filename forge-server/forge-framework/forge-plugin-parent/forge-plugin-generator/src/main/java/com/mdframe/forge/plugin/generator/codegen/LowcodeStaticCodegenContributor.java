package com.mdframe.forge.plugin.generator.codegen;

import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import org.apache.velocity.VelocityContext;
import org.springframework.core.Ordered;

import java.util.Map;

/**
 * 下载后端静态协议编译扩展点。
 *
 * <p>新增低代码后端能力时实现本接口，即可让应用级、访问入口级和 configKey 级下载
 * 自动经过同一编译贡献器。贡献器只允许增加文件，不能覆盖核心生成文件。</p>
 */
public interface LowcodeStaticCodegenContributor extends Ordered {

    /**
     * 稳定能力标识，用于生成清单和契约测试。
     */
    String capabilityId();

    default boolean supports(AiCrudConfig config) {
        return true;
    }

    default void contributeContext(AiCrudConfig config, VelocityContext context) {
        // 默认不增加模板上下文。
    }

    default void contributeFiles(AiCrudConfig config,
                                 VelocityContext context,
                                 Map<String, String> files) {
        // 默认不增加文件。
    }

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
