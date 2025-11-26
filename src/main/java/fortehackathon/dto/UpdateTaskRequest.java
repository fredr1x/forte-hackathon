package fortehackathon.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateTaskRequest {

    private String summary;
    private String description;
    private String status;
    private String priority;
    private LocalDateTime deadline;
    private Long assigneeId;
}