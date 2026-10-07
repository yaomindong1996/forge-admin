package com.mdframe.forge.starter.plugin.delivery;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static com.mdframe.forge.starter.plugin.delivery.PackagePathRules.require;

final class SourcePomValidator {
    // 交付包始终使用原始 Forge 坐标；分段防止生成工程将协议坐标替换成宿主包名。
    private static final String SOURCE_GROUP = String.join(".", "com", "mdframe", "forge");

    private SourcePomValidator() {
    }

    static void validate(byte[] bytes, String module) {
        require(bytes != null && bytes.length <= 2 * 1024 * 1024, "插件 POM 缺失或超限");
        String text = new String(bytes, StandardCharsets.UTF_8);
        require(!text.matches("(?is).*(<!DOCTYPE|<!ENTITY|<!\\[CDATA\\[).*"), "POM 禁止 DTD/实体/CDATA");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxElementDepth", "64");
            Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes)).getDocumentElement();
            validateRoot(root, module);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("插件 POM XML 无法解析", exception);
        }
    }

    private static void validateRoot(Element root, String module) {
        require(root.getTagName().equals("project") && module.equals(field(root, "artifactId"))
                && child(root, "modules") == null, "POM 必须为声明的单模块");
        Element parent = child(root, "parent");
        require(parent != null && SOURCE_GROUP.equals(field(parent, "groupId"))
                && "forge-server".equals(field(parent, "artifactId"))
                && "${revision}".equals(field(parent, "version")), "POM parent 须继承 Forge 根及 revision");
        String version = field(root, "version");
        String group = field(root, "groupId");
        require(version == null || version.equals("${revision}"), "POM 版本须继承 revision");
        require(group == null || group.equals(SOURCE_GROUP), "POM groupId 非法");
        Element properties = child(root, "properties");
        require(properties == null || child(properties, "revision") == null, "POM 不能覆盖 revision");
    }

    private static String field(Element parent, String name) {
        Element element = child(parent, name);
        if (element == null) {
            return null;
        }
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            require(!(node instanceof Element), "POM 字段不能含嵌套元素");
        }
        return element.getTextContent().trim();
    }

    private static Element child(Element parent, String name) {
        Element result = null;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) {
                require(result == null, "POM 含重复字段：" + name);
                result = element;
            }
        }
        return result;
    }
}
