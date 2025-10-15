package com.simeng.pib.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XML和JSON格式转换工具
 */
@Slf4j
@Component
public class XmlJsonConverter {


    private static XmlMapper createXmlMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        // 配置以匹配 xmltodict 的行为
        xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, false);
        xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 禁用命名空间
        xmlMapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public PropertyName findRootName(AnnotatedClass ac) {
                return PropertyName.USE_DEFAULT;
            }
        });

        return xmlMapper;
    }

    public static String jsonToXml(Object jsonObject, String rootTag) {
        try {
            XmlMapper xmlMapper = createXmlMapper();

            // 创建包装对象
            ObjectNode rootNode = xmlMapper.createObjectNode();
            JsonNode contentNode = xmlMapper.valueToTree(jsonObject);
            rootNode.set(rootTag, contentNode);

            // 转换为 XML
            return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

        } catch (JsonProcessingException e) {
            log.error("JSON to XML conversion failed", e);
            return null;
        }
    }

    // 如果需要，还可以添加 XML 转 JSON 的方法
    public static JsonNode xmlToJson(String xmlString) {
        try {
            XmlMapper xmlMapper = createXmlMapper();
            return xmlMapper.readTree(xmlString);
        } catch (JsonProcessingException e) {
            log.error("XML to JSON conversion failed", e);
            return null;
        }
    }
}
