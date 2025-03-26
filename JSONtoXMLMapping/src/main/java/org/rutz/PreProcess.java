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

--------------------------------------------
    // Clean the responses using the utility class in the reactive stream
        graphApiResponses
            .map(response -> {
                try {
                    // Clean the response using GraphQLResponseCleanerUtil
                    return GraphQLResponseCleanerUtil.cleanGraphQLResponse(response);
                } catch (IOException e) {
                    // Handle JSON processing exceptions (e.g., malformed JSON)
                    System.err.println("Error cleaning response: " + e.getMessage());
                    return response; // Return original response if cleaning fails
                }
            })
            .subscribe(cleanedResponse -> {
                // Use the cleaned response (e.g., log, process, etc.)
                System.out.println("Cleaned Response: " + cleanedResponse);
            });
    }
	
	*------------------------------------------*

public void processGraphQLResponses(Flux<String> graphApiResponses) {
        graphApiResponses
            .map(response -> {
                try {
                    // Clean the response using GraphQLResponseCleanerUtil
                    return GraphQLResponseCleanerUtil.cleanGraphQLResponse(response);
                } catch (IOException e) {
                    // Log the error with more context
                    logger.error("Error cleaning GraphQL response", e);
                    
                    // Option 1: Return original response
                    return response;
                    
                }
            })
    
            .subscribe(
                cleanedResponse -> {
                    // Process the cleaned response
                    logger.info("Processed Cleaned Response: {}", cleanedResponse);
                },
                error -> {
                    // Handle any unexpected errors in the stream
                    logger.error("Error in GraphQL response processing", error);
                },
                () -> {
                    // Completion handler
                    logger.info("GraphQL response processing completed");
                }
            );
    }

    // Alternative method with more explicit error handling
    public Flux<String> cleanGraphQLResponses(Flux<String> graphApiResponses) {
        return graphApiResponses
            .map(response -> {
                try {
                    return GraphQLResponseCleanerUtil.cleanGraphQLResponse(response);
                } catch (IOException e) {
                    logger.error("Error cleaning GraphQL response", e);
                    return response; // or handle differently based on your requirements
                }
            });
    }
	
