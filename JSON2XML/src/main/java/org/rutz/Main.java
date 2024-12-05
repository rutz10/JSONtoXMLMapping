package org.rutz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.List;
import java.util.logging.*;

/**
 * Main class to execute the JSON to XML conversion.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Configure Logger
        configureLogging();

        // Specify resource file names
        String mappingFileName = "mapping.csv";   // Located in src/main/resources/
        String inputJsonFileName = "data.json";  // Located in src/main/resources/
        String outputXmlPath = "output.xml";      // Output file path (can be absolute or relative)

        try {
            // Load Mappings from resources
            logger.info("Loading mappings from resources: " + mappingFileName);
            InputStream mappingInputStream = Main.class.getClassLoader().getResourceAsStream(mappingFileName);
            if (mappingInputStream == null) {
                logger.severe("Mapping file '" + mappingFileName + "' not found in resources.");
                throw new FileNotFoundException("Mapping file '" + mappingFileName + "' not found in resources.");
            }
            List<FieldMapping> mappings = MappingLoader.loadMappings(mappingInputStream);
            logger.info("Loaded " + mappings.size() + " mappings.");

            // Parse JSON from resources
            logger.info("Parsing JSON input from resources: " + inputJsonFileName);
            InputStream jsonInputStream = Main.class.getClassLoader().getResourceAsStream(inputJsonFileName);
            if (jsonInputStream == null) {
                logger.severe("Input JSON file '" + inputJsonFileName + "' not found in resources.");
                throw new FileNotFoundException("Input JSON file '" + inputJsonFileName + "' not found in resources.");
            }
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonData = objectMapper.readTree(jsonInputStream);
            logger.info("Parsed JSON data successfully.");

            // Initialize XmlBuilder
            XmlBuilder xmlBuilder = new XmlBuilder(mappings, outputXmlPath);

            // Build XML
            xmlBuilder.buildXml(jsonData);

            // Close XmlBuilder
            xmlBuilder.close();

            logger.info("XML conversion completed successfully! Output saved to " + outputXmlPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O Error: " + e.getMessage(), e);
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, "XML Stream Error: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Argument Error: " + e.getMessage(), e);
        }
    }

    /**
     * Configures the logger to output to the console with a simple format.
     */
    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        // Remove default handlers
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }

        // Create console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINE); // Set to FINE for detailed logs
        consoleHandler.setFormatter(new SimpleFormatter());

        // Add handler to root logger
        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(Level.INFO); // Set to INFO or FINE as needed
    }
}
