package org.rutz;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses CSV mapping files into a list of Mapping objects.
 */
public class MappingParser {

    /**
     * Parses the mappings from a CSV file located in the resources folder.
     *
     * @param csvFileName The name of the CSV file in the resources folder.
     * @return A list of Mapping objects.
     * @throws Exception If the file is not found or an I/O error occurs.
     */
    public static List<Mapping> parseMappings(String csvFileName) throws Exception {
        List<Mapping> mappings = new ArrayList<>();

        ClassLoader classLoader = MappingParser.class.getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(csvFileName)) {
            if (is == null) {
                throw new IllegalArgumentException("CSV file not found: " + csvFileName);
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                         .withFirstRecordAsHeader()
                         .withIgnoreHeaderCase()
                         .withTrim())) {

                for (CSVRecord csvRecord : csvParser) {
                    String jPath = csvRecord.get("jPath");
                    String xPath = csvRecord.get("xPath");
                    boolean isList = Boolean.parseBoolean(csvRecord.get("isList"));
                    String jsonType = csvRecord.get("jsonType");
                    String exprsn = csvRecord.get("exprsn");
                    String namespace = csvRecord.get("namespace");

                    Mapping mapping = new Mapping(jPath, xPath, isList, jsonType, exprsn, namespace, namespace);
                    mappings.add(mapping);
                }
            }
        }

        return mappings;
    }
}
