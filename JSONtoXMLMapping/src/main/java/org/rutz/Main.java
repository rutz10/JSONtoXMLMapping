// File: Main.java
package org.rutz;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class Main {
    public static void main(String[] args) {
        // Create Tasks
        Company.Task task1 = Company.Task.builder()
                .taskId("T001")
                .description("Develop API")
                .status("Completed")
                .build();

        // Create Campaigns
        Company.Campaign campaign1 = Company.Campaign.builder()
                .campaignId("C001")
                .name("Winter Sale")
                .status("Ongoing")
                .build();

//        Company.Campaign campaign2 = Company.Campaign.builder()
//                .campaignId("C002")
//                .name("Summer Sale")
//                .build();

        // Create Members with Salary
        Company.Member member1 = Company.Member.builder()
                .id("S101")
                .name("Michael Turner")
                .role("Lead Developer")
                .salary("1500.45") // Setting salary as String
                .tasks(Arrays.asList(task1))
                .campaigns(Arrays.asList(campaign1))
                .build();

        // Create Teams
        Company.Team team1 = Company.Team.builder()
                .teamNameSD("Software Development SD")
                .teamNameMK("Software Development MK")
                .members(Arrays.asList(member1))
                .build();

        // Create Branches
        Company.Branch branch1 = Company.Branch.builder()
                .branchNameNA("North America")
                .branchNameEU("North America Branch EU")
                .teams(Arrays.asList(team1))
                .build();

        // Create Company
        Company company = Company.builder()
                .companyName("Global Enterprises")
                .companyLocation("London")
                .branches(Arrays.asList(branch1))
                .build();

        // Define the output XML file path
        String outputFilePath = "output/company.xml"; // Specify your desired path

        // Proceed with XML conversion using XmlBuilder
        try {
            // Assume that ExcelMappingReader.readMappings("ur.xlsx") correctly reads your ur.xlsx mappings
            List<ExcelMappingReader.XmlMapping> mappings = ExcelMappingReader.readMappings("my.xlsx");

            // Build XML
            XmlBuilder.buildXml(mappings, company, outputFilePath);

            System.out.println("XML has been successfully written to " + outputFilePath);
        } catch (Exception e) {
            log.error("Error during XML generation: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
