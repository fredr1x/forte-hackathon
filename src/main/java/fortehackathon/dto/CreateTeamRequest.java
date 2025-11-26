package fortehackathon.dto;

import lombok.Data;

@Data
public class CreateTeamRequest {
    private String name;
    private String jiraProjectKey;
    private String jiraUrl;
}