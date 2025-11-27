package fortehackathon.dto;

import lombok.Data;

@Data
public class PMLoginRequest {
    private String jiraEmail;
    private String password;
    private String jiraApiToken;
    private Long telegramId;
}