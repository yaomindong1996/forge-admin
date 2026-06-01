package com.mdframe.forge.plugin.generator.service.businessapp;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务对象和字段命名推理服务。
 */
@Service
public class BusinessNamingService {

    private static final Pattern SEGMENT_PATTERN = Pattern.compile("[A-Za-z0-9]+|[\\u4E00-\\u9FFF]+");
    private static final Pattern SAFE_WORD_PATTERN = Pattern.compile("[A-Za-z0-9]+");

    private static final Map<String, String> KNOWN_FIELD_CODES = Map.ofEntries(
            Map.entry("客户名称", "customerName"),
            Map.entry("联系电话", "contactPhone"),
            Map.entry("客户等级", "customerLevel"),
            Map.entry("客户状态", "customerStatus"),
            Map.entry("负责人", "ownerUserId"),
            Map.entry("所属部门", "ownerDeptId"),
            Map.entry("所属地区", "regionCode"),
            Map.entry("所属区域", "regionCode"),
            Map.entry("备注", "remark"),
            Map.entry("客户编码", "customerCode"),
            Map.entry("联系人", "contactName"),
            Map.entry("联系邮箱", "contactEmail"),
            Map.entry("详细地址", "address"),
            Map.entry("跟进状态", "followStatus"),
            Map.entry("创建时间", "createTime"),
            Map.entry("更新时间", "updateTime")
    );

    private static final List<Term> CHINESE_TERMS = List.of(
            term("客户", "customer"), term("联系人", "contact"), term("联系", "contact"),
            term("电话", "phone"), term("手机号", "mobile"), term("手机", "mobile"),
            term("邮箱", "email"), term("邮件", "email"), term("名称", "name"),
            term("姓名", "name"), term("编码", "code"), term("编号", "no"),
            term("代码", "code"), term("等级", "level"), term("级别", "level"),
            term("状态", "status"), term("类型", "type"), term("分类", "category"),
            term("负责人", "ownerUserId"), term("所属", "owner"), term("部门", "dept"),
            term("组织", "org"), term("机构", "org"), term("地区", "region"),
            term("区域", "region"), term("省份", "province"), term("城市", "city"),
            term("地址", "address"), term("详细", "detail"), term("备注", "remark"),
            term("描述", "description"), term("说明", "description"), term("创建", "create"),
            term("更新", "update"), term("时间", "time"), term("日期", "date"),
            term("开始", "start"), term("结束", "end"), term("跟进", "follow"),
            term("行业", "industry"), term("来源", "source"), term("性别", "gender"),
            term("年龄", "age"), term("金额", "amount"), term("价格", "price"),
            term("数量", "quantity"), term("单价", "unitPrice"), term("总价", "totalPrice"),
            term("折扣", "discount"), term("税额", "taxAmount"), term("合同", "contract"),
            term("订单", "order"), term("产品", "product"), term("商品", "product"),
            term("供应商", "supplier"), term("商机", "opportunity"), term("线索", "lead"),
            term("回款", "payment"), term("支付", "payment"), term("付款", "payment"),
            term("收款", "receipt"), term("发票", "invoice"), term("项目", "project"),
            term("任务", "task"), term("计划", "plan"), term("审批", "approval"),
            term("流程", "flow"), term("用户", "user"), term("人员", "user"),
            term("员工", "employee"), term("角色", "role"), term("岗位", "post"),
            term("菜单", "menu"), term("权限", "permission"), term("数据", "data"),
            term("字典", "dict"), term("父级", "parent"), term("上级", "parent"),
            term("下级", "child"), term("子级", "child"), term("是否", "is"),
            term("启用", "enabled"), term("禁用", "disabled"), term("有效", "valid"),
            term("失效", "invalid"), term("排序", "sort"), term("图片", "image"),
            term("附件", "file"), term("文件", "file"), term("标题", "title"),
            term("内容", "content"), term("明细", "detail"), term("详情", "detail"),
            term("主表", "master"), term("子表", "detail"), term("关联", "relation"),
            term("引用", "reference"), term("申请", "apply"), term("审核", "audit"),
            term("通过", "passed"), term("拒绝", "rejected"), term("评分", "score"),
            term("标签", "tag"), term("颜色", "color"), term("规格", "spec"),
            term("单位", "unit"), term("库存", "stock"), term("仓库", "warehouse"),
            term("物流", "logistics"), term("运输", "transport"), term("车牌", "plateNo"),
            term("司机", "driver"), term("车辆", "vehicle"), term("银行", "bank"),
            term("账号", "accountNo"), term("账户", "account"), term("开户行", "bankName"),
            term("管理", "management"), term("选择器", "selector"), term("选择", "select"),
            term("单选", "radio"), term("多选", "checkbox"), term("输入框", "input"),
            term("输入", "input"), term("字段", "field"), term("开关", "switch")
    ).stream().sorted(Comparator.comparingInt((Term term) -> term.source().length()).reversed()).toList();

    public String generateFieldCode(String fieldName) {
        String label = StringUtils.trimToEmpty(fieldName);
        String known = KNOWN_FIELD_CODES.get(label);
        if (StringUtils.isNotBlank(known)) {
            return known;
        }
        List<String> words = inferNameWords(label);
        if (!words.isEmpty()) {
            return toLowerCamel(words);
        }
        return "field" + Integer.toUnsignedString(hash(label), 36);
    }

    public String normalizeFieldCode(String value, String fallbackLabel) {
        String source = StringUtils.defaultIfBlank(value, generateFieldCode(fallbackLabel));
        List<String> words = source.contains("_") ? List.of(source.split("_")) : splitAsciiWords(source);
        String normalized = words.isEmpty() ? generateFieldCode(source) : toLowerCamel(words);
        if (StringUtils.isBlank(normalized)) {
            normalized = "field";
        }
        if (!Character.isLowerCase(normalized.charAt(0))) {
            normalized = "field" + StringUtils.capitalize(normalized);
        }
        return StringUtils.left(normalized, 64);
    }

    public String generateObjectCode(String objectName) {
        List<String> words = inferNameWords(objectName);
        String source = words.isEmpty() ? objectName : String.join("_", words);
        return normalizeSnakeCode(source, "business_object", 48);
    }

    public String normalizeObjectCode(String value, String fallbackName) {
        String source = StringUtils.defaultIfBlank(value, generateObjectCode(fallbackName));
        return normalizeSnakeCode(source, "business_object", 48);
    }

    public String normalizeModelCode(String value, String fallbackName) {
        String source = StringUtils.defaultIfBlank(value, generateObjectCode(fallbackName));
        return normalizeSnakeCode(source, "business_object", 64);
    }

    public String buildModelCode(String suiteCode, String objectCode) {
        String suite = normalizeSnakeCode(suiteCode, "", 24);
        String object = normalizeSnakeCode(objectCode, "business_object", 48);
        if (StringUtils.isBlank(suite) || object.startsWith(suite + "_")) {
            return StringUtils.left(object, 64);
        }
        return StringUtils.left(suite + "_" + object, 64);
    }

    public String camelToSnake(String value) {
        return normalizeSnakeCode(value, "", 64);
    }

    private String normalizeSnakeCode(String value, String fallback, int maxLength) {
        String source = StringUtils.defaultString(value);
        List<String> words = inferNameWords(source);
        String normalized;
        if (words.isEmpty()) {
            normalized = source
                    .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                    .replaceAll("[^A-Za-z0-9_]+", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_+|_+$", "")
                    .toLowerCase(Locale.ROOT);
        } else {
            normalized = String.join("_", words);
        }
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (StringUtils.isBlank(normalized)) {
            normalized = fallback;
        }
        if (StringUtils.isNotBlank(normalized) && !Character.isLetter(normalized.charAt(0))) {
            normalized = StringUtils.defaultIfBlank(fallback, "code") + "_" + normalized;
        }
        return StringUtils.left(StringUtils.defaultIfBlank(normalized, fallback), maxLength).replaceAll("_+$", "");
    }

    private List<String> inferNameWords(String value) {
        List<String> words = new ArrayList<>();
        Matcher matcher = SEGMENT_PATTERN.matcher(StringUtils.defaultString(value));
        while (matcher.find()) {
            String segment = matcher.group();
            if (SAFE_WORD_PATTERN.matcher(segment).matches()) {
                words.addAll(splitAsciiWords(segment));
            } else {
                words.addAll(translateChineseSegment(segment));
            }
        }
        return words.stream()
                .map(this::normalizeWord)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private List<String> translateChineseSegment(String segment) {
        List<String> words = new ArrayList<>();
        int index = 0;
        while (index < segment.length()) {
            Term match = null;
            for (Term term : CHINESE_TERMS) {
                if (segment.startsWith(term.source(), index)) {
                    match = term;
                    break;
                }
            }
            if (match == null) {
                index++;
            } else {
                words.addAll(splitAsciiWords(match.target()));
                index += match.source().length();
            }
        }
        return words;
    }

    private List<String> splitAsciiWords(String value) {
        String normalized = StringUtils.defaultString(value)
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim();
        if (StringUtils.isBlank(normalized)) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    private String normalizeWord(String value) {
        return StringUtils.defaultString(value)
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    private String toLowerCamel(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            String normalized = normalizeWord(word);
            if (StringUtils.isBlank(normalized)) {
                continue;
            }
            if (result.length() == 0) {
                result.append(normalized);
            } else {
                result.append(StringUtils.capitalize(normalized));
            }
        }
        return result.length() == 0 ? "field" : result.toString();
    }

    private int hash(String value) {
        int result = 0;
        for (byte b : StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8)) {
            result = 31 * result + b;
        }
        return result;
    }

    private static Term term(String source, String target) {
        return new Term(source, target);
    }

    private record Term(String source, String target) {
    }
}
