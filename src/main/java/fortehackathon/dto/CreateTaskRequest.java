package fortehackathon.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateTaskRequest {

    private String summary;
    private String description;
    private Long assigneeId;
    private String priority;
    private LocalDateTime deadline;
}