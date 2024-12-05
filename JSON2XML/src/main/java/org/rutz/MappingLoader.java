package org.rutz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to load field mappings from a CSV file.
 */
public class MappingLoader {
    /**
     * Loads mappings from a CSV InputStream.
     *
     * @param inputStream The InputStream of the mapping CSV file.
     * @return A list of FieldMapping objects.
     * @throws IOException If an I/O error occurs.
     */
    public static List<FieldMapping> loadMappings(InputStream inputStream) throws IOException {
        List<FieldMapping> mappings = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                // Skip header
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",", -1); // -1 to include trailing empty strings
                if (parts.length < 4) {
                    System.err.println("Invalid mapping line: " + line);
                    continue;
                }
                String jsonField = parts[0].trim();
                String dataType = parts[1].trim();
                String group = parts[2].trim();
                String xmlPath = parts[3].trim();
                mappings.add(new FieldMapping(jsonField, dataType, group, xmlPath));
            }
        }
        return mappings;
    }
}
