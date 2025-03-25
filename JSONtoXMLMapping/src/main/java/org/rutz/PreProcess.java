package org.rutz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;

public class PreProcess {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String cleanGraphQLResponse(String jsonResponse) throws IOException {
        // Parse the JSON response and clean it in one step
        JsonNode cleanedNode = cleanJsonNode(OBJECT_MAPPER.readTree(jsonResponse));
        return OBJECT_MAPPER.writeValueAsString(cleanedNode);
    }

    private static JsonNode cleanJsonNode(JsonNode node) {
        if (node == null) {
            return null;
        }

        // Clean object nodes
        if (node.isObject()) {
            ObjectNode cleaned = OBJECT_MAPPER.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                JsonNode cleanedChild = cleanJsonNode(entry.getValue());
                if (!isNullOrEmpty(cleanedChild)) {
                    cleaned.set(entry.getKey(), cleanedChild);
                }
            });
            return cleaned;
        }

        // Clean array nodes
        if (node.isArray()) {
            ArrayNode cleaned = OBJECT_MAPPER.createArrayNode();
            node.forEach(element -> {
                JsonNode cleanedElement = cleanJsonNode(element);
                if (!isNullOrEmpty(cleanedElement)) {
                    cleaned.add(cleanedElement);
                }
            });
            return cleaned;
        }

        return node;
    }

    private static boolean isNullOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }

        switch (node.getNodeType()) {
            case STRING:
                return node.asText().trim().isEmpty();
            case ARRAY:
                return node.size() == 0;
            case OBJECT:
                return node.size() == 0;
            default:
                return false;
        }
    }
}
