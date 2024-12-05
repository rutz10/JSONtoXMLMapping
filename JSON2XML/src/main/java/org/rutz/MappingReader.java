package org.rutz;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MappingReader {
    public List<FieldMapping> readMappings(String csvFilePath) throws Exception {
        List<FieldMapping> mappings = new ArrayList<>();

        InputStream is = getClass().getClassLoader().getResourceAsStream(csvFilePath);
        if (is == null) {
            throw new IllegalArgumentException("File not found: " + csvFilePath);
        }

        try (InputStreamReader reader = new InputStreamReader(is);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                String jsonField = record.get("JSON Field").trim();
                String dataType = record.get("Data Type").trim();
                String group = record.get("Group").trim();
                String xmlPath = record.get("XML Path").trim();

                FieldMapping mapping = new FieldMapping(jsonField, dataType, group, xmlPath);
                mappings.add(mapping);
            }
        }

        return mappings;
    }
}
