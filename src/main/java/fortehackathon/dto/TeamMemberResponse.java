package fortehackathon.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamMemberResponse {
    private Long id;
    private String username;
    private String role;
    private String email;
}