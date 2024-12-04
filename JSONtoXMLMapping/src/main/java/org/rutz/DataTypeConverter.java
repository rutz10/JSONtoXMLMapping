package org.rutz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTypeConverter {

    private static final Logger logger = LoggerFactory.getLogger(DataTypeConverter.class);

    /**
     * Converts a value from API data type to XML data type.
     *
     * @param value         The value to convert.
     * @param apiDataType   The data type of the API field.
     * @param xmlDataType   The desired data type in XML.
     * @return The converted value as a String.
     */
    public static String convert(Object value, String apiDataType, String xmlDataType) {
        logger.debug("Converting value '{}' from API data type '{}' to XML data type '{}'", value, apiDataType, xmlDataType);
        if (value == null) {
            logger.warn("Value is null. Returning empty string.");
            return "";
        }

        try {
            switch (apiDataType) {
                case "String":
                    return value.toString();
                case "int":
                case "Integer":
                    return String.valueOf((Integer) value);
                case "long":
                case "Long":
                    return String.valueOf((Long) value);
                case "double":
                case "Double":
                    return String.valueOf((Double) value);
                case "boolean":
                case "Boolean":
                    return String.valueOf((Boolean) value);
                // Add more data type conversions as needed
                default:
                    logger.warn("Unsupported API data type '{}'. Converting to String.", apiDataType);
                    return value.toString();
            }
        } catch (Exception e) {
            logger.error("Error converting value '{}': {}", value, e.getMessage());
            return "";
        }
    }
}
