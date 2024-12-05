package org.rutz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class JsonParser {
    private ObjectMapper objectMapper;

    public JsonParser() {
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode parseJson(String jsonFilePath) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(jsonFilePath);
        if (is == null) {
            throw new IllegalArgumentException("File not found: " + jsonFilePath);
        }
        return objectMapper.readTree(is);
    }
}
