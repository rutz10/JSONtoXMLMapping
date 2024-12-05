package org.rutz;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class XmlBuilder {
    private static final Logger logger = Logger.getLogger(XmlBuilder.class.getName());
    private Document doc;
    private List<FieldMapping> mappings;
    private Map<String, List<FieldMapping>> mappingsByGroup;

    public XmlBuilder(List<FieldMapping> mappings) throws ParserConfigurationException {
        this.mappings = mappings;
        this.mappingsByGroup = mappings.stream()
                .collect(Collectors.groupingBy(FieldMapping::getGroup));
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        doc = dBuilder.newDocument();
        logger.info("Initialized XML Document.");
    }

    /**
     * Builds the XML document based on the provided JSON data.
     *
     * @param jsonData The root JsonNode of the JSON data.
     */
    public void buildXml(JsonNode jsonData) {
        logger.info("Starting XML building process.");

        // Define root group
        String rootGroup = "Company";
        List<FieldMapping> rootMappings = mappingsByGroup.get(rootGroup);
        if (rootMappings == null || rootMappings.isEmpty()) {
            logger.severe("No mappings found for root group 'Company'. Aborting XML construction.");
            throw new IllegalArgumentException("No mappings found for root group 'Company'.");
        }

        // Create root element <Company>
        String rootTag = "Company";
        logger.info("Creating root element: <" + rootTag + ">");
        Element root = doc.createElement(rootTag);
        doc.appendChild(root);
        logger.info("Created root element: <" + rootTag + ">");

        // Process all mappings in root group
        for (FieldMapping mapping : rootMappings) {
            logger.info("Processing root mapping: JSON Field '" + mapping.getJsonField() + "' -> XML Path '" + mapping.getXmlPath() + "'");
            String[] pathSegments = mapping.getXmlPath().split("/");
            pathSegments = Arrays.stream(pathSegments).filter(s -> !s.isEmpty()).toArray(String[]::new);

            if (mapping.getDataType().equalsIgnoreCase("Array")) {
                // For Array mappings, split into container and item tags
                if (pathSegments.length < 2) {
                    logger.severe("Invalid XML Path '" + mapping.getXmlPath() + "' for Array mapping. Expected format 'Container/Item'. Skipping.");
                    continue;
                }
                String arrayItemTag = pathSegments[pathSegments.length - 1];
                String[] containerPathSegments = Arrays.copyOf(pathSegments, pathSegments.length - 1);
                Element containerElement = createOrGetElement(containerPathSegments, root);
                setElementValue(containerElement, mapping, jsonData, "/" + rootTag + "/" + String.join("/", containerPathSegments), arrayItemTag);
            } else {
                // For non-Array mappings
                Element targetElement = createOrGetElement(pathSegments, root);
                setElementValue(targetElement, mapping, jsonData, "/" + rootTag + "/" + String.join("/", pathSegments));
            }
        }

        logger.info("Completed XML building process.");
    }

    /**
     * Recursively creates or retrieves XML elements based on the path segments.
     *
     * @param pathSegments The array of path segments.
     * @param parent       The parent element.
     * @return The last element in the path.
     */
    private Element createOrGetElement(String[] pathSegments, Element parent) {
        Element currentElement = parent;
        StringBuilder currentPath = new StringBuilder(parent.getNodeName());

        for (String segment : pathSegments) {
            currentPath.append("/").append(segment);
            logger.info("Processing segment: '" + segment + "' under parent: <" + currentElement.getNodeName() + "> at path: " + currentPath.toString());

            Element child = getImmediateChild(currentElement, segment);
            if (child == null) {
                child = doc.createElement(segment);
                currentElement.appendChild(child);
                logger.info("Created element: <" + segment + "> under parent: <" + currentElement.getNodeName() + "> at path: " + currentPath.toString());
            } else {
                logger.info("Element: <" + segment + "> already exists under parent: <" + currentElement.getNodeName() + "> at path: " + currentPath.toString());
            }
            currentElement = child;
        }

        return currentElement;
    }

    /**
     * Finds an immediate child element with the given tag name.
     *
     * @param parent  The parent element.
     * @param tagName The tag name of the child to find.
     * @return The child element if found; otherwise, null.
     */
    private Element getImmediateChild(Element parent, String tagName) {
        if (parent == null) {
            logger.warning("getImmediateChild: Parent is null, cannot find child '" + tagName + "'");
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
                return (Element) child;
            }
        }
        return null;
    }

    /**
     * Sets the value of an XML element based on the JSON data and mapping.
     * This method handles non-array mappings.
     *
     * @param element     The XML element to set the value for.
     * @param mapping     The field mapping.
     * @param jsonData    The root JSON node.
     * @param currentPath The current XML path for logging purposes.
     */
    private void setElementValue(Element element, FieldMapping mapping, JsonNode jsonData, String currentPath) {
        String jsonFieldPath = mapping.getJsonField();
        logger.info("Setting value for element <" + element.getNodeName() + "> based on JSON field '" + jsonFieldPath + "' at XML path: " + currentPath);

        JsonNode valueNode = getJsonNode(jsonData, jsonFieldPath);

        if (valueNode == null || valueNode.isMissingNode()) {
            logger.warning("Value for JSON field '" + jsonFieldPath + "' is missing. Element <" + element.getNodeName() + "> at path: " + currentPath + " will remain empty.");
            return;
        }

        if (mapping.getDataType().equalsIgnoreCase("Array")) {
            // Handle array
            if (valueNode.isArray()) {
                logger.info("Processing array for JSON field '" + jsonFieldPath + "' with " + valueNode.size() + " items.");
                String arrayItemGroup = getGroupFromXmlPath(mapping.getXmlPath());

                for (int i = 0; i < valueNode.size(); i++) {
                    JsonNode arrayItem = valueNode.get(i);
                    String arrayItemTag = getLastPathSegment(mapping.getXmlPath());
                    Element arrayElement = doc.createElement(arrayItemTag);
                    element.appendChild(arrayElement);
                    logger.info("Created array element: <" + arrayItemTag + "> under parent: <" + element.getNodeName() + "> at path: " + currentPath + "/" + arrayItemTag + "[" + (i + 1) + "]");
                    // Recursively handle nested fields using the correct group
                    processNestedFields(arrayItem, arrayItemGroup, arrayElement, currentPath + "/" + arrayItemTag + "[" + (i + 1) + "]");
                }
            } else {
                logger.warning("Expected an array for JSON field '" + jsonFieldPath + "', but found non-array node. Element <" + element.getNodeName() + "> at path: " + currentPath + " will remain empty.");
            }
        } else {
            // Handle simple value
            logger.info("Setting text content for element <" + element.getNodeName() + "> at path: " + currentPath);
            element.setTextContent(valueNode.asText());
            logger.fine("Set content: '" + valueNode.asText() + "' for element <" + element.getNodeName() + "> at path: " + currentPath);
        }
    }

    /**
     * Sets the value of an XML element based on the JSON data and mapping.
     * This method handles array mappings.
     *
     * @param element      The XML element to set the value for.
     * @param mapping      The field mapping.
     * @param jsonData     The root JSON node.
     * @param currentPath  The current XML path for logging purposes.
     * @param arrayItemTag The tag name for array items.
     */
    private void setElementValue(Element element, FieldMapping mapping, JsonNode jsonData, String currentPath, String arrayItemTag) {
        String jsonFieldPath = mapping.getJsonField();
        logger.info("Setting value for element <" + element.getNodeName() + "> based on JSON field '" + jsonFieldPath + "' at XML path: " + currentPath);

        JsonNode valueNode = getJsonNode(jsonData, jsonFieldPath);

        if (valueNode == null || valueNode.isMissingNode()) {
            logger.warning("Value for JSON field '" + jsonFieldPath + "' is missing. Element <" + element.getNodeName() + "> at path: " + currentPath + " will remain empty.");
            return;
        }

        if (mapping.getDataType().equalsIgnoreCase("Array")) {
            // Handle array
            if (valueNode.isArray()) {
                logger.info("Processing array for JSON field '" + jsonFieldPath + "' with " + valueNode.size() + " items.");
                String arrayItemGroup = getGroupFromXmlPath(mapping.getXmlPath());

                for (int i = 0; i < valueNode.size(); i++) {
                    JsonNode arrayItem = valueNode.get(i);
                    Element arrayElement = doc.createElement(arrayItemTag);
                    element.appendChild(arrayElement);
                    logger.info("Created array element: <" + arrayItemTag + "> under parent: <" + element.getNodeName() + "> at path: " + currentPath + "/" + arrayItemTag + "[" + (i + 1) + "]");
                    // Recursively handle nested fields using the correct group
                    processNestedFields(arrayItem, arrayItemGroup, arrayElement, currentPath + "/" + arrayItemTag + "[" + (i + 1) + "]");
                }
            } else {
                logger.warning("Expected an array for JSON field '" + jsonFieldPath + "', but found non-array node. Element <" + element.getNodeName() + "> at path: " + currentPath + " will remain empty.");
            }
        } else {
            // Handle simple value
            logger.info("Setting text content for element <" + element.getNodeName() + "> at path: " + currentPath);
            element.setTextContent(valueNode.asText());
            logger.fine("Set content: '" + valueNode.asText() + "' for element <" + element.getNodeName() + "> at path: " + currentPath);
        }
    }

    /**
     * Recursively processes nested fields based on the group.
     *
     * @param jsonNode      The current JSON node.
     * @param parentGroup   The current group.
     * @param parentElement The current XML parent element.
     * @param currentPath   The current XML path for logging purposes.
     */
    private void processNestedFields(JsonNode jsonNode, String parentGroup, Element parentElement, String currentPath) {
        List<FieldMapping> childMappings = mappingsByGroup.get(parentGroup);
        if (childMappings == null) {
            logger.warning("No mappings found for group '" + parentGroup + "'. Skipping nested fields at path: " + currentPath);
            return;
        }

        for (FieldMapping mapping : childMappings) {
            String jsonFieldPath = mapping.getJsonField();
            logger.info("Processing nested mapping: JSON Field '" + jsonFieldPath + "' -> XML Path '" + mapping.getXmlPath() + "' for group '" + parentGroup + "' at XML path: " + currentPath);

            // Since the JSON Field is relative, use it directly
            String relativePath = jsonFieldPath;
            JsonNode valueNode = getJsonNode(jsonNode, relativePath);

            if (valueNode == null || valueNode.isMissingNode()) {
                logger.warning("Value for JSON field '" + jsonFieldPath + "' is missing in nested fields. Element <" + parentElement.getNodeName() + "> at path: " + currentPath + " will skip this field.");
                continue;
            }

            if (mapping.getDataType().equalsIgnoreCase("Array")) {
                if (valueNode.isArray()) {
                    logger.info("Processing nested array for JSON field '" + jsonFieldPath + "' with " + valueNode.size() + " items.");
                    String arrayItemGroup = getGroupFromXmlPath(mapping.getXmlPath());
                    String arrayItemTag = getLastPathSegment(mapping.getXmlPath());

                    // Create the container element (e.g., <Teams>)
                    String[] containerPathSegments = mapping.getXmlPath().split("/");
                    if (containerPathSegments.length < 2) {
                        logger.severe("Invalid XML Path '" + mapping.getXmlPath() + "' for Array mapping. Expected format 'Container/Item'. Skipping.");
                        continue;
                    }
                    String containerTag = containerPathSegments[containerPathSegments.length - 2];
                    Element containerElement = createOrGetElement(Arrays.copyOf(containerPathSegments, containerPathSegments.length - 1), parentElement);

                    for (int i = 0; i < valueNode.size(); i++) {
                        JsonNode arrayItem = valueNode.get(i);
                        Element arrayElement = doc.createElement(arrayItemTag);
                        containerElement.appendChild(arrayElement);
                        logger.info("Created nested array element: <" + arrayItemTag + "> under parent: <" + containerElement.getNodeName() + "> at path: " + currentPath + "/" + containerTag + "/" + arrayItemTag + "[" + (i + 1) + "]");
                        // Recursive call for deeper nesting
                        processNestedFields(arrayItem, arrayItemGroup, arrayElement, currentPath + "/" + containerTag + "/" + arrayItemTag + "[" + (i + 1) + "]");
                    }
                } else {
                    logger.warning("Expected an array for JSON field '" + jsonFieldPath + "', but found non-array node. Element <" + parentElement.getNodeName() + "> at path: " + currentPath + " will skip this field.");
                }
            } else {
                // Extract the tag name
                String tagName = getLastPathSegment(mapping.getXmlPath());
                Element childElement = doc.createElement(tagName);
                parentElement.appendChild(childElement);
                String childPath = currentPath + "/" + tagName;
                logger.info("Created element: <" + tagName + "> under parent: <" + parentElement.getNodeName() + "> at path: " + childPath);
                // Set the text content
                logger.info("Setting text content for element <" + tagName + "> at path: " + childPath);
                childElement.setTextContent(valueNode.asText());
                logger.fine("Set content: '" + valueNode.asText() + "' for element <" + tagName + "> at path: " + childPath);
            }
        }
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
            logger.fine("getJsonNode: Path is empty. Returning the root node.");
            return root;
        }
        String[] parts = path.split("\\.");
        JsonNode current = root;
        StringBuilder traversedPath = new StringBuilder();
        for (String part : parts) {
            traversedPath.append(part).append(".");
            if (current == null) {
                logger.warning("getJsonNode: Current node is null while processing part '" + part + "'. Traversed path: " + traversedPath.toString());
                return null;
            }
            current = current.get(part);
            if (current == null) {
                logger.warning("getJsonNode: No node found for part '" + part + "'. Traversed path: " + traversedPath.toString());
                return null;
            }
        }
        logger.fine("getJsonNode: Successfully retrieved node for path '" + path + "'.");
        return current;
    }

    /**
     * Extracts the group name from the XML path.
     *
     * @param xmlPath The full XML path.
     * @return The group name.
     */
    private String getGroupFromXmlPath(String xmlPath) {
        return getLastPathSegment(xmlPath);
    }

    /**
     * Extracts the last segment of an XML path.
     *
     * @param xmlPath The full XML path.
     * @return The last segment.
     */
    private String getLastPathSegment(String xmlPath) {
        String[] segments = xmlPath.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (!segments[i].isEmpty()) {
                return segments[i];
            }
        }
        return "";
    }

    /**
     * Saves the constructed XML document to a file.
     *
     * @param outputPath The file path to save the XML.
     * @throws TransformerException If an error occurs during transformation.
     */
    public void saveXml(String outputPath) throws TransformerException {
        logger.info("Saving XML document to: " + outputPath);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        // Beautify the XML output
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-number", "4");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outputPath));

        transformer.transform(source, result);
        logger.info("XML document saved successfully to: " + outputPath);
    }
}
