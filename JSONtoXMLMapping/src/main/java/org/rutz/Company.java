// File: Company.java
package org.rutz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Company with multiple Branches.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {
    private String companyName;
    private String companyLocation;
    private List<Branch> branches = new ArrayList<>();

    /**
     * Represents a Branch within a Company, located in NA and EU regions.
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class Branch {
        private String branchNameNA;
        private String branchNameEU;
        private List<Team> teams = new ArrayList<>();
    }

    /**
     * Represents a Team within a Branch.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Team {
        private String teamNameSD;
        private String teamNameMK;
        private List<Member> members = new ArrayList<>();
    }

    /**
     * Represents a Member within a Team.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Member {
        private String id;
        private String name;
        private String role;
        private String salary;
        private List<Task> tasks = new ArrayList<>();
        private List<Campaign> campaigns = new ArrayList<>();
    }

    /**
     * Represents a Task assigned to a Member.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Task {
        private String taskId;
        private String description;
        private String status;
    }

    /**
     * Represents a Campaign associated with a Member.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Campaign {
        private String campaignId;
        private String name;
        private String status;
    }
}
