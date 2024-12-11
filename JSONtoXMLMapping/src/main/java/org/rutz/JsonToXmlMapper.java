package org.rutz;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonToXmlMapper {
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

    private static void writeXmlElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
        String[] xpathParts = mapping.getXPath().split("/");

        if (xpathParts.length == 1) {
            // Extract element name and attribute (if present)
            String elementName = xpathParts[0];

            // Log mapping and current JSON node
            System.out.println("Processing mapping: " + mapping.getXPath());
            System.out.println("JSON Node: " + jsonNode.toPrettyString());

            createXMLStructure(writer, jsonNode, mapping, elementName);

        } else if (xpathParts.length > 1) {
            for (int i = 0; i < xpathParts.length - 1; i++) {
                writer.writeStartElement(xpathParts[i]);
            }

            String elementName = xpathParts[xpathParts.length - 1];

            // Log mapping and current JSON node
            System.out.println("Processing mapping: " + mapping.getXPath());
            System.out.println("JSON Node: " + jsonNode.toPrettyString());

            createXMLStructure(writer, jsonNode, mapping, elementName);

            for (int i = 0; i < xpathParts.length - 1; i++) {
                writer.writeEndElement();
            }
        }
    }

    private static void createXMLStructure(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName) throws Exception {
        if (mapping.isList() && jsonNode.isArray()) {

            System.out.println("jsonNode array count: " + jsonNode.size());

            for (JsonNode listitem : jsonNode) {
                System.out.println("Processing list item: " + elementName);
                writer.writeStartElement(elementName);

                writeAttributes(writer, listitem, mapping);
                // Process child mappings or write direct value
                if (listitem.isValueNode()) {
                    System.out.println("Writing value: " + listitem.asText());
                    writer.writeCharacters(AttributeLevelTransformation.transform(listitem.asText(), mapping));

                } else {
                    // Process child mappings, Recursively process child mappings (if any)
                processChildMappings(writer, listitem, mapping);
            }
                writer.writeEndElement();
                System.out.println("Finised End of list item: " + elementName);
            }

        }
        else if (jsonNode.isObject()) {
            //Hanlding object
            writer.writeStartElement(elementName);
            writeAttributes(writer, jsonNode, mapping);

            if(mapping.getChildMappings() != null && !mapping.getChildMappings().isEmpty()) {
                processChildMappings(writer, jsonNode, mapping);
            } else if(jsonNode.isValueNode()) {
                System.out.println("Writing direct value for element: " + jsonNode.asText());
                writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
            }
            writer.writeEndElement();
            System.out.println("Finised End of object: " + elementName);
        } else if (jsonNode.isValueNode()) {
            //Handling single value node
            writer.writeStartElement(elementName);
            writeAttributes(writer, jsonNode, mapping);
            System.out.println("Writing value: " + jsonNode.asText());
            writer.writeCharacters(AttributeLevelTransformation.transform(jsonNode.asText(), mapping));
            writer.writeEndElement();
            System.out.println("Finised End of value node: " + elementName);
        }
    }

    // Helper method to write attributes
    private static void writeAttributes(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
        if (mapping.getChildMappings() != null) {
            for (Mapping childMapping : mapping.getChildMappings()) {
                if (childMapping.getXPath().contains("@")) {
                    String childPointer = convertJsonPathToJsonPointer(childMapping.getJPath());
                    JsonNode attributeNode = jsonNode.at(childPointer);
                    if (!attributeNode.isMissingNode()) {
                        String attrName = childMapping.getXPath().split("@")[1];
                        System.out.println("Writing attribute: " + attrName + " = " + attributeNode.asText());
                        writer.writeAttribute(attrName, attributeNode.asText());
                    }
                }
            }
        }
    }

    // Helper method to process child mappings
    private static void processChildMappings(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping) throws Exception {
        if (mapping.getChildMappings() != null) {
            for (Mapping childMapping : mapping.getChildMappings()) {
                if (!childMapping.getXPath().contains("@")) { // Skip attributes
                    String childPointer = convertJsonPathToJsonPointer(childMapping.getJPath());
                    JsonNode childNode = jsonNode.at(childPointer);
                    if (!childNode.isMissingNode()) {
                        System.out.println("Processing child mapping: " + childMapping.getXPath());
                        writeXmlElement(writer, childNode, childMapping);
                    } else {
                        System.out.println("Child node missing for: " + childMapping.getXPath());
                    }
                }
            }
        }
    }

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