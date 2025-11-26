package fortehackathon.dto;

import lombok.Data;

@Data
public class AddTeamMemberRequest {

    private String username;
    private String email;
    private String jiraUsername;
    private String role;
}