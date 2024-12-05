package org.rutz;

import com.fasterxml.jackson.databind.JsonNode;

import javax.xml.transform.TransformerException;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            // Define file paths relative to resources
            String csvMappingPath = "js.csv";   // Ensure this file is in src/main/resources/
            String jsonDataPath = "data.json";       // Ensure this file is in src/main/resources/
            String xmlOutputPath = "output.xml";     // Desired output path

            logger.info("Starting JSON to XML conversion.");

            // Step 1: Read CSV Mappings
            MappingReader mappingReader = new MappingReader();
            List<FieldMapping> mappings = mappingReader.readMappings(csvMappingPath);
            logger.info("CSV mappings loaded successfully.");

            // Step 2: Parse JSON Data
            JsonParser jsonParser = new JsonParser();
            JsonNode jsonData = jsonParser.parseJson(jsonDataPath);
            logger.info("JSON data parsed successfully.");

            // Step 3: Build XML
            XmlBuilder xmlBuilder = new XmlBuilder(mappings);
            xmlBuilder.buildXml(jsonData);
            logger.info("XML document constructed successfully.");

            // Step 4: Save XML to File
            xmlBuilder.saveXml(xmlOutputPath);
            logger.info("XML conversion completed successfully! Output saved to " + xmlOutputPath);

        } catch (ParserConfigurationException | TransformerException e) {
            logger.log(Level.SEVERE, "XML processing error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred: " + e.getMessage(), e);
        }
    }
}
