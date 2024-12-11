package org.rutz;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonToXmlMapper1 {

    // Main method to transform JSON to XML
    public static String transformJsonToXml(String jsonString, List<Mapping> mappings) throws Exception {
        // Parse JSON
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonString);

        // Initialize XML writer
        StringWriter stringWriter = new StringWriter();
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(stringWriter);

        writer.writeStartDocument("UTF-8", "1.0");

        // Process each mapping
        for (Mapping mapping : mappings) {
            String jsonPointer = convertJsonPathToJsonPointer(mapping.getJPath());
            JsonNode jsonValue = rootNode.at(jsonPointer);

            if (!jsonValue.isMissingNode()) {
                writeXmlElement(writer, jsonValue, mapping);
            } else {
                System.out.println("Skipping missing node for: " + mapping.getJPath());
            }
        }

        writer.writeEndDocument();
        writer.close();

        return stringWriter.toString();
    }

    // Process each XML element
    private static void writeXmlElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
        String[] xpathParts = mapping.getXPath().split("/");

        if (xpathParts.length == 1) {
            String elementName = xpathParts[0];
            processElement(writer, jsonNode, mapping, elementName);
        } else {
            // Handle nested elements
            for (int i = 0; i < xpathParts.length - 1; i++) {
                writer.writeStartElement(xpathParts[i]);
            }

            String elementName = xpathParts[xpathParts.length - 1];
            processElement(writer, jsonNode, mapping, elementName);

            for (int i = 0; i < xpathParts.length - 1; i++) {
                writer.writeEndElement();
            }
        }
    }

    // Process each element, including handling objects, lists, and value nodes
    private static void processElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        if (mapping.isList() && jsonNode.isArray()) {
            processArrayElement(writer, jsonNode, mapping, elementName);
        } else if (jsonNode.isObject()) {
            processObjectElement(writer, jsonNode, mapping, elementName);
        } else if (jsonNode.isValueNode()) {
            processValueNode(writer, jsonNode, mapping, elementName);
        }
    }

    // Process JSON arrays as XML list elements
    private static void processArrayElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        System.out.println("jsonNode array count: " + jsonNode.size());

        for (JsonNode listItem : jsonNode) {
            writer.writeStartElement(elementName);
            writeAttributes(writer, listItem, mapping);

            if (listItem.isValueNode()) {
                writer.writeCharacters(AttributeLevelTransformation.transform(listItem.asText(), mapping));
            } else {
                processChildMappings(writer, listItem, mapping);
            }

            writer.writeEndElement();
        }
    }

    // Process JSON objects as XML elements
    private static void processObjectElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        writer.writeStartElement(elementName);
        writeAttributes(writer, jsonNode, mapping);

        if (mapping.getChildMappings() != null && !mapping.getChildMappings().isEmpty()) {
            processChildMappings(writer, jsonNode, mapping);
        } else if (jsonNode.isValueNode()) {
            writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
        }

        writer.writeEndElement();
    }

    // Process JSON value nodes as XML elements
    private static void processValueNode(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        writer.writeStartElement(elementName);
        writeAttributes(writer, jsonNode, mapping);
        writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
        writer.writeEndElement();
    }

    // Helper method to write attributes for elements
    private static void writeAttributes(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
        if (mapping.getChildMappings() != null) {
            for (Mapping childMapping : mapping.getChildMappings()) {
                if (childMapping.getXPath().contains("@")) {
                    String childPointer = convertJsonPathToJsonPointer(childMapping.getJPath());
                    JsonNode attributeNode = jsonNode.at(childPointer);

                    if (!attributeNode.isMissingNode()) {
                        String attrName = childMapping.getXPath().split("@")[1];
                        writer.writeAttribute(attrName, attributeNode.asText());
                    }
                }
            }
        }
    }

    // Process child mappings recursively
    private static void processChildMappings(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
        if (mapping.getChildMappings() != null) {
            for (Mapping childMapping : mapping.getChildMappings()) {
                if (!childMapping.getXPath().contains("@")) {  // Skip attributes
                    String childPointer = convertJsonPathToJsonPointer(childMapping.getJPath());
                    JsonNode childNode = jsonNode.at(childPointer);

                    if (!childNode.isMissingNode()) {
                        writeXmlElement(writer, childNode, childMapping);
                    } else {
                        System.out.println("Child node missing for: " + childMapping.getXPath());
                    }
                }
            }
        }
    }

    // Convert JSONPath to JSON Pointer
    private static String convertJsonPathToJsonPointer(String jsonPath) {
        if (jsonPath.startsWith("$.") && jsonPath.length() > 2) {
            return "/" + jsonPath.substring(2).replace(".", "/").replace("[*]", "");
        } else if (jsonPath.equals("$")) {
            return ""; // Root JSON path
        } else {
            throw new IllegalArgumentException("Invalid JSONPath expression: " + jsonPath);
        }
    }
}
