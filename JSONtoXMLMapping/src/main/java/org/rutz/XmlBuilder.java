package org.rutz;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.*;

/**
 * XmlBuilder is responsible for converting API response objects into XML format
 * based on field mappings defined in an Excel file.
 */
public class XmlBuilder {

    private static final Logger logger = LoggerFactory.getLogger(XmlBuilder.class);

    /**
     * Converts the API response object to XML based on the provided mappings and writes it to a file.
     *
     * @param mappings       List of XmlMapping defining the field mappings.
     * @param apiResponse    The API response object.
     * @param outputFilePath The file path where the XML will be written.
     * @throws Exception if an error occurs during conversion or file writing.
     */
    public static void buildXml(List<ExcelMappingReader.XmlMapping> mappings, Object apiResponse, String outputFilePath) throws Exception {
        logger.info("Starting XML build process.");
        Document document;
        try {
            // Initialize XML Document
            document = createNewDocument();

            // Determine root element from the first mapping
            String rootPath = getRootPath(mappings);
            String rootElementName = getLastPathSegment(rootPath);
            Element rootElement = document.createElement(rootElementName);
            document.appendChild(rootElement);
            logger.debug("Created root element: <{}>", rootElementName);

            // Group mappings by their "Group" column
            Map<String, List<ExcelMappingReader.XmlMapping>> groupMap = groupMappingsByGroup(mappings);
            logger.debug("Grouped mappings by group. Total groups: {}", groupMap.size());

            // Process each group
            for (Map.Entry<String, List<ExcelMappingReader.XmlMapping>> entry : groupMap.entrySet()) {
                String group = entry.getKey();
                List<ExcelMappingReader.XmlMapping> groupMappings = entry.getValue();
                logger.info("Processing group: {}", group);

                try {
                    if (group.equalsIgnoreCase("Company")) {
                        // Handle 'Company' as a single object
                        handleSingleObjectGroup(document, rootElement, groupMappings, apiResponse);
                    } else {
                        // Handle other groups as collections
                        handleCollectionGroup(document, rootElement, groupMappings, apiResponse, rootPath);
                    }
                } catch (Exception e) {
                    logger.error("Error processing group '{}': {}", group, e.getMessage(), e);
                }
            }

            logger.info("XML build process completed successfully.");
        } catch (Exception e) {
            logger.error("Error during XML build: {}", e.getMessage(), e);
            throw e;
        }

        // Convert Document to String and write to file
        try {
            String xmlString = transformDocumentToString(document);
            logger.debug("Generated XML:\n{}", xmlString);
            writeXmlToFile(xmlString, outputFilePath);
            logger.info("XML successfully written to file: {}", outputFilePath);
        } catch (Exception e) {
            logger.error("Failed to write XML to file '{}': {}", outputFilePath, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Creates a new XML Document.
     */
    private static Document createNewDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Optional: factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    /**
     * Determines the root path from the first mapping.
     */
    private static String getRootPath(List<ExcelMappingReader.XmlMapping> mappings) {
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("No mappings provided.");
        }
        String rootPath = mappings.get(0).getXmlPath().split("/")[0];
        logger.debug("Determined root path: {}", rootPath);
        return rootPath;
    }

    /**
     * Gets the last segment of an XML path.
     */
    private static String getLastPathSegment(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Groups mappings by their "Group" column.
     */
    private static Map<String, List<ExcelMappingReader.XmlMapping>> groupMappingsByGroup(List<ExcelMappingReader.XmlMapping> mappings) {
        Map<String, List<ExcelMappingReader.XmlMapping>> grouped = new HashMap<>();
        for (ExcelMappingReader.XmlMapping mapping : mappings) {
            grouped.computeIfAbsent(mapping.getGroup(), k -> new ArrayList<>()).add(mapping);
        }
        return grouped;
    }

    /**
     * Handles groups that are single objects (e.g., Company).
     */
    private static void handleSingleObjectGroup(Document document, Element rootElement, List<ExcelMappingReader.XmlMapping> groupMappings, Object apiResponse) throws Exception {
        logger.info("Handling single object group: Company");

        for (ExcelMappingReader.XmlMapping mapping : groupMappings) {
            logger.debug("Processing Mapping - Group: {}, API Field: {}, XML Path: {}",
                    mapping.getGroup(), mapping.getApiFieldName(), mapping.getXmlPath());
            String xmlElementName = getLastPathSegment(mapping.getXmlPath());
            String apiFieldName = mapping.getApiFieldName();
            String xmlDataType = mapping.getXmlDataType();
            Object fieldValue = getFieldValue(apiResponse, apiFieldName);

            if (fieldValue == null) {
                logger.warn("Field '{}' in Company is null. Skipping XML element '{}'.", apiFieldName, xmlElementName);
                continue;
            }

            String convertedValue;
            try {
                convertedValue = convertToXmlDataType(fieldValue, xmlDataType);
            } catch (Exception e) {
                logger.error("Error converting field '{}' to XML Data Type '{}': {}", apiFieldName, xmlDataType, e.getMessage());
                continue; // Skip this field if conversion fails
            }

            // Create and append the XML element with the converted value
            Element element = document.createElement(xmlElementName);
            element.appendChild(document.createTextNode(convertedValue));
            rootElement.appendChild(element);
            logger.debug("Added XML element <{}> with value '{}'.", xmlElementName, convertedValue);
        }
    }

    /**
 * Handles groups that are collections (e.g., Branch, Team, Member).
 */
private static void handleCollectionGroup(Document document, Element rootElement,
                                          List<ExcelMappingReader.XmlMapping> groupMappings,
                                          Object apiResponse, String rootPath) throws Exception {
    String groupName = groupMappings.get(0).getGroup();
    logger.info("Handling collection group: {}", groupName);

    Optional<ExcelMappingReader.XmlMapping> collectionMappingOpt = groupMappings.stream()
            .filter(m -> m.getApiDataType().startsWith("List<"))
            .findFirst();

    if (!collectionMappingOpt.isPresent()) {
        logger.warn("No collection mapping found in group '{}'. Skipping group.", groupName);
        return;
    }

    String basePath = collectionMappingOpt.get().getXmlPath();
    if (!basePath.startsWith(rootPath + "/")) {
        logger.warn("Group base path '{}' does not start with root path '{}'. Skipping group '{}'.", basePath, rootPath, groupName);
        return;
    }

    String groupRelativePath = basePath.substring(rootPath.length() + 1);
    List<Object> collectionObjects;
    try {
        collectionObjects = extractObjects(apiResponse, groupRelativePath);
        if (collectionObjects == null || collectionObjects.isEmpty()) {
            logger.warn("No objects found at path '{}'. Skipping group '{}'.", groupRelativePath, groupName);
            return;
        }
    } catch (NoSuchFieldException nsfe) {
        logger.error("Failed to extract collection object: {}", nsfe.getMessage());
        return;
    }

    Element parentContainer = getOrCreateElement(document, rootElement, getParentPath(groupRelativePath));
    for (Object item : collectionObjects) {
        if (item == null) {
            logger.warn("Encountered a null object in collection '{}'. Skipping.", groupRelativePath);
            continue;
        }

        String collectionElementName = getLastPathSegment(groupRelativePath);
        Element itemElement = document.createElement(collectionElementName);
        parentContainer.appendChild(itemElement);

        for (ExcelMappingReader.XmlMapping subMapping : groupMappings) {
            String subXmlPath = subMapping.getXmlPath();
            if (!subXmlPath.startsWith(basePath + "/")) {
                continue;
            }

            String subRelativePath = subXmlPath.substring((basePath + "/").length());
            if (subRelativePath.contains("/")) {
                continue;
            }

            String xmlElementName = getLastPathSegment(subRelativePath);
            String subApiFieldName = subMapping.getApiFieldName();
            String subXmlDataType = subMapping.getXmlDataType();

            Object fieldValue;
            try {
                fieldValue = getFieldValue(item, subApiFieldName);
                if (fieldValue == null) {
                    continue;
                }
            } catch (NoSuchFieldException nsfe) {
                continue;
            }

            if (subMapping.getApiDataType().startsWith("List<")) {
                if (!(fieldValue instanceof List<?>)) {
                    continue;
                }

                List<?> list = (List<?>) fieldValue;
                for (Object listItem : list) {
                    if (listItem == null) {
                        continue;
                    }

                    String convertedValue;
                    try {
                        convertedValue = convertToXmlDataType(listItem, subXmlDataType);
                    } catch (Exception e) {
                        continue;
                    }

                    Element element = document.createElement(xmlElementName);
                    element.appendChild(document.createTextNode(convertedValue));
                    itemElement.appendChild(element);
                }
            } else {
                String convertedValue;
                try {
                    convertedValue = convertToXmlDataType(fieldValue, subXmlDataType);
                } catch (Exception e) {
                    continue;
                }

                Element element = document.createElement(xmlElementName);
                element.appendChild(document.createTextNode(convertedValue));
                itemElement.appendChild(element);
            }
        }
    }
}
    /**
     * Extracts objects from the API response based on the XML Path.
     *
     * @param apiResponse The API response object.
     * @param xmlPath     The XML path to extract objects from.
     * @return A list of extracted objects.
     * @throws Exception if an error occurs during extraction.
     */
    private static List<Object> extractObjects(Object apiResponse, String xmlPath) throws Exception {
        logger.info("Starting extraction of objects with XML Path: '{}'", xmlPath);

        String[] pathSegments = xmlPath.split("/");
        logger.debug("XML Path split into segments: {}", Arrays.toString(pathSegments));

        List<Object> currentObjects = new ArrayList<>();
        currentObjects.add(apiResponse);
        logger.debug("Initial object list contains: {}", apiResponse != null ? apiResponse.getClass().getName() : "null");

        for (String segment : pathSegments) {
            logger.info("Attempting to access field '{}' on {} objects.", segment, currentObjects.size());

            List<Object> nextObjects = new ArrayList<>();

            for (Object obj : currentObjects) {
                if (obj == null) {
                    logger.warn("Current object is null. Cannot access field '{}'. Skipping.", segment);
                    continue;
                }

                logger.info("Retrieving value for field '{}' from object of type '{}'", segment, obj.getClass().getName());

                try {
                    Object fieldValue = getFieldValue(obj, segment);
                    if (fieldValue == null) {
                        logger.warn("Field '{}' in object of type '{}' is null. Skipping.", segment, obj.getClass().getName());
                        continue;
                    }

                    if (fieldValue instanceof List<?>) {
                        List<?> list = (List<?>) fieldValue;
                        logger.debug("Field '{}' is a List with {} items.", segment, list.size());
                        nextObjects.addAll(list);
                    } else {
                        logger.debug("Field '{}' is a single object of type '{}'.", segment, fieldValue.getClass().getName());
                        nextObjects.add(fieldValue);
                    }
                } catch (NoSuchFieldException nsfe) {
                    logger.error("Field '{}' not found in class '{}'. Exception: {}", segment, obj.getClass().getName(), nsfe.getMessage());
                    throw nsfe;
                } catch (Exception e) {
                    logger.error("Error accessing field '{}' in class '{}'. Exception: {}", segment, obj.getClass().getName(), e.getMessage());
                    throw e;
                }
            }

            currentObjects = nextObjects;
            logger.debug("After accessing field '{}', number of current objects: {}", segment, currentObjects.size());

            if (currentObjects.isEmpty()) {
                logger.warn("No objects found after accessing field '{}'. Terminating extraction.", segment);
                break;
            }
        }

        logger.info("Completed extraction. Total objects extracted: {}", currentObjects.size());
        return currentObjects;
    }

    /**
     * Retrieves the value of a field from an object using reflection.
     */
    private static Object getFieldValue(Object obj, String fieldName) throws Exception {
        logger.info("Retrieving value for field '{}' from object of type '{}'", fieldName,
                obj != null ? obj.getClass().getName() : "null");

        if (obj == null) {
            logger.warn("Provided object is null. Cannot retrieve field '{}'.", fieldName);
            return null;
        }

        Field field = getField(obj.getClass(), fieldName);

        if (field == null) {
            logger.error("Field '{}' not found in class '{}'.", fieldName, obj.getClass().getName());
            throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + obj.getClass().getName());
        }

        field.setAccessible(true);
        Object value;
        try {
            value = field.get(obj);
            logger.debug("Value of field '{}' retrieved successfully: {}", fieldName, value);
        } catch (IllegalAccessException iae) {
            logger.error("Illegal access when retrieving field '{}': {}", fieldName, iae.getMessage());
            throw iae;
        }

        return value;
    }

    /**
     * Recursively searches for a field in a class and its superclasses.
     */
    private static Field getField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            logger.warn("Reached top of class hierarchy. Field '{}' not found.", fieldName);
            return null;
        }

        logger.debug("Searching for field '{}' in class '{}'", fieldName, clazz.getName());

        try {
            Field field = clazz.getDeclaredField(fieldName);
            logger.debug("Field '{}' found in class '{}'", fieldName, clazz.getName());
            return field;
        } catch (NoSuchFieldException e) {
            logger.debug("Field '{}' not found in class '{}'. Checking superclass.", fieldName, clazz.getName());
            return getField(clazz.getSuperclass(), fieldName);
        }
    }

    /**
     * Retrieves all fields from a class and its superclasses.
     */
    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    /**
     * Checks if a class is a primitive type or its wrapper.
     */
    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == Boolean.class ||
                type == Byte.class ||
                type == Character.class ||
                type == Short.class ||
                type == Integer.class ||
                type == Long.class ||
                type == Float.class ||
                type == Double.class ||
                type == Void.class;
    }

    /**
     * Gets the parent path of a given XML path.
     * E.g., for 'branches/teams/members/tasks', returns 'branches/teams/members'
     */
    private static String getParentPath(String xmlPath) {
        int lastSlash = xmlPath.lastIndexOf('/');
        if (lastSlash == -1) return xmlPath;
        return xmlPath.substring(0, lastSlash);
    }

    /**
     * Creates or retrieves an element based on the XML path.
     */
    private static Element getOrCreateElement(Document document, Element rootElement, String xmlPath) throws Exception {
        String[] pathSegments = xmlPath.split("/");
        Element current = rootElement;
        for (String segment : pathSegments) {
            NodeList nodeList = current.getElementsByTagName(segment);
            if (nodeList.getLength() == 0) {
                Element newElement = document.createElement(segment);
                current.appendChild(newElement);
                current = newElement;
                logger.debug("Created element <{}>", segment);
            } else {
                current = (Element) nodeList.item(0);
                logger.debug("Reusing existing element <{}>", segment);
            }
        }
        return current;
    }

    /**
     * Converts an object value to the appropriate XML data type.
     *
     * @param value       The value to convert.
     * @param xmlDataType The target XML data type.
     * @return The converted value as a string.
     * @throws Exception if conversion fails.
     */
    private static String convertToXmlDataType(Object value, String xmlDataType) throws Exception {
        if (value == null) {
            return null;
        }

        switch (xmlDataType.toLowerCase()) {
            case "float":
                try {
                    float floatValue = Float.parseFloat(value.toString());
                    return String.valueOf(floatValue);
                } catch (NumberFormatException e) {
                    throw new Exception("Cannot convert value '" + value + "' to float.");
                }
                // Add more cases here for other XML Data Types as needed
            default:
                return value.toString();
        }
    }

    /**
     * Transforms an XML Document to a formatted String.
     */
    private static String transformDocumentToString(Document document) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        // Pretty print the XML
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        // Set XML declaration
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));

        return writer.getBuffer().toString();
    }

    /**
     * Writes the XML string to a file.
     *
     * @param xmlContent     The XML content as a string.
     * @param outputFilePath The file path where the XML will be written.
     * @throws Exception if an error occurs during file writing.
     */
    private static void writeXmlToFile(String xmlContent, String outputFilePath) throws Exception {
    logger.info("Writing XML content to file: {}", outputFilePath);
    File file = new File(outputFilePath);

    // Ensure parent directories exist
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
        boolean dirsCreated = parent.mkdirs();
        if (dirsCreated) {
            logger.debug("Created parent directories for file '{}'", outputFilePath);
        } else {
            logger.warn("Failed to create parent directories for file '{}'", outputFilePath);
        }
    }

    // Create a transformer to write the Document to the file
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();

    // Do not pretty print the XML
    transformer.setOutputProperty(OutputKeys.INDENT, "no");

    // Set XML declaration
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

    Source source = new DOMSource(parseXmlFromString(xmlContent));
    Result result = new StreamResult(file);
    transformer.transform(source, result);
}

    /**
     * Parses XML content from a string into a Document object.
     *
     * @param xmlContent The XML content as a string.
     * @return The parsed Document object.
     * @throws Exception if an error occurs during parsing.
     */
    private static Document parseXmlFromString(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(xmlContent)));
    }
}
