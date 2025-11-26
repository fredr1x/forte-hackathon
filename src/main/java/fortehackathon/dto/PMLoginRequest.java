package fortehackathon.dto;

import lombok.Data;

@Data
public class PMLoginRequest {
    private String username;
    private String password;
    private Long telegramId;
}