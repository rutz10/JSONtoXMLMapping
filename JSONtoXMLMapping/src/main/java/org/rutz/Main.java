package org.rutz;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try {
            // Path to the CSV file containing the mappings
            String csvFile = "C:\\Users\\rushi\\IdeaProjects\\JSONtoXMLMapping\\src\\main\\resources\\mappings.csv";
            // Step 2: Prepare JSON input for transformation (as an example)
            String jsonString = """
                {
                           "rootName": "ComplexRoot",
                           "metadata": {
                             "createdBy": "User123",
                             "timestamp": "2024-11-27T12:00:00Z"
                           },
                           "groups": [
                             {
                               "name": "ParentGroup",
                               "type": "parent",
                               "subGroups": [
                                 {
                                   "name": "ChildGroup1",
                                   "items": [
                                     {
                                       "id": "item1",
                                       "value": "5"
                                     },
                                     {
                                       "id": "item2",
                                       "value": "15"
                                     }
                                   ]
                                 },
                                 {
                                   "name": "ChildGroup2",
                                   "items": []
                                 }
                               ]
                             },
                             {
                               "name": "AnotherGroup",
                               "type": "independent",
                               "items": ["itemA", "itemB"]
                             }
                           ]
                         }
            """;

            // Step 3: Transform JSON to XML using JsonToXmlMapper
          List<Mapping> mappings = MappingGenerator.readMappingsFromCsv(csvFile);
          String actualXml = JsonToXmlMapper.transformJsonToXml(jsonString, mappings);
          System.out.println(actualXml);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the CSV file and returns a list of rows (each row is an array of strings).
     * @param csvFile Path to the CSV file
     * @return List of rows in the CSV
     */
    public static List<String[]> readCsvFile(String csvFile) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] row = line.split(",");
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }
}
