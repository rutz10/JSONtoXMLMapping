package org.rutz;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeLevelTransformation implements Serializable {

    public static final String INTEGER = "Integer";
    public static final String LONG = "Long";
    public static final String UTC = "UTC";
    public static final String DATE = "Date";
    public static final String STRING = "String";
    public static final String DOUBLE = "Double";

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeLevelTransformation.class);

    public static String transform(String sourceAttributeValue, Mapping mapping) throws Exception {
        String result;
        JexlContext context = new MapContext();

        // Log initial input and mapping information
        LOGGER.info("Starting transformation for field: {}", mapping.getJPath());
        LOGGER.info("Source value: {}", sourceAttributeValue);
        LOGGER.info("Mapping expression: {}", mapping.getExprsn());
        LOGGER.info("Expected XML type: {}", mapping.getXmlType());

        // Log the data type of sourceAttributeValue
        if (sourceAttributeValue != null) {
            LOGGER.info("Source value data type: {}", sourceAttributeValue.getClass().getName());
        } else {
            LOGGER.info("Source value is null");
        }

        // Ensure correct value and expression setup
        if (sourceAttributeValue != null && mapping.getExprsn() != null && !"".equals(mapping.getExprsn())) {
            try {
                if (sourceAttributeValue == null) {
                    context.set("val", null);
                } else {
                    Object convertedValue = convertToDataTypeValue(sourceAttributeValue, mapping.getXmlType());
                    context.set("val", convertedValue);
                    LOGGER.info("Converted value: {} (type: {})", convertedValue, convertedValue.getClass().getName());
                }
            } catch (DataTypeTransformationException e) {
                context.set("val", null);
                LOGGER.error("Error during setting JEXL context for attrName {} with Value {}",
                        mapping.getJPath(), sourceAttributeValue, e);
            }
        }

        // Log the context before evaluating the expression
        LOGGER.info("Evaluating expression with context: {}", context);

        try {
            // Evaluating the JEXL expression
            result = ExpressionEvaluator.attrEval(mapping.getExprsn(), context, String.class);
        } catch (Exception e) {
            LOGGER.error("Error evaluating JEXL expression for field: {} with value: {}", mapping.getJPath(), sourceAttributeValue, e);
            throw new AttributeLevelTransformationException("Transformation failed for field: "
                    + mapping.getJPath().split("\\.")[1] + " Val: " + sourceAttributeValue);
        }
        // Log the result after transformation
        if (result == null) {
            LOGGER.info("Transformation resulted in null value, returning source value: {}", sourceAttributeValue);
        } else {
            LOGGER.info("Transformation result for field: {} is {}", mapping.getJPath(), result);
        }

        return result == null ? sourceAttributeValue : result;
    }

    public static Object convertToDataTypeValue(String value, String dataType) throws DataTypeTransformationException {
        try {
            if (null == value || "null".equalsIgnoreCase(value)) {
                LOGGER.info("Received null value for conversion, returning null.");
                return null;
            }

            switch (dataType.toLowerCase()) {
                case DOUBLE:
                    LOGGER.info("Converting value {} to Double", value);
                    return Double.parseDouble(value);
                case INTEGER:
                    LOGGER.info("Converting value {} to Integer", value);
                    return Integer.parseInt(value);
                case LONG:
                    LOGGER.info("Converting value {} to Long", value);
                    return Long.valueOf(value);
                case DATE:
                    LOGGER.info("Converting value {} to Date", value);
                    SimpleDateFormat formatterDT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    formatterDT.setTimeZone(TimeZone.getDefault());
                    return formatterDT.parse(value);
                case STRING:
                    LOGGER.info("Returning value {} as String", value);
                    return value;
                default:
                    LOGGER.warn("Unrecognized data type for conversion: {}", dataType);
                    return value;  // Returning the original value if data type is unrecognized
            }
        } catch (Exception e) {
            LOGGER.error("Error during type conversion. DataType: {} Value: {}", dataType, value, e);
            throw new DataTypeTransformationException("Datatype: " + dataType + " Value: " + value);
        }
    }
}
