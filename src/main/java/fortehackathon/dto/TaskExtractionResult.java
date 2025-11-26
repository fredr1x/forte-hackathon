package fortehackathon.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskExtractionResult {

    private String summary;
    private String description;
    private String assigneeName;
    private String priority;
    private LocalDateTime deadline;
}