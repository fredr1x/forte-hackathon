package fortehackathon.dto;

import lombok.Data;

@Data
public class  RegisterRequest {

    private String username;
    private String password;
    private String email;
    private Long telegramId;
    private String role;
    private Long teamId;
}