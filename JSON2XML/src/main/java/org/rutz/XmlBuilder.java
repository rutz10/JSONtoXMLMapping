package org.rutz;

import com.fasterxml.jackson.databind.JsonNode;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class responsible for building XML from JSON data based on field mappings.
 */
public class XmlBuilder {
    private static final Logger logger = Logger.getLogger(XmlBuilder.class.getName());
    private final List<FieldMapping> mappings;
    private final Map<String, List<FieldMapping>> mappingsByGroup;
    private final XMLStreamWriter xmlWriter;
    private final Writer writer;

    public XmlBuilder(List<FieldMapping> mappings, String outputFilePath) throws IOException, XMLStreamException {
        this.mappings = mappings;
        this.mappingsByGroup = mappings.stream()
                .collect(Collectors.groupingBy(FieldMapping::getGroup));
        this.writer = new OutputStreamWriter(new FileOutputStream(outputFilePath), "UTF-8");
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        this.xmlWriter = factory.createXMLStreamWriter(writer);
        logger.info("Initialized XMLStreamWriter.");
    }

    /**
     * Builds the XML document based on the provided JSON data.
     *
     * @param jsonData The root JsonNode of the JSON data.
     * @throws XMLStreamException If an error occurs during XML writing.
     */
    public void buildXml(JsonNode jsonData) throws XMLStreamException {
        logger.info("Starting XML building process.");

        // Define root group
        String rootGroup = "Company";
        List<FieldMapping> rootMappings = mappingsByGroup.get(rootGroup);
        if (rootMappings == null || rootMappings.isEmpty()) {
            logger.severe("No mappings found for root group 'Company'. Aborting XML construction.");
            throw new IllegalArgumentException("No mappings found for root group 'Company'.");
        }

        // Start Document
        xmlWriter.writeStartDocument("UTF-8", "1.0");
        xmlWriter.writeCharacters("\n");

        // Create root element <Company>
        String rootTag = "Company";
        logger.info("Creating root element: <" + rootTag + ">");
        xmlWriter.writeStartElement(rootTag);
        xmlWriter.writeCharacters("\n");

        // Process all mappings in root group
        for (FieldMapping mapping : rootMappings) {
            processField(mapping, jsonData, new ArrayList<>());
        }

        // End root element
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n");

        // End Document
        xmlWriter.writeEndDocument();
        xmlWriter.flush();
        logger.info("Completed XML building process.");
    }

    /**
     * Recursively processes each field based on the mapping.
     *
     * @param mapping    The FieldMapping object.
     * @param jsonNode   The current JsonNode to extract data from.
     * @param parentPath The list representing the current XML path hierarchy.
     * @throws XMLStreamException If an error occurs during XML writing.
     */
    private void processField(FieldMapping mapping, JsonNode jsonNode, List<String> parentPath) throws XMLStreamException {
        String jsonField = mapping.getJsonField();
        String dataType = mapping.getDataType();
        String xmlPath = mapping.getXmlPath();

        if (jsonField == null || jsonField.trim().isEmpty()) {
            // Handle mappings with empty JSON Field (create XML tag based on mapping)
            String[] pathSegments = xmlPath.split("/");
            if (dataType.equalsIgnoreCase("Array")) {
                logger.warning("Array mapping with empty JSON Field is not supported. Skipping mapping: " + mapping);
                return;
            }
            writeStaticElement(pathSegments);
            return;
        }

        JsonNode currentNode = getJsonNode(jsonNode, jsonField);
        if (currentNode == null || currentNode.isMissingNode()) {
            logger.warning("Missing JSON field '" + jsonField + "'. Skipping element.");
            return;
        }

        if (dataType.equalsIgnoreCase("Array")) {
            // Handle array mappings
            String[] pathSegments = xmlPath.split("/");
            if (pathSegments.length < 2) {
                logger.severe("Invalid XML Path '" + xmlPath + "' for Array mapping. Expected format 'Container/Item'. Skipping.");
                return;
            }
            String containerTag = pathSegments[0];
            String itemTag = pathSegments[1];

            // Navigate/Create container element
            xmlWriter.writeStartElement(containerTag);
            xmlWriter.writeCharacters("\n");

            if (currentNode.isArray()) {
                logger.info("Processing array for JSON field '" + jsonField + "' with " + currentNode.size() + " items.");
                for (JsonNode itemNode : currentNode) {
                    xmlWriter.writeStartElement(itemTag);
                    xmlWriter.writeCharacters("\n");

                    // Determine the group of the current item
                    String childGroup = getGroupFromXmlPath(xmlPath);
                    List<FieldMapping> childMappings = mappingsByGroup.get(childGroup);
                    if (childMappings != null) {
                        for (FieldMapping childMapping : childMappings) {
                            processField(childMapping, itemNode, appendToPath(parentPath, containerTag, itemTag));
                        }
                    }

                    xmlWriter.writeEndElement();
                    xmlWriter.writeCharacters("\n");
                }
            } else {
                logger.warning("Expected an array for JSON field '" + jsonField + "', but found none. Skipping.");
            }

            // Close container element
            xmlWriter.writeEndElement();
            xmlWriter.writeCharacters("\n");
        } else {
            // Handle non-array mappings
            String[] pathSegments = xmlPath.split("/");
            String currentTag = pathSegments[pathSegments.length - 1];

            if (pathSegments.length > 1) {
                // Navigate/Create nested elements excluding the last segment
                for (int i = 0; i < pathSegments.length - 1; i++) {
                    String tag = pathSegments[i];
                    xmlWriter.writeStartElement(tag);
                    xmlWriter.writeCharacters("\n");
                }
            }

            // Write the actual element with its value
            logger.info("Writing element: <" + currentTag + "> with value: " + currentNode.asText());
            xmlWriter.writeStartElement(currentTag);
            xmlWriter.writeCharacters(currentNode.asText());
            xmlWriter.writeEndElement();
            xmlWriter.writeCharacters("\n");

            if (pathSegments.length > 1) {
                // Close nested elements
                for (int i = pathSegments.length - 2; i >= 0; i--) {
                    String tag = pathSegments[i];
                    xmlWriter.writeEndElement();
                    xmlWriter.writeCharacters("\n");
                }
            }
        }
    }

    /**
     * Writes a static XML element based on the mapping when JSON Field is empty.
     *
     * @param pathSegments The array of path segments for the XML Path.
     * @throws XMLStreamException If an error occurs during XML writing.
     */
    private void writeStaticElement(String[] pathSegments) throws XMLStreamException {
        String tagName = pathSegments[pathSegments.length - 1];
        logger.info("Writing static element: <" + tagName + ">");
        xmlWriter.writeStartElement(tagName);
        xmlWriter.writeCharacters(""); // Empty content; modify if a default value is desired
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters("\n");
    }

    /**
     * Appends new tags to the current XML path.
     *
     * @param currentPath The current list of XML path segments.
     * @param tags        The new tags to append.
     * @return A new list representing the updated XML path.
     */
    private List<String> appendToPath(List<String> currentPath, String... tags) {
        List<String> newPath = new ArrayList<>(currentPath);
        Collections.addAll(newPath, tags);
        return newPath;
    }

    /**
     * Retrieves a JsonNode based on a dot-separated path.
     *
     * @param root The root JsonNode.
     * @param path The dot-separated path.
     * @return The target JsonNode or null if not found.
     */
    private JsonNode getJsonNode(JsonNode root, String path) {
        if (path == null || path.isEmpty()) {
            logger.warning("getJsonNode: Path is empty. Returning the root node.");
            return root;
        }
        String[] parts = path.split("\\.");
        JsonNode current = root;
        for (String part : parts) {
            if (current == null) {
                logger.warning("getJsonNode: Current node is null while processing part '" + part + "'.");
                return null;
            }
            current = current.get(part);
            if (current == null) {
                logger.warning("getJsonNode: No node found for part '" + part + "'.");
                return null;
            }
        }
        return current;
    }

    /**
     * Extracts the group name from the XML path.
     *
     * @param xmlPath The full XML path.
     * @return The group name.
     */
    private String getGroupFromXmlPath(String xmlPath) {
        String[] segments = xmlPath.split("/");
        if (segments.length < 1) return "";
        return segments[segments.length - 1];
    }

    /**
     * Closes the XMLStreamWriter and underlying writer.
     *
     * @throws XMLStreamException If an error occurs during XML writing.
     * @throws IOException        If an I/O error occurs.
     */
    public void close() throws XMLStreamException, IOException {
        if (xmlWriter != null) {
            xmlWriter.close();
        }
        if (writer != null) {
            writer.close();
        }
        logger.info("Closed XMLStreamWriter and underlying writer.");
    }
}
