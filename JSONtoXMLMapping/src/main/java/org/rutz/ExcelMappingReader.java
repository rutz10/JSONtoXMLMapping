package org.rutz;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelMappingReader {

    private static final Logger logger = LoggerFactory.getLogger(ExcelMappingReader.class);

    public static class XmlMapping {
        private String group;
        private String apiFieldName;
        private String apiDataType;
        private String xmlDataType;
        private String xmlPath;

        // Constructor
        public XmlMapping(String group, String apiFieldName, String apiDataType, String xmlDataType, String xmlPath) {
            this.group = group;
            this.apiFieldName = apiFieldName;
            this.apiDataType = apiDataType;
            this.xmlDataType = xmlDataType;
            this.xmlPath = xmlPath;
        }

        // Getters
        public String getGroup() {
            return group;
        }

        public String getApiFieldName() {
            return apiFieldName;
        }

        public String getApiDataType() {
            return apiDataType;
        }

        public String getXmlDataType() {
            return xmlDataType;
        }

        public String getXmlPath() {
            return xmlPath;
        }

        @Override
        public String toString() {
            return "XmlMapping{" +
                    "group='" + group + '\'' +
                    ", apiFieldName='" + apiFieldName + '\'' +
                    ", apiDataType='" + apiDataType + '\'' +
                    ", xmlDataType='" + xmlDataType + '\'' +
                    ", xmlPath='" + xmlPath + '\'' +
                    '}';
        }
    }

    /**
     * Reads the Excel file from the resource folder and returns a list of XmlMapping objects.
     *
     * @param excelFileName Name of the Excel file (e.g., "field_mappings.xlsx").
     * @return List of XmlMapping.
     * @throws Exception If an error occurs during reading.
     */
    public static List<XmlMapping> readMappings(String excelFileName) throws Exception {
        logger.info("Starting to read Excel mappings from resource: {}", excelFileName);
        List<XmlMapping> mappings = new ArrayList<>();

        // Use ClassLoader to load the resource as a stream
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(excelFileName)) {
            if (is == null) {
                String errorMsg = "Resource not found: " + excelFileName;
                logger.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0); // Assuming data is in the first sheet
            logger.debug("Excel sheet '{}' loaded successfully.", sheet.getSheetName());

            // Iterate over rows, skipping the header
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Start from row 1 to skip header
                Row row = sheet.getRow(i);
                if (row == null) {
                    logger.warn("Row {} is empty. Skipping.", i + 1);
                    continue; // Skip empty rows
                }

                String group = getCellValueAsString(row.getCell(0));
                String apiFieldName = getCellValueAsString(row.getCell(1));
                String apiDataType = getCellValueAsString(row.getCell(2));
                String xmlDataType = getCellValueAsString(row.getCell(3));
                String xmlPath = getCellValueAsString(row.getCell(4));

                if (apiFieldName.isEmpty()) {
                    logger.warn("Row {} has an empty API Field Name. Skipping.", i + 1);
                    continue; // Skip if API Field Name is empty
                }

                XmlMapping mapping = new XmlMapping(group, apiFieldName, apiDataType, xmlDataType, xmlPath);
                mappings.add(mapping);
                logger.debug("Added mapping: {}", mapping);
            }

            workbook.close();
            logger.info("Excel mappings loaded successfully. Total mappings: {}", mappings.size());
        } catch (Exception e) {
            logger.error("Error while reading Excel mappings: {}", e.getMessage(), e);
            throw e;
        }

        return mappings;
    }

    /**
     * Helper method to get cell value as String.
     *
     * @param cell The cell to read.
     * @return String representation of the cell value.
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString(); // Customize date format as needed
                } else {
                    double num = cell.getNumericCellValue();
                    if (num == (long) num)
                        return String.valueOf((long) num);
                    else
                        return String.valueOf(num);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Evaluate the formula and return the result as String
                FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                CellValue cellValue = evaluator.evaluate(cell);
                switch (cellValue.getCellType()) {
                    case BOOLEAN:
                        return String.valueOf(cellValue.getBooleanValue());
                    case NUMERIC:
                        double num = cellValue.getNumberValue();
                        if (num == (long) num)
                            return String.valueOf((long) num);
                        else
                            return String.valueOf(num);
                    case STRING:
                        return cellValue.getStringValue().trim();
                    default:
                        return "";
                }
            case BLANK:
            default:
                return "";
        }
    }
}
