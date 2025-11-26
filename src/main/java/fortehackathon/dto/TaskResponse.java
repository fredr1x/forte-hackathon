package fortehackathon.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String key;
    private String summary;
    private String description;
    private String assignee;
    private String status;
    private String priority;
    private LocalDateTime deadline;
    private String url;
    private LocalDateTime createdAt;
}
