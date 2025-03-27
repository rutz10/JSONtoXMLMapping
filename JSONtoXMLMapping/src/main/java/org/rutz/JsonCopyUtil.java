package org.rutz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class JsonCopyUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String copyNestedArrayElements(
            String jsonResponse,
            String arrayToSearch,
            String fieldToMatch,
            String valueToMatch,
            String newArrayName) throws IOException {

        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode modifiedNode = copyNestedArrayElements(rootNode, arrayToSearch, fieldToMatch, valueToMatch, newArrayName);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(modifiedNode);
    }

    public static JsonNode copyNestedArrayElements(
            JsonNode rootNode,
            String arrayToSearch,
            String fieldToMatch,
            String valueToMatch,
            String newArrayName) {

        JsonNode modifiedNode = rootNode.deepCopy(); //copy only once.
        searchAndCopyNestedArray((ObjectNode) modifiedNode, arrayToSearch, fieldToMatch, valueToMatch, newArrayName);
        return modifiedNode;
    }

    private static void searchAndCopyNestedArray(
            ObjectNode rootNode,
            String arrayToSearch,
            String fieldToMatch,
            String valueToMatch,
            String newArrayName) {

        Queue<ObjectNode> queue = new LinkedList<>();
        queue.add(rootNode);

        while (!queue.isEmpty()) {
            ObjectNode currentNode = queue.poll();
            Iterator<java.util.Map.Entry<String, JsonNode>> fields = currentNode.fields();

            while (fields.hasNext()) {
                java.util.Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();

                if (fieldName.equals(arrayToSearch) && value.isArray()) {
                    ArrayNode originalArray = (ArrayNode) value;
                    ArrayNode matchedElements = objectMapper.createArrayNode();

                    for (JsonNode element : originalArray) {
                        if (element.isObject()) {
                            JsonNode matchedField = findNestedField(element, fieldToMatch);
                            if (matchedField != null && matchedField.asText().equals(valueToMatch)) {
                                matchedElements.add(element.deepCopy());
                            }
                        }
                    }
                    if (matchedElements.size() > 0) {
                        currentNode.set(newArrayName, matchedElements);
                    }
                } else if (value.isObject()) {
                    queue.add((ObjectNode) value);
                } else if (value.isArray()) {
                    for (JsonNode arrayElement : value) {
                        if (arrayElement.isObject()) {
                            queue.add((ObjectNode) arrayElement);
                        }
                    }
                }
            }
        }
    }

    private static JsonNode findNestedField(JsonNode node, String fieldPath) {
        String[] pathParts = fieldPath.split("\\.");
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
