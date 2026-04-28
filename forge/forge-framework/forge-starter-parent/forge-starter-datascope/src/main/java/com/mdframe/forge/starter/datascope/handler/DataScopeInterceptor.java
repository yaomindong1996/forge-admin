package com.mdframe.forge.starter.datascope.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.enums.DataScopeType;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 数据权限拦截器 基于 MyBatis Plus InnerInterceptor 实现
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {
    
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        
        // 0. 检查是否跳过数据权限控制（用于后台任务场景）
        if (DataScopeContextHolder.isSkip()) {
            log.debug("数据权限拦截器：已设置跳过标记，跳过权限控制");
            return;
        }
        // 2. 获取Mapper方法全路径
        String mapperId = ms.getId();
        log.info("mapperId:{}", mapperId);
        if (mapperId.contains("com.mdframe.forge.starter.datascope.mapper")) {
            return;
        }
        IDataScopeService dataScopeService = SpringUtil.getBean(IDataScopeService.class);
        
        // 1. 获取当前用户数据权限上下文
        DataScopeContext context = null;
        try {
            context = dataScopeService.getCurrentUserDataScope();
        } catch (Exception e) {
            // 获取用户上下文失败（可能是后台任务），跳过权限控制
            //log.debug("数据权限拦截器：获取用户上下文失败，可能是后台任务，跳过权限控制", e);
            return;
        }
        
        if (context == null || context.getUserId() == null) {
            log.debug("数据权限拦截器：未获取到用户信息，跳过权限控制");
            return;
        }
        
        // 3. 处理分页count查询（方法名以_mpCount或_COUNT结尾）
        // 需要根据原方法名查询配置
        String actualMapperId = mapperId;
        if (mapperId.endsWith("_mpCount") || mapperId.endsWith("_COUNT")) {
            // 去掉_mpCount或_COUNT后缀，获取原方法名
            actualMapperId = mapperId.replaceAll("(_mpCount|_COUNT)$", "");
            log.debug("数据权限拦截器：检测到分页count查询，原方法: {}", actualMapperId);
        }
        
        // 4. 查询该方法的数据权限配置
        SysDataScopeConfig config = dataScopeService.getDataScopeConfig(actualMapperId);
        if (config == null || config.getEnabled() == 0) {
            log.debug("数据权限拦截器：方法 {} 未配置数据权限或已禁用", actualMapperId);
            return;
        }
        
        // 5. 判断数据权限类型
        DataScopeType scopeType = DataScopeType.getByCode(context.getMinDataScope());
        if (scopeType == null) {
            log.warn("数据权限拦截器：未知的数据权限类型 {}", context.getMinDataScope());
            return;
        }
        
        // 6. 全部数据权限，直接放行；行政区划权限中省级(level=1)也等同全部
        if (scopeType == DataScopeType.ALL) {
            log.debug("数据权限拦截器：用户拥有全部数据权限，跳过SQL改写");
            return;
        }
        if (scopeType == DataScopeType.REGION && Integer.valueOf(1).equals(context.getRegionLevel())) {
            log.debug("数据权限拦截器：用户为省级行政区划，视为全部数据权限，跳过SQL改写");
            return;
        }
        
        // 7. 改写SQL
        try {
            String originalSql = boundSql.getSql();
            String modifiedSql = buildDataScopeSql(originalSql, config, context, scopeType);
            
            // 使用反射修改BoundSql中的sql
            PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
            mpBoundSql.sql(modifiedSql);
            
            log.debug("数据权限拦截器：原始SQL: {}", originalSql);
            log.debug("数据权限拦截器：改写SQL: {}", modifiedSql);
            
        } catch (Exception e) {
            log.error("数据权限拦截器：SQL改写失败", e);
        }
    }
    
    /**
     * 构建数据权限SQL
     */
    private String buildDataScopeSql(String originalSql, SysDataScopeConfig config, DataScopeContext context,
            DataScopeType scopeType) throws Exception {
        
        // 解析SQL
        Statement statement = CCJSqlParserUtil.parse(originalSql);
        if (!(statement instanceof Select)) {
            return originalSql;
        }
        
        Select select = (Select) statement;
        Select selectBody = select.getSelectBody();
        
        if (!(selectBody instanceof PlainSelect)) {
            return originalSql;
        }
        
        PlainSelect plainSelect = (PlainSelect) selectBody;
        Expression where = plainSelect.getWhere();
        
        // 构建数据权限条件
        Expression dataScopeCondition = buildDataScopeCondition(config, context, scopeType);
        
        if (dataScopeCondition != null) {
            if (where != null) {
                // 原有WHERE条件存在，使用AND连接
                AndExpression andExpression = new AndExpression(where, dataScopeCondition);
                plainSelect.setWhere(andExpression);
            } else {
                // 原有WHERE条件不存在，直接设置
                plainSelect.setWhere(dataScopeCondition);
            }
        }
        
        return select.toString();
    }
    
    /**
     * 构建数据权限条件表达式
     */
    private Expression buildDataScopeCondition(SysDataScopeConfig config, DataScopeContext context,
            DataScopeType scopeType) {
        
        String tableAlias = config.getTableAlias();
        String userIdColumn = config.getUserIdColumn();
        String orgIdColumn = config.getOrgIdColumn();
        String tenantIdColumn = config.getTenantIdColumn();
        IDataScopeService dataScopeService = SpringUtil.getBean(IDataScopeService.class);
        
        switch (scopeType) {
            case SELF:
                // 本人数据权限
                return buildColumnCondition(tableAlias, userIdColumn, context, scopeType, null, null);
            
            case ORG:
                // 本组织数据权限
                if (context.getOrgIds() != null && !context.getOrgIds().isEmpty()) {
                    return buildColumnCondition(tableAlias, orgIdColumn, context, scopeType, context.getOrgIds(), null);
                }
                break;
            
            case ORG_AND_CHILD:
                // 本组织及子组织数据权限
                Set<Long> allOrgIds = dataScopeService.getOrgAndChildIds(context.getOrgIds());
                if (allOrgIds != null && !allOrgIds.isEmpty()) {
                    return buildColumnCondition(tableAlias, orgIdColumn, context, scopeType, new ArrayList<>(allOrgIds), null);
                }
                break;
            
            case CUSTOM:
                // 自定义数据权限
                if (context.getCustomOrgIds() != null && !context.getCustomOrgIds().isEmpty()) {
                    return buildColumnCondition(tableAlias, orgIdColumn, context, scopeType, null, context.getCustomOrgIds());
                }
                break;
            
            case TENANT_ALL:
                // 租户全部数据权限
                if (tenantIdColumn != null && !tenantIdColumn.isEmpty()) {
                    return buildColumnCondition(tableAlias, tenantIdColumn, context, scopeType, null, null);
                }
                break;

            case REGION:
                // 本行政区划数据权限
                return buildRegionCondition(config, context);
            
            default:
                break;
        }
        
        return null;
    }
    
    /**
     * 构建字段条件（支持简单字段和复杂SQL）
     *
     * @param tableAlias   表别名
     * @param columnConfig 字段配置（可能是简单字段或 <sql>开头的复杂SQL）
     * @param context      数据权限上下文
     * @param scopeType    权限范围类型（用于判断使用哪个值）
     * @param orgIds       组织ID列表（用于 ORG 类型）
     * @param customOrgIds 自定义组织ID列表（用于 CUSTOM 类型）
     */
    private Expression buildColumnCondition(String tableAlias, String columnConfig, DataScopeContext context,
            DataScopeType scopeType, List<Long> orgIds, Set<Long> customOrgIds) {
        if (columnConfig == null || columnConfig.isEmpty()) {
            return null;
        }
        
        // 检查是否为复杂SQL表达式（以 <sql> 开头）
        if (columnConfig.trim().startsWith("<sql>")) {
            // 去掉 <sql> 标签，提取真实SQL
            String sqlExpression = columnConfig.trim().substring(5).trim();
            return buildCustomSqlCondition(sqlExpression, context);
        }
        
        // 简单字段模式
        // 如果有 orgIds 或 customOrgIds，使用 IN 条件
        if (orgIds != null && !orgIds.isEmpty()) {
            return buildInCondition(tableAlias, columnConfig, orgIds);
        }
        if (customOrgIds != null && !customOrgIds.isEmpty()) {
            return buildInCondition(tableAlias, columnConfig, customOrgIds);
        }
        
        // 否则使用等值条件（用于 SELF 和 TENANT_ALL）
        // 根据权限范围类型决定使用哪个值
        Long value = switch (scopeType) {
            case SELF ->
                // 本人数据权限：使用 userId
                    context.getUserId();
            case TENANT_ALL ->
                // 租户全部数据权限：使用 tenantId
                    context.getTenantId();
            default -> {
                // 其他情况不应该走到这里，因为 ORG/ORG_AND_CHILD/CUSTOM 都会传入 orgIds 或 customOrgIds
                log.warn("数据权限拦截器：未预期的权限范围类型: {}", scopeType);
                yield context.getUserId();
            }
        };
        
        return value != null ? buildEqualsCondition(tableAlias, columnConfig, value) : null;
    }
    
    /**
     * 构建自定义SQL条件表达式 支持占位符：#{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}
     */
    private Expression buildCustomSqlCondition(String customSql, DataScopeContext context) {
        try {
            // 替换占位符
            String processedSql = replaceCustomSqlPlaceholders(customSql, context);
            
            // 解析为 Expression
            Expression expression = CCJSqlParserUtil.parseCondExpression(processedSql);
            
            log.debug("数据权限拦截器：自定义SQL条件 - 原始: {}", customSql);
            log.debug("数据权限拦截器：自定义SQL条件 - 处理后: {}", processedSql);
            
            return expression;
        } catch (Exception e) {
            log.error("数据权限拦截器：解析自定义SQL条件失败: {}", customSql, e);
            return null;
        }
    }
    
    /**
     * 替换自定义SQL中的占位符
     */
    private String replaceCustomSqlPlaceholders(String sql, DataScopeContext context) {
        String result = sql;
        
        // 替换 #{userId}
        if (context.getUserId() != null) {
            result = result.replace("#{userId}", context.getUserId().toString());
        }
        
        // 替换 #{tenantId}
        if (context.getTenantId() != null) {
            result = result.replace("#{tenantId}", context.getTenantId().toString());
        }
        
        // 替换 #{orgIds}
        if (context.getOrgIds() != null && !context.getOrgIds().isEmpty()) {
            String orgIdsStr = context.getOrgIds().stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
            result = result.replace("#{orgIds}", orgIdsStr);
        }
        
        // 替换 #{customOrgIds}
        if (context.getCustomOrgIds() != null && !context.getCustomOrgIds().isEmpty()) {
            String customOrgIdsStr = context.getCustomOrgIds().stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
            result = result.replace("#{customOrgIds}", customOrgIdsStr);
        }

        // 替换 #{regionCode}
        if (context.getRegionCode() != null) {
            result = result.replace("#{regionCode}", context.getRegionCode());
        }

        // 替换 #{regionLevel}
        if (context.getRegionLevel() != null) {
            result = result.replace("#{regionLevel}", context.getRegionLevel().toString());
        }

        // 替换 #{regionAncestors}
        if (context.getRegionAncestors() != null && !context.getRegionAncestors().isEmpty()) {
            result = result.replace("#{regionAncestors}", context.getRegionAncestors());
        }

        return result;
    }
    
    /**
     * 构建等值条件：column = value
     */
    private Expression buildEqualsCondition(String tableAlias, String columnName, Object value) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(
                new Column(StrUtil.isNotBlank(tableAlias) ? tableAlias + "." + columnName : columnName));
        equalsTo.setRightExpression(new LongValue(value.toString()));
        return equalsTo;
    }
    
    /**
     * 构建IN条件：column IN (value1, value2, ...)
     */
    private Expression buildInCondition(String tableAlias, String columnName, Iterable<?> values) {
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(
                new Column(StrUtil.isNotBlank(tableAlias) ? tableAlias + "." + columnName : columnName));
        
        // 构建值列表
        java.util.List<Expression> expressions = new java.util.ArrayList<>();
        for (Object value : values) {
            expressions.add(new LongValue(value.toString()));
        }
        
        ExpressionList expressionList = new ExpressionList();
        expressionList.setExpressions(expressions);
        
        // 使用ItemsList的实现类
        inExpression.setRightExpression(expressionList);
        
        return inExpression;
    }

    // =========================== 行政区划(REGION) 数据权限 ===========================

    /**
     * 构建行政区划数据权限条件
     */
    private Expression buildRegionCondition(SysDataScopeConfig config, DataScopeContext context) {
        String regionCodeColumn = config.getRegionCodeColumn();
        if (regionCodeColumn == null || regionCodeColumn.isEmpty()) {
            log.warn("数据权限拦截器：REGION 权限类型但 regionCodeColumn 未配置");
            return null;
        }

        String regionCode = context.getRegionCode();
        if (regionCode == null || regionCode.isEmpty()) {
            log.debug("数据权限拦截器：REGION 权限类型但用户无 regionCode，返回恒真条件");
            return buildAlwaysTrue();
        }

        String tableAlias = config.getTableAlias();

        // 构建主表行政区划条件
        Expression orgCondition = buildRegionSimpleCondition(tableAlias, regionCodeColumn, regionCode, context);

        // 如果配置了用户表行政区划字段（JOIN 场景：同时匹配组织/用户 area_code）
        String userRegionColumn = config.getUserRegionColumn();
        String userTableAlias = config.getUserTableAlias();
        Expression userCondition = null;
        if (userRegionColumn != null && !userRegionColumn.isEmpty()) {
            userCondition = buildRegionSimpleCondition(userTableAlias, userRegionColumn, regionCode, context);
        }

        if (orgCondition != null && userCondition != null) {
            // 两者 OR 连接
            return new OrExpression(wrapWithParentheses(orgCondition), wrapWithParentheses(userCondition));
        }
        return orgCondition;
    }

    /**
     * 构建单列的行政区划条件（精确匹配 + 下级区划 IN）
     */
    private Expression buildRegionSimpleCondition(String tableAlias, String column, String regionCode, DataScopeContext context) {
        String fullColumnName = StrUtil.isNotBlank(tableAlias) ? tableAlias + "." + column : column;

        // 检查是否为复杂 SQL 模板
        if (column.trim().startsWith("<sql>")) {
            String sqlTemplate = column.trim().substring(5).trim();
            return buildCustomSqlCondition(sqlTemplate, context);
        }

        // 简单模式：精确匹配 + 下级区划 IN（市级/区级及以下）
        Integer level = context.getRegionLevel();
        if (level != null && level >= 2) {
            return buildRegionWithChildCondition(fullColumnName, regionCode);
        }

        // 省级（level=1）在入口已被跳过，此处仅处理无 level 兜底
        return buildEqualsCondition(tableAlias, column, regionCode);
    }

    /**
     * 构建 本级 OR 下级 IN 条件：column = 'XXX' OR column IN (SELECT code FROM sys_region_code WHERE parent_code = 'XXX')
     */
    private Expression buildRegionWithChildCondition(String fullColumnName, String regionCode) {
        // 本级匹配
        EqualsTo eq = new EqualsTo();
        eq.setLeftExpression(new Column(fullColumnName));
        eq.setRightExpression(new StringValue(regionCode));

        // 下级子查询
        String subQuerySql = "SELECT code FROM sys_region_code WHERE parent_code = '" + regionCode + "'";
        try {
            ParenthesedSelect parenthesedSelect = new ParenthesedSelect();
            Select parsed = (Select) CCJSqlParserUtil.parse(subQuerySql);
            parenthesedSelect.setSelect(parsed);

            InExpression inExp = new InExpression();
            inExp.setLeftExpression(new Column(fullColumnName));
            inExp.setRightExpression(parenthesedSelect);

            return new OrExpression(eq, wrapWithParentheses(inExp));
        } catch (Exception e) {
            log.error("数据权限拦截器：构建行政区划子查询失败，降级为精确匹配", e);
            return eq;
        }
    }

    /**
     * 构建恒真条件 1=1（用于无需限制的场景）
     */
    private Expression buildAlwaysTrue() {
        EqualsTo eq = new EqualsTo();
        eq.setLeftExpression(new LongValue(1));
        eq.setRightExpression(new LongValue(1));
        return eq;
    }

    /**
     * 用括号包裹表达式（用于 OR 连接时避免优先级问题）
     */
    private Expression wrapWithParentheses(Expression expression) {
        if (expression instanceof Parenthesis) {
            return expression;
        }
        Parenthesis p = new Parenthesis();
        p.setExpression(expression);
        return p;
    }
}

