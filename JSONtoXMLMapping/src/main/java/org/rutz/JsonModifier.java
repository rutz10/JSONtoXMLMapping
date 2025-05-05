package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class JsonModifier {

    public static JSONObject modifyJson(JSONObject inputJson, String targetFieldName, String targetFieldValue, String copyFieldName) {
        // Process the JSON object recursively
        processJsonObject(inputJson, targetFieldName, targetFieldValue, copyFieldName);
        return inputJson;
    }

    private static void processJsonObject(JSONObject jsonObject, String targetFieldName, String targetFieldValue, String copyFieldName) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);

            if (value instanceof JSONObject) {
                JSONObject childObj = (JSONObject) value;
                boolean matched = childObj.has(targetFieldName) && targetFieldValue.equals(childObj.optString(targetFieldName));
                if (matched && childObj.has("projects")) {
                    deepCopyArrayAndAddField(childObj, copyFieldName, targetFieldName);
                    // Skip further recursion for this object since it's already processed
                    continue;
                }
                processJsonObject(childObj, targetFieldName, targetFieldValue, copyFieldName);
            } else if (value instanceof JSONArray) {
                processJsonArray((JSONArray) value, targetFieldName, targetFieldValue, copyFieldName);
            }
        }
    }

    private static void processJsonArray(JSONArray jsonArray, String targetFieldName, String targetFieldValue, String copyFieldName) {
        for (int i = 0; i < jsonArray.length(); i++) {
            Object element = jsonArray.get(i);
            if (element instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) element;
                boolean matched = jsonObject.has(targetFieldName) && targetFieldValue.equals(jsonObject.optString(targetFieldName));
                if (matched && jsonObject.has("projects")) {
                    deepCopyArrayAndAddField(jsonObject, copyFieldName, targetFieldName);
                    // Skip further recursion for this object since it's already processed
                    continue;
                }
                processJsonObject(jsonObject, targetFieldName, targetFieldValue, copyFieldName);
            }
        }
    }

    private static void deepCopyArrayAndAddField(JSONObject jsonObject, String copyFieldName, String targetFieldName) {
        JSONArray projects = jsonObject.optJSONArray("projects");
        if (projects == null) return;
        JSONArray projectsCopied = new JSONArray();
        for (int i = 0; i < projects.length(); i++) {
            JSONObject project = projects.getJSONObject(i);
            JSONObject copiedProject = new JSONObject(project.toString());
            copiedProject.put(copyFieldName, jsonObject.optString(targetFieldName));
            projectsCopied.put(copiedProject);
        }
        jsonObject.put("projects_copied", projectsCopied);
    }

    public static void main(String[] args) {
        // Read the JSON data from a file in the resources folder
        JSONObject inputJson = loadJsonFromFile("data.json");

        if (inputJson != null) {
            // Modify the JSON based on the specified role and copy field name
            JSONObject modifiedJson = modifyJson(inputJson, "role", "Software Engineer", "Copied_field_role");

            // Output the modified JSON
            System.out.println(modifiedJson.toString(2)); // Pretty print with 2 spaces
        }
    }

    private static JSONObject loadJsonFromFile(String fileName) {
        // Try to load the file from the resources folder
        try (InputStream inputStream = JsonModifier.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                System.err.println("File not found: " + fileName);
                return null;
            }

            // Convert the InputStream to a String
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // Return the parsed JSONObject
            return new JSONObject(jsonContent);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
