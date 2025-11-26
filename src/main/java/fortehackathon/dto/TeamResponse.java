package fortehackathon.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamResponse {
    private Long id;
    private String name;
    private String projectManager;
    private List<TeamMemberResponse> members;
    private String jiraProjectKey;
}