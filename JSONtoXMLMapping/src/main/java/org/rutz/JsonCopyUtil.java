import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonCopyUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generic method to copy array elements based on a specific condition
     * 
     * @param jsonResponse Original JSON response as a string
     * @param arrayToSearch Name of the array to search within
     * @param fieldToMatch Nested field path to match (e.g., "departmentObject.departmentName")
     * @param valueToMatch Value to match against
     * @param newArrayName Name of the new array to create
     * @return Modified JSON as a string
     * @throws IOException If there's an error parsing the JSON
     */
    public static String copyNestedArrayElements(
            String jsonResponse, 
            String arrayToSearch, 
            String fieldToMatch, 
            String valueToMatch, 
            String newArrayName) throws IOException {
        
        // Parse the input JSON
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        // Perform the copy operation
        JsonNode modifiedNode = copyNestedArrayElements(
            rootNode, 
            arrayToSearch, 
            fieldToMatch, 
            valueToMatch, 
            newArrayName
        );

        // Convert to formatted JSON string
        return objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(modifiedNode);
    }

    /**
     * Overloaded method to work with JsonNode directly
     */
    public static JsonNode copyNestedArrayElements(
            JsonNode rootNode, 
            String arrayToSearch, 
            String fieldToMatch, 
            String valueToMatch, 
            String newArrayName) {
        
        // Create a deep copy of the original node to avoid modifying the original
        JsonNode modifiedNode = objectMapper.valueToTree(rootNode);

        // Recursively search and modify the node
        searchAndCopyNestedArray(
            (ObjectNode) modifiedNode, 
            arrayToSearch, 
            fieldToMatch, 
            valueToMatch, 
            newArrayName
        );

        return modifiedNode;
    }

    /**
     * Recursive method to search and copy nested arrays
     */
    private static void searchAndCopyNestedArray(
            ObjectNode currentNode, 
            String arrayToSearch, 
            String fieldToMatch, 
            String valueToMatch, 
            String newArrayName) {
        
        // Create a list to store keys to process
        List<String> keysToProcess = new ArrayList<>();
        
        // Collect keys first to avoid concurrent modification
        Iterator<Map.Entry<String, JsonNode>> fields = currentNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            keysToProcess.add(entry.getKey());
        }
        
        // Process collected keys
        for (String key : keysToProcess) {
            JsonNode value = currentNode.get(key);
            
            // If the current field is an array with the specified name
            if (key.equals(arrayToSearch) && value.isArray()) {
                ArrayNode originalArray = (ArrayNode) value;
                ArrayNode matchedElements = objectMapper.createArrayNode();

                // Find and copy matching elements
                for (JsonNode element : originalArray) {
                    if (element.isObject()) {
                        // Handle nested field matching
                        JsonNode matchedField = findNestedField(element, fieldToMatch);
                        
                        if (matchedField != null && matchedField.asText().equals(valueToMatch)) {
                            matchedElements.add(element.deepCopy());
                        }
                    }
                }

                // Add the new array if not empty
                if (matchedElements.size() > 0) {
                    currentNode.set(newArrayName, matchedElements);
                }
            } 
            // If the current field is an object, recursively search
            else if (value.isObject()) {
                searchAndCopyNestedArray(
                    (ObjectNode) value, 
                    arrayToSearch, 
                    fieldToMatch, 
                    valueToMatch, 
                    newArrayName
                );
            } 
            // If the current field is an array, recursively search each object
            else if (value.isArray()) {
                for (JsonNode arrayElement : value) {
                    if (arrayElement.isObject()) {
                        searchAndCopyNestedArray(
                            (ObjectNode) arrayElement, 
                            arrayToSearch, 
                            fieldToMatch, 
                            valueToMatch, 
                            newArrayName
                        );
                    }
                }
            }
        }
    }

    /**
     * Find a nested field in a JsonNode using dot notation
     * 
     * @param node JsonNode to search
     * @param fieldPath Dot-separated field path (e.g., "departmentObject.departmentName")
     * @return Matched JsonNode or null if not found
     */
    private static JsonNode findNestedField(JsonNode node, String fieldPath) {
        // Split the field path
        String[] pathParts = fieldPath.split("\\.");
        
        // Traverse the node
        JsonNode currentNode = node;
        for (String part : pathParts) {
            if (currentNode == null || !currentNode.isObject()) {
                return null;
            }
            currentNode = currentNode.get(part);
        }
        
        return currentNode;
    }
}
