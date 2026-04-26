package com.mdframe.forge.plugin.generator.codegen;

import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiPageTemplate;

import java.util.Map;

/**
 * 代码生成策略接口
 * <p>
 * 不同模板类型（TEMPLATE / AI）实现此接口，AiCrudCodegenService 通过策略路由自动选择。
 */
public interface CodegenStrategy {

    /**
     * 是否支持该模板类型
     *
     * @param codegenType 模板的 codegen_type 字段值（TEMPLATE / AI）
     */
    boolean supports(String codegenType);

    /**
     * 生成代码文件（文件路径 -> 文件内容）
     *
     * @param config   AI CRUD 配置
     * @param template 页面模板（可能为 null，当 layoutType 未配置时）
     * @return 文件 Map，key 为 zip 内路径，value 为文件内容
     */
    Map<String, String> generate(AiCrudConfig config, AiPageTemplate template) throws Exception;
}
